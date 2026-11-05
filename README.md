# Stok Gudang — Aplikasi Inventaris Gudang Desktop (Java Swing + SQLite)

Aplikasi inventaris gudang desktop untuk distributor, toko bangunan, gudang proyek, gudang cabang, atau UMKM yang menyimpan banyak barang. Mencatat barang masuk dari supplier, barang keluar ke proyek/cabang/pelanggan, stok opname, kartu stok, dan laporan mutasi persediaan. Jalan offline di Windows, Linux, dan macOS; semua data di satu file SQLite.

## Menu
| Kelompok | Menu | Isi |
|---|---|---|
| Ringkasan | Dasbor | Jenis barang, nilai persediaan, dokumen hari ini, grafik masuk vs keluar 14 hari, perlu restok, aktivitas terbaru, barang paling sering keluar |
| Transaksi gudang | Barang Masuk | Dokumen BM multi-baris dari supplier, scan kode, cetak bukti penerimaan, pembatalan |
| | Barang Keluar | Dokumen BK ke proyek/cabang/pelanggan, cek stok, cetak surat jalan, pembatalan |
| | Stok Opname | Cetak lembar hitung (tanpa angka sistem), input hasil hitung fisik, selisih & nilai otomatis, berita acara |
| Data master | Daftar Barang | Kode, kategori, satuan, lokasi rak, harga, stok minimum, cetak label barcode Code 128, ekspor CSV |
| | Supplier & Tujuan | Data pemasok dan penerima barang |
| Laporan | Kartu Stok | Saldo awal, setiap mutasi, saldo berjalan per barang; cetak & CSV |
| | Mutasi Persediaan | Awal, masuk, keluar, penyesuaian, akhir, nilai akhir semua barang per periode; cetak & CSV |
| | Analisis Barang | Fast moving, dead stock (modal tertahan), saran belanja restok dan perkiraan biaya |
| Sistem | Pengguna | Admin dan staf gudang, reset password, aktif/nonaktif |
| | Pengaturan | Kop dokumen (perusahaan, gudang, alamat, kota, penanda tangan), ganti password, cadangkan database |

## Daftar file
```
stok-gudang-java/
├── pom.xml                      konfigurasi Maven
├── jalankan.bat / jalankan.sh   build otomatis lalu jalankan
├── src/main/java/id/papicode/gudang/
│   ├── App.java, Konteks.java
│   ├── db/                      Database, Skema, DataContoh
│   ├── model/                   Barang, Dokumen, ItemDokumen, Pihak, Pengguna, ...
│   ├── service/                 AuthService, BarangService, DokumenService, LaporanService, PengaturanService
│   ├── util/                    Rupiah, Waktu, Csv, Password
│   └── ui/                      Tema blueprint, Ikon, komponen, LembarCetak (A4), LabelBarcode (Code 128), Pratinjau
│       └── halaman/             semua halaman & dialog
├── src/main/resources/fonts/    Chivo + Space Mono (SIL Open Font License)
└── src/test/java/               17 tes JUnit
```

## Kebutuhan & menjalankan
- JDK 17+ dan Apache Maven 3.8+ (atau NetBeans / IntelliJ IDEA / VS Code dengan Java).
- Windows: klik dua kali `jalankan.bat`. Linux/macOS: `./jalankan.sh`. Manual: `mvn package` lalu `java -jar target/stok-gudang.jar`.
- Login pertama **admin / admin123**. Saat database kosong, aplikasi menawarkan data contoh gudang bahan bangunan (45 barang, 7 supplier, 7 tujuan, transaksi 60 hari, satu opname).
- Isi **Pengaturan** (kop dokumen, kota, nama kepala gudang) dan **segera ganti password admin**.

Lokasi database default `data/stok-gudang.db`. Ganti dengan `java -Dgudang.db=D:/Data/gudang.db -jar target/stok-gudang.jar`. Untuk mulai bersih, tutup aplikasi lalu hapus/ganti nama file tersebut.

Installer .exe (opsional): `jpackage --input target --main-jar stok-gudang.jar --name "Stok Gudang" --type exe --win-shortcut --win-menu`.

## Alur kerja singkat
1. **Barang masuk**: pilih supplier, isi nomor surat jalan supplier, scan/ketik kode barang lalu Enter, isi jumlah, Simpan (Ctrl+S). Cetak bukti penerimaan untuk ditandatangani.
2. **Barang keluar**: pilih tujuan, isi nomor PO, masukkan barang. Kolom *Sisa stok* berubah merah bila jumlah melebihi stok. Cetak surat jalan rangkap untuk sopir dan penerima.
3. **Opname**: cetak lembar hitung per rak/kategori, petugas menulis hasil hitung, admin memasukkan angkanya (Enter pindah ke baris berikut, tombol *Isi = sistem* untuk mengisi baris yang sama). Stok disamakan dan berita acara bisa dicetak.
4. **Label barcode**: pilih barang di Daftar Barang (Ctrl/Shift untuk banyak), klik *Cetak label*, tentukan jumlah per barang. Kertas A4 3 x 8 label 70 x 37 mm, bisa dibaca scanner USB biasa.

## Hak akses
- **Admin**: semua menu, termasuk ubah master data, opname, pembatalan dokumen, pengguna, pengaturan, cadangan.
- **Staf gudang**: mencatat barang masuk & keluar, melihat dan mencetak semua laporan.

## Tes
```
mvn test
```
17 skenario: konsistensi data contoh (stok = jumlah mutasi = saldo akhir laporan), penomoran BM/BK/OP, rollback dokumen multi-baris saat satu baris stok kurang, validasi dokumen, pembatalan (termasuk menolak batal barang masuk yang sudah terlanjur keluar), opname, hak akses staf, 8 staf rebutan stok terakhir bersamaan, kartu stok saldo berjalan, validasi master, kunci login, barcode Code 128, pagination lembar cetak, CSV aman, cadangan database.

## Catatan keamanan & integritas data
- Stok **hanya** berubah lewat dokumen (masuk, keluar, opname, pembatalan); setiap perubahan tercatat di tabel mutasi sehingga kartu stok selalu bisa direkonstruksi.
- Semua dokumen disimpan dalam transaksi `BEGIN IMMEDIATE`. Pengurangan stok memakai `UPDATE ... WHERE stok >= qty`; bila satu baris gagal, seluruh dokumen dibatalkan. Kolom stok diberi `CHECK (stok >= 0)`.
- Nomor dokumen (BM/BK/OP-yyMMdd-001) dibuat di dalam transaksi yang sama sehingga tidak dobel walau dua komputer memakai file database yang sama.
- Dokumen tidak pernah dihapus; pembatalan butuh alasan dan tercatat. Laporan mengabaikan dokumen batal beserta mutasi pembaliknya.
- Password PBKDF2-HMAC-SHA256 bersalt, kunci akun 5 menit setelah 5 kali salah, pesan galat seragam, akun nonaktif langsung ditolak saat menyimpan.
- Query memakai `PreparedStatement`; pencarian di-escape untuk `LIKE`. Ekspor CSV aman dari formula injection Excel.

## Lisensi pihak ketiga
FlatLaf (Apache 2.0), sqlite-jdbc (Apache 2.0), SLF4J (MIT), font Chivo & Space Mono (SIL OFL 1.1, lihat `src/main/resources/fonts/OFL.txt`), bentuk ikon mengikuti Lucide (ISC).
