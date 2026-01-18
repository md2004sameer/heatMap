package com.example.heatmap.domain

class ValidateUsernameUseCase {
    operator fun invoke(username: String): String? {
        if (username.isBlank()) return "Username cannot be empty"
        if (username.length < 3) return "Too short (min 3 chars)"
        if (!username.first().isLetter()) return "Must start with a letter"
        val regex = "^[a-zA-Z][a-zA-Z0-9_-]*$".toRegex()
        if (!username.matches(regex)) return "Only letters, numbers, _ and - allowed"
        return null
    }

    fun isValidFormat(username: String): Boolean {
        val regex = "^[a-zA-Z][a-zA-Z0-9_-]{2,29}$".toRegex()
        return username.matches(regex)
    }
}
