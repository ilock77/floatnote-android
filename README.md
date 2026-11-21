# FloatNote

Catatan dan timer sebagai kartu melayang di atas aplikasi lain. Kotlin + Jetpack Compose, tanpa login, iklan, atau cloud.
minSdk 26 (Android 8.0), targetSdk 35 (Android 15).

## Struktur
```
app/src/main/java/com/example/floatnote/
  FloatNoteApplication.kt     init data + channel notifikasi
  MainActivity.kt             host Compose, status izin, sinkronisasi saat dibuka
  data/Models.kt              Note, TimerItem, OverlayState, JSON
  data/Repository.kt          satu sumber data (StateFlow) + simpan atomik ke floatnote.json
  system/Permissions.kt       cek & buka layar izin (overlay, notifikasi, alarm, baterai)
  system/Scheduler.kt         AlarmManager (timer & pengingat) + syncAll()
  system/TimerEngine.kt       mulai/jeda/lanjut/reset/selesai (+ ReminderEngine)
  system/Notifications.kt     channel, notifikasi service/timer/pengingat
  system/Receivers.kt         AlarmReceiver, boot/update/perubahan izin alarm
  overlay/OverlayService.kt   foreground service, mengelola jendela
  overlay/OverlayWindow.kt    satu jendela melayang (WindowManager + ComposeView)
  overlay/DragLayout.kt       geser kartu tanpa mengganggu tombol di dalamnya
  overlay/OverlayContent.kt   isi kartu catatan/timer + gelembung ciut
  ui/                         HomeScreen, NoteEditorScreen, SettingsScreen, izin, dialog timer, tema pastel
```

## Cara kerja timer (inti ketahanan)
- Saat Mulai: `endAt = jam sekarang + durasi` disimpan ke disk, lalu alarm `setExactAndAllowWhileIdle` dipasang.
- Layar hanya menggambar `endAt - sekarang`. Menutup app, mematikan layar, atau proses dimatikan sistem tidak mengubah hasil.
- Timer habis: `AlarmReceiver` menandai FINISHED (hanya sekali) dan memunculkan notifikasi.
- HP restart / app diperbarui / izin alarm berubah / app dibuka: `Scheduler.syncAll()` memasang ulang alarm dan menyelesaikan timer yang sudah lewat.
- Jeda menyimpan sisa waktu; Lanjutkan menghitung `endAt` baru dari sisa itu.

## Build APK
Prasyarat: Android Studio (Ladybug atau lebih baru) dengan JDK 17 dan Android SDK 35.

Debug (untuk dicoba):
1. Android Studio > Open > pilih folder `FloatNote`, tunggu Gradle sync.
2. Build > Build Bundle(s) / APK(s) > Build APK(s).
3. Hasil: `app/build/outputs/apk/debug/app-debug.apk`.

Lewat terminal (butuh Gradle terpasang sekali untuk membuat wrapper):
```
gradle wrapper --gradle-version 8.9
./gradlew assembleDebug
```

Pasang ke HP: aktifkan Opsi pengembang > Debugging USB, lalu `adb install -r app/build/outputs/apk/debug/app-debug.apk`,
atau salin APK ke HP dan buka (izinkan "instal dari sumber ini").

Release: Build > Generate Signed Bundle / APK > APK, buat keystore sendiri dan simpan baik-baik.
