package id.papicode.gudang.service;

import id.papicode.gudang.db.Database;
import id.papicode.gudang.model.*;
import id.papicode.gudang.util.Waktu;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Dokumen gudang: Barang Masuk (BM), Barang Keluar (BK), dan Stok Opname (OP).
 * Semua perubahan stok terjadi di sini, di dalam transaksi BEGIN IMMEDIATE, dan tercatat di tabel mutasi.
 */
public class DokumenService {

    public static final int MAKS_QTY = 1_000_000;
    public static final int MAKS_MUNDUR_HARI = 366;

    private static final String SELECT = """
            SELECT d.*, p.nama AS pihak, u.nama AS oleh,
              (SELECT COUNT(*) FROM dokumen_item i WHERE i.dokumen_id=d.id) AS n,
              (SELECT COALESCE(SUM(CASE WHEN d.jenis='OPNAME' THEN i.qty - i.stok_sistem ELSE i.qty END),0) FROM dokumen_item i WHERE i.dokumen_id=d.id) AS tq,
              (SELECT COALESCE(SUM(CASE WHEN d.jenis='OPNAME' THEN (i.qty - i.stok_sistem) * i.harga ELSE i.qty * i.harga END),0) FROM dokumen_item i WHERE i.dokumen_id=d.id) AS tn
            FROM dokumen d LEFT JOIN pihak p ON p.id=d.pihak_id JOIN pengguna u ON u.id=d.dibuat_oleh""";

    private final Database db;
    private final Clock jam;

    public DokumenService(Database db, Clock jam) {
        this.db = db;
        this.jam = jam;
    }

    public Dokumen barangMasuk(Pengguna aktor, LocalDate tgl, long supplierId, String referensi, String catatan, List<BarisInput> baris) {
        return simpan(aktor, Dokumen.MASUK, tgl, supplierId, referensi, catatan, baris);
    }

    public Dokumen barangKeluar(Pengguna aktor, LocalDate tgl, long tujuanId, String referensi, String catatan, List<BarisInput> baris) {
        return simpan(aktor, Dokumen.KELUAR, tgl, tujuanId, referensi, catatan, baris);
    }

    /** Stok opname: qty tiap baris = hasil hitung fisik. Stok sistem disamakan dan selisihnya dicatat (admin). */
    public Dokumen opname(Pengguna aktor, LocalDate tgl, String catatan, List<BarisInput> hitungFisik) {
        AuthService.wajibAdmin(aktor);
        return simpan(aktor, Dokumen.OPNAME, tgl, 0, null, catatan, hitungFisik);
    }

