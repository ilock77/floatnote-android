package com.example.floatnote.data

import org.json.JSONArray
import org.json.JSONObject

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class TimerStatus { IDLE, RUNNING, PAUSED, FINISHED }

/** Status kartu melayang untuk satu catatan/timer. x/y = -1 artinya belum pernah digeser. */
data class OverlayState(
    val enabled: Boolean = false,
    val collapsed: Boolean = false,
    val x: Int = -1,
    val y: Int = -1,
)

data class Note(
    val id: String,
    val title: String = "",
    val body: String = "",
    val colorIndex: Int = 0,
    val updatedAt: Long = 0L,
    val reminderAt: Long? = null,
    val reminderFired: Boolean = false,
    val overlay: OverlayState = OverlayState(),
)

data class TimerItem(
    val id: String,
    val title: String = "",
    val durationMs: Long = 300_000L,
    val status: TimerStatus = TimerStatus.IDLE,
    /** Waktu selesai (epoch ms). Sumber kebenaran saat RUNNING; layar hanya menghitung selisihnya. */
    val endAt: Long = 0L,
    /** Sisa waktu saat PAUSED. */
    val remainingMs: Long = durationMs,
    val colorIndex: Int = 1,
    val createdAt: Long = 0L,
    val overlay: OverlayState = OverlayState(),
) {
    fun remainingAt(now: Long): Long = when (status) {
        TimerStatus.IDLE -> durationMs
        TimerStatus.RUNNING -> (endAt - now).coerceIn(0L, maxOf(durationMs, 0L))
        TimerStatus.PAUSED -> remainingMs
        TimerStatus.FINISHED -> 0L
    }

    fun progressAt(now: Long): Float {
        if (durationMs <= 0L) return 0f
        return (1f - remainingAt(now).toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    }
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val overlayAlpha: Float = 0.92f,
    val onboardingDone: Boolean = false,
    val notificationAsked: Boolean = false,
)

data class AppData(
    val notes: List<Note> = emptyList(),
    val timers: List<TimerItem> = emptyList(),
    val settings: AppSettings = AppSettings(),
) {
    val floatingCount: Int
        get() = notes.count { it.overlay.enabled } + timers.count { it.overlay.enabled }
}

/** Serialisasi JSON sederhana (org.json bawaan Android), tanpa library tambahan. */
internal object Json {

    fun encode(d: AppData): String {
        val root = JSONObject()
        root.put("version", 1)
        root.put(
            "settings",
            JSONObject()
                .put("themeMode", d.settings.themeMode.name)
                .put("overlayAlpha", d.settings.overlayAlpha.toDouble())
                .put("onboardingDone", d.settings.onboardingDone)
                .put("notificationAsked", d.settings.notificationAsked),
        )
        val notes = JSONArray()
        d.notes.forEach { notes.put(noteToJson(it)) }
        root.put("notes", notes)
        val timers = JSONArray()
        d.timers.forEach { timers.put(timerToJson(it)) }
        root.put("timers", timers)
        return root.toString()
    }

    fun decode(text: String): AppData {
        val root = JSONObject(text)
        val s = root.optJSONObject("settings")
        val settings = if (s == null) AppSettings() else AppSettings(
            themeMode = enumOr(s.optString("themeMode"), ThemeMode.SYSTEM),
            overlayAlpha = s.optDouble("overlayAlpha", 0.92).toFloat().coerceIn(0.3f, 1f),
            onboardingDone = s.optBoolean("onboardingDone", false),
            notificationAsked = s.optBoolean("notificationAsked", false),
        )
        val notes = mutableListOf<Note>()
        val na = root.optJSONArray("notes")
        if (na != null) {
            for (i in 0 until na.length()) {
                val o = na.optJSONObject(i) ?: continue
                if (o.optString("id").isBlank()) continue
                notes.add(noteFrom(o))
            }
        }
        val timers = mutableListOf<TimerItem>()
        val ta = root.optJSONArray("timers")
        if (ta != null) {
            for (i in 0 until ta.length()) {
                val o = ta.optJSONObject(i) ?: continue
                if (o.optString("id").isBlank()) continue
                timers.add(timerFrom(o))
            }
        }
        return AppData(notes, timers, settings)
    }

    private inline fun <reified E : Enum<E>> enumOr(name: String?, default: E): E =
        try {
            enumValueOf<E>(name ?: "")
        } catch (e: IllegalArgumentException) {
            default
        }

    private fun overlayToJson(o: OverlayState): JSONObject =
        JSONObject().put("enabled", o.enabled).put("collapsed", o.collapsed).put("x", o.x).put("y", o.y)

    private fun overlayFrom(o: JSONObject?): OverlayState =
        if (o == null) OverlayState()
        else OverlayState(o.optBoolean("enabled", false), o.optBoolean("collapsed", false), o.optInt("x", -1), o.optInt("y", -1))

    private fun noteToJson(n: Note): JSONObject =
        JSONObject()
            .put("id", n.id)
            .put("title", n.title)
            .put("body", n.body)
            .put("colorIndex", n.colorIndex)
            .put("updatedAt", n.updatedAt)
            .put("reminderAt", n.reminderAt ?: JSONObject.NULL)
            .put("reminderFired", n.reminderFired)
            .put("overlay", overlayToJson(n.overlay))

    private fun noteFrom(o: JSONObject): Note = Note(
        id = o.getString("id"),
        title = o.optString("title", ""),
        body = o.optString("body", ""),
        colorIndex = o.optInt("colorIndex", 0),
        updatedAt = o.optLong("updatedAt", 0L),
        reminderAt = if (o.isNull("reminderAt")) null else o.getLong("reminderAt"),
        reminderFired = o.optBoolean("reminderFired", false),
        overlay = overlayFrom(o.optJSONObject("overlay")),
    )

    private fun timerToJson(t: TimerItem): JSONObject =
        JSONObject()
            .put("id", t.id)
            .put("title", t.title)
            .put("durationMs", t.durationMs)
            .put("status", t.status.name)
            .put("endAt", t.endAt)
            .put("remainingMs", t.remainingMs)
            .put("colorIndex", t.colorIndex)
            .put("createdAt", t.createdAt)
            .put("overlay", overlayToJson(t.overlay))

    private fun timerFrom(o: JSONObject): TimerItem {
        val duration = o.optLong("durationMs", 300_000L)
        return TimerItem(
            id = o.getString("id"),
            title = o.optString("title", ""),
            durationMs = duration,
            status = enumOr(o.optString("status"), TimerStatus.IDLE),
            endAt = o.optLong("endAt", 0L),
            remainingMs = o.optLong("remainingMs", duration),
            colorIndex = o.optInt("colorIndex", 1),
            createdAt = o.optLong("createdAt", 0L),
            overlay = overlayFrom(o.optJSONObject("overlay")),
        )
    }
}
