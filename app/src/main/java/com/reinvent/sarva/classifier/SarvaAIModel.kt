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
import org.tensorflow.lite.Interpreter
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SarvaAIModel @Inject constructor()
{
    fun initModel(context : Context)
    {
        MainScope().launch(Dispatchers.IO) {
            if(_modelInterpreter.value == null)
            {
                initModelInternal(context)
            }
        }
    }

    private fun initModelInternal(context : Context)
    {
        val conditions = CustomModelDownloadConditions.Builder()
            .build()
        FirebaseModelDownloader.getInstance()
            .getModel(
                MODEL_NAME,
                DownloadType.LOCAL_MODEL_UPDATE_IN_BACKGROUND,
                conditions)
            .addOnSuccessListener { model: CustomModel? ->
                 if(model != null)
                 {
                     SarvaLogger.logWithTag( message = "Model Downloaded Successfully")
                     val modelFile = model.file
                     if (modelFile != null)
                     {
                         _modelInterpreter.postValue(Interpreter(modelFile))
                     }
                     else
                     {
                         initInterpreterWithLocalModel(context)
                     }
                 }
                 else
                 {
                     initInterpreterWithLocalModel(context)
                 }
            }.addOnFailureListener { ex ->
                SarvaLogger.logWithTag( message = "Failed to download the model: ${ex.message}")
                initInterpreterWithLocalModel(context)
            }
    }

    private fun initInterpreterWithLocalModel(context : Context)
    {
        val localModelFile = loadLocalModel(context)
        _modelInterpreter.postValue(Interpreter(localModelFile))
    }

    private fun loadLocalModel(context: Context): File
    {
        val localFile = File(context.filesDir, LOCAL_MODEL_FILE_PATH)
        localFile.parentFile?.mkdirs()
        if (!localFile.exists()) {
            context.assets.open(LOCAL_MODEL_FILE_PATH).use { inputStream ->
                localFile.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        }
        return localFile
    }

    private var _modelInterpreter = MutableLiveData<Interpreter>()
    val modelInterpreter: LiveData<Interpreter> = _modelInterpreter

    companion object
    {
        const val MODEL_NAME = "Sarva-Crop-Disease-Detector"
        const val LOCAL_MODEL_FILE_PATH = "models/Sarva-Crop-Disease-Detector.tflite"
        const val MODEL_OUTPUT_SIZE = 39
    }
}