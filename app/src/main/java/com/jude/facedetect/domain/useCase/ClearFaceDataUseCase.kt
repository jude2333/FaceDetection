package com.jude.facedetect.domain.useCase

import com.jude.facedetect.domain.repository.FaceRepository

import javax.inject.Inject

class ClearFaceDataUseCase @Inject constructor(
    private val repository: FaceRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        return repository.clearFaceData()
    }
}