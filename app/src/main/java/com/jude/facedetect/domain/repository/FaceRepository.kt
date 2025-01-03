package com.jude.facedetect.domain.repository

import com.jude.facedetect.domain.model.FaceData

interface FaceRepository {
    suspend fun storeFaceData(faceData: FaceData): Result<Unit>
    suspend fun getFaceData(): Result<List<FaceData>>
    suspend fun clearFaceData(): Result<Unit>
}