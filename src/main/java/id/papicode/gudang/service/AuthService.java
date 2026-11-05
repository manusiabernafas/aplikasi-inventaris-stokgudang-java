package id.papicode.gudang.service;

import id.papicode.gudang.db.Database;
import id.papicode.gudang.model.Pengguna;
import id.papicode.gudang.util.Password;
import id.papicode.gudang.util.Waktu;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/** Login, kunci akun setelah salah berulang, kelola pengguna. */
public class AuthService {

    public static final int MAKS_GAGAL = 5;
    public static final long LAMA_KUNCI_MS = 5 * 60_000L;

    private final Database db;
    private final Clock jam;

    public AuthService(Database db, Clock jam) {
        this.db = db;
        this.jam = jam;
    }

    /** Membuat akun admin awal (admin / admin123) bila belum ada pengguna sama sekali. */
    public boolean pastikanAdminAwal() {
        if (db.angka("SELECT COUNT(*) FROM pengguna") > 0) return false;
        db.ubah("INSERT INTO pengguna(username,nama,peran,hash,dibuat) VALUES(?,?,?,?,?)",
                "admin", "Kepala Gudang", Pengguna.ADMIN, Password.hash("admin123"), sekarang());
        return true;
    }

    public Pengguna login(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isEmpty()) {
            throw new Gagal("Isi username dan password.");
        }
        String u = username.trim();
        // Hasil: pengguna bila berhasil; null bila password salah (penghitung gagal tetap di-commit)
        Pengguna hasil = db.tx(c -> {
            Object[] r = Database.satu(c, "SELECT id,hash,aktif,gagal,kunci_sampai FROM pengguna WHERE username=?",
                    rs -> new Object[]{rs.getLong(1), rs.getString(2), rs.getInt(3), rs.getInt(4), rs.getLong(5)}, u);
            long now = jam.millis();
            if (r == null) {
                Password.cocok(password, DUMMY); // samakan waktu respons agar username tidak bisa ditebak
                return null;
            }
            long id = (long) r[0];
            long kunci = (long) r[4];
            if (kunci > now) {
                long menit = Math.max(1, (kunci - now + 59_999) / 60_000);
                throw new Gagal("Terlalu banyak percobaan. Coba lagi " + menit + " menit lagi.");
            }
            if (!Password.cocok(password, (String) r[1])) {
                int gagal = (int) r[3] + 1;
                if (gagal >= MAKS_GAGAL) {
                    Database.ubah(c, "UPDATE pengguna SET gagal=0, kunci_sampai=? WHERE id=?", now + LAMA_KUNCI_MS, id);
                } else {
                    Database.ubah(c, "UPDATE pengguna SET gagal=? WHERE id=?", gagal, id);
                }
                return null;
            }
            if ((int) r[2] != 1) throw new Gagal("Akun ini dinonaktifkan. Hubungi kepala gudang.");
            Database.ubah(c, "UPDATE pengguna SET gagal=0, kunci_sampai=0 WHERE id=?", id);
            return Database.satu(c, "SELECT * FROM pengguna WHERE id=?", AuthService::map, id);
        });
        if (hasil != null) return hasil;
        if (db.angka("SELECT kunci_sampai FROM pengguna WHERE username=?", u) > jam.millis()) {
            throw new Gagal("Terlalu banyak percobaan. Akun dikunci 5 menit.");
        }
        throw new Gagal("Username atau password salah.");
    }

    private static final String DUMMY = Password.hash("dummy-untuk-waktu-konstan");

    public List<Pengguna> daftar() {
        return db.daftar("SELECT * FROM pengguna ORDER BY peran, nama", AuthService::map);
    }

    public Pengguna tambah(Pengguna aktor, String username, String nama, String peran, String password) {
        wajibAdmin(aktor);
        String u = username == null ? "" : username.trim();
        if (!u.matches("[A-Za-z0-9_.]{3,30}")) throw new Gagal("Username 3-30 karakter: huruf, angka, titik, garis bawah.");
        if (nama == null || nama.isBlank()) throw new Gagal("Nama wajib diisi.");
        if (!Pengguna.ADMIN.equals(peran) && !Pengguna.STAF.equals(peran)) throw new Gagal("Peran tidak dikenal.");
        cekPassword(password);
        if (db.angka("SELECT COUNT(*) FROM pengguna WHERE username=?", u) > 0) throw new Gagal("Username sudah dipakai.");
        return db.tx(c -> {
            long id = Database.sisip(c, "INSERT INTO pengguna(username,nama,peran,hash,dibuat) VALUES(?,?,?,?,?)",
                    u, nama.trim(), peran, Password.hash(password), sekarang());
            return Database.satu(c, "SELECT * FROM pengguna WHERE id=?", AuthService::map, id);
        });
    }

    public void resetPassword(Pengguna aktor, long id, String baru) {
        wajibAdmin(aktor);
        cekPassword(baru);
        db.ubah("UPDATE pengguna SET hash=?, gagal=0, kunci_sampai=0 WHERE id=?", Password.hash(baru), id);
    }

    public void gantiPassword(Pengguna aktor, String lama, String baru) {
        String hash = db.satu("SELECT hash FROM pengguna WHERE id=?", rs -> rs.getString(1), aktor.id());
        if (!Password.cocok(lama, hash)) throw new Gagal("Password lama salah.");
        cekPassword(baru);
        if (baru.equals(lama)) throw new Gagal("Password baru harus berbeda dari yang lama.");
        db.ubah("UPDATE pengguna SET hash=? WHERE id=?", Password.hash(baru), aktor.id());
    }

    public void setAktif(Pengguna aktor, long id, boolean aktif) {
        wajibAdmin(aktor);
        if (id == aktor.id() && !aktif) throw new Gagal("Tidak bisa menonaktifkan akun sendiri.");
        db.tx(c -> {
            String peran = Database.satu(c, "SELECT peran FROM pengguna WHERE id=?", rs -> rs.getString(1), id);
            if (peran == null) throw new Gagal("Pengguna tidak ditemukan.");
            if (!aktif && Pengguna.ADMIN.equals(peran)
                    && Database.angka(c, "SELECT COUNT(*) FROM pengguna WHERE peran='ADMIN' AND aktif=1 AND id<>?", id) == 0) {
                throw new Gagal("Minimal harus ada satu admin aktif.");
            }
            Database.ubah(c, "UPDATE pengguna SET aktif=? WHERE id=?", aktif ? 1 : 0, id);
            return null;
        });
    }

    /** Dipakai sebelum aksi penting: pastikan akun masih ada dan aktif (misal dinonaktifkan admin lain). */
    public Pengguna segarkan(Pengguna p) {
        Pengguna x = db.satu("SELECT * FROM pengguna WHERE id=?", AuthService::map, p.id());
        if (x == null || !x.aktif()) throw new Gagal("Akun sudah tidak aktif. Silakan login ulang.");
        return x;
    }

    public static void wajibAdmin(Pengguna p) {
        if (p == null || !p.admin()) throw new Gagal("Hanya admin yang boleh melakukan ini.");
    }

    private static void cekPassword(String pw) {
        if (pw == null || pw.length() < 6) throw new Gagal("Password minimal 6 karakter.");
        if (pw.length() > 100) throw new Gagal("Password terlalu panjang.");
    }

    private String sekarang() { return Waktu.db(LocalDateTime.now(jam)); }

    static Pengguna map(ResultSet rs) throws SQLException {
        return new Pengguna(rs.getLong("id"), rs.getString("username"), rs.getString("nama"),
                rs.getString("peran"), rs.getInt("aktif") == 1);
    }
}
