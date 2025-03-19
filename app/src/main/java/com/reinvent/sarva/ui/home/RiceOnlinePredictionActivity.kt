package com.reinvent.sarva.ui.home

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.reinvent.sarva.databinding.ActivityOnlinePredictionBinding
import com.reinvent.sarva.ui.prediction.DiseaseDesc
import com.reinvent.sarva.ui.prediction.DiseaseInfo
import com.reinvent.sarva.ui.prediction.DiseaseRemedy
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException

class RiceOnlinePredictionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnlinePredictionBinding
    private lateinit var imagePreview: ImageView
    private lateinit var uploadButton: Button
    private lateinit var apiResponseText: TextView
    private var selectedImageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnlinePredictionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        imagePreview = binding.imagePreview
        uploadButton = binding.uploadButton
        apiResponseText = binding.predictionResult

        binding.cameraButton.setOnClickListener { openCamera() }
        binding.galleryButton.setOnClickListener { openGallery() }
        binding.uploadButton.setOnClickListener { uploadImageToApi() }
    }

    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(intent, CAMERA_REQUEST)
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, GALLERY_REQUEST)
    }
    private fun saveBitmapToFile(bitmap: Bitmap): Uri {
        val file = File(cacheDir, "captured_image.jpg")
        file.createNewFile()

        val bos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos)
        val bitmapData = bos.toByteArray()

        file.outputStream().use {
            it.write(bitmapData)
            it.flush()
        }

        return Uri.fromFile(file)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                CAMERA_REQUEST -> {
                    val bitmap = data?.extras?.get("data") as Bitmap
                    selectedImageUri = saveBitmapToFile(bitmap)
                    imagePreview.setImageURI(selectedImageUri)
                }
                GALLERY_REQUEST -> {
                    selectedImageUri = data?.data
                    imagePreview.setImageURI(selectedImageUri)
                }
            }
            uploadButton.isEnabled = selectedImageUri != null
        }
    }


    private fun getImageUriFromBitmap(bitmap: Bitmap): Uri {
        val bytes = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
        val path = MediaStore.Images.Media.insertImage(contentResolver, bitmap, "CapturedImage", null)
        return Uri.parse(path)
    }
    private fun getFileFromUri(uri: Uri): File? {
        try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val file = File(cacheDir, "uploaded_image.jpg")
            file.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
            }
            return file
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }


    private fun uploadImageToApi() {
        if (selectedImageUri == null) {
            Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show()
            return
        }

        val file = getFileFromUri(selectedImageUri!!)
        if (file == null || !file.exists()) {
            Toast.makeText(this, "Failed to process image!", Toast.LENGTH_SHORT).show()
            return
        }
        fun displayDiseaseDetails(info: DiseaseInfo) {
            val result = """
        🌱 **Plant Name:** ${info.plantName}
        🔬 **Botanical Name:** ${info.botanicalName}
        
        ⚠️ **Disease:** ${info.diseaseDesc.diseaseName}
        🤒 **Symptoms:** ${info.diseaseDesc.symptoms}
        🦠 **Causes:** ${info.diseaseDesc.diseaseCauses}
        
        💊 **Remedies:**
        ${info.diseaseRemedyList.joinToString("\n\n") { remedy ->
                "• **${remedy.title}**\n   - *${remedy.diseaseRemedyShortDesc}*\n   - ${remedy.diseaseRemedy}"
            }}
    """.trimIndent()

            binding.predictionResult.text = result
            binding.predictionResult.visibility = TextView.VISIBLE
        }

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", file.name, file.asRequestBody("image/*".toMediaTypeOrNull()))
            .build()

        val request = Request.Builder()
            .url(API_ENDPOINT)
            .post(requestBody)
            .build()
        fun getStaticDiseaseInfo(prediction: String): DiseaseInfo {
            return when (prediction.lowercase()) {
                "blight" -> DiseaseInfo(
                    key = "Blight",
                    plantName = "Rice",
                    botanicalName = "TBD",
                    diseaseDesc = DiseaseDesc(
                        diseaseName = "Blight",
                        symptoms = "Yellowing and wilting of leaves.",
                        diseaseCauses = "Caused by a bacterial infection."
                    ),
                    diseaseRemedyList = listOf(
                        DiseaseRemedy(
                            title = "Use Resistant Varieties",
                            diseaseRemedyShortDesc = "Choose blight-resistant rice strains.",
                            diseaseRemedy = "Consult local agricultural services for the best variety."
                        ),
                        DiseaseRemedy(
                            title = "Proper Water Management",
                            diseaseRemedyShortDesc = "Avoid excessive irrigation.",
                            diseaseRemedy = "Ensure proper drainage to prevent disease spread."
                        )
                    )
                )

                "brown_spots" -> DiseaseInfo(
                    key = "Brown Spots",
                    plantName = "Rice",
                    botanicalName = "TBD",
                    diseaseDesc = DiseaseDesc(
                        diseaseName = "Brown Spots",
                        symptoms = "Small, brown, oval spots on leaves.",
                        diseaseCauses = "Fungal infection due to excessive moisture."
                    ),
                    diseaseRemedyList = listOf(
                        DiseaseRemedy(
                            title = "Use Fungicides",
                            diseaseRemedyShortDesc = "Apply recommended fungicides.",
                            diseaseRemedy = "Spray fungicide early when symptoms appear."
                        ),
                        DiseaseRemedy(
                            title = "Crop Rotation",
                            diseaseRemedyShortDesc = "Avoid planting rice continuously in the same field.",
                            diseaseRemedy = "Use crop rotation to reduce fungal buildup."
                        )
                    )
                )

                else -> DiseaseInfo(
                    key = "Unknown",
                    plantName = "Unknown",
                    botanicalName = "N/A",
                    diseaseDesc = DiseaseDesc(
                        diseaseName = "Unknown Disease",
                        symptoms = "No known symptoms.",
                        diseaseCauses = "Unknown cause."
                    ),
                    diseaseRemedyList = listOf(
                        DiseaseRemedy(
                            title = "Consult an Expert",
                            diseaseRemedyShortDesc = "Seek help from an agronomist.",
                            diseaseRemedy = "Take a sample to a local agricultural center for testing."
                        )
                    )
                )
            }
        }

        val client = OkHttpClient()
        client.newCall(request).enqueue(object : Callback {
            @SuppressLint("SetTextI18n")
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    apiResponseText.text = "API call failed: ${e.message}"
                    apiResponseText.visibility = TextView.VISIBLE
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseText = response.body?.string() ?: "No response"
                Log.d("API_RESPONSE", "Raw response: $responseText")

                runOnUiThread {
                    try {
                        val jsonObject = JSONObject(responseText)
                        val prediction = jsonObject.optString("prediction", "Unknown")
                        Log.d("PARSED_PREDICTION", "Prediction: $prediction")

                        val diseaseInfo = getStaticDiseaseInfo(prediction)
                        displayDiseaseDetails(diseaseInfo)
                    } catch (e: Exception) {
                        Log.e("PARSE_ERROR", "Error parsing JSON: ${e.message}")
                        binding.predictionResult.text = "Error parsing response"
                    }
                }
            }

        })
    }



    companion object {
        private const val CAMERA_REQUEST = 1001
        private const val GALLERY_REQUEST = 1002
        private const val API_ENDPOINT = "https://navpan2-sarva-api.hf.space/predict"
    }
}
