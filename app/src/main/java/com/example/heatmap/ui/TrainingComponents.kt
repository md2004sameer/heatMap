package com.example.heatmap.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.heatmap.DailyTrainingPlan
import com.example.heatmap.TaskType
import com.example.heatmap.TrainingTask

@Composable
fun TrainingPlanSection(
    plan: DailyTrainingPlan?,
    onGenerate: () -> Unit,
    onToggleTask: (String) -> Unit,
    onAddTask: (String, String, String, Int) -> Unit,
    onDeleteTask: (String) -> Unit
) {
    var showAddTaskDialog by remember { mutableStateOf(false) }

    if (showAddTaskDialog) {
        AddTaskDialog(
            onDismiss = { showAddTaskDialog = false },
            onAdd = { title, desc, cat, time ->
                onAddTask(title, desc, cat, time)
                showAddTaskDialog = false
            }
        )
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Training Plan",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (plan != null) {
                    IconButton(
                        onClick = { showAddTaskDialog = true },
                        modifier = Modifier.size(32.dp).background(Color(0xFF2d333b), CircleShape)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Task", tint = Color(0xFFffa116), modifier = Modifier.size(18.dp))
                    }
                } else {
                    Button(
                        onClick = onGenerate,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFffa116)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Generate Today's Plan", fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        if (plan != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161b22)),
                border = BorderStroke(1.dp, Color(0xFF30363d)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    if (plan.tasks.isEmpty()) {
                        Text("No tasks yet. Add one or regenerate plan.", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(vertical = 16.dp))
                    }

                    plan.tasks.forEach { task ->
                        TrainingTaskItem(
                            task = task, 
                            onToggle = { onToggleTask(task.id) },
                            onDelete = { onDeleteTask(task.id) }
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    
                    if (plan.tasks.isNotEmpty()) {
                        val progress = plan.tasks.count { it.isCompleted }.toFloat() / plan.tasks.size
                        val totalTime = plan.tasks.sumOf { it.estimatedTime }
                        
                        Spacer(Modifier.height(8.dp))
                        HorizontalDivider(color = Color(0xFF30363d))
                        Spacer(Modifier.height(12.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Total Est Time: ${totalTime} min",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                            Text(
                                "${(progress * 100).toInt()}% Done",
                                color = Color(0xFFffa116),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = Color(0xFFffa116),
                            trackColor = Color(0xFF2d333b)
                        )
                    }
                }
            }
        } else {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161b22).copy(alpha = 0.5f)),
                border = BorderStroke(1.dp, Color(0xFF30363d).copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.DateRange,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "No plan generated for today",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun AddTaskDialog(onDismiss: () -> Unit, onAdd: (String, String, String, Int) -> Unit) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("20") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF161b22),
        titleContentColor = Color.White,
        textContentColor = Color.White,
        title = { Text("Add Custom Task", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Title") },
                    colors = TextFieldDefaults.colors(unfocusedContainerColor = Color(0xFF2d333b), focusedContainerColor = Color(0xFF2d333b), unfocusedTextColor = Color.White, focusedTextColor = Color.White)
                )
                TextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Description") },
                    colors = TextFieldDefaults.colors(unfocusedContainerColor = Color(0xFF2d333b), focusedContainerColor = Color(0xFF2d333b), unfocusedTextColor = Color.White, focusedTextColor = Color.White)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextField(
                        value = category,
                        onValueChange = { category = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("Topic") },
                        colors = TextFieldDefaults.colors(unfocusedContainerColor = Color(0xFF2d333b), focusedContainerColor = Color(0xFF2d333b), unfocusedTextColor = Color.White, focusedTextColor = Color.White)
                    )
                    TextField(
                        value = time,
                        onValueChange = { time = it.filter { c -> c.isDigit() } },
                        modifier = Modifier.weight(0.5f),
                        label = { Text("Min") },
                        colors = TextFieldDefaults.colors(unfocusedContainerColor = Color(0xFF2d333b), focusedContainerColor = Color(0xFF2d333b), unfocusedTextColor = Color.White, focusedTextColor = Color.White)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (title.isNotBlank()) onAdd(title, desc, category, time.toIntOrNull() ?: 20) },
                enabled = title.isNotBlank()
            ) {
                Text("Add Task", color = Color(0xFFffa116))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0.5f, 0.5f, 0.5f))
            }
        }
    )
}

@Composable
fun TrainingTaskItem(task: TrainingTask, onToggle: () -> Unit, onDelete: () -> Unit) {
    val typeLabel = when (task.type) {
        TaskType.NEW_SOLVE -> "New Solve"
        TaskType.RE_SOLVE -> "Re-Solve (SPACED)"
        TaskType.REVIEW -> "Review"
        TaskType.DRILL -> "Drill"
        TaskType.CUSTOM -> "Custom Task"
    }
    
    val typeColor = when (task.type) {
        TaskType.NEW_SOLVE -> Color(0xFF00b8a3)
        TaskType.RE_SOLVE -> Color(0xFFffa116)
        TaskType.REVIEW -> Color(0xFF0b86ca)
        TaskType.DRILL -> Color(0xFFff375f)
        TaskType.CUSTOM -> Color(0xFF8b949e)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Checkbox(
            checked = task.isCompleted,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(
                checkedColor = Color(0xFF00b8a3),
                uncheckedColor = Color.Gray,
                checkmarkColor = Color.Black
            ),
            modifier = Modifier.size(24.dp).padding(top = 2.dp)
        )
        
        Spacer(Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f).clickable { onToggle() }) {
            Text(
                text = task.title,
                color = if (task.isCompleted) Color.Gray else Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null
            )
            
            Text(
                text = "$typeLabel ${if (task.category.isNotBlank()) "• ${task.category}" else ""}",
                color = typeColor.copy(alpha = 0.8f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
            
            if (!task.isCompleted) {
                Text(
                    text = task.description,
                    color = Color.Gray,
                    fontSize = 11.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${task.estimatedTime} min",
                    color = Color.Gray,
                    fontSize = 11.sp
                )
                if (task.difficulty != "N/A" && task.difficulty != "Custom") {
                    Text(
                        text = task.difficulty,
                        color = when(task.difficulty) {
                            "Easy" -> Color(0xFF00b8a3)
                            "Medium" -> Color(0xFFffc01e)
                            "Hard" -> Color(0xFFff375f)
                            else -> Color.Gray
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            IconButton(onClick = onDelete, modifier = Modifier.size(24.dp).padding(start = 4.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
            }
        }
    }
}
