package id.papicode.gudang.ui;

import id.papicode.gudang.service.Gagal;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.function.BooleanSupplier;

/** Dialog bergaya tema: pesan, konfirmasi, dan form. */
public final class Kotak {
    private Kotak() { }

    /** Jalankan aksi; pesan Gagal ditampilkan apa adanya, error lain ditampilkan umum. */
    public static boolean aman(Component induk, Runnable r) {
        try {
            r.run();
            return true;
        } catch (Gagal g) {
            pesan(induk, "Belum bisa diproses", g.getMessage(), true);
        } catch (RuntimeException e) {
            e.printStackTrace();
            pesan(induk, "Terjadi kesalahan", e.getMessage() == null ? e.toString() : e.getMessage(), true);
        }
        return false;
    }

    public static void pesan(Component induk, String judul, String isi, boolean peringatan) {
        JDialog d = buat(induk, judul);
        JPanel root = akar();
        JPanel atas = Ui.transparan(new BorderLayout(14, 0));
        atas.add(lingkaranIkon(peringatan ? "alert" : "check", peringatan ? Tema.MERAH : Tema.UTAMA,
                peringatan ? Tema.KARTU_2 : Tema.KARTU_2), BorderLayout.WEST);
        JPanel teks = Ui.transparan(new BorderLayout(0, 6));
        teks.add(Ui.judul(judul, 17f), BorderLayout.NORTH);
        teks.add(paragraf(isi), BorderLayout.CENTER);
        atas.add(teks, BorderLayout.CENTER);
        root.add(atas, BorderLayout.CENTER);
        Ui.Tombol ok = Ui.tombol("Oke", null, Ui.Jenis.UTAMA);
        ok.addActionListener(e -> d.dispose());
        root.add(barisTombol(ok), BorderLayout.SOUTH);
        tampil(d, root, ok);
    }

    public static boolean konfirmasi(Component induk, String judul, String isi, String labelYa, boolean bahaya) {
        JDialog d = buat(induk, judul);
        boolean[] hasil = {false};
        JPanel root = akar();
        JPanel atas = Ui.transparan(new BorderLayout(14, 0));
        atas.add(lingkaranIkon(bahaya ? "alert" : "check", bahaya ? Tema.MERAH : Tema.UTAMA,
                bahaya ? Tema.KARTU_2 : Tema.KARTU_2), BorderLayout.WEST);
        JPanel teks = Ui.transparan(new BorderLayout(0, 6));
        teks.add(Ui.judul(judul, 17f), BorderLayout.NORTH);
        teks.add(paragraf(isi), BorderLayout.CENTER);
        atas.add(teks, BorderLayout.CENTER);
        root.add(atas, BorderLayout.CENTER);
        Ui.Tombol ya = Ui.tombol(labelYa, null, bahaya ? Ui.Jenis.BAHAYA : Ui.Jenis.UTAMA);
        Ui.Tombol batal = Ui.tombol("Batal", null, Ui.Jenis.GARIS);
        ya.addActionListener(e -> { hasil[0] = true; d.dispose(); });
        batal.addActionListener(e -> d.dispose());
        root.add(barisTombol(batal, ya), BorderLayout.SOUTH);
        tampil(d, root, ya);
        return hasil[0];
    }

    /** Minta satu isian teks. Mengembalikan null bila dibatalkan. */
    public static String input(Component induk, String judul, String label, String awal) {
        JTextField f = Ui.field("");
        f.setText(awal == null ? "" : awal);
        f.setColumns(24);
        String[] hasil = {null};
        form(induk, judul, Ui.baris(label, f), "Simpan", () -> { hasil[0] = f.getText(); return true; });
        return hasil[0];
    }

    /**
     * Dialog form umum. aksiSimpan dipanggil saat tombol utama ditekan; kembalikan true untuk menutup.
     * Pesan Gagal dari aksiSimpan ditampilkan dan dialog tetap terbuka.
     */
    public static JDialog form(Component induk, String judul, JComponent isi, String labelSimpan, BooleanSupplier aksiSimpan) {
        JDialog d = buat(induk, judul);
        JPanel root = akar();
        root.add(Ui.judul(judul, 19f), BorderLayout.NORTH);
        isi.setOpaque(false);
        root.add(isi, BorderLayout.CENTER);
        Ui.Tombol simpan = Ui.tombol(labelSimpan, "check", Ui.Jenis.UTAMA);
        Ui.Tombol batal = Ui.tombol("Batal", null, Ui.Jenis.GARIS);
        simpan.addActionListener(e -> {
            boolean[] tutup = {false};
            if (aman(d, () -> tutup[0] = aksiSimpan.getAsBoolean()) && tutup[0]) d.dispose();
        });
        batal.addActionListener(e -> d.dispose());
        root.add(barisTombol(batal, simpan), BorderLayout.SOUTH);
        tampil(d, root, simpan);
        return d;
    }

    // ---------------- bagian dalam ----------------

    static JDialog buat(Component induk, String judul) {
        Window w = induk == null ? null : (induk instanceof Window win ? win : SwingUtilities.getWindowAncestor(induk));
        JDialog d = new JDialog(w, judul, java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        d.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        d.getRootPane().registerKeyboardAction(e -> d.dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        return d;
    }

    static JPanel akar() {
        JPanel root = new JPanel(new BorderLayout(0, 20));
        root.setBackground(Tema.KARTU);
        root.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(3, 0, 0, 0, Tema.UTAMA), new EmptyBorder(22, 26, 20, 26)));
        return root;
    }

    static void tampil(JDialog d, JPanel root, JButton utama) {
        d.setContentPane(root);
        d.getRootPane().setDefaultButton(utama);
        d.pack();
        d.setMinimumSize(new Dimension(Math.max(380, d.getWidth()), d.getHeight()));
        d.setLocationRelativeTo(d.getOwner());
        d.setResizable(false);
        d.setVisible(true);
    }

    static JPanel barisTombol(JButton... tombol) {
        JPanel p = Ui.transparan(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        for (JButton b : tombol) p.add(b);
        return p;
    }

    static JComponent paragraf(String isi) {
        JTextArea t = new JTextArea(isi);
        t.setLineWrap(true);
        t.setWrapStyleWord(true);
        t.setEditable(false);
        t.setFocusable(false);
        t.setOpaque(false);
        t.setFont(Tema.reg(14f));
        t.setForeground(Tema.TEKS_2);
        t.setColumns(30);
        t.setBorder(null);
        return t;
    }

    static JComponent lingkaranIkon(String ikon, Color warna, Color latar) {
        JLabel l = new JLabel(Ikon.of(ikon, 22, warna)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(latar);
                g2.fillRect(0, 0, 44, 44);
                g2.setColor(warna);
                g2.drawRect(0, 0, 43, 43);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        l.setPreferredSize(new Dimension(44, 44));
        l.setHorizontalAlignment(SwingConstants.CENTER);
        l.setVerticalAlignment(SwingConstants.CENTER);
        JPanel wrap = Ui.transparan(new BorderLayout());
        wrap.add(l, BorderLayout.NORTH);
        return wrap;
    }
}
