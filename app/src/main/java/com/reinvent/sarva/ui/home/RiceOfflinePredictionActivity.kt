package com.reinvent.sarva.ui.home
import android.util.Log

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.canhub.cropper.CropImage
import com.canhub.cropper.CropImageView
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import okhttp3.*
import com.google.gson.Gson
import com.reinvent.sarva.R
import com.reinvent.sarva.databinding.ActivityOfflinePredictionBinding
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.io.IOException
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class RiceOfflinePredictionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOfflinePredictionBinding
    private lateinit var interpreter: Interpreter
    private var selectedBitmap: Bitmap? = null

    companion object {
        private const val MODEL_PATH = "disease_detection_model_mobnet.tflite"
    }

    private val cropActivityResultLauncher = registerForActivityResult(CropImageContract()) { result ->
        if (result.isSuccessful) {
            val uri = result.uriContent
            uri?.let {
                selectedBitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
                binding.imagePreview.setImageBitmap(selectedBitmap)
                binding.predictButton.isEnabled = true
            }
        } else {
            Toast.makeText(this, "Image cropping failed", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOfflinePredictionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        interpreter = Interpreter(loadModelFile())

        binding.cameraButton.setOnClickListener { openCamera() }
        binding.galleryButton.setOnClickListener { openGallery() }
        binding.predictButton.setOnClickListener { runPrediction() }
    }

    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraLauncher.launch(intent)
    }
    private fun displayDiseaseDetails(info: DiseaseInfo) {
        val result = """
        🌱 Plant Name: ${info.plantName}
        🔬 Botanical Name: ${info.botanicalName}
        
        ⚠️ Disease: ${info.diseaseDesc.diseaseName}
        🤒 Symptoms: ${info.diseaseDesc.symptoms}
        🦠 Causes: ${info.diseaseDesc.diseaseCauses}
        
        💊 Remedies:
        ${info.diseaseRemedyList.joinToString("\n\n") {
            "• ${it.title}\n   - ${it.diseaseRemedyShortDesc}\n   - ${it.diseaseRemedy}"
        }}
    """.trimIndent()

        binding.predictionResult.text = result
        binding.predictionResult.visibility = TextView.VISIBLE
    }

    data class DiseaseInfo(
        val key: String,
        val plantName: String,
        val botanicalName: String,
        val diseaseDesc: DiseaseDesc,
        val diseaseRemedyList: List<DiseaseRemedy>
    )

    data class DiseaseDesc(
        val diseaseName: String,
        val symptoms: String,
        val diseaseCauses: String
    )

    data class DiseaseRemedy(
        val title: String,
        val diseaseRemedyShortDesc: String,
        val diseaseRemedy: String
    )

    @SuppressLint("SetTextI18n")

    private fun fetchDiseaseDetails(className: String) {
        runOnUiThread {
            Log.d("PREDICTION_CLASS", "Predicted class: $className")
            binding.predictionResult.text = "Predicted Disease: $className"
            binding.predictionResult.visibility = TextView.VISIBLE
        }

        // 🔥 Static Disease Data
        val diseaseInfo = when (className) {
            "Blight" -> DiseaseInfo(
                key = "blight",
                plantName = "Rice",
                botanicalName = "Oryza sativa",
                diseaseDesc = DiseaseDesc(
                    diseaseName = "Blight",
                    symptoms = "Yellowing and browning of leaves with lesions.",
                    diseaseCauses = "Caused by fungal infection in humid conditions."
                ),
                diseaseRemedyList = listOf(
                    DiseaseRemedy(
                        title = "Chemical Treatment",
                        diseaseRemedyShortDesc = "Use Fungicides",
                        diseaseRemedy = "Apply recommended fungicides such as Mancozeb."
                    ),
                    DiseaseRemedy(
                        title = "Preventive Measures",
                        diseaseRemedyShortDesc = "Improve air circulation",
                        diseaseRemedy = "Ensure proper spacing between plants."
                    )
                )
            )

            "Brown_spots" -> DiseaseInfo(
                key = "brown_spots",
                plantName = "Rice",
                botanicalName = "Oryza sativa",
                diseaseDesc = DiseaseDesc(
                    diseaseName = "Brown Spots",
                    symptoms = "Brown lesions on leaves and stems.",
                    diseaseCauses = "Caused by bacterial or fungal infection."
                ),
                diseaseRemedyList = listOf(
                    DiseaseRemedy(
                        title = "Fungicide Spray",
                        diseaseRemedyShortDesc = "Use Copper-based Fungicides",
                        diseaseRemedy = "Apply copper sulfate to control the infection."
                    ),
                    DiseaseRemedy(
                        title = "Crop Rotation",
                        diseaseRemedyShortDesc = "Rotate crops",
                        diseaseRemedy = "Avoid continuous rice cultivation to reduce disease buildup."
                    )
                )
            )

            else -> null
        }

        // 🔥 Ensure UI Updates Properly
        runOnUiThread {
            if (diseaseInfo != null) {
                displayDiseaseDetails(diseaseInfo)  // ✅ Call the function to update UI
            } else {
                binding.predictionResult.text = "Unknown disease detected: $className"
                binding.predictionResult.visibility = TextView.VISIBLE
            }
        }
    }


    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryLauncher.launch(intent)
    }

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageBitmap = result.data?.extras?.get("data") as? Bitmap
            imageBitmap?.let { startCrop(getImageUri(it)) }
        }
    }

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageUri = result.data?.data
            imageUri?.let { startCrop(it) }
        }
    }

    private fun startCrop(imageUri: Uri) {
        cropActivityResultLauncher.launch(
            CropImageContractOptions(
                uri = imageUri,
                cropImageOptions = CropImageOptions(
                    guidelines = CropImageView.Guidelines.ON,
                    aspectRatioX = 1,
                    aspectRatioY = 1
                )
            )
        )
    }


    private fun getImageUri(bitmap: Bitmap): Uri {
        val path = MediaStore.Images.Media.insertImage(contentResolver, bitmap, "Title", null)
        return Uri.parse(path)
    }

    private fun loadModelFile(): MappedByteBuffer {
        val fileDescriptor = assets.openFd(MODEL_PATH)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        Log.d("Model_loaded", "model loaded")
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)

    }

    @SuppressLint("SetTextI18n")
    private fun runPrediction() {
        selectedBitmap?.let { bitmap ->
            val inputSize = 224
            val resizedBitmap = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)

            // Preparing input for the model
            val input = Array(1) { Array(inputSize) { Array(inputSize) { FloatArray(3) } } }
            for (x in 0 until inputSize) {
                for (y in 0 until inputSize) {
                    val pixel = resizedBitmap.getPixel(x, y)
                    input[0][x][y][0] = ((pixel shr 16 and 0xFF) / 255.0f) // Red channel
                    input[0][x][y][1] = ((pixel shr 8 and 0xFF) / 255.0f)  // Green channel
                    input[0][x][y][2] = ((pixel and 0xFF) / 255.0f)       // Blue channel
                }
            }

            // Model output
            val output = Array(1) { FloatArray(2) }
            interpreter.run(input, output)

            // Finding the predicted class
            Log.d("RESPONSE", "Response from model: $output")
            val predictedClass = output[0].indices.maxByOrNull { output[0][it] } ?: -1

            // Mapping predicted class to class name
            val classLabels = mapOf(
                0 to "Blight",
                1 to "Brown_spots"
            )
//            val classLabels = mapOf(
//                0 to "Apple___Apple_scab",
//                1 to "Apple___Black_rot",
//                2 to "Apple___Cedar_apple_rust",
//                3 to "Apple___healthy",
//                4 to "Background_without_leaves",
//                5 to "Blueberry___healthy",
//                6 to "Cherry_Powdery___mildew",
//                7 to "Cherry___healthy",
//                8 to "Corn___Cercospora_leaf_spot_Gray_leaf_spot",
//                9 to "Corn___Common_rust",
//                10 to "Corn___Northern_Leaf_Blight",
//                11 to "Corn___healthy",
//                12 to "Grape___Black_rot",
//                13 to "Grape___Esca(Black_Measles)",
//                14 to "Grape___Leaf_blight(Isariopsis_Leaf_Spot)",
//                15 to "Grape___healthy",
//                16 to "Orange___Haunglongbing(Citrus_greening)",
//                17 to "Peach___Bacterial_spot",
//                18 to "Peach___healthy",
//                19 to "Pepper,bell___Bacterial_spot",
//                20 to "Pepper,___bell_healthy",
//                21 to "Potato___Early_blight",
//                22 to "Potato___Late_blight",
//                23 to "Potato___healthy",
//                24 to "Raspberry___healthy",
//                25 to "Soybean___healthy",
//                26 to "Squash___Powdery_mildew",
//                27 to "Strawberry___Leaf_scorch",
//                28 to "Strawberry___healthy",
//                29 to "Tomato___Bacterial_spot",
//                30 to "Tomato___Early_blight",
//                31 to "Tomato___Late_blight",
//                32 to "Tomato___Leaf_Mold",
//                33 to "Tomato___Septoria_leaf_spot",
//                34 to "Tomato___Spider_mites_Two-spotted_spider_mite",
//                35 to "Tomato___Target_Spot",
//                36 to "Tomato___Tomato_Yellow_Leaf_Curl_Virus",
//                37 to "Tomato___Tomato_mosaic_virus",
//                38 to "Tomato___healthy"
//            )

            // Getting the class name from the dictionary
            val className = classLabels[predictedClass] ?: "Unknown Class"

            if (className != "Background_without_leaves") {
                fetchDiseaseDetails(className)
            } else {
                binding.predictionResult.text = "This image does not contain any plant disease."
                binding.predictionResult.visibility = TextView.VISIBLE
            }
        } ?: run {
            Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show()
        }
    }

}
