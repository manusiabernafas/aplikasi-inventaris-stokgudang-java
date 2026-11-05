package id.papicode.gudang.ui.halaman;

import id.papicode.gudang.Konteks;
import id.papicode.gudang.model.Barang;
import id.papicode.gudang.model.Dokumen;
import id.papicode.gudang.service.LaporanService;
import id.papicode.gudang.ui.*;
import id.papicode.gudang.util.Rupiah;
import id.papicode.gudang.util.Waktu;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Ringkasan gudang: nilai persediaan, grafik masuk vs keluar, perlu restok, aktivitas terbaru. */
public class HalamanDasbor extends MainFrame.Lembar implements MainFrame.Halaman {

    private final Konteks k;
    private final MainFrame frame;
    private final JLabel vSku = Ui.angka("0", 24f, Tema.TEKS), kSku = Ui.teks(" ", 12f, Tema.TEKS_2);
    private final JLabel vNilai = Ui.angka("Rp0", 24f, Tema.TEKS), kNilai = Ui.teks(" ", 12f, Tema.TEKS_2);
    private final JLabel vHari = Ui.angka("0", 24f, Tema.TEKS), kHari = Ui.teks(" ", 12f, Tema.TEKS_2);
    private final JLabel vRestok = Ui.angka("0", 24f, Tema.UTAMA), kRestok = Ui.teks(" ", 12f, Tema.TEKS_2);
    private final GrafikBatang grafik = new GrafikBatang(new String[]{"Masuk", "Keluar"}, Tema.HIJAU, Tema.MERAH);
    private final JPanel restok = Ui.transparan(null);
    private final JPanel aktivitas = Ui.transparan(null);
    private final DefaultTableModel mTop = new DefaultTableModel(new String[]{"Kode", "Barang", "Keluar"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };

    public HalamanDasbor(Konteks k, MainFrame frame) {
        super(new BorderLayout());
        this.k = k;
        this.frame = frame;
        Ui.Tombol masuk = Ui.tombol("Barang masuk", "masuk", Ui.Jenis.GARIS);
        masuk.addActionListener(e -> frame.buka("masuk"));
        Ui.Tombol keluar = Ui.tombol("Barang keluar", "keluar", Ui.Jenis.UTAMA);
        keluar.addActionListener(e -> frame.buka("keluar"));
        add(MainFrame.kepala("Lembar 01 / Ringkasan", "Dasbor gudang", Waktu.HARI.format(LocalDate.now(k.jam)), masuk, keluar), BorderLayout.NORTH);

        JPanel isi = Ui.transparan(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        JPanel stat = Ui.transparan(new GridLayout(1, 4, 14, 0));
        stat.add(Bagian.stat("Jenis barang aktif", vSku, kSku, Tema.AKSEN));
        stat.add(Bagian.stat("Nilai persediaan", vNilai, kNilai, Tema.HIJAU));
        stat.add(Bagian.stat("Dokumen hari ini", vHari, kHari, Tema.GARIS_TERANG));
        stat.add(Bagian.stat("Perlu restok", vRestok, kRestok, Tema.UTAMA));
        c.gridx = 0; c.gridy = 0; c.gridwidth = 3; c.weightx = 1; c.insets = new Insets(0, 0, 14, 0);
        isi.add(stat, c);

        c.gridy = 1; c.gridwidth = 2; c.weightx = 0.66; c.weighty = 0.5; c.insets = new Insets(0, 0, 14, 14);
        isi.add(Bagian.panel("Unit masuk vs keluar, 14 hari terakhir", grafik, grafik.legenda()), c);
        restok.setLayout(new BoxLayout(restok, BoxLayout.Y_AXIS));
        Ui.Tombol lihat = Ui.ikonSaja("arrow", "Lihat analisis", Ui.Jenis.HANTU);
        lihat.addActionListener(e -> frame.buka("analisis"));
        c.gridx = 2; c.gridwidth = 1; c.weightx = 0.34; c.insets = new Insets(0, 0, 14, 0);
        isi.add(Bagian.panel("Perlu restok", Ui.gulir(restok), lihat), c);

        aktivitas.setLayout(new BoxLayout(aktivitas, BoxLayout.Y_AXIS));
        c.gridx = 0; c.gridy = 2; c.gridwidth = 2; c.weighty = 0.5; c.insets = new Insets(0, 0, 0, 14);
        isi.add(Bagian.panel("Aktivitas terbaru", Ui.gulir(aktivitas)), c);
        JTable t = new JTable(mTop);
        Ui.rapikanTabel(t);
        t.setShowVerticalLines(false);
        Ui.lebarKolom(t, 80, 200, 70);
        Ui.kolomKode(t, 0);
        Ui.kolomAngka(t, 2);
        c.gridx = 2; c.gridwidth = 1; c.insets = new Insets(0, 0, 0, 0);
        isi.add(Bagian.panel("Paling sering keluar (30 hari)", Ui.gulir(t)), c);
        add(isi, BorderLayout.CENTER);
    }

    @Override
    public void muat() {
        LocalDate h = LocalDate.now(k.jam);
        LaporanService.Dasbor d = k.laporan.dasbor(h);
        vSku.setText(Rupiah.ribuan(d.sku()));
        kSku.setText(Rupiah.ribuan(d.totalUnit()) + " unit di rak");
        vNilai.setText(Rupiah.format(d.nilai()));
        kNilai.setText("stok x harga satuan");
        vHari.setText(String.valueOf(d.masukHariIni() + d.keluarHariIni()));
        kHari.setText(d.masukHariIni() + " masuk · " + d.keluarHariIni() + " keluar");
        vRestok.setText(String.valueOf(d.menipis()));
        kRestok.setText(d.habis() + " barang habis total");

        Map<LocalDate, long[]> g = k.laporan.harian(h.minusDays(13), h);
        List<String> lab = new ArrayList<>();
        DateTimeFormatter f = DateTimeFormatter.ofPattern("d/M");
        for (LocalDate x : g.keySet()) lab.add(x.format(f));
        grafik.isi(lab, new ArrayList<>(g.values()), 1);

        restok.removeAll();
        List<Barang> tipis = k.barang.menipis();
        if (tipis.isEmpty()) restok.add(Ui.teks("Semua stok di atas batas minimum.", 13f, Tema.TEKS_2));
        for (Barang b : tipis) {
            JPanel row = Ui.transparan(new BorderLayout(8, 0));
            row.setBorder(new EmptyBorder(5, 0, 5, 4));
            JLabel n = new JLabel("<html><font color='#79D4F7'>" + b.kode() + "</font>&nbsp; " + esc(b.nama()) + "</html>");
            n.setFont(Tema.reg(12.5f));
            n.setForeground(Tema.TEKS);
            row.add(n, BorderLayout.CENTER);
            row.add(Ui.lencana(b.stok() == 0 ? "Habis" : b.stok() + "/" + b.stokMin(), b.stok() == 0 ? Tema.MERAH : Tema.UTAMA), BorderLayout.EAST);
            row.setAlignmentX(0f);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
            restok.add(row);
        }
        restok.revalidate();

        aktivitas.removeAll();
        for (Dokumen x : k.dokumen.terbaru(10)) {
            JPanel row = Ui.transparan(new BorderLayout(12, 0));
            row.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0x24507F)), new EmptyBorder(6, 0, 6, 4)));
            Color w = x.batal() ? Tema.TEKS_2 : x.jenis().equals(Dokumen.MASUK) ? Tema.HIJAU : x.jenis().equals(Dokumen.KELUAR) ? Tema.MERAH : Tema.AKSEN;
            String ik = x.jenis().equals(Dokumen.MASUK) ? "masuk" : x.jenis().equals(Dokumen.KELUAR) ? "keluar" : "clipboard";
            JLabel ikon = new JLabel(Ikon.of(ik, 18, w));
            row.add(ikon, BorderLayout.WEST);
            String pihak = x.pihak() == null ? (x.catatan() == null ? "" : x.catatan()) : x.pihak();
            JLabel teks = new JLabel("<html><b>" + x.nomor() + "</b>&nbsp;&nbsp;" + esc(pihak) + (x.batal() ? " <font color='#FF8566'>(batal)</font>" : "") + "</html>");
            teks.setFont(Tema.reg(12.5f));
            teks.setForeground(Tema.TEKS);
            row.add(teks, BorderLayout.CENTER);
            JLabel kanan = new JLabel(x.jumlahBaris() + " baris · " + Waktu.tampil(x.dibuatPada()) + " · " + x.dibuatOleh());
            kanan.setFont(Tema.mono(10.5f));
            kanan.setForeground(Tema.TEKS_2);
            row.add(kanan, BorderLayout.EAST);
            row.setAlignmentX(0f);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
            aktivitas.add(row);
        }
        aktivitas.revalidate();

        mTop.setRowCount(0);
        for (LaporanService.Peringkat p : k.laporan.palingKeluar(h.minusDays(29), h, 10)) {
            mTop.addRow(new Object[]{p.kode(), p.nama(), p.qty()});
        }
        repaint();
    }

    static String esc(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
