package com.reinvent.sarva.ui.crops

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.reinvent.sarva.databinding.ActivityMyCropsBinding
import java.io.File

class MyCropsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMyCropsBinding
    private lateinit var myCropsAdapter: MyCropsAdapter
    private val myCrops: MutableList<String> = mutableListOf()

    private val cropsNeedingModels = setOf(
        "Apple", "Cherry", "Corn", "Grape", "Peach",
        "Potato", "Rice", "Strawberry", "Tomato", "Pepperbell"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyCropsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.cropsRecyclerView.layoutManager = LinearLayoutManager(this)

        // ✅ Load saved crops before initializing adapter
        loadMyCrops()

        // ✅ Initialize adapter with the removal function
        myCropsAdapter = MyCropsAdapter(myCrops, ::showRemoveConfirmationDialog)
        binding.cropsRecyclerView.adapter = myCropsAdapter

        updateUI()
        binding.backButton.setOnClickListener {
            finish()  // ✅ This should correctly close the activity and return to the previous screen
        }

    }

    private fun loadMyCrops() {
        val sharedPreferences = getSharedPreferences("SarvaPrefs", Context.MODE_PRIVATE)
        myCrops.clear()
        myCrops.addAll(sharedPreferences.getStringSet("myCropsList", emptySet()) ?: emptySet())
        Log.d("MyCropsActivity", "Loaded crops list: $myCrops")

        updateUI()
    }

    private fun showRemoveConfirmationDialog(cropName: String) {
        AlertDialog.Builder(this)
            .setTitle("Remove $cropName?")
            .setMessage("Are you sure you want to remove $cropName from My Crops?")
            .setPositiveButton("Yes") { _, _ ->
                removeCrop(cropName)
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun removeCrop(cropName: String) {
        myCrops.remove(cropName)
        saveMyCrops()
        myCropsAdapter.notifyDataSetChanged()

        if (cropsNeedingModels.contains(cropName)) {
            deleteModelFile(cropName)
        }

        Toast.makeText(this, "$cropName removed from My Crops", Toast.LENGTH_SHORT).show()

        updateUI()
    }

    private fun saveMyCrops() {
        val sharedPreferences = getSharedPreferences("SarvaPrefs", Context.MODE_PRIVATE)
        sharedPreferences.edit()
            .putStringSet("myCropsList", myCrops.toSet())
            .apply()
    }

    private fun deleteModelFile(cropName: String) {
        val assetsDir = getExternalFilesDir("models")
        val modelFile = File(assetsDir, "$cropName.tflite")

        if (modelFile.exists()) {
            if (modelFile.delete()) {
                Log.d("ModelDelete", "Deleted model: ${modelFile.absolutePath}")
            } else {
                Log.e("ModelDelete", "Failed to delete model: ${modelFile.absolutePath}")
            }
        }
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
