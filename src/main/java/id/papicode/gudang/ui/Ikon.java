package id.papicode.gudang.ui;

import javax.swing.Icon;
import java.awt.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Ikon garis gaya Lucide (ISC License, https://lucide.dev) digambar dengan Java2D.
 * Data ikon ditulis sebagai potongan SVG di kotak 24x24, lalu diurai sekali dan di-cache.
 * Pemakaian: Ikon.of("search", 18, Tema.TEKS)
 */
public final class Ikon implements Icon {

    private static final Map<String, String> SVG = new HashMap<>();
    private static final Map<String, Shape> CACHE = new HashMap<>();

    static {
        SVG.put("cart", "<circle cx='8' cy='21' r='1'/><circle cx='19' cy='21' r='1'/><path d='M2.05 2.05h2l2.66 12.42a2 2 0 0 0 2 1.58h9.78a2 2 0 0 0 1.95-1.57l1.65-7.43H5.12'/>");
        SVG.put("package", "<path d='m7.5 4.27 9 5.15'/><path d='M21 8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16Z'/><path d='m3.3 7 8.7 5 8.7-5'/><path d='M12 22V12'/>");
        SVG.put("receipt", "<path d='M4 2v20l2-1 2 1 2-1 2 1 2-1 2 1 2-1 2 1V2l-2 1-2-1-2 1-2-1-2 1-2-1-2 1Z'/><path d='M16 8h-6a2 2 0 1 0 0 4h4a2 2 0 1 1 0 4H8'/><path d='M12 17.5v-11'/>");
        SVG.put("chart", "<path d='M3 3v18h18'/><path d='M18 17V9'/><path d='M13 17V5'/><path d='M8 17v-3'/>");
        SVG.put("users", "<path d='M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2'/><circle cx='9' cy='7' r='4'/><path d='M22 21v-2a4 4 0 0 0-3-3.87'/><path d='M16 3.13a4 4 0 0 1 0 7.75'/>");
        SVG.put("user", "<path d='M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2'/><circle cx='12' cy='7' r='4'/>");
        SVG.put("sliders", "<line x1='21' x2='14' y1='4' y2='4'/><line x1='10' x2='3' y1='4' y2='4'/><line x1='21' x2='12' y1='12' y2='12'/><line x1='8' x2='3' y1='12' y2='12'/><line x1='21' x2='16' y1='20' y2='20'/><line x1='12' x2='3' y1='20' y2='20'/><line x1='14' x2='14' y1='2' y2='6'/><line x1='8' x2='8' y1='10' y2='14'/><line x1='16' x2='16' y1='18' y2='22'/>");
        SVG.put("logout", "<path d='M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4'/><polyline points='16 17 21 12 16 7'/><line x1='21' x2='9' y1='12' y2='12'/>");
        SVG.put("search", "<circle cx='11' cy='11' r='8'/><path d='m21 21-4.3-4.3'/>");
        SVG.put("plus", "<path d='M5 12h14'/><path d='M12 5v14'/>");
        SVG.put("minus", "<path d='M5 12h14'/>");
        SVG.put("trash", "<path d='M3 6h18'/><path d='M19 6v14c0 1-1 2-2 2H7c-1 0-2-1-2-2V6'/><path d='M8 6V4c0-1 1-2 2-2h4c1 0 2 1 2 2v2'/><line x1='10' x2='10' y1='11' y2='17'/><line x1='14' x2='14' y1='11' y2='17'/>");
        SVG.put("printer", "<polyline points='6 9 6 2 18 2 18 9'/><path d='M6 18H4a2 2 0 0 1-2-2v-5a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v5a2 2 0 0 1-2 2h-2'/><rect width='12' height='8' x='6' y='14'/>");
        SVG.put("barcode", "<path d='M3 7V5a2 2 0 0 1 2-2h2'/><path d='M17 3h2a2 2 0 0 1 2 2v2'/><path d='M21 17v2a2 2 0 0 1-2 2h-2'/><path d='M7 21H5a2 2 0 0 1-2-2v-2'/><path d='M8 7v10'/><path d='M12 7v10'/><path d='M17 7v10'/>");
        SVG.put("pencil", "<path d='M17 3a2.85 2.83 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5Z'/><path d='m15 5 4 4'/>");
        SVG.put("x", "<path d='M18 6 6 18'/><path d='m6 6 12 12'/>");
        SVG.put("check", "<path d='M20 6 9 17l-5-5'/>");
        SVG.put("alert", "<path d='m21.73 18-8-14a2 2 0 0 0-3.48 0l-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.73-3Z'/><path d='M12 9v4'/><path d='M12 17h.01'/>");
        SVG.put("wallet", "<path d='M21 12V7H5a2 2 0 0 1 0-4h14v4'/><path d='M3 5v14a2 2 0 0 0 2 2h16v-5'/><path d='M18 12a2 2 0 0 0 0 4h4v-4Z'/>");
        SVG.put("calendar", "<rect width='18' height='18' x='3' y='4' rx='2' ry='2'/><line x1='16' x2='16' y1='2' y2='6'/><line x1='8' x2='8' y1='2' y2='6'/><line x1='3' x2='21' y1='10' y2='10'/>");
        SVG.put("download", "<path d='M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4'/><polyline points='7 10 12 15 17 10'/><line x1='12' x2='12' y1='15' y2='3'/>");
        SVG.put("lock", "<rect width='18' height='11' x='3' y='11' rx='2' ry='2'/><path d='M7 11V7a5 5 0 0 1 10 0v4'/>");
        SVG.put("store", "<path d='M3 9h18l-2-5H5Z'/><path d='M3 9a3 3 0 0 0 6 0a3 3 0 0 0 6 0a3 3 0 0 0 6 0'/><path d='M5 11.5V20h14v-8.5'/><path d='M10 20v-5h4v5'/>");
        SVG.put("dashboard", "<rect width='7' height='9' x='3' y='3' rx='1'/><rect width='7' height='5' x='14' y='3' rx='1'/><rect width='7' height='9' x='14' y='12' rx='1'/><rect width='7' height='5' x='3' y='16' rx='1'/>");
        SVG.put("clock", "<circle cx='12' cy='12' r='10'/><polyline points='12 6 12 12 16 14'/>");
        SVG.put("tag", "<path d='M12 2H2v10l9.29 9.29c.94.94 2.48.94 3.42 0l6.58-6.58c.94-.94.94-2.48 0-3.42L12 2Z'/><path d='M7 7h.01'/>");
        SVG.put("trending", "<polyline points='22 7 13.5 15.5 8.5 10.5 2 17'/><polyline points='16 7 22 7 22 13'/>");
        SVG.put("coins", "<circle cx='8' cy='8' r='6'/><path d='M18.09 10.37A6 6 0 1 1 10.34 18'/><path d='M7 6h1v4'/><path d='m16.71 13.88.7.71-2.82 2.82'/>");
        SVG.put("undo", "<path d='M3 7v6h6'/><path d='M21 17a9 9 0 0 0-9-9 9 9 0 0 0-6 2.3L3 13'/>");
        SVG.put("eye", "<path d='M2 12s3-7 10-7 10 7 10 7-3 7-10 7-10-7-10-7Z'/><circle cx='12' cy='12' r='3'/>");
        SVG.put("boxes", "<path d='M3 3h7v7H3z'/><path d='M14 3h7v7h-7z'/><path d='M14 14h7v7h-7z'/><path d='M3 14h7v7H3z'/>");
        SVG.put("refresh", "<path d='M3 12a9 9 0 0 1 9-9 9.75 9.75 0 0 1 6.74 2.74L21 8'/><path d='M21 3v5h-5'/><path d='M21 12a9 9 0 0 1-9 9 9.75 9.75 0 0 1-6.74-2.74L3 16'/><path d='M8 16H3v5'/>");
        SVG.put("database", "<ellipse cx='12' cy='5' rx='9' ry='3'/><path d='M3 5V19A9 3 0 0 0 21 19V5'/><path d='M3 12A9 3 0 0 0 21 12'/>");
        SVG.put("keyboard", "<rect width='20' height='16' x='2' y='4' rx='2'/><path d='M6 8h.01'/><path d='M10 8h.01'/><path d='M14 8h.01'/><path d='M18 8h.01'/><path d='M8 12h.01'/><path d='M12 12h.01'/><path d='M16 12h.01'/><path d='M7 16h10'/>");
        SVG.put("qr", "<rect width='5' height='5' x='3' y='3' rx='1'/><rect width='5' height='5' x='16' y='3' rx='1'/><rect width='5' height='5' x='3' y='16' rx='1'/><path d='M21 16h-3a2 2 0 0 0-2 2v3'/><path d='M21 21v.01'/><path d='M12 7v3a2 2 0 0 1-2 2H7'/><path d='M3 12h.01'/><path d='M12 3h.01'/><path d='M12 16v.01'/><path d='M16 12h1'/><path d='M21 12v.01'/><path d='M12 21v-1'/>");
        SVG.put("card", "<rect width='20' height='14' x='2' y='5' rx='2'/><line x1='2' x2='22' y1='10' y2='10'/>");
        SVG.put("warehouse", "<path d='M22 8.35V20a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V8.35A2 2 0 0 1 3.26 6.5l8-3.2a2 2 0 0 1 1.48 0l8 3.2A2 2 0 0 1 22 8.35Z'/><path d='M6 18h12'/><path d='M6 14h12'/><rect width='12' height='12' x='6' y='10'/>");
        SVG.put("masuk", "<path d='M12 17V3'/><path d='m6 11 6 6 6-6'/><path d='M19 21H5'/>");
        SVG.put("keluar", "<path d='m18 9-6-6-6 6'/><path d='M12 3v14'/><path d='M5 21h14'/>");
        SVG.put("clipboard", "<rect width='8' height='4' x='8' y='2' rx='1' ry='1'/><path d='M16 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2'/><path d='m9 14 2 2 4-4'/>");
        SVG.put("truck", "<path d='M14 18V6a2 2 0 0 0-2-2H4a2 2 0 0 0-2 2v11a1 1 0 0 0 1 1h2'/><path d='M15 18H9'/><path d='M19 18h2a1 1 0 0 0 1-1v-3.65a1 1 0 0 0-.22-.624l-3.48-4.35A1 1 0 0 0 17.52 8H14'/><circle cx='17' cy='18' r='2'/><circle cx='7' cy='18' r='2'/>");
        SVG.put("file", "<path d='M15 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7Z'/><path d='M14 2v4a2 2 0 0 0 2 2h4'/><path d='M10 9H8'/><path d='M16 13H8'/><path d='M16 17H8'/>");
        SVG.put("layers", "<path d='M12.83 2.18a2 2 0 0 0-1.66 0L2.6 6.08a1 1 0 0 0 0 1.83l8.58 3.91a2 2 0 0 0 1.66 0l8.58-3.9a1 1 0 0 0 0-1.83Z'/><path d='m22 17.65-9.17 4.16a2 2 0 0 1-1.66 0L2 17.65'/><path d='m22 12.65-9.17 4.16a2 2 0 0 1-1.66 0L2 12.65'/>");
        SVG.put("pin", "<path d='M20 10c0 6-8 12-8 12s-8-6-8-12a8 8 0 0 1 16 0Z'/><circle cx='12' cy='10' r='3'/>");
        SVG.put("xcircle", "<circle cx='12' cy='12' r='10'/><path d='m15 9-6 6'/><path d='m9 9 6 6'/>");
        SVG.put("arrow", "<path d='M5 12h14'/><path d='m12 5 7 7-7 7'/>");
    }

    private final Shape shape;
    private final int size;
    private final Color color;
    private final float stroke;

    private Ikon(Shape shape, int size, Color color, float stroke) {
        this.shape = shape;
        this.size = size;
        this.color = color;
        this.stroke = stroke;
    }

    public static Ikon of(String name, int size, Color color) {
        return of(name, size, color, 2f);
    }

    public static Ikon of(String name, int size, Color color, float stroke) {
        Shape s;
        synchronized (CACHE) {
            s = CACHE.computeIfAbsent(name, n -> {
                String svg = SVG.get(n);
                if (svg == null) throw new IllegalArgumentException("Ikon tidak ada: " + n);
                return parse(svg);
            });
        }
        return new Ikon(s, size, color, stroke);
    }

    public Ikon warna(Color c) {
        return new Ikon(shape, size, c, stroke);
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2.translate(x, y);
        double k = size / 24.0;
        g2.scale(k, k);
        g2.setColor(color);
        g2.setStroke(new BasicStroke(stroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(shape);
        g2.dispose();
    }

    @Override public int getIconWidth() { return size; }
    @Override public int getIconHeight() { return size; }

    // ---------- pengurai SVG mini: path, circle, ellipse, rect, line, polyline, polygon ----------

    private static final Pattern ELEM = Pattern.compile("<(\\w+)([^>]*)/>");
    private static final Pattern ATTR = Pattern.compile("([\\w-]+)='([^']*)'");

    static Shape parse(String svg) {
        Path2D.Double all = new Path2D.Double();
        Matcher m = ELEM.matcher(svg);
        while (m.find()) {
            Map<String, String> a = new HashMap<>();
            Matcher am = ATTR.matcher(m.group(2));
            while (am.find()) a.put(am.group(1), am.group(2));
            switch (m.group(1)) {
                case "path" -> all.append(path(a.get("d")), false);
                case "circle" -> {
                    double r = d(a, "r");
                    all.append(new Ellipse2D.Double(d(a, "cx") - r, d(a, "cy") - r, r * 2, r * 2), false);
                }
                case "ellipse" -> {
                    double rx = d(a, "rx"), ry = d(a, "ry");
                    all.append(new Ellipse2D.Double(d(a, "cx") - rx, d(a, "cy") - ry, rx * 2, ry * 2), false);
                }
                case "rect" -> {
                    double rx = d(a, "rx");
                    all.append(new RoundRectangle2D.Double(d(a, "x"), d(a, "y"), d(a, "width"), d(a, "height"), rx * 2, rx * 2), false);
                }
                case "line" -> all.append(new Line2D.Double(d(a, "x1"), d(a, "y1"), d(a, "x2"), d(a, "y2")), false);
                case "polyline", "polygon" -> {
                    String[] p = a.get("points").trim().split("[\\s,]+");
                    Path2D.Double pl = new Path2D.Double();
                    for (int i = 0; i + 1 < p.length; i += 2) {
                        double px = Double.parseDouble(p[i]), py = Double.parseDouble(p[i + 1]);
                        if (i == 0) pl.moveTo(px, py); else pl.lineTo(px, py);
                    }
                    if (m.group(1).equals("polygon")) pl.closePath();
                    all.append(pl, false);
                }
                default -> { }
            }
        }
        return all;
    }

    private static double d(Map<String, String> a, String k) {
        String v = a.get(k);
        return v == null ? 0 : Double.parseDouble(v);
    }

    private static final Pattern TOKEN = Pattern.compile("[MmLlHhVvCcSsQqTtAaZz]|-?(?:\\d+\\.?\\d*|\\.\\d+)(?:e-?\\d+)?");

    static Path2D.Double path(String d) {
        List<String> t = new ArrayList<>();
        Matcher m = TOKEN.matcher(d);
        while (m.find()) t.add(m.group());
        Path2D.Double p = new Path2D.Double();
        double x = 0, y = 0, sx = 0, sy = 0, cx2 = 0, cy2 = 0;
        char cmd = 'M', prev = ' ';
        int i = 0;
        while (i < t.size()) {
            String tok = t.get(i);
            if (Character.isLetter(tok.charAt(0)) && tok.length() == 1) { cmd = tok.charAt(0); i++; }
            boolean rel = Character.isLowerCase(cmd);
            double ox = rel ? x : 0, oy = rel ? y : 0;
            switch (Character.toUpperCase(cmd)) {
                case 'M' -> {
                    x = ox + n(t, i++); y = oy + n(t, i++);
                    p.moveTo(x, y); sx = x; sy = y;
                    cmd = rel ? 'l' : 'L';
                }
                case 'L' -> { x = ox + n(t, i++); y = oy + n(t, i++); p.lineTo(x, y); }
                case 'H' -> { x = ox + n(t, i++); p.lineTo(x, y); }
                case 'V' -> { y = oy + n(t, i++); p.lineTo(x, y); }
                case 'C' -> {
                    double x1 = ox + n(t, i++), y1 = oy + n(t, i++);
                    cx2 = ox + n(t, i++); cy2 = oy + n(t, i++);
                    x = ox + n(t, i++); y = oy + n(t, i++);
                    p.curveTo(x1, y1, cx2, cy2, x, y);
                }
                case 'S' -> {
                    boolean adaKontrol = "CcSs".indexOf(prev) >= 0;
                    double x1 = adaKontrol ? 2 * x - cx2 : x, y1 = adaKontrol ? 2 * y - cy2 : y;
                    cx2 = ox + n(t, i++); cy2 = oy + n(t, i++);
                    x = ox + n(t, i++); y = oy + n(t, i++);
                    p.curveTo(x1, y1, cx2, cy2, x, y);
                }
                case 'Q' -> {
                    double x1 = ox + n(t, i++), y1 = oy + n(t, i++);
                    x = ox + n(t, i++); y = oy + n(t, i++);
                    p.quadTo(x1, y1, x, y);
                }
                case 'A' -> {
                    double rx = n(t, i++), ry = n(t, i++), rot = n(t, i++);
                    boolean large = n(t, i++) != 0, sweep = n(t, i++) != 0;
                    double nx = ox + n(t, i++), ny = oy + n(t, i++);
                    arc(p, x, y, rx, ry, rot, large, sweep, nx, ny);
                    x = nx; y = ny;
                }
                case 'Z' -> { p.closePath(); x = sx; y = sy; }
                default -> i++;
            }
            prev = cmd;
        }
        return p;
    }

    private static double n(List<String> t, int i) {
        // flag busur boleh menempel tanpa spasi, misal "0 1 1" atau "011"; Lucide selalu memberi spasi
        return Double.parseDouble(t.get(i));
    }

    /** Konversi busur SVG (endpoint) ke Arc2D (center parameterization). */
    private static void arc(Path2D.Double p, double x0, double y0, double rx, double ry, double angle,
                            boolean large, boolean sweep, double x, double y) {
        if (rx == 0 || ry == 0) { p.lineTo(x, y); return; }
        double dx2 = (x0 - x) / 2.0, dy2 = (y0 - y) / 2.0;
        double a = Math.toRadians(angle % 360.0);
        double cos = Math.cos(a), sin = Math.sin(a);
        double x1 = cos * dx2 + sin * dy2, y1 = -sin * dx2 + cos * dy2;
        rx = Math.abs(rx); ry = Math.abs(ry);
        double lam = (x1 * x1) / (rx * rx) + (y1 * y1) / (ry * ry);
        if (lam > 1) { rx *= Math.sqrt(lam); ry *= Math.sqrt(lam); }
        double sign = (large == sweep) ? -1 : 1;
        double num = rx * rx * ry * ry - rx * rx * y1 * y1 - ry * ry * x1 * x1;
        double den = rx * rx * y1 * y1 + ry * ry * x1 * x1;
        double coef = sign * Math.sqrt(Math.max(0, num / den));
        double cx1 = coef * ((rx * y1) / ry), cy1 = coef * -((ry * x1) / rx);
        double cx = (x0 + x) / 2.0 + (cos * cx1 - sin * cy1);
        double cy = (y0 + y) / 2.0 + (sin * cx1 + cos * cy1);
        double ux = (x1 - cx1) / rx, uy = (y1 - cy1) / ry, vx = (-x1 - cx1) / rx, vy = (-y1 - cy1) / ry;
        double start = Math.toDegrees(sudut(1, 0, ux, uy));
        double ext = Math.toDegrees(sudut(ux, uy, vx, vy)) % 360;
        if (!sweep && ext > 0) ext -= 360; else if (sweep && ext < 0) ext += 360;
        Arc2D.Double arc = new Arc2D.Double(cx - rx, cy - ry, rx * 2, ry * 2, -start, -ext, Arc2D.OPEN);
        Shape s = AffineTransform.getRotateInstance(a, cx, cy).createTransformedShape(arc);
        p.append(s, true);
    }

    private static double sudut(double ux, double uy, double vx, double vy) {
        double dot = ux * vx + uy * vy, len = Math.hypot(ux, uy) * Math.hypot(vx, vy);
        double ang = Math.acos(Math.max(-1, Math.min(1, dot / len)));
        return (ux * vy - uy * vx < 0) ? -ang : ang;
    }
}
