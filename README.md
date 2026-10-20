# Sticker Studio (Android, pribadi)

Aplikasi pembuat stiker WhatsApp offline dan bebas iklan.

## v1.1 Editor Foto
- Crop: geser empat sudut kotak crop, klik **Terapkan Crop**.
- Teks: klik **Teks**, masukkan kalimat, lalu geser tulisan langsung di atas foto. Sesuaikan warna dan ukuran.
- Hapus BG: klik **Hapus BG**, lalu ketuk warna latar foto. Atur slider toleransi dan ulangi jika diperlukan. Penghapusan menggunakan flood-fill warna serupa, **bukan** segmentasi AI/otomatis untuk latar kompleks.
- Undo, simpan WebP 512x512, koleksi sampai 30, ekspor paket ke WhatsApp.

## Membuat APK tanpa laptop
Repository -> Actions -> **Build StickerStudio APK** -> pilih run hijau -> Artifacts -> **StickerStudio-APK** -> ekstrak ZIP -> instal app-debug.apk.

### Penting tentang update APK
Build debug GitHub di-runner baru mungkin memakai sertifikat tanda tangan berbeda. Jika instalasi berbunyi "App not installed" karena tanda tangan tidak cocok, Android meminta **hapus versi lama terlebih dahulu**. Menghapus aplikasi juga akan menghapus koleksi stiker lokal. Simpan foto aslinya sebelum menghapus.

Fitur video animasi dan penghapusan background AI belum tersedia.
