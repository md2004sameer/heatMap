package com.example.heatmap.domain

import com.example.heatmap.LeetCodeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.LocalDate

class GetGfgPotdUseCase(private val repository: LeetCodeRepository) {
    operator fun invoke(): Flow<List<GfgPotdEntity>> = flow {
        // 1. Emit existing history
        val cached = repository.getAllGfgPotd()
        emit(cached)

        // 2. Fetch today's if not already fetched
        val today = LocalDate.now().toString()
        if (cached.none { it.date.startsWith(today) }) {
            try {
                repository.fetchAndStoreGfgPotd()
                emit(repository.getAllGfgPotd())
            } catch (e: Exception) {
                // Handle or log error
            }
        }
    }
}
