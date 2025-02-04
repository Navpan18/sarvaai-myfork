package com.reinvent.sarva.ui.home

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import com.google.gson.Gson
import com.reinvent.sarva.databinding.ActivityOnlinePredictionBinding
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException

class OnlinePredictionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnlinePredictionBinding
    private var selectedImageUri: Uri? = null

    private val classLabels = mapOf(

        0 to "Apple___Apple_scab",
        1 to "Apple___Black_rot",
        2 to "Apple___Cedar_apple_rust",
        3 to "Apple___healthy",
        4 to "Background_without_leaves",
        5 to "Blueberry___healthy",
        6 to "Cherry_Powdery___mildew",
        7 to "Cherry___healthy",
        8 to "Corn___Cercospora_leaf_spot_Gray_leaf_spot",
        9 to "Corn___Common_rust",
        10 to "Corn___Northern_Leaf_Blight",
        11 to "Corn___healthy",
        12 to "Grape___Black_rot",
        13 to "Grape___Esca(Black_Measles)",
        14 to "Grape___Leaf_blight(Isariopsis_Leaf_Spot)",
        15 to "Grape___healthy",
        16 to "Orange___Haunglongbing(Citrus_greening)",
        17 to "Peach___Bacterial_spot",
        18 to "Peach___healthy",
        19 to "Pepper,bell___Bacterial_spot",
        20 to "Pepper,___bell_healthy",
        21 to "Potato___Early_blight",
        22 to "Potato___Late_blight",
        23 to "Potato___healthy",
        24 to "Raspberry___healthy",
        25 to "Soybean___healthy",
        26 to "Squash___Powdery_mildew",
        27 to "Strawberry___Leaf_scorch",
        28 to "Strawberry___healthy",
        29 to "Tomato___Bacterial_spot",
        30 to "Tomato___Early_blight",
        31 to "Tomato___Late_blight",
        32 to "Tomato___Leaf_Mold",
        33 to "Tomato___Septoria_leaf_spot",
        34 to "Tomato___Spider_mites_Two-spotted_spider_mite",
        35 to "Tomato___Target_Spot",
        36 to "Tomato___Tomato_Yellow_Leaf_Curl_Virus",
        37 to "Tomato___Tomato_mosaic_virus",
        38 to "Tomato___healthy"
    )

    private val cropActivityResultLauncher = registerForActivityResult(CropImageContract()) { result ->
        if (result.isSuccessful) {
            result.uriContent?.let { uri ->
                selectedImageUri = uri
                binding.imagePreview.setImageURI(uri)
                binding.uploadButton.isEnabled = true
            } ?: showToast("Error loading cropped image.")
        } else {
            showToast("Image cropping failed.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnlinePredictionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.cameraButton.setOnClickListener { openCamera() }
        binding.galleryButton.setOnClickListener { openGallery() }
        binding.uploadButton.setOnClickListener { uploadImageToApi() }
    }

    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraLauncher.launch(intent)
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryLauncher.launch(intent)
    }

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val bitmap = result.data?.extras?.get("data") as? Bitmap
            bitmap?.let { startCrop(getImageUri(it)) }
                ?: showToast("Camera capture failed.")
        }
    }

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { startCrop(it) }
                ?: showToast("Failed to load image from gallery.")
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
        val bytes = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
        val path = MediaStore.Images.Media.insertImage(contentResolver, bitmap, "CapturedImage", null)
        return Uri.parse(path ?: "")
    }

    private fun uploadImageToApi() {
        val uri = selectedImageUri
        if (uri == null) {
            showToast("No image selected.")
            return
        }

        val file = File(cacheDir, "uploaded_image.jpg").apply {
            outputStream().use { outputStream ->
                contentResolver.openInputStream(uri)?.copyTo(outputStream)
                    ?: run { showToast("Failed to process image."); return }
            }
        }

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", file.name, file.asRequestBody("image/*".toMediaTypeOrNull()))
            .build()

        val request = Request.Builder()
            .url(API_ENDPOINT)
            .post(requestBody)
            .build()

        OkHttpClient().newCall(request).enqueue(object : Callback {
            @SuppressLint("SetTextI18n")
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    binding.apiResponseText.text = "API call failed: ${e.message ?: "Unknown error"}"
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.string()?.let { handlePredictionResponse(it) }
                    ?: runOnUiThread { showToast("Empty API response.") }
            }
        })
    }

    private fun handlePredictionResponse(response: String) {
        val predictedClass = classLabels.entries.find { response.contains(it.value) }?.value ?: "Unknown Class"

        if (predictedClass == "Background_without_leaves") {
            runOnUiThread {
                binding.apiResponseText.text = "No plant disease detected."
            }
            return
        }

        val apiUrl = "https://navpan2-sarva-ai-back.hf.space/kotlinback/$predictedClass"
        fetchDiseaseDetails(apiUrl)
    }

    private fun fetchDiseaseDetails(apiUrl: String) {
        val request = Request.Builder().url(apiUrl).build()

        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    binding.apiResponseText.text = "Failed to fetch details: ${e.message ?: "Unknown error"}"
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.string()?.let {
                    val diseaseDetails = Gson().fromJson(it, DiseaseDetails::class.java)
                    runOnUiThread { displayDiseaseDetails(diseaseDetails) }
                } ?: runOnUiThread { showToast("Invalid response from server.") }
            }
        })
    }

    private fun displayDiseaseDetails(details: DiseaseDetails?) {
        if (details == null) {
            showToast("No disease details available.")
            return
        }

        val result = """
            🌿 Plant Name: ${details.plantName ?: "N/A"}
            🔍 Disease: ${details.diseaseDesc?.diseaseName ?: "Unknown"}
            🧬 Botanical Name: ${details.botanicalName ?: "N/A"}
            ❗ Symptoms: ${details.diseaseDesc?.symptoms ?: "Not provided"}
            💡 Remedies:
            ${details.diseaseRemedyList?.joinToString("\n") { "- ${it.diseaseRemedy ?: "No remedy provided"}" } ?: "No remedies found."}
        """.trimIndent()

        binding.apiResponseText.text = result
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val API_ENDPOINT = "https://navpan2-sarva-api.hf.space/predict"
    }
}

data class DiseaseDetails(
    val plantName: String?,
    val botanicalName: String?,
    val diseaseDesc: DiseaseDesc?,
    val diseaseRemedyList: List<DiseaseRemedy>?
)

data class DiseaseDesc(
    val diseaseName: String?,
    val symptoms: String?,
    val diseaseCauses: String?
)

data class DiseaseRemedy(
    val title: String?,
    val diseaseRemedyShortDesc: String?,
    val diseaseRemedy: String?
)
