package id.papicode.gudang.service;

import id.papicode.gudang.db.Database;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Laporan persediaan. Dokumen yang dibatalkan beserta mutasi pembatalannya diabaikan bersama-sama
 * (efek bersihnya nol), sehingga angka masuk/keluar hanya menghitung dokumen yang sah.
 */
public class LaporanService {

    /** Filter mutasi yang sah: bukan baris BATAL dan dokumennya (jika ada) tidak dibatalkan. */
    private static final String SAH = " m.jenis <> 'BATAL' AND (m.dokumen_id IS NULL OR (SELECT status FROM dokumen d WHERE d.id=m.dokumen_id)='AKTIF') ";

    private final Database db;

    public LaporanService(Database db) { this.db = db; }

    public record Dasbor(long sku, long nilai, long totalUnit, long menipis, long habis, long masukHariIni, long keluarHariIni) { }

    public Dasbor dasbor(LocalDate hariIni) {
        return db.satu("""
                SELECT COUNT(*), COALESCE(SUM(harga*stok),0), COALESCE(SUM(stok),0),
                  SUM(CASE WHEN stok <= stok_min THEN 1 ELSE 0 END), SUM(CASE WHEN stok = 0 THEN 1 ELSE 0 END),
                  (SELECT COUNT(*) FROM dokumen WHERE jenis='MASUK' AND status='AKTIF' AND tanggal=?),
                  (SELECT COUNT(*) FROM dokumen WHERE jenis='KELUAR' AND status='AKTIF' AND tanggal=?)
                FROM barang WHERE aktif=1""",
                rs -> new Dasbor(rs.getLong(1), rs.getLong(2), rs.getLong(3), rs.getLong(4), rs.getLong(5), rs.getLong(6), rs.getLong(7)),
                hariIni.toString(), hariIni.toString());
    }

    /** Jumlah unit masuk & keluar per hari: nilai[0] = masuk, nilai[1] = keluar. */
    public Map<LocalDate, long[]> harian(LocalDate dari, LocalDate sampai) {
        Map<LocalDate, long[]> out = new LinkedHashMap<>();
        for (LocalDate d = dari; !d.isAfter(sampai); d = d.plusDays(1)) out.put(d, new long[2]);
        db.daftar("SELECT m.tanggal, SUM(CASE WHEN m.jenis='MASUK' THEN m.qty ELSE 0 END), SUM(CASE WHEN m.jenis='KELUAR' THEN -m.qty ELSE 0 END) "
                        + "FROM mutasi m WHERE " + SAH + " AND m.tanggal >= ? AND m.tanggal <= ? GROUP BY m.tanggal",
                rs -> {
                    long[] v = out.get(LocalDate.parse(rs.getString(1)));
                    if (v != null) { v[0] = rs.getLong(2); v[1] = rs.getLong(3); }
                    return null;
                }, dari.toString(), sampai.toString());
        return out;
    }

    public record Peringkat(String kode, String nama, String satuan, long qty, long nilai) { }

    /** Barang paling sering keluar (fast moving) dalam periode. */
    public List<Peringkat> palingKeluar(LocalDate dari, LocalDate sampai, int batas) {
        return db.daftar("SELECT b.kode, b.nama, b.satuan, SUM(-m.qty), SUM(-m.qty*b.harga) FROM mutasi m JOIN barang b ON b.id=m.barang_id "
                        + "WHERE " + SAH + " AND m.jenis='KELUAR' AND m.tanggal >= ? AND m.tanggal <= ? GROUP BY b.id ORDER BY 4 DESC LIMIT ?",
                rs -> new Peringkat(rs.getString(1), rs.getString(2), rs.getString(3), rs.getLong(4), rs.getLong(5)),
                dari.toString(), sampai.toString(), batas);
    }

    public record TakBergerak(String kode, String nama, String lokasi, int stok, String satuan, long nilai, String terakhirKeluar) { }

    /** Barang berstok yang tidak keluar sama sekali sejak tanggal tertentu (dead stock). */
    public List<TakBergerak> takBergerak(LocalDate sejak) {
        return db.daftar("""
                SELECT b.kode, b.nama, b.lokasi, b.stok, b.satuan, b.stok*b.harga,
                  (SELECT MAX(m.tanggal) FROM mutasi m WHERE m.barang_id=b.id AND m.jenis='KELUAR') AS akhir
                FROM barang b WHERE b.aktif=1 AND b.stok > 0
                  AND NOT EXISTS (SELECT 1 FROM mutasi m WHERE m.barang_id=b.id AND m.jenis='KELUAR' AND m.tanggal >= ?
                                  AND (SELECT status FROM dokumen d WHERE d.id=m.dokumen_id)='AKTIF')
                ORDER BY 6 DESC""",
                rs -> new TakBergerak(rs.getString(1), rs.getString(2), rs.getString(3), rs.getInt(4), rs.getString(5), rs.getLong(6), rs.getString(7)),
                sejak.toString());
    }

    public record BarisMutasi(long barangId, String kode, String nama, String kategori, String satuan,
                              long awal, long masuk, long keluar, long koreksi, long harga) {
        public long akhir() { return awal + masuk - keluar + koreksi; }
        public long nilaiAkhir() { return akhir() * harga; }
    }

    /** Laporan mutasi persediaan: saldo awal, masuk, keluar, koreksi, saldo akhir per barang. */
    public List<BarisMutasi> mutasi(LocalDate dari, LocalDate sampai, Long kategoriId) {
        return db.daftar("""
                SELECT b.id, b.kode, b.nama, k.nama, b.satuan,
                  COALESCE(SUM(CASE WHEN m.tanggal < ? THEN m.qty END),0),
                  COALESCE(SUM(CASE WHEN m.tanggal >= ? AND m.tanggal <= ? AND m.jenis IN ('MASUK') THEN m.qty END),0),
                  COALESCE(SUM(CASE WHEN m.tanggal >= ? AND m.tanggal <= ? AND m.jenis='KELUAR' THEN -m.qty END),0),
                  COALESCE(SUM(CASE WHEN m.tanggal >= ? AND m.tanggal <= ? AND m.jenis IN ('KOREKSI','AWAL') THEN m.qty END),0),
                  b.harga
                FROM barang b LEFT JOIN kategori k ON k.id=b.kategori_id
                LEFT JOIN mutasi m ON m.barang_id=b.id AND m.tanggal <= ? AND """ + SAH + """

                WHERE (b.aktif=1 OR EXISTS (SELECT 1 FROM mutasi x WHERE x.barang_id=b.id AND x.tanggal >= ?))
                  AND (? IS NULL OR b.kategori_id = ?)
                GROUP BY b.id ORDER BY b.kode""",
                rs -> new BarisMutasi(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5),
                        rs.getLong(6), rs.getLong(7), rs.getLong(8), rs.getLong(9), rs.getLong(10)),
                dari.toString(), dari.toString(), sampai.toString(), dari.toString(), sampai.toString(),
                dari.toString(), sampai.toString(), sampai.toString(), dari.toString(), kategoriId, kategoriId);
    }

    public record BarisKartu(String tanggal, String nomor, String jenis, String keterangan, long masuk, long keluar, long saldo) { }

    /** Kartu stok satu barang: baris pertama adalah saldo awal periode. */
    public List<BarisKartu> kartuStok(long barangId, LocalDate dari, LocalDate sampai) {
        long awal = db.angka("SELECT COALESCE(SUM(m.qty),0) FROM mutasi m WHERE m.barang_id=? AND m.tanggal < ? AND " + SAH, barangId, dari.toString());
        List<BarisKartu> out = new ArrayList<>();
        out.add(new BarisKartu(dari.toString(), "", "SALDO", "Saldo awal", 0, 0, awal));
        long[] saldo = {awal};
        db.daftar("""
                SELECT m.tanggal, d.nomor, m.jenis, COALESCE(p.nama, m.keterangan), m.qty FROM mutasi m
                LEFT JOIN dokumen d ON d.id=m.dokumen_id LEFT JOIN pihak p ON p.id=d.pihak_id
                WHERE m.barang_id=? AND m.tanggal >= ? AND m.tanggal <= ? AND """ + SAH + " ORDER BY m.tanggal, m.id",
                rs -> {
                    long q = rs.getLong(5);
                    saldo[0] += q;
                    out.add(new BarisKartu(rs.getString(1), rs.getString(2) == null ? "" : rs.getString(2), rs.getString(3),
                            rs.getString(4), q > 0 ? q : 0, q < 0 ? -q : 0, saldo[0]));
                    return null;
                }, barangId, dari.toString(), sampai.toString());
        return out;
    }
}
