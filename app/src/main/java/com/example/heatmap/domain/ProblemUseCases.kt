package com.example.heatmap.domain

import com.example.heatmap.LeetCodeRepository
import com.example.heatmap.ProblemEntity
import com.example.heatmap.StriverProblemEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetAllProblemsUseCase(private val repository: LeetCodeRepository) {
    operator fun invoke(refresh: Boolean = false): Flow<List<Problem>> {
        return repository.getAllProblems(refresh).map { entities ->
            entities.map { it.toDomain() }
        }
    }
}

class SearchProblemsUseCase(private val repository: LeetCodeRepository) {
    suspend operator fun invoke(query: String): List<Problem> {
        return repository.searchProblems(query).map { it.toDomain() }
    }
}

class GetProblemDetailUseCase(private val repository: LeetCodeRepository) {
    operator fun invoke(slug: String): Flow<Problem?> {
        return repository.getProblemDetail(slug).map { it?.toDomain() }
    }
}

// Mappers
fun ProblemEntity.toDomain(): Problem {
    return Problem(
        id = questionId,
        frontendId = questionFrontendId,
        title = title,
        slug = titleSlug,
        difficulty = difficulty,
        isPaidOnly = isPaidOnly,
        acRate = acRate,
        tags = tags.split(",").filter { it.isNotBlank() },
        content = content
    )
}

fun StriverProblemEntity.toLeetCodeProblem(): Problem? {
    if (!solveUrl.contains("leetcode.com")) return null
    
    // Extract slug from URL: https://leetcode.com/problems/two-sum/ -> two-sum
    val slug = solveUrl.substringBeforeLast("/").substringAfterLast("/")
    if (slug.isEmpty()) return null

    return Problem(
        id = id.toString(),
        frontendId = "", // Not readily available from URL
        title = title,
        slug = slug,
        difficulty = difficulty,
        isPaidOnly = false, // Assume false, will be updated if details fetched
        acRate = 0.0,
        tags = emptyList(),
        content = null
    )
}
