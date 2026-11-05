package id.papicode.gudang.ui.halaman;

import id.papicode.gudang.Konteks;
import id.papicode.gudang.model.*;
import id.papicode.gudang.service.Gagal;
import id.papicode.gudang.ui.*;
import id.papicode.gudang.util.Rupiah;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/** Form dokumen barang masuk / keluar dengan banyak baris barang. */
final class DialogDokumen {
    private DialogDokumen() { }

    static class Baris {
        final Barang b;
        int qty;
        Baris(Barang b, int qty) { this.b = b; this.qty = qty; }
    }

    /** Mengembalikan dokumen yang tersimpan, atau null bila dibatalkan. */
    static Dokumen tampil(Component induk, Konteks k, MainFrame frame, String jenis) {
        boolean masuk = jenis.equals(Dokumen.MASUK);
        Window w = SwingUtilities.getWindowAncestor(induk);
        JDialog d = new JDialog(w, masuk ? "Barang masuk baru" : "Barang keluar baru", java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        Dokumen[] hasil = {null};
        List<Baris> baris = new ArrayList<>();

        JPanel root = new JPanel(new BorderLayout(0, 14));
        root.setBackground(Tema.KARTU);
        root.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(3, 0, 0, 0, masuk ? Tema.HIJAU : Tema.MERAH),
                new EmptyBorder(18, 22, 18, 22)));

        // kepala form
        JPanel kepala = Ui.transparan(new BorderLayout(0, 12));
        JLabel an = Ui.label(masuk ? "Dokumen BM / penerimaan dari supplier" : "Dokumen BK / surat jalan pengiriman");
        an.setForeground(masuk ? Tema.HIJAU : Tema.MERAH);
        JPanel judul = Ui.transparan(new BorderLayout(0, 2));
        judul.add(an, BorderLayout.NORTH);
        judul.add(Ui.judul(masuk ? "Catat barang masuk" : "Catat barang keluar", 21f), BorderLayout.CENTER);
        kepala.add(judul, BorderLayout.NORTH);

        JSpinner tgl = new JSpinner(new SpinnerDateModel());
        tgl.setEditor(new JSpinner.DateEditor(tgl, "dd/MM/yyyy"));
        tgl.setValue(Date.from(LocalDate.now(k.jam).atStartOfDay(ZoneId.systemDefault()).toInstant()));
        JComboBox<Pihak> pihak = new JComboBox<>();
        for (Pihak p : k.barang.pihak(masuk ? Pihak.SUPPLIER : Pihak.TUJUAN, false)) pihak.addItem(p);
        JTextField ref = Ui.field(masuk ? "No. surat jalan / faktur supplier" : "No. PO / permintaan");
        JTextField cat = Ui.field(masuk ? "Misal: 2 sak sobek, sudah dipisah" : "Misal: kirim truk engkel, bongkar di blok C");
        JPanel info = Ui.transparan(new GridLayout(1, 4, 10, 0));
        info.add(Ui.baris("Tanggal", tgl));
        info.add(Ui.baris(masuk ? "Supplier" : "Tujuan / penerima", pihak));
        info.add(Ui.baris("No. referensi", ref));
        info.add(Ui.baris("Catatan", cat));
        kepala.add(info, BorderLayout.CENTER);
        root.add(kepala, BorderLayout.NORTH);

