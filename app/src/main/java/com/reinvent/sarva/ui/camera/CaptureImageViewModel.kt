package com.reinvent.sarva.ui.camera

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reinvent.sarva.classifier.DiseaseClassifier
import com.reinvent.sarva.input.InputPreprocessor
import com.reinvent.sarva.input.ProcessingConstants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CaptureImageViewModel @Inject constructor(
    private var inputPreprocessor: InputPreprocessor,
    private var diseaseClassifier : DiseaseClassifier,
) : ViewModel()
{
    fun setCapturedInput(image: Bitmap)
    {
        captureInputImage = image
    }

    fun processInput(
        width : Int = ProcessingConstants.THRESH_HOLD_WIDTH_FOR_CROP ,
        height : Int = ProcessingConstants.THRESH_HOLD_WIDTH_FOR_CROP
    )
    {
        viewModelScope.launch(Dispatchers.IO) {
            val centerCropImage =
                if (captureInputImage.width > ProcessingConstants.THRESH_HOLD_WIDTH_FOR_CROP)
                {
                    inputPreprocessor.cropCenter(
                        captureInputImage ,
                        width ,
                        height
                    )
                }
                else
                {
                    captureInputImage
                }

            val processedBitmapForModel = inputPreprocessor.resizeBitmap(
                centerCropImage ,
                ProcessingConstants.IMAGE_SIZE_IN_PX ,
                ProcessingConstants.IMAGE_SIZE_IN_PX
            )
            _processedInputBitmap.postValue(processedBitmapForModel)
        }
    }

    fun runClassifier(context : Context)
    {
        viewModelScope.launch(Dispatchers.IO) {
            _processedInputBitmap.value?.let {
               val output = diseaseClassifier.processCapturedImage(it)
               _classifierOutput.postValue(output)
            }
        }
    }

    fun clearInput()
    {
        _processedInputBitmap.value = null
    }

    private lateinit var captureInputImage: Bitmap
    private val _processedInputBitmap: MutableLiveData<Bitmap?> = MutableLiveData()
    val capturedInput: LiveData<Bitmap?> = _processedInputBitmap
    private val _classifierOutput = MutableLiveData<String>()
    val classifierOutput: LiveData<String> = _classifierOutput
}