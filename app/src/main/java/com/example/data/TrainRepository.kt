package com.example.data

import com.example.network.TrainApiService
import com.example.network.TrainResponse
import kotlinx.coroutines.flow.Flow

class TrainRepository(
    private val trainDao: TrainDao,
    private val apiService: TrainApiService
) {
    val savedTrains: Flow<List<SavedTrain>> = trainDao.getSavedTrains()

    suspend fun getTrainStatus(trainNo: String): TrainResponse {
        return apiService.getTrainRunningStatus(trainNo)
    }

    suspend fun saveTrain(trainNo: String, trainName: String) {
        val savedTrain = SavedTrain(
            trainNo = trainNo,
            trainName = trainName,
            lastSearched = System.currentTimeMillis(),
            isFavorite = false
        )
        trainDao.insertTrain(savedTrain)
    }

    suspend fun toggleFavorite(train: SavedTrain) {
        trainDao.updateTrain(train.copy(isFavorite = !train.isFavorite))
    }

    suspend fun deleteTrain(trainNo: String) {
        trainDao.deleteByNo(trainNo)
    }
}
