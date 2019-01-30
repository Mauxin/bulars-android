package com.example.mauxin.bulars.screens

import android.os.StrictMode
import com.google.firebase.firestore.FirebaseFirestore
import org.jsoup.Jsoup
import java.io.IOException
import java.net.URL

class SearchableViewModel constructor(medicate: String) {

    private var db = FirebaseFirestore.getInstance()
    private var site: URL = URL("https://www.bulario.com/" + medicate + "/")

    fun searchMedicate(medicate: String, callback: (String) -> Unit) {
        db.collection("medicates")
            .whereEqualTo("name", medicate)
            .get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    if (!task.result?.documents?.isEmpty()!!) {
                        for (document in task.result!!) {
                            callback(document["medicineBottle"].toString())
                        }
                    } else {
                        getDescriptionFromWeb(medicate, callback)
                    }
                } else {
                    callback("Erro")
                }
            }
    }

    private fun getDescriptionFromWeb(medicate: String, callback: (String) -> Unit) {
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        try {

            val url : URL = site
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
            callback(myText)

        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

    private fun addMedicateToDB(name: String, description: String) {
        val medicate = HashMap<String, Any>()
        medicate["name"] = name
        medicate["medicineBottle"] = description

        db.collection("medicates").add(medicate).addOnSuccessListener{ documentReference ->
        }.addOnFailureListener{ e -> }
    }
}