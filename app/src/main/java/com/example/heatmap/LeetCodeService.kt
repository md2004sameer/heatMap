package com.example.heatmap

import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface LeetCodeService {
    @Headers("Content-Type: application/json")
    @POST("graphql")
    suspend fun getProfile(@Body request: GraphQLRequest): LeetCodeResponse
}
