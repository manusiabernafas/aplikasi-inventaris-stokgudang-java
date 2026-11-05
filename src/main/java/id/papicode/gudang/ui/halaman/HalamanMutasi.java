package id.papicode.gudang.ui.halaman;

import id.papicode.gudang.Konteks;
import id.papicode.gudang.model.Kategori;
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

/** Laporan mutasi persediaan per periode: saldo awal, masuk, keluar, penyesuaian, saldo akhir, nilai. */
public class HalamanMutasi extends MainFrame.Lembar implements MainFrame.Halaman {

    private final Konteks k;
    private final MainFrame frame;
    private final PilihPeriode periode;
    private final JComboBox<Object> fKategori = new JComboBox<>();
    private final JCheckBox fBergerak = new JCheckBox("Hanya yang bergerak");
    private final List<LaporanService.BarisMutasi> data = new ArrayList<>();
    private final AbstractTableModel model;
    private final JLabel tAwal = Ui.angka("0", 17f, Tema.TEKS), tMasuk = Ui.angka("0", 17f, Tema.HIJAU),
            tKeluar = Ui.angka("0", 17f, Tema.MERAH), tKoreksi = Ui.angka("0", 17f, Tema.AKSEN), tNilai = Ui.angka("Rp0", 17f, Tema.UTAMA);
    private boolean memuat;

    public HalamanMutasi(Konteks k, MainFrame frame) {
        super(new BorderLayout());
        this.k = k;
        this.frame = frame;
        periode = new PilihPeriode(k.jam, 4);
        periode.onChange(this::muat);
        Ui.Tombol cetak = Ui.tombol("Cetak", "printer", Ui.Jenis.GARIS);
        cetak.addActionListener(e -> cetak());
        Ui.Tombol csv = Ui.tombol("CSV", "download", Ui.Jenis.GARIS);
        csv.addActionListener(e -> ekspor());
        add(MainFrame.kepala("Lembar 08 / Laporan", "Mutasi persediaan", "Rekap keluar-masuk semua barang dalam satu periode, siap untuk laporan bulanan.", csv, cetak), BorderLayout.NORTH);

        JPanel atas = Ui.transparan(new BorderLayout(0, 12));
        JPanel f = Ui.transparan(new FlowLayout(FlowLayout.LEFT, 8, 0));
        f.add(periode);
        fKategori.setPreferredSize(new Dimension(190, 38));
        fKategori.addActionListener(e -> { if (!memuat) muatData(); });
        f.add(fKategori);
        fBergerak.setOpaque(false);
        fBergerak.addActionListener(e -> muatData());
        f.add(fBergerak);
        atas.add(f, BorderLayout.NORTH);
        Ui.Kartu tot = new Ui.Kartu(new GridLayout(1, 5, 20, 0));
        tot.setBorder(new EmptyBorder(12, 18, 12, 18));
        tot.add(kecil("Saldo awal (unit)", tAwal));
        tot.add(kecil("Masuk", tMasuk));
        tot.add(kecil("Keluar", tKeluar));
        tot.add(kecil("Penyesuaian", tKoreksi));
        tot.add(kecil("Nilai saldo akhir", tNilai));
        atas.add(tot, BorderLayout.CENTER);
        atas.setBorder(new EmptyBorder(0, 0, 12, 0));

        model = new AbstractTableModel() {
            final String[] kol = {"Kode", "Nama barang", "Kategori", "Satuan", "Awal", "Masuk", "Keluar", "Penyesuaian", "Akhir", "Harga", "Nilai akhir"};
            public int getRowCount() { return data.size(); }
            public int getColumnCount() { return kol.length; }
            public String getColumnName(int c) { return kol[c]; }
            public Object getValueAt(int r, int c) {
                LaporanService.BarisMutasi b = data.get(r);
                return switch (c) {
                    case 0 -> b.kode(); case 1 -> b.nama(); case 2 -> b.kategori(); case 3 -> b.satuan();
                    case 4 -> b.awal(); case 5 -> b.masuk(); case 6 -> b.keluar(); case 7 -> b.koreksi();
                    case 8 -> b.akhir(); case 9 -> b.harga(); default -> b.nilaiAkhir();
                };
            }
        };
        JTable t = new JTable(model);
        Ui.rapikanTabel(t);
        Ui.lebarKolom(t, 80, 240, 120, 60, 70, 70, 70, 90, 70, 90, 110);
        Ui.kolomKode(t, 0);
        Ui.kolomAngka(t, 4, 5, 6, 7, 8, 9, 10);
        JPanel tengah = Ui.transparan(new BorderLayout());
        tengah.add(atas, BorderLayout.NORTH);
        tengah.add(Bagian.tabel(t), BorderLayout.CENTER);
        add(tengah, BorderLayout.CENTER);
    }

    private static JComponent kecil(String l, JLabel v) {
        JPanel p = Ui.transparan(new BorderLayout(0, 2));
        p.add(Ui.label(l), BorderLayout.NORTH);
        p.add(v, BorderLayout.CENTER);
        return p;
    }

