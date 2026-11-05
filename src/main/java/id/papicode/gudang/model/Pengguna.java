package id.papicode.gudang.model;

public record Pengguna(long id, String username, String nama, String peran, boolean aktif) {
    public static final String ADMIN = "ADMIN";
    public static final String STAF = "STAF";

    public boolean admin() { return ADMIN.equals(peran); }
}
