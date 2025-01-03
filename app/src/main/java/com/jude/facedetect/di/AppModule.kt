package com.jude.facedetect.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.jude.facedetect.domain.repository.FaceRepository
import com.jude.facedetect.domain.repository.FaceRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideFaceDetector(options: FaceDetectorOptions): FaceDetector {
        return FaceDetection.getClient(options)
    }

    @Provides
    @Singleton
    fun provideFaceDetectorOptions(): FaceDetectorOptions {
        return FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setMinFaceSize(0.15f)
            .enableTracking()
            .build()
    }

    @Provides
    @Singleton
    fun provideFaceRepository(
        @ApplicationContext context: Context,
        faceDetector: FaceDetector,
        dataStore: DataStore<Preferences> // Corrected dataStore type
    ): FaceRepository {
        return FaceRepositoryImpl(dataStore, faceDetector) // Passing dataStore instead of context
    }

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile("face_detection_prefs") }
        )
    }
}
