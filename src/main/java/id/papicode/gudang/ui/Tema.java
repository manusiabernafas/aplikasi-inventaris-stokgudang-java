package id.papicode.gudang.ui;

import com.formdev.flatlaf.FlatDarkLaf;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import java.awt.*;
import java.io.InputStream;

/**
 * Tema "Blueprint": kertas cetak biru dengan kisi garis tipis, garis putih kebiruan, dan kuning
 * keselamatan untuk aksi utama. Warna solid, sudut nyaris siku seperti gambar teknik.
 * Font: Chivo (teks) + Space Mono (angka, kode, keterangan kecil). Keduanya SIL OFL.
 */
public final class Tema {

    public static final Color LATAR = new Color(0x1A4A82);
    public static final Color KISI = new Color(0x205291);
    public static final Color KISI_BESAR = new Color(0x2A5E9D);
    public static final Color KARTU = new Color(0x143B69);
    public static final Color KARTU_2 = new Color(0x17416F);
    public static final Color GARIS = new Color(0x4D7BB5);
    public static final Color GARIS_TERANG = new Color(0x9FC0E6);
    public static final Color TEKS = new Color(0xF3F7FD);
    public static final Color TEKS_2 = new Color(0xA7C1E2);
    public static final Color UTAMA = new Color(0xFFD23F);       // kuning keselamatan
    public static final Color UTAMA_HOVER = new Color(0xFFE07A);
    public static final Color UTAMA_TEKS = new Color(0x10294A);
    public static final Color AKSEN = new Color(0x79D4F7);       // cyan
    public static final Color HIJAU = new Color(0x7DDBA3);       // barang masuk
    public static final Color MERAH = new Color(0xFF8566);       // barang keluar / bahaya
    public static final Color SIDEBAR = new Color(0x0F2C50);
    public static final Color PILIH = new Color(0x25609F);

    public static final int SUDUT = 4;

    public static Font F_REG, F_BOLD, F_BLACK, F_MONO, F_MONO_BOLD;

    private Tema() { }

    /** Muat font sekali (dipanggil otomatis, juga aman dipakai tanpa layar seperti saat tes/cetak). */
    public static synchronized void muatFont() {
        if (F_REG != null) return;
        F_REG = muat("Chivo-Regular.ttf", Font.SANS_SERIF, Font.PLAIN);
        F_BOLD = muat("Chivo-Bold.ttf", Font.SANS_SERIF, Font.BOLD);
        F_BLACK = muat("Chivo-Black.ttf", Font.SANS_SERIF, Font.BOLD);
        F_MONO = muat("SpaceMono-Regular.ttf", Font.MONOSPACED, Font.PLAIN);
        F_MONO_BOLD = muat("SpaceMono-Bold.ttf", Font.MONOSPACED, Font.BOLD);
    }