    @Override
    public void muat() {
        memuat = true;
        Object pilih = fKategori.getSelectedItem();
        fKategori.removeAllItems();
        fKategori.addItem("Semua kategori");
        k.barang.kategori().forEach(fKategori::addItem);
        if (pilih != null) fKategori.setSelectedItem(pilih);
        memuat = false;
        muatData();
    }

    private void muatData() {
        Long kat = fKategori.getSelectedItem() instanceof Kategori c ? c.id() : null;
        data.clear();
        data.addAll(k.laporan.mutasi(periode.dari(), periode.sampai(), kat));
        if (fBergerak.isSelected()) data.removeIf(b -> b.masuk() == 0 && b.keluar() == 0 && b.koreksi() == 0);
        model.fireTableDataChanged();
        tAwal.setText(Rupiah.ribuan(data.stream().mapToLong(LaporanService.BarisMutasi::awal).sum()));
        tMasuk.setText("+" + Rupiah.ribuan(data.stream().mapToLong(LaporanService.BarisMutasi::masuk).sum()));
        tKeluar.setText("-" + Rupiah.ribuan(data.stream().mapToLong(LaporanService.BarisMutasi::keluar).sum()));
        long kor = data.stream().mapToLong(LaporanService.BarisMutasi::koreksi).sum();
        tKoreksi.setText((kor > 0 ? "+" : "") + Rupiah.ribuan(kor));
        tNilai.setText(Rupiah.format(data.stream().mapToLong(LaporanService.BarisMutasi::nilaiAkhir).sum()));
    }

    private void cetak() {
        LembarCetak l = new LembarCetak("LAPORAN MUTASI PERSEDIAAN", k.pengaturan.get("nama_perusahaan"),
                k.pengaturan.get("nama_gudang") + " - " + k.pengaturan.get("alamat"));
        l.info("Periode", Waktu.tampilTgl(periode.dari()) + " s/d " + Waktu.tampilTgl(periode.sampai()))
         .info("Kategori", String.valueOf(fKategori.getSelectedItem()));
        l.kolom(new String[]{"Kode", "Nama barang", "Sat", "Awal", "Masuk", "Keluar", "Penyes.", "Akhir", "Nilai akhir"},
                new double[]{0.09, 0.3, 0.07, 0.08, 0.08, 0.08, 0.08, 0.08, 0.14},
                new boolean[]{false, false, false, true, true, true, true, true, true});
        long[] t = new long[6];
        for (LaporanService.BarisMutasi b : data) {
            l.baris(b.kode(), b.nama(), b.satuan(), Rupiah.ribuan(b.awal()), Rupiah.ribuan(b.masuk()), Rupiah.ribuan(b.keluar()),
                    Rupiah.ribuan(b.koreksi()), Rupiah.ribuan(b.akhir()), Rupiah.ribuan(b.nilaiAkhir()));
            t[0] += b.awal(); t[1] += b.masuk(); t[2] += b.keluar(); t[3] += b.koreksi(); t[4] += b.akhir(); t[5] += b.nilaiAkhir();
        }
        l.total("", "Total", "", Rupiah.ribuan(t[0]), Rupiah.ribuan(t[1]), Rupiah.ribuan(t[2]), Rupiah.ribuan(t[3]), Rupiah.ribuan(t[4]), Rupiah.ribuan(t[5]));
        l.tandaTangan(k.pengaturan.get("kota") + ", " + Waktu.tampilTgl(LocalDate.now(k.jam)),
                new String[]{"Dibuat oleh", frame.pengguna().nama()}, new String[]{"Kepala gudang", k.pengaturan.get("penanggung_jawab")});
        Pratinjau.tampil(this, l, "Mutasi persediaan", false);
    }

    private void ekspor() {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("mutasi-" + periode.dari() + "_" + periode.sampai() + ".csv"));
        fc.setFileFilter(new FileNameExtensionFilter("CSV (Excel)", "csv"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File file = fc.getSelectedFile().getName().toLowerCase().endsWith(".csv") ? fc.getSelectedFile() : new File(fc.getSelectedFile() + ".csv");
        Kotak.aman(this, () -> {
            List<List<Object>> rows = new ArrayList<>();
            for (LaporanService.BarisMutasi b : data) {
                rows.add(List.of(b.kode(), b.nama(), b.kategori() == null ? "" : b.kategori(), b.satuan(), b.awal(), b.masuk(), b.keluar(),
                        b.koreksi(), b.akhir(), b.harga(), b.nilaiAkhir()));
            }
            try { Csv.tulis(file.toPath(), List.of("Kode", "Nama", "Kategori", "Satuan", "Awal", "Masuk", "Keluar", "Penyesuaian", "Akhir", "Harga", "Nilai akhir"), rows); }
            catch (java.io.IOException e) { throw new Gagal("File tidak bisa disimpan: " + e.getMessage()); }
            frame.kabar(rows.size() + " baris diekspor ke " + file.getName());
        });
    }
}
