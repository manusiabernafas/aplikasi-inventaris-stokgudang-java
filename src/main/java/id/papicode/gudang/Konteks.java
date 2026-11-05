package id.papicode.gudang;

import id.papicode.gudang.db.Database;
import id.papicode.gudang.service.*;

import java.io.File;
import java.time.Clock;

/** Wadah semua service supaya mudah dioper ke layar dan ke tes. */
public class Konteks {
    public final Database db;
    public final Clock jam;
    public final AuthService auth;
    public final BarangService barang;
    public final DokumenService dokumen;
    public final LaporanService laporan;
    public final PengaturanService pengaturan;

    public Konteks(File fileDb, Clock jam) {
        this.db = new Database(fileDb);
        this.jam = jam;
        this.auth = new AuthService(db, jam);
        this.barang = new BarangService(db, jam);
        this.dokumen = new DokumenService(db, jam);
        this.laporan = new LaporanService(db);
        this.pengaturan = new PengaturanService(db);
    }
}
