package id.papicode.gudang.ui.halaman;

import id.papicode.gudang.Konteks;
import id.papicode.gudang.model.Dokumen;
import id.papicode.gudang.model.ItemDokumen;
import id.papicode.gudang.ui.*;
import id.papicode.gudang.util.Rupiah;
import id.papicode.gudang.util.Waktu;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Daftar dokumen barang masuk atau keluar, detail baris, cetak bukti / surat jalan, dan pembatalan. */
public class HalamanDokumen extends MainFrame.Lembar implements MainFrame.Halaman {

    private final Konteks k;
    private final MainFrame frame;
    private final String jenis;
    private final boolean masuk;
    private final PilihPeriode periode;
    private final JTextField cari = Ui.cari("Cari nomor, pihak, referensi");
    private final Model model = new Model();
    private final JTable tabel = new JTable(model);
    private final JLabel ringkas = Ui.label(" ");
    private final JPanel detail = Ui.transparan(new BorderLayout(0, 10));

    public HalamanDokumen(Konteks k, MainFrame frame, String jenis) {
        super(new BorderLayout());
        this.k = k;
        this.frame = frame;
        this.jenis = jenis;
        this.masuk = jenis.equals(Dokumen.MASUK);
        periode = new PilihPeriode(k.jam, 3);
        periode.onChange(this::muat);

        Ui.Tombol baru = Ui.tombol(masuk ? "Catat barang masuk" : "Catat barang keluar", "plus", Ui.Jenis.UTAMA);
        baru.addActionListener(e -> {
            Dokumen d = DialogDokumen.tampil(this, k, frame, jenis);
            if (d != null) {
                muat();
                frame.kabar("Dokumen " + d.nomor() + " tersimpan, stok diperbarui");
                pilih(d.id());
            }
        });
        add(MainFrame.kepala(masuk ? "Lembar 02 / Penerimaan" : "Lembar 03 / Pengeluaran", masuk ? "Barang masuk" : "Barang keluar",
                masuk ? "Penerimaan barang dari supplier. Stok bertambah saat dokumen disimpan."
                      : "Pengiriman ke proyek, cabang, atau pelanggan. Stok berkurang dan surat jalan bisa dicetak.", baru), BorderLayout.NORTH);

        JPanel kiri = Ui.transparan(new BorderLayout(0, 10));
        JPanel filter = Ui.transparan(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filter.add(periode);
        cari.setPreferredSize(new Dimension(230, 38));
        cari.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { muat(); }
            public void removeUpdate(DocumentEvent e) { muat(); }
            public void changedUpdate(DocumentEvent e) { muat(); }
        });
        filter.add(cari);
        kiri.add(filter, BorderLayout.NORTH);
        Ui.rapikanTabel(tabel);
        Ui.lebarKolom(tabel, 135, 72, 200, 105, 48, 58, 110, 70);
        Ui.kolomKode(tabel, 0);
        Ui.kolomAngka(tabel, 4, 5, 6);
        tabel.getColumnModel().getColumn(7).setCellRenderer(new Bagian.Pil(Bagian.WARNA_STATUS));
        tabel.getSelectionModel().addListSelectionListener(e -> { if (!e.getValueIsAdjusting()) tampilDetail(); });
        kiri.add(Bagian.tabel(tabel), BorderLayout.CENTER);
        kiri.add(ringkas, BorderLayout.SOUTH);

