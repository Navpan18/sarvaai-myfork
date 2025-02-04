package com.reinvent.sarva.classifier

import android.content.Context
import android.graphics.Bitmap
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
    private val sarvaAIModel: SarvaAIModel,
    private val inputPreprocessor: InputPreprocessor,
) {
    private val classLabels = mapOf(
        0 to "Apple__Apple_scab",
        1 to "Apple_Black_rot",
        2 to "Apple_Cedar_apple_rust",
        3 to "Apple__healthy",
        4 to "Background_without_leaves",
        5 to "Blueberry__healthy",
        6 to "Cherry_Powdery_mildew",
        7 to "Cherry__healthy",
        8 to "Corn__Cercospora_leaf_spot_Gray_leaf_spot",
        9 to "Corn_Common_rust",
        10 to "Corn__Northern_Leaf_Blight",
        11 to "Corn__healthy",
        12 to "Grape_Black_rot",
        13 to "Grape_Esca(Black_Measles)",
        14 to "Grape__Leaf_blight(Isariopsis_Leaf_Spot)",
        15 to "Grape__healthy",
        16 to "Orange_Haunglongbing(Citrus_greening)",
        17 to "Peach__Bacterial_spot",
        18 to "Peach__healthy",
        19 to "Pepper,bell_Bacterial_spot",
        20 to "Pepper,_bell_healthy",
        21 to "Potato_Early_blight",
        22 to "Potato__Late_blight",
        23 to "Potato__healthy",
        24 to "Raspberry_healthy",
        25 to "Soybean_healthy",
        26 to "Squash_Powdery_mildew",
        27 to "Strawberry__Leaf_scorch",
        28 to "Strawberry__healthy",
        29 to "Tomato_Bacterial_spot",
        30 to "Tomato_Early_blight",
        31 to "Tomato__Late_blight",
        32 to "Tomato__Leaf_Mold",
        33 to "Tomato_Septoria_leaf_spot",
        34 to "Tomato__Spider_mites_Two-spotted_spider_mite",
        35 to "Tomato__Target_Spot",
        36 to "Tomato_Tomato_Yellow_Leaf_Curl_Virus",
        37 to "Tomato_Tomato_mosaic_virus",
        38 to "Tomato__healthy"
    )


    @WorkerThread
    fun processCapturedImage(inputBitmap: Bitmap): String {
        val sarvaAIModelInterpreter = sarvaAIModel.modelInterpreter.value
        return sarvaAIModelInterpreter?.let { interpreter ->
            val inputImageByteBuffer = inputPreprocessor.normaliseInputBitmap(inputBitmap)
            val inputBufferForModel = TensorBuffer.createFixedSize(
                intArrayOf(1, IMAGE_SIZE_IN_PX, IMAGE_SIZE_IN_PX, NUMBER_OF_CHANNEL_PER_PIXEL),
                DataType.FLOAT32
            )
            inputBufferForModel.loadBuffer(inputImageByteBuffer)

            val outputArray = runInference(interpreter, inputImageByteBuffer)
            val maxIndex = outputArray.indices.maxByOrNull { outputArray[it] } ?: -1
            val predictedClass = classLabels[maxIndex] ?: "Unknown"
            val outputMsg = "Predicted class: $predictedClass with probability: ${outputArray[maxIndex]}"
            outputMsg
        } ?: ""
    }

    private fun runInference(interpreter: Interpreter, inputBuffer: ByteBuffer): FloatArray {
        val outputBuffer = Array(1) { FloatArray(SarvaAIModel.MODEL_OUTPUT_SIZE) }
        interpreter.run(inputBuffer, outputBuffer)
        return outputBuffer[0]
    }
}
