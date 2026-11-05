package id.papicode.gudang.ui.halaman;

import id.papicode.gudang.Konteks;
import id.papicode.gudang.model.*;
import id.papicode.gudang.ui.*;
import id.papicode.gudang.util.Rupiah;
import id.papicode.gudang.util.Waktu;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Stok opname: cetak lembar hitung, input hasil hitung fisik, dan berita acara selisih. */
public class HalamanOpname extends MainFrame.Lembar implements MainFrame.Halaman {

    private final Konteks k;
    private final MainFrame frame;
    private final PilihPeriode periode;
    private final List<Dokumen> data = new ArrayList<>();
    private final AbstractTableModel model;
    private final JTable tabel;
    private final JPanel detail = Ui.transparan(new BorderLayout(0, 10));
    private final JComboBox<Object> fKategori = new JComboBox<>();
    private final JComboBox<String> fRak = new JComboBox<>();

    public HalamanOpname(Konteks k, MainFrame frame) {
        super(new BorderLayout());
        this.k = k;
        this.frame = frame;
        periode = new PilihPeriode(k.jam, 3);
        periode.onChange(this::muat);

        Ui.Tombol lembar = Ui.tombol("Cetak lembar hitung", "printer", Ui.Jenis.GARIS);
        lembar.addActionListener(e -> cetakLembarHitung());
        Ui.Tombol mulai = Ui.tombol("Input hasil opname", "clipboard", Ui.Jenis.UTAMA);
        mulai.setEnabled(frame.pengguna().admin());
        mulai.setToolTipText(frame.pengguna().admin() ? null : "Hanya admin");
        mulai.addActionListener(e -> mulai());
        add(MainFrame.kepala("Lembar 04 / Pemeriksaan", "Stok opname",
                "1. Cetak lembar hitung  2. Hitung fisik di rak  3. Input hasilnya, selisih otomatis tercatat.", lembar, mulai), BorderLayout.NORTH);

        model = new AbstractTableModel() {
            final String[] kol = {"Nomor", "Tanggal", "Barang dihitung", "Selisih unit", "Selisih nilai", "Petugas"};
            public int getRowCount() { return data.size(); }
            public int getColumnCount() { return kol.length; }
            public String getColumnName(int c) { return kol[c]; }
            public Object getValueAt(int r, int c) {
                Dokumen d = data.get(r);
                return switch (c) {
                    case 0 -> d.nomor();
                    case 1 -> Waktu.tampilTgl(LocalDate.parse(d.tanggal()));
                    case 2 -> d.jumlahBaris();
                    case 3 -> d.totalQty();
                    case 4 -> d.totalNilai();
                    default -> d.dibuatOleh();
                };
            }
        };
        tabel = new JTable(model);
        Ui.rapikanTabel(tabel);
        Ui.lebarKolom(tabel, 140, 110, 110, 100, 110, 130);
        Ui.kolomKode(tabel, 0);
        Ui.kolomAngka(tabel, 2, 3, 4);
        tabel.getSelectionModel().addListSelectionListener(e -> { if (!e.getValueIsAdjusting()) tampilDetail(); });

        JPanel kiri = Ui.transparan(new BorderLayout(0, 10));
        JPanel filter = Ui.transparan(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filter.add(periode);
        kiri.add(filter, BorderLayout.NORTH);
        kiri.add(Bagian.tabel(tabel), BorderLayout.CENTER);
        detail.setPreferredSize(new Dimension(460, 0));
        JPanel tengah = Ui.transparan(new BorderLayout(14, 0));
        tengah.add(kiri, BorderLayout.CENTER);
        tengah.add(detail, BorderLayout.EAST);
        add(tengah, BorderLayout.CENTER);
    }

    @Override
    public void muat() {
        data.clear();
        data.addAll(k.dokumen.daftar(Dokumen.OPNAME, periode.dari(), periode.sampai(), null));
        model.fireTableDataChanged();
        if (!data.isEmpty()) tabel.setRowSelectionInterval(0, 0);
        tampilDetail();
    }

    private void tampilDetail() {
        detail.removeAll();
        int r = tabel.getSelectedRow();
        if (r < 0) {
            Ui.Kartu kosong = new Ui.Kartu(new GridBagLayout());
            kosong.add(Ui.teks("<html><center>Belum ada opname di periode ini.<br>Mulai dengan mencetak lembar hitung.</center></html>", 13.5f, Tema.TEKS_2));
            detail.add(kosong);
        } else {
            Dokumen d = data.get(r);
            List<ItemDokumen> item = k.dokumen.item(d.id());
            List<ItemDokumen> beda = item.stream().filter(i -> i.selisih() != 0).toList();
            Ui.Kartu c = new Ui.Kartu(new BorderLayout(0, 10));
            c.setBorder(new EmptyBorder(16, 18, 16, 18));
            JPanel atas = Ui.transparan(new GridLayout(0, 1, 0, 4));
            atas.add(Ui.angka(d.nomor(), 18f, Tema.AKSEN));
            atas.add(Ui.teks(item.size() + " barang dihitung, " + beda.size() + " berselisih", 13f, Tema.TEKS));
            if (d.catatan() != null && !d.catatan().isBlank()) atas.add(Ui.teks(d.catatan(), 12.5f, Tema.TEKS_2));
            c.add(atas, BorderLayout.NORTH);
            String[] kol = {"Kode", "Barang", "Sistem", "Fisik", "Selisih"};
            Object[][] isi = new Object[beda.size()][];
            for (int i = 0; i < beda.size(); i++) {
                ItemDokumen it = beda.get(i);
                isi[i] = new Object[]{it.kode(), it.nama(), it.stokSistem(), it.qty(), it.selisih()};
            }
            JTable t = new JTable(isi, kol) { @Override public boolean isCellEditable(int a, int b) { return false; } };
            Ui.rapikanTabel(t);
            Ui.lebarKolom(t, 82, 150, 56, 56, 56);
            Ui.kolomKode(t, 0);
            Ui.kolomAngka(t, 2, 3);
            t.getColumnModel().getColumn(4).setCellRenderer(rendererSelisih());
            JScrollPane sp = Ui.gulir(t);
            sp.setBorder(BorderFactory.createLineBorder(Tema.GARIS));
            c.add(sp, BorderLayout.CENTER);
            c.add(Ui.label("Selisih nilai " + Rupiah.format(d.totalNilai())), BorderLayout.SOUTH);
            detail.add(c, BorderLayout.CENTER);
            Ui.Tombol cetak = Ui.tombol("Cetak berita acara", "printer", Ui.Jenis.UTAMA);
            cetak.addActionListener(e -> Pratinjau.tampil(this, HalamanDokumen.lembar(k, d, item), d.nomor(), false));
            detail.add(cetak, BorderLayout.SOUTH);
        }
        detail.revalidate();
        detail.repaint();
    }

    static DefaultTableCellRenderer rendererSelisih() {
        return new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable tb, Object v, boolean s, boolean f, int r, int c) {
                int n = v instanceof Integer i ? i : 0;
                super.getTableCellRendererComponent(tb, (n > 0 ? "+" : "") + n, s, false, r, c);
                setHorizontalAlignment(RIGHT);
                setFont(Tema.monoBold(12.5f));
                setForeground(n < 0 ? Tema.MERAH : n > 0 ? Tema.HIJAU : Tema.TEKS_2);
                return this;
            }
        };
    }

    private JPanel pilihCakupan() {
        fKategori.removeAllItems();
        fKategori.addItem("Semua kategori");
        k.barang.kategori().forEach(fKategori::addItem);
        fRak.removeAllItems();
        fRak.addItem("Semua rak");
        for (String r : k.barang.daftarLokasi()) fRak.addItem("Rak " + r);
        JPanel p = Ui.transparan(new GridLayout(1, 2, 10, 0));
        p.add(Ui.baris("Kategori", fKategori));
        p.add(Ui.baris("Rak", fRak));
        return p;
    }

    private List<Barang> barangCakupan() {
        Long kat = fKategori.getSelectedItem() instanceof Kategori c ? c.id() : null;
        String rak = fRak.getSelectedIndex() > 0 ? ((String) fRak.getSelectedItem()).substring(4) : null;
        List<Barang> l = new ArrayList<>(k.barang.daftar(null, kat, rak, false));
        l.sort((a, b) -> String.valueOf(a.lokasi()).compareTo(String.valueOf(b.lokasi())));
        return l;
    }

    /** Lembar kosong untuk petugas: stok sistem sengaja tidak dicetak agar hitungan jujur. */
    private void cetakLembarHitung() {
        JPanel f = Ui.transparan(new BorderLayout(0, 10));
        f.add(Ui.teks("<html>Lembar diurutkan per lokasi rak. Kolom stok sistem sengaja tidak dicetak<br>agar petugas benar-benar menghitung fisik barang.</html>", 13f, Tema.TEKS_2), BorderLayout.NORTH);
        f.add(pilihCakupan(), BorderLayout.CENTER);
        boolean[] ok = {false};
        Kotak.form(this, "Cetak lembar hitung", f, "Pratinjau", () -> ok[0] = true);
        if (!ok[0]) return;
        List<Barang> l = barangCakupan();
        LembarCetak c = new LembarCetak("LEMBAR HITUNG STOK OPNAME", k.pengaturan.get("nama_perusahaan"),
                k.pengaturan.get("nama_gudang") + " - " + k.pengaturan.get("alamat"));
        c.info("Cakupan", fKategori.getSelectedItem() + " / " + fRak.getSelectedItem()).info("Tanggal hitung", "....................")
         .info("Jumlah barang", String.valueOf(l.size())).info("Petugas", "....................");
        c.kolom(new String[]{"No", "Rak", "Kode", "Nama barang", "Satuan", "Hitung fisik", "Paraf"},
                new double[]{0.05, 0.1, 0.12, 0.4, 0.09, 0.14, 0.1},
                new boolean[]{true, false, false, false, false, false, false});
        int no = 1;
        for (Barang b : l) c.baris(String.valueOf(no++), b.lokasi(), b.kode(), b.nama(), b.satuan(), "", "");
        c.tandaTangan(k.pengaturan.get("kota") + ", ....................", new String[]{"Petugas hitung", ""}, new String[]{"Saksi", ""});
        Pratinjau.tampil(this, c, "Lembar hitung opname", false);
    }

    private void mulai() {
        JPanel f = Ui.transparan(new BorderLayout(0, 10));
        f.add(Ui.teks("Pilih barang yang dihitung pada opname kali ini.", 13f, Tema.TEKS_2), BorderLayout.NORTH);
        f.add(pilihCakupan(), BorderLayout.CENTER);
        boolean[] ok = {false};
        Kotak.form(this, "Mulai input opname", f, "Muat barang", () -> ok[0] = true);
        if (!ok[0]) return;
        List<Barang> l = barangCakupan();
        if (l.isEmpty()) { frame.kabar("Tidak ada barang di cakupan ini"); return; }
        Dokumen d = DialogOpname.tampil(this, k, frame, l);
        if (d != null) {
            frame.kabar("Opname " + d.nomor() + " tersimpan, stok sudah disamakan");
            muat();
        }
    }
}