    public static void pasang() {
        muatFont();
        FlatDarkLaf.setup();
        UIManager.put("defaultFont", new FontUIResource(F_REG.deriveFont(13.5f)));
        UIManager.put("Component.arc", SUDUT);
        UIManager.put("Button.arc", SUDUT);
        UIManager.put("TextComponent.arc", SUDUT);
        UIManager.put("CheckBox.arc", 2);
        UIManager.put("Component.focusWidth", 0);
        UIManager.put("Component.innerFocusWidth", 1);
        UIManager.put("Component.focusedBorderColor", UTAMA);
        UIManager.put("Component.borderColor", GARIS);
        UIManager.put("Component.disabledBorderColor", KISI_BESAR);
        UIManager.put("Component.accentColor", UTAMA);
        UIManager.put("Panel.background", LATAR);
        UIManager.put("RootPane.background", LATAR);
        UIManager.put("OptionPane.background", KARTU);
        for (String k : new String[]{"TextField", "PasswordField", "FormattedTextField", "ComboBox", "Spinner", "TextArea"}) {
            UIManager.put(k + ".background", new Color(0x113560));
            UIManager.put(k + ".foreground", TEKS);
            UIManager.put(k + ".disabledBackground", KARTU);
            UIManager.put(k + ".inactiveBackground", KARTU);
        }
        UIManager.put("ComboBox.buttonBackground", new Color(0x113560));
        UIManager.put("ComboBox.buttonArrowColor", GARIS_TERANG);
        UIManager.put("ComboBox.popupBackground", KARTU);
        UIManager.put("ComboBox.selectionBackground", PILIH);
        UIManager.put("List.background", KARTU);
        UIManager.put("List.selectionBackground", PILIH);
        UIManager.put("TextField.placeholderForeground", new Color(0x7E9CC4));
        UIManager.put("TextField.margin", new Insets(6, 10, 6, 10));
        UIManager.put("PasswordField.margin", new Insets(6, 10, 6, 10));
        UIManager.put("Table.background", KARTU);
        UIManager.put("Table.foreground", TEKS);
        UIManager.put("Table.alternateRowColor", KARTU_2);
        UIManager.put("Table.selectionBackground", PILIH);
        UIManager.put("Table.selectionForeground", TEKS);
        UIManager.put("Table.selectionInactiveBackground", PILIH);
        UIManager.put("Table.selectionInactiveForeground", TEKS);
        UIManager.put("Table.showHorizontalLines", true);
        UIManager.put("Table.showVerticalLines", true);
        UIManager.put("Table.gridColor", new Color(0x24507F));
        UIManager.put("Table.cellMargins", new Insets(4, 10, 4, 10));
        UIManager.put("TableHeader.background", SIDEBAR);
        UIManager.put("TableHeader.foreground", GARIS_TERANG);
        UIManager.put("TableHeader.separatorColor", new Color(0x24507F));
        UIManager.put("TableHeader.bottomSeparatorColor", GARIS);
        UIManager.put("TableHeader.height", 36);
        UIManager.put("TableHeader.cellMargins", new Insets(4, 10, 4, 10));
        UIManager.put("ScrollBar.thumbArc", 0);
        UIManager.put("ScrollBar.thumbInsets", new Insets(2, 2, 2, 2));
        UIManager.put("ScrollBar.width", 10);
        UIManager.put("ScrollBar.track", KARTU);
        UIManager.put("ScrollBar.thumb", GARIS);
        UIManager.put("ScrollBar.hoverThumbColor", GARIS_TERANG);
        UIManager.put("ScrollPane.border", BorderFactory.createEmptyBorder());
        UIManager.put("ScrollPane.background", KARTU);
        UIManager.put("Viewport.background", KARTU);
        UIManager.put("CheckBox.icon.checkmarkColor", UTAMA_TEKS);
        UIManager.put("CheckBox.icon.selectedBackground", UTAMA);
        UIManager.put("CheckBox.icon.selectedBorderColor", UTAMA);
        UIManager.put("CheckBox.icon.borderColor", GARIS_TERANG);
        UIManager.put("CheckBox.icon.background", KARTU);
        UIManager.put("ToolTip.background", UTAMA);
        UIManager.put("ToolTip.foreground", UTAMA_TEKS);
        UIManager.put("ToolTip.border", BorderFactory.createEmptyBorder(5, 9, 5, 9));
        UIManager.put("Spinner.buttonStyle", "none");
        UIManager.put("PopupMenu.background", KARTU);
        UIManager.put("FileChooser.background", KARTU);
        UIManager.put("Label.foreground", TEKS);
        UIManager.put("CheckBox.foreground", TEKS);
        UIManager.put("Button.background", KARTU);
        UIManager.put("Button.foreground", TEKS);
        UIManager.put("Button.borderColor", GARIS);
    }

    private static Font muat(String file, String cadangan, int gaya) {
        try (InputStream in = Tema.class.getResourceAsStream("/fonts/" + file)) {
            Font f = Font.createFont(Font.TRUETYPE_FONT, in);
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(f);
            return f.deriveFont(14f);
        } catch (Exception e) {
            return new Font(cadangan, gaya, 14);
        }
    }

    public static Font reg(float s) { muatFont(); return F_REG.deriveFont(s); }
    public static Font bold(float s) { muatFont(); return F_BOLD.deriveFont(s); }
    public static Font black(float s) { muatFont(); return F_BLACK.deriveFont(s); }
    public static Font mono(float s) { muatFont(); return F_MONO.deriveFont(s); }
    public static Font monoBold(float s) { muatFont(); return F_MONO_BOLD.deriveFont(s); }

    /** Latar kertas cetak biru: kisi kecil tiap 20px, kisi besar tiap 100px. */
    public static void gambarKisi(Graphics g, int w, int h) {
        g.setColor(LATAR);
        g.fillRect(0, 0, w, h);
        g.setColor(KISI);
        for (int x = 0; x < w; x += 20) g.drawLine(x, 0, x, h);
        for (int y = 0; y < h; y += 20) g.drawLine(0, y, w, y);
        g.setColor(KISI_BESAR);
        for (int x = 0; x < w; x += 100) g.drawLine(x, 0, x, h);
        for (int y = 0; y < h; y += 100) g.drawLine(0, y, w, y);
    }
}
