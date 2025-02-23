package com.reinvent.sarva.ui.home

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
import com.google.gson.JsonObject
import com.reinvent.sarva.R
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
            binding.predictionResult.visibility = View.VISIBLE  // Make sure it is visible
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
//                        val jsonObject = gson.fromJson(it, JsonObject::class.java)
                        val jsonObject = JSONObject(responseBody)
                        // 🔥 Log entire response
                        Log.d("API_RAW_RESPONSE", it)

                        // 🔥 Check if "prediction" exists
                        if (jsonObject.has("prediction")) {
                            val predictionJson = jsonObject.getJSONObject("prediction")

                            // 🔥 Log extracted "prediction" object
                            Log.d("API_PREDICTION_JSON", predictionJson.toString())

                            // ✅ Correctly parse DiseaseInfo
                            val key = if (predictionJson.has("key")) predictionJson.getString("key") else "N/A"
                            val plantName = if (predictionJson.has("plantName")) predictionJson.getString("plantName") else "N/A"
                            val botanicalName = if (predictionJson.has("botanicalName")) predictionJson.getString("botanicalName") else "N/A"
                            Log.d("API_FIELDS", "Key: $key, Plant: $plantName, Botanical: $botanicalName")

                            // 🔥 Convert JSON to DiseaseInfo
                            val diseaseInfo: DiseaseInfo? = gson.fromJson(predictionJson.toString(), DiseaseInfo::class.java)

                            Log.d("API_PREDICTION_JSON", "$diseaseInfo")
                            runOnUiThread {
                                if (diseaseInfo != null) {
                                    displayDiseaseDetails(diseaseInfo)
                                }
                            }
                        } else {
                            runOnUiThread {
                                binding.predictionResult.text = "Error: 'prediction' key not found!"
                            }
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
        private const val GALLERY_REQUEST = 1002
        private const val API_ENDPOINT = "https://navpan2-sarvaapiplantvillage.hf.space/predict"
    }
}
