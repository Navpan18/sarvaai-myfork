package com.reinvent.sarva.ui.home

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.auth.FirebaseAuth
import com.reinvent.sarva.Utils
import com.reinvent.sarva.databinding.FragmentHomeBinding
import com.reinvent.sarva.ui.camera.CaptureImageActivity
import com.reinvent.sarva.ui.cropdoctor.CropDoctorActivity
import com.reinvent.sarva.ui.crops.AllCropsActivity
import com.reinvent.sarva.ui.crops.MyCropsActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class HomeFragment : Fragment()
{

    private var _binding : FragmentHomeBinding? = null
    private val binding get() = _binding !!

    override fun onCreateView(
        inflater : LayoutInflater ,
        container : ViewGroup? ,
        savedInstanceState : Bundle?
    ) : View
    {
        val homeViewModel =
            ViewModelProvider(this)[HomeViewModel::class.java]

        _binding = FragmentHomeBinding.inflate(inflater , container , false)
        val root : View = binding.root

        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            binding.farmerName.text = "Hello, ${user.email?.split("@")?.get(0) ?: "Farmer"}"
        } else {
            binding.farmerName.text = "Hello, Farmer"
        }
//        val adapter = CommodityAndFoodAdapter(requireContext())
//        binding.commodityAndFoodList.adapter = adapter

        binding.cropDoctorButton.setOnClickListener{
            if (checkPermissions())
            {
                 val intent = Intent(context, CropDoctorActivity::class.java)
                 startActivity(intent)
            }
            else
            {
                requestPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.CAMERA
                    )
                )
            }
        }

//        binding.myCropButton.setOnClickListener{
//            Toast.makeText(requireContext().applicationContext, "My Crop Clicked",Toast.LENGTH_LONG).show()
//        }
        binding.allCropsButton.setOnClickListener {
            startActivity(Intent(requireContext(), AllCropsActivity::class.java))
        }

        // My Crops Button - Open My Crops Activity
        binding.myCropButton.setOnClickListener {
            startActivity(Intent(requireContext(), MyCropsActivity::class.java))
        }
//
//        // My Crop Doctor Button - Open Diagnosis Activity
        binding.cropDoctorButton.setOnClickListener {
            startActivity(Intent(requireContext(), CropDoctorActivity::class.java))
        }





//        binding.offlinePredictionButton.setOnClickListener {
//            val intent = Intent(requireContext(), OfflinePredictionActivity::class.java)
//            startActivity(intent)
//        }
//        binding.riceOnlinePredictionCard.setOnClickListener {
//            val intent = Intent(requireContext(), RiceOnlinePredictionActivity::class.java)
//            startActivity(intent)
//        }
//
//        binding.riceOfflinePredictionCard.setOnClickListener {
//            val intent = Intent(requireContext(), RiceOfflinePredictionActivity::class.java)
//            startActivity(intent)
//        }
//
//
//        binding.onlinePredictionButton.setOnClickListener {
//            val intent = Intent(requireContext(), OnlinePredictionActivity::class.java)
//            startActivity(intent)
//        }

        binding.todayDate.text = utils.getFormattedDate()
        return root
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions.all { it.value }) {
                Toast.makeText(requireContext().applicationContext, "Permission granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext().applicationContext, "Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }


    private fun checkPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroyView()
    {
        super.onDestroyView()
        _binding = null
    }

    @Inject
    lateinit var utils: Utils
}