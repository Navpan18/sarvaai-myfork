package com.reinvent.sarva.ui.prediction
import com.reinvent.sarva.classifier.SarvaAIModel
import com.reinvent.sarva.classifier.DiseaseClassifier
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

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
import com.canhub.cropper.CropImageView
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import okhttp3.*
import com.google.gson.Gson
import com.reinvent.sarva.databinding.ActivityOfflinePredictionBinding
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.io.IOException
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log

@AndroidEntryPoint
class OfflinePredictionActivity : AppCompatActivity() {
    private lateinit var binding: ActivityOfflinePredictionBinding
    private lateinit var interpreter: Interpreter

    @Inject
    lateinit var sarvaAIModel: SarvaAIModel

    @Inject
    lateinit var diseaseClassifier: DiseaseClassifier

    private var selectedBitmap: Bitmap? = null

//    companion object {
//        private var MODEL_PATH = "Sarva-Crop-Disease-Detector.tflite"
//    }
private var MODEL_PATH = "Sarva-Crop-Disease-Detector.tflite" // Declare at class level
private var selectedCrop = ""


//    if (selectedCrop.isNotEmpty() && plantDiseaseDict.containsKey(selectedCrop)) {
//        MODEL_PATH = "$selectedCrop.tflite" // ✅ Now it's mutable and recognized
//    }

//    if (selectedCrop.isNotEmpty() && plantDiseaseDict.containsKey(selectedCrop)) {
//        MODEL_PATH = "$selectedCrop.tflite" // Change model file dynamically
//    }

    private val plantDiseaseDict = mapOf(
        "Rice" to listOf("Blight", "Brown_Spots"),
        "Tomato" to listOf(
            "Tomato___Bacterial_spot", "Tomato___Early_blight", "Tomato___Late_blight",
            "Tomato___Leaf_Mold", "Tomato___Septoria_leaf_spot",
            "Tomato___Spider_mites Two-spotted_spider_mite",
            "Tomato___Target_Spot", "Tomato___Tomato_Yellow_Leaf_Curl_Virus",
            "Tomato___Tomato_mosaic_virus", "Tomato___healthy"
        ),
        "Strawberry" to listOf("Strawberry___Leaf_scorch", "Strawberry___healthy"),
        "Potato" to listOf("Potato___Early_blight", "Potato___Late_blight", "Potato___healthy"),
        "Pepperbell" to listOf("Pepper,_bell___Bacterial_spot", "Pepper,_bell___healthy"),
        "Peach" to listOf("Peach___Bacterial_spot", "Peach___healthy"),
        "Grape" to listOf(
            "Grape___Black_rot", "Grape___Esca_(Black_Measles)",
            "Grape___Leaf_blight_(Isariopsis_Leaf_Spot)", "Grape___healthy"
        ),
        "Apple" to listOf("Apple___Apple_scab", "Apple___Black_rot", "Apple___Cedar_apple_rust", "Apple___healthy"),
        "Cherry" to listOf("Cherry___Powdery_mildew", "Cherry___healthy"),
        "Corn" to listOf("Corn___Cercospora_leaf_spot Gray_leaf_spot", "Corn___Common_rust",
            "Corn___Northern_Leaf_Blight", "Corn___healthy"),
    )

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

        // ✅ Retrieve the selected crop
        selectedCrop = intent.getStringExtra("selectedCrop") ?: ""

        Log.d("OfflinePredictionActivity", "Selected Crop: $selectedCrop")

        // ✅ Initialize Model (Dynamically Load Correct Model)
        sarvaAIModel.initModel(applicationContext,selectedCrop)

        // ✅ Use SarvaAIModel's interpreter once it's available
        sarvaAIModel.modelInterpreter.observe(this) { loadedInterpreter ->
            if (loadedInterpreter != null) {
                interpreter = loadedInterpreter  // ✅ Properly assign interpreter
                diseaseClassifier.initialize(interpreter, selectedCrop) // ✅ Initialize classifier
                Log.d("OfflinePredictionActivity", "Model Loaded Successfully for $selectedCrop")
            } else {
                Log.e("OfflinePredictionActivity", "Failed to load model for $selectedCrop")
                Toast.makeText(this, "Error: Model not available!", Toast.LENGTH_LONG).show()
            }
        }

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
    private fun showStaticDiseaseInfo(className: String) {
        val diseaseInfo = staticDiseaseData[className]
        runOnUiThread {
            if (diseaseInfo != null) {
                displayDiseaseDetails(diseaseInfo)
            } else {
                binding.predictionResult.text = "No offline data available for: $className"
                binding.predictionResult.visibility = TextView.VISIBLE
            }
        }
    }
    private fun isInternetAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun fetchDiseaseDetails(className: String) {
        if (!isInternetAvailable()) {
            showStaticDiseaseInfo(className)
            return
        }
        val apiUrl = "https://navpan2-sarva-ai-back.hf.space/kotlinback/$className"
        val client = OkHttpClient()

        val request = Request.Builder()
            .url(apiUrl)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    binding.predictionResult.text = "API call failed: ${e.message}"
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                responseBody?.let {
                    val gson = Gson()
                    val diseaseInfo = gson.fromJson(it, DiseaseInfo::class.java)
                    runOnUiThread { displayDiseaseDetails(diseaseInfo) }
                }
            }
        })
    }
    private val staticDiseaseData = mapOf(
        "Apple___Apple_scab" to DiseaseInfo(
            key = "apple_scab",
            plantName = "Apple",
            botanicalName = "Malus domestica",
            diseaseDesc = DiseaseDesc(
                diseaseName = "Apple Scab",
                symptoms = "Dark, scaly lesions on leaves and fruit.",
                diseaseCauses = "Caused by fungus *Venturia inaequalis* in humid conditions."
            ),
            diseaseRemedyList = listOf(
                DiseaseRemedy(
                    title = "Fungicide Treatment",
                    diseaseRemedyShortDesc = "Use Mancozeb",
                    diseaseRemedy = "Apply Mancozeb-based fungicides before symptoms appear."
                ),
                DiseaseRemedy(
                    title = "Cultural Control",
                    diseaseRemedyShortDesc = "Prune Infected Parts",
                    diseaseRemedy = "Remove infected leaves and fruit to prevent spread."
                )
            )
        ),
        "Apple___Black_rot" to DiseaseInfo(
            key = "apple_black_rot",
            plantName = "Apple",
            botanicalName = "Malus domestica",
            diseaseDesc = DiseaseDesc(
                diseaseName = "Black Rot",
                symptoms = "Dark, sunken lesions on fruit and leaves.",
                diseaseCauses = "Caused by *Botryosphaeria obtusa* fungus."
            ),
            diseaseRemedyList = listOf(
                DiseaseRemedy(
                    title = "Chemical Control",
                    diseaseRemedyShortDesc = "Use Captan Fungicide",
                    diseaseRemedy = "Apply Captan or Thiophanate-methyl for control."
                ),
                DiseaseRemedy(
                    title = "Cultural Practices",
                    diseaseRemedyShortDesc = "Sanitize Orchard",
                    diseaseRemedy = "Remove infected fruit and prune diseased branches."
                )
            )
        ),
        "Apple___Cedar_apple_rust" to DiseaseInfo(
            key = "apple_cedar_apple_rust",
            plantName = "Apple",
            botanicalName = "Malus domestica",
            diseaseDesc = DiseaseDesc(
                diseaseName = "Cedar Apple Rust",
                symptoms = "Orange spots on leaves with tube-like fungal growths.",
                diseaseCauses = "Caused by fungus *Gymnosporangium juniperi-virginianae*."
            ),
            diseaseRemedyList = listOf(
                DiseaseRemedy(
                    title = "Fungicide Treatment",
                    diseaseRemedyShortDesc = "Use Myclobutanil",
                    diseaseRemedy = "Spray Myclobutanil-based fungicides during spring."
                ),
                DiseaseRemedy(
                    title = "Resistant Varieties",
                    diseaseRemedyShortDesc = "Grow Rust-Resistant Apples",
                    diseaseRemedy = "Choose resistant apple varieties like 'Enterprise' or 'Liberty'."
                )
            )
        ),
        "Apple___healthy" to DiseaseInfo(
            key = "apple_healthy",
            plantName = "Apple",
            botanicalName = "Malus domestica",
            diseaseDesc = DiseaseDesc(
                diseaseName = "Healthy",
                symptoms = "No disease symptoms observed.",
                diseaseCauses = "N/A"
            ),
            diseaseRemedyList = listOf(
                DiseaseRemedy(
                    title = "Preventive Care",
                    diseaseRemedyShortDesc = "Maintain Orchard Health",
                    diseaseRemedy = "Use proper spacing, irrigation, and pruning to keep trees healthy."
                )
            )
        ),
        "Corn___Cercospora_leaf_spot_Gray_leaf_spot" to DiseaseInfo(
            key = "corn_gray_leaf_spot",
            plantName = "Corn",
            botanicalName = "Zea mays",
            diseaseDesc = DiseaseDesc(
                diseaseName = "Gray Leaf Spot",
                symptoms = "Grayish lesions on leaves that expand over time.",
                diseaseCauses = "Caused by *Cercospora zeae-maydis* fungus."
            ),
            diseaseRemedyList = listOf(
                DiseaseRemedy(
                    title = "Fungicide Treatment",
                    diseaseRemedyShortDesc = "Use Azoxystrobin",
                    diseaseRemedy = "Spray Azoxystrobin-based fungicides at early stages."
                ),
                DiseaseRemedy(
                    title = "Crop Rotation",
                    diseaseRemedyShortDesc = "Rotate Crops",
                    diseaseRemedy = "Avoid continuous corn planting to prevent disease buildup."
                )
            )
        ),
        // 🔥 ADD ALL 39 DISEASES IN SIMILAR FORMAT 🔥
        "Tomato___healthy" to DiseaseInfo(
            key = "tomato_healthy",
            plantName = "Tomato",
            botanicalName = "Solanum lycopersicum",
            diseaseDesc = DiseaseDesc(
                diseaseName = "Healthy",
                symptoms = "No disease symptoms observed.",
                diseaseCauses = "N/A"
            ),
            diseaseRemedyList = listOf(
                DiseaseRemedy(
                    title = "Preventive Care",
                    diseaseRemedyShortDesc = "Maintain Proper Growth Conditions",
                    diseaseRemedy = "Ensure proper sunlight, watering, and nutrient supply."
                )
            )
        )
    )

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

        Log.d("OfflinePredictionActivity", "Loading Model: $MODEL_PATH")
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }


    @SuppressLint("SetTextI18n")
    private fun runPrediction() {
        if (!::interpreter.isInitialized) { // ✅ Check if interpreter is initialized
            Toast.makeText(this, "Model not loaded yet. Please wait!", Toast.LENGTH_SHORT).show()
            return
        }
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

            // Retrieve disease classes for selected crop from plantDiseaseDict
            val diseaseClasses = plantDiseaseDict[selectedCrop] ?: emptyList()

            // If only one class exists, log the prediction directly
            if (diseaseClasses.size == 1) {
                val singlePrediction = diseaseClasses[0]
                Log.d("OfflinePredictionActivity", "Predicted Class (Single): $singlePrediction")
                binding.predictionResult.text = "Prediction: $singlePrediction"
                binding.predictionResult.visibility = TextView.VISIBLE
                return
            }

            // Model output
            val output = Array(1) { FloatArray(diseaseClasses.size) }
            interpreter.run(input, output)

            // Finding the predicted class index
            val predictedIndex = output[0].indices.maxByOrNull { output[0][it] } ?: -1

            // Map prediction to the corresponding disease class
            val predictedClass = if (predictedIndex in diseaseClasses.indices) {
                diseaseClasses[predictedIndex]
            } else {
                "Unknown Class"
            }

            Log.d("OfflinePredictionActivity", "Predicted Class: $predictedClass")

            if (predictedClass != "Background_without_leaves") {
                fetchDiseaseDetails(predictedClass)
            } else {
                binding.predictionResult.text = "This image does not contain any plant disease."
                binding.predictionResult.visibility = TextView.VISIBLE
            }
        } ?: run {
            Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show()
        }
    }


}
