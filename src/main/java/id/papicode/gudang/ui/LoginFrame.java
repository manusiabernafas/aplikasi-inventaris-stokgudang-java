package id.papicode.gudang.ui;

import id.papicode.gudang.Konteks;
import id.papicode.gudang.db.DataContoh;
import id.papicode.gudang.model.Pengguna;
import id.papicode.gudang.service.Gagal;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/** Layar masuk: gambar teknik rak gudang di kiri (latar cetak biru), form login di kanan. */
public class LoginFrame extends JFrame {

    private final Konteks k;
    private final JTextField user = Ui.field("Username");
    private final JPasswordField pass = new JPasswordField();
    private final JLabel galat = Ui.teks(" ", 13f, Tema.MERAH);

    public LoginFrame(Konteks k) {
        super("Stok Gudang - Masuk");
        this.k = k;
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1060, 650);
        setMinimumSize(new Dimension(820, 560));
        setLocationRelativeTo(null);
        JPanel root = new JPanel(new BorderLayout());
        root.add(new Gambar(k.pengaturan.get("nama_gudang"), k.pengaturan.get("nama_perusahaan")), BorderLayout.CENTER);
        root.add(form(), BorderLayout.EAST);
        setContentPane(root);
    }

    private JPanel form() {
        JPanel luar = new JPanel(new GridBagLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(Tema.GARIS);
                g.drawLine(0, 0, 0, getHeight());
            }
        };
        luar.setBackground(Tema.SIDEBAR);
        luar.setPreferredSize(new Dimension(430, 0));
        JPanel f = Ui.transparan(null);
        f.setLayout(new BoxLayout(f, BoxLayout.Y_AXIS));

        JLabel an = Ui.label("Lembar 00 / Akses");
        an.setForeground(Tema.UTAMA);
        JLabel judul = Ui.judul("Masuk gudang", 30f);
        JLabel sub = Ui.teks("Catat barang masuk, keluar, dan opname.", 13.5f, Tema.TEKS_2);
        user.putClientProperty("JTextField.leadingIcon", Ikon.of("user", 16, Tema.TEKS_2));
        pass.putClientProperty("JTextField.placeholderText", "Password");
        pass.putClientProperty("JTextField.leadingIcon", Ikon.of("lock", 16, Tema.TEKS_2));
        pass.putClientProperty("JPasswordField.showRevealButton", true);
        for (JComponent c : new JComponent[]{user, pass}) {
            c.setMaximumSize(new Dimension(320, 42));
            c.setPreferredSize(new Dimension(320, 42));
            c.setFont(Tema.reg(14.5f));
        }
        Ui.Tombol masuk = Ui.tombol("MASUK", "arrow", Ui.Jenis.UTAMA);
        masuk.setHorizontalTextPosition(SwingConstants.LEFT);
        masuk.setFont(Tema.monoBold(14f));
        masuk.setMaximumSize(new Dimension(320, 46));
        masuk.setPreferredSize(new Dimension(320, 46));
        masuk.addActionListener(e -> masuk());
        getRootPane().setDefaultButton(masuk);
        JLabel info = Ui.teks("<html>Akun bawaan <b>admin / admin123</b>.<br>Ganti segera di menu Pengaturan.</html>", 12.5f, Tema.TEKS_2);
        for (JComponent c : new JComponent[]{an, judul, sub, user, pass, galat, masuk, info}) c.setAlignmentX(0f);
        f.add(an);
        f.add(Ui.jarak(0, 6));
        f.add(judul);
        f.add(Ui.jarak(0, 4));
        f.add(sub);
        f.add(Ui.jarak(0, 28));
        f.add(user);
        f.add(Ui.jarak(0, 12));
        f.add(pass);
        f.add(Ui.jarak(0, 8));
        f.add(galat);
        f.add(Ui.jarak(0, 10));
        f.add(masuk);
        f.add(Ui.jarak(0, 22));
        f.add(info);
        f.setPreferredSize(new Dimension(320, 390));
        luar.add(f);
        return luar;
    }

    private void masuk() {
        try {
            Pengguna p = k.auth.login(user.getText(), new String(pass.getPassword()));
            pass.setText("");
            if (p.admin() && DataContoh.kosong(k.db)) {
                if (Kotak.konfirmasi(this, "Database masih kosong",
                        "Isi data contoh gudang bahan bangunan (45 barang, supplier, tujuan, dan transaksi 60 hari) untuk mencoba semua fitur?",
                        "Isi data contoh", false)) {
                    setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                    DataContoh.isi(k.db, k.jam);
                }
            }
            new MainFrame(k, p).setVisible(true);
            dispose();
        } catch (Gagal g) {
            galat.setText(g.getMessage());
            pass.requestFocusInWindow();
        } catch (RuntimeException e) {
            Kotak.pesan(this, "Terjadi kesalahan", String.valueOf(e.getMessage()), true);
        } finally {
            setCursor(Cursor.getDefaultCursor());
        }
    }

    /** Gambar tampak depan rak palet dengan garis ukur dan kop gambar, seperti lembar cetak biru. */
    static class Gambar extends JPanel {
        private final String gudang, perusahaan;

        Gambar(String gudang, String perusahaan) {
            this.gudang = gudang;
            this.perusahaan = perusahaan;
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            Tema.gambarKisi(g2, w, h);
            Color garis = new Color(0xDCE9F8);
            g2.setColor(garis);
            g2.setStroke(new BasicStroke(1.4f));

            // rak 3 tingkat x 3 bay
            int x0 = 70, y0 = 90, lebar = Math.min(w - 150, 520), tinggi = Math.min(h - 260, 300);
            int bay = lebar / 3, tingkat = tinggi / 3;
            for (int i = 0; i <= 3; i++) g2.fillRect(x0 + i * bay - 3, y0, 6, tinggi + 10);
            for (int j = 0; j <= 3; j++) g2.drawRect(x0, y0 + j * tingkat, lebar, 4);
            java.util.Random r = new java.util.Random(3);
            for (int j = 0; j < 3; j++) {
                for (int i = 0; i < 3; i++) {
                    int bx = x0 + i * bay + 12, by = y0 + j * tingkat + 4;
                    int jml = 1 + r.nextInt(3);
                    int lk = (bay - 24 - (jml - 1) * 6) / jml;
                    for (int n = 0; n < jml; n++) {
                        int kt = tingkat - 20 - r.nextInt(30);
                        int kx = bx + n * (lk + 6);
                        g2.drawRect(kx, by + tingkat - kt - 4, lk, kt);
                        g2.drawLine(kx, by + tingkat - kt - 4, kx + lk, by + tingkat - 4);
                        g2.drawLine(kx + lk, by + tingkat - kt - 4, kx, by + tingkat - 4);
                    }
                    g2.setFont(Tema.mono(10f));
                    g2.drawString((char) ('A' + i) + "-0" + (3 - j), x0 + i * bay + 10, y0 + j * tingkat + 18);
                }
            }
            // garis ukur
            Stroke tipis = new BasicStroke(1f);
            g2.setStroke(tipis);
            g2.setColor(Tema.UTAMA);
            int yu = y0 + tinggi + 34;
            g2.drawLine(x0, yu, x0 + lebar, yu);
            g2.drawLine(x0, yu - 6, x0, yu + 6);
            g2.drawLine(x0 + lebar, yu - 6, x0 + lebar, yu + 6);
            g2.setFont(Tema.mono(11f));
            String uk = "L = 8.100 mm";
            g2.drawString(uk, x0 + lebar / 2 - g2.getFontMetrics().stringWidth(uk) / 2, yu - 6);
            int xu = x0 - 30;
            g2.drawLine(xu, y0, xu, y0 + tinggi);
            g2.drawLine(xu - 6, y0, xu + 6, y0);
            g2.drawLine(xu - 6, y0 + tinggi, xu + 6, y0 + tinggi);
            Graphics2D gt = (Graphics2D) g2.create();
            gt.rotate(-Math.PI / 2, xu - 6, y0 + tinggi / 2.0);
            gt.drawString("H = 4.500", xu - 40, y0 + tinggi / 2);
            gt.dispose();

            // kop gambar (title block)
            int tw = Math.min(380, w - 60), th = 92, tx = w - tw - 28, ty = h - th - 28;
            g2.setColor(Tema.SIDEBAR);
            g2.fillRect(tx, ty, tw, th);
            g2.setColor(garis);
            g2.drawRect(tx, ty, tw, th);
            g2.drawLine(tx, ty + 46, tx + tw, ty + 46);
            g2.drawLine(tx + tw - 110, ty + 46, tx + tw - 110, ty + th);
            g2.setFont(Tema.black(17f));
            g2.drawString(gudang, tx + 14, ty + 30);
            g2.setFont(Tema.mono(10f));
            g2.setColor(Tema.TEKS_2);
            g2.drawString("PEMILIK  " + perusahaan.toUpperCase(), tx + 14, ty + 64);
            g2.drawString("SISTEM   STOK & MUTASI", tx + 14, ty + 80);
            g2.drawString("SKALA 1:50", tx + tw - 98, ty + 64);
            g2.drawString("LEMBAR 01", tx + tw - 98, ty + 80);
            g2.dispose();
        }
    }
}
