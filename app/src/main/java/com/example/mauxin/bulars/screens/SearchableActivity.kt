package com.example.mauxin.bulars.screens

import android.app.Activity
import android.os.Bundle
import com.example.mauxin.bulars.R
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.searchable.*

class SearchableActivity: Activity() {

    private var db: FirebaseFirestore? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.searchable)
        db = FirebaseFirestore.getInstance()
        searchMedicate(intent.getStringExtra("MEDICATE"))
    }

    private fun searchMedicate(medicate: String) {
        db?.collection("medicates")
            ?.whereEqualTo("name", medicate)
            ?.get()
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    for (document in task.result!!) {
                        resultTextView.text = document["medicineBottle"].toString()
                    }
                } else {
                    resultTextView.text = "Erro"
                }
            }
    }

}