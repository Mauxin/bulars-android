package com.example.mauxin.bulars

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_main.*






class MainActivity : AppCompatActivity() {

    private var analytics: FirebaseAnalytics? = null
    private var db: FirebaseFirestore? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        analytics = FirebaseAnalytics.getInstance(this)
        db = FirebaseFirestore.getInstance()
        searchCameraButton.setOnClickListener { changeText() }
    }

    fun changeText() {
        logClickEvent()

        db?.collection("medicates")
            ?.whereEqualTo("name", "benalet")
            ?.get()
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    for (document in task.result) {
                       recognizedTextView.text = document["medicineBottle"].toString()
                    }
                } else {
                    recognizedTextView.text = "Erro"
                }
            }
    }

    fun logClickEvent() {
        val bundle = Bundle()
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "android_button_click")
        analytics?.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle)
    }
}
