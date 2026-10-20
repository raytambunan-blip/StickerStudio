# Sticker Studio v1.2 — AI Hapus Background (Offline)

Sticker Studio sekarang memiliki tombol HAPUS BACKGROUND AI (OFFLINE) untuk memotong objek utama (manusia, barang, hewan) dengan model Silueta/U²-Net. Foto tidak pernah dikirim ke internet.

Fitur lama tetap ada: crop, teks yang bisa digeser, warna teks, hapus BG manual berdasarkan warna, Undo, penyimpanan WebP stiker WhatsApp.

## Cara menggunakan
1. Ketuk Pilih foto untuk diedit.
2. Tekan HAPUS BACKGROUND AI (OFFLINE). Tunggu mesin ONNX menyelesaikan pemrosesan di HP.
3. Lanjutkan crop/teks atau Undo. Simpan stiker.

## Teknis
- AI: silueta.onnx (sekitar 43 MB), referensi https://github.com/danielgatis/rembg.
- Mesin lokal: com.microsoft.onnxruntime:onnxruntime-android:1.28.0.
- Model diunduh oleh GitHub Actions pada saat build (BUKAN saat digunakan), diverifikasi MD5 sebagaimana upstream rembg.
- Semua proses hapus BG AI dilakukan on-device dan gratis. APK lebih besar karena menyertakan bobot model.
- Hasil AI tidak dijamin sempurna pada rambut halus, objek transparan, objek jamak, atau foto yang rumit.

## APK baru
Buka https://github.com/raytambunan-blip/StickerStudio/actions, pilih run terbaru hijau dan unduh Artifacts StickerStudio-APK. Ekstrak APK.

PERINGATAN: Sertifikat debug pada GitHub runner dapat berubah tiap build. Jika Android tidak bisa memperbarui, perlu menghapus versi lama; uninstall akan menghapus stiker lokal. Simpan foto asli sebelum uninstall.

Pengolahan video animasi belum termasuk versi ini.