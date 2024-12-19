package com.example.objectscanner

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.common.model.LocalModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabel
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.custom.CustomImageLabelerOptions

class MainActivity : AppCompatActivity() {

    private lateinit var objectImage: ImageView
    private lateinit var labelText: TextView
    private lateinit var captureImgBtn: Button
    private lateinit var imageLabeler: com.google.mlkit.vision.label.ImageLabeler
    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        objectImage = findViewById(R.id.objectImage)
        labelText = findViewById(R.id.labelText)
        captureImgBtn = findViewById(R.id.captureImgBtn)

        checkCameraPermission()

        // Initialize LocalModel for your custom TFLite model
        val localModel = LocalModel.Builder()
            .setAssetFilePath("metadata.tflite") // Path to your model in assets folder
            .build()

        // Configure the labeler to use the LocalModel
        val customOptions = CustomImageLabelerOptions.Builder(localModel)
            .setConfidenceThreshold(0.5f) // Optional: confidence threshold
            .build()

        imageLabeler = ImageLabeling.getClient(customOptions)

        // Configure camera launcher
        cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val extras = result.data?.extras
                val imageBitmap = extras?.get("data") as? Bitmap
                if (imageBitmap != null) {
                    objectImage.setImageBitmap(imageBitmap)
                    labelImage(imageBitmap)
                } else {
                    labelText.text = "Unable to capture image."
                }
            } else {
                labelText.text = "Image capture cancelled."
            }
        }

        // Set onClickListener for the capture button
        captureImgBtn.setOnClickListener {
            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            if (takePictureIntent.resolveActivity(packageManager) != null) {
                cameraLauncher.launch(takePictureIntent)
            } else {
                labelText.text = "No camera app found on the device."
            }
        }
    }

    private fun labelImage(bitmap: Bitmap) {
        // Create an InputImage from the captured Bitmap
        val inputImage = InputImage.fromBitmap(bitmap, 0)

        // Process the image using the custom labeler
        imageLabeler.process(inputImage)
            .addOnSuccessListener { labels ->
                displayLabels(labels)
            }
            .addOnFailureListener { e ->
                labelText.text = "Error: ${e.message}"
            }
    }

    private fun displayLabels(labels: List<ImageLabel>) {
        if (labels.isNotEmpty()) {
            // Display all detected labels with confidence
            val labelTexts = labels.joinToString(separator = "\n") {it.text}
            labelText.text = labelTexts
        } else {
            labelText.text = "No labels found."
        }
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 1)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, no action needed
            } else {
                labelText.text = "Camera permission is required to use this feature."
            }
        }
    }
}
