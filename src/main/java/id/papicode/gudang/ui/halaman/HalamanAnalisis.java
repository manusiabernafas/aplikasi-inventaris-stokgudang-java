package id.papicode.gudang.ui.halaman;

import id.papicode.gudang.Konteks;
import id.papicode.gudang.model.Barang;
import id.papicode.gudang.service.LaporanService;
import id.papicode.gudang.ui.*;
import id.papicode.gudang.util.Rupiah;
import id.papicode.gudang.util.Waktu;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.util.List;

/** Analisis: barang cepat keluar (fast moving), barang mati (dead stock), dan saran belanja restok. */
public class HalamanAnalisis extends MainFrame.Lembar implements MainFrame.Halaman {

    private final Konteks k;
    private final JComboBox<String> rentang = new JComboBox<>(new String[]{"30 hari", "60 hari", "90 hari"});
    private final DefaultTableModel mCepat = model("Kode", "Barang", "Keluar", "Nilai");
    private final DefaultTableModel mMati = model("Kode", "Barang", "Rak", "Stok", "Nilai", "Keluar terakhir");
    private final DefaultTableModel mRestok = model("Kode", "Barang", "Stok", "Min", "Pesan", "Biaya");
    private final JLabel nilaiMati = Ui.label(" ");
    private final JLabel biayaRestok = Ui.label(" ");

    public HalamanAnalisis(Konteks k, MainFrame frame) {
        super(new BorderLayout());
        this.k = k;
        rentang.setPreferredSize(new Dimension(130, 38));
        rentang.addActionListener(e -> muat());
        JPanel kanan = Ui.transparan(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        kanan.add(Ui.label("Rentang analisis"));
        kanan.add(rentang);
        add(MainFrame.kepala("Lembar 09 / Laporan", "Analisis barang", "Mana yang laris, mana yang menumpuk, dan apa yang perlu dipesan.", kanan), BorderLayout.NORTH);

        JPanel isi = Ui.transparan(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 0.5;
        c.gridx = 0; c.gridy = 0; c.weightx = 0.42; c.insets = new Insets(0, 0, 14, 14);
        JTable tc = tabel(mCepat, new int[]{76, 190, 60, 110}, 2, 3);
        isi.add(Bagian.panel("Paling cepat keluar (fast moving)", Ui.gulir(tc)), c);
        c.gridx = 1; c.weightx = 0.58; c.insets = new Insets(0, 0, 14, 0);
        JTable tr = tabel(mRestok, new int[]{76, 210, 50, 50, 70, 120}, 2, 3, 4, 5);
        isi.add(Bagian.panel("Saran belanja restok", Ui.gulir(tr), biayaRestok), c);
        c.gridx = 0; c.gridy = 1; c.gridwidth = 2; c.insets = new Insets(0, 0, 0, 0);
        JTable tm = tabel(mMati, new int[]{80, 300, 80, 70, 120, 130}, 3, 4);
        isi.add(Bagian.panel("Barang tidak bergerak (dead stock): tidak ada pengeluaran di rentang ini", Ui.gulir(tm), nilaiMati), c);
        add(isi, BorderLayout.CENTER);
    }

    private static DefaultTableModel model(String... kol) {
        return new DefaultTableModel(kol, 0) { @Override public boolean isCellEditable(int r, int c) { return false; } };
    }

    private static JTable tabel(DefaultTableModel m, int[] lebar, int... angka) {
        JTable t = new JTable(m);
        Ui.rapikanTabel(t);
        t.setShowVerticalLines(false);
        Ui.lebarKolom(t, lebar);
        Ui.kolomKode(t, 0);
        Ui.kolomAngka(t, angka);
        return t;
    }

    @Override
    public void muat() {
        int hari = 30 * (rentang.getSelectedIndex() + 1);
        LocalDate h = LocalDate.now(k.jam), dari = h.minusDays(hari - 1);
        mCepat.setRowCount(0);
        for (LaporanService.Peringkat p : k.laporan.palingKeluar(dari, h, 15)) mCepat.addRow(new Object[]{p.kode(), p.nama(), p.qty(), p.nilai()});

        mMati.setRowCount(0);
        List<LaporanService.TakBergerak> mati = k.laporan.takBergerak(dari);
        long total = 0;
        for (LaporanService.TakBergerak t : mati) {
            total += t.nilai();
            mMati.addRow(new Object[]{t.kode(), t.nama(), t.lokasi(), t.stok(), t.nilai(),
                    t.terakhirKeluar() == null ? "belum pernah" : Waktu.tampilTgl(LocalDate.parse(t.terakhirKeluar()))});
        }
        nilaiMati.setText(mati.size() + " barang · modal tertahan " + Rupiah.format(total));

        mRestok.setRowCount(0);
        long biaya = 0;
        for (Barang b : k.barang.menipis()) {
            int saran = Math.max(b.stokMin() * 2 - b.stok(), 1);
            biaya += saran * b.harga();
            mRestok.addRow(new Object[]{b.kode(), b.nama(), b.stok(), b.stokMin(), saran, saran * b.harga()});
        }
        biayaRestok.setText("Total " + Rupiah.format(biaya));
    }
}
