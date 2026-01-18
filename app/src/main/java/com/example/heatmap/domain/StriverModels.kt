package com.example.heatmap.domain

data class StriverProblem(
    val id: Int,
    val section: String,
    val subSection: String,
    val title: String,
    val difficulty: String,
    val resources: StriverResources
)

data class StriverResources(
    val solve: String,
    val editorial: String,
    val postLink: String,
    val youtube: String
)
