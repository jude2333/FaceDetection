package com.jude.facedetect.domain.model

import android.graphics.Rect
import kotlinx.serialization.Serializable


@Serializable
data class FaceData(
    val id: String,
    val position: CaptureState,
    val landmarks: List<Pair<Float, Float>> = emptyList()
)