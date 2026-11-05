package id.papicode.gudang.model;

/** Baris dokumen. qty selalu positif untuk masuk/keluar; untuk opname qty = stok fisik hasil hitung. */
public record ItemDokumen(long barangId, String kode, String nama, String satuan, int qty, Integer stokSistem, long harga) {
    public int selisih() { return stokSistem == null ? 0 : qty - stokSistem; }
}
