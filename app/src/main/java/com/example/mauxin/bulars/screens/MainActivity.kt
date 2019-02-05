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
import com.example.mauxin.bulars.components.AnalyticsEvents
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.text.FirebaseVisionText
import android.content.Context
import android.graphics.Matrix
import android.media.ExifInterface
import com.example.mauxin.bulars.R


class MainActivity : AppCompatActivity() {

    private var photoUriToLoad: Uri? = null
    private var imageBitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        cameraButton.setOnClickListener {
            AnalyticsEvents.clickEvent(this, "start_image_selection")
            showDialog()
        }

        searchTextBar.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextChange(newText: String): Boolean {
                return false
            }

            override fun onQueryTextSubmit(query: String): Boolean {
                AnalyticsEvents.searchingEvent(searchTextBar.context, query, "text")
                textSearching(query)
                return false
            }
        })

    }

    private fun showDialog(){
        lateinit var dialog: AlertDialog
        val builder = AlertDialog.Builder(this)

        builder.setTitle(R.string.select_image)

        val dialogClickListener = DialogInterface.OnClickListener{ _, which ->
            when(which){
                DialogInterface.BUTTON_POSITIVE -> getImageFromCamera()
                DialogInterface.BUTTON_NEGATIVE -> getImageFromFile()
                DialogInterface.BUTTON_NEUTRAL -> AnalyticsEvents.clickEvent(this, "cancel_image_selection")
            }
        }

        builder.setPositiveButton(R.string.camera,dialogClickListener)
        builder.setNegativeButton(R.string.library,dialogClickListener)
        builder.setNeutralButton(R.string.cancel,dialogClickListener)

        dialog = builder.create()
        dialog.show()
    }

    fun textSearching(query: String) {
        val searchTxtIntent = Intent(this, SearchableActivity::class.java)
        val queryOk = query.replace(" ", "_").toLowerCase()
        searchTxtIntent.putExtra("MEDICATE", queryOk)
        startActivity(searchTxtIntent)
    }

    private fun getImageFromCamera() {
        AnalyticsEvents.clickEvent(this, "open_camera")

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
                    getString(R.string.project_package),
                    photoFile)
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUriToLoad)
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
            }
        }
    }

    fun createImageFile(): File {
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
        AnalyticsEvents.clickEvent(this, "open_library")

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
        imageBitmap = getCameraPhotoOrientation(imageBitmap!!, photoUriToLoad.toString())

        //imageBitmap?.let { recognizeText(imageBitmap)}
        displayBitmap(imageBitmap)
    }

    private fun loadPhotoFromUri(photoUri: Uri?): Bitmap? = try {
        val imageStream = contentResolver.openInputStream(photoUri)
        BitmapFactory.decodeStream(imageStream)
    } catch (exception: Exception) {
        null
    }

    private fun displayBitmap(bitmap: Bitmap?) = bitmap?.let { exampleImageView.setImageBitmap(it) }

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


                AnalyticsEvents.searchingEvent(this, medicate!!, "image")
                searchIntent.putExtra("MEDICATE", medicate)
                startActivity(searchIntent)
            }
            .addOnFailureListener {
                recognizedTextView.text = getString(R.string.error)
            }

    }

    fun getCameraPhotoOrientation(image: Bitmap, imagePath: String): Bitmap {
        val ei = ExifInterface(imagePath)
        val orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                                     ExifInterface.ORIENTATION_UNDEFINED)

        var rotatedBitmap: Bitmap?

        rotatedBitmap = when(orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> rotateImage(image, 90)
            ExifInterface.ORIENTATION_ROTATE_180 -> rotateImage(image, 180)
            ExifInterface.ORIENTATION_ROTATE_270 -> rotateImage(image, 270)
            ExifInterface.ORIENTATION_NORMAL -> image
            else -> image
        }

        return rotatedBitmap!!
    }

    private fun rotateImage(source: Bitmap, angle: Int): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(angle.toFloat())
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height,
                                   matrix, true)
    }

    companion object {
        const val REQUEST_IMAGE_CAPTURE = 1
        const val REQUEST_PICK_IMAGE = 2
    }

}
