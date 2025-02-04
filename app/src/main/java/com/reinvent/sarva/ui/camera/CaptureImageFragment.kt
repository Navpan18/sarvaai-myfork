package com.reinvent.sarva.ui.camera

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.MediaActionSound
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.reinvent.sarva.R
import com.reinvent.sarva.Utils
import com.reinvent.sarva.databinding.FragmentCaptureImageBinding
import com.reinvent.sarva.input.ProcessingConstants
import dagger.hilt.android.AndroidEntryPoint
import java.io.InputStream
import javax.inject.Inject


@AndroidEntryPoint
class CaptureImageFragment : Fragment()
{

    private var _binding : FragmentCaptureImageBinding? = null
    private val binding get() = _binding !!
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraProvider: ProcessCameraProvider

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri : Uri? ->
        if (uri != null)
        {
            val inputStream : InputStream? = requireContext().contentResolver.openInputStream(uri)
            val selectedImage : Bitmap = BitmapFactory.decodeStream(inputStream)
            viewModel.setCapturedInput(selectedImage)
            viewModel.processInput()
        }
    }

    companion object
    {
        fun newInstance() = CaptureImageFragment()
    }

    private val viewModel : CaptureImageViewModel by viewModels()

    override fun onCreateView(
        inflater : LayoutInflater , container : ViewGroup? ,
        savedInstanceState : Bundle?
    ) : View
    {
        _binding = FragmentCaptureImageBinding.inflate(inflater , container , false)
        val root : View = binding.root
        setCaptureArea()
        startCamera()
        binding.captureImageButton.setOnClickListener {
               binding.progressBar.visibility = View.VISIBLE
               if(viewModel.capturedInput.value == null)
               {
                   takePhoto()
               }
               else
               {
                   viewModel.runClassifier(requireContext())
               }
        }
        binding.openFilePicker.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }

        binding.retakeImageButton.setOnClickListener{
            viewModel.clearInput()
            setAnimationForFocusArea()
            binding.captureImageButton.text = requireContext().resources.getString(R.string.ID_CLICK_PHOTO)
            startCamera()
        }
        return root
    }


    private fun setCaptureArea()
    {
        val params = binding.captureAreaImageView.layoutParams

        // Optionally, you can set specific values in pixels or dp
        val newWidthInPx : Int = utils.dpToPx(requireContext() , 224.0f) // Model input
        val newHeightInPx : Int = utils.dpToPx(requireContext() , 224.0f) // model input

        params.width = newWidthInPx
        params.height = newHeightInPx

        // Apply the new LayoutParams to the view
        binding.captureAreaImageView.layoutParams = params
    }

    private fun takePhoto()
    {
        val imageCapture = imageCapture ?: return
        imageCapture.takePicture(ContextCompat.getMainExecutor(requireContext()), object :
            ImageCapture.OnImageCapturedCallback()
        {
            override fun onCaptureSuccess(image : ImageProxy)
            {
                val sound = MediaActionSound()
                sound.play(MediaActionSound.SHUTTER_CLICK)
                var bitmap = imageToBitmap(image)
                bitmap = rotateBitmap(bitmap, image.imageInfo.rotationDegrees)
                viewModel.setCapturedInput(bitmap)
                utils.getScreenWidthInPx(requireContext())
                val widthOfImageInCaptureArea = getWidthOfImageBasedOnCaptureAreaHeight(bitmap.width)
                viewModel.processInput(
                    widthOfImageInCaptureArea - ProcessingConstants.CORRECTION_FACTOR ,
                    widthOfImageInCaptureArea - ProcessingConstants.CORRECTION_FACTOR
                )
                image.close()
            }
            })
    }

    /**
     * This method try to find out the size of image in capture area. The full image is equal to width
     * of screen. So Total width of image divided by width of screen will tell us how many pixel are
     * occupied by per px of image. Now multiple it with capture area width in pixel to determine the
     * correct size of image
     */
    private fun getWidthOfImageBasedOnCaptureAreaHeight(capturedImageWidth: Int): Int
    {
        return (capturedImageWidth / utils.getScreenWidthInPx(
            requireContext()) * binding.captureAreaImageView.width
        )
    }

    //todo<sunny> can create a bitmap util and move it there
    private fun rotateBitmap(original : Bitmap , rotationDegrees : Int) : Bitmap
    {
        // Create a matrix for rotation
        val matrix = Matrix()
        matrix.postRotate(rotationDegrees.toFloat())

        // Rotate the original bitmap and return it
        return Bitmap.createBitmap(
            original ,
            0 ,
            0 ,
            original.width ,
            original.height ,
            matrix ,
            true
        )
    }

    private fun imageToBitmap(image : ImageProxy) : Bitmap
    {
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer[bytes]
        return BitmapFactory.decodeByteArray(bytes , 0 , bytes.size)
    }

    private fun setAnimationForFocusArea()
    {
        Glide.with(this).asGif().diskCacheStrategy(DiskCacheStrategy.ALL)
            .load(R.raw.capture_area).into(binding.captureAreaImageView)
    }

    override fun onViewCreated(view : View , savedInstanceState : Bundle?)
    {
        super.onViewCreated(view , savedInstanceState)
       setAnimationForFocusArea()
        viewModel.capturedInput.observe(
            this.viewLifecycleOwner
        ) { capturedImage ->
            if(capturedImage != null)
            {
                binding.progressBar.visibility = View.GONE
                cameraProvider.unbindAll()
                binding.captureAreaImageView.setImageBitmap(capturedImage)
                binding.captureImageButton.text =
                    requireContext().resources.getString(R.string.ID_PROCEED_TEXT)
                binding.retakeImageButton.visibility = View.VISIBLE
            }
        }

        viewModel.classifierOutput.observe(this.viewLifecycleOwner
        ) { output ->
            binding.progressBar.visibility = View.GONE
            binding.modelOutputTemp.text = output
        }
    }


    private fun startCamera()
    {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.captureView.surfaceProvider)
                }
            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            imageCapture = ImageCapture.Builder()
                .build()

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture)

            } catch(exc: Exception) {
                Toast.makeText(requireContext(), "Camera binding failed", Toast.LENGTH_LONG).show()
            }

        } , ContextCompat.getMainExecutor(requireContext()))
    }

    override fun onDestroyView()
    {
        super.onDestroyView()
        cameraProvider.unbindAll()
    }

    @Inject
    lateinit var utils : Utils
}