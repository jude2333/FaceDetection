package com.jude.facedetect

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.camera.core.ExperimentalGetImage
import androidx.compose.runtime.DisposableEffect
import com.jude.facedetect.presentation.state.ui.FaceRegisterScreen
import com.jude.facedetect.presentation.state.viewModel.FaceRegisterViewModel
import com.jude.facedetect.ui.theme.FaceRegisterTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: FaceRegisterViewModel by viewModels()

    // permission launcher for camera access
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // permission granted, proceed with face registration
            setupFaceRegistration()
        } else {
            // handliing permission denied case
            handlePermissionDenied()
        }
    }

    @androidx.annotation.OptIn(ExperimentalGetImage::class)
//    @OptIn(ExperimentalGetImage::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // req camera permission on startup
        requestPermissionLauncher.launch(Manifest.permission.CAMERA)

        setContent {
            FaceRegisterTheme {
                // using disposableEffect to handle cleanup
                DisposableEffect(Unit) {
                    onDispose {
                        // Cleanup resources when activity is destroyed
                        viewModel.clearAllFaceData()
                    }
                }

                FaceRegisterScreen(viewModel = viewModel)
            }
        }
    }

    private fun setupFaceRegistration() {
        // additionaol setup if needed after permission granted....
    }

    private fun handlePermissionDenied() {
        // hamndling the case where camera permission is denied
        finish()
    }
}