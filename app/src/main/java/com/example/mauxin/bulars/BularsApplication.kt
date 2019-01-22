package com.example.mauxin.bulars

import android.app.Application
import com.google.firebase.FirebaseApp

class BularsApplication: Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
    }
}