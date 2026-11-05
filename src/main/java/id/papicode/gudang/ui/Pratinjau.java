package id.papicode.gudang.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.print.*;

/** Pratinjau cetak A4 untuk Printable apa pun, lalu kirim ke printer. */
public final class Pratinjau {
    private Pratinjau() { }

    public static PageFormat a4(boolean mendatar) {
        PageFormat pf = new PageFormat();
        Paper p = new Paper();
        double w = 210 / 25.4 * 72, h = 297 / 25.4 * 72;
        p.setSize(w, h);
        p.setImageableArea(28, 28, w - 56, h - 56);
        pf.setPaper(p);
        pf.setOrientation(mendatar ? PageFormat.LANDSCAPE : PageFormat.PORTRAIT);
        return pf;
    }

    public static void tampil(Component induk, LembarCetak lembar, String judul, boolean mendatar) {
        tampil(induk, judul, lembar, lembar.jumlahHalaman(a4(mendatar)), mendatar);
    }

    public static void tampil(Component induk, String judul, Printable isi, int halaman, boolean mendatar) {
        PageFormat pf = a4(mendatar);
        Window w = SwingUtilities.getWindowAncestor(induk);
        JDialog d = new JDialog(w, judul, java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        JPanel root = new JPanel(new BorderLayout(0, 14));
        root.setBackground(Tema.KARTU);
        root.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(3, 0, 0, 0, Tema.UTAMA), new EmptyBorder(18, 20, 16, 20)));
        JPanel atas = Ui.transparan(new BorderLayout());
        atas.add(Ui.judul(judul, 18f), BorderLayout.WEST);
        atas.add(Ui.label(halaman + " halaman A4" + (mendatar ? " mendatar" : "")), BorderLayout.EAST);
        root.add(atas, BorderLayout.NORTH);

        double skala = mendatar ? 1.0 : 0.85;
        JPanel lembar = Ui.transparan(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.insets = new Insets(10, 10, 4, 10);
        for (int i = 0; i < halaman; i++) {
            int hal = i;
            JComponent kertas = new JComponent() {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(Color.WHITE);
                    g2.fillRect(0, 0, getWidth(), getHeight());
                    g2.scale(skala, skala);
                    try { isi.print(g2, pf, hal); } catch (PrinterException ignored) { }
                    g2.dispose();
                }
            };
            Dimension ukuran = new Dimension((int) (pf.getWidth() * skala), (int) (pf.getHeight() * skala));
            kertas.setPreferredSize(ukuran);
            kertas.setMinimumSize(ukuran);
            gc.gridy = i;
            lembar.add(kertas, gc);
        }
        JScrollPane sp = new JScrollPane(lembar);
        sp.setBorder(BorderFactory.createLineBorder(Tema.GARIS));
        sp.getViewport().setBackground(Tema.LATAR);
        sp.getVerticalScrollBar().setUnitIncrement(24);
        sp.setPreferredSize(new Dimension((int) (pf.getWidth() * skala) + 40, Math.min(620, (int) (pf.getHeight() * skala) + 20)));
        root.add(sp, BorderLayout.CENTER);

        Ui.Tombol cetak = Ui.tombol("Cetak", "printer", Ui.Jenis.UTAMA);
        Ui.Tombol tutup = Ui.tombol("Tutup", null, Ui.Jenis.GARIS);
        cetak.addActionListener(e -> {
            PrinterJob job = PrinterJob.getPrinterJob();
            job.setJobName(judul);
            Book buku = new Book();
            buku.append(isi, pf, halaman);
            job.setPageable(buku);
            if (job.printDialog()) {
                try { job.print(); d.dispose(); }
                catch (PrinterException ex) { Kotak.pesan(d, "Gagal mencetak", ex.getMessage(), true); }
            }
        });
        tutup.addActionListener(e -> d.dispose());
        JPanel bawah = Ui.transparan(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        bawah.add(tutup);
        bawah.add(cetak);
        root.add(bawah, BorderLayout.SOUTH);
        d.getRootPane().registerKeyboardAction(e -> d.dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        d.setContentPane(root);
        d.pack();
        d.setLocationRelativeTo(w);
        d.setVisible(true);
    }
}
