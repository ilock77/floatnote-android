package com.example.floatnote.data

import android.content.Context
import android.util.AtomicFile
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException

/**
 * Satu-satunya sumber data. Dipakai bersama oleh Activity, OverlayService, dan BroadcastReceiver
 * (semuanya satu proses), jadi perubahan langsung terlihat di semua tempat lewat [state].
 * Disimpan ke satu file JSON di penyimpanan internal dengan penulisan atomik (anti-korup).
 */
object Repository {
    private const val TAG = "Repository"

    private var file: AtomicFile? = null
    private val _state = MutableStateFlow(AppData())
    val state: StateFlow<AppData> = _state.asStateFlow()

    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val writeLock = Any()
    private var pendingSave: Job? = null

    fun init(context: Context) {
        if (file != null) return
        val base = File(context.filesDir, "floatnote.json")
        val f = AtomicFile(base)
        file = f
        try {
            // readFully juga memulihkan dari file cadangan bila HP mati di tengah penulisan.
            _state.value = Json.decode(String(f.readFully(), Charsets.UTF_8))
        } catch (e: FileNotFoundException) {
            // Pemakaian pertama: belum ada data.
        } catch (e: Exception) {
            Log.e(TAG, "File data rusak, disimpan sebagai cadangan", e)
            try {
                base.copyTo(File(context.filesDir, "floatnote.corrupt.json"), overwrite = true)
            } catch (ignored: IOException) {
            }
        }
    }

    // ---- penyimpanan ----

    fun saveNow() {
        val f = file ?: return
        synchronized(writeLock) {
            val bytes = Json.encode(_state.value).toByteArray(Charsets.UTF_8)
            var out: FileOutputStream? = null
            try {
                out = f.startWrite()
                out.write(bytes)
                f.finishWrite(out)
            } catch (e: IOException) {
                Log.e(TAG, "Gagal menyimpan", e)
                if (out != null) f.failWrite(out)
            }
        }
    }

    @Synchronized
    private fun scheduleSave() {
        pendingSave?.cancel()
        pendingSave = ioScope.launch {
            delay(500)
            saveNow()
        }
    }

    /** Tulis segera. Dipanggil saat Activity berhenti / service mati. */
    fun flush() {
        synchronized(this) { pendingSave?.cancel() }
        saveNow()
    }

    /** Ubah state. debounced=true untuk ketikan teks (disimpan 0,5 dtk setelah ketikan terakhir). */
    fun update(debounced: Boolean = false, transform: (AppData) -> AppData) {
        _state.update(transform)
        if (debounced) scheduleSave() else saveNow()
    }

    // ---- baca ----

    fun note(id: String): Note? = _state.value.notes.firstOrNull { it.id == id }
    fun timer(id: String): TimerItem? = _state.value.timers.firstOrNull { it.id == id }

    // ---- catatan ----

    fun addNote(n: Note) = update { it.copy(notes = listOf(n) + it.notes) }

    fun deleteNote(id: String) = update { d -> d.copy(notes = d.notes.filter { it.id != id }) }

    fun updateNote(id: String, debounced: Boolean = false, transform: (Note) -> Note) =
        update(debounced) { d -> d.copy(notes = d.notes.map { if (it.id == id) transform(it) else it }) }

    // ---- timer ----

    fun addTimer(t: TimerItem) = update { it.copy(timers = listOf(t) + it.timers) }

    fun deleteTimer(id: String) = update { d -> d.copy(timers = d.timers.filter { it.id != id }) }

    fun updateTimer(id: String, transform: (TimerItem) -> TimerItem) =
        update { d -> d.copy(timers = d.timers.map { if (it.id == id) transform(it) else it }) }

    /**
     * Ubah timer secara atomik. Mengembalikan timer baru bila transform menghasilkan perubahan,
     * atau null bila transform menolak (mengembalikan null) / timer tidak ada.
     * Dipakai agar transisi (mis. RUNNING -> FINISHED) hanya terjadi sekali walau
     * alarm dan tampilan sama-sama mendeteksinya.
     */
    fun modifyTimer(id: String, transform: (TimerItem) -> TimerItem?): TimerItem? {
        var result: TimerItem? = null
        _state.update { d ->
            result = null
            val cur = d.timers.firstOrNull { it.id == id } ?: return@update d
            val next = transform(cur) ?: return@update d
            result = next
            d.copy(timers = d.timers.map { if (it.id == id) next else it })
        }
        if (result != null) saveNow()
        return result
    }

    // ---- kartu melayang (berlaku untuk catatan maupun timer) ----

    fun updateOverlay(id: String, debounced: Boolean = false, transform: (OverlayState) -> OverlayState) =
        update(debounced) { d ->
            d.copy(
                notes = d.notes.map { if (it.id == id) it.copy(overlay = transform(it.overlay)) else it },
                timers = d.timers.map { if (it.id == id) it.copy(overlay = transform(it.overlay)) else it },
            )
        }

    fun disableAllFloating() = update { d ->
        d.copy(
            notes = d.notes.map { if (it.overlay.enabled) it.copy(overlay = it.overlay.copy(enabled = false)) else it },
            timers = d.timers.map { if (it.overlay.enabled) it.copy(overlay = it.overlay.copy(enabled = false)) else it },
        )
    }

    // ---- pengaturan ----

    fun updateSettings(debounced: Boolean = false, transform: (AppSettings) -> AppSettings) =
        update(debounced) { it.copy(settings = transform(it.settings)) }
}
