package com.example.heatmap.domain

data class Problem(
    val id: String,
    val frontendId: String,
    val title: String,
    val slug: String,
    val difficulty: String,
    val isPaidOnly: Boolean,
    val acRate: Double,
    val tags: List<String>,
    val content: String? = null
)

data class ProblemDetail(
    val problem: Problem,
    val codeSnippets: List<CodeSnippetDomain> = emptyList(),
    val sampleTestCase: String? = null,
    val hints: List<String> = emptyList()
)

data class CodeSnippetDomain(
    val lang: String,
    val langSlug: String,
    val code: String
)
