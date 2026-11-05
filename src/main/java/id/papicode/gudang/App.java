package id.papicode.gudang;

import id.papicode.gudang.ui.LoginFrame;
import id.papicode.gudang.ui.Tema;

import javax.swing.*;
import java.io.File;
import java.time.Clock;

/**
 * Titik masuk aplikasi Stok Gudang.
 * Lokasi database bisa diganti: java -Dgudang.db=D:/data/gudang.db -jar stok-gudang.jar
 */
public class App {
    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        File fileDb = new File(System.getProperty("gudang.db", "data/stok-gudang.db"));
        SwingUtilities.invokeLater(() -> {
            Tema.pasang();
            Konteks k;
            try {
                k = new Konteks(fileDb, Clock.systemDefaultZone());
                k.auth.pastikanAdminAwal();
            } catch (RuntimeException e) {
                JOptionPane.showMessageDialog(null, "Database tidak bisa dibuka:\n" + e.getMessage(), "Stok Gudang", JOptionPane.ERROR_MESSAGE);
                return;
            }
            new LoginFrame(k).setVisible(true);
        });
    }
}
