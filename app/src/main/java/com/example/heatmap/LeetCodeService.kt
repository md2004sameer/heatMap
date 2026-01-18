package com.example.heatmap

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST

interface LeetCodeService {
    @Headers("Content-Type: application/json")
    @POST("graphql")
    suspend fun getProfile(@Body request: GraphQLRequest): LeetCodeResponse

    @GET("api/problems/all/")
    suspend fun getAllProblemsRest(): AllProblemsRestResponse
}

// Models for the REST approach
data class AllProblemsRestResponse(
    val stat_status_pairs: List<StatStatusPair>
)

data class StatStatusPair(
    val stat: QuestionStat,
    val difficulty: Difficulty,
    val paid_only: Boolean
)

data class QuestionStat(
    val question_id: Int,
    val question__title: String,
    val question__title_slug: String,
    val frontend_question_id: Int,
    val total_acs: Int,
    val total_submitted: Int
)

data class Difficulty(
    val level: Int
)
