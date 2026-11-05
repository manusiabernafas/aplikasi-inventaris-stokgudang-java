package id.papicode.gudang.ui;

import id.papicode.gudang.util.Rupiah;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/** Komponen bergaya cetak biru: panel bergaris dengan tanda sudut, tombol garis, label anotasi. */
public final class Ui {

    private Ui() { }

    // ---------------- panel ----------------

    /** Panel berbingkai garis tipis dengan tanda siku di keempat sudut (seperti gambar teknik). */
    public static class Kartu extends JPanel {
        private Color isi;
        private Color garis;
        private boolean tandaSudut = true;

        public Kartu(LayoutManager lm, Color isi, Color garis) {
            super(lm);
            this.isi = isi;
            this.garis = garis;
            setOpaque(false);
        }

        public Kartu(LayoutManager lm) { this(lm, Tema.KARTU, Tema.GARIS); }

        public Kartu tanpaTanda() { tandaSudut = false; return this; }
        public void setIsi(Color c) { isi = c; repaint(); }
        public void setGaris(Color c) { garis = c; repaint(); }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            int w = getWidth(), h = getHeight();
            if (isi != null) {
                g2.setColor(isi);
                g2.fillRect(0, 0, w, h);
            }
            if (garis != null) {
                g2.setColor(garis);
                g2.drawRect(0, 0, w - 1, h - 1);
            }
            if (tandaSudut) {
                g2.setColor(Tema.GARIS_TERANG);
                g2.setStroke(new BasicStroke(2f));
                int t = 10;
                g2.drawLine(1, 1, t, 1); g2.drawLine(1, 1, 1, t);
                g2.drawLine(w - 2, 1, w - t - 1, 1); g2.drawLine(w - 2, 1, w - 2, t);
                g2.drawLine(1, h - 2, t, h - 2); g2.drawLine(1, h - 2, 1, h - t - 1);
                g2.drawLine(w - 2, h - 2, w - t - 1, h - 2); g2.drawLine(w - 2, h - 2, w - 2, h - t - 1);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    public static Kartu kartu(LayoutManager lm, int pad) {
        Kartu k = new Kartu(lm);
        k.setBorder(new EmptyBorder(pad, pad, pad, pad));
        return k;
    }

    /** Panel latar berkisi (dipakai untuk area halaman). */
    public static class Kisi extends JPanel {
        public Kisi(LayoutManager lm) { super(lm); }
        @Override protected void paintComponent(Graphics g) { Tema.gambarKisi(g, getWidth(), getHeight()); }
    }

    // ---------------- tombol ----------------

    public enum Jenis { UTAMA, AKSEN, GARIS, HANTU, BAHAYA }

    public static class Tombol extends JButton {
        private final Jenis jenis;
        private boolean hover;

        public Tombol(String teks, String ikon, Jenis jenis) {
            super(teks);
            this.jenis = jenis;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setFont(Tema.bold(13f));
            setForeground(teksWarna());
            setBorder(new EmptyBorder(9, 15, 9, 15));
            setIconTextGap(8);
            if (ikon != null) setIcon(Ikon.of(ikon, 16, teksWarna()));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hover = false; repaint(); }
            });
        }

