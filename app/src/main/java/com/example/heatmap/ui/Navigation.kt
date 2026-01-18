package com.example.heatmap.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class ProfileSection(val title: String, val icon: ImageVector) {
    object Details : ProfileSection("Details", Icons.Default.Person)
    object Info : ProfileSection("Info", Icons.Default.Info)

    companion object {
        val all: List<ProfileSection> by lazy { 
            listOf(Details, Info) 
        }
    }
}

sealed class ProductivitySection(val title: String, val icon: ImageVector) {
    object Todo : ProductivitySection("Todo", Icons.AutoMirrored.Filled.List)
    object Notes : ProductivitySection("Notes", Icons.Default.Edit)

    companion object {
        val all: List<ProductivitySection> by lazy { 
            listOf(Todo, Notes) 
        }
    }
}

sealed class Screen {
    data class Profile(val section: ProfileSection) : Screen()
    data class Productivity(val section: ProductivitySection) : Screen()
}
