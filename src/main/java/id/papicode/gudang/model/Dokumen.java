package id.papicode.gudang.model;

public record Dokumen(long id, String nomor, String jenis, String tanggal, Long pihakId, String pihak,
                      String referensi, String catatan, String dibuatOleh, String dibuatPada,
                      String status, String alasanBatal, int jumlahBaris, long totalQty, long totalNilai) {
    public static final String MASUK = "MASUK", KELUAR = "KELUAR", OPNAME = "OPNAME";

    public boolean batal() { return "BATAL".equals(status); }
}
