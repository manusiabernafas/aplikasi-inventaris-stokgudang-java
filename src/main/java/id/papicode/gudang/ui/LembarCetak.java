package id.papicode.gudang.ui;

import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.util.ArrayList;
import java.util.List;

/**
 * Dokumen cetak A4 hitam-putih: kop perusahaan, judul, info, tabel berhalaman, baris total,
 * catatan, dan kolom tanda tangan. Dipakai untuk bukti barang masuk, surat jalan, berita acara opname,
 * lembar hitung opname, kartu stok, dan laporan mutasi.
 */
public class LembarCetak implements Printable {

    private final String[] kop;
    private final String judul;
    private final List<String[]> info = new ArrayList<>();
    private String[] header = {};
    private double[] lebar = {};
    private boolean[] kanan = {};
    private final List<String[]> baris = new ArrayList<>();
    private String[] total;
    private String catatan;
    private String[][] ttd;
    private String tempatTanggal;

    private static final double RH = 15, KOP = 64, JUDUL = 34, TTD = 110;

    public LembarCetak(String judul, String... kop) {
        this.judul = judul;
        this.kop = kop;
    }

    public LembarCetak info(String label, String nilai) { info.add(new String[]{label, nilai == null ? "" : nilai}); return this; }

    public LembarCetak kolom(String[] header, double[] lebar, boolean[] kanan) {
        this.header = header;
        this.lebar = lebar;
        this.kanan = kanan;
        return this;
    }

    public LembarCetak baris(String... sel) { baris.add(sel); return this; }
    public LembarCetak total(String... sel) { total = sel; return this; }
    public LembarCetak catatan(String c) { catatan = c; return this; }

    /** Kolom tanda tangan: pasangan {peran, nama}. Nama kosong = garis titik untuk diisi tangan. */
    public LembarCetak tandaTangan(String tempatTanggal, String[]... ttd) {
        this.tempatTanggal = tempatTanggal;
        this.ttd = ttd;
        return this;
    }

    private double tinggiInfo() { return Math.ceil(info.size() / 2.0) * 14 + (info.isEmpty() ? 0 : 10); }

    /** Pembagian baris per halaman. */
    private List<int[]> halaman(PageFormat pf) {
        double tinggi = pf.getImageableHeight() - 20; // footer
        List<int[]> out = new ArrayList<>();
        int i = 0;
        boolean pertama = true;
        int n = baris.size() + (total != null ? 1 : 0);
        do {
            double ruang = tinggi - (pertama ? KOP + JUDUL + tinggiInfo() : 10) - RH - 4;
            int muat = Math.max(1, (int) (ruang / RH));
            int akhir = Math.min(n, i + muat);
            out.add(new int[]{i, akhir});
            i = akhir;
            pertama = false;
        } while (i < n);
        // pastikan ada ruang untuk catatan + tanda tangan di halaman terakhir
        int[] last = out.get(out.size() - 1);
        double pakai = (out.size() == 1 ? KOP + JUDUL + tinggiInfo() : 10) + RH + 4 + (last[1] - last[0]) * RH;
        double perlu = (catatan != null ? 28 : 0) + (ttd != null ? TTD : 0);
        if (pakai + perlu > tinggi) out.add(new int[]{n, n});
        return out;
    }

    public int jumlahHalaman(PageFormat pf) { return halaman(pf).size(); }

    @Override
    public int print(Graphics g, PageFormat pf, int hal) {
        List<int[]> h = halaman(pf);
        if (hal >= h.size()) return NO_SUCH_PAGE;
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        double x0 = pf.getImageableX(), y = pf.getImageableY(), w = pf.getImageableWidth();
        g2.setColor(Color.BLACK);
        if (hal == 0) {
            g2.setFont(Tema.black(14f));
            g2.drawString(kop[0], (float) x0, (float) (y + 14));
            g2.setFont(Tema.reg(8.5f));
            for (int i = 1; i < kop.length; i++) g2.drawString(kop[i], (float) x0, (float) (y + 14 + i * 12));
            y += KOP - 8;
            g2.setStroke(new BasicStroke(1.5f));
            g2.draw(new Line2D.Double(x0, y, x0 + w, y));
            g2.setStroke(new BasicStroke(0.5f));
            g2.draw(new Line2D.Double(x0, y + 3, x0 + w, y + 3));
            y += 8;
            g2.setFont(Tema.black(12.5f));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(judul, (float) (x0 + (w - fm.stringWidth(judul)) / 2), (float) (y + 18));
            y += JUDUL;
            g2.setFont(Tema.reg(8.5f));
            for (int i = 0; i < info.size(); i++) {
                double ix = x0 + (i % 2) * (w / 2);
                double iy = y + (i / 2) * 14;
                g2.setFont(Tema.mono(7.5f));
                g2.drawString(info.get(i)[0].toUpperCase(), (float) ix, (float) iy);
                g2.setFont(Tema.reg(8.5f));
                g2.drawString(": " + info.get(i)[1], (float) (ix + 78), (float) iy);
            }
            y += tinggiInfo();
        } else {
            g2.setFont(Tema.mono(7.5f));
            g2.drawString(judul + " (lanjutan)", (float) x0, (float) (y + 8));
            y += 10;
        }

        int[] rentang = h.get(hal);
        if (rentang[1] > rentang[0] || hal == 0) {
            // header tabel
            double[] xs = new double[lebar.length + 1];
            xs[0] = x0;
            for (int i = 0; i < lebar.length; i++) xs[i + 1] = xs[i] + lebar[i] * w;
            g2.setColor(new Color(0xE6E6E6));
            g2.fill(new java.awt.geom.Rectangle2D.Double(x0, y, w, RH + 2));
            g2.setColor(Color.BLACK);
            g2.setFont(Tema.monoBold(7f));
            for (int i = 0; i < header.length; i++) sel(g2, header[i].toUpperCase(), xs[i], xs[i + 1], y + 11, kanan[i]);
            g2.setStroke(new BasicStroke(0.6f));
            g2.draw(new Line2D.Double(x0, y, x0 + w, y));
            y += RH + 2;
            g2.draw(new Line2D.Double(x0, y, x0 + w, y));
            for (int r = rentang[0]; r < rentang[1]; r++) {
                boolean tot = r >= baris.size();
                String[] s = tot ? total : baris.get(r);
                g2.setFont(tot ? Tema.bold(8f) : Tema.reg(8f));
                for (int i = 0; i < s.length && i < header.length; i++) {
                    if (kanan[i] && !tot) g2.setFont(Tema.mono(7.5f));
                    else g2.setFont(tot ? Tema.bold(8f) : Tema.reg(8f));
                    sel(g2, s[i], xs[i], xs[i + 1], y + 11, kanan[i]);
                }
                y += RH;
                g2.setColor(tot ? Color.BLACK : new Color(0xCCCCCC));
                g2.draw(new Line2D.Double(x0, y, x0 + w, y));
                g2.setColor(Color.BLACK);
            }
        }
        if (hal == h.size() - 1) {
            y += 14;
            if (catatan != null && !catatan.isBlank()) {
                g2.setFont(Tema.reg(8f));
                g2.drawString("Catatan: " + catatan, (float) x0, (float) y);
                y += 14;
            }
            if (ttd != null) {
                g2.setFont(Tema.reg(8.5f));
                if (tempatTanggal != null) {
                    FontMetrics fm = g2.getFontMetrics();
                    g2.drawString(tempatTanggal, (float) (x0 + w - fm.stringWidth(tempatTanggal)), (float) (y + 6));
                }
                y += 22;
                double kw = w / ttd.length;
                for (int i = 0; i < ttd.length; i++) {
                    double cx = x0 + kw * i + kw / 2;
                    tengah(g2, ttd[i][0], cx, y);
                    String nama = ttd[i][1] == null || ttd[i][1].isBlank() ? "(......................................)" : "( " + ttd[i][1] + " )";
                    tengah(g2, nama, cx, y + 62);
                }
            }
        }
        g2.setFont(Tema.mono(6.5f));
        g2.setColor(new Color(0x666666));
        String kaki = "Halaman " + (hal + 1) + " dari " + h.size();
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(kaki, (float) (x0 + w - fm.stringWidth(kaki)), (float) (pf.getImageableY() + pf.getImageableHeight() - 4));
        return PAGE_EXISTS;
    }

    private static void sel(Graphics2D g2, String s, double x1, double x2, double y, boolean kanan) {
        if (s == null) s = "";
        FontMetrics fm = g2.getFontMetrics();
        double ruang = x2 - x1 - 8;
        while (s.length() > 1 && fm.stringWidth(s) > ruang) s = s.substring(0, s.length() - 2) + ".";
        double x = kanan ? x2 - 4 - fm.stringWidth(s) : x1 + 4;
        g2.drawString(s, (float) x, (float) y);
    }

    private static void tengah(Graphics2D g2, String s, double cx, double y) {
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(s, (float) (cx - fm.stringWidth(s) / 2.0), (float) y);
    }
}
