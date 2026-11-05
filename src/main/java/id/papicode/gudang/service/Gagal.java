package id.papicode.gudang.service;

/** Kesalahan yang pesannya aman dan layak ditampilkan ke pengguna. */
public class Gagal extends RuntimeException {
    public Gagal(String pesan) { super(pesan); }
}
