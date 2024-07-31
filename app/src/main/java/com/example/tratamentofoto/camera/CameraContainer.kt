package com.example.tratamentofoto.camera

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.icu.text.DecimalFormat
import android.net.Uri
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.tratamentofoto.BuildConfig
import java.io.File
import java.io.IOException

@Composable
fun CameraContainer(
    modifier: Modifier = Modifier,
    onImageNoEntrapmentClick: () -> Unit,
    onImageWithEntrapmentClick: () -> Unit,
) {


}

fun Context.createTempPictureUri(
    provider: String = "${BuildConfig.APPLICATION_ID}.provider",
    tempFile: File?
): Uri {
    return FileProvider.getUriForFile(this, provider, tempFile!!)
}

fun Context.createTemporaryFile(
    fileName: String = "pc_${System.currentTimeMillis()}".substring(10),
    fileExtension: String = ".png"
): File? {
    val rootFolder = this.createTempImageFolder()
    val fileStorage = File(rootFolder, "${File.separator}fotos")
    if (fileStorage.exists().not()) fileStorage.mkdirs()

    return try {
        File.createTempFile(
            fileName,
            fileExtension,
            fileStorage
        )
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun Context.createTempImageFolder(): File {
    val folder = File(this.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "${File.separator}LOCALIZA")
    if (folder.exists().not()) folder.mkdirs()
    return folder
}

fun Context.checkPermissionState(permission: String): Boolean {
    return ContextCompat.checkSelfPermission(this, permission).run {
        this == PackageManager.PERMISSION_GRANTED
    }
}

fun Context.toBitmap(imageUri: Uri): Bitmap {
    return this.contentResolver.openInputStream(imageUri).use { inputStream ->
        BitmapFactory.decodeStream(inputStream)
    } ?: throw IOException("Error to decode bitmap")
}

fun Context.getFileSizeInMb(uri: Uri): String {
    val fileDescriptor = this.contentResolver.openFileDescriptor(uri, "r")
    val fileSizeInBytes = fileDescriptor?.statSize ?: 0
    fileDescriptor?.close()

    val kb = 1024
    val mb = kb * 1024
    val sizeFormat = DecimalFormat("#.##")
    return when {
        fileSizeInBytes < kb -> "$fileSizeInBytes B"
        fileSizeInBytes < mb -> "${sizeFormat.format(fileSizeInBytes / kb.toDouble())} Kb"
        else -> "${sizeFormat.format(fileSizeInBytes / mb.toDouble())} Mb"
    }
}