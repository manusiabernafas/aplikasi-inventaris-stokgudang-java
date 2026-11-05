package id.papicode.gudang.ui;

import id.papicode.gudang.util.Rupiah;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/** Grafik batang berkelompok (beberapa seri berdampingan), datar, dengan tooltip per kelompok. */
public class GrafikBatang extends JComponent {

    private final List<String> label = new ArrayList<>();
    private final List<long[]> nilai = new ArrayList<>();
    private final Color[] warna;
    private final String[] namaSeri;
    private int sorot = -1;
    private int jedaLabel = 1;

    public GrafikBatang(String[] namaSeri, Color... warna) {
        this.namaSeri = namaSeri;
        this.warna = warna;
        setPreferredSize(new Dimension(400, 220));
        MouseAdapter m = new MouseAdapter() {
            @Override public void mouseMoved(MouseEvent e) {
                int i = indeksDi(e.getX());
                if (i != sorot) { sorot = i; repaint(); }
                if (i < 0) { setToolTipText(null); return; }
                StringBuilder sb = new StringBuilder("<html><b>" + label.get(i) + "</b>");
                for (int s = 0; s < namaSeri.length; s++) sb.append("<br>").append(namaSeri[s]).append(": ").append(Rupiah.ribuan(nilai.get(i)[s]));
                setToolTipText(sb.append("</html>").toString());
            }
            @Override public void mouseExited(MouseEvent e) { sorot = -1; repaint(); }
        };
        addMouseMotionListener(m);
        addMouseListener(m);
    }

    public void isi(List<String> l, List<long[]> v, int jedaLabel) {
        label.clear();
        nilai.clear();
        label.addAll(l);
        nilai.addAll(v);
        this.jedaLabel = Math.max(1, jedaLabel);
        repaint();
    }

    private static final int KIRI = 48, BAWAH = 24, ATAS = 8;

    private int indeksDi(int x) {
        if (nilai.isEmpty()) return -1;
        double lebar = (getWidth() - KIRI - 4.0) / nilai.size();
        int i = (int) ((x - KIRI) / lebar);
        return i >= 0 && i < nilai.size() ? i : -1;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int w = getWidth(), h = getHeight();
        int tinggiArea = h - BAWAH - ATAS;
        long maks = 1;
        for (long[] v : nilai) for (long x : v) maks = Math.max(maks, x);
        long langkah = langkahBulat(maks / 4.0);
        long puncak = langkah * 4;

        g2.setFont(Tema.mono(10.5f));
        FontMetrics fm = g2.getFontMetrics();
        Stroke putus = new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{3f, 4f}, 0f);
        for (int i = 0; i <= 4; i++) {
            int y = ATAS + tinggiArea - (int) (tinggiArea * i / 4.0);
            g2.setColor(i == 0 ? Tema.GARIS_TERANG : Tema.GARIS);
            g2.setStroke(i == 0 ? new BasicStroke(1f) : putus);
            g2.drawLine(KIRI, y, w - 4, y);
            g2.setColor(Tema.TEKS_2);
            String s = Rupiah.ringkas(langkah * i);
            g2.drawString(s, KIRI - 8 - fm.stringWidth(s), y + fm.getAscent() / 2 - 1);
        }
        g2.setStroke(new BasicStroke(1f));
        if (nilai.isEmpty()) { g2.dispose(); return; }
        double lebar = (w - KIRI - 4.0) / nilai.size();
        int seri = warna.length;
        int lebarKelompok = (int) Math.max(seri * 2, Math.min(56, lebar * 0.7));
        int lebarBatang = Math.max(2, lebarKelompok / seri);
        for (int i = 0; i < nilai.size(); i++) {
            int x0 = (int) (KIRI + lebar * i + (lebar - lebarBatang * seri) / 2);
            if (i == sorot) {
                g2.setColor(Tema.PILIH);
                g2.fillRect((int) (KIRI + lebar * i), ATAS, (int) Math.ceil(lebar), tinggiArea);
            }
            for (int s = 0; s < seri; s++) {
                long v = nilai.get(i)[s];
                int bh = (int) Math.round(tinggiArea * (v / (double) puncak));
                if (bh <= 0) continue;
                g2.setColor(warna[s]);
                g2.fillRect(x0 + s * lebarBatang, ATAS + tinggiArea - bh, lebarBatang - (seri > 1 ? 1 : 0), bh);
            }
            if (i % jedaLabel == 0) {
                g2.setColor(Tema.TEKS_2);
                String s = label.get(i);
                g2.drawString(s, (int) (KIRI + lebar * i + (lebar - fm.stringWidth(s)) / 2), h - 6);
            }
        }
        g2.dispose();
    }

    static long langkahBulat(double kasar) {
        if (kasar <= 1) return 1;
        double pangkat = Math.pow(10, Math.floor(Math.log10(kasar)));
        double[] kandidat = {1, 2, 2.5, 5, 10};
        for (double c : kandidat) if (c * pangkat >= kasar) return (long) Math.ceil(c * pangkat);
        return (long) (10 * pangkat);
    }

    /** Legenda kecil: kotak warna + nama seri. */
    public JPanel legenda() {
        JPanel p = Ui.transparan(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        for (int s = 0; s < warna.length; s++) {
            Color c = warna[s];
            JLabel l = new JLabel(namaSeri[s].toUpperCase(), new Icon() {
                public void paintIcon(Component cmp, Graphics g, int x, int y) { g.setColor(c); g.fillRect(x, y + 1, 10, 10); }
                public int getIconWidth() { return 10; }
                public int getIconHeight() { return 12; }
            }, SwingConstants.LEFT);
            l.setFont(Tema.mono(10.5f));
            l.setForeground(Tema.TEKS_2);
            p.add(l);
        }
        return p;
    }
}