    private Dokumen simpan(Pengguna aktor, String jenis, LocalDate tgl, long pihakId, String referensi, String catatan, List<BarisInput> baris) {
        if (aktor == null) throw new Gagal("Silakan login dulu.");
        LocalDate hariIni = LocalDate.now(jam);
        if (tgl == null) throw new Gagal("Tanggal wajib diisi.");
        if (tgl.isAfter(hariIni)) throw new Gagal("Tanggal dokumen tidak boleh di masa depan.");
        if (tgl.isBefore(hariIni.minusDays(MAKS_MUNDUR_HARI))) throw new Gagal("Tanggal dokumen terlalu lama (maks 1 tahun ke belakang).");
        if (baris == null || baris.isEmpty()) throw new Gagal("Tambahkan minimal satu barang.");
        String ref = referensi == null ? "" : referensi.trim();
        String cat = catatan == null ? "" : catatan.trim();
        if (ref.length() > 60) throw new Gagal("No. referensi maksimal 60 karakter.");
        if (cat.length() > 300) throw new Gagal("Catatan maksimal 300 karakter.");

        Map<Long, Integer> gabung = new LinkedHashMap<>();
        for (BarisInput b : baris) {
            boolean opname = jenis.equals(Dokumen.OPNAME);
            if (opname ? b.qty() < 0 : b.qty() <= 0) throw new Gagal(opname ? "Stok fisik tidak boleh negatif." : "Jumlah barang harus lebih dari 0.");
            if (b.qty() > MAKS_QTY) throw new Gagal("Jumlah terlalu besar.");
            if (opname && gabung.containsKey(b.barangId())) throw new Gagal("Barang yang sama muncul dua kali di lembar opname.");
            gabung.merge(b.barangId(), b.qty(), Integer::sum);
        }
        for (int q : gabung.values()) if (q > MAKS_QTY) throw new Gagal("Jumlah terlalu besar.");

        return db.tx(c -> {
            Long pihak = null;
            if (!jenis.equals(Dokumen.OPNAME)) {
                String harus = jenis.equals(Dokumen.MASUK) ? Pihak.SUPPLIER : Pihak.TUJUAN;
                String jp = Database.satu(c, "SELECT jenis FROM pihak WHERE id=? AND aktif=1", rs -> rs.getString(1), pihakId);
                if (!harus.equals(jp)) throw new Gagal(jenis.equals(Dokumen.MASUK) ? "Pilih supplier." : "Pilih tujuan pengiriman.");
                pihak = pihakId;
            }
            String nomor = nomorBaru(c, jenis, tgl);
            long id = Database.sisip(c, """
                    INSERT INTO dokumen(nomor,jenis,tanggal,pihak_id,referensi,catatan,dibuat_oleh,dibuat_pada,status)
                    VALUES(?,?,?,?,?,?,?,?,'AKTIF')""",
                    nomor, jenis, tgl.toString(), pihak, ref, cat, aktor.id(), Waktu.db(LocalDateTime.now(jam)));
            for (Map.Entry<Long, Integer> e : gabung.entrySet()) {
                long bid = e.getKey();
                int qty = e.getValue();
                Object[] b = Database.satu(c, "SELECT nama, stok, harga, aktif, satuan FROM barang WHERE id=?",
                        rs -> new Object[]{rs.getString(1), rs.getInt(2), rs.getLong(3), rs.getInt(4), rs.getString(5)}, bid);
                if (b == null || (int) b[3] != 1) throw new Gagal("Ada barang yang tidak aktif/tidak ditemukan. Muat ulang daftar.");
                switch (jenis) {
                    case Dokumen.MASUK -> {
                        Database.ubah(c, "UPDATE barang SET stok = stok + ? WHERE id=?", qty, bid);
                        Database.ubah(c, "INSERT INTO dokumen_item(dokumen_id,barang_id,qty,harga) VALUES(?,?,?,?)", id, bid, qty, b[2]);
                        mutasi(c, bid, id, tgl, "MASUK", qty, nomor);
                    }
                    case Dokumen.KELUAR -> {
                        if (Database.ubah(c, "UPDATE barang SET stok = stok - ? WHERE id=? AND stok >= ?", qty, bid, qty) == 0) {
                            throw new Gagal("Stok " + b[0] + " tidak cukup (tersedia " + b[1] + " " + b[4] + ").");
                        }
                        Database.ubah(c, "INSERT INTO dokumen_item(dokumen_id,barang_id,qty,harga) VALUES(?,?,?,?)", id, bid, qty, b[2]);
                        mutasi(c, bid, id, tgl, "KELUAR", -qty, nomor);
                    }
                    default -> { // OPNAME
                        int sistem = (int) b[1];
                        Database.ubah(c, "INSERT INTO dokumen_item(dokumen_id,barang_id,qty,stok_sistem,harga) VALUES(?,?,?,?,?)", id, bid, qty, sistem, b[2]);
                        if (qty != sistem) {
                            Database.ubah(c, "UPDATE barang SET stok=? WHERE id=?", qty, bid);
                            mutasi(c, bid, id, tgl, "KOREKSI", qty - sistem, "Opname " + nomor);
                        }
                    }
                }
            }
            return Database.satu(c, SELECT + " WHERE d.id=?", DokumenService::map, id);
        });
    }

    /** Nomor per jenis per hari: BM-yyMMdd-001, BK-..., OP-.... Dibuat di dalam transaksi sehingga tidak dobel. */
    static String nomorBaru(Connection c, String jenis, LocalDate tgl) throws SQLException {
        String kode = switch (jenis) { case Dokumen.MASUK -> "BM"; case Dokumen.KELUAR -> "BK"; default -> "OP"; };
        String prefix = kode + "-" + tgl.format(DateTimeFormatter.ofPattern("yyMMdd")) + "-";
        String last = Database.satu(c, "SELECT nomor FROM dokumen WHERE nomor LIKE ? ORDER BY nomor DESC LIMIT 1", rs -> rs.getString(1), prefix + "%");
        int urut = last == null ? 1 : Integer.parseInt(last.substring(prefix.length())) + 1;
        return prefix + String.format("%03d", urut);
    }

    private void mutasi(Connection c, long barangId, long dokId, LocalDate tgl, String jenis, int qty, String ket) throws SQLException {
        Database.ubah(c, "INSERT INTO mutasi(barang_id,dokumen_id,tanggal,jenis,qty,keterangan) VALUES(?,?,?,?,?,?)",
                barangId, dokId, tgl.toString(), jenis, qty, ket);
    }

