package com.example.heatmap.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NoteAdd
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.heatmap.Note
import com.example.heatmap.ui.theme.LeetCodeOrange
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun NotesModuleSection(viewModel: MainViewModel) {
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    val notes by viewModel.currentNotes.collectAsStateWithLifecycle()
    val selectedFolderId by viewModel.selectedFolderId.collectAsStateWithLifecycle()
    
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

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Notebook", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color.White)
                Text("${notes.size} notes in ${folders.find { it.id == selectedFolderId }?.name ?: "folder"}", 
                    fontSize = 12.sp, color = Color.Gray)
            }
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    onClick = { showFolderDialog = true },
                    color = Color(0xFF161b22),
                    shape = CircleShape,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.CreateNewFolder, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                    }
                }
                
                Surface(
                    onClick = { selectedFolderId?.let { viewModel.createNote(it) } },
                    color = LeetCodeOrange,
                    shape = CircleShape,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Add, null, tint = Color.Black, modifier = Modifier.size(24.dp))
                    }
                }
            }
        }

        // Horizontal Folders List - Minimalist
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            folders.forEach { folder ->
                val isSelected = selectedFolderId == folder.id
                Column(
                    modifier = Modifier.clickable { viewModel.selectFolder(folder.id) },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = folder.name,
                        color = if (isSelected) LeetCodeOrange else Color.Gray,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                    AnimatedVisibility(visible = isSelected) {
                        Box(Modifier.padding(top = 4.dp).size(4.dp).background(LeetCodeOrange, CircleShape))
                    }
                }
            }
        }

        // Notes List - High Level Design
        if (notes.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.AutoMirrored.Filled.NoteAdd, null, tint = Color(0xFF21262d), modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("Capture your thoughts", color = Color.Gray, fontSize = 14.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(notes, key = { it.id }) { note ->
                    MinimalistNoteItem(
                        note = note, 
                        onClick = { showEditorByNote = note }, 
                        onDelete = { viewModel.deleteNote(note) }
                    )
                }
            }
        }
    }
}

@Composable
fun MinimalistNoteItem(note: Note, onClick: () -> Unit, onDelete: () -> Unit) {
    Surface(
        onClick = onClick,
        color = Color(0xFF161b22),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFF30363d).copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = note.title.ifBlank { "Untitled Note" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = note.body.ifBlank { "No additional text" },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(8.dp))
                val date = remember(note.updatedAt) {
                    SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(note.updatedAt))
                }
                Text(date, fontSize = 10.sp, color = Color.Gray.copy(alpha = 0.6f), fontWeight = FontWeight.Bold)
            }
            
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.DeleteOutline, null, tint = Color.Gray.copy(alpha = 0.3f), modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun NoteEditorDialog(note: Note, onDismiss: () -> Unit, onSave: (Note) -> Unit) {
    var title by remember { mutableStateOf(note.title) }
    var body by remember { mutableStateOf(note.body) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF0d1117)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Editor Toolbar
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, null, tint = Color.White)
                    }
                    Button(
                        onClick = { onSave(note.copy(title = title, body = body)) },
                        colors = ButtonDefaults.buttonColors(containerColor = LeetCodeOrange),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Save", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }

                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    TextField(
                        value = title,
                        onValueChange = { title = it },
                        placeholder = { Text("Title", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color.Gray) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        textStyle = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    HorizontalDivider(color = Color(0xFF30363d), modifier = Modifier.padding(vertical = 8.dp))

                    TextField(
                        value = body,
                        onValueChange = { body = it },
                        placeholder = { Text("Start writing your mastery insights...", color = Color.Gray) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        textStyle = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
fun CreateFolderDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF161b22),
        title = { Text("New Folder", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text("Enter folder name", color = Color.Gray) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = LeetCodeOrange,
                    unfocusedBorderColor = Color(0xFF30363d),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name) },
                colors = ButtonDefaults.buttonColors(containerColor = LeetCodeOrange)
            ) { 
                Text("Create", color = Color.Black, fontWeight = FontWeight.Bold) 
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Color.Gray) }
        }
    )
}
