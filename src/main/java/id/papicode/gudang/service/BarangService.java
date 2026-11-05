package id.papicode.gudang.service;

import id.papicode.gudang.db.Database;
import id.papicode.gudang.model.Barang;
import id.papicode.gudang.model.Kategori;
import id.papicode.gudang.model.Pengguna;
import id.papicode.gudang.model.Pihak;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Master data: barang, kategori, supplier & tujuan. Stok TIDAK diubah di sini, hanya lewat dokumen. */
public class BarangService {

    private static final String SELECT = "SELECT b.*, k.nama AS kategori FROM barang b LEFT JOIN kategori k ON k.id = b.kategori_id";

    private final Database db;
    private final Clock jam;

    public BarangService(Database db, Clock jam) {
        this.db = db;
        this.jam = jam;
    }

    public List<Barang> daftar(String cari, Long kategoriId, String lokasi, boolean termasukNonaktif) {
        StringBuilder sql = new StringBuilder(SELECT).append(" WHERE 1=1");
        List<Object> p = new ArrayList<>();
        if (!termasukNonaktif) sql.append(" AND b.aktif=1");
        if (kategoriId != null) { sql.append(" AND b.kategori_id=?"); p.add(kategoriId); }
        if (lokasi != null && !lokasi.isBlank()) { sql.append(" AND b.lokasi LIKE ? ESCAPE '\\'"); p.add(like(lokasi.trim()) + "%"); }
        if (cari != null && !cari.isBlank()) {
            sql.append(" AND (b.nama LIKE ? ESCAPE '\\' OR b.kode LIKE ? ESCAPE '\\')");
            String k = "%" + like(cari.trim()) + "%";
            p.add(k);
            p.add(k);
        }
        sql.append(" ORDER BY b.kode");
        return db.daftar(sql.toString(), BarangService::map, p.toArray());
    }

    public Barang byId(long id) { return db.satu(SELECT + " WHERE b.id=?", BarangService::map, id); }

    public Barang byKode(String kode) {
        if (kode == null || kode.isBlank()) return null;
        return db.satu(SELECT + " WHERE b.kode=? AND b.aktif=1", BarangService::map, kode.trim());
    }

    public List<Barang> menipis() {
        return db.daftar(SELECT + " WHERE b.aktif=1 AND b.stok <= b.stok_min ORDER BY (b.stok * 1.0 / MAX(b.stok_min,1)), b.kode", BarangService::map);
    }

    public List<String> daftarLokasi() {
        return db.daftar("SELECT DISTINCT substr(lokasi,1,1) FROM barang WHERE lokasi IS NOT NULL AND lokasi<>'' ORDER BY 1", rs -> rs.getString(1));
    }

    /** Kode berikutnya untuk barang baru, misal "SMN-004" bila prefix SMN. */
    public String saranKode(String prefix) {
        String pre = prefix == null || prefix.isBlank() ? "BRG" : prefix.trim().toUpperCase();
        String last = db.satu("SELECT kode FROM barang WHERE kode LIKE ? ORDER BY kode DESC LIMIT 1", rs -> rs.getString(1), pre + "-%");
        int n = 1;
        if (last != null) {
            try { n = Integer.parseInt(last.substring(pre.length() + 1)) + 1; } catch (NumberFormatException ignored) { }
        }
        return pre + "-" + String.format("%03d", n);
    }

    /** Tambah/ubah barang (admin). Barang baru boleh punya stok awal; tercatat sebagai mutasi AWAL. */
    public Barang simpan(Pengguna aktor, Barang b, int stokAwal) {
        AuthService.wajibAdmin(aktor);
        String kode = b.kode() == null ? "" : b.kode().trim().toUpperCase();
        String nama = b.nama() == null ? "" : b.nama().trim();
        if (!kode.matches("[A-Z0-9][A-Z0-9\\-./]{1,29}")) throw new Gagal("Kode barang 2-30 karakter: huruf, angka, - . /");
        if (nama.isEmpty() || nama.length() > 120) throw new Gagal("Nama barang wajib diisi (maks 120 karakter).");
        if (b.harga() < 0) throw new Gagal("Harga tidak boleh negatif.");
        if (b.stokMin() < 0) throw new Gagal("Stok minimum tidak boleh negatif.");
        if (stokAwal < 0) throw new Gagal("Stok awal tidak boleh negatif.");
        String satuan = b.satuan() == null || b.satuan().isBlank() ? "pcs" : b.satuan().trim();
        String lokasi = b.lokasi() == null || b.lokasi().isBlank() ? null : b.lokasi().trim().toUpperCase();
        if (lokasi != null && lokasi.length() > 20) throw new Gagal("Lokasi rak maksimal 20 karakter.");
        return db.tx(c -> {
            if (Database.angka(c, "SELECT COUNT(*) FROM barang WHERE kode=? AND id<>?", kode, b.id()) > 0) {
                throw new Gagal("Kode " + kode + " sudah dipakai barang lain.");
            }
            long id = b.id();
            if (id == 0) {
                id = Database.sisip(c, """
                        INSERT INTO barang(kode,nama,kategori_id,satuan,lokasi,harga,stok,stok_min,aktif) VALUES(?,?,?,?,?,?,?,?,?)""",
                        kode, nama, b.kategoriId(), satuan, lokasi, b.harga(), stokAwal, b.stokMin(), b.aktif() ? 1 : 0);
                if (stokAwal > 0) {
                    Database.ubah(c, "INSERT INTO mutasi(barang_id,tanggal,jenis,qty,keterangan) VALUES(?,?,?,?,?)",
                            id, LocalDate.now(jam).toString(), "AWAL", stokAwal, "Saldo awal");
                }
            } else {
                if (Database.ubah(c, "UPDATE barang SET kode=?,nama=?,kategori_id=?,satuan=?,lokasi=?,harga=?,stok_min=?,aktif=? WHERE id=?",
                        kode, nama, b.kategoriId(), satuan, lokasi, b.harga(), b.stokMin(), b.aktif() ? 1 : 0, id) == 0) {
                    throw new Gagal("Barang tidak ditemukan.");
                }
            }
            return Database.satu(c, SELECT + " WHERE b.id=?", BarangService::map, id);
        });
    }

