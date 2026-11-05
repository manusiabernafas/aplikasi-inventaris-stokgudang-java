package id.papicode.gudang.db;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Akses SQLite. Satu file database (default: data/stok-gudang.db di folder aplikasi).
 * Setiap operasi membuka koneksi sendiri; transaksi lewat {@link #tx(Kerja)}.
 */
public class Database {

    private final String url;

    public Database(File file) {
        File dir = file.getAbsoluteFile().getParentFile();
        if (dir != null && !dir.exists() && !dir.mkdirs()) {
            throw new IllegalStateException("Folder data tidak bisa dibuat: " + dir);
        }
        this.url = "jdbc:sqlite:" + file.getAbsolutePath();
        jalankanSkema();
    }

    public Connection koneksi() throws SQLException {
        Connection c = DriverManager.getConnection(url);
        try (Statement st = c.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");
            st.execute("PRAGMA busy_timeout = 5000");
        }
        return c;
    }

    @FunctionalInterface
    public interface Kerja<T> { T jalan(Connection c) throws SQLException; }

    @FunctionalInterface
    public interface Baris<T> { T dari(ResultSet rs) throws SQLException; }

    /** Jalankan dalam satu transaksi. BEGIN IMMEDIATE mengunci tulis sejak awal agar cek-lalu-tulis aman. */
    public <T> T tx(Kerja<T> kerja) {
        try (Connection c = koneksi()) {
            try (Statement st = c.createStatement()) { st.execute("BEGIN IMMEDIATE"); }
            try {
                T hasil = kerja.jalan(c);
                try (Statement st = c.createStatement()) { st.execute("COMMIT"); }
                return hasil;
            } catch (RuntimeException | SQLException e) {
                try (Statement st = c.createStatement()) { st.execute("ROLLBACK"); } catch (SQLException ignored) { }
                throw e;
            }
        } catch (SQLException e) {
            throw new DbException(e);
        }
    }

    public <T> List<T> daftar(String sql, Baris<T> map, Object... p) {
        try (Connection c = koneksi()) {
            return daftar(c, sql, map, p);
        } catch (SQLException e) {
            throw new DbException(e);
        }
    }

    public static <T> List<T> daftar(Connection c, String sql, Baris<T> map, Object... p) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            isi(ps, p);
            try (ResultSet rs = ps.executeQuery()) {
                List<T> out = new ArrayList<>();
                while (rs.next()) out.add(map.dari(rs));
                return out;
            }
        }
    }

    public <T> T satu(String sql, Baris<T> map, Object... p) {
        List<T> l = daftar(sql, map, p);
        return l.isEmpty() ? null : l.get(0);
    }

    public static <T> T satu(Connection c, String sql, Baris<T> map, Object... p) throws SQLException {
        List<T> l = daftar(c, sql, map, p);
        return l.isEmpty() ? null : l.get(0);
    }

    public long angka(String sql, Object... p) {
        Long v = satu(sql, rs -> rs.getLong(1), p);
        return v == null ? 0 : v;
    }

    public static long angka(Connection c, String sql, Object... p) throws SQLException {
        Long v = satu(c, sql, rs -> rs.getLong(1), p);
        return v == null ? 0 : v;
    }

    public int ubah(String sql, Object... p) {
        try (Connection c = koneksi()) {
            return ubah(c, sql, p);
        } catch (SQLException e) {
            throw new DbException(e);
        }
    }

    public static int ubah(Connection c, String sql, Object... p) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            isi(ps, p);
            return ps.executeUpdate();
        }
    }

    /** INSERT lalu kembalikan id baris baru. */
    public static long sisip(Connection c, String sql, Object... p) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            isi(ps, p);
            ps.executeUpdate();
        }
        return angka(c, "SELECT last_insert_rowid()");
    }

    private static void isi(PreparedStatement ps, Object... p) throws SQLException {
        for (int i = 0; i < p.length; i++) ps.setObject(i + 1, p[i]);
    }

    /** Salin database yang sedang dipakai ke file lain secara konsisten (VACUUM INTO). */
    public void cadangkan(File tujuan) {
        try (Connection c = koneksi(); PreparedStatement ps = c.prepareStatement("VACUUM INTO ?")) {
            ps.setString(1, tujuan.getAbsolutePath());
            ps.execute();
        } catch (SQLException e) {
            throw new DbException(e);
        }
    }

    private void jalankanSkema() {
        try (Connection c = koneksi(); Statement st = c.createStatement()) {
            st.execute("PRAGMA journal_mode = WAL");
            for (String sql : Skema.SQL) st.execute(sql);
        } catch (SQLException e) {
            throw new DbException(e);
        }
    }

    public static class DbException extends RuntimeException {
        public DbException(SQLException e) { super("Kesalahan database: " + e.getMessage(), e); }
    }
}
