package id.papicode.gudang.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class Waktu {
    private Waktu() { }

    public static final DateTimeFormatter DB = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public static final DateTimeFormatter TGL = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final Locale ID = Locale.forLanguageTag("id-ID");
    public static final DateTimeFormatter TAMPIL = DateTimeFormatter.ofPattern("d MMM yyyy HH:mm", ID);
    public static final DateTimeFormatter TAMPIL_TGL = DateTimeFormatter.ofPattern("d MMM yyyy", ID);
    public static final DateTimeFormatter HARI = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", ID);

    public static String db(LocalDateTime t) { return t.format(DB); }

    public static String tampil(String db) {
        return db == null ? "" : LocalDateTime.parse(db, DB).format(TAMPIL);
    }

    public static String tampilTgl(LocalDate d) { return d.format(TAMPIL_TGL); }

    /** Batas bawah inklusif dan atas eksklusif untuk filter WHERE waktu >= ? AND waktu < ?. */
    public static String awal(LocalDate d) { return d.atStartOfDay().format(DB); }
    public static String akhir(LocalDate d) { return d.plusDays(1).atStartOfDay().format(DB); }
}
