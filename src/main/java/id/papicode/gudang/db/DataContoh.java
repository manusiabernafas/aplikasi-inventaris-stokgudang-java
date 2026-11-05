package id.papicode.gudang.db;

import id.papicode.gudang.model.BarisInput;
import id.papicode.gudang.model.Pengguna;
import id.papicode.gudang.service.Gagal;
import id.papicode.gudang.service.DokumenService;
import id.papicode.gudang.util.Password;
import id.papicode.gudang.util.Waktu;

import java.time.*;
import java.util.*;

/**
 * Data contoh gudang bahan bangunan: 45 barang, 7 supplier, 7 tujuan, 3 pengguna, serta dokumen
 * barang masuk/keluar 60 hari terakhir dan satu stok opname. Tanggal mengikuti hari ini.
 */
public final class DataContoh {
    private DataContoh() { }

    // kode | nama | kategori | satuan | lokasi | harga | stok min | bobot keluar
    private static final String[] BARANG = {
        "SMN-001|Semen Gresik 40 kg|Semen & Mortar|sak|A-01-01|62000|60|9",
        "SMN-002|Semen Tiga Roda 50 kg|Semen & Mortar|sak|A-01-02|68000|40|6",
        "SMN-003|Semen Putih Tiga Roda 40 kg|Semen & Mortar|sak|A-01-03|98000|8|2",
        "SMN-004|Mortar Instan MU-380 40 kg|Semen & Mortar|sak|A-02-01|112000|10|2",
        "BSI-001|Besi Beton 8 mm SNI|Besi & Kawat|batang|B-01-01|52000|100|7",
        "BSI-002|Besi Beton 10 mm SNI|Besi & Kawat|batang|B-01-02|81000|80|6",
        "BSI-003|Besi Beton 12 mm SNI|Besi & Kawat|batang|B-01-03|117000|40|3",
        "BSI-004|Kawat Bendrat 1 kg|Besi & Kawat|roll|B-02-01|23000|20|4",
        "BSI-005|Wiremesh M6 2,1 x 5,4 m|Besi & Kawat|lembar|B-02-02|285000|8|1",
        "CAT-001|Cat Tembok Avitex Putih 5 kg|Cat & Finishing|pail|C-01-01|145000|12|4",
        "CAT-002|Cat Dulux Catylac Putih 5 kg|Cat & Finishing|pail|C-01-02|132000|10|3",
        "CAT-003|Cat Kayu Besi Avian 1 kg|Cat & Finishing|kaleng|C-01-03|72000|10|2",
        "CAT-004|Thinner A Special 1 L|Cat & Finishing|kaleng|C-02-01|28000|20|3",
        "CAT-005|Plamir Tembok Avian 1 kg|Cat & Finishing|kaleng|C-02-02|38000|10|2",
        "CAT-006|Kuas Eterna 3 inch|Cat & Finishing|pcs|C-03-01|18000|24|3",
        "CAT-007|Roll Cat 9 inch|Cat & Finishing|pcs|C-03-02|32000|12|2",
        "PPA-001|Pipa PVC Rucika 1/2\" AW 4 m|Pipa & Sanitasi|batang|D-01-01|38000|40|5",
        "PPA-002|Pipa PVC Rucika 3/4\" AW 4 m|Pipa & Sanitasi|batang|D-01-02|49000|30|4",
        "PPA-003|Pipa PVC Rucika 3\" D 4 m|Pipa & Sanitasi|batang|D-01-03|132000|10|2",
        "PPA-004|Knee PVC 1/2\"|Pipa & Sanitasi|pcs|D-02-01|3000|100|6",
        "PPA-005|Lem PVC Rucika 100 g|Pipa & Sanitasi|tube|D-02-02|16000|24|4",
        "PPA-006|Kran Air Onda 1/2\"|Pipa & Sanitasi|pcs|D-03-01|45000|12|3",
        "PPA-007|Kloset Jongkok Toto CE6|Pipa & Sanitasi|unit|D-04-01|285000|4|1",
        "LST-001|Kabel Eterna NYM 2x1,5 50 m|Listrik|roll|E-01-01|465000|5|2",
        "LST-002|Kabel Eterna NYA 1,5 50 m|Listrik|roll|E-01-02|245000|5|2",
        "LST-003|Saklar Broco Engkel|Listrik|pcs|E-02-01|21000|20|4",
        "LST-004|Stop Kontak Broco|Listrik|pcs|E-02-02|24000|20|4",
        "LST-005|Lampu Philips LED 12 W|Listrik|pcs|E-03-01|52000|20|4",
        "LST-006|MCB Schneider 10 A|Listrik|pcs|E-03-02|68000|10|2",
        "LST-007|Fitting Lampu Plafon|Listrik|pcs|E-02-03|9000|30|3",
        "KYU-001|Triplek 9 mm 122x244|Kayu & Atap|lembar|F-01-01|128000|15|3",
        "KYU-002|Kayu Kaso 5/7 4 m|Kayu & Atap|batang|F-01-02|42000|40|4",
        "KYU-003|Genteng Metal Pasir|Kayu & Atap|lembar|F-02-01|38000|50|3",
        "KYU-004|Baja Ringan C75 6 m|Kayu & Atap|batang|F-02-02|98000|30|3",
        "KYU-005|Gypsum Jayaboard 9 mm|Kayu & Atap|lembar|F-03-01|72000|20|2",
        "ALT-001|Paku Kayu 5 cm|Paku & Alat|kg|G-01-01|21000|25|5",
        "ALT-002|Paku Kayu 7 cm|Paku & Alat|kg|G-01-02|21000|20|3",
        "ALT-003|Sekrup Gypsum 1 inch|Paku & Alat|box|G-01-03|35000|10|2",
        "ALT-004|Meteran Tajima 5 m|Paku & Alat|pcs|G-02-01|65000|5|1",
        "ALT-005|Sarung Tangan Kain|Paku & Alat|lusin|G-02-02|36000|10|3",
        "ALT-006|Ember Cor Hitam|Paku & Alat|pcs|G-02-03|12000|20|2",
        "ALT-007|Gerobak Sorong Artco|Paku & Alat|unit|G-03-01|650000|2|1",
        "KRM-001|Keramik Roman 40x40 Putih|Keramik & Lantai|dus|H-01-01|78000|30|4",
        "KRM-002|Granit Indogress 60x60 Krem|Keramik & Lantai|dus|H-01-02|185000|15|2",
        "KRM-003|Semen Nat AM 1 kg|Keramik & Lantai|bks|H-02-01|15000|20|3",
    };

    private static final String[][] SUPPLIER = {
        {"PT Semen Indonesia Distributor Jatim", "031-3981-732", "Jl. Veteran 129, Gresik", "SMN"},
        {"CV Baja Prima Surabaya", "031-7491-005", "Jl. Margomulyo 44, Surabaya", "BSI"},
        {"PT Avia Avian Distribusi", "031-8968-123", "Jl. Raya Ngoro, Sidoarjo", "CAT"},
        {"CV Rucika Mitra Sanitasi", "0812-3300-4411", "Jl. Kenjeran 210, Surabaya", "PPA"},
        {"UD Listrik Terang Jaya", "0813-3170-2286", "Jl. Kapasan 61, Surabaya", "LST"},
        {"PT Kayu Lestari Gresik", "031-3951-778", "Jl. Raya Duduk Sampeyan, Gresik", "KYU"},
        {"UD Sumber Paku Makmur", "0821-4422-9010", "Pasar Turi Blok C-12, Surabaya", "ALT KRM"},
    };

    private static final String[][] TUJUAN = {
        {"Proyek Perumahan Griya Asri Candi", "0812-3456-7781", "Jl. Raya Candi, Sidoarjo"},
        {"Proyek Ruko Jl. Ahmad Yani", "0813-5512-0098", "Jl. A. Yani 301, Surabaya"},
        {"Toko Cabang Krian", "031-8971-334", "Jl. Raya Krian 17, Sidoarjo"},
        {"Toko Cabang Gedangan", "031-8913-552", "Jl. Raya Gedangan 5, Sidoarjo"},
        {"Renovasi SDN Waru 2", "0857-3011-4520", "Jl. Brigjen Katamso, Waru"},
        {"Pak Joko Susilo (Kontraktor)", "0811-3322-6600", "Perum Pondok Jati AN-7, Sidoarjo"},
        {"Pelanggan Umum", "", ""},
    };

