package com.example.heatmap.domain

import com.example.heatmap.LeetCodeData
import com.example.heatmap.LeetCodeRepository
import kotlinx.coroutines.flow.Flow

class GetProfileUseCase(private val repository: LeetCodeRepository) {
    operator fun invoke(username: String): Flow<LeetCodeData?> {
        return repository.getProfile(username)
    }
}