    /** Hapus barang. Bila sudah punya mutasi selain saldo awal, hanya dinonaktifkan. */
    public boolean hapus(Pengguna aktor, long id) {
        AuthService.wajibAdmin(aktor);
        return db.tx(c -> {
            if (Database.angka(c, "SELECT COUNT(*) FROM dokumen_item WHERE barang_id=?", id) > 0) {
                Database.ubah(c, "UPDATE barang SET aktif=0 WHERE id=?", id);
                return false;
            }
            Database.ubah(c, "DELETE FROM mutasi WHERE barang_id=?", id);
            Database.ubah(c, "DELETE FROM barang WHERE id=?", id);
            return true;
        });
    }

    // ---------------- kategori ----------------

    public List<Kategori> kategori() {
        return db.daftar("SELECT id, nama FROM kategori ORDER BY nama", rs -> new Kategori(rs.getLong(1), rs.getString(2)));
    }

    public Kategori tambahKategori(Pengguna aktor, String nama) {
        AuthService.wajibAdmin(aktor);
        String n = nama == null ? "" : nama.trim();
        if (n.isEmpty() || n.length() > 40) throw new Gagal("Nama kategori 1-40 karakter.");
        if (db.angka("SELECT COUNT(*) FROM kategori WHERE nama=?", n) > 0) throw new Gagal("Kategori sudah ada.");
        return db.tx(c -> new Kategori(Database.sisip(c, "INSERT INTO kategori(nama) VALUES(?)", n), n));
    }

    public void hapusKategori(Pengguna aktor, long id) {
        AuthService.wajibAdmin(aktor);
        db.tx(c -> {
            Database.ubah(c, "UPDATE barang SET kategori_id=NULL WHERE kategori_id=?", id);
            Database.ubah(c, "DELETE FROM kategori WHERE id=?", id);
            return null;
        });
    }

    // ---------------- supplier & tujuan ----------------

    public List<Pihak> pihak(String jenis, boolean termasukNonaktif) {
        return db.daftar("SELECT * FROM pihak WHERE (? IS NULL OR jenis=?) AND (? OR aktif=1) ORDER BY jenis, nama",
                BarangService::mapPihak, jenis, jenis, termasukNonaktif ? 1 : 0);
    }

    public Pihak simpanPihak(Pengguna aktor, Pihak p) {
        AuthService.wajibAdmin(aktor);
        if (!Pihak.SUPPLIER.equals(p.jenis()) && !Pihak.TUJUAN.equals(p.jenis())) throw new Gagal("Jenis harus supplier atau tujuan.");
        String nama = p.nama() == null ? "" : p.nama().trim();
        if (nama.isEmpty() || nama.length() > 80) throw new Gagal("Nama wajib diisi (maks 80 karakter).");
        String telp = p.telepon() == null ? "" : p.telepon().trim();
        if (!telp.isEmpty() && !telp.matches("[0-9+\\-() ]{6,20}")) throw new Gagal("Nomor telepon tidak valid.");
        return db.tx(c -> {
            if (Database.angka(c, "SELECT COUNT(*) FROM pihak WHERE jenis=? AND nama=? AND id<>?", p.jenis(), nama, p.id()) > 0) {
                throw new Gagal("Nama \"" + nama + "\" sudah terdaftar.");
            }
            long id = p.id();
            if (id == 0) {
                id = Database.sisip(c, "INSERT INTO pihak(jenis,nama,telepon,alamat,aktif) VALUES(?,?,?,?,?)",
                        p.jenis(), nama, telp, p.alamat() == null ? "" : p.alamat().trim(), p.aktif() ? 1 : 0);
            } else {
                Database.ubah(c, "UPDATE pihak SET nama=?,telepon=?,alamat=?,aktif=? WHERE id=?",
                        nama, telp, p.alamat() == null ? "" : p.alamat().trim(), p.aktif() ? 1 : 0, id);
            }
            return Database.satu(c, "SELECT * FROM pihak WHERE id=?", BarangService::mapPihak, id);
        });
    }

    static String like(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    static Barang map(ResultSet rs) throws SQLException {
        long kat = rs.getLong("kategori_id");
        Long kategoriId = rs.wasNull() ? null : kat;
        return new Barang(rs.getLong("id"), rs.getString("kode"), rs.getString("nama"), kategoriId, rs.getString("kategori"),
                rs.getString("satuan"), rs.getString("lokasi"), rs.getLong("harga"), rs.getInt("stok"), rs.getInt("stok_min"),
                rs.getInt("aktif") == 1);
    }

    static Pihak mapPihak(ResultSet rs) throws SQLException {
        return new Pihak(rs.getLong("id"), rs.getString("jenis"), rs.getString("nama"), rs.getString("telepon"),
                rs.getString("alamat"), rs.getInt("aktif") == 1);
    }
}
