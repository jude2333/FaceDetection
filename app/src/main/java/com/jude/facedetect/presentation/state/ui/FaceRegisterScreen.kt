package com.jude.facedetect.presentation.state.ui

import android.Manifest
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.jude.facedetect.domain.model.CaptureState
import com.jude.facedetect.presentation.state.viewModel.FaceRegisterViewModel

@OptIn(ExperimentalPermissionsApi::class)
@ExperimentalGetImage
@Composable
fun FaceRegisterScreen(
    viewModel: FaceRegisterViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    val uiState by viewModel.uiState

    // handling permission request on launch
    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (cameraPermissionState.status.isGranted) {
            // Camera Preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(3f / 4f)
                    .border(2.dp, Color.Gray, RoundedCornerShape(16.dp))
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { context ->
                        PreviewView(context).apply {
                            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        }
                    },
                    update = { previewView ->
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()

                            val preview = Preview.Builder().build()
                            preview.setSurfaceProvider(previewView.surfaceProvider)

                            val imageAnalyzer = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()
                                .apply {
                                    setAnalyzer(
                                        ContextCompat.getMainExecutor(context)
                                    ) { imageProxy ->
                                        viewModel.processImage(imageProxy)
                                    }
                                }

                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_FRONT_CAMERA,
                                    preview,
                                    imageAnalyzer
                                )
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }, ContextCompat.getMainExecutor(context))
                    }
                )
            }
        } else {
            // cam permission denied
            Text(
                text = "Camera permission is required for face registration",
                style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.error)
            )
        }

        // stat and inst
        Text(
            text = when (uiState.captureState) {
                CaptureState.CAPTURE_CENTER -> "Position your face in the center"
                CaptureState.CAPTURE_LEFT -> "Turn your head to the left"
                CaptureState.CAPTURE_RIGHT -> "Turn your head to the right"
                CaptureState.COMPLETE -> "Face registration complete!"
                else -> "Preparing..."
            },
            style = MaterialTheme.typography.headlineSmall.copy(color = MaterialTheme.colorScheme.primary)

        )

        // prog Indicator based on captured faces count
        val progress = when (uiState.faces.size) {
            0 -> 0f
            1 -> 0.33f
            2 -> 0.66f
            3 -> 1f
            else -> 0f
        }

        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            color = MaterialTheme.colorScheme.secondary,
            trackColor = MaterialTheme.colorScheme.surface
        )

        // disp error message if any
        uiState.error?.let { errorMessage ->
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(CornerSize(12.dp))
                    )
                    .padding(8.dp)
            )
        }

        // disp captured faces count
        Text(
            text = "Captured faces: ${uiState.faces.size}/3",
            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface)
        )
    }
}
