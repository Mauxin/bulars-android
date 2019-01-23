package com.example.mauxin.bulars.screens

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.support.v4.content.FileProvider
import com.example.mauxin.bulars.R
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.text.FirebaseVisionText
import kotlinx.android.synthetic.main.searchable.*


class MainActivity : AppCompatActivity() {

    private var analytics: FirebaseAnalytics? = null
    private var db: FirebaseFirestore? = null

    private var photoUriToLoad: Uri? = null
    private var imageBitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        analytics = FirebaseAnalytics.getInstance(this)
        db = FirebaseFirestore.getInstance()
        setSupportActionBar(toolbarSearch)

        searchCameraButton.setOnClickListener { getImageFromFile() }

    }

    /*fun changeText() {
        logClickEvent()

        db?.collection("medicates")
            ?.whereEqualTo("name", "benalet")
            ?.get()
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    for (document in task.result) {
                       recognizedTextView.text = document["name"].toString()
                    }
                } else {
                    recognizedTextView.text = "Erro"
                }
            }
    }*/

    private fun getImageFromCamera() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(packageManager) != null) {

            val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            val photoFile: File? = try {
                createImageFile(storageDir)
            } catch (exception: IOException) {
                null
            }

            if (photoFile != null) {
                photoUriToLoad = FileProvider.getUriForFile(
                    this,
                    "com.example.mauxin.bulars",
                    photoFile)
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUriToLoad)
                startActivityForResult(takePictureIntent, 1)
            }
        }
    }


    @SuppressLint("SimpleDateFormat")
    @Throws(IOException::class)
    fun createImageFile(storageDir: File): File {
        // Create an image file name
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = "JPEG_" + timeStamp + "_"


        return File.createTempFile(
            imageFileName, /* prefix */
            ".jpg", /* suffix */
            storageDir      /* directory */
        )
    }

    private fun getFileImageFromResultData(data: Intent) {
        imageBitmap = loadPhotoFromUri(data.data)
        imageBitmap?.let { recognizeText(imageBitmap)}
        displayBitmap(imageBitmap)
    }

    private fun getImageFromFile() {
        val pickPhotoIntent = Intent(Intent.ACTION_PICK)
        pickPhotoIntent.type = "image/*"

        if (pickPhotoIntent.resolveActivity(packageManager) != null) {
            startActivityForResult(pickPhotoIntent, 2)
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == RESULT_OK && data != null) {
            when (requestCode) {
                1 -> getCameraImageFromResultData(data)
                2 -> getFileImageFromResultData(data)
                else -> Unit
            }
        }
    }

    private fun getCameraImageFromResultData(data: Intent) {
        imageBitmap = if (photoUriToLoad != null) {
            loadPhotoFromUri(photoUriToLoad)
        } else {
            val extras = data.extras
            extras["data"] as Bitmap
        }

        recognizeText(imageBitmap)
        displayBitmap(imageBitmap)
    }

    private fun loadPhotoFromUri(photoUri: Uri?): Bitmap? = try {
        val imageStream = contentResolver.openInputStream(photoUri)
        BitmapFactory.decodeStream(imageStream)
    } catch (exception: Exception) {
        null
    }

    private fun displayBitmap(bitmap: Bitmap?) = bitmap?.let { exampleImageView.setImageBitmap(it) }

    /*private fun searchMedicate(medicate: String) {
        recognizedTextView.text = medicate
    }

    fun logClickEvent() {
        val bundle = Bundle()
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "android_button_click")
        analytics?.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle)
    }*/

    private fun recognizeText(image: Bitmap?) = image?.let {
        val firImage  = FirebaseVisionImage.fromBitmap(it)

        val detector = FirebaseVision.getInstance()
            .onDeviceTextRecognizer

        val result = detector.processImage(firImage)
            .addOnSuccessListener { firebaseVisionText ->

                var biggestText: FirebaseVisionText.Element? = firebaseVisionText.textBlocks.first().lines.first().elements.first()

                for (block in firebaseVisionText.textBlocks) {
                    for (line in block.lines) {
                        for (element in line.elements) {
                            if (element.boundingBox?.height()!! > biggestText?.boundingBox?.height()!!) {
                                biggestText = element
                            }
                        }
                    }
                }

                recognizedTextView.text = biggestText?.text
            }
            .addOnFailureListener {
                recognizedTextView.text = "Deumerdaaa"
            }


        /*for (block in result.result!!.textBlocks) {
            val blockText = block.text
            recognizedTextView.text = blockText
        }*/
    }

}
