package id.papicode.gudang.model;

/** Supplier (asal barang masuk) atau tujuan (penerima barang keluar: proyek, cabang, pelanggan). */
public record Pihak(long id, String jenis, String nama, String telepon, String alamat, boolean aktif) {
    public static final String SUPPLIER = "SUPPLIER";
    public static final String TUJUAN = "TUJUAN";

    @Override public String toString() { return nama; }
}
