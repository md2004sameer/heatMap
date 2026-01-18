package com.example.heatmap.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.heatmap.Note
import com.example.heatmap.ui.theme.BorderDark
import com.example.heatmap.ui.theme.LeetCodeOrange
import com.example.heatmap.ui.theme.SurfaceDark
import com.example.heatmap.ui.theme.Typography

@Composable
fun NotesModuleSection(viewModel: MainViewModel) {
    val folders by viewModel.folders.collectAsState()
    val notes by viewModel.currentNotes.collectAsState()
    val selectedFolderId by viewModel.selectedFolderId.collectAsState()
    
    var showEditorByNote by remember { mutableStateOf<Note?>(null) }
    var showFolderDialog by remember { mutableStateOf(false) }

    if (showEditorByNote != null) {
        NoteEditorDialog(
            note = showEditorByNote!!,
            onDismiss = { showEditorByNote = null },
            onSave = { updatedNote -> 
                viewModel.updateNote(updatedNote)
                showEditorByNote = null
            }
        )
    }

    if (showFolderDialog) {
        CreateFolderDialog(
            onDismiss = { showFolderDialog = false },
            onConfirm = { name ->
                viewModel.createFolder(name)
                showFolderDialog = false
            }
        )
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("My Study Notes", style = Typography.titleMedium, color = Color.White)
            
            Row {
                IconButton(onClick = { showFolderDialog = true }) {
                    Icon(Icons.Default.Create, contentDescription = "New Folder", tint = Color.Gray, modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = { selectedFolderId?.let { viewModel.createNote(it) } }) {
                    Icon(Icons.Default.Add, contentDescription = "New Note", tint = LeetCodeOrange)
                }
            }
        }

        // Folders Row
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            folders.forEach { folder ->
                val isSelected = selectedFolderId == folder.id
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.selectFolder(folder.id) },
                    label = { Text(folder.name) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = Color(0xFF2d333b),
                        selectedContainerColor = LeetCodeOrange.copy(alpha = 0.2f),
                        labelColor = Color.Gray,
                        selectedLabelColor = LeetCodeOrange
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = BorderDark,
                        selectedBorderColor = LeetCodeOrange,
                        borderWidth = 1.dp,
                        selectedBorderWidth = 1.dp
                    )
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Notes Previews
        if (notes.isEmpty()) {
            Box(Modifier.fillMaxWidth().height(100.dp).background(SurfaceDark, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                Text("No notes in this folder", style = Typography.labelMedium, color = Color.Gray)
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                notes.take(3).forEach { note ->
                    NotePreviewItem(note, onClick = { showEditorByNote = note }, onDelete = { viewModel.deleteNote(note) })
                }
                if (notes.size > 3) {
                    TextButton(onClick = { /* Could open full notes list */ }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                        Text("View All ${notes.size} Notes", color = LeetCodeOrange, style = Typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
fun NotePreviewItem(note: Note, onClick: () -> Unit, onDelete: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = BorderStroke(1.dp, BorderDark),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(note.title, style = Typography.bodyLarge, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(note.body.ifBlank { "No content" }, style = Typography.bodySmall, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Gray.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun NoteEditorDialog(note: Note, onDismiss: () -> Unit, onSave: (Note) -> Unit) {
    var title by remember { mutableStateOf(note.title) }
    var body by remember { mutableStateOf(note.body) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0d1117),
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxSize().padding(16.dp),
        title = {
            TextField(
                value = title,
                onValueChange = { title = it },
                placeholder = { Text("Title", color = Color.Gray) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                textStyle = Typography.titleLarge,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            TextField(
                value = body,
                onValueChange = { body = it },
                placeholder = { Text("Start typing your insights...", color = Color.Gray) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                textStyle = Typography.bodyLarge,
                modifier = Modifier.fillMaxSize()
            )
        },
        confirmButton = {
            Button(onClick = { onSave(note.copy(title = title, body = body)) }, colors = ButtonDefaults.buttonColors(containerColor = LeetCodeOrange)) {
                Text("Save", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close", color = Color.Gray) }
        }
    )
}

@Composable
fun CreateFolderDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = { Text("New Folder", style = Typography.titleLarge, color = Color.White) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text("Folder Name", color = Color.Gray) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = LeetCodeOrange,
                    unfocusedBorderColor = BorderDark,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            )
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onConfirm(name) }) { Text("Create", color = LeetCodeOrange, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Color.Gray) }
        }
    )
}
