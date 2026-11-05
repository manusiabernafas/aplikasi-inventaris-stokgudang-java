package id.papicode.gudang.model;

public record Kategori(long id, String nama) {
    @Override public String toString() { return nama; }
}
