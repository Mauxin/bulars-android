package com.example.mauxin.bulars.screens

import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.support.v4.content.FileProvider
import android.support.v7.app.AlertDialog
import android.widget.SearchView
import com.example.mauxin.bulars.R
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.text.FirebaseVisionText




class MainActivity : AppCompatActivity() {

    private var analytics: FirebaseAnalytics? = null

    private var photoUriToLoad: Uri? = null
    private var imageBitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        analytics = FirebaseAnalytics.getInstance(this)
        //setSupportActionBar(toolbarSearch)

        cameraButton.setOnClickListener {
            logClickEvent()
            showDialog()
        }

        searchTextBar.setOnQueryTextListener(object : SearchView.OnQueryTextListener {

            override fun onQueryTextChange(newText: String): Boolean {
                return false
            }

            override fun onQueryTextSubmit(query: String): Boolean {
                textSearching(query)
                return false
            }

        })

    }

    // Method to show an alert dialog with yes, no and cancel button
    private fun showDialog(){
        lateinit var dialog: AlertDialog
        val builder = AlertDialog.Builder(this)

        builder.setTitle("Selecione sua imagem")

        val dialogClickListener = DialogInterface.OnClickListener{ _, which ->
            when(which){
                DialogInterface.BUTTON_POSITIVE -> getImageFromCamera()
                DialogInterface.BUTTON_NEGATIVE -> getImageFromFile()
            }
        }

        builder.setPositiveButton("CÂMERA",dialogClickListener)
        builder.setNegativeButton("GALERIA",dialogClickListener)
        builder.setNeutralButton("CANCELAR",dialogClickListener)

        dialog = builder.create()
        dialog.show()
    }

    fun textSearching(query: String) {
        val searchTxtIntent = Intent(this, SearchableActivity::class.java)
        searchTxtIntent.putExtra("MEDICATE", query)
        startActivity(searchTxtIntent)
    }

    private fun getImageFromCamera() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(packageManager) != null) {

            val photoFile: File? = try {
                createImageFile()
            } catch (exception: IOException) {
                null
            }

            if (photoFile != null) {
                photoUriToLoad = FileProvider.getUriForFile(
                    this,
                    "com.example.mauxin.bulars",
                    photoFile)
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUriToLoad)
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
            }
        }
    }

    fun createImageFile(): File {
        // Create an image file name
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = "JPEG_" + timeStamp + "_"
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val image = File.createTempFile(
            imageFileName, /* prefix */
            ".jpg", /* suffix */
            storageDir      /* directory */
        )

        return image
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
            startActivityForResult(pickPhotoIntent, REQUEST_PICK_IMAGE)
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == RESULT_OK ) {
            when (requestCode) {
                REQUEST_IMAGE_CAPTURE -> getCameraImageFromResultData()
                REQUEST_PICK_IMAGE -> getFileImageFromResultData(data!!)
                else -> Unit
            }
        }
    }

    private fun getCameraImageFromResultData() {
        imageBitmap = loadPhotoFromUri(photoUriToLoad)

        imageBitmap?.let { recognizeText(imageBitmap)}
        displayBitmap(imageBitmap)
    }

    private fun loadPhotoFromUri(photoUri: Uri?): Bitmap? = try {
        val imageStream = contentResolver.openInputStream(photoUri)
        BitmapFactory.decodeStream(imageStream)
    } catch (exception: Exception) {
        null
    }

    private fun displayBitmap(bitmap: Bitmap?) = bitmap?.let { exampleImageView.setImageBitmap(it) }

    fun logClickEvent() {
        val bundle = Bundle()
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "android_button_click")
        analytics?.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle)
    }

    private fun recognizeText(image: Bitmap?) = image?.let {
        val firImage  = FirebaseVisionImage.fromBitmap(it)

        val detector = FirebaseVision.getInstance()
            .onDeviceTextRecognizer

        detector.processImage(firImage)
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
                val searchIntent = Intent(this, SearchableActivity::class.java)
                var medicate = biggestText?.text

                medicate = medicate?.replace(".", "")?.toLowerCase()

                searchIntent.putExtra("MEDICATE", medicate)
                startActivity(searchIntent)
            }
            .addOnFailureListener {
                recognizedTextView.text = "Error"
            }

    }

    companion object {
        const val REQUEST_IMAGE_CAPTURE = 1
        const val REQUEST_PICK_IMAGE = 2
    }

}
