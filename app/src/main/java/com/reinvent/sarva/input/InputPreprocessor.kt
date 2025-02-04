package com.reinvent.sarva.input

import android.graphics.Bitmap
import android.graphics.Color
import com.reinvent.sarva.input.ProcessingConstants.NUMBER_OF_BYTE_PER_CHANNEL
import com.reinvent.sarva.input.ProcessingConstants.NUMBER_OF_CHANNEL_PER_PIXEL
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InputPreprocessor @Inject constructor()
{
    /***
     * This function assume that the normalise input is a float byte buffer where float size is 32 bit
     * and the input bitmap has 3 channel.
     */
    fun normaliseInputBitmap(
        inputImage : Bitmap ,
        imageWidth : Int = ProcessingConstants.IMAGE_SIZE_IN_PX ,
        imageHeight : Int = ProcessingConstants.IMAGE_SIZE_IN_PX
    ) : ByteBuffer
    {
        val bitmap = Bitmap.createScaledBitmap(inputImage , imageWidth , imageHeight , true)
        val processedInputImage =
            ByteBuffer.allocateDirect(imageWidth * imageHeight * NUMBER_OF_CHANNEL_PER_PIXEL * NUMBER_OF_BYTE_PER_CHANNEL)
                .order(
                    ByteOrder.nativeOrder()
                )
        for (y in 0 until imageHeight)
        {
            for (x in 0 until imageWidth)
            {
                val px = bitmap.getPixel(x , y)

                // Get channel values from the pixel value.
                val r = Color.red(px)
                val g = Color.green(px)
                val b = Color.blue(px)

                // Normalize channel values to [-1.0, 1.0]. This requirement depends on the model.
                // For example, some models might require values to be normalized to the range
                // [0.0, 1.0] instead.
                val rf = (r - 127) / 255f
                val gf = (g - 127) / 255f
                val bf = (b - 127) / 255f

                processedInputImage.putFloat(rf)
                processedInputImage.putFloat(gf)
                processedInputImage.putFloat(bf)
            }
        }
        return processedInputImage
    }

    fun cropCenter(original: Bitmap, newWidth : Int, newHeight : Int): Bitmap {

        val width = original.width
        val height = original.height

        val cropX = maxOf(0, (width - newWidth) / 2)
        val cropY = maxOf(0, (height - newHeight) / 2)

        val cropWidth = minOf(newWidth, width)
        val cropHeight = minOf(newHeight, height)

        return Bitmap.createBitmap(original, cropX, cropY, cropWidth, cropHeight)
    }

    fun resizeBitmap(originalBitmap: Bitmap , newWidth: Int , newHeight: Int): Bitmap {
        return Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true)
    }
}