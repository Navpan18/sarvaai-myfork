package com.reinvent.sarva.ui.crops

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.reinvent.sarva.databinding.ActivityAllCropsBinding
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class AllCropsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAllCropsBinding
    private lateinit var progressDialog: ProgressDialog
    private lateinit var allCropsAdapter: AllCropsAdapter  // ✅ Adapter instance

    private val cropsList = listOf(
        "Apple", "Blueberry", "Cherry", "Corn", "Grape", "Orange", "Peach",
        "Pepperbell", "Potato", "Raspberry", "Rice", "Soybean", "Squash", "Strawberry", "Tomato"
    )

    private val cropsNeedingModels = setOf(
        "Apple", "Cherry", "Corn", "Grape", "Peach",
        "Potato", "Rice", "Strawberry", "Tomato", "Pepperbell"
    )

    private val myCrops: MutableSet<String> = mutableSetOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAllCropsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadMyCrops()

        allCropsAdapter = AllCropsAdapter(cropsList, myCrops) { selectedCrop ->
            handleCropSelection(selectedCrop)
        }

        binding.cropsRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.cropsRecyclerView.adapter = allCropsAdapter

        // Initialize ProgressDialog
        progressDialog = ProgressDialog(this).apply {
            setTitle("Downloading Model")
            setMessage("Please wait while the model is being downloaded...")
            setCancelable(false)
        }

        // ✅ Navigate to My Crops
        binding.viewMyCropsButton.setOnClickListener {
            startActivity(Intent(this, MyCropsActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        loadMyCrops()
        allCropsAdapter.notifyDataSetChanged()  // ✅ Refresh UI on return from MyCropsActivity
    }

    private fun loadMyCrops() {
        val sharedPreferences = getSharedPreferences("SarvaPrefs", Context.MODE_PRIVATE)
        myCrops.clear()
        myCrops.addAll(sharedPreferences.getStringSet("myCropsList", emptySet()) ?: emptySet())
    }

    private fun handleCropSelection(cropName: String) {
        if (myCrops.contains(cropName)) {
            Toast.makeText(this, "$cropName is already in My Crops", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Add $cropName to My Crops?")
            .setMessage("Would you like to add $cropName to My Crops?")
            .setPositiveButton("Yes") { _, _ ->
                myCrops.add(cropName)
                saveMyCrops()
                binding.cropsRecyclerView.adapter?.notifyDataSetChanged() // ✅ UI updates instantly

                if (cropsNeedingModels.contains(cropName)) {
                    confirmModelDownload(cropName)
                } else {
                    showFakeDownloadMessage(cropName)
                }
            }
            .setNegativeButton("No", null)
            .show()
    }



    private fun confirmModelDownload(cropName: String) {
        AlertDialog.Builder(this)
            .setTitle("Download Offline Model?")
            .setMessage("Would you like to download the offline model for $cropName?")
            .setPositiveButton("Yes") { _, _ ->
                downloadModel(cropName)
            }
            .setNegativeButton("No") { _, _ ->
                Toast.makeText(this, "$cropName added without offline model", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun showFakeDownloadMessage(cropName: String) {
        AlertDialog.Builder(this)
            .setTitle("Success")
            .setMessage("$cropName is added to My Crops.")
            .setPositiveButton("OK") { _, _ ->
                Toast.makeText(this, "$cropName added", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun downloadModel(cropName: String) {
        val modelUrl = "https://navpan2-download-model.hf.space/download/$cropName"
        val fileName = "$cropName.tflite"
        val assetsDir = getExternalFilesDir("models")

        progressDialog.show() // Show Loader

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val filePath = saveModelFromUrl(modelUrl, assetsDir, fileName)
                withContext(Dispatchers.Main) {
                    progressDialog.dismiss() // Hide Loader
                    Toast.makeText(this@AllCropsActivity, "Model downloaded: $filePath", Toast.LENGTH_LONG).show()
                    Log.d("ModelDownload", "Model saved at: $filePath")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressDialog.dismiss() // Hide Loader on Failure
                    Toast.makeText(this@AllCropsActivity, "Failed to download model", Toast.LENGTH_SHORT).show()
                    Log.e("ModelDownload", "Error: ${e.message}")
                }
            }
        }
    }

    private fun saveModelFromUrl(url: String, assetsDir: File?, fileName: String): String {
        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()

        if (!response.isSuccessful) throw Exception("Failed to download file: ${response.code}")

        val inputStream: InputStream? = response.body?.byteStream()
        val file = File(assetsDir, fileName)

        inputStream?.use { input ->
            FileOutputStream(file).use { output ->
                input.copyTo(output)
            }
        }

        return file.absolutePath
    }

    private fun saveMyCrops() {
        val sharedPreferences = getSharedPreferences("SarvaPrefs", Context.MODE_PRIVATE)
        sharedPreferences.edit()
            .putStringSet("myCropsList", myCrops)
            .apply()
    }
}
