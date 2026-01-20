package com.example.heatmap.ui

data class PatternResponse(
    val slidingWindow: SlidingWindowData
)

data class SlidingWindowData(
    val patterns: List<PatternSheet>
)

data class PatternSheet(
    val id: Int,
    val pattern: String,
    val description: String,
    val practiceProblems: List<PatternProblem>,
    val bonusProblems: List<PatternProblem>
)

data class PatternProblem(
    val title: String,
    val difficulty: String,
    val link: String
)
