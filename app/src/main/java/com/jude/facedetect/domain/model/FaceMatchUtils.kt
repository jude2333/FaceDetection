package com.jude.facedetect.domain.model

import kotlin.math.sqrt

object FaceMatchUtils {
    fun validateFaceMatch(newFace: FaceData, referenceFace: FaceData, threshold: Float): Boolean {
        if (newFace.landmarks.size != referenceFace.landmarks.size) return false

        val distance = calculateDistance(newFace.landmarks, referenceFace.landmarks)
        return distance < threshold
    }

    fun calculateDistance(
        landmarks1: List<Pair<Float, Float>>,
        landmarks2: List<Pair<Float, Float>>
    ): Float {
        if (landmarks1.size != landmarks2.size) return Float.MAX_VALUE

        val normalizedLandmarks1 = normalizeLandmarks(landmarks1)
        val normalizedLandmarks2 = normalizeLandmarks(landmarks2)

        var sumSquaredDist = 0f
        for (i in landmarks1.indices) {
            val dx = normalizedLandmarks1[i].first - normalizedLandmarks2[i].first
            val dy = normalizedLandmarks1[i].second - normalizedLandmarks2[i].second
            sumSquaredDist += dx * dx + dy * dy
        }

        return sqrt(sumSquaredDist / landmarks1.size)
    }

    private fun normalizeLandmarks(landmarks: List<Pair<Float, Float>>): List<Pair<Float, Float>> {
        if (landmarks.isEmpty()) return landmarks

        // finding center and scale
        val centerX = landmarks.map { it.first }.average().toFloat()
        val centerY = landmarks.map { it.second }.average().toFloat()

        val maxDist = landmarks.maxOf { landmark ->
            sqrt(
                (landmark.first - centerX) * (landmark.first - centerX) +
                        (landmark.second - centerY) * (landmark.second - centerY)
            )
        }

        // normalizeing to center at origin and scale to unit size
        return landmarks.map { (x, y) ->
            Pair(
                (x - centerX) / maxDist,
                (y - centerY) / maxDist
            )
        }
    }
}