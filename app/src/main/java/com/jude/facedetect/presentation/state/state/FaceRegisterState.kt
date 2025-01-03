package com.jude.facedetect.presentation.state.state

import com.jude.facedetect.domain.model.CaptureState
import com.jude.facedetect.domain.model.FaceData


data class FaceRegisterState(
    val captureState: CaptureState = CaptureState.CAPTURE_CENTER,
    val faces: List<FaceData> = emptyList(),
    val isProcessing: Boolean = false,
    val error: String? = null,

    // adding these properties to track face matching
    val centerFaceId: String? = null,
    val leftFaceId: String? = null,
    val rightFaceId: String? = null,
    val isFaceMatched: Boolean = false
)