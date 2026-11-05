package id.papicode.gudang.ui.halaman;

import id.papicode.gudang.Konteks;
import id.papicode.gudang.model.Barang;
import id.papicode.gudang.model.Kategori;
import id.papicode.gudang.service.Gagal;
import id.papicode.gudang.ui.*;
import id.papicode.gudang.util.Csv;
import id.papicode.gudang.util.Rupiah;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** Master barang: kode, lokasi rak, stok minimum, harga satuan. Cetak label barcode dan ekspor CSV. */
public class HalamanBarang extends MainFrame.Lembar implements MainFrame.Halaman {

    private final Konteks k;
    private final MainFrame frame;
    private final JTextField cari = Ui.cari("Cari kode atau nama");
    private final JComboBox<Object> fKategori = new JComboBox<>();
    private final JComboBox<String> fRak = new JComboBox<>();
    private final JCheckBox fTipis = new JCheckBox("Perlu restok");
    private final JCheckBox fNonaktif = new JCheckBox("Tampilkan nonaktif");
    private final Model model = new Model();
    private final JTable tabel = new JTable(model);
    private final JLabel ringkas = Ui.label(" ");
    private boolean memuat;

    public HalamanBarang(Konteks k, MainFrame frame) {
        super(new BorderLayout());
        this.k = k;
        this.frame = frame;
        boolean admin = frame.pengguna().admin();

        Ui.Tombol tambah = Ui.tombol("Barang baru", "plus", Ui.Jenis.UTAMA);
        tambah.addActionListener(e -> form(null));
        Ui.Tombol kat = Ui.tombol("Kategori", "tag", Ui.Jenis.GARIS);
        kat.addActionListener(e -> kelolaKategori());
        Ui.Tombol label = Ui.tombol("Cetak label", "barcode", Ui.Jenis.GARIS);
        label.addActionListener(e -> cetakLabel());
        Ui.Tombol ekspor = Ui.tombol("CSV", "download", Ui.Jenis.GARIS);
        ekspor.addActionListener(e -> ekspor());
        add(admin ? MainFrame.kepala("Lembar 05 / Data master", "Daftar barang", "Kode, lokasi rak, batas minimum, dan nilai persediaan tiap barang.", ekspor, label, kat, tambah)
                  : MainFrame.kepala("Lembar 05 / Data master", "Daftar barang", "Lihat stok dan lokasi rak. Perubahan data hanya oleh admin.", ekspor, label), BorderLayout.NORTH);

        JPanel tengah = Ui.transparan(new BorderLayout(0, 10));
        JPanel filter = Ui.transparan(new BorderLayout());
        JPanel kiri = Ui.transparan(new FlowLayout(FlowLayout.LEFT, 8, 0));
        cari.setPreferredSize(new Dimension(260, 38));
        fKategori.setPreferredSize(new Dimension(190, 38));
        fRak.setPreferredSize(new Dimension(120, 38));
        kiri.add(cari);
        kiri.add(fKategori);
        kiri.add(fRak);
        fTipis.setOpaque(false);
        fNonaktif.setOpaque(false);
        kiri.add(fTipis);
        kiri.add(fNonaktif);
        filter.add(kiri, BorderLayout.WEST);
        JPanel aksi = Ui.transparan(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        Ui.Tombol kartu = Ui.ikonSaja("file", "Kartu stok barang ini", Ui.Jenis.GARIS);
        kartu.addActionListener(e -> {
            Barang b = terpilih();
            if (b != null) { frame.buka("kartu"); frame.<HalamanKartuStok>halaman("kartu").tampilkan(b); }
        });
        aksi.add(kartu);
        if (admin) {
            Ui.Tombol ubah = Ui.ikonSaja("pencil", "Ubah barang", Ui.Jenis.GARIS);
            ubah.addActionListener(e -> { Barang b = terpilih(); if (b != null) form(b); });
            Ui.Tombol hapus = Ui.ikonSaja("trash", "Hapus / nonaktifkan", Ui.Jenis.GARIS);
            hapus.addActionListener(e -> hapus());
            aksi.add(ubah);
            aksi.add(hapus);
        }
        filter.add(aksi, BorderLayout.EAST);
        tengah.add(filter, BorderLayout.NORTH);

        Ui.rapikanTabel(tabel);
        tabel.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        tabel.setAutoCreateRowSorter(true);
        Ui.lebarKolom(tabel, 90, 270, 140, 80, 70, 70, 60, 100, 120, 90);
        Ui.kolomKode(tabel, 0);
        Ui.kolomAngka(tabel, 5, 6, 7, 8);
        tabel.getColumnModel().getColumn(9).setCellRenderer(new Bagian.Pil(Bagian.WARNA_STATUS));
        tabel.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && admin) { Barang b = terpilih(); if (b != null) form(b); }
            }
        });
        tengah.add(Bagian.tabel(tabel), BorderLayout.CENTER);
        ringkas.setBorder(new EmptyBorder(4, 2, 0, 0));
        tengah.add(ringkas, BorderLayout.SOUTH);
        add(tengah, BorderLayout.CENTER);

        cari.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { muatTabel(); }
            public void removeUpdate(DocumentEvent e) { muatTabel(); }
            public void changedUpdate(DocumentEvent e) { muatTabel(); }
        });
        fKategori.addActionListener(e -> { if (!memuat) muatTabel(); });
        fRak.addActionListener(e -> { if (!memuat) muatTabel(); });
        fTipis.addActionListener(e -> muatTabel());
        fNonaktif.addActionListener(e -> muatTabel());
    }

    @Override
    public void muat() {
        memuat = true;
        Object kat = fKategori.getSelectedItem(), rak = fRak.getSelectedItem();
        fKategori.removeAllItems();
        fKategori.addItem("Semua kategori");
        k.barang.kategori().forEach(fKategori::addItem);
        if (kat != null) fKategori.setSelectedItem(kat);
        fRak.removeAllItems();
        fRak.addItem("Semua rak");
        for (String r : k.barang.daftarLokasi()) fRak.addItem("Rak " + r);
        if (rak != null) fRak.setSelectedItem(rak);
        memuat = false;
        muatTabel();
    }

    private void muatTabel() {
        Long kat = fKategori.getSelectedItem() instanceof Kategori c ? c.id() : null;
        String rak = fRak.getSelectedIndex() > 0 ? ((String) fRak.getSelectedItem()).substring(4) : null;
        List<Barang> l = k.barang.daftar(cari.getText(), kat, rak, fNonaktif.isSelected());
        if (fTipis.isSelected()) l.removeIf(b -> !b.menipis());
        model.isi(l);
        long nilai = l.stream().mapToLong(Barang::nilai).sum();
        ringkas.setText(l.size() + " barang  ·  " + Rupiah.ribuan(l.stream().mapToLong(Barang::stok).sum()) + " unit  ·  nilai " + Rupiah.format(nilai));
    }

    private Barang terpilih() {
        int r = tabel.getSelectedRow();
        if (r < 0) { frame.kabar("Pilih barang di tabel dulu"); return null; }
        return model.data.get(tabel.convertRowIndexToModel(r));
    }

    private void form(Barang b) {
        boolean baru = b == null;
        JComboBox<Object> kat = new JComboBox<>();
        kat.addItem("Tanpa kategori");
        k.barang.kategori().forEach(kat::addItem);
        JTextField kode = Ui.field("Misal SMN-005");
        JTextField nama = Ui.field("Nama lengkap barang");
        JTextField satuan = Ui.field("sak, batang, pcs, roll");
        JTextField lokasi = Ui.field("Misal A-01-03");
        JTextField harga = Ui.field("0");
        JTextField min = Ui.field("0");
        JTextField awal = Ui.field("0");
        JCheckBox aktif = new JCheckBox("Aktif (bisa dipakai di dokumen)", true);
        aktif.setOpaque(false);
        for (JTextField f : new JTextField[]{kode, harga, min, awal, lokasi}) f.setFont(Tema.mono(13f));
        Ui.Tombol saran = Ui.tombol("Kode otomatis", null, Ui.Jenis.GARIS);
        saran.addActionListener(e -> {
            String pre = kode.getText().contains("-") ? kode.getText().substring(0, kode.getText().indexOf('-')) : kode.getText();
            if (pre.isBlank() && kat.getSelectedItem() instanceof Kategori c) pre = c.nama().replaceAll("[^A-Za-z]", "").substring(0, Math.min(3, c.nama().replaceAll("[^A-Za-z]", "").length()));
            kode.setText(k.barang.saranKode(pre));
        });
        if (!baru) {
            kode.setText(b.kode());
            nama.setText(b.nama());
            satuan.setText(b.satuan());
            lokasi.setText(b.lokasi() == null ? "" : b.lokasi());
            harga.setText(Rupiah.ribuan(b.harga()));
            min.setText(String.valueOf(b.stokMin()));
            aktif.setSelected(b.aktif());
            for (int i = 0; i < kat.getItemCount(); i++) {
                if (kat.getItemAt(i) instanceof Kategori c && b.kategoriId() != null && c.id() == b.kategoriId()) kat.setSelectedIndex(i);
            }
        } else satuan.setText("pcs");

        JPanel f = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL; c.weightx = 1; c.insets = new Insets(0, 0, 12, 12);
        JPanel kodeBaris = Ui.transparan(new BorderLayout(6, 0));
        kodeBaris.add(kode);
        kodeBaris.add(saran, BorderLayout.EAST);
        c.gridx = 0; c.gridy = 0; f.add(Ui.baris("Kategori", kat), c);
        c.gridx = 1; f.add(Ui.baris("Kode barang", kodeBaris), c);
        c.gridx = 0; c.gridy = 1; c.gridwidth = 2; f.add(Ui.baris("Nama barang", nama), c);
        c.gridwidth = 1;
        c.gridx = 0; c.gridy = 2; f.add(Ui.baris("Satuan", satuan), c);
        c.gridx = 1; f.add(Ui.baris("Lokasi rak", lokasi), c);
        c.gridx = 0; c.gridy = 3; f.add(Ui.baris("Harga satuan (Rp)", harga), c);
        c.gridx = 1; f.add(Ui.baris("Stok minimum", min), c);
        if (baru) { c.gridx = 0; c.gridy = 4; f.add(Ui.baris("Saldo awal", awal), c); }
        c.gridx = 0; c.gridy = 5; c.gridwidth = 2; f.add(aktif, c);
        if (!baru) {
            c.gridy = 6;
            f.add(Ui.teks("<html>Stok sekarang <b>" + b.stok() + " " + b.satuan() + "</b>. Stok hanya berubah lewat dokumen masuk, keluar, atau opname.</html>", 12.5f, Tema.TEKS_2), c);
        }
        f.setPreferredSize(new Dimension(560, f.getPreferredSize().height));
        Kotak.form(this, baru ? "Barang baru" : "Ubah barang", f, "Simpan", () -> {
            Long kid = kat.getSelectedItem() instanceof Kategori kk ? kk.id() : null;
            Barang data = new Barang(baru ? 0 : b.id(), kode.getText(), nama.getText(), kid, null, satuan.getText(), lokasi.getText(),
                    angka(harga.getText(), "Harga"), 0, (int) angka(min.getText(), "Stok minimum"), aktif.isSelected());
            Barang hasil = k.barang.simpan(frame.aktor(), data, baru ? (int) angka(awal.getText(), "Saldo awal") : 0);
            muat();
            frame.kabar((baru ? "Barang ditambahkan: " : "Barang disimpan: ") + hasil.kode());
            return true;
        });
    }

    private void hapus() {
        Barang b = terpilih();
        if (b == null) return;
        if (!Kotak.konfirmasi(this, "Hapus " + b.kode() + "?",
                "Barang yang sudah pernah dipakai di dokumen hanya dinonaktifkan agar kartu stok dan laporan tetap utuh.", "Hapus", true)) return;
        Kotak.aman(this, () -> {
            boolean permanen = k.barang.hapus(frame.aktor(), b.id());
            muatTabel();
            frame.kabar(permanen ? "Barang dihapus" : "Barang dinonaktifkan");
        });
    }

    private void cetakLabel() {
        List<Barang> pilih = new ArrayList<>();
        int[] rows = tabel.getSelectedRows();
        if (rows.length == 0) for (Barang b : model.data) pilih.add(b);
        else for (int r : rows) pilih.add(model.data.get(tabel.convertRowIndexToModel(r)));
        if (pilih.isEmpty()) { frame.kabar("Tidak ada barang untuk dicetak"); return; }
        JTextField jml = Ui.field("1");
        jml.setText(rows.length == 1 ? "24" : "1");
        jml.setFont(Tema.mono(13f));
        JPanel f = Ui.transparan(new BorderLayout(0, 10));
        f.add(Ui.teks("<html>" + (rows.length == 0 ? "Semua barang di tabel (" + pilih.size() + " jenis)." : pilih.size() + " barang terpilih.")
                + "<br>Kertas A4, 3 x 8 label ukuran 70 x 37 mm.</html>", 13.5f, Tema.TEKS_2), BorderLayout.NORTH);
        f.add(Ui.baris("Jumlah label per barang", jml), BorderLayout.CENTER);
        int[] n = {0};
        Kotak.form(this, "Cetak label barcode", f, "Pratinjau", () -> {
            n[0] = (int) angka(jml.getText(), "Jumlah label");
            if (n[0] < 1 || n[0] > 240) throw new Gagal("Jumlah label 1-240 per barang.");
            return true;
        });
        if (n[0] == 0) return;
        List<Barang> semua = new ArrayList<>();
        for (Barang b : pilih) for (int i = 0; i < n[0]; i++) semua.add(b);
        LabelBarcode l = new LabelBarcode(semua, k.pengaturan.get("nama_perusahaan"));
        Pratinjau.tampil(this, "Label barcode", l, l.jumlahHalaman(), false);
    }

    private void kelolaKategori() {
        DefaultListModel<Kategori> lm = new DefaultListModel<>();
        k.barang.kategori().forEach(lm::addElement);
        JList<Kategori> list = new JList<>(lm);
        list.setFixedCellHeight(32);
        JScrollPane sp = Ui.gulir(list);
        sp.setPreferredSize(new Dimension(360, 260));
        sp.setBorder(BorderFactory.createLineBorder(Tema.GARIS));
        Ui.Tombol tambah = Ui.tombol("Tambah", "plus", Ui.Jenis.GARIS);
        Ui.Tombol hapus = Ui.tombol("Hapus", "trash", Ui.Jenis.BAHAYA);
        tambah.addActionListener(e -> {
            String n = Kotak.input(this, "Kategori baru", "Nama kategori", "");
            if (n != null) Kotak.aman(this, () -> lm.addElement(k.barang.tambahKategori(frame.aktor(), n)));
        });
        hapus.addActionListener(e -> {
            Kategori c = list.getSelectedValue();
            if (c != null && Kotak.konfirmasi(this, "Hapus kategori " + c.nama() + "?", "Barang di kategori ini menjadi tanpa kategori.", "Hapus", true)) {
                Kotak.aman(this, () -> { k.barang.hapusKategori(frame.aktor(), c.id()); lm.removeElement(c); });
            }
        });
        JPanel f = Ui.transparan(new BorderLayout(0, 10));
        f.add(sp);
        JPanel t = Ui.transparan(new FlowLayout(FlowLayout.LEFT, 0, 0));
        t.add(tambah);
        t.add(Box.createHorizontalStrut(8));
        t.add(hapus);
        f.add(t, BorderLayout.SOUTH);
        Kotak.form(this, "Kategori barang", f, "Selesai", () -> true);
        muat();
    }

    private void ekspor() {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("daftar-barang.csv"));
        fc.setFileFilter(new FileNameExtensionFilter("CSV (Excel)", "csv"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File file = fc.getSelectedFile().getName().toLowerCase().endsWith(".csv") ? fc.getSelectedFile() : new File(fc.getSelectedFile() + ".csv");
        Kotak.aman(this, () -> {
            List<List<Object>> rows = new ArrayList<>();
            for (Barang b : model.data) {
                rows.add(List.of(b.kode(), b.nama(), b.kategori() == null ? "" : b.kategori(), b.lokasi() == null ? "" : b.lokasi(),
                        b.satuan(), (long) b.stok(), (long) b.stokMin(), b.harga(), b.nilai()));
            }
            try {
                Csv.tulis(file.toPath(), List.of("Kode", "Nama", "Kategori", "Rak", "Satuan", "Stok", "Stok min", "Harga", "Nilai"), rows);
            } catch (java.io.IOException e) { throw new Gagal("File tidak bisa disimpan: " + e.getMessage()); }
            frame.kabar(rows.size() + " barang diekspor ke " + file.getName());
        });
    }

    static long angka(String s, String nama) {
        try { return Rupiah.parse(s); } catch (IllegalArgumentException e) { throw new Gagal(nama + " harus berupa angka."); }
    }

    class Model extends AbstractTableModel {
        final List<Barang> data = new ArrayList<>();
        final String[] kol = {"Kode", "Nama barang", "Kategori", "Rak", "Satuan", "Stok", "Min", "Harga", "Nilai", "Status"};

        void isi(List<Barang> l) { data.clear(); data.addAll(l); fireTableDataChanged(); }
        @Override public int getRowCount() { return data.size(); }
        @Override public int getColumnCount() { return kol.length; }
        @Override public String getColumnName(int c) { return kol[c]; }
        @Override public Class<?> getColumnClass(int c) { return c == 5 || c == 6 ? Integer.class : c == 7 || c == 8 ? Long.class : String.class; }
        @Override public Object getValueAt(int r, int c) {
            Barang b = data.get(r);
            return switch (c) {
                case 0 -> b.kode();
                case 1 -> b.nama();
                case 2 -> b.kategori() == null ? "-" : b.kategori();
                case 3 -> b.lokasi() == null ? "-" : b.lokasi();
                case 4 -> b.satuan();
                case 5 -> b.stok();
                case 6 -> b.stokMin();
                case 7 -> b.harga();
                case 8 -> b.nilai();
                default -> !b.aktif() ? "Nonaktif" : b.stok() == 0 ? "Habis" : b.menipis() ? "Menipis" : "Aman";
            };
        }
    }
}
