package id.papicode.gudang.ui.halaman;

import id.papicode.gudang.Konteks;
import id.papicode.gudang.model.Pengguna;
import id.papicode.gudang.ui.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/** Kelola akun kasir dan admin (khusus admin). */
public class HalamanPengguna extends MainFrame.Lembar implements MainFrame.Halaman {

    private final Konteks k;
    private final MainFrame frame;
    private final Model model = new Model();
    private final JTable tabel = new JTable(model);

    public HalamanPengguna(Konteks k, MainFrame frame) {
        super(new BorderLayout());
        this.k = k;
        this.frame = frame;

        Ui.Tombol tambah = Ui.tombol("Tambah pengguna", "plus", Ui.Jenis.UTAMA);
        tambah.addActionListener(e -> tambah());
        Ui.Tombol reset = Ui.tombol("Reset password", "lock", Ui.Jenis.GARIS);
        reset.addActionListener(e -> reset());
        Ui.Tombol status = Ui.tombol("Aktif / nonaktif", "refresh", Ui.Jenis.GARIS);
        status.addActionListener(e -> ubahStatus());
        add(MainFrame.kepala("Lembar 10 / Sistem", "Pengguna", "Staf gudang bisa mencatat barang masuk/keluar. Admin bisa semua, termasuk opname dan pembatalan.", status, reset, tambah), BorderLayout.NORTH);

        Ui.rapikanTabel(tabel);
        Ui.lebarKolom(tabel, 260, 180, 160, 120);
        tabel.getColumnModel().getColumn(3).setCellRenderer(new Bagian.Pil(Bagian.WARNA_STATUS));
        add(Bagian.tabel(tabel), BorderLayout.CENTER);
    }

    @Override public void muat() { model.isi(k.auth.daftar()); }

    private Pengguna terpilih() {
        int r = tabel.getSelectedRow();
        if (r < 0) { frame.kabar("Pilih pengguna di tabel dulu"); return null; }
        return model.data.get(r);
    }

    private void tambah() {
        JTextField nama = Ui.field("Nama lengkap");
        JTextField user = Ui.field("Huruf/angka, tanpa spasi");
        JComboBox<String> peran = new JComboBox<>(new String[]{"Staf gudang", "Admin"});
        JPasswordField pw = new JPasswordField();
        pw.putClientProperty("JTextField.placeholderText", "Minimal 6 karakter");
        pw.putClientProperty("JPasswordField.showRevealButton", true);
        JPanel f = Ui.transparan(new GridLayout(4, 1, 0, 14));
        f.add(Ui.baris("Nama", nama));
        f.add(Ui.baris("Username", user));
        f.add(Ui.baris("Peran", peran));
        f.add(Ui.baris("Password awal", pw));
        f.setPreferredSize(new Dimension(420, f.getPreferredSize().height));
        Kotak.form(this, "Tambah pengguna", f, "Simpan", () -> {
            Pengguna p = k.auth.tambah(frame.aktor(), user.getText(), nama.getText(),
                    peran.getSelectedIndex() == 1 ? Pengguna.ADMIN : Pengguna.STAF, new String(pw.getPassword()));
            muat();
            frame.kabar("Akun " + p.username() + " dibuat");
            return true;
        });
    }

    private void reset() {
        Pengguna p = terpilih();
        if (p == null) return;
        JPasswordField pw = new JPasswordField();
        pw.putClientProperty("JPasswordField.showRevealButton", true);
        pw.putClientProperty("JTextField.placeholderText", "Minimal 6 karakter");
        JPanel f = Ui.transparan(new BorderLayout(0, 10));
        f.add(Ui.teks("Atur password baru untuk " + p.nama() + " (" + p.username() + ").", 14f, Tema.TEKS_2), BorderLayout.NORTH);
        f.add(Ui.baris("Password baru", pw), BorderLayout.CENTER);
        Kotak.form(this, "Reset password", f, "Simpan", () -> {
            k.auth.resetPassword(frame.aktor(), p.id(), new String(pw.getPassword()));
            frame.kabar("Password " + p.username() + " direset");
            return true;
        });
    }

    private void ubahStatus() {
        Pengguna p = terpilih();
        if (p == null) return;
        boolean jadi = !p.aktif();
        if (!Kotak.konfirmasi(this, (jadi ? "Aktifkan " : "Nonaktifkan ") + p.nama() + "?",
                jadi ? "Akun bisa dipakai login lagi." : "Akun tidak bisa login, tetapi riwayat transaksinya tetap tersimpan.",
                jadi ? "Aktifkan" : "Nonaktifkan", !jadi)) return;
        if (Kotak.aman(this, () -> k.auth.setAktif(frame.aktor(), p.id(), jadi))) muat();
    }

    class Model extends AbstractTableModel {
        final List<Pengguna> data = new ArrayList<>();
        final String[] kol = {"Nama", "Username", "Peran", "Status"};

        void isi(List<Pengguna> l) { data.clear(); data.addAll(l); fireTableDataChanged(); }
        @Override public int getRowCount() { return data.size(); }
        @Override public int getColumnCount() { return kol.length; }
        @Override public String getColumnName(int c) { return kol[c]; }
        @Override public Object getValueAt(int r, int c) {
            Pengguna p = data.get(r);
            return switch (c) {
                case 0 -> p.nama() + (p.id() == frame.pengguna().id() ? "  (Anda)" : "");
                case 1 -> p.username();
                case 2 -> p.admin() ? "Admin" : "Staf gudang";
                default -> p.aktif() ? "Aktif" : "Nonaktif";
            };
        }
    }
}
