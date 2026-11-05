package id.papicode.gudang.service;

import id.papicode.gudang.db.Database;
import id.papicode.gudang.model.Pengguna;

import java.util.LinkedHashMap;
import java.util.Map;

/** Identitas perusahaan & gudang untuk kop dokumen cetak. Disimpan di tabel pengaturan (kunci-nilai). */
public class PengaturanService {

    public static final Map<String, String> BAWAAN = new LinkedHashMap<>();

    static {
        BAWAAN.put("nama_perusahaan", "CV Sinar Bangun Abadi");
        BAWAAN.put("nama_gudang", "Gudang Utama Waru");
        BAWAAN.put("alamat", "Jl. Raya Waru No. 88, Sidoarjo");
        BAWAAN.put("kota", "Sidoarjo");
        BAWAAN.put("telepon", "031-8531-220");
        BAWAAN.put("penanggung_jawab", "Hendra Wijaya");
    }

    private final Database db;

    public PengaturanService(Database db) { this.db = db; }

    public String get(String kunci) {
        String v = db.satu("SELECT nilai FROM pengaturan WHERE kunci=?", rs -> rs.getString(1), kunci);
        return v != null ? v : BAWAAN.getOrDefault(kunci, "");
    }

    public void simpan(Pengguna aktor, Map<String, String> nilai) {
        AuthService.wajibAdmin(aktor);
        for (Map.Entry<String, String> e : nilai.entrySet()) {
            if (!BAWAAN.containsKey(e.getKey())) throw new Gagal("Pengaturan tidak dikenal: " + e.getKey());
            String v = e.getValue() == null ? "" : e.getValue().trim();
            if (v.length() > 120) throw new Gagal("Isian terlalu panjang (maks 120 karakter).");
            if (e.getKey().equals("nama_perusahaan") && v.isEmpty()) throw new Gagal("Nama perusahaan wajib diisi.");
        }
        db.tx(c -> {
            for (Map.Entry<String, String> e : nilai.entrySet()) {
                Database.ubah(c, "INSERT INTO pengaturan(kunci,nilai) VALUES(?,?) ON CONFLICT(kunci) DO UPDATE SET nilai=excluded.nilai",
                        e.getKey(), e.getValue() == null ? "" : e.getValue().trim());
            }
            return null;
        });
    }
}
