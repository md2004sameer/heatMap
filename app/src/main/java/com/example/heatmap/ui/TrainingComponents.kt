package com.example.heatmap.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.heatmap.TaskEntity
import com.example.heatmap.ui.theme.LeetCodeGreen
import com.example.heatmap.ui.theme.LeetCodeOrange

@Composable
fun MinimalistTodoScreen(viewModel: MainViewModel) {
    val tasks by viewModel.allTasks.collectAsStateWithLifecycle()
    val filter by viewModel.todoFilter.collectAsStateWithLifecycle()
    var newTaskTitle by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Top App Bar Area
        Text(
            text = "To-Do",
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            color = Color.White,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Add Task Input
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = newTaskTitle,
                onValueChange = { newTaskTitle = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Add a task...", color = Color.Gray) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = LeetCodeOrange,
                    unfocusedBorderColor = Color(0xFF30363d),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )
            Spacer(Modifier.width(12.dp))
            Surface(
                onClick = {
                    if (newTaskTitle.isNotBlank()) {
                        viewModel.addTask(newTaskTitle)
                        newTaskTitle = ""
                    }
                },
                color = LeetCodeOrange,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Add, null, tint = Color.Black)
                }
            }
        }

        // Task List
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(tasks, key = { it.id }) { task ->
                TodoTaskItem(
                    task = task,
                    onToggle = { viewModel.toggleTask(task) },
                    onDelete = { viewModel.deleteTask(task) }
                )
            }
        }

        // Bottom Filter Toggles
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            color = Color(0xFF161b22),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TodoFilterButton("All", filter == "All") { viewModel.setTodoFilter("All") }
                TodoFilterButton("Active", filter == "Active") { viewModel.setTodoFilter("Active") }
                TodoFilterButton("Completed", filter == "Completed") { viewModel.setTodoFilter("Completed") }
            }
        }
    }
}

@Composable
private fun TodoTaskItem(
    task: TaskEntity,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        onClick = onToggle,
        color = Color(0xFF161b22),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (task.isCompleted) LeetCodeGreen.copy(alpha = 0.2f) else Color(0xFF30363d).copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (task.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (task.isCompleted) LeetCodeGreen else Color.Gray.copy(alpha = 0.4f),
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(Modifier.width(16.dp))
            
            Text(
                text = task.title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = if (task.isCompleted) Color.Gray else Color.White,
                textDecoration = if (task.isCompleted) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
            )
            
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Close, null, tint = Color.Gray.copy(alpha = 0.3f), modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun RowScope.TodoFilterButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.weight(1f),
        color = if (isSelected) Color(0xFF21262d) else Color.Transparent,
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(
            modifier = Modifier.padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                color = if (isSelected) LeetCodeOrange else Color.Gray,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}