        // tabel baris
        AbstractTableModel m = new AbstractTableModel() {
            final String[] kol = {"Kode", "Nama barang", "Rak", "Stok kini", "Jumlah", "Satuan", masuk ? "Stok jadi" : "Sisa stok", "Nilai"};
            public int getRowCount() { return baris.size(); }
            public int getColumnCount() { return kol.length; }
            public String getColumnName(int c) { return kol[c]; }
            public Class<?> getColumnClass(int c) { return c == 3 || c == 4 || c == 6 ? Integer.class : c == 7 ? Long.class : String.class; }
            public boolean isCellEditable(int r, int c) { return c == 4; }
            public Object getValueAt(int r, int c) {
                Baris x = baris.get(r);
                return switch (c) {
                    case 0 -> x.b.kode();
                    case 1 -> x.b.nama();
                    case 2 -> x.b.lokasi();
                    case 3 -> x.b.stok();
                    case 4 -> x.qty;
                    case 5 -> x.b.satuan();
                    case 6 -> masuk ? x.b.stok() + x.qty : x.b.stok() - x.qty;
                    default -> x.b.harga() * x.qty;
                };
            }
            public void setValueAt(Object v, int r, int c) {
                if (v instanceof Integer q && q > 0) baris.get(r).qty = q;
                fireTableRowsUpdated(r, r);
            }
        };
        JTable t = new JTable(m);
        Ui.rapikanTabel(t);
        t.setRowHeight(32);
        Ui.lebarKolom(t, 90, 280, 80, 70, 80, 70, 80, 110);
        Ui.kolomKode(t, 0);
        Ui.kolomAngka(t, 3, 4, 7);
        t.getColumnModel().getColumn(6).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable tb, Object v, boolean s, boolean f, int r, int c) {
                super.getTableCellRendererComponent(tb, v, s, false, r, c);
                setHorizontalAlignment(RIGHT);
                setFont(Tema.monoBold(12.5f));
                setForeground(((Integer) v) < 0 ? Tema.MERAH : masuk ? Tema.HIJAU : Tema.TEKS);
                return this;
            }
        });
        t.putClientProperty("terminateEditOnFocusLost", true);

        JLabel ringkas = Ui.label(" ");
        Runnable segar = () -> {
            m.fireTableDataChanged();
            long unit = baris.stream().mapToLong(x -> x.qty).sum();
            long nilai = baris.stream().mapToLong(x -> x.qty * x.b.harga()).sum();
            ringkas.setText(baris.size() + " baris  ·  " + Rupiah.ribuan(unit) + " unit  ·  nilai " + Rupiah.format(nilai));
        };
        m.addTableModelListener(e -> {
            long unit = baris.stream().mapToLong(x -> x.qty).sum();
            long nilai = baris.stream().mapToLong(x -> x.qty * x.b.harga()).sum();
            ringkas.setText(baris.size() + " baris  ·  " + Rupiah.ribuan(unit) + " unit  ·  nilai " + Rupiah.format(nilai));
        });

        // baris input: kode + jumlah
        JTextField kode = Ui.field("Scan / ketik kode barang lalu Enter");
        kode.putClientProperty("JTextField.leadingIcon", Ikon.of("barcode", 16, Tema.AKSEN));
        kode.setFont(Tema.mono(13f));
        JTextField qty = Ui.field("Jumlah");
        qty.setFont(Tema.mono(13f));
        qty.setColumns(7);
        Ui.Tombol tambah = Ui.tombol("Tambah", "plus", Ui.Jenis.AKSEN);
        Ui.Tombol cari = Ui.tombol("Cari barang", "search", Ui.Jenis.GARIS);
        Ui.Tombol hapus = Ui.tombol("Hapus baris", "trash", Ui.Jenis.BAHAYA);
        Runnable masukkan = () -> {
            Barang b = k.barang.byKode(kode.getText());
            if (b == null) b = PilihBarang.pilih(d, k, kode.getText());
            if (b == null) return;
            int q;
            try { q = qty.getText().isBlank() ? 1 : (int) Rupiah.parse(qty.getText()); } catch (RuntimeException ex) { q = 0; }
            if (q <= 0) { Kotak.pesan(d, "Jumlah tidak valid", "Isi jumlah dengan angka lebih dari 0.", true); return; }
            Barang pilih = b;
            Baris ada = baris.stream().filter(x -> x.b.id() == pilih.id()).findFirst().orElse(null);
            if (ada != null) ada.qty += q; else baris.add(new Baris(b, q));
            segar.run();
            kode.setText("");
            qty.setText("");
            kode.requestFocusInWindow();
        };
        kode.addActionListener(e -> { if (qty.getText().isBlank() && k.barang.byKode(kode.getText()) != null) qty.requestFocusInWindow(); else masukkan.run(); });
        qty.addActionListener(e -> masukkan.run());
        tambah.addActionListener(e -> masukkan.run());
        cari.addActionListener(e -> {
            Barang b = PilihBarang.pilih(d, k, kode.getText());
            if (b != null) { kode.setText(b.kode()); qty.requestFocusInWindow(); }
        });
        hapus.addActionListener(e -> {
            int r = t.getSelectedRow();
            if (r >= 0) { baris.remove(r); segar.run(); }
        });
        JPanel input = Ui.transparan(new BorderLayout(8, 0));
        input.add(kode, BorderLayout.CENTER);
        JPanel kanan = Ui.transparan(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        kanan.add(qty);
        kanan.add(tambah);
        kanan.add(cari);
        kanan.add(hapus);
        input.add(kanan, BorderLayout.EAST);

        JPanel tengah = Ui.transparan(new BorderLayout(0, 8));
        tengah.add(input, BorderLayout.NORTH);
        JScrollPane sp = Ui.gulir(t);
        sp.setBorder(BorderFactory.createLineBorder(Tema.GARIS));
        sp.setPreferredSize(new Dimension(900, 300));
        tengah.add(sp, BorderLayout.CENTER);
        tengah.add(ringkas, BorderLayout.SOUTH);
        root.add(tengah, BorderLayout.CENTER);

        Ui.Tombol simpan = Ui.tombol("Simpan dokumen", "check", Ui.Jenis.UTAMA);
        Ui.Tombol batal = Ui.tombol("Batal", null, Ui.Jenis.GARIS);
        simpan.addActionListener(e -> {
            if (t.isEditing()) t.getCellEditor().stopCellEditing();
            Kotak.aman(d, () -> {
                if (pihak.getSelectedItem() == null) throw new Gagal(masuk ? "Tambahkan supplier dulu di menu Supplier & Tujuan." : "Tambahkan tujuan dulu di menu Supplier & Tujuan.");
                if (baris.isEmpty()) throw new Gagal("Tambahkan minimal satu barang.");
                LocalDate tanggal = ((Date) tgl.getValue()).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                List<BarisInput> in = new ArrayList<>();
                for (Baris x : baris) in.add(new BarisInput(x.b.id(), x.qty));
                long pid = ((Pihak) pihak.getSelectedItem()).id();
                hasil[0] = masuk ? k.dokumen.barangMasuk(frame.aktor(), tanggal, pid, ref.getText(), cat.getText(), in)
                                 : k.dokumen.barangKeluar(frame.aktor(), tanggal, pid, ref.getText(), cat.getText(), in);
                d.dispose();
            });
        });
        batal.addActionListener(e -> {
            if (baris.isEmpty() || Kotak.konfirmasi(d, "Buang isian?", "Baris barang yang sudah diisi akan hilang.", "Buang", true)) d.dispose();
        });
        JPanel bawah = Ui.transparan(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        bawah.add(batal);
        bawah.add(simpan);
        root.add(bawah, BorderLayout.SOUTH);

        d.getRootPane().registerKeyboardAction(e -> batal.doClick(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        d.getRootPane().registerKeyboardAction(e -> simpan.doClick(), KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK), JComponent.WHEN_IN_FOCUSED_WINDOW);
        d.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        d.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosing(java.awt.event.WindowEvent e) { batal.doClick(); }
        });
        d.setContentPane(root);
        d.pack();
        d.setLocationRelativeTo(w);
        segar.run();
        SwingUtilities.invokeLater(kode::requestFocusInWindow);
        d.setVisible(true);
        return hasil[0];
    }
}
