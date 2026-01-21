package com.example.heatmap

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "minimalist_notes")
data class Note(
    @PrimaryKey val id: String,
    val title: String,
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Dao
interface NotesDao {
    @Query("SELECT * FROM minimalist_notes ORDER BY updatedAt DESC")
    fun getAllNotesFlow(): Flow<List<Note>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note)

    @Update
    suspend fun updateNote(note: Note)

    @Delete
    suspend fun deleteNote(note: Note)

    @Query("SELECT * FROM minimalist_notes WHERE id = :noteId")
    suspend fun getNoteById(noteId: String): Note?
}
