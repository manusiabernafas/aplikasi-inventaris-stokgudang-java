package id.papicode.gudang.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.Map;

/** Potongan tampilan yang dipakai banyak halaman: kartu statistik, panel berjudul, pil status di tabel. */
public final class Bagian {
    private Bagian() { }

    /** Kartu angka besar dengan garis warna di kiri. */
    public static JPanel stat(String label, JLabel nilai, JLabel ket, Color aksen) {
        Ui.Kartu c = new Ui.Kartu(new BorderLayout(0, 4)) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(aksen);
                g.fillRect(0, 0, 4, getHeight());
            }
        };
        c.setBorder(new EmptyBorder(14, 20, 14, 16));
        c.add(Ui.label(label), BorderLayout.NORTH);
        c.add(nilai, BorderLayout.CENTER);
        c.add(ket, BorderLayout.SOUTH);
        return c;
    }

    public static JPanel panel(String judul, JComponent isi) { return panel(judul, isi, null); }

    /** Panel berjudul anotasi dengan komponen opsional di kanan judul. */
    public static JPanel panel(String judul, JComponent isi, JComponent kanan) {
        Ui.Kartu c = new Ui.Kartu(new BorderLayout(0, 10));
        c.setBorder(new EmptyBorder(14, 16, 14, 16));
        JPanel atas = Ui.transparan(new BorderLayout());
        JLabel j = new JLabel(judul);
        j.setFont(Tema.bold(14f));
        j.setForeground(Tema.TEKS);
        atas.add(j, BorderLayout.WEST);
        if (kanan != null) atas.add(kanan, BorderLayout.EAST);
        c.add(atas, BorderLayout.NORTH);
        c.add(isi, BorderLayout.CENTER);
        c.setPreferredSize(new Dimension(100, 220));
        return c;
    }

    /** Tabel dalam kartu tanpa padding besar. */
    public static JPanel tabel(JTable t) {
        Ui.Kartu w = new Ui.Kartu(new BorderLayout());
        w.setBorder(new EmptyBorder(1, 1, 1, 1));
        w.add(Ui.gulir(t));
        return w;
    }

    /** Renderer pil bergaris: teks sel dipetakan ke warna. */
    public static class Pil extends DefaultTableCellRenderer {
        private final Map<String, Color> warna;
        private Color c = Tema.TEKS_2;

        public Pil(Map<String, Color> warna) { this.warna = warna; }

        @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int col) {
            super.getTableCellRendererComponent(t, v == null ? "" : v.toString().toUpperCase(), s, false, r, col);
            c = warna.getOrDefault(String.valueOf(v), Tema.TEKS_2);
            setForeground(c);
            setFont(Tema.monoBold(10.5f));
            setHorizontalAlignment(CENTER);
            return this;
        }

        @Override protected void paintComponent(Graphics g) {
            g.setColor(getBackground());
            g.fillRect(0, 0, getWidth(), getHeight());
            int w = Math.min(getWidth() - 14, 92), h = 20;
            g.setColor(c);
            g.drawRect((getWidth() - w) / 2, (getHeight() - h) / 2, w, h);
            setOpaque(false);
            super.paintComponent(g);
            setOpaque(true);
        }
    }

    public static final Map<String, Color> WARNA_STATUS = Map.of(
            "Aman", Tema.HIJAU, "Menipis", Tema.UTAMA, "Habis", Tema.MERAH, "Nonaktif", Tema.TEKS_2,
            "Aktif", Tema.HIJAU, "Batal", Tema.MERAH, "Masuk", Tema.HIJAU, "Keluar", Tema.MERAH, "Opname", Tema.AKSEN);
}
