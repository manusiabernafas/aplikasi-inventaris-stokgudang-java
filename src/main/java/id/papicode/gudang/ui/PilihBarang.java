package id.papicode.gudang.ui;

import id.papicode.gudang.Konteks;
import id.papicode.gudang.model.Barang;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/** Dialog pencarian barang (kode/nama). Enter atau klik dua kali untuk memilih. */
public final class PilihBarang {
    private PilihBarang() { }

    public static Barang pilih(Component induk, Konteks k, String awal) {
        Window w = SwingUtilities.getWindowAncestor(induk);
        JDialog d = new JDialog(w, "Pilih barang", java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        Barang[] hasil = {null};
        List<Barang> data = new ArrayList<>();
        AbstractTableModel m = new AbstractTableModel() {
            final String[] kol = {"Kode", "Nama barang", "Rak", "Stok", "Satuan"};
            public int getRowCount() { return data.size(); }
            public int getColumnCount() { return kol.length; }
            public String getColumnName(int c) { return kol[c]; }
            public Object getValueAt(int r, int c) {
                Barang b = data.get(r);
                return switch (c) { case 0 -> b.kode(); case 1 -> b.nama(); case 2 -> b.lokasi(); case 3 -> b.stok(); default -> b.satuan(); };
            }
        };
        JTable t = new JTable(m);
        Ui.rapikanTabel(t);
        Ui.lebarKolom(t, 90, 300, 80, 60, 70);
        Ui.kolomKode(t, 0);
        Ui.kolomAngka(t, 3);
        JTextField cari = Ui.cari("Ketik kode atau nama barang");
        cari.setText(awal == null ? "" : awal);
        Runnable saring = () -> {
            data.clear();
            data.addAll(k.barang.daftar(cari.getText(), null, null, false));
            m.fireTableDataChanged();
            if (!data.isEmpty()) t.setRowSelectionInterval(0, 0);
        };
        cari.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { saring.run(); }
            public void removeUpdate(DocumentEvent e) { saring.run(); }
            public void changedUpdate(DocumentEvent e) { saring.run(); }
        });
        Runnable ambil = () -> {
            int r = t.getSelectedRow();
            if (r >= 0) { hasil[0] = data.get(r); d.dispose(); }
        };
        cari.addActionListener(e -> ambil.run());
        cari.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                int r = t.getSelectedRow();
                if (e.getKeyCode() == KeyEvent.VK_DOWN && r < data.size() - 1) t.setRowSelectionInterval(r + 1, r + 1);
                if (e.getKeyCode() == KeyEvent.VK_UP && r > 0) t.setRowSelectionInterval(r - 1, r - 1);
                t.scrollRectToVisible(t.getCellRect(Math.max(0, t.getSelectedRow()), 0, true));
            }
        });
        t.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { if (e.getClickCount() == 2) ambil.run(); }
        });
        saring.run();
        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBackground(Tema.KARTU);
        root.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(3, 0, 0, 0, Tema.UTAMA), new EmptyBorder(16, 18, 16, 18)));
        root.add(cari, BorderLayout.NORTH);
        JScrollPane sp = Ui.gulir(t);
        sp.setPreferredSize(new Dimension(640, 380));
        sp.setBorder(BorderFactory.createLineBorder(Tema.GARIS));
        root.add(sp, BorderLayout.CENTER);
        root.add(Ui.label("Panah atas/bawah untuk memilih, Enter untuk ambil, Esc batal"), BorderLayout.SOUTH);
        d.getRootPane().registerKeyboardAction(e -> d.dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        d.setContentPane(root);
        d.pack();
        d.setLocationRelativeTo(w);
        d.setVisible(true);
        return hasil[0];
    }
}
