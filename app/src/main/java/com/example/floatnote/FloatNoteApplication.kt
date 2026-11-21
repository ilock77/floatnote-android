package com.example.floatnote

import android.app.Application
import com.example.floatnote.data.Repository
import com.example.floatnote.system.Notifications

class FloatNoteApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Dijalankan juga saat proses dibangunkan oleh alarm/receiver, jadi data selalu siap.
        Repository.init(this)
        Notifications.createChannels(this)
    }
}
