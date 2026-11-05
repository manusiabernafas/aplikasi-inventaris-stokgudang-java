package id.papicode.gudang;

import id.papicode.gudang.db.DataContoh;
import id.papicode.gudang.model.*;
import id.papicode.gudang.service.Gagal;
import id.papicode.gudang.service.LaporanService;
import id.papicode.gudang.ui.Ikon;
import id.papicode.gudang.ui.LabelBarcode;
import id.papicode.gudang.ui.LembarCetak;
import id.papicode.gudang.ui.Pratinjau;
import id.papicode.gudang.util.Csv;
import id.papicode.gudang.util.Password;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class StokGudangTest {

    static class JamUji extends Clock {
        Instant t = LocalDateTime.of(2026, 5, 20, 10, 0).atZone(ZoneId.of("Asia/Jakarta")).toInstant();
        @Override public ZoneId getZone() { return ZoneId.of("Asia/Jakarta"); }
        @Override public Clock withZone(ZoneId z) { return this; }
        @Override public Instant instant() { return t; }
        void maju(Duration d) { t = t.plus(d); }
    }

    @TempDir Path tmp;
    JamUji jam;
    Konteks k;
    Pengguna admin;
    long supplier, tujuan;
    LocalDate hari;

    @BeforeEach
    void siap() {
        jam = new JamUji();
        k = new Konteks(tmp.resolve("uji.db").toFile(), jam);
        k.auth.pastikanAdminAwal();
        admin = k.auth.login("admin", "admin123");
        supplier = k.barang.simpanPihak(admin, new Pihak(0, Pihak.SUPPLIER, "PT Semen Indonesia", "031-3981732", "Gresik", true)).id();
        tujuan = k.barang.simpanPihak(admin, new Pihak(0, Pihak.TUJUAN, "Proyek Griya Asri", "0812345678", "Sidoarjo", true)).id();
        hari = LocalDate.now(jam);
    }

    Barang barang(String kode, int awal) {
        return k.barang.simpan(admin, new Barang(0, kode, "Barang " + kode, null, null, "sak", "A-01-01", 62000, 0, 5, true), awal);
    }

    Pengguna staf() {
        k.auth.tambah(admin, "budi", "Budi Santoso", Pengguna.STAF, "rahasia1");
        return k.auth.login("budi", "rahasia1");
    }

    int stok(Barang b) { return k.barang.byId(b.id()).stok(); }

    // ---------------- data contoh ----------------

    @Test
    void dataContohKonsistenDenganLaporan() {
        Konteks kosong = new Konteks(tmp.resolve("contoh.db").toFile(), jam);
        kosong.auth.pastikanAdminAwal();
        DataContoh.isi(kosong.db, jam);
        assertEquals(45, kosong.db.angka("SELECT COUNT(*) FROM barang"));
        assertEquals(14, kosong.db.angka("SELECT COUNT(*) FROM pihak"));
        assertTrue(kosong.db.angka("SELECT COUNT(*) FROM dokumen WHERE jenis='KELUAR'") > 150);
        assertEquals(1, kosong.db.angka("SELECT COUNT(*) FROM dokumen WHERE jenis='OPNAME'"));
        assertEquals(1, kosong.db.angka("SELECT COUNT(*) FROM dokumen WHERE status='BATAL'"));
        assertEquals(0, kosong.db.angka("SELECT COUNT(*) FROM barang WHERE stok < 0"));
        // stok setiap barang = jumlah semua mutasi
        assertEquals(0, kosong.db.angka("SELECT COUNT(*) FROM barang b WHERE b.stok <> (SELECT COALESCE(SUM(qty),0) FROM mutasi m WHERE m.barang_id=b.id)"));
        // saldo akhir laporan mutasi = stok sekarang
        for (LaporanService.BarisMutasi m : kosong.laporan.mutasi(hari.minusDays(30), hari, null)) {
            assertEquals(kosong.barang.byId(m.barangId()).stok(), m.akhir(), m.kode());
        }
        assertEquals(5, kosong.barang.menipis().size());
        assertThrows(IllegalStateException.class, () -> DataContoh.isi(kosong.db, jam));
        assertNotNull(kosong.auth.login("budi", "staf123"));
    }

    // ---------------- dokumen ----------------

    @Test
    void barangMasukMenambahStokDanMencatatMutasi() {
        Barang b = barang("SMN-001", 10);
        Dokumen d = k.dokumen.barangMasuk(admin, hari, supplier, "SJ/2605/0012", "", List.of(new BarisInput(b.id(), 40), new BarisInput(b.id(), 10)));
        assertEquals("BM-260520-001", d.nomor());
        assertEquals(60, stok(b));
        assertEquals(1, d.jumlahBaris(), "baris barang yang sama digabung");
        assertEquals(50, d.totalQty());
        assertEquals(50L * 62000, d.totalNilai());
        assertEquals("BM-260520-002", k.dokumen.barangMasuk(admin, hari, supplier, "", "", List.of(new BarisInput(b.id(), 1))).nomor());
        List<LaporanService.BarisKartu> kartu = k.laporan.kartuStok(b.id(), hari, hari);
        assertEquals(61, kartu.get(kartu.size() - 1).saldo());
    }

    @Test
    void barangKeluarTidakBolehMelebihiStokDanSemuaBarisDibatalkan() {
        Barang a = barang("SMN-001", 10);
        Barang b = barang("BSI-001", 3);
        Gagal g = assertThrows(Gagal.class, () -> k.dokumen.barangKeluar(admin, hari, tujuan, "", "", List.of(new BarisInput(a.id(), 5), new BarisInput(b.id(), 4))));
        assertTrue(g.getMessage().contains("tidak cukup"), g.getMessage());
        assertEquals(10, stok(a), "baris pertama ikut batal (rollback)");
        assertEquals(0, k.db.angka("SELECT COUNT(*) FROM dokumen"));
        Dokumen d = k.dokumen.barangKeluar(admin, hari, tujuan, "PO-1", "", List.of(new BarisInput(a.id(), 5), new BarisInput(b.id(), 3)));
        assertEquals("BK-260520-001", d.nomor());
        assertEquals(5, stok(a));
        assertEquals(0, stok(b));
    }

    @Test
    void validasiDokumen() {
        Barang a = barang("SMN-001", 10);
        List<BarisInput> satu = List.of(new BarisInput(a.id(), 1));
        assertThrows(Gagal.class, () -> k.dokumen.barangMasuk(admin, hari, tujuan, "", "", satu), "masuk harus dari supplier");
        assertThrows(Gagal.class, () -> k.dokumen.barangKeluar(admin, hari, supplier, "", "", satu), "keluar harus ke tujuan");
        assertThrows(Gagal.class, () -> k.dokumen.barangMasuk(admin, hari.plusDays(1), supplier, "", "", satu), "tanggal masa depan");
        assertThrows(Gagal.class, () -> k.dokumen.barangMasuk(admin, hari.minusYears(2), supplier, "", "", satu), "terlalu lama");
        assertThrows(Gagal.class, () -> k.dokumen.barangMasuk(admin, hari, supplier, "", "", List.of()), "tanpa barang");
        assertThrows(Gagal.class, () -> k.dokumen.barangMasuk(admin, hari, supplier, "", "", List.of(new BarisInput(a.id(), 0))), "qty 0");
        assertThrows(Gagal.class, () -> k.dokumen.barangMasuk(admin, hari, supplier, "", "", List.of(new BarisInput(a.id(), -3))), "qty negatif");
        k.barang.simpan(admin, new Barang(a.id(), a.kode(), a.nama(), null, null, "sak", null, 1, 0, 0, false), 0);
        assertThrows(Gagal.class, () -> k.dokumen.barangMasuk(admin, hari, supplier, "", "", satu), "barang nonaktif");
    }

    @Test
    void pembatalanMembalikStokDanDiabaikanLaporan() {
        Barang a = barang("SMN-001", 0);
        Dokumen m = k.dokumen.barangMasuk(admin, hari, supplier, "", "", List.of(new BarisInput(a.id(), 20)));
        Dokumen kel = k.dokumen.barangKeluar(admin, hari, tujuan, "", "", List.of(new BarisInput(a.id(), 15)));
        assertThrows(Gagal.class, () -> k.dokumen.batal(admin, m.id(), "salah supplier"), "barang masuk sudah terlanjur keluar");
        k.dokumen.batal(admin, kel.id(), "proyek menunda");
        assertEquals(20, stok(a));
        assertThrows(Gagal.class, () -> k.dokumen.batal(admin, kel.id(), "lagi"), "tidak bisa dua kali");
        k.dokumen.batal(admin, m.id(), "salah supplier");
        assertEquals(0, stok(a));
        LaporanService.BarisMutasi r = k.laporan.mutasi(hari, hari, null).get(0);
        assertEquals(0, r.masuk());
        assertEquals(0, r.keluar());
        assertEquals(0, r.akhir());
        assertThrows(Gagal.class, () -> k.dokumen.batal(admin, m.id(), ""), "alasan wajib");
    }

    @Test
    void opnameMenyamakanStokDanMencatatSelisih() {
        Barang a = barang("SMN-001", 50);
        Barang b = barang("BSI-001", 20);
        Dokumen op = k.dokumen.opname(admin, hari, "Opname bulanan", List.of(new BarisInput(a.id(), 47), new BarisInput(b.id(), 20)));
        assertEquals("OP-260520-001", op.nomor());
        assertEquals(47, stok(a));
        assertEquals(20, stok(b));
        assertEquals(-3, op.totalQty());
        assertEquals(-3L * 62000, op.totalNilai());
        List<ItemDokumen> it = k.dokumen.item(op.id());
        assertEquals(50, it.get(0).stokSistem());
        assertEquals(-3, it.get(0).selisih());
        assertThrows(Gagal.class, () -> k.dokumen.batal(admin, op.id(), "salah hitung"), "opname tidak bisa dibatalkan");
        assertThrows(Gagal.class, () -> k.dokumen.opname(admin, hari, "", List.of(new BarisInput(a.id(), 1), new BarisInput(a.id(), 2))), "barang dobel");
        assertThrows(Gagal.class, () -> k.dokumen.opname(admin, hari, "", List.of(new BarisInput(a.id(), -1))));
        // saldo awal 50 (AWAL) dan koreksi -3 jatuh di hari yang sama, keduanya masuk kolom penyesuaian
        assertEquals(47, k.laporan.mutasi(hari, hari, null).stream().filter(m -> m.kode().equals("SMN-001")).findFirst().orElseThrow().koreksi());
    }

    @Test
    void hakAksesStafDibatasi() {
        Pengguna budi = staf();
        Barang a = barang("SMN-001", 10);
        assertNotNull(k.dokumen.barangMasuk(budi, hari, supplier, "", "", List.of(new BarisInput(a.id(), 1))));
        Dokumen d = k.dokumen.barangKeluar(budi, hari, tujuan, "", "", List.of(new BarisInput(a.id(), 1)));
        assertThrows(Gagal.class, () -> k.dokumen.batal(budi, d.id(), "salah"));
        assertThrows(Gagal.class, () -> k.dokumen.opname(budi, hari, "", List.of(new BarisInput(a.id(), 5))));
        assertThrows(Gagal.class, () -> k.barang.simpan(budi, a, 0));
        assertThrows(Gagal.class, () -> k.barang.simpanPihak(budi, new Pihak(0, Pihak.TUJUAN, "X", "", "", true)));
        k.auth.setAktif(admin, budi.id(), false);
        assertThrows(Gagal.class, () -> k.auth.segarkan(budi), "akun nonaktif langsung ditolak");
    }

    @Test
    void duaStafRebutanStokTerakhir() throws Exception {
        Barang a = barang("SMN-001", 9);
        int n = 8;
        ExecutorService ex = Executors.newFixedThreadPool(n);
        CountDownLatch mulai = new CountDownLatch(1);
        AtomicInteger sukses = new AtomicInteger();
        File file = tmp.resolve("uji.db").toFile();
        List<Future<?>> f = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            f.add(ex.submit(() -> {
                Konteks lain = new Konteks(file, jam);
                mulai.await();
                try {
                    lain.dokumen.barangKeluar(admin, hari, tujuan, "", "", List.of(new BarisInput(a.id(), 4)));
                    sukses.incrementAndGet();
                } catch (Gagal g) { /* stok habis */ }
                return null;
            }));
        }
        mulai.countDown();
        for (Future<?> x : f) x.get(30, TimeUnit.SECONDS);
        ex.shutdown();
        assertEquals(2, sukses.get());
        assertEquals(1, stok(a));
        assertEquals(2, k.db.angka("SELECT COUNT(DISTINCT nomor) FROM dokumen"));
    }

    @Test
    void kartuStokSaldoBerjalan() {
        Barang a = barang("SMN-001", 0);
        k.dokumen.barangMasuk(admin, hari.minusDays(5), supplier, "", "", List.of(new BarisInput(a.id(), 100)));
        k.dokumen.barangKeluar(admin, hari.minusDays(3), tujuan, "", "", List.of(new BarisInput(a.id(), 30)));
        k.dokumen.barangKeluar(admin, hari, tujuan, "", "", List.of(new BarisInput(a.id(), 20)));
        List<LaporanService.BarisKartu> l = k.laporan.kartuStok(a.id(), hari.minusDays(3), hari);
        assertEquals(100, l.get(0).saldo(), "saldo awal dari mutasi sebelum periode");
        assertEquals(70, l.get(1).saldo());
        assertEquals(50, l.get(2).saldo());
        assertEquals(stok(a), l.get(l.size() - 1).saldo());
    }

    // ---------------- master data ----------------

    @Test
    void validasiBarangDanPihak() {
        barang("SMN-001", 0);
        assertThrows(Gagal.class, () -> barang("smn-001", 0), "kode unik tanpa beda huruf besar");
        assertThrows(Gagal.class, () -> barang("A", 0), "kode terlalu pendek");
        assertThrows(Gagal.class, () -> barang("SMN 001", 0), "kode berspasi");
        assertEquals("SMN-002", k.barang.saranKode("smn"));
        assertEquals("BRG-001", k.barang.saranKode(""));
        assertThrows(Gagal.class, () -> k.barang.simpanPihak(admin, new Pihak(0, Pihak.SUPPLIER, "PT Semen Indonesia", "", "", true)), "nama dobel");
        assertThrows(Gagal.class, () -> k.barang.simpanPihak(admin, new Pihak(0, Pihak.SUPPLIER, "CV Baru", "telp=1", "", true)), "telepon aneh");
        assertEquals(0, k.barang.daftar("%", null, null, true).size(), "wildcard LIKE di-escape");
    }

    @Test
    void hapusBarangBerriwayatHanyaNonaktif() {
        Barang a = barang("SMN-001", 5);
        Barang b = barang("SMN-002", 5);
        k.dokumen.barangKeluar(admin, hari, tujuan, "", "", List.of(new BarisInput(a.id(), 1)));
        assertFalse(k.barang.hapus(admin, a.id()));
        assertFalse(k.barang.byId(a.id()).aktif());
        assertTrue(k.barang.hapus(admin, b.id()));
        assertNull(k.barang.byId(b.id()));
    }

    // ---------------- keamanan, cetak, utilitas ----------------

    @Test
    void loginTerkunciSetelahLimaKaliSalah() {
        for (int i = 0; i < 5; i++) assertThrows(Gagal.class, () -> k.auth.login("admin", "salah"));
        Gagal g = assertThrows(Gagal.class, () -> k.auth.login("admin", "admin123"));
        assertTrue(g.getMessage().contains("Terlalu banyak"));
        jam.maju(Duration.ofMinutes(6));
        assertNotNull(k.auth.login("admin", "admin123"));
        assertTrue(Password.hash("x").startsWith("pbkdf2$"));
    }

    @Test
    void gantiPasswordMembatalkanYangLama() {
        k.auth.gantiPassword(admin, "admin123", "gudang2026");
        assertThrows(Gagal.class, () -> k.auth.login("admin", "admin123"));
        assertNotNull(k.auth.login("admin", "gudang2026"));
    }

    @Test
    void barcodeCode128Valid() {
        // setiap pola Code 128 lebarnya 11 modul (stop 13)
        List<Integer> m = LabelBarcode.code128("SMN-001");
        int total = m.stream().mapToInt(Integer::intValue).sum();
        assertEquals(11 * (1 + 7 + 1) + 13, total, "start + 7 karakter + checksum + stop");
        // checksum contoh: start B (104) + S(51)*1 + M(45)*2 + N(46)*3 + -(13)*4 + 0(16)*5 + 0(16)*6 + 1(17)*7
        int cek = (104 + 51 + 90 + 138 + 52 + 80 + 96 + 119) % 103; // = 9, pola "221213"
        assertEquals(9, cek);
        assertEquals(List.of(2, 2, 1, 2, 1, 3), m.subList(48, 54));
        assertEquals(List.of(2, 3, 3, 1, 1, 1, 2), m.subList(m.size() - 7, m.size()));
        assertEquals(List.of(2, 1, 1, 2, 1, 4), m.subList(0, 6), "start B");
        assertThrows(IllegalArgumentException.class, () -> LabelBarcode.code128("é"));
    }

    @Test
    void lembarCetakBerhalaman() {
        LembarCetak l = new LembarCetak("UJI", "CV Uji");
        l.kolom(new String[]{"A", "B"}, new double[]{0.5, 0.5}, new boolean[]{false, true});
        for (int i = 0; i < 120; i++) l.baris("baris " + i, String.valueOf(i));
        l.tandaTangan("Sidoarjo", new String[]{"Kepala", "Hendra"});
        int hal = l.jumlahHalaman(Pratinjau.a4(false));
        assertTrue(hal >= 3, "120 baris harus lebih dari 2 halaman, dapat " + hal);
        BufferedImage img = new BufferedImage(600, 850, BufferedImage.TYPE_INT_RGB);
        for (int i = 0; i < hal; i++) assertEquals(java.awt.print.Printable.PAGE_EXISTS, l.print(img.createGraphics(), Pratinjau.a4(false), i));
        assertEquals(java.awt.print.Printable.NO_SUCH_PAGE, l.print(img.createGraphics(), Pratinjau.a4(false), hal));
    }

    @Test
    void csvAmanDanCadangan() throws Exception {
        assertEquals("'=1+1", Csv.sel("=1+1"));
        assertEquals("'@cmd", Csv.sel("@cmd"));
        assertEquals("-5", Csv.sel(-5L));
        Path f = tmp.resolve("a.csv");
        Csv.tulis(f, List.of("Kode"), List.of(List.of("+62")));
        assertTrue(Files.readString(f).contains("'+62"));
        barang("SMN-001", 3);
        File c = tmp.resolve("cadangan.db").toFile();
        k.db.cadangkan(c);
        assertEquals(1, new Konteks(c, jam).db.angka("SELECT COUNT(*) FROM barang"));
    }

    @Test
    void semuaIkonValid() throws Exception {
        java.lang.reflect.Field f = Ikon.class.getDeclaredField("SVG");
        f.setAccessible(true);
        @SuppressWarnings("unchecked")
        java.util.Map<String, String> svg = (java.util.Map<String, String>) f.get(null);
        BufferedImage img = new BufferedImage(24, 24, BufferedImage.TYPE_INT_ARGB);
        for (String n : svg.keySet()) Ikon.of(n, 24, java.awt.Color.BLACK).paintIcon(null, img.createGraphics(), 0, 0);
    }
}
