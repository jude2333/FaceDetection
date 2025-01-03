package com.jude.facedetect.domain.useCase


import com.jude.facedetect.domain.model.FaceData
import com.jude.facedetect.domain.repository.FaceRepository
import javax.inject.Inject

class GetFaceDataUseCase @Inject constructor(
    private val repository: FaceRepository
) {
    suspend operator fun invoke(): Result<List<FaceData>> {
        return repository.getFaceData()
    }
}