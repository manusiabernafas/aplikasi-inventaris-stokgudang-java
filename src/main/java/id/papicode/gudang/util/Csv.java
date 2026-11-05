package id.papicode.gudang.util;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Ekspor CSV untuk Excel Indonesia: pemisah titik koma, UTF-8 BOM, aman dari formula injection. */
public final class Csv {
    private Csv() { }

    public static void tulis(Path file, List<String> header, List<List<Object>> baris) throws IOException {
        try (OutputStream os = Files.newOutputStream(file);
             Writer w = new OutputStreamWriter(os, StandardCharsets.UTF_8)) {
            w.write('﻿');
            w.write(barisCsv(header.stream().map(h -> (Object) h).toList()));
            for (List<Object> b : baris) w.write(barisCsv(b));
        }
    }

    static String barisCsv(List<Object> sel) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < sel.size(); i++) {
            if (i > 0) sb.append(';');
            sb.append(sel(sel.get(i)));
        }
        return sb.append("\r\n").toString();
    }

    /** Teks diawali = + - @ (atau tab/CR) diberi petik satu agar Excel tidak menjalankannya sebagai rumus. Angka asli dibiarkan. */
    public static String sel(Object v) {
        if (v == null) return "";
        if (v instanceof Number) return v.toString();
        String s = v.toString();
        if (!s.isEmpty() && "=+-@\t\r".indexOf(s.charAt(0)) >= 0) s = "'" + s;
        if (s.contains(";") || s.contains("\"") || s.contains("\n") || s.contains("\r")) {
            s = "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}
