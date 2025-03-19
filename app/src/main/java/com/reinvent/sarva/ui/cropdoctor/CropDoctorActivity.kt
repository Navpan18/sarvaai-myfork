package com.reinvent.sarva.ui.cropdoctor

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.reinvent.sarva.databinding.ActivityCropDoctorBinding
import com.reinvent.sarva.ui.prediction.OfflinePredictionActivity
import com.reinvent.sarva.ui.prediction.OnlinePredictionActivity
import java.io.File

class CropDoctorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCropDoctorBinding
    private lateinit var cropDoctorAdapter: CropDoctorAdapter
    private val myCrops: MutableList<String> = mutableListOf()
    private val cropsNeedingModels = setOf(
        "Apple", "Cherry", "Corn", "Grape", "Peach",
        "Potato", "Rice", "Strawberry", "Tomato", "Pepperbell"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCropDoctorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadMyCrops()

        binding.cropsRecyclerView.layoutManager = LinearLayoutManager(this)
        cropDoctorAdapter = CropDoctorAdapter(myCrops) { selectedCrop ->
            handleCropSelection(selectedCrop)
        }
        binding.cropsRecyclerView.adapter = cropDoctorAdapter

        // Back button functionality
        binding.backButton.setOnClickListener {
            finish()
        }

        updateUI()
    }

    private fun loadMyCrops() {
        val sharedPreferences = getSharedPreferences("SarvaPrefs", Context.MODE_PRIVATE)
        myCrops.clear()
        myCrops.addAll(sharedPreferences.getStringSet("myCropsList", emptySet()) ?: emptySet())
        updateUI()
    }

    private fun handleCropSelection(cropName: String) {
        if (isInternetAvailable()) {
            // Redirect to Online Prediction
            val intent = Intent(this, OnlinePredictionActivity::class.java)
            intent.putExtra("crop_name", cropName)
            startActivity(intent)
        } else {
            // Check if offline model exists
            if (cropsNeedingModels.contains(cropName) && isModelDownloaded(cropName)) {
                val intent = Intent(this, OfflinePredictionActivity::class.java)
                intent.putExtra("selectedCrop", cropName)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Offline model not available. Please go online to download it.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun isModelDownloaded(cropName: String): Boolean {
        val modelFile = File(getExternalFilesDir("models"), "$cropName.tflite")
        return modelFile.exists()
    }

    private fun isInternetAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
        return activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
    }

    private fun updateUI() {
        if (myCrops.isEmpty()) {
            binding.cropsRecyclerView.visibility = View.GONE
            binding.emptyMessage.visibility = View.VISIBLE
        } else {
            binding.cropsRecyclerView.visibility = View.VISIBLE
            binding.emptyMessage.visibility = View.GONE
        }
    }
}
