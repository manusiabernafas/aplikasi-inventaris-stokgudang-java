package id.papicode.gudang.util;

public final class Rupiah {
    private Rupiah() { }

    /** 12500 -> "Rp12.500" (pemisah ribuan titik, tanpa desimal). */
    public static String format(long n) {
        return (n < 0 ? "-Rp" : "Rp") + ribuan(Math.abs(n));
    }

    public static String ribuan(long n) {
        String s = Long.toString(Math.abs(n));
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            if (i > 0 && (s.length() - i) % 3 == 0) b.append('.');
            b.append(s.charAt(i));
        }
        return (n < 0 ? "-" : "") + b;
    }

    /** "Rp 12.500", "12500", "12,500" -> 12500. Teks kosong -> 0. Huruf lain ditolak. */
    public static long parse(String s) {
        if (s == null) return 0;
        String t = s.trim().replaceFirst("(?i)^rp", "").replace(".", "").replace(",", "").replace(" ", "");
        if (t.isEmpty()) return 0;
        if (!t.matches("\\d{1,13}")) throw new IllegalArgumentException("Nominal tidak valid: " + s);
        return Long.parseLong(t);
    }

    /** Ringkas untuk sumbu grafik: 1.250.000 -> "1,3jt". */
    public static String ringkas(long n) {
        if (n >= 1_000_000_000) return String.format("%.1fM", n / 1e9).replace('.', ',');
        if (n >= 1_000_000) return String.format("%.1fjt", n / 1e6).replace('.', ',');
        if (n >= 1_000) return (n / 1000) + "rb";
        return Long.toString(n);
    }
}
