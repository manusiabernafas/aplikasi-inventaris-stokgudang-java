package id.papicode.gudang.ui;

import javax.swing.*;
import java.awt.*;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

/** Pilihan periode: preset (hari ini, 7 hari, bulan ini, ...) + tanggal bebas dari-sampai. */
public class PilihPeriode extends JPanel {

    private static final String[] PRESET = {"Hari ini", "Kemarin", "7 hari terakhir", "30 hari terakhir", "Bulan ini", "Bulan lalu", "Pilih tanggal"};

    private final JComboBox<String> preset = new JComboBox<>(PRESET);
    private final JSpinner dari = tanggal();
    private final JSpinner sampai = tanggal();
    private final Clock jam;
    private Runnable saatBerubah = () -> { };
    private boolean diam;

    public PilihPeriode(Clock jam, int awal) {
        super(new FlowLayout(FlowLayout.LEFT, 8, 0));
        this.jam = jam;
        setOpaque(false);
        preset.setPreferredSize(new Dimension(170, 40));
        add(preset);
        add(dari);
        JLabel s = Ui.teks("s/d", 13f, Tema.TEKS_2);
        add(s);
        add(sampai);
        preset.setSelectedIndex(awal);
        terapkanPreset();
        preset.addActionListener(e -> { terapkanPreset(); saatBerubah.run(); });
        dari.addChangeListener(e -> manual());
        sampai.addChangeListener(e -> manual());
    }

    public void onChange(Runnable r) { saatBerubah = r; }

    public LocalDate dari() { return ke(dari); }
    public LocalDate sampai() { return ke(sampai); }

    private void manual() {
        if (diam) return;
        diam = true;
        preset.setSelectedIndex(PRESET.length - 1);
        if (dari().isAfter(sampai())) sampai.setValue(dari.getValue());
        diam = false;
        saatBerubah.run();
    }

    private void terapkanPreset() {
        LocalDate h = LocalDate.now(jam);
        LocalDate a, b;
        switch (preset.getSelectedIndex()) {
            case 0 -> { a = h; b = h; }
            case 1 -> { a = h.minusDays(1); b = a; }
            case 2 -> { a = h.minusDays(6); b = h; }
            case 3 -> { a = h.minusDays(29); b = h; }
            case 4 -> { a = h.withDayOfMonth(1); b = h; }
            case 5 -> { a = h.minusMonths(1).withDayOfMonth(1); b = h.withDayOfMonth(1).minusDays(1); }
            default -> { return; }
        }
        diam = true;
        dari.setValue(Date.from(a.atStartOfDay(ZoneId.systemDefault()).toInstant()));
        sampai.setValue(Date.from(b.atStartOfDay(ZoneId.systemDefault()).toInstant()));
        diam = false;
    }

    private static JSpinner tanggal() {
        JSpinner s = new JSpinner(new SpinnerDateModel());
        s.setEditor(new JSpinner.DateEditor(s, "dd/MM/yyyy"));
        s.setPreferredSize(new Dimension(130, 40));
        s.setFont(Tema.mono(13.5f));
        return s;
    }

    private static LocalDate ke(JSpinner s) {
        return ((Date) s.getValue()).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }
}
