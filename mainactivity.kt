package com.example.objectscanner

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.google.mlkit.common.model.LocalModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.custom.CustomImageLabelerOptions
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var objectImage: ImageView
    private lateinit var labelText: TextView
    private lateinit var captureImgBtn: Button
    private lateinit var resetBtn: Button
    private lateinit var nextBtn: Button
    private lateinit var recommendBtn: Button
    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>
    private lateinit var imageLabeler: com.google.mlkit.vision.label.ImageLabeler // Declare as a class property

    private val detectedIngredients = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        objectImage = findViewById(R.id.objectImage)
        labelText = findViewById(R.id.labelText)
        captureImgBtn = findViewById(R.id.captureImgBtn)
        resetBtn = findViewById(R.id.resetBtn)
        nextBtn = findViewById(R.id.nextBtn)
        recommendBtn = findViewById(R.id.recommendBtn)

        checkCameraPermission()

        val localModel = LocalModel.Builder()
            .setAssetFilePath("metadata.tflite")
            .build()

        val customOptions = CustomImageLabelerOptions.Builder(localModel)
            .setConfidenceThreshold(0.5f)
            .build()

        imageLabeler = ImageLabeling.getClient(customOptions) // Initialize the imageLabeler here

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

        captureImgBtn.setOnClickListener {
            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            if (takePictureIntent.resolveActivity(packageManager) != null) {
                cameraLauncher.launch(takePictureIntent)
            } else {
                labelText.text = "No camera app found on the device."
            }
        }

        resetBtn.setOnClickListener {
            detectedIngredients.clear()
            labelText.text = "Reset complete. Start fresh!"
            objectImage.setImageDrawable(null)
        }

        nextBtn.setOnClickListener {
            labelText.text = "Scan the next ingredient."
        }

        recommendBtn.setOnClickListener {
            navigateToRecommendations()
        }
    }

    private fun labelImage(bitmap: Bitmap) {
        val inputImage = InputImage.fromBitmap(bitmap, 0)

        imageLabeler.process(inputImage)
            .addOnSuccessListener { labels ->
                if (labels.isNotEmpty()) {
                    val ingredient = labels[0].text // Use the most confident label
                    detectedIngredients.add(ingredient)
                    labelText.text = "Detected: $ingredient"
                } else {
                    labelText.text = "No ingredient detected."
                }
            }
            .addOnFailureListener { e ->
                labelText.text = "Error: ${e.message}"
            }
    }

    private fun navigateToRecommendations() {
        val intent = Intent(this, RecipeRecommendationActivity::class.java)
        intent.putStringArrayListExtra("detectedIngredients", ArrayList(detectedIngredients))
        startActivity(intent)
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 1)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                labelText.text = "Camera permission is required to use this feature."
            }
        }
    }

    data class Recipe(
        val name: String,
        val ingredients: List<String>,
        val link: String
    )
}
