package com.example.tratamentofoto

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.util.Base64
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Files

class BitmapUtils(private val context: Context) {

    /**
     * Decodes a bitmap from the given URI, applies scaling, and handles rotation based on EXIF data.
     *
     * @param imageUri The URI of the image to be decoded.
     * @return The decoded and possibly rotated bitmap.
     * @throws IOException if there is an error decoding the bitmap.
     * @throws Exception if there is any other error.
     */
    fun decodeBitmap(imageUri: Uri): Bitmap {
        return try {
            // Get the image path from the URI
            val imagePath = imageUri.path ?: throw Exception("Image path not found")
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true // Set to true to read image dimensions only
            }

            // Open input stream and decode the image dimensions
            var inputStream = context.contentResolver.openInputStream(imageUri)
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()

            // Calculate inSampleSize for image scaling
            options.inSampleSize = calculateInSampleSize(options)

            // Set inJustDecodeBounds to false to decode the actual bitmap
            options.inJustDecodeBounds = false
            options.inPreferredConfig = Bitmap.Config.ARGB_8888
            inputStream = context.contentResolver.openInputStream(imageUri)
            val bitmap = BitmapFactory.decodeStream(inputStream, null, options)
                ?: throw IOException("Error decode bitmap")

            inputStream?.close()

            // Get the EXIF data to check image orientation
            val exifInterface = ExifInterface(imagePath)
            val orientation = exifInterface.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED
            )

            // Rotate the bitmap based on the orientation
            rotateBitmap(bitmap, orientation)
        } catch (ioException: IOException) {
            ioException.printStackTrace()
            throw IOException("Error while decoding bitmap")
        } catch (exception: Exception) {
            exception.printStackTrace()
            throw Exception("Some Error occurred")
        }
    }

    /**
     * Saves a bitmap as an image file.
     *
     * @param bitmap The bitmap to be saved.
     * @param file The file where the bitmap will be saved.
     * @param quality The quality of the saved image (0-100).
     * @return True if the image was saved successfully, false otherwise.
     */
    suspend fun saveBitmapAsImage(bitmap: Bitmap, file: File, quality: Int = 50): Boolean =
        withContext(Dispatchers.IO) {
            return@withContext try {
                // Open the output stream to the file
                val outputStream = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    BufferedOutputStream(Files.newOutputStream(file.toPath()))
                else BufferedOutputStream(FileOutputStream(file))

                // Compress and save the bitmap to the output stream
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                    bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, quality, outputStream)
                else bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
                outputStream.close()
                true
            } catch (exception: Exception) {
                exception.printStackTrace()
                false
            }
        }

    /**
     * Converts an image to a Base64 encoded string.
     *
     * @param imagePath The path of the image to be converted.
     * @return The Base64 encoded string of the image.
     */
    fun convertImageToBase64(imagePath: String): String {
        // Decode the image file to a bitmap
        val bitmap = BitmapFactory.decodeFile(imagePath)
        val byteArrayOutPutStream = ByteArrayOutputStream()

        // Convert the bitmap to byte array
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutPutStream)
        val bytes = byteArrayOutPutStream.toByteArray()

        // Encode the byte array to Base64 string
        return Base64.encodeToString(bytes, Base64.DEFAULT).replace("\n", "")
    }

    /**
     * Calculates the inSampleSize value for image scaling.
     *
     * @param options The BitmapFactory.Options object containing the original dimensions of the image.
     * @return The calculated inSampleSize value.
     */
    private fun calculateInSampleSize(options: BitmapFactory.Options): Int {
        val maxHeight = context.resources.displayMetrics.heightPixels
        val maxWidth = context.resources.displayMetrics.widthPixels

        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        // Check if the image dimensions are larger than the screen dimensions
        if (height > maxHeight || width > maxWidth) {
            val heightRatio = Math.round((height.toFloat() / maxHeight.toFloat()))
            val widthRatio = Math.round((width.toFloat() / maxWidth.toFloat()))
            inSampleSize = heightRatio.coerceAtMost(widthRatio)

            // Calculate the total pixels and required pixels cap
            val totalPixels = width * height
            val totalReqPixelsCap = maxWidth * maxHeight * 2

            // Adjust inSampleSize to fit within the required pixels cap
            while (totalPixels / (inSampleSize * inSampleSize) > totalReqPixelsCap) {
                inSampleSize++
            }
        }
        return inSampleSize
    }

    /**
     * Rotates a bitmap based on the given orientation.
     *
     * @param bitmap The bitmap to be rotated.
     * @param orientation The orientation value from the EXIF data.
     * @return The rotated bitmap.
     */
    private fun rotateBitmap(bitmap: Bitmap, orientation: Int): Bitmap {
        return when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> rotateImage(bitmap, 90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> rotateImage(bitmap, 180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> rotateImage(bitmap, 270f)
            ExifInterface.ORIENTATION_NORMAL -> bitmap
            else -> bitmap
        }
    }

    /**
     * Rotates a bitmap by the specified angle.
     *
     * @param bitmap The bitmap to be rotated.
     * @param angle The angle by which to rotate the bitmap.
     * @return The rotated bitmap.
     */
    private fun rotateImage(bitmap: Bitmap, angle: Float): Bitmap {
        // Create a new matrix and set the rotation angle
        val matrix = Matrix()
        matrix.postRotate(angle)

        // Create a new bitmap with the applied rotation
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
