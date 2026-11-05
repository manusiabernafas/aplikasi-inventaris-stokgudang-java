package id.papicode.gudang.ui.halaman;

import id.papicode.gudang.Konteks;
import id.papicode.gudang.service.AuthService;
import id.papicode.gudang.service.Gagal;
import id.papicode.gudang.ui.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/** Identitas toko & struk (admin), ganti password (semua), cadangkan database (admin). */
public class HalamanPengaturan extends MainFrame.Lembar implements MainFrame.Halaman {

    private final Konteks k;
    private final MainFrame frame;
    private final JTextField perusahaan = Ui.field("Nama perusahaan");
    private final JTextField gudang = Ui.field("Nama gudang");
    private final JTextField alamat = Ui.field("Alamat gudang");
    private final JTextField kota = Ui.field("Kota (untuk tanda tangan)");
    private final JTextField telp = Ui.field("Telepon");
    private final JTextField pj = Ui.field("Nama kepala gudang");

    public HalamanPengaturan(Konteks k, MainFrame frame) {
        super(new BorderLayout());
        this.k = k;
        this.frame = frame;
        add(MainFrame.kepala("Lembar 11 / Sistem", "Pengaturan", "Kop dokumen cetak, keamanan akun, dan cadangan data."), BorderLayout.NORTH);

        JPanel isi = Ui.transparan(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH; c.anchor = GridBagConstraints.NORTH;
        c.gridx = 0; c.gridy = 0; c.weightx = 0.55; c.gridheight = 2; c.insets = new Insets(0, 0, 0, 18);
        isi.add(kartuToko(), c);
        c.gridx = 1; c.gridheight = 1; c.weightx = 0.45; c.insets = new Insets(0, 0, 18, 0);
        isi.add(kartuPassword(), c);
        c.gridy = 1; c.insets = new Insets(0, 0, 0, 0);
        isi.add(kartuData(), c);
        c.gridx = 0; c.gridy = 2; c.gridwidth = 2; c.weighty = 1;
        isi.add(Box.createGlue(), c);
        add(isi, BorderLayout.CENTER);
    }

    private JPanel kartu(String judul, String ikon) {
        Ui.Kartu k1 = new Ui.Kartu(null);
        k1.setLayout(new BoxLayout(k1, BoxLayout.Y_AXIS));
        k1.setBorder(new EmptyBorder(20, 22, 22, 22));
        JLabel j = Ui.judul(judul, 17f);
        j.setIcon(Ikon.of(ikon, 20, Tema.UTAMA));
        j.setIconTextGap(10);
        j.setAlignmentX(0f);
        k1.add(j);
        k1.add(Ui.jarak(0, 16));
        return k1;
    }

    private void tambahBaris(JPanel p, String label, JComponent c) {
        JPanel b = Ui.baris(label, c);
        b.setAlignmentX(0f);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
        p.add(b);
        p.add(Ui.jarak(0, 12));
    }

    private JPanel kartuToko() {
        JPanel p = kartu("Identitas & kop dokumen", "warehouse");
        tambahBaris(p, "Nama perusahaan", perusahaan);
        tambahBaris(p, "Nama gudang", gudang);
        tambahBaris(p, "Alamat", alamat);
        tambahBaris(p, "Kota", kota);
        tambahBaris(p, "Telepon", telp);
        tambahBaris(p, "Kepala gudang (penanda tangan)", pj);
        boolean admin = frame.pengguna().admin();
        for (JComponent f : new JComponent[]{perusahaan, gudang, alamat, kota, telp, pj}) f.setEnabled(admin);
        Ui.Tombol simpan = Ui.tombol("Simpan identitas", "check", Ui.Jenis.UTAMA);
        simpan.setEnabled(admin);
        simpan.addActionListener(e -> Kotak.aman(this, () -> {
            Map<String, String> m = new LinkedHashMap<>();
            m.put("nama_perusahaan", perusahaan.getText());
            m.put("nama_gudang", gudang.getText());
            m.put("alamat", alamat.getText());
            m.put("kota", kota.getText());
            m.put("telepon", telp.getText());
            m.put("penanggung_jawab", pj.getText());
            k.pengaturan.simpan(frame.aktor(), m);
            frame.kabar("Identitas disimpan. Nama di sidebar berubah setelah login ulang.");
        }));
        JPanel t = Ui.transparan(new FlowLayout(FlowLayout.LEFT, 0, 4));
        t.add(simpan);
        if (!admin) t.add(Ui.teks("   Hanya admin yang bisa mengubah.", 13f, Tema.TEKS_2));
        t.setAlignmentX(0f);
        p.add(t);
        return p;
    }

    private JPanel kartuPassword() {
        JPanel p = kartu("Ganti password saya", "lock");
        JPasswordField lama = new JPasswordField(), baru = new JPasswordField(), ulang = new JPasswordField();
        for (JPasswordField f : new JPasswordField[]{lama, baru, ulang}) f.putClientProperty("JPasswordField.showRevealButton", true);
        tambahBaris(p, "Password lama", lama);
        tambahBaris(p, "Password baru (min. 6 karakter)", baru);
        tambahBaris(p, "Ulangi password baru", ulang);
        Ui.Tombol simpan = Ui.tombol("Ganti password", "check", Ui.Jenis.UTAMA);
        simpan.addActionListener(e -> Kotak.aman(this, () -> {
            String b = new String(baru.getPassword());
            if (!b.equals(new String(ulang.getPassword()))) throw new Gagal("Ulangi password baru belum sama.");
            k.auth.gantiPassword(frame.aktor(), new String(lama.getPassword()), b);
            lama.setText(""); baru.setText(""); ulang.setText("");
            frame.kabar("Password berhasil diganti");
        }));
        JPanel t = Ui.transparan(new FlowLayout(FlowLayout.LEFT, 0, 4));
        t.add(simpan);
        t.setAlignmentX(0f);
        p.add(t);
        return p;
    }

    private JPanel kartuData() {
        JPanel p = kartu("Cadangan data", "database");
        JLabel info = Ui.teks("<html>Semua data tersimpan di satu file SQLite. Cadangkan rutin ke flashdisk atau Google Drive.</html>", 13.5f, Tema.TEKS_2);
        info.setAlignmentX(0f);
        p.add(info);
        p.add(Ui.jarak(0, 14));
        Ui.Tombol cadang = Ui.tombol("Cadangkan sekarang", "download", Ui.Jenis.AKSEN);
        cadang.setEnabled(frame.pengguna().admin());
        cadang.addActionListener(e -> cadangkan());
        JPanel t = Ui.transparan(new FlowLayout(FlowLayout.LEFT, 0, 0));
        t.add(cadang);
        t.setAlignmentX(0f);
        p.add(t);
        return p;
    }

    private void cadangkan() {
        JFileChooser fc = new JFileChooser();
        String stamp = LocalDateTime.now(k.jam).format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmm"));
        fc.setSelectedFile(new File("cadangan-gudang-" + stamp + ".db"));
        fc.setFileFilter(new FileNameExtensionFilter("Database SQLite", "db"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File f = fc.getSelectedFile();
        Kotak.aman(this, () -> {
            AuthService.wajibAdmin(frame.aktor());
            if (f.exists()) throw new Gagal("File sudah ada, pilih nama lain.");
            k.db.cadangkan(f);
            frame.kabar("Cadangan tersimpan: " + f.getName());
        });
    }

    @Override
    public void muat() {
        perusahaan.setText(k.pengaturan.get("nama_perusahaan"));
        gudang.setText(k.pengaturan.get("nama_gudang"));
        alamat.setText(k.pengaturan.get("alamat"));
        kota.setText(k.pengaturan.get("kota"));
        telp.setText(k.pengaturan.get("telepon"));
        pj.setText(k.pengaturan.get("penanggung_jawab"));
    }
}
