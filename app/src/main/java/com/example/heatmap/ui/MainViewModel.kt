package com.example.heatmap.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.heatmap.*
import com.example.heatmap.domain.GetProfileUseCase
import com.example.heatmap.domain.ValidateUsernameUseCase
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID

sealed class UiState {
    object Idle : UiState()
    object Onboarding : UiState()
    object Loading : UiState()
    data class Success(val data: LeetCodeData) : UiState()
    data class Error(val message: String) : UiState()
}

class MainViewModel(
    application: Application,
    private val getProfileUseCase: GetProfileUseCase,
    private val validateUsernameUseCase: ValidateUsernameUseCase
) : AndroidViewModel(application) {
    
    private val context = application.applicationContext
    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState

    private val _currentScreen = MutableStateFlow<Screen>(Screen.Profile(ProfileSection.Details))
    val currentScreen: StateFlow<Screen> = _currentScreen

    private val _trainingPlan = MutableStateFlow<DailyTrainingPlan?>(null)
    val trainingPlan: StateFlow<DailyTrainingPlan?> = _trainingPlan

    // Notes State
    private val notesDao by lazy { LeetCodeDatabase.getDatabase(context).notesDao() }
    private val _folders = MutableStateFlow<List<Folder>>(emptyList())
    val folders: StateFlow<List<Folder>> = _folders

    private val _currentNotes = MutableStateFlow<List<Note>>(emptyList())
    val currentNotes: StateFlow<List<Note>> = _currentNotes

    private val _selectedFolderId = MutableStateFlow<String?>(null)
    val selectedFolderId: StateFlow<String?> = _selectedFolderId

    private val prefs = context.getSharedPreferences("leetcode_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    init {
        checkOnboarding()
        loadTrainingPlan()
        loadFolders()
    }

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }

    // Notes Operations
    private fun loadFolders() {
        viewModelScope.launch {
            try {
                val folderList = notesDao.getAllFolders()
                if (folderList.isEmpty()) {
                    val defaultFolder = Folder(UUID.randomUUID().toString(), "All Notes")
                    notesDao.insertFolder(defaultFolder)
                    _folders.value = listOf(defaultFolder)
                    selectFolder(defaultFolder.id)
                } else {
                    _folders.value = folderList
                    selectFolder(folderList.first().id)
                }
            } catch (e: Exception) {
                // Log error or handle gracefully
            }
        }
    }

    fun selectFolder(folderId: String) {
        _selectedFolderId.value = folderId
        viewModelScope.launch {
            _currentNotes.value = notesDao.getNotesInFolder(folderId)
        }
    }

    fun createFolder(name: String) {
        viewModelScope.launch {
            val newFolder = Folder(UUID.randomUUID().toString(), name)
            notesDao.insertFolder(newFolder)
            loadFolders()
        }
    }

    fun createNote(folderId: String, title: String = "New Note", body: String = "") {
        viewModelScope.launch {
            val newNote = Note(
                id = UUID.randomUUID().toString(),
                folderId = folderId,
                title = title,
                body = body,
                tags = ""
            )
            notesDao.insertNote(newNote)
            selectFolder(folderId)
        }
    }

    fun updateNote(note: Note) {
        viewModelScope.launch {
            notesDao.insertNote(note.copy(updatedAt = System.currentTimeMillis()))
            selectFolder(note.folderId)
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch {
            notesDao.deleteNote(note)
            selectFolder(note.folderId)
        }
    }

    fun searchNotes(query: String) {
        viewModelScope.launch {
            if (query.isBlank()) {
                _selectedFolderId.value?.let { selectFolder(it) }
            } else {
                _currentNotes.value = notesDao.searchNotes(query)
            }
        }
    }

    // Training Plan Operations
    private fun checkOnboarding() {
        if (!prefs.contains("last_username")) {
            _uiState.value = UiState.Onboarding
        } else {
            val username = prefs.getString("last_username", "") ?: ""
            if (username.isNotEmpty()) {
                fetchProfile(username)
            } else {
                _uiState.value = UiState.Onboarding
            }
        }
    }

    private fun loadTrainingPlan() {
        try {
            val json = prefs.getString("daily_plan", null)
            if (json != null) {
                val plan = gson.fromJson(json, DailyTrainingPlan::class.java)
                if (plan.date == LocalDate.now().toString()) {
                    _trainingPlan.value = plan
                }
            }
        } catch (e: Exception) {
            // Handle parsing or date errors
        }
    }

    fun generateTrainingPlan(data: LeetCodeData) {
        val tasks = mutableListOf<TrainingTask>()
        data.activeDailyCodingChallengeQuestion?.let { challenge ->
            val question = challenge.question
            if (question != null) {
                tasks.add(TrainingTask(
                    UUID.randomUUID().toString(), 
                    question.title ?: "Daily Challenge", 
                    "Daily Challenge", 
                    "Mixed", 
                    question.difficulty ?: "N/A", 
                    45, 
                    TaskType.NEW_SOLVE
                ))
            }
        }
        data.recentSubmissionList?.firstOrNull { it.statusDisplay == "Accepted" }?.let { sub ->
            tasks.add(TrainingTask(UUID.randomUUID().toString(), sub.title, "Re-solve", "Mixed", "Medium", 25, TaskType.RE_SOLVE))
        }
        val skill = data.matchedUser?.profile?.skillTags?.randomOrNull() ?: "Arrays"
        tasks.add(TrainingTask(UUID.randomUUID().toString(), "$skill Pattern Review", "Concept review", skill, "N/A", 15, TaskType.REVIEW))

        val newPlan = DailyTrainingPlan(LocalDate.now().toString(), tasks)
        _trainingPlan.value = newPlan
        savePlan(newPlan)
    }

    fun addCustomTask(title: String, description: String, category: String, estimatedTime: Int) {
        val currentPlan = _trainingPlan.value ?: DailyTrainingPlan(LocalDate.now().toString(), emptyList())
        val newTask = TrainingTask(UUID.randomUUID().toString(), title, description, category, "Custom", estimatedTime, TaskType.CUSTOM)
        val updatedPlan = currentPlan.copy(tasks = currentPlan.tasks + newTask)
        _trainingPlan.value = updatedPlan
        savePlan(updatedPlan)
    }

    fun removeTask(taskId: String) {
        val currentPlan = _trainingPlan.value ?: return
        val updatedPlan = currentPlan.copy(tasks = currentPlan.tasks.filter { it.id != taskId })
        _trainingPlan.value = updatedPlan
        savePlan(updatedPlan)
    }

    fun toggleTaskCompletion(taskId: String) {
        val currentPlan = _trainingPlan.value ?: return
        val updatedTasks = currentPlan.tasks.map { if (it.id == taskId) it.copy(isCompleted = !it.isCompleted) else it }
        val updatedPlan = currentPlan.copy(tasks = updatedTasks)
        _trainingPlan.value = updatedPlan
        savePlan(updatedPlan)
    }

    private fun savePlan(plan: DailyTrainingPlan) {
        prefs.edit().putString("daily_plan", gson.toJson(plan)).apply()
    }

    fun saveUsernameAndFetch(username: String) {
        val error = validateUsernameUseCase(username)
        if (error != null) {
            _uiState.value = UiState.Error(error)
            return
        }
        prefs.edit().putString("last_username", username).apply()
        fetchProfile(username)
    }

    fun fetchProfile(username: String) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            getProfileUseCase(username).collectLatest { data ->
                if (data != null && data.matchedUser != null) {
                    _uiState.value = UiState.Success(data)
                } else {
                    _uiState.value = UiState.Error("User not found or network error.")
                }
            }
        }
    }

    fun resetToOnboarding() {
        _uiState.value = UiState.Onboarding
    }
}
