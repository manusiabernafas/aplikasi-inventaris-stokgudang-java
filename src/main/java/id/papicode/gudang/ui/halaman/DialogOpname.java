package id.papicode.gudang.ui.halaman;

import id.papicode.gudang.Konteks;
import id.papicode.gudang.model.Barang;
import id.papicode.gudang.model.BarisInput;
import id.papicode.gudang.model.Dokumen;
import id.papicode.gudang.ui.*;
import id.papicode.gudang.util.Rupiah;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Lembar input hasil hitung fisik. Kolom "Fisik" diisi petugas; selisih dan nilainya dihitung langsung. */
final class DialogOpname {
    private DialogOpname() { }

    static Dokumen tampil(Component induk, Konteks k, MainFrame frame, List<Barang> barang) {
        Window w = SwingUtilities.getWindowAncestor(induk);
        JDialog d = new JDialog(w, "Input hasil opname", java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        Dokumen[] hasil = {null};
        Integer[] fisik = new Integer[barang.size()];

        JLabel ringkas = Ui.label(" ");
        AbstractTableModel m = new AbstractTableModel() {
            final String[] kol = {"Rak", "Kode", "Nama barang", "Satuan", "Sistem", "Fisik", "Selisih", "Nilai selisih"};
            public int getRowCount() { return barang.size(); }
            public int getColumnCount() { return kol.length; }
            public String getColumnName(int c) { return kol[c]; }
            public Class<?> getColumnClass(int c) { return c >= 4 && c <= 6 ? Integer.class : c == 7 ? Long.class : String.class; }
            public boolean isCellEditable(int r, int c) { return c == 5; }
            public Object getValueAt(int r, int c) {
                Barang b = barang.get(r);
                Integer f = fisik[r];
                return switch (c) {
                    case 0 -> b.lokasi();
                    case 1 -> b.kode();
                    case 2 -> b.nama();
                    case 3 -> b.satuan();
                    case 4 -> b.stok();
                    case 5 -> f;
                    case 6 -> f == null ? null : f - b.stok();
                    default -> f == null ? null : (long) (f - b.stok()) * b.harga();
                };
            }
            public void setValueAt(Object v, int r, int c) {
                if (v == null || (v instanceof Integer i && i >= 0)) fisik[r] = (Integer) v;
                fireTableRowsUpdated(r, r);
            }
        };
        m.addTableModelListener(e -> {
            int terisi = 0, beda = 0;
            long nilai = 0;
            for (int i = 0; i < fisik.length; i++) {
                if (fisik[i] == null) continue;
                terisi++;
                int s = fisik[i] - barang.get(i).stok();
                if (s != 0) beda++;
                nilai += (long) s * barang.get(i).harga();
            }
            ringkas.setText(terisi + " dari " + fisik.length + " terisi  ·  " + beda + " berselisih  ·  selisih nilai " + Rupiah.format(nilai));
        });
        JTable t = new JTable(m);
        Ui.rapikanTabel(t);
        t.setRowHeight(32);
        t.putClientProperty("terminateEditOnFocusLost", true);
        t.setSurrendersFocusOnKeystroke(true);
        Ui.lebarKolom(t, 70, 90, 280, 70, 70, 80, 70, 110);
        Ui.kolomKode(t, 1);
        Ui.kolomAngka(t, 4, 7);
        t.getColumnModel().getColumn(6).setCellRenderer(HalamanOpname.rendererSelisih());
        t.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable tb, Object v, boolean s, boolean f, int r, int c) {
                super.getTableCellRendererComponent(tb, v == null ? "isi..." : Rupiah.ribuan((Integer) v), s, false, r, c);
                setHorizontalAlignment(RIGHT);
                setFont(Tema.monoBold(12.5f));
                setForeground(v == null ? Tema.GARIS : Tema.UTAMA);
                setBackground(s ? Tema.PILIH : new Color(0x0F3157));
                return this;
            }
        });
        // Enter pindah ke baris berikutnya pada kolom Fisik
        t.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "turun");
        t.getActionMap().put("turun", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                if (t.isEditing()) t.getCellEditor().stopCellEditing();
                int r = Math.min(t.getRowCount() - 1, t.getSelectedRow() + 1);
                t.changeSelection(r, 5, false, false);
                t.editCellAt(r, 5);
            }
        });

        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBackground(Tema.KARTU);
        root.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(3, 0, 0, 0, Tema.AKSEN), new EmptyBorder(18, 22, 18, 22)));
        JPanel atas = Ui.transparan(new BorderLayout(14, 0));
        JPanel judul = Ui.transparan(new BorderLayout(0, 2));
        JLabel an = Ui.label("Dokumen OP / penyesuaian stok");
        an.setForeground(Tema.AKSEN);
        judul.add(an, BorderLayout.NORTH);
        judul.add(Ui.judul("Hasil hitung fisik", 21f), BorderLayout.CENTER);
        atas.add(judul, BorderLayout.WEST);
        JTextField catatan = Ui.field("Catatan, misal: opname bulanan rak A-B");
        catatan.setPreferredSize(new Dimension(340, 38));
        Ui.Tombol samakan = Ui.tombol("Isi = sistem", null, Ui.Jenis.GARIS);
        samakan.setToolTipText("Isi semua baris kosong dengan angka sistem, lalu ubah yang berbeda saja");
        samakan.addActionListener(e -> {
            for (int i = 0; i < fisik.length; i++) if (fisik[i] == null) fisik[i] = barang.get(i).stok();
            m.fireTableDataChanged();
        });
        JPanel kanan = Ui.transparan(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        kanan.add(catatan);
        kanan.add(samakan);
        atas.add(kanan, BorderLayout.EAST);
        root.add(atas, BorderLayout.NORTH);
        JScrollPane sp = Ui.gulir(t);
        sp.setBorder(BorderFactory.createLineBorder(Tema.GARIS));
        sp.setPreferredSize(new Dimension(960, 420));
        JPanel tengah = Ui.transparan(new BorderLayout(0, 8));
        tengah.add(sp);
        tengah.add(ringkas, BorderLayout.SOUTH);
        root.add(tengah, BorderLayout.CENTER);

        Ui.Tombol simpan = Ui.tombol("Simpan & sesuaikan stok", "check", Ui.Jenis.UTAMA);
        Ui.Tombol batal = Ui.tombol("Batal", null, Ui.Jenis.GARIS);
        simpan.addActionListener(e -> {
            if (t.isEditing()) t.getCellEditor().stopCellEditing();
            List<BarisInput> in = new ArrayList<>();
            for (int i = 0; i < fisik.length; i++) if (fisik[i] != null) in.add(new BarisInput(barang.get(i).id(), fisik[i]));
            if (in.isEmpty()) { Kotak.pesan(d, "Belum ada hasil hitung", "Isi kolom Fisik minimal satu barang.", true); return; }
            int kosong = fisik.length - in.size();
            if (kosong > 0 && !Kotak.konfirmasi(d, kosong + " baris belum diisi",
                    "Baris yang kosong tidak ikut disesuaikan. Lanjut simpan " + in.size() + " barang?", "Simpan", false)) return;
            Kotak.aman(d, () -> {
                hasil[0] = k.dokumen.opname(frame.aktor(), LocalDate.now(k.jam), catatan.getText(), in);
                d.dispose();
            });
        });
        batal.addActionListener(e -> d.dispose());
        JPanel bawah = Ui.transparan(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        bawah.add(batal);
        bawah.add(simpan);
        root.add(bawah, BorderLayout.SOUTH);
        d.setContentPane(root);
        d.pack();
        d.setLocationRelativeTo(w);
        m.fireTableDataChanged();
        SwingUtilities.invokeLater(() -> { t.changeSelection(0, 5, false, false); t.requestFocusInWindow(); });
        d.setVisible(true);
        return hasil[0];
    }
}