        private Color teksWarna() {
            return switch (jenis) {
                case UTAMA, AKSEN -> Tema.UTAMA_TEKS;
                case BAHAYA -> Tema.MERAH;
                case GARIS, HANTU -> Tema.TEKS;
            };
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            int w = getWidth(), h = getHeight();
            Color isi = switch (jenis) {
                case UTAMA -> hover ? Tema.UTAMA_HOVER : Tema.UTAMA;
                case AKSEN -> hover ? new Color(0xA5E4FA) : Tema.AKSEN;
                case GARIS, BAHAYA -> hover ? Tema.PILIH : Tema.KARTU;
                case HANTU -> hover ? Tema.PILIH : null;
            };
            if (!isEnabled()) isi = Tema.KARTU_2;
            if (isi != null) {
                g2.setColor(isi);
                g2.fillRect(0, 0, w, h);
            }
            if (jenis == Jenis.GARIS || jenis == Jenis.BAHAYA) {
                g2.setColor(jenis == Jenis.BAHAYA ? Tema.MERAH : Tema.GARIS_TERANG);
                g2.drawRect(0, 0, w - 1, h - 1);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    public static Tombol tombol(String teks, String ikon, Jenis jenis) { return new Tombol(teks, ikon, jenis); }

    public static Tombol ikonSaja(String ikon, String tip, Jenis jenis) {
        Tombol t = new Tombol(null, ikon, jenis);
        t.setToolTipText(tip);
        t.setBorder(new EmptyBorder(7, 7, 7, 7));
        return t;
    }

    // ---------------- teks ----------------

    public static JLabel judul(String s, float size) {
        JLabel l = new JLabel(s);
        l.setFont(Tema.black(size));
        l.setForeground(Tema.TEKS);
        return l;
    }

    public static JLabel teks(String s, float size, Color c) {
        JLabel l = new JLabel(s);
        l.setFont(Tema.reg(size));
        l.setForeground(c);
        return l;
    }

    /** Label anotasi: huruf kapital, monospace kecil (gaya keterangan gambar teknik). */
    public static JLabel label(String s) {
        JLabel l = new JLabel(s.toUpperCase());
        l.setFont(Tema.mono(11f));
        l.setForeground(Tema.TEKS_2);
        return l;
    }

    public static JLabel angka(String s, float size, Color c) {
        JLabel l = new JLabel(s);
        l.setFont(Tema.monoBold(size));
        l.setForeground(c);
        return l;
    }

    /** Lencana bergaris (tanpa isi) dengan warna status. */
    public static JLabel lencana(String s, Color warna) {
        JLabel l = new JLabel(s.toUpperCase()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(warna);
                g2.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        l.setFont(Tema.monoBold(10.5f));
        l.setForeground(warna);
        l.setBorder(new EmptyBorder(3, 7, 3, 7));
        return l;
    }

    // ---------------- field ----------------

    public static JTextField field(String placeholder) {
        JTextField f = new JTextField();
        f.putClientProperty("JTextField.placeholderText", placeholder);
        return f;
    }

    public static JTextField cari(String placeholder) {
        JTextField f = field(placeholder);
        f.putClientProperty("JTextField.leadingIcon", Ikon.of("search", 16, Tema.TEKS_2));
        f.putClientProperty("JTextField.showClearButton", true);
        return f;
    }

    public static JPanel baris(String label, JComponent c) {
        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.setOpaque(false);
        p.add(label(label), BorderLayout.NORTH);
        p.add(c, BorderLayout.CENTER);
        return p;
    }

    // ---------------- tabel ----------------

    public static JScrollPane gulir(Component c) {
        JScrollPane sp = new JScrollPane(c);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getViewport().setBackground(Tema.KARTU);
        sp.getVerticalScrollBar().setUnitIncrement(18);
        return sp;
    }

    public static void rapikanTabel(JTable t) {
        t.setFillsViewportHeight(true);
        t.setRowHeight(34);
        t.setFont(Tema.reg(13f));
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        t.getTableHeader().setReorderingAllowed(false);
        t.getTableHeader().setFont(Tema.mono(11f));
        DefaultTableCellRenderer h = new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable tb, Object v, boolean s, boolean f, int r, int c) {
                super.getTableCellRendererComponent(tb, v == null ? "" : v.toString().toUpperCase(), false, false, r, c);
                setFont(Tema.mono(11f));
                setForeground(Tema.GARIS_TERANG);
                setBackground(Tema.SIDEBAR);
                setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 1, Tema.GARIS), new EmptyBorder(0, 10, 0, 10)));
                return this;
            }
        };
        t.getTableHeader().setDefaultRenderer(h);
    }

    /** Kolom angka: rata kanan, monospace, pemisah ribuan. */
    public static void kolomAngka(JTable t, int... kolom) {
        for (int k : kolom) {
            t.getColumnModel().getColumn(k).setCellRenderer(new DefaultTableCellRenderer() {
                @Override public Component getTableCellRendererComponent(JTable tb, Object v, boolean s, boolean f, int r1, int c1) {
                    Object tampil = v instanceof Long || v instanceof Integer ? Rupiah.ribuan(((Number) v).longValue()) : v;
                    super.getTableCellRendererComponent(tb, tampil, s, false, r1, c1);
                    setHorizontalAlignment(SwingConstants.RIGHT);
                    setFont(Tema.mono(12.5f));
                    return this;
                }
            });
        }
    }

    /** Kolom kode: monospace berwarna cyan. */
    public static void kolomKode(JTable t, int... kolom) {
        for (int k : kolom) {
            t.getColumnModel().getColumn(k).setCellRenderer(new DefaultTableCellRenderer() {
                @Override public Component getTableCellRendererComponent(JTable tb, Object v, boolean s, boolean f, int r1, int c1) {
                    super.getTableCellRendererComponent(tb, v, s, false, r1, c1);
                    setFont(Tema.mono(12.5f));
                    setForeground(Tema.AKSEN);
                    return this;
                }
            });
        }
    }

    public static void lebarKolom(JTable t, int... lebar) {
        for (int i = 0; i < lebar.length && i < t.getColumnCount(); i++) {
            if (lebar[i] > 0) t.getColumnModel().getColumn(i).setPreferredWidth(lebar[i]);
        }
    }

    public static JPanel transparan(LayoutManager lm) {
        JPanel p = new JPanel(lm);
        p.setOpaque(false);
        return p;
    }

    /** FlowLayout yang membungkus ke baris baru bila lebar tidak cukup. */
    public static class AlirBungkus extends FlowLayout {
        public AlirBungkus(int hgap, int vgap) { super(LEFT, hgap, vgap); }

        @Override public Dimension preferredLayoutSize(Container target) { return ukur(target); }
        @Override public Dimension minimumLayoutSize(Container target) {
            Dimension d = ukur(target);
            d.width -= getHgap() + 1;
            return d;
        }

        private Dimension ukur(Container target) {
            synchronized (target.getTreeLock()) {
                int lebarMaks = target.getWidth();
                Container c = target;
                while (lebarMaks == 0 && c.getParent() != null) { c = c.getParent(); lebarMaks = c.getWidth(); }
                if (lebarMaks == 0) lebarMaks = Integer.MAX_VALUE;
                Insets in = target.getInsets();
                lebarMaks -= in.left + in.right + getHgap() * 2;
                int w = 0, h = 0, barisW = 0, barisH = 0;
                for (Component m : target.getComponents()) {
                    if (!m.isVisible()) continue;
                    Dimension d = m.getPreferredSize();
                    if (barisW + d.width > lebarMaks && barisW > 0) {
                        w = Math.max(w, barisW);
                        h += barisH + getVgap();
                        barisW = 0;
                        barisH = 0;
                    }
                    barisW += d.width + getHgap();
                    barisH = Math.max(barisH, d.height);
                }
                w = Math.max(w, barisW);
                h += barisH;
                return new Dimension(w + in.left + in.right, h + in.top + in.bottom + getVgap() * 2);
            }
        }
    }

    public static Component jarak(int w, int h) { return Box.createRigidArea(new Dimension(w, h)); }
}
