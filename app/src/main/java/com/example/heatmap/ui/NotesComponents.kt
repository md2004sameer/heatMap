package com.example.heatmap.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
    val notes by viewModel.allNotes.collectAsStateWithLifecycle()
    var showEditorByNote by remember { mutableStateOf<Note?>(null) }

    showEditorByNote?.let { selectedNote ->
        NoteEditorDialog(
            note = selectedNote,
            onDismiss = { showEditorByNote = null },
            onSave = { updatedNote -> 
                viewModel.updateNote(updatedNote)
            }
        )
    }

    Scaffold(
        topBar = {
            Text(
                "Notes",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    val newNote = Note(id = UUID.randomUUID().toString(), title = "", content = "")
                    viewModel.insertNote(newNote)
                    showEditorByNote = newNote
                },
                containerColor = LeetCodeOrange,
                contentColor = Color.Black
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Note")
            }
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        if (notes.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("Zero notes. Tap + to start.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
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
private fun MinimalistNoteItem(note: Note, onClick: () -> Unit, onDelete: () -> Unit) {
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
                    text = note.title.ifBlank { note.content.lineSequence().firstOrNull() ?: "Untitled Note" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
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
    var content by remember { mutableStateOf(note.content) }

    // Auto-save logic
    LaunchedEffect(title, content) {
        if (title != note.title || content != note.content) {
            onSave(note.copy(title = title, content = content, updatedAt = System.currentTimeMillis()))
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF0d1117)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                    }
                    Text("Editor", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(48.dp)) // Balance the layout
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
                        value = content,
                        onValueChange = { content = it },
                        placeholder = { Text("Start typing...", color = Color.Gray) },
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
