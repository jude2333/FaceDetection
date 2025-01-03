package com.jude.facedetect.domain.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.mlkit.vision.face.FaceDetector
import com.jude.facedetect.domain.model.FaceData
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

class FaceRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val faceDetector: FaceDetector
) : FaceRepository {

    override suspend fun storeFaceData(faceData: FaceData): Result<Unit> = try {
        dataStore.edit { preferences ->
            val existingFaces = getFaceDataFromPreferences(preferences)
            val updatedFaces = existingFaces + faceData
            preferences[PreferencesKeys.FACE_DATA] = Json.encodeToString(updatedFaces)
        }
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getFaceData(): Result<List<FaceData>> = try {
        val faces = dataStore.data
            .catch { emit(emptyPreferences()) }
            .map { preferences ->
                getFaceDataFromPreferences(preferences)
            }.first()
        Result.success(faces)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun clearFaceData(): Result<Unit> = try {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.FACE_DATA] = Json.encodeToString(emptyList<FaceData>())
        }
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    private fun getFaceDataFromPreferences(preferences: Preferences): List<FaceData> {
        return try {
            val jsonString = preferences[PreferencesKeys.FACE_DATA] ?: return emptyList()
            Json.decodeFromString(jsonString)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private object PreferencesKeys {
        val FACE_DATA = stringPreferencesKey("face_data")
    }
}
