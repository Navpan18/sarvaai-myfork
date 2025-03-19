package com.reinvent.sarva.classifier

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.annotation.WorkerThread
import com.reinvent.sarva.input.InputPreprocessor
import com.reinvent.sarva.input.ProcessingConstants.IMAGE_SIZE_IN_PX
import com.reinvent.sarva.input.ProcessingConstants.NUMBER_OF_CHANNEL_PER_PIXEL
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.nio.ByteBuffer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DiseaseClassifier @Inject constructor(
    private val inputPreprocessor: InputPreprocessor,
) {
    private var interpreter: Interpreter? = null
    private var classLabels: List<String> = emptyList() // Dynamic class labels

    // ✅ New function to initialize the model dynamically
    fun initialize(interpreter: Interpreter, cropName: String) {
        this.interpreter = interpreter
        this.classLabels = getClassLabelsForCrop(cropName) // Load correct class labels
        Log.d("DiseaseClassifier", "Model initialized for $cropName with ${classLabels.size} classes")
    }

    @WorkerThread
    fun processCapturedImage(inputBitmap: Bitmap): String {
        val currentInterpreter = interpreter ?: return "Error: Model not initialized"
        val inputImageByteBuffer = inputPreprocessor.normaliseInputBitmap(inputBitmap)

        val outputArray = runInference(currentInterpreter, inputImageByteBuffer)
        val maxIndex = outputArray.indices.maxByOrNull { outputArray[it] } ?: -1
        val predictedClass = classLabels.getOrNull(maxIndex) ?: "Unknown"

        return "Predicted class: $predictedClass with probability: ${outputArray[maxIndex]}"
    }

    private fun runInference(interpreter: Interpreter, inputBuffer: ByteBuffer): FloatArray {
        val outputBuffer = Array(1) { FloatArray(classLabels.size) }
        interpreter.run(inputBuffer, outputBuffer)
        return outputBuffer[0]
    }

    // ✅ Function to get class labels dynamically
    private fun getClassLabelsForCrop(cropName: String): List<String> {
        val plantDiseaseDict = mapOf(
            "Rice" to listOf("Blight", "Brown_Spots"),
            "Tomato" to listOf("Tomato___Bacterial_spot", "Tomato___Early_blight", "Tomato___Late_blight"),
            "Strawberry" to listOf("Strawberry___Leaf_scorch", "Strawberry___healthy"),
            "Potato" to listOf("Potato___Early_blight", "Potato___Late_blight", "Potato___healthy"),
            "Pepperbell" to listOf("Pepper,_bell___Bacterial_spot", "Pepper,_bell___healthy"),
            "Peach" to listOf("Peach___Bacterial_spot", "Peach___healthy"),
            "Grape" to listOf("Grape___Black_rot", "Grape___Esca_(Black_Measles)", "Grape___Leaf_blight"),
            "Apple" to listOf("Apple___Apple_scab", "Apple___Black_rot", "Apple___Cedar_apple_rust"),
            "Cherry" to listOf("Cherry___Powdery_mildew", "Cherry___healthy"),
            "Corn" to listOf("Corn___Cercospora_leaf_spot Gray_leaf_spot", "Corn___Common_rust")
        )
        return plantDiseaseDict[cropName] ?: listOf("Healthy") // Default class label
    }
}
