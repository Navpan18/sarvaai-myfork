package com.reinvent.sarva.classifier

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.ml.modeldownloader.CustomModel
import com.google.firebase.ml.modeldownloader.CustomModelDownloadConditions
import com.google.firebase.ml.modeldownloader.DownloadType
import com.google.firebase.ml.modeldownloader.FirebaseModelDownloader
import com.reinvent.sarva.SarvaLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import okio.FileNotFoundException
import org.tensorflow.lite.Interpreter
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SarvaAIModel @Inject constructor() {

    private var _modelInterpreter = MutableLiveData<Interpreter>()
    val modelInterpreter: LiveData<Interpreter> = _modelInterpreter

    private var modelOutputSize: Int = DEFAULT_MODEL_OUTPUT_SIZE
    private var selectedModelName: String = DEFAULT_MODEL_NAME

    /**
     * Initializes the model based on the selected crop.
     */
    private var cropnamer=""
    fun initModel(context: Context, cropName: String) {
        cropnamer=cropName
        selectedModelName = "$cropName.tflite" // Set the model filename based on crop
        modelOutputSize = getModelOutputSize(cropName) // Get model output size

        MainScope().launch(Dispatchers.IO) {
            if (_modelInterpreter.value == null) {
                initModelInternal(context)
            }
        }
    }

    /**
     * Initializes the interpreter either from Firebase or from local assets.
     */
    private fun initModelInternal(context: Context) {
        val conditions = CustomModelDownloadConditions.Builder().build()
        FirebaseModelDownloader.getInstance()
            .getModel(
                selectedModelName,
                DownloadType.LOCAL_MODEL_UPDATE_IN_BACKGROUND,
                conditions
            )
            .addOnSuccessListener { model: CustomModel? ->
                if (model != null) {
                    SarvaLogger.logWithTag(message = "Model Downloaded Successfully: $selectedModelName")
                    val modelFile = model.file
                    if (modelFile != null) {
                        _modelInterpreter.postValue(Interpreter(modelFile))
                    } else {
                        initInterpreterWithLocalModel(context)
                    }
                } else {
                    initInterpreterWithLocalModel(context)
                }
            }
            .addOnFailureListener { ex ->
                SarvaLogger.logWithTag(message = "Failed to download the model: ${ex.message}")
                initInterpreterWithLocalModel(context)
            }
    }

    /**
     * Initializes the interpreter using the locally stored model file.
     */
    private fun initInterpreterWithLocalModel(context: Context) {
        val localModelFile = loadLocalModel(context, cropnamer )
        _modelInterpreter.postValue(Interpreter(localModelFile))
    }

    /**
     * Loads the locally stored TensorFlow Lite model.
     */
    private fun loadLocalModel(context: Context, cropName: String): File {
        val externalFilesDir = context.getExternalFilesDir("models") // ✅ Correct path
        val localFile = File(externalFilesDir, "$cropName.tflite") // ✅ Load specific crop model

        if (!localFile.exists()) {
            throw FileNotFoundException("Model file not found: ${localFile.absolutePath}")
        }

        return localFile
    }


    /**
     * Returns the model output size based on the selected crop.
     */
    private fun getModelOutputSize(cropName: String): Int {
        return when (cropName) {
            "Rice" -> 2
            "Tomato" -> 10
            "Strawberry" -> 2
            "Potato" -> 3
            "Pepperbell" -> 2
            "Peach" -> 2
            "Grape" -> 4
            "Apple" -> 4
            "Cherry" -> 2
            "Corn" -> 6
            else -> DEFAULT_MODEL_OUTPUT_SIZE // Default size if model not found
        }
    }

    companion object {
        private const val DEFAULT_MODEL_NAME = "Sarva-Crop-Disease-Detector.tflite"
        private const val DEFAULT_MODEL_OUTPUT_SIZE = 39
    }

    /**
     * Returns the current model output size.
     */
    fun getCurrentModelOutputSize(): Int = modelOutputSize
}
