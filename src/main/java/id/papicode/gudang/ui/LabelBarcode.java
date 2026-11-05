package id.papicode.gudang.ui;

import id.papicode.gudang.model.Barang;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.util.ArrayList;
import java.util.List;

/**
 * Label rak/barang dengan barcode Code 128 (set B), dicetak di kertas A4: 3 kolom x 8 baris (70 x 37 mm).
 * Barcode digambar sendiri, tanpa library tambahan, sehingga bisa dibaca scanner USB biasa.
 */
public final class LabelBarcode implements Printable {

    /** Pola lebar garis-spasi Code 128 untuk nilai 0..106 (106 = stop). */
    static final String[] POLA = {
        "212222", "222122", "222221", "121223", "121322", "131222", "122213", "122312", "132212", "221213",
        "221312", "231212", "112232", "122132", "122231", "113222", "123122", "123221", "223211", "221132",
        "221231", "213212", "223112", "312131", "311222", "321122", "321221", "312212", "322112", "322211",
        "212123", "212321", "232121", "111323", "131123", "131321", "112313", "132113", "132311", "211313",
        "231113", "231311", "112133", "112331", "132131", "113123", "113321", "133121", "313121", "211331",
        "231131", "213113", "213311", "213131", "311123", "311321", "331121", "312113", "312311", "332111",
        "314111", "221411", "431111", "111224", "111422", "121124", "121421", "141122", "141221", "112214",
        "112412", "122114", "122411", "142112", "142211", "241211", "221114", "413111", "241112", "134111",
        "111242", "121142", "121241", "114212", "124112", "124211", "411212", "421112", "421211", "212141",
        "214121", "412121", "111143", "111341", "131141", "114113", "114311", "411113", "411311", "113141",
        "114131", "311141", "411131", "211412", "211214", "211232", "2331112"
    };

    /** Urutan lebar modul (garis, spasi, garis, ...) untuk teks ASCII 32..126, termasuk start B, checksum, stop. */
    public static List<Integer> code128(String teks) {
        List<Integer> nilai = new ArrayList<>();
        nilai.add(104); // start B
        int cek = 104;
        for (int i = 0; i < teks.length(); i++) {
            char ch = teks.charAt(i);
            if (ch < 32 || ch > 126) throw new IllegalArgumentException("Karakter tidak didukung Code 128-B: " + ch);
            int v = ch - 32;
            nilai.add(v);
            cek += v * (i + 1);
        }
        nilai.add(cek % 103);
        nilai.add(106);
        List<Integer> modul = new ArrayList<>();
        for (int v : nilai) for (char c : POLA[v].toCharArray()) modul.add(c - '0');
        return modul;
    }

    /** Gambar barcode memenuhi kotak (x, y, w, h). Garis gelap pada indeks genap. */
    public static void gambarBarcode(Graphics2D g, String teks, double x, double y, double w, double h) {
        List<Integer> m = code128(teks);
        int total = m.stream().mapToInt(Integer::intValue).sum() + 20; // 10 modul zona sepi kiri-kanan
        double unit = w / total;
        double cx = x + unit * 10;
        for (int i = 0; i < m.size(); i++) {
            double lebar = m.get(i) * unit;
            if (i % 2 == 0) g.fill(new Rectangle2D.Double(cx, y, lebar, h));
            cx += lebar;
        }
    }

    private final List<Barang> barang;   // satu entri = satu label
    private final String perusahaan;

    public LabelBarcode(List<Barang> barang, String perusahaan) {
        this.barang = barang;
        this.perusahaan = perusahaan;
    }

    public static final int KOLOM = 3, BARIS = 8;

    public int jumlahHalaman() { return Math.max(1, (barang.size() + KOLOM * BARIS - 1) / (KOLOM * BARIS)); }

    @Override
    public int print(Graphics g, PageFormat pf, int halaman) {
        if (halaman >= jumlahHalaman()) return NO_SUCH_PAGE;
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        double mm = 72 / 25.4;
        double lw = 70 * mm, lh = 37 * mm;
        double ox = (pf.getWidth() - KOLOM * lw) / 2, oy = (pf.getHeight() - BARIS * lh) / 2;
        int mulai = halaman * KOLOM * BARIS;
        for (int i = 0; i < KOLOM * BARIS && mulai + i < barang.size(); i++) {
            Barang b = barang.get(mulai + i);
            double x = ox + (i % KOLOM) * lw, y = oy + (i / KOLOM) * lh;
            g2.setColor(new Color(0xBBBBBB));
            g2.draw(new Rectangle2D.Double(x, y, lw, lh));
            g2.setColor(Color.BLACK);
            g2.setFont(Tema.bold(8f));
            String nama = b.nama().length() > 38 ? b.nama().substring(0, 37) + "." : b.nama();
            g2.drawString(nama, (float) (x + 3 * mm), (float) (y + 5.5 * mm));
            g2.setFont(Tema.mono(6.5f));
            g2.drawString((b.lokasi() == null ? "" : "RAK " + b.lokasi() + "  ") + b.satuan().toUpperCase(), (float) (x + 3 * mm), (float) (y + 9 * mm));
            gambarBarcode(g2, b.kode(), x + 2 * mm, y + 11 * mm, lw - 4 * mm, 16 * mm);
            g2.setFont(Tema.monoBold(8f));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(b.kode(), (float) (x + (lw - fm.stringWidth(b.kode())) / 2), (float) (y + 31 * mm));
            g2.setFont(Tema.mono(5.5f));
            g2.drawString(perusahaan, (float) (x + 3 * mm), (float) (y + 35 * mm));
        }
        return PAGE_EXISTS;
    }
}
