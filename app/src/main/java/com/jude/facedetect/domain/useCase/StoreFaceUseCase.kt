package com.jude.facedetect.domain.useCase



import com.jude.facedetect.domain.model.FaceData
import com.jude.facedetect.domain.model.FaceMatchUtils
import com.jude.facedetect.domain.repository.FaceRepository
import javax.inject.Inject

class StoreFaceUseCase @Inject constructor(
    private val repository: FaceRepository
) {
    suspend operator fun invoke(faceData: FaceData): Result<Unit> {
        return try {
            val existingFaces = repository.getFaceData().getOrDefault(emptyList())

            if (existingFaces.isNotEmpty()) {
                val isMatch = existingFaces.any { existingFace ->
                    FaceMatchUtils.validateFaceMatch(faceData, existingFace, FACE_MATCH_THRESHOLD)
                }

                if (!isMatch) {
                    return Result.failure(Exception("Face does not match existing data"))
                }
            }

            repository.storeFaceData(faceData)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        private const val FACE_MATCH_THRESHOLD = 0.5f
    }
}