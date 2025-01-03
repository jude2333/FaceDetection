package com.jude.facedetect.presentation.state.viewModel


import android.util.Log
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetector
import com.jude.facedetect.domain.model.CaptureState
import com.jude.facedetect.domain.model.FaceData
import com.jude.facedetect.domain.model.FaceMatchUtils.validateFaceMatch
import com.jude.facedetect.domain.useCase.ClearFaceDataUseCase
import com.jude.facedetect.domain.useCase.StoreFaceUseCase
import com.jude.facedetect.presentation.state.state.FaceRegisterState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import kotlin.math.abs
import com.google.mlkit.vision.face.Face as MLKitFace


@HiltViewModel
class FaceRegisterViewModel @Inject constructor(
    private val faceDetector: FaceDetector,
    private val storeFaceUseCase: StoreFaceUseCase,
    private val clearFaceDataUseCase: ClearFaceDataUseCase
) : ViewModel() {

    private val _uiState = mutableStateOf(FaceRegisterState())
    val uiState: State<FaceRegisterState> = _uiState

    @OptIn(androidx.camera.core.ExperimentalGetImage::class)
    @ExperimentalGetImage
    fun processImage(imageProxy: ImageProxy) {
        if (_uiState.value.isProcessing || _uiState.value.captureState == CaptureState.COMPLETE) {
            imageProxy.close()
            return
        }

        _uiState.value = _uiState.value.copy(isProcessing = true)

        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            faceDetector.process(image)
                .addOnSuccessListener { faces ->
                    handleFaceDetection(faces)
                }
                .addOnFailureListener { e ->
                    handleError("Error processing image: ${e.localizedMessage}")
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
            _uiState.value = _uiState.value.copy(isProcessing = false)
        }
    }

    private fun handleFaceDetection(faces: List<MLKitFace>) {
        if (faces.size == 1) {
            val detectedFace = faces[0]
            val newFaceData = extractFaceData(detectedFace)

            matchFace(newFaceData)

            val eulerX = detectedFace.headEulerAngleX // up and down rotation
            val eulerY = detectedFace.headEulerAngleY // Left and right rotation
            val eulerZ = detectedFace.headEulerAngleZ // tilt

            when (_uiState.value.captureState) {
                CaptureState.CAPTURE_CENTER -> {
                    if (isFaceAligned(eulerX, eulerY, eulerZ)) {
                        captureFace(detectedFace, CaptureState.CAPTURE_CENTER)
                    }
                }
                CaptureState.CAPTURE_LEFT -> {
                    if (isFaceLeftTurn(eulerY, eulerZ)) {
                        captureFace(detectedFace, CaptureState.CAPTURE_LEFT)
                    }
                }
                CaptureState.CAPTURE_RIGHT -> {
                    if (isFaceRightTurn(eulerY, eulerZ)) {
                        captureFace(detectedFace, CaptureState.CAPTURE_RIGHT)
                    }
                }
                CaptureState.COMPLETE -> {} // no action needed......
            }
        } else {
            val errorMessage = if (faces.isEmpty()) "No face detected" else "Multiple faces detected"
            handleError(errorMessage)
        }
        _uiState.value = _uiState.value.copy(isProcessing = false)
    }

    private fun handleError(message: String) {
        _uiState.value = _uiState.value.copy(
            error = message,
            isProcessing = false
        )
    }

    private fun matchFace(newFace: FaceData) {
        viewModelScope.launch {
            val referenceFace = _uiState.value.faces.find { face ->
                validateFaceMatch(newFace, face, THRESHOLD)
            }

            val updatedState = if (referenceFace != null) {
                _uiState.value.copy(
                    error = "Face match successful: ${referenceFace.id}"
                )
            } else {
                _uiState.value.copy(
                    error = "No matching face found"
                )
            }

            _uiState.value = updatedState
        }
    }

    private fun extractFaceData(face: MLKitFace): FaceData {
        return FaceData(
            id = UUID.randomUUID().toString(),
            position = _uiState.value.captureState,
            landmarks = extractLandmarks(face)
        )
    }

    private fun extractLandmarks(face: MLKitFace): List<Pair<Float, Float>> {
        return face.allLandmarks.map { landmark ->
            Pair(landmark.position.x, landmark.position.y)
        }
    }

    private fun isFaceAligned(eulerX: Float, eulerY: Float, eulerZ: Float): Boolean {
        return abs(eulerX) < 15 && // allowing slight up/down movement
                abs(eulerY) < 15 && // allowing slight left/right movement
                abs(eulerZ) < 15    // allowing slight tilt
    }

    private fun isFaceLeftTurn(eulerY: Float, eulerZ: Float): Boolean {
        return eulerY in 25f..60f && // moree permissive range for left turn
                abs(eulerZ) < 20      // allowing some natural tilt
    }

    private fun isFaceRightTurn(eulerY: Float, eulerZ: Float): Boolean {
        return eulerY in -60f..-25f && // more permissive range for right turn
                abs(eulerZ) < 20        // allowing some natural tilt
    }

    private fun captureFace(face: MLKitFace, position: CaptureState) {
        viewModelScope.launch {
            val faceData = extractFaceData(face)

            // Add debug logging
            Log.d("FaceCapture", "Capturing face for position: $position")
            Log.d("FaceCapture", "EulerX: ${face.headEulerAngleX}")
            Log.d("FaceCapture", "EulerY: ${face.headEulerAngleY}")
            Log.d("FaceCapture", "EulerZ: ${face.headEulerAngleZ}")

            storeFaceUseCase(faceData).onSuccess {
                _uiState.value = _uiState.value.copy(
                    faces = _uiState.value.faces + faceData,
                    captureState = getNextCaptureState(position),
                    isProcessing = false,
                    error = null
                )
                delay(1000) // reduced delay for better UX
            }.onFailure { error ->
                handleError("Failed to store face data: ${error.message}")
            }
        }
    }

    private fun getNextCaptureState(currentState: CaptureState): CaptureState {
        return when (currentState) {
            CaptureState.CAPTURE_CENTER -> CaptureState.CAPTURE_LEFT
            CaptureState.CAPTURE_LEFT -> CaptureState.CAPTURE_RIGHT
            CaptureState.CAPTURE_RIGHT -> CaptureState.COMPLETE
            CaptureState.COMPLETE -> CaptureState.COMPLETE
        }
    }

    fun clearAllFaceData() {
        viewModelScope.launch {
            clearFaceDataUseCase().onSuccess {
                _uiState.value = FaceRegisterState()
            }.onFailure { error ->
                handleError("Failed to clear face data: ${error.message}")
            }
        }
    }

    companion object {
        private const val THRESHOLD = 0.5f
    }
}
