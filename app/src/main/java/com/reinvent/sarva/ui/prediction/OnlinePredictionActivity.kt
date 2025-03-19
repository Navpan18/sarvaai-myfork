package com.reinvent.sarva.ui.prediction

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.reinvent.sarva.databinding.ActivityOnlinePredictionBinding
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import com.google.gson.annotations.SerializedName
import org.json.JSONObject

data class DiseaseInfo(
    @SerializedName("key") val key: String,
    @SerializedName("plantName") val plantName: String,
    @SerializedName("botanicalName") val botanicalName: String,
    @SerializedName("diseaseDesc") val diseaseDesc: DiseaseDesc,
    @SerializedName("diseaseRemedyList") val diseaseRemedyList: List<DiseaseRemedy>
)

data class DiseaseDesc(
    @SerializedName("diseaseName") val diseaseName: String,
    @SerializedName("symptoms") val symptoms: String,
    @SerializedName("diseaseCauses") val diseaseCauses: String
)

data class DiseaseRemedy(
    @SerializedName("title") val title: String,
    @SerializedName("diseaseRemedyShortDesc") val diseaseRemedyShortDesc: String,
    @SerializedName("diseaseRemedy") val diseaseRemedy: String
)

class OnlinePredictionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnlinePredictionBinding
    private lateinit var imagePreview: ImageView
    private lateinit var uploadButton: Button
    private lateinit var apiResponseText: TextView
    private var selectedImageUri: Uri? = null
    private var selectedCrop = ""
    private var API_ENDPOINT = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnlinePredictionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        imagePreview = binding.imagePreview
        uploadButton = binding.uploadButton
        apiResponseText = binding.predictionResult
        selectedCrop = intent.getStringExtra("crop_name") ?: ""
        Log.d("OnlinePredictionActivity", "Selected Crop: $selectedCrop")
        API_ENDPOINT="https://navpan2-predictionmodel-15mar.hf.space/predict/$selectedCrop"
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
        🔬 **Botanical Name:** ${info.botanicalName.ifEmpty { "TBD" }}
        
        ⚠️ **Disease:** ${info.diseaseDesc.diseaseName}
        🤒 **Symptoms:** ${info.diseaseDesc.symptoms.ifEmpty { "TBD" }}
        🦠 **Causes:** ${info.diseaseDesc.diseaseCauses.ifEmpty { "TBD" }}
        
        💊 **Remedies:**
        ${info.diseaseRemedyList.joinToString("\n\n") { remedy ->
                "• **${remedy.title}**\n   - *${remedy.diseaseRemedyShortDesc.ifEmpty { "TBD" }}*\n   - ${remedy.diseaseRemedy.ifEmpty { "TBD" }}"
            }}
    """.trimIndent()

            binding.predictionResult.text = result
            binding.predictionResult.visibility = View.VISIBLE  // Make sure it's visible
        }




        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", file.name, file.asRequestBody("image/*".toMediaTypeOrNull()))
            .build()

        val request = Request.Builder()
            .url(API_ENDPOINT)
            .post(requestBody)
            .build()

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
                val responseBody = response.body?.string()

                responseBody?.let {
                    val gson = Gson()

                    try {
                        val jsonObject = JSONObject(responseBody) // ✅ Convert response to JSON
                        Log.d("API_RAW_RESPONSE", it) // 🔥 Log entire response for debugging

                        val diseaseInfo = DiseaseInfo(
                            key = jsonObject.optString("plantName", "N/A"), // Using plantName as key since 'key' is missing
                            plantName = jsonObject.optString("plantName", "Unknown Plant"),
                            botanicalName = jsonObject.optString("botanicalName", "TBD"),
                            diseaseDesc = DiseaseDesc(
                                diseaseName = jsonObject.getJSONObject("diseaseDesc").optString("diseaseName", "Unknown Disease"),
                                symptoms = jsonObject.getJSONObject("diseaseDesc").optString("symptoms", "TBD"),
                                diseaseCauses = jsonObject.getJSONObject("diseaseDesc").optString("diseaseCauses", "TBD")
                            ),
                            diseaseRemedyList = jsonObject.getJSONArray("diseaseRemedyList").let { remedyArray ->
                                List(remedyArray.length()) { index ->
                                    val remedyJson = remedyArray.getJSONObject(index)
                                    DiseaseRemedy(
                                        title = remedyJson.optString("title", "Unknown Remedy"),
                                        diseaseRemedyShortDesc = remedyJson.optString("diseaseRemedyShortDesc", "TBD"),
                                        diseaseRemedy = remedyJson.optString("diseaseRemedy", "TBD")
                                    )
                                }
                            }
                        )

                        runOnUiThread {
                            displayDiseaseDetails(diseaseInfo)
                        }

                    } catch (e: Exception) {
                        runOnUiThread {
                            binding.predictionResult.text = "JSON Parsing Error: ${e.message}"
                        }
                        Log.e("API_PARSE_ERROR", "JSON Parsing failed: ${e.message}")
                    }
                }
            }




        })
    }



    companion object {
        private const val CAMERA_REQUEST = 1001
        private const val GALLERY_REQUEST = 1002}
}
