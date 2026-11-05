package id.papicode.gudang.db;

/** Skema tabel. Dijalankan setiap aplikasi start (CREATE IF NOT EXISTS, aman diulang). */
final class Skema {
    private Skema() { }

    static final String[] SQL = {
        "CREATE TABLE IF NOT EXISTS pengaturan (kunci TEXT PRIMARY KEY, nilai TEXT NOT NULL)",
        """
        CREATE TABLE IF NOT EXISTS pengguna (
          id INTEGER PRIMARY KEY AUTOINCREMENT,
          username TEXT NOT NULL UNIQUE COLLATE NOCASE,
          nama TEXT NOT NULL,
          peran TEXT NOT NULL CHECK (peran IN ('ADMIN','STAF')),
          hash TEXT NOT NULL,
          aktif INTEGER NOT NULL DEFAULT 1,
          gagal INTEGER NOT NULL DEFAULT 0,
          kunci_sampai INTEGER NOT NULL DEFAULT 0,
          dibuat TEXT NOT NULL
        )""",
        "CREATE TABLE IF NOT EXISTS kategori (id INTEGER PRIMARY KEY AUTOINCREMENT, nama TEXT NOT NULL UNIQUE COLLATE NOCASE)",
        """
        CREATE TABLE IF NOT EXISTS barang (
          id INTEGER PRIMARY KEY AUTOINCREMENT,
          kode TEXT NOT NULL UNIQUE COLLATE NOCASE,
          nama TEXT NOT NULL,
          kategori_id INTEGER REFERENCES kategori(id),
          satuan TEXT NOT NULL DEFAULT 'pcs',
          lokasi TEXT,
          harga INTEGER NOT NULL DEFAULT 0 CHECK (harga >= 0),
          stok INTEGER NOT NULL DEFAULT 0 CHECK (stok >= 0),
          stok_min INTEGER NOT NULL DEFAULT 0 CHECK (stok_min >= 0),
          aktif INTEGER NOT NULL DEFAULT 1
        )""",
        """
        CREATE TABLE IF NOT EXISTS pihak (
          id INTEGER PRIMARY KEY AUTOINCREMENT,
          jenis TEXT NOT NULL CHECK (jenis IN ('SUPPLIER','TUJUAN')),
          nama TEXT NOT NULL,
          telepon TEXT,
          alamat TEXT,
          aktif INTEGER NOT NULL DEFAULT 1,
          UNIQUE (jenis, nama)
        )""",
        """
        CREATE TABLE IF NOT EXISTS dokumen (
          id INTEGER PRIMARY KEY AUTOINCREMENT,
          nomor TEXT NOT NULL UNIQUE,
          jenis TEXT NOT NULL CHECK (jenis IN ('MASUK','KELUAR','OPNAME')),
          tanggal TEXT NOT NULL,
          pihak_id INTEGER REFERENCES pihak(id),
          referensi TEXT,
          catatan TEXT,
          dibuat_oleh INTEGER NOT NULL REFERENCES pengguna(id),
          dibuat_pada TEXT NOT NULL,
          status TEXT NOT NULL DEFAULT 'AKTIF' CHECK (status IN ('AKTIF','BATAL')),
          alasan_batal TEXT
        )""",
        "CREATE INDEX IF NOT EXISTS ix_dokumen_tgl ON dokumen(jenis, tanggal)",
        """
        CREATE TABLE IF NOT EXISTS dokumen_item (
          id INTEGER PRIMARY KEY AUTOINCREMENT,
          dokumen_id INTEGER NOT NULL REFERENCES dokumen(id) ON DELETE CASCADE,
          barang_id INTEGER NOT NULL REFERENCES barang(id),
          qty INTEGER NOT NULL,
          stok_sistem INTEGER,
          harga INTEGER NOT NULL DEFAULT 0
        )""",
        "CREATE INDEX IF NOT EXISTS ix_item_dok ON dokumen_item(dokumen_id)",
        """
        CREATE TABLE IF NOT EXISTS mutasi (
          id INTEGER PRIMARY KEY AUTOINCREMENT,
          barang_id INTEGER NOT NULL REFERENCES barang(id),
          dokumen_id INTEGER REFERENCES dokumen(id),
          tanggal TEXT NOT NULL,
          jenis TEXT NOT NULL CHECK (jenis IN ('AWAL','MASUK','KELUAR','KOREKSI','BATAL')),
          qty INTEGER NOT NULL,
          keterangan TEXT
        )""",
        "CREATE INDEX IF NOT EXISTS ix_mutasi_barang ON mutasi(barang_id, tanggal)",
        "CREATE INDEX IF NOT EXISTS ix_mutasi_tgl ON mutasi(tanggal)"
    };
}