        detail.setPreferredSize(new Dimension(420, 0));
        JPanel tengah = Ui.transparan(new BorderLayout(14, 0));
        tengah.add(kiri, BorderLayout.CENTER);
        tengah.add(detail, BorderLayout.EAST);
        add(tengah, BorderLayout.CENTER);
    }

    @Override
    public void muat() {
        long dipilih = tabel.getSelectedRow() >= 0 ? model.data.get(tabel.getSelectedRow()).id() : -1;
        List<Dokumen> l = k.dokumen.daftar(jenis, periode.dari(), periode.sampai(), cari.getText());
        model.isi(l);
        long aktif = l.stream().filter(d -> !d.batal()).count();
        long unit = l.stream().filter(d -> !d.batal()).mapToLong(Dokumen::totalQty).sum();
        long nilai = l.stream().filter(d -> !d.batal()).mapToLong(Dokumen::totalNilai).sum();
        ringkas.setText(aktif + " dokumen sah  ·  " + Rupiah.ribuan(unit) + " unit  ·  nilai " + Rupiah.format(nilai));
        if (dipilih >= 0) pilih(dipilih); else tampilDetail();
    }

    private void pilih(long id) {
        for (int i = 0; i < model.data.size(); i++) {
            if (model.data.get(i).id() == id) { tabel.setRowSelectionInterval(i, i); tabel.scrollRectToVisible(tabel.getCellRect(i, 0, true)); return; }
        }
        tampilDetail();
    }

    private void tampilDetail() {
        detail.removeAll();
        int r = tabel.getSelectedRow();
        if (r < 0) {
            Ui.Kartu kosong = new Ui.Kartu(new GridBagLayout());
            JLabel l = Ui.teks("<html><center>Pilih dokumen di tabel<br>untuk melihat rinciannya.</center></html>", 13.5f, Tema.TEKS_2);
            l.setIcon(Ikon.of(masuk ? "masuk" : "keluar", 34, Tema.GARIS, 1.5f));
            l.setHorizontalTextPosition(SwingConstants.CENTER);
            l.setVerticalTextPosition(SwingConstants.BOTTOM);
            l.setIconTextGap(12);
            kosong.add(l);
            detail.add(kosong);
            detail.revalidate();
            detail.repaint();
            return;
        }
        Dokumen d = model.data.get(r);
        List<ItemDokumen> item = k.dokumen.item(d.id());
        Ui.Kartu c = new Ui.Kartu(new BorderLayout(0, 10));
        c.setBorder(new EmptyBorder(16, 18, 16, 18));
        JPanel atas = Ui.transparan(new BorderLayout(0, 6));
        JPanel no = Ui.transparan(new BorderLayout());
        JLabel nomor = Ui.angka(d.nomor(), 18f, masuk ? Tema.HIJAU : Tema.MERAH);
        no.add(nomor, BorderLayout.WEST);
        no.add(Ui.lencana(d.batal() ? "Batal" : "Aktif", d.batal() ? Tema.MERAH : Tema.HIJAU), BorderLayout.EAST);
        atas.add(no, BorderLayout.NORTH);
        JPanel info = Ui.transparan(new GridLayout(0, 1, 0, 3));
        info.add(infoBaris("Tanggal", Waktu.tampilTgl(LocalDate.parse(d.tanggal()))));
        info.add(infoBaris(masuk ? "Supplier" : "Tujuan", d.pihak()));
        info.add(infoBaris("Referensi", d.referensi() == null || d.referensi().isBlank() ? "-" : d.referensi()));
        info.add(infoBaris("Dicatat", d.dibuatOleh() + ", " + Waktu.tampil(d.dibuatPada())));
        if (d.catatan() != null && !d.catatan().isBlank()) info.add(infoBaris("Catatan", d.catatan()));
        if (d.batal()) info.add(infoBaris("Alasan batal", d.alasanBatal()));
        atas.add(info, BorderLayout.CENTER);
        c.add(atas, BorderLayout.NORTH);

        DefaultTableModel mi = new DefaultTableModel(new String[]{"Kode", "Barang", "Qty"}, 0) {
            @Override public boolean isCellEditable(int r1, int c1) { return false; }
        };
        for (ItemDokumen it : item) mi.addRow(new Object[]{it.kode(), it.nama(), it.qty() + " " + it.satuan()});
        JTable ti = new JTable(mi);
        Ui.rapikanTabel(ti);
        ti.setRowHeight(30);
        Ui.lebarKolom(ti, 76, 190, 100);
        Ui.kolomKode(ti, 0);
        JScrollPane sp = Ui.gulir(ti);
        sp.setBorder(BorderFactory.createLineBorder(Tema.GARIS));
        c.add(sp, BorderLayout.CENTER);
        JLabel tot = Ui.label("Total " + Rupiah.ribuan(d.totalQty()) + " unit  ·  nilai " + Rupiah.format(d.totalNilai()));
        c.add(tot, BorderLayout.SOUTH);
        detail.add(c, BorderLayout.CENTER);

        JPanel tombol = Ui.transparan(new GridLayout(1, 0, 8, 0));
        Ui.Tombol cetak = Ui.tombol(masuk ? "Cetak bukti terima" : "Cetak surat jalan", "printer", Ui.Jenis.UTAMA);
        cetak.addActionListener(e -> Pratinjau.tampil(this, lembar(k, d, item), d.nomor(), false));
        tombol.add(cetak);
        if (frame.pengguna().admin() && !d.batal()) {
            Ui.Tombol batal = Ui.tombol("Batalkan", "xcircle", Ui.Jenis.BAHAYA);
            batal.addActionListener(e -> {
                String alasan = Kotak.input(this, "Batalkan " + d.nomor(), masuk ? "Alasan (stok akan dikurangi kembali)" : "Alasan (stok akan dikembalikan)", "");
                if (alasan != null && Kotak.aman(this, () -> k.dokumen.batal(frame.aktor(), d.id(), alasan))) {
                    frame.kabar(d.nomor() + " dibatalkan, stok sudah disesuaikan");
                    muat();
                }
            });
            tombol.add(batal);
        }
        detail.add(tombol, BorderLayout.SOUTH);
        detail.revalidate();
        detail.repaint();
    }

    private static JComponent infoBaris(String label, String nilai) {
        JPanel p = Ui.transparan(new BorderLayout(10, 0));
        JLabel l = Ui.label(label);
        l.setPreferredSize(new Dimension(92, 18));
        p.add(l, BorderLayout.WEST);
        JLabel v = Ui.teks(nilai == null ? "-" : nilai, 13f, Tema.TEKS);
        p.add(v, BorderLayout.CENTER);
        return p;
    }

    /** Susun lembar cetak A4 untuk dokumen masuk (bukti terima), keluar (surat jalan), atau opname (berita acara). */
    static LembarCetak lembar(Konteks k, Dokumen d, List<ItemDokumen> item) {
        String judul = switch (d.jenis()) {
            case Dokumen.MASUK -> "BUKTI PENERIMAAN BARANG";
            case Dokumen.KELUAR -> "SURAT JALAN";
            default -> "BERITA ACARA STOK OPNAME";
        };
        LembarCetak l = new LembarCetak(judul + (d.batal() ? " (DIBATALKAN)" : ""), k.pengaturan.get("nama_perusahaan"),
                k.pengaturan.get("nama_gudang") + " - " + k.pengaturan.get("alamat"), "Telp. " + k.pengaturan.get("telepon"));
        l.info("Nomor", d.nomor()).info("Tanggal", Waktu.tampilTgl(LocalDate.parse(d.tanggal())));
        if (!d.jenis().equals(Dokumen.OPNAME)) {
            l.info(d.jenis().equals(Dokumen.MASUK) ? "Supplier" : "Kepada", d.pihak());
            l.info("Referensi", d.referensi() == null || d.referensi().isBlank() ? "-" : d.referensi());
        }
        l.info("Dicatat oleh", d.dibuatOleh());
        String kota = k.pengaturan.get("kota");
        String tt = kota + ", " + Waktu.tampilTgl(LocalDate.parse(d.tanggal()));
        String pj = k.pengaturan.get("penanggung_jawab");
        if (d.jenis().equals(Dokumen.OPNAME)) {
            l.kolom(new String[]{"No", "Kode", "Nama barang", "Satuan", "Sistem", "Fisik", "Selisih", "Nilai selisih"},
                    new double[]{0.05, 0.11, 0.34, 0.09, 0.09, 0.09, 0.09, 0.14},
                    new boolean[]{true, false, false, false, true, true, true, true});
            int no = 1;
            long tot = 0;
            for (ItemDokumen it : item) {
                long ns = (long) it.selisih() * it.harga();
                tot += ns;
                l.baris(String.valueOf(no++), it.kode(), it.nama(), it.satuan(), Rupiah.ribuan(it.stokSistem()), Rupiah.ribuan(it.qty()),
                        (it.selisih() > 0 ? "+" : "") + it.selisih(), Rupiah.ribuan(ns));
            }
            l.total("", "", "Total selisih nilai", "", "", "", "", Rupiah.ribuan(tot));
            l.tandaTangan(tt, new String[]{"Petugas hitung", d.dibuatOleh()}, new String[]{"Saksi", ""}, new String[]{"Kepala gudang", pj});
        } else {
            l.kolom(new String[]{"No", "Kode", "Nama barang", "Rak", "Jumlah", "Satuan", "Keterangan"},
                    new double[]{0.05, 0.12, 0.38, 0.1, 0.1, 0.1, 0.15},
                    new boolean[]{true, false, false, false, true, false, false});
            int no = 1;
            long unit = 0;
            for (ItemDokumen it : item) {
                unit += it.qty();
                Object lok = k.barang.byId(it.barangId()).lokasi();
                l.baris(String.valueOf(no++), it.kode(), it.nama(), lok == null ? "" : lok.toString(), Rupiah.ribuan(it.qty()), it.satuan(), "");
            }
            l.total("", "", "Jumlah", "", Rupiah.ribuan(unit), "unit", "");
            if (d.jenis().equals(Dokumen.MASUK)) {
                l.tandaTangan(tt, new String[]{"Pengirim", ""}, new String[]{"Penerima gudang", d.dibuatOleh()}, new String[]{"Kepala gudang", pj});
            } else {
                l.tandaTangan(tt, new String[]{"Penerima", ""}, new String[]{"Sopir", ""}, new String[]{"Petugas gudang", d.dibuatOleh()}, new String[]{"Kepala gudang", pj});
            }
        }
        if (d.catatan() != null && !d.catatan().isBlank()) l.catatan(d.catatan());
        return l;
    }

    class Model extends AbstractTableModel {
        final List<Dokumen> data = new ArrayList<>();
        final String[] kol = {"Nomor", "Tanggal", "Pihak", "Referensi", "Baris", "Unit", "Nilai", "Status"};

        void isi(List<Dokumen> l) { data.clear(); data.addAll(l); fireTableDataChanged(); }
        @Override public int getRowCount() { return data.size(); }
        @Override public int getColumnCount() { return kol.length; }
        @Override public String getColumnName(int c) { return c == 2 ? (masuk ? "Supplier" : "Tujuan") : kol[c]; }
        @Override public Object getValueAt(int r, int c) {
            Dokumen d = data.get(r);
            return switch (c) {
                case 0 -> d.nomor();
                case 1 -> LocalDate.parse(d.tanggal()).format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yy"));
                case 2 -> d.pihak();
                case 3 -> d.referensi();
                case 4 -> d.jumlahBaris();
                case 5 -> d.totalQty();
                case 6 -> d.totalNilai();
                default -> d.batal() ? "Batal" : "Aktif";
            };
        }
    }
}
