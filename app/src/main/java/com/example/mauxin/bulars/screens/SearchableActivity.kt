package com.example.mauxin.bulars.screens

import android.app.Activity
import android.os.Bundle
import com.example.mauxin.bulars.R
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.searchable.*
import java.net.URL
import org.jsoup.Jsoup
import java.io.IOException
import android.os.StrictMode


class SearchableActivity: Activity() {

    private var db: FirebaseFirestore? = null
    private var site: URL? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.searchable)
        db = FirebaseFirestore.getInstance()
        site = URL("https://www.bulario.com/" + intent.getStringExtra("MEDICATE") + "/")
        searchMedicate(intent.getStringExtra("MEDICATE"))
    }

    private fun searchMedicate(medicate: String) {
        db?.collection("medicates")
            ?.whereEqualTo("name", medicate)
            ?.get()
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    if (!task.result?.documents?.isEmpty()!!) {
                        for (document in task.result!!) {
                            resultTextView.text = document["medicineBottle"].toString()
                        }
                    } else {
                        getDescriptionFromWeb(medicate)
                    }
                } else {
                    resultTextView.text = "Erro"
                }
            }
    }

    private fun getDescriptionFromWeb(medicate: String) {
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        try {

            val url : URL = site!!
            val doc = Jsoup.parse(url, 100000)
            val body = doc.body().getElementById("bulaBody")

            val headers = body.getElementsByTag("h3")
            val texts = body.getElementsByTag("p")

            var myText = ""
            var counter = 0

            while (counter < headers.size) {
                myText += ("\n\n")
                myText += headers[counter].text()
                myText += ("\n\n")
                myText += ("       " + texts[counter].text())

                counter += 1
            }

            addMedicateToDB(medicate, myText)
            resultTextView.text = myText

        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

    fun addMedicateToDB(name: String, description: String) {
        val medicate = HashMap<String, Any>()
        medicate["name"] = name
        medicate["medicineBottle"] = description

        db?.collection("medicates")?.add(medicate)?.addOnSuccessListener{ documentReference ->
        }?.addOnFailureListener{ e -> }
    }

}