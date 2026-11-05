package id.papicode.gudang.ui.halaman;

import id.papicode.gudang.Konteks;
import id.papicode.gudang.model.Barang;
import id.papicode.gudang.service.Gagal;
import id.papicode.gudang.service.LaporanService;
import id.papicode.gudang.ui.*;
import id.papicode.gudang.util.Csv;
import id.papicode.gudang.util.Rupiah;
import id.papicode.gudang.util.Waktu;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Kartu stok per barang: saldo awal, setiap mutasi, dan saldo berjalan. */
public class HalamanKartuStok extends MainFrame.Lembar implements MainFrame.Halaman {

    private final Konteks k;
    private final MainFrame frame;
    private final PilihPeriode periode;
    private Barang barang;
    private final JLabel nama = Ui.judul("Pilih barang", 19f);
    private final JLabel info = Ui.label(" ");
    private final JLabel vAwal = Ui.angka("-", 18f, Tema.TEKS), vMasuk = Ui.angka("-", 18f, Tema.HIJAU),
            vKeluar = Ui.angka("-", 18f, Tema.MERAH), vAkhir = Ui.angka("-", 18f, Tema.UTAMA);
    private final List<LaporanService.BarisKartu> data = new ArrayList<>();
    private final AbstractTableModel model;

    public HalamanKartuStok(Konteks k, MainFrame frame) {
        super(new BorderLayout());
        this.k = k;
        this.frame = frame;
        periode = new PilihPeriode(k.jam, 3);
        periode.onChange(this::muat);

        Ui.Tombol pilih = Ui.tombol("Pilih barang", "search", Ui.Jenis.UTAMA);
        pilih.addActionListener(e -> {
            Barang b = PilihBarang.pilih(this, k, "");
            if (b != null) tampilkan(b);
        });
        Ui.Tombol cetak = Ui.tombol("Cetak", "printer", Ui.Jenis.GARIS);
        cetak.addActionListener(e -> cetak());
        Ui.Tombol csv = Ui.tombol("CSV", "download", Ui.Jenis.GARIS);
        csv.addActionListener(e -> ekspor());
        add(MainFrame.kepala("Lembar 07 / Laporan", "Kartu stok", "Riwayat keluar-masuk satu barang dengan saldo berjalan.", csv, cetak, pilih), BorderLayout.NORTH);

        JPanel atas = Ui.transparan(new BorderLayout(0, 12));
        Ui.Kartu kb = new Ui.Kartu(new BorderLayout(20, 0));
        kb.setBorder(new EmptyBorder(14, 18, 14, 18));
        JPanel kiri = Ui.transparan(new BorderLayout(0, 4));
        kiri.add(nama, BorderLayout.NORTH);
        kiri.add(info, BorderLayout.CENTER);
        kb.add(kiri, BorderLayout.CENTER);
        JPanel angka = Ui.transparan(new GridLayout(1, 4, 24, 0));
        angka.add(kecil("Saldo awal", vAwal));
        angka.add(kecil("Masuk", vMasuk));
        angka.add(kecil("Keluar", vKeluar));
        angka.add(kecil("Saldo akhir", vAkhir));
        kb.add(angka, BorderLayout.EAST);
        atas.add(kb, BorderLayout.NORTH);
        JPanel f = Ui.transparan(new FlowLayout(FlowLayout.LEFT, 0, 0));
        f.add(periode);
        atas.add(f, BorderLayout.CENTER);
        atas.setBorder(new EmptyBorder(0, 0, 12, 0));

        model = new AbstractTableModel() {
            final String[] kol = {"Tanggal", "No. dokumen", "Jenis", "Pihak / keterangan", "Masuk", "Keluar", "Saldo"};
            public int getRowCount() { return data.size(); }
            public int getColumnCount() { return kol.length; }
            public String getColumnName(int c) { return kol[c]; }
            public Object getValueAt(int r, int c) {
                LaporanService.BarisKartu b = data.get(r);
                return switch (c) {
                    case 0 -> Waktu.tampilTgl(LocalDate.parse(b.tanggal()));
                    case 1 -> b.nomor();
                    case 2 -> jenis(b.jenis());
                    case 3 -> b.keterangan();
                    case 4 -> b.masuk() == 0 ? null : b.masuk();
                    case 5 -> b.keluar() == 0 ? null : b.keluar();
                    default -> b.saldo();
                };
            }
        };
        JTable t = new JTable(model);
        Ui.rapikanTabel(t);
        Ui.lebarKolom(t, 110, 130, 90, 300, 90, 90, 100);
        Ui.kolomKode(t, 1);
        Ui.kolomAngka(t, 4, 5, 6);
        t.getColumnModel().getColumn(2).setCellRenderer(new Bagian.Pil(Map.of("Masuk", Tema.HIJAU, "Keluar", Tema.MERAH,
                "Opname", Tema.AKSEN, "Saldo awal", Tema.UTAMA, "Saldo", Tema.GARIS_TERANG)));
        JPanel tengah = Ui.transparan(new BorderLayout());
        tengah.add(atas, BorderLayout.NORTH);
        tengah.add(Bagian.tabel(t), BorderLayout.CENTER);
        add(tengah, BorderLayout.CENTER);
    }

    static String jenis(String j) {
        return switch (j) {
            case "MASUK" -> "Masuk";
            case "KELUAR" -> "Keluar";
            case "KOREKSI" -> "Opname";
            case "AWAL" -> "Saldo awal";
            default -> "Saldo";
        };
    }

    private static JComponent kecil(String l, JLabel v) {
        JPanel p = Ui.transparan(new BorderLayout(0, 2));
        p.add(Ui.label(l), BorderLayout.NORTH);
        p.add(v, BorderLayout.CENTER);
        return p;
    }

    /** Dipanggil dari halaman lain (misal Daftar Barang). */
    public void tampilkan(Barang b) {
        barang = b;
        muat();
    }

    @Override
    public void muat() {
        data.clear();
        if (barang == null) {
            // awali dengan barang yang paling sering keluar agar kartu langsung informatif
            LocalDate h = LocalDate.now(k.jam);
            List<LaporanService.Peringkat> top = k.laporan.palingKeluar(h.minusDays(29), h, 1);
            barang = top.isEmpty() ? null : k.barang.byKode(top.get(0).kode());
            if (barang == null) {
                List<Barang> semua = k.barang.daftar(null, null, null, false);
                if (!semua.isEmpty()) barang = semua.get(0);
            }
        }
        if (barang != null) {
            barang = k.barang.byId(barang.id());
            nama.setText(barang.kode() + "  " + barang.nama());
            info.setText("Rak " + (barang.lokasi() == null ? "-" : barang.lokasi()) + "  ·  " + barang.kategori() + "  ·  stok sekarang "
                    + barang.stok() + " " + barang.satuan() + "  ·  min " + barang.stokMin());
            data.addAll(k.laporan.kartuStok(barang.id(), periode.dari(), periode.sampai()));
            long masuk = data.stream().mapToLong(LaporanService.BarisKartu::masuk).sum();
            long keluar = data.stream().mapToLong(LaporanService.BarisKartu::keluar).sum();
            vAwal.setText(Rupiah.ribuan(data.get(0).saldo()));
            vMasuk.setText("+" + Rupiah.ribuan(masuk));
            vKeluar.setText("-" + Rupiah.ribuan(keluar));
            vAkhir.setText(Rupiah.ribuan(data.get(data.size() - 1).saldo()));
        }
        model.fireTableDataChanged();
    }

    private void cetak() {
        if (barang == null) return;
        LembarCetak l = new LembarCetak("KARTU STOK", k.pengaturan.get("nama_perusahaan"),
                k.pengaturan.get("nama_gudang") + " - " + k.pengaturan.get("alamat"));
        l.info("Kode", barang.kode()).info("Nama barang", barang.nama()).info("Lokasi rak", barang.lokasi())
         .info("Satuan", barang.satuan()).info("Periode", Waktu.tampilTgl(periode.dari()) + " s/d " + Waktu.tampilTgl(periode.sampai()))
         .info("Stok minimum", String.valueOf(barang.stokMin()));
        l.kolom(new String[]{"Tanggal", "No. dokumen", "Jenis", "Pihak / keterangan", "Masuk", "Keluar", "Saldo"},
                new double[]{0.12, 0.15, 0.1, 0.33, 0.1, 0.1, 0.1}, new boolean[]{false, false, false, false, true, true, true});
        long masuk = 0, keluar = 0;
        for (LaporanService.BarisKartu b : data) {
            masuk += b.masuk();
            keluar += b.keluar();
            l.baris(Waktu.tampilTgl(LocalDate.parse(b.tanggal())), b.nomor(), jenis(b.jenis()), b.keterangan(),
                    b.masuk() == 0 ? "" : Rupiah.ribuan(b.masuk()), b.keluar() == 0 ? "" : Rupiah.ribuan(b.keluar()), Rupiah.ribuan(b.saldo()));
        }
        l.total("", "", "", "Jumlah", Rupiah.ribuan(masuk), Rupiah.ribuan(keluar), Rupiah.ribuan(data.get(data.size() - 1).saldo()));
        l.tandaTangan(k.pengaturan.get("kota") + ", " + Waktu.tampilTgl(LocalDate.now(k.jam)), new String[]{"Kepala gudang", k.pengaturan.get("penanggung_jawab")});
        Pratinjau.tampil(this, l, "Kartu stok " + barang.kode(), false);
    }

    private void ekspor() {
        if (barang == null) return;
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("kartu-stok-" + barang.kode() + ".csv"));
        fc.setFileFilter(new FileNameExtensionFilter("CSV (Excel)", "csv"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File file = fc.getSelectedFile().getName().toLowerCase().endsWith(".csv") ? fc.getSelectedFile() : new File(fc.getSelectedFile() + ".csv");
        Kotak.aman(this, () -> {
            List<List<Object>> rows = new ArrayList<>();
            for (LaporanService.BarisKartu b : data) rows.add(List.of(b.tanggal(), b.nomor(), jenis(b.jenis()), b.keterangan() == null ? "" : b.keterangan(), b.masuk(), b.keluar(), b.saldo()));
            try { Csv.tulis(file.toPath(), List.of("Tanggal", "No. dokumen", "Jenis", "Keterangan", "Masuk", "Keluar", "Saldo"), rows); }
            catch (java.io.IOException e) { throw new Gagal("File tidak bisa disimpan: " + e.getMessage()); }
            frame.kabar("Kartu stok diekspor ke " + file.getName());
        });
    }
}