    public static boolean kosong(Database db) {
        return db.angka("SELECT COUNT(*) FROM barang") == 0 && db.angka("SELECT COUNT(*) FROM dokumen") == 0;
    }

    public static void isi(Database db, Clock jam) {
        if (!kosong(db)) throw new IllegalStateException("Data contoh hanya bisa diisi pada database kosong.");
        Random r = new Random(17081945L);
        LocalDate hariIni = LocalDate.now(jam);
        LocalDate mulai = hariIni.minusDays(60);
        ZoneId zona = jam.getZone();

        // master data
        Map<String, Long> kat = new LinkedHashMap<>();
        List<long[]> barang = new ArrayList<>();         // id, bobot, stokMin
        Map<String, List<Integer>> perPrefix = new HashMap<>();
        long[] pengguna = new long[3];
        List<Long> supplier = new ArrayList<>(), tujuan = new ArrayList<>();
        List<String> prefixSupplier = new ArrayList<>();
        db.tx(c -> {
            String dibuat = Waktu.db(mulai.minusDays(1).atTime(8, 0));
            Long admin = Database.satu(c, "SELECT id FROM pengguna WHERE username='admin'", rs -> rs.getLong(1));
            if (admin == null) {
                admin = Database.sisip(c, "INSERT INTO pengguna(username,nama,peran,hash,dibuat) VALUES(?,?,?,?,?)",
                        "admin", "Hendra Wijaya", Pengguna.ADMIN, Password.hash("admin123"), dibuat);
            } else {
                Database.ubah(c, "UPDATE pengguna SET nama='Hendra Wijaya' WHERE id=? AND nama='Kepala Gudang'", admin);
            }
            pengguna[0] = admin;
            pengguna[1] = staf(c, "budi", "Budi Santoso", dibuat);
            pengguna[2] = staf(c, "sari", "Sari Rahmawati", dibuat);
            for (int i = 0; i < BARANG.length; i++) {
                String[] f = BARANG[i].split("\\|");
                long kid = kat.containsKey(f[2]) ? kat.get(f[2]) : Database.sisip(c, "INSERT INTO kategori(nama) VALUES(?)", f[2]);
                kat.put(f[2], kid);
                int min = Integer.parseInt(f[6]), bobot = Integer.parseInt(f[7]);
                int awal = min * 3 + bobot * 25;
                long id = Database.sisip(c, "INSERT INTO barang(kode,nama,kategori_id,satuan,lokasi,harga,stok,stok_min) VALUES(?,?,?,?,?,?,?,?)",
                        f[0], f[1], kid, f[3], f[4], Long.parseLong(f[5]), awal, min);
                Database.ubah(c, "INSERT INTO mutasi(barang_id,tanggal,jenis,qty,keterangan) VALUES(?,?,?,?,?)", id, mulai.toString(), "AWAL", awal, "Saldo awal");
                barang.add(new long[]{id, bobot, min});
                perPrefix.computeIfAbsent(f[0].substring(0, 3), x -> new ArrayList<>()).add(i);
            }
            for (String[] s : SUPPLIER) {
                supplier.add(Database.sisip(c, "INSERT INTO pihak(jenis,nama,telepon,alamat) VALUES('SUPPLIER',?,?,?)", s[0], s[1], s[2]));
                prefixSupplier.add(s[3]);
            }
            for (String[] t : TUJUAN) tujuan.add(Database.sisip(c, "INSERT INTO pihak(jenis,nama,telepon,alamat) VALUES('TUJUAN',?,?,?)", t[0], t[1], t[2]));
            return null;
        });
        int totalBobot = barang.stream().mapToInt(b -> (int) b[1]).sum();
        Pengguna[] akun = {
            new Pengguna(pengguna[0], "admin", "Hendra Wijaya", Pengguna.ADMIN, true),
            new Pengguna(pengguna[1], "budi", "Budi Santoso", Pengguna.STAF, true),
            new Pengguna(pengguna[2], "sari", "Sari Rahmawati", Pengguna.STAF, true),
        };
        String[] catatanKeluar = {"Kirim pakai truk engkel", "Diambil sendiri oleh mandor", "Kirim pagi sebelum jam 9", "", "", "Bongkar di lokasi blok C", ""};

        for (LocalDate d = mulai.plusDays(1); !d.isAfter(hariIni); d = d.plusDays(1)) {
            if (d.getDayOfWeek() == DayOfWeek.SUNDAY) continue;
            boolean hariIniSaja = d.equals(hariIni);
            // opname 20 hari lalu
            if (d.equals(hariIni.minusDays(20))) {
                List<BarisInput> op = new ArrayList<>();
                for (String pre : List.of("SMN", "BSI", "ALT")) {
                    for (int idx : perPrefix.get(pre)) {
                        long id = barang.get(idx)[0];
                        int stok = (int) db.angka("SELECT stok FROM barang WHERE id=?", id);
                        int selisih = r.nextDouble() < 0.3 ? -(1 + r.nextInt(3)) : 0;
                        op.add(new BarisInput(id, Math.max(0, stok + selisih)));
                    }
                }
                layanan(db, d, 16, 30, zona).opname(akun[0], d, "Opname rutin rak A, B, G (semen, besi, paku)", op);
            }
            // barang masuk: 1-2 kiriman supplier, biasanya pagi
            int nMasuk = hariIniSaja ? 1 : (r.nextDouble() < 0.75 ? 1 + r.nextInt(2) : 0);
            for (int m = 0; m < nMasuk; m++) {
                int si = r.nextInt(supplier.size());
                List<Integer> kandidat = new ArrayList<>();
                for (String pre : prefixSupplier.get(si).split(" ")) kandidat.addAll(perPrefix.get(pre));
                Collections.shuffle(kandidat, r);
                List<BarisInput> baris = new ArrayList<>();
                for (int idx : kandidat.subList(0, Math.min(kandidat.size(), 2 + r.nextInt(4)))) {
                    long[] b = barang.get(idx);
                    int stok = (int) db.angka("SELECT stok FROM barang WHERE id=?", b[0]);
                    int target = (int) (b[2] * 3 + b[1] * 20);
                    int qty = Math.max((int) b[2], target - stok);
                    qty = (int) Math.round(qty / 5.0) * 5;
                    if (qty > 0) baris.add(new BarisInput(b[0], qty));
                }
                if (baris.isEmpty()) continue;
                Pengguna oleh = akun[1 + r.nextInt(2)];
                String sj = "SJ/" + d.getYear() % 100 + String.format("%02d", d.getMonthValue()) + "/" + (1000 + r.nextInt(8999));
                layanan(db, d, 8 + m, r.nextInt(60), zona).barangMasuk(oleh, d, supplier.get(si), sj, "", baris);
            }
            // barang keluar: 3-7 per hari
            int nKeluar = hariIniSaja ? 3 : 3 + r.nextInt(5) - (d.getDayOfWeek() == DayOfWeek.SATURDAY ? 2 : 0);
            for (int k = 0; k < nKeluar; k++) {
                int ti = r.nextInt(tujuan.size());
                Set<Integer> pilih = new LinkedHashSet<>();
                int nBaris = 1 + r.nextInt(ti <= 1 ? 7 : 4);
                while (pilih.size() < nBaris) pilih.add(pilihBobot(r, barang, totalBobot));
                List<BarisInput> baris = new ArrayList<>();
                for (int idx : pilih) {
                    long[] b = barang.get(idx);
                    int stok = (int) db.angka("SELECT stok FROM barang WHERE id=?", b[0]);
                    int maks = Math.max(1, (int) (b[1] * (ti <= 1 ? 5 : 2)));
                    int qty = Math.min(stok, 1 + r.nextInt(maks));
                    if (qty > 0) baris.add(new BarisInput(b[0], qty));
                }
                if (baris.isEmpty()) continue;
                Pengguna oleh = akun[1 + r.nextInt(2)];
                try {
                    layanan(db, d, 9 + k, r.nextInt(60), zona).barangKeluar(oleh, d, tujuan.get(ti),
                            ti < 6 ? "PO-" + (2400 + r.nextInt(500)) : "", catatanKeluar[r.nextInt(catatanKeluar.length)], baris);
                } catch (Gagal ignored) { /* stok tidak cukup: lewati */ }
            }
        }
        // pengiriman besar hari ini membuat beberapa barang menipis/habis agar peringatan terlihat
        List<BarisInput> besar = new ArrayList<>();
        int[] sisa = {0, 3, 2, 1, 4};
        String[] kodeTipis = {"PPA-007", "SMN-003", "LST-001", "CAT-003", "KRM-002"};
        for (int i = 0; i < kodeTipis.length; i++) {
            Object[] b = db.satu("SELECT id, stok FROM barang WHERE kode=?", rs -> new Object[]{rs.getLong(1), rs.getInt(2)}, kodeTipis[i]);
            int qty = (int) b[1] - sisa[i];
            if (qty > 0) besar.add(new BarisInput((long) b[0], qty));
        }
        if (!besar.isEmpty()) {
            layanan(db, hariIni, 13, 15, zona).barangKeluar(akun[1], hariIni, tujuan.get(0), "PO-2931",
                    "Pengiriman tahap 3 blok D, pakai truk Colt Diesel", besar);
        }
        // satu dokumen keluar dibatalkan agar fitur pembatalan terlihat
        Long batal = db.satu("SELECT id FROM dokumen WHERE jenis='KELUAR' AND status='AKTIF' AND tanggal=? ORDER BY id LIMIT 1",
                rs -> rs.getLong(1), hariIni.minusDays(3).toString());
        if (batal == null) batal = db.satu("SELECT id FROM dokumen WHERE jenis='KELUAR' ORDER BY id DESC LIMIT 1 OFFSET 10", rs -> rs.getLong(1));
        if (batal != null) new DokumenService(db, jam).batal(akun[0], batal, "Proyek menunda pengiriman, barang tidak jadi diambil");
    }

    private static DokumenService layanan(Database db, LocalDate d, int jam, int menit, ZoneId zona) {
        return new DokumenService(db, Clock.fixed(d.atTime(Math.min(jam, 20), menit).atZone(zona).toInstant(), zona));
    }

    private static long staf(java.sql.Connection c, String u, String nama, String dibuat) throws java.sql.SQLException {
        Long ada = Database.satu(c, "SELECT id FROM pengguna WHERE username=?", rs -> rs.getLong(1), u);
        if (ada != null) return ada;
        return Database.sisip(c, "INSERT INTO pengguna(username,nama,peran,hash,dibuat) VALUES(?,?,?,?,?)",
                u, nama, Pengguna.STAF, Password.hash("staf123"), dibuat);
    }

    private static int pilihBobot(Random r, List<long[]> b, int total) {
        int x = r.nextInt(total);
        for (int i = 0; i < b.size(); i++) { x -= (int) b.get(i)[1]; if (x < 0) return i; }
        return b.size() - 1;
    }
}
