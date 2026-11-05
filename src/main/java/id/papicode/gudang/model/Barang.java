package id.papicode.gudang.model;

public record Barang(long id, String kode, String nama, Long kategoriId, String kategori, String satuan,
                     String lokasi, long harga, int stok, int stokMin, boolean aktif) {

    public boolean menipis() { return aktif && stok <= stokMin; }

    public long nilai() { return harga * Math.max(0, stok); }

    @Override public String toString() { return kode + "  " + nama; }
}