    /**
     * Batalkan dokumen masuk/keluar (admin). Stok dibalik. Pembatalan barang masuk ditolak bila barangnya
     * sudah terlanjur keluar sehingga stok tidak cukup untuk ditarik kembali. Opname tidak bisa dibatalkan
     * (buat opname baru untuk mengoreksi).
     */
    public void batal(Pengguna aktor, long dokId, String alasan) {
        AuthService.wajibAdmin(aktor);
        if (alasan == null || alasan.trim().length() < 3) throw new Gagal("Tulis alasan pembatalan.");
        db.tx(c -> {
            Dokumen d = Database.satu(c, SELECT + " WHERE d.id=?", DokumenService::map, dokId);
            if (d == null) throw new Gagal("Dokumen tidak ditemukan.");
            if (d.batal()) throw new Gagal("Dokumen ini sudah dibatalkan.");
            if (d.jenis().equals(Dokumen.OPNAME)) throw new Gagal("Dokumen opname tidak bisa dibatalkan. Lakukan opname ulang bila ada salah hitung.");
            Database.ubah(c, "UPDATE dokumen SET status='BATAL', alasan_batal=? WHERE id=?", alasan.trim(), dokId);
            LocalDate tgl = LocalDate.parse(d.tanggal());
            for (ItemDokumen it : Database.daftar(c, SELECT_ITEM, DokumenService::mapItem, dokId)) {
                if (d.jenis().equals(Dokumen.MASUK)) {
                    if (Database.ubah(c, "UPDATE barang SET stok = stok - ? WHERE id=? AND stok >= ?", it.qty(), it.barangId(), it.qty()) == 0) {
                        throw new Gagal("Tidak bisa dibatalkan: stok " + it.nama() + " sudah terpakai/keluar sehingga kurang dari " + it.qty() + ".");
                    }
                    mutasi(c, it.barangId(), dokId, tgl, "BATAL", -it.qty(), "Batal " + d.nomor());
                } else {
                    Database.ubah(c, "UPDATE barang SET stok = stok + ? WHERE id=?", it.qty(), it.barangId());
                    mutasi(c, it.barangId(), dokId, tgl, "BATAL", it.qty(), "Batal " + d.nomor());
                }
            }
            return null;
        });
    }

    public List<Dokumen> daftar(String jenis, LocalDate dari, LocalDate sampai, String cari) {
        StringBuilder sql = new StringBuilder(SELECT).append(" WHERE d.jenis=? AND d.tanggal >= ? AND d.tanggal <= ?");
        List<Object> p = new ArrayList<>(List.of(jenis, dari.toString(), sampai.toString()));
        if (cari != null && !cari.isBlank()) {
            sql.append(" AND (d.nomor LIKE ? ESCAPE '\\' OR p.nama LIKE ? ESCAPE '\\' OR d.referensi LIKE ? ESCAPE '\\')");
            String k = "%" + BarangService.like(cari.trim()) + "%";
            p.add(k); p.add(k); p.add(k);
        }
        sql.append(" ORDER BY d.tanggal DESC, d.nomor DESC LIMIT 2000");
        return db.daftar(sql.toString(), DokumenService::map, p.toArray());
    }

    public List<Dokumen> terbaru(int n) {
        return db.daftar(SELECT + " ORDER BY d.dibuat_pada DESC, d.id DESC LIMIT ?", DokumenService::map, n);
    }

    public Dokumen byId(long id) { return db.satu(SELECT + " WHERE d.id=?", DokumenService::map, id); }

    private static final String SELECT_ITEM = """
            SELECT i.*, b.kode, b.nama, b.satuan FROM dokumen_item i JOIN barang b ON b.id=i.barang_id
            WHERE i.dokumen_id=? ORDER BY i.id""";

    public List<ItemDokumen> item(long dokId) { return db.daftar(SELECT_ITEM, DokumenService::mapItem, dokId); }

    static Dokumen map(ResultSet rs) throws SQLException {
        long pid = rs.getLong("pihak_id");
        Long pihakId = rs.wasNull() ? null : pid;
        return new Dokumen(rs.getLong("id"), rs.getString("nomor"), rs.getString("jenis"), rs.getString("tanggal"),
                pihakId, rs.getString("pihak"), rs.getString("referensi"), rs.getString("catatan"), rs.getString("oleh"),
                rs.getString("dibuat_pada"), rs.getString("status"), rs.getString("alasan_batal"),
                rs.getInt("n"), rs.getLong("tq"), rs.getLong("tn"));
    }

    static ItemDokumen mapItem(ResultSet rs) throws SQLException {
        int ss = rs.getInt("stok_sistem");
        Integer sistem = rs.wasNull() ? null : ss;
        return new ItemDokumen(rs.getLong("barang_id"), rs.getString("kode"), rs.getString("nama"), rs.getString("satuan"),
                rs.getInt("qty"), sistem, rs.getLong("harga"));
    }
}
