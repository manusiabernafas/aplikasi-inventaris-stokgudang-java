package id.papicode.gudang.model;

/** Input baris dari form: id barang dan jumlah (untuk opname: stok fisik). */
public record BarisInput(long barangId, int qty) { }
