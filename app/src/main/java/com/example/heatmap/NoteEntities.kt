package com.example.heatmap

import androidx.room.*

@Entity(tableName = "folders")
data class Folder(
    @PrimaryKey val id: String,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "notes",
    foreignKeys = [
        ForeignKey(
            entity = Folder::class,
            parentColumns = ["id"],
            childColumns = ["folderId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("folderId")]
)
data class Note(
    @PrimaryKey val id: String,
    val folderId: String,
    val title: String,
    val body: String,
    val tags: String, // Comma-separated tags
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val linkedProblems: String? = null // JSON or comma-separated problem IDs
)

@Dao
interface NotesDao {
    @Query("SELECT * FROM folders ORDER BY createdAt DESC")
    suspend fun getAllFolders(): List<Folder>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: Folder)

    @Delete
    suspend fun deleteFolder(folder: Folder)

    @Query("SELECT * FROM notes WHERE folderId = :folderId ORDER BY isPinned DESC, updatedAt DESC")
    suspend fun getNotesInFolder(folderId: String): List<Note>

    @Query("SELECT * FROM notes WHERE isPinned = 1 ORDER BY updatedAt DESC")
    suspend fun getPinnedNotes(): List<Note>

    @Query("SELECT * FROM notes WHERE title LIKE '%' || :query || '%' OR body LIKE '%' || :query || '%' OR tags LIKE '%' || :query || '%'")
    suspend fun searchNotes(query: String): List<Note>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note)

    @Delete
    suspend fun deleteNote(note: Note)

    @Query("SELECT * FROM notes WHERE id = :noteId")
    suspend fun getNoteById(noteId: String): Note?
}
