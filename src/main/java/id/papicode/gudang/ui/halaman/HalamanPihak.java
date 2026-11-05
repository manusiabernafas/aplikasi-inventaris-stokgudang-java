package id.papicode.gudang.ui.halaman;

import id.papicode.gudang.Konteks;
import id.papicode.gudang.model.Pihak;
import id.papicode.gudang.ui.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/** Data supplier (asal barang masuk) dan tujuan (penerima barang keluar). */
public class HalamanPihak extends MainFrame.Lembar implements MainFrame.Halaman {

    private final Konteks k;
    private final MainFrame frame;
    private final Model supplier = new Model(), tujuan = new Model();
    private final JTable tSupplier = new JTable(supplier), tTujuan = new JTable(tujuan);

    public HalamanPihak(Konteks k, MainFrame frame) {
        super(new BorderLayout());
        this.k = k;
        this.frame = frame;
        boolean admin = frame.pengguna().admin();
        add(MainFrame.kepala("Lembar 06 / Data master", "Supplier & tujuan",
                "Supplier dipakai di barang masuk, tujuan (proyek, cabang, pelanggan) dipakai di barang keluar."), BorderLayout.NORTH);
        JPanel isi = Ui.transparan(new GridLayout(1, 2, 14, 0));
        isi.add(kolom("Supplier", Pihak.SUPPLIER, tSupplier, admin));
        isi.add(kolom("Tujuan pengiriman", Pihak.TUJUAN, tTujuan, admin));
        add(isi, BorderLayout.CENTER);
    }

    private JPanel kolom(String judul, String jenis, JTable t, boolean admin) {
        Ui.rapikanTabel(t);
        Ui.lebarKolom(t, 230, 120, 230, 70);
        t.getColumnModel().getColumn(3).setCellRenderer(new Bagian.Pil(Bagian.WARNA_STATUS));
        Ui.Tombol tambah = Ui.tombol("Tambah", "plus", Ui.Jenis.UTAMA);
        tambah.setEnabled(admin);
        tambah.addActionListener(e -> form(jenis, null));
        Ui.Tombol ubah = Ui.ikonSaja("pencil", "Ubah", Ui.Jenis.GARIS);
        ubah.setEnabled(admin);
        ubah.addActionListener(e -> {
            int r = t.getSelectedRow();
            if (r < 0) { frame.kabar("Pilih baris dulu"); return; }
            form(jenis, ((Model) t.getModel()).data.get(r));
        });
        if (admin) t.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && t.getSelectedRow() >= 0) form(jenis, ((Model) t.getModel()).data.get(t.getSelectedRow()));
            }
        });
        JPanel aksi = Ui.transparan(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        aksi.add(ubah);
        aksi.add(tambah);
        JPanel p = Ui.transparan(new BorderLayout(0, 10));
        JPanel atas = Ui.transparan(new BorderLayout());
        JLabel j = Ui.judul(judul, 16f);
        j.setIcon(Ikon.of(jenis.equals(Pihak.SUPPLIER) ? "masuk" : "truck", 18, jenis.equals(Pihak.SUPPLIER) ? Tema.HIJAU : Tema.MERAH));
        j.setIconTextGap(10);
        atas.add(j, BorderLayout.WEST);
        atas.add(aksi, BorderLayout.EAST);
        p.add(atas, BorderLayout.NORTH);
        p.add(Bagian.tabel(t), BorderLayout.CENTER);
        return p;
    }

    private void form(String jenis, Pihak p) {
        boolean baru = p == null;
        JTextField nama = Ui.field("Nama perusahaan / proyek / orang");
        JTextField telp = Ui.field("08xx atau 031-xxx");
        JTextField alamat = Ui.field("Alamat singkat");
        JCheckBox aktif = new JCheckBox("Aktif", true);
        aktif.setOpaque(false);
        if (!baru) {
            nama.setText(p.nama());
            telp.setText(p.telepon());
            alamat.setText(p.alamat());
            aktif.setSelected(p.aktif());
        }
        JPanel f = Ui.transparan(new GridLayout(4, 1, 0, 12));
        f.add(Ui.baris("Nama", nama));
        f.add(Ui.baris("Telepon", telp));
        f.add(Ui.baris("Alamat", alamat));
        f.add(aktif);
        f.setPreferredSize(new Dimension(440, f.getPreferredSize().height));
        String label = jenis.equals(Pihak.SUPPLIER) ? "supplier" : "tujuan";
        Kotak.form(this, (baru ? "Tambah " : "Ubah ") + label, f, "Simpan", () -> {
            Pihak hasil = k.barang.simpanPihak(frame.aktor(), new Pihak(baru ? 0 : p.id(), jenis, nama.getText(), telp.getText(), alamat.getText(), aktif.isSelected()));
            muat();
            frame.kabar("Tersimpan: " + hasil.nama());
            return true;
        });
    }

    @Override
    public void muat() {
        supplier.isi(k.barang.pihak(Pihak.SUPPLIER, true));
        tujuan.isi(k.barang.pihak(Pihak.TUJUAN, true));
    }

    static class Model extends AbstractTableModel {
        final List<Pihak> data = new ArrayList<>();
        final String[] kol = {"Nama", "Telepon", "Alamat", "Status"};
        void isi(List<Pihak> l) { data.clear(); data.addAll(l); fireTableDataChanged(); }
        public int getRowCount() { return data.size(); }
        public int getColumnCount() { return kol.length; }
        public String getColumnName(int c) { return kol[c]; }
        public Object getValueAt(int r, int c) {
            Pihak p = data.get(r);
            return switch (c) { case 0 -> p.nama(); case 1 -> p.telepon(); case 2 -> p.alamat(); default -> p.aktif() ? "Aktif" : "Nonaktif"; };
        }
    }
}
