package com.example.tratamentofoto

import android.Manifest
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.tratamentofoto.camera.checkPermissionState
import com.example.tratamentofoto.camera.createTempPictureUri
import com.example.tratamentofoto.camera.createTemporaryFile
import com.example.tratamentofoto.camera.getFileSizeInMb
import com.example.tratamentofoto.camera.toBitmap
import com.example.tratamentofoto.ui.theme.TratamentoFotoTheme

data class UiState(
    val imageUriNoEntrapment: Uri = Uri.EMPTY,
    val imageUriWithEntrapment: Uri = Uri.EMPTY,
    val isImageWithEntrapment: Boolean = true
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val bitmapUtils = BitmapUtils(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TratamentoFotoTheme {
                var uiState by remember {
                    mutableStateOf(UiState())
                }

                var isSavingImage by remember {
                    mutableStateOf(false)
                }
                val context = LocalContext.current
                var tempPhotoUri by remember { mutableStateOf(value = Uri.EMPTY) }
                var tempFileWithEntrapment by remember {
                    mutableStateOf(context.createTemporaryFile())
                }
                var tempFileNoEntrapment by remember {
                    mutableStateOf(context.createTemporaryFile())
                }
                var bitmap1WithEntrapment by remember {
                    mutableStateOf<Bitmap?>(null)
                }
                var bitmap2NoEntrapment by remember {
                    mutableStateOf<Bitmap?>(null)
                }

                val cameraLauncher =
                    rememberLauncherForActivityResult(
                        ActivityResultContracts.TakePicture()
                    ) { isSaved ->
                        if (isSaved) {
                            uiState = if (uiState.isImageWithEntrapment)
                                uiState.copy(imageUriWithEntrapment = Uri.fromFile(tempFileWithEntrapment))
                            else uiState.copy(imageUriNoEntrapment = tempPhotoUri)
                        }
                    }

                val cameraPermission = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { result ->
                    if (result) {
                        tempPhotoUri =
                            context.createTempPictureUri(
                                tempFile =
                                if (uiState.isImageWithEntrapment) tempFileWithEntrapment
                                else tempFileNoEntrapment
                            )
                        cameraLauncher.launch(tempPhotoUri)
                    }
                }

                LaunchedEffect(key1 = isSavingImage) {
                    if (isSavingImage) {
                        val result1 = bitmapUtils.saveBitmapAsImage(bitmap1WithEntrapment!!, tempFileWithEntrapment!!, 50)
                        val result2 = bitmapUtils.saveBitmapAsImage(bitmap2NoEntrapment!!, tempFileNoEntrapment!!, 100)
                        isSavingImage = !result1 && !result2
                        uiState = UiState(
                            isImageWithEntrapment = false,
                            imageUriWithEntrapment = uiState.imageUriWithEntrapment,
                            imageUriNoEntrapment = uiState.imageUriNoEntrapment
                        )
                    }
                }





                if (isSavingImage) {
                    AppProgressDialog()
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        modifier = Modifier.padding(innerPadding),
                        uiState = uiState,
                        onImageNoEntrapmentClick = {
                            uiState = uiState.copy(isImageWithEntrapment = false)
                            if (context.checkPermissionState(Manifest.permission.CAMERA)) {
                                tempPhotoUri = context.createTempPictureUri(
                                    tempFile = tempFileNoEntrapment
                                )
                                cameraLauncher.launch(tempPhotoUri)
                            } else cameraPermission.launch(Manifest.permission.CAMERA)
                        },
                        onImageWithEntrapmentClick = {
                            uiState = uiState.copy(isImageWithEntrapment = true)
                            if (context.checkPermissionState(Manifest.permission.CAMERA)) {
                                tempPhotoUri = context.createTempPictureUri(
                                    tempFile = tempFileWithEntrapment
                                )
                                cameraLauncher.launch(tempPhotoUri)
                            } else cameraPermission.launch(Manifest.permission.CAMERA)
                        },
                        onSaveBitmapClick = { bitmap1, bitmap2 ->
                            bitmap1WithEntrapment = bitmap1
                            bitmap2NoEntrapment = bitmap2
                            isSavingImage = true
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(
    modifier: Modifier = Modifier,
    uiState: UiState,
    onImageNoEntrapmentClick: () -> Unit,
    onImageWithEntrapmentClick: () -> Unit,
    onSaveBitmapClick: (Bitmap, Bitmap) -> Unit
) {
    val context = LocalContext.current
    val bitmapUtils = BitmapUtils(context)

    var bitmapWithNoEntrapment by remember {
        mutableStateOf<Bitmap?>(null)
    }
    var bitmapWithEntrapment by remember {
        mutableStateOf<Bitmap?>(null)
    }

    LaunchedEffect(key1 = uiState.imageUriWithEntrapment) {
        if (uiState.imageUriWithEntrapment != Uri.EMPTY)
            bitmapWithEntrapment = bitmapUtils.decodeBitmap(uiState.imageUriWithEntrapment)
    }

    LaunchedEffect(key1 = uiState.imageUriNoEntrapment) {
        if (uiState.imageUriNoEntrapment != Uri.EMPTY)
            bitmapWithNoEntrapment = context.toBitmap(uiState.imageUriNoEntrapment)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceAround,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(onClick = onImageNoEntrapmentClick) {
                Text(text = "Imagem sem tratamento")
            }
            bitmapWithNoEntrapment?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = null
                )
                Text(text = context.getFileSizeInMb(uiState.imageUriNoEntrapment))
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(onClick = onImageWithEntrapmentClick) {
                Text(text = "Imagem com tratamento")
            }
            bitmapWithEntrapment?.let {
                if (uiState.imageUriWithEntrapment != Uri.EMPTY) {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = null
                    )
                    Text(text = context.getFileSizeInMb(uiState.imageUriWithEntrapment))
                }
            }
        }
        Button(onClick = {
            onSaveBitmapClick(bitmapWithEntrapment!!, bitmapWithNoEntrapment!!)
        }) {
            Text(text = "Save images")
        }
    }
}



@Composable
fun AppProgressDialog(
    modifier: Modifier = Modifier,
    message: String = "Salvando imagem...",
) {
    Dialog(onDismissRequest = {}) {
        Card(
            modifier = modifier,
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = message,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    TratamentoFotoTheme {
        Greeting(
            uiState = UiState(),
            onImageNoEntrapmentClick = {},
            onImageWithEntrapmentClick = {},
            onSaveBitmapClick = { _, _ ->}
        )
    }
}