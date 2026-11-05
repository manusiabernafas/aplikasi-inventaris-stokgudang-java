package id.papicode.gudang.ui;

import id.papicode.gudang.Konteks;
import id.papicode.gudang.model.Dokumen;
import id.papicode.gudang.model.Pengguna;
import id.papicode.gudang.ui.halaman.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.LinkedHashMap;
import java.util.Map;

/** Jendela utama: sidebar navy bermenu kelompok + area halaman berkisi cetak biru. */
public class MainFrame extends JFrame {

    public interface Halaman {
        void muat();
        default JComponent fokusAwal() { return null; }
    }

    private final Konteks k;
    private Pengguna pengguna;
    private final CardLayout kartu = new CardLayout();
    private final JPanel isi = new JPanel(kartu);
    private final Map<String, Halaman> halaman = new LinkedHashMap<>();
    private final Map<String, ItemNav> nav = new LinkedHashMap<>();
    private final JPanel menu = Ui.transparan(null);
    private final Toast toast = new Toast();

    public MainFrame(Konteks k, Pengguna p) {
        super("Stok Gudang - " + k.pengaturan.get("nama_gudang"));
        this.k = k;
        this.pengguna = p;
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1366, 800);
        setMinimumSize(new Dimension(1120, 700));
        setLocationRelativeTo(null);
        setExtendedState(getExtendedState() | MAXIMIZED_BOTH);
        menu.setLayout(new BoxLayout(menu, BoxLayout.Y_AXIS));

        grup("Ringkasan");
        tambah("dasbor", "Dasbor", "dashboard", new HalamanDasbor(k, this), false);
        grup("Transaksi gudang");
        tambah("masuk", "Barang Masuk", "masuk", new HalamanDokumen(k, this, Dokumen.MASUK), false);
        tambah("keluar", "Barang Keluar", "keluar", new HalamanDokumen(k, this, Dokumen.KELUAR), false);
        tambah("opname", "Stok Opname", "clipboard", new HalamanOpname(k, this), false);
        grup("Data master");
        tambah("barang", "Daftar Barang", "package", new HalamanBarang(k, this), false);
        tambah("pihak", "Supplier & Tujuan", "truck", new HalamanPihak(k, this), false);
        grup("Laporan");
        tambah("kartu", "Kartu Stok", "file", new HalamanKartuStok(k, this), false);
        tambah("mutasi", "Mutasi Persediaan", "layers", new HalamanMutasi(k, this), false);
        tambah("analisis", "Analisis Barang", "trending", new HalamanAnalisis(k, this), false);
        grup("Sistem");
        tambah("pengguna", "Pengguna", "users", new HalamanPengguna(k, this), true);
        tambah("pengaturan", "Pengaturan", "sliders", new HalamanPengaturan(k, this), false);

        JPanel root = new JPanel(new BorderLayout());
        root.add(sidebar(), BorderLayout.WEST);
        root.add(isi, BorderLayout.CENTER);
        setContentPane(root);
        getLayeredPane().add(toast, JLayeredPane.POPUP_LAYER);
        buka("dasbor");
    }

    public Pengguna pengguna() { return pengguna; }

    public Pengguna aktor() {
        pengguna = k.auth.segarkan(pengguna);
        return pengguna;
    }

    @SuppressWarnings("unchecked")
    public <T> T halaman(String id) { return (T) halaman.get(id); }

    private void grup(String judul) {
        JLabel l = new JLabel(judul.toUpperCase());
        l.setFont(Tema.mono(10.5f));
        l.setForeground(new Color(0x6F93C2));
        l.setBorder(new EmptyBorder(menu.getComponentCount() == 0 ? 0 : 14, 12, 6, 0));
        l.setAlignmentX(0f);
        menu.add(l);
    }

    private void tambah(String id, String label, String ikon, JComponent h, boolean adminSaja) {
        if (adminSaja && !pengguna.admin()) return;
        halaman.put(id, (Halaman) h);
        isi.add(h, id);
        ItemNav n = new ItemNav(label, ikon, () -> buka(id));
        nav.put(id, n);
        menu.add(n);
    }

    public void buka(String id) {
        Halaman h = halaman.get(id);
        if (h == null) return;
        nav.forEach((kunci, n) -> n.setAktif(kunci.equals(id)));
        kartu.show(isi, id);
        Kotak.aman(this, h::muat);
        JComponent f = h.fokusAwal();
        if (f != null) SwingUtilities.invokeLater(f::requestFocusInWindow);
    }

    public void kabar(String pesan) { toast.tampil(pesan); }

    private JPanel sidebar() {
        JPanel s = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(Tema.GARIS);
                g.drawLine(getWidth() - 1, 0, getWidth() - 1, getHeight());
            }
        };
        s.setBackground(Tema.SIDEBAR);
        s.setPreferredSize(new Dimension(240, 0));
        s.setBorder(new EmptyBorder(20, 12, 14, 12));

        JPanel merek = Ui.transparan(new BorderLayout(12, 0));
        JLabel logo = new JLabel(Ikon.of("warehouse", 22, Tema.UTAMA_TEKS)) {
            @Override protected void paintComponent(Graphics g) {
                g.setColor(Tema.UTAMA);
                g.fillRect(0, 0, getWidth(), getHeight());
                super.paintComponent(g);
            }
        };
        logo.setPreferredSize(new Dimension(42, 42));
        logo.setHorizontalAlignment(SwingConstants.CENTER);
        merek.add(logo, BorderLayout.WEST);
        JPanel nama = Ui.transparan(new GridLayout(2, 1));
        JLabel n1 = new JLabel(k.pengaturan.get("nama_gudang"));
        n1.setFont(Tema.black(14.5f));
        n1.setForeground(Tema.TEKS);
        JLabel n2 = new JLabel(k.pengaturan.get("nama_perusahaan"));
        n2.setFont(Tema.mono(10.5f));
        n2.setForeground(Tema.TEKS_2);
        nama.add(n1);
        nama.add(n2);
        merek.add(nama, BorderLayout.CENTER);
        merek.setBorder(new EmptyBorder(0, 6, 20, 0));
        s.add(merek, BorderLayout.NORTH);

        JScrollPane sp = new JScrollPane(menu);
        sp.setBorder(null);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        JPanel bungkus = Ui.transparan(new BorderLayout());
        bungkus.add(sp, BorderLayout.NORTH);
        s.add(sp, BorderLayout.CENTER);

        Ui.Kartu akun = new Ui.Kartu(new BorderLayout(10, 0), new Color(0x143661), Tema.GARIS).tanpaTanda();
        akun.setBorder(new EmptyBorder(9, 10, 9, 6));
        JLabel ini = new JLabel(inisial(pengguna.nama()), SwingConstants.CENTER) {
            @Override protected void paintComponent(Graphics g) {
                g.setColor(Tema.AKSEN);
                g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
                super.paintComponent(g);
            }
        };
        ini.setFont(Tema.monoBold(12f));
        ini.setForeground(Tema.AKSEN);
        ini.setPreferredSize(new Dimension(34, 34));
        akun.add(ini, BorderLayout.WEST);
        JPanel teks = Ui.transparan(new GridLayout(2, 1));
        JLabel nm = new JLabel(pengguna.nama());
        nm.setFont(Tema.bold(13f));
        nm.setForeground(Tema.TEKS);
        JLabel pr = new JLabel(pengguna.admin() ? "ADMIN" : "STAF GUDANG");
        pr.setFont(Tema.mono(9.5f));
        pr.setForeground(Tema.TEKS_2);
        teks.add(nm);
        teks.add(pr);
        akun.add(teks, BorderLayout.CENTER);
        JButton keluar = new JButton(Ikon.of("logout", 18, Tema.TEKS_2));
        keluar.setToolTipText("Keluar");
        keluar.setContentAreaFilled(false);
        keluar.setBorderPainted(false);
        keluar.setFocusable(false);
        keluar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        keluar.addActionListener(e -> {
            if (!Kotak.konfirmasi(this, "Keluar dari aplikasi?", "Form yang belum disimpan akan hilang.", "Keluar", false)) return;
            dispose();
            new LoginFrame(k).setVisible(true);
        });
        akun.add(keluar, BorderLayout.EAST);
        JPanel bawah = Ui.transparan(new BorderLayout());
        bawah.setBorder(new EmptyBorder(12, 0, 0, 0));
        bawah.add(akun);
        s.add(bawah, BorderLayout.SOUTH);
        return s;
    }

    static String inisial(String nama) {
        String[] p = nama.trim().split("\\s+");
        return (p[0].substring(0, 1) + (p.length > 1 ? p[1].substring(0, 1) : "")).toUpperCase();
    }

    static class ItemNav extends JPanel {
        private boolean aktif, hover;
        private final JLabel label;
        private final String ikon;

        ItemNav(String teks, String ikon, Runnable klik) {
            super(new BorderLayout());
            this.ikon = ikon;
            setOpaque(false);
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
            setPreferredSize(new Dimension(200, 36));
            setAlignmentX(0f);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            label = new JLabel(teks);
            label.setIconTextGap(11);
            label.setBorder(new EmptyBorder(0, 12, 0, 0));
            add(label);
            perbarui();
            addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) { klik.run(); }
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hover = false; repaint(); }
            });
        }

        void setAktif(boolean a) { aktif = a; perbarui(); repaint(); }

        private void perbarui() {
            Color c = aktif ? Tema.UTAMA : Tema.TEKS_2;
            label.setForeground(aktif ? Tema.TEKS : Tema.TEKS_2);
            label.setFont(aktif ? Tema.bold(13.5f) : Tema.reg(13.5f));
            label.setIcon(Ikon.of(ikon, 17, c, 1.75f));
        }

        @Override protected void paintComponent(Graphics g) {
            if (aktif || hover) {
                g.setColor(aktif ? new Color(0x17406F) : new Color(0x133763));
                g.fillRect(0, 0, getWidth(), getHeight());
            }
            if (aktif) {
                g.setColor(Tema.UTAMA);
                g.fillRect(0, 0, 3, getHeight());
            }
            super.paintComponent(g);
        }
    }

    class Toast extends JLabel {
        private final Timer timer = new Timer(2800, e -> setVisible(false));

        Toast() {
            setVisible(false);
            setFont(Tema.bold(13f));
            setForeground(Tema.UTAMA_TEKS);
            setIcon(Ikon.of("check", 16, Tema.UTAMA_TEKS));
            setIconTextGap(10);
            setBorder(new EmptyBorder(11, 14, 11, 16));
            timer.setRepeats(false);
        }

        void tampil(String pesan) {
            setText(pesan);
            Dimension d = getPreferredSize();
            JLayeredPane lp = getLayeredPane();
            setBounds(lp.getWidth() - d.width - 26, lp.getHeight() - d.height - 26, d.width, d.height);
            setVisible(true);
            timer.restart();
        }

        @Override protected void paintComponent(Graphics g) {
            g.setColor(Tema.UTAMA);
            g.fillRect(0, 0, getWidth(), getHeight());
            super.paintComponent(g);
        }
    }

    /** Kepala halaman: anotasi kecil, judul besar, subjudul, aksi di kanan. */
    public static JPanel kepala(String kode, String judul, String sub, JComponent... aksi) {
        JPanel p = Ui.transparan(new BorderLayout(16, 0));
        JPanel kiri = Ui.transparan(new BorderLayout(0, 3));
        JLabel an = new JLabel(kode.toUpperCase());
        an.setFont(Tema.mono(11f));
        an.setForeground(Tema.UTAMA);
        kiri.add(an, BorderLayout.NORTH);
        kiri.add(Ui.judul(judul, 25f), BorderLayout.CENTER);
        if (sub != null) kiri.add(Ui.teks(sub, 13.5f, Tema.TEKS_2), BorderLayout.SOUTH);
        p.add(kiri, BorderLayout.CENTER);
        JPanel kanan = Ui.transparan(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        for (JComponent a : aksi) kanan.add(a);
        p.add(kanan, BorderLayout.EAST);
        p.setBorder(new EmptyBorder(0, 0, 18, 0));
        return p;
    }

    /** Halaman berlatar kisi cetak biru dengan padding standar. */
    public static class Lembar extends Ui.Kisi {
        public Lembar(LayoutManager lm) {
            super(lm);
            setBorder(new EmptyBorder(24, 28, 24, 28));
        }
    }
}
