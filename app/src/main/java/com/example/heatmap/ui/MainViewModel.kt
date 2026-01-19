package com.example.heatmap.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.heatmap.*
import com.example.heatmap.domain.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

sealed class UiState {
    object Idle : UiState()
    object Onboarding : UiState()
    object Loading : UiState()
    data class Success(val data: LeetCodeData) : UiState()
    data class Error(val message: String) : UiState()
}

data class StriverStats(
    val totalCount: Int = 0,
    val completedTotal: Int = 0,
    val percentage: Int = 0,
    val easyTotal: Int = 0,
    val easyDone: Int = 0,
    val mediumTotal: Int = 0,
    val mediumDone: Int = 0,
    val hardTotal: Int = 0,
    val hardDone: Int = 0
)

class MainViewModel(
    application: Application,
    private val getProfileUseCase: GetProfileUseCase,
    private val validateUsernameUseCase: ValidateUsernameUseCase
) : AndroidViewModel(application) {
    
    private val db = LeetCodeDatabase.getDatabase(application)
    private val repository = LeetCodeRepository.getInstance(application)
    private val getAllProblemsUseCase = GetAllProblemsUseCase(repository)
    private val searchProblemsUseCase = SearchProblemsUseCase(repository)
    private val getProblemDetailUseCase = GetProblemDetailUseCase(repository)
    private val getGfgPotdUseCase = GetGfgPotdUseCase(repository)

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState

    private val _currentScreen = MutableStateFlow<Screen>(Screen.Profile(ProfileSection.Details))
    val currentScreen: StateFlow<Screen> = _currentScreen

    private val _trainingPlan = MutableStateFlow<DailyTrainingPlan?>(null)
    val trainingPlan: StateFlow<DailyTrainingPlan?> = _trainingPlan

    // Problems State
    private val _problems = MutableStateFlow<List<Problem>>(emptyList())
    val problems: StateFlow<List<Problem>> = _problems

    private val _selectedProblem = MutableStateFlow<Problem?>(null)
    val selectedProblem: StateFlow<Problem?> = _selectedProblem

    private val _isProblemsSyncing = MutableStateFlow(false)
    val isProblemsSyncing: StateFlow<Boolean> = _isProblemsSyncing

    // Striver Sheet State
    private val _striverProblems = MutableStateFlow<List<StriverProblemEntity>>(emptyList())
    val striverProblems: StateFlow<List<StriverProblemEntity>> = _striverProblems

    private val _completedStriverIds = MutableStateFlow<Set<Int>>(emptySet())
    val completedStriverIds: StateFlow<Set<Int>> = _completedStriverIds

    val striverStats: StateFlow<StriverStats> = combine(_striverProblems, _completedStriverIds) { problems, completedIds ->
        val total = problems.size
        val done = completedIds.size
        val percentage = if (total > 0) (done * 100 / total) else 0
        
        val easy = problems.filter { it.difficulty == "Easy" }
        val medium = problems.filter { it.difficulty == "Medium" }
        val hard = problems.filter { it.difficulty == "Hard" }

        StriverStats(
            totalCount = total,
            completedTotal = done,
            percentage = percentage,
            easyTotal = easy.size,
            easyDone = easy.count { it.id in completedIds },
            mediumTotal = medium.size,
            mediumDone = medium.count { it.id in completedIds },
            hardTotal = hard.size,
            hardDone = hard.count { it.id in completedIds }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StriverStats())

    // GFG POTD State
    private val _gfgPotdList = MutableStateFlow<List<GfgPotdEntity>>(emptyList())
    val gfgPotdList: StateFlow<List<GfgPotdEntity>> = _gfgPotdList

    // Notes State
    private val notesDao = db.notesDao()
    private val _folders = MutableStateFlow<List<Folder>>(emptyList())
    val folders: StateFlow<List<Folder>> = _folders

    private val _currentNotes = MutableStateFlow<List<Note>>(emptyList())
    val currentNotes: StateFlow<List<Note>> = _currentNotes

    private val _selectedFolderId = MutableStateFlow<String?>(null)
    val selectedFolderId: StateFlow<String?> = _selectedFolderId

    val submissionByDate: StateFlow<Map<LocalDate, Int>> = uiState.map { state ->
        if (state is UiState.Success) {
            parseSubmissionCalendar(state.data.matchedUser?.userCalendar?.submissionCalendar)
        } else {
            emptyMap()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    private val gson = Gson()

    init {
        viewModelScope.launch {
            checkOnboarding()
            loadTrainingPlan()
            loadFolders()
            loadProblems()
            loadStriverSheet()
            loadGfgPotd()
            
            // Prefetch some problem descriptions in background for offline use
            repository.prefetchProblemDetails(20)
        }
    }

    private fun parseSubmissionCalendar(calendar: String?): Map<LocalDate, Int> {
        return try {
            if (calendar.isNullOrBlank()) {
                emptyMap()
            } else {
                val type = object : TypeToken<Map<String, Int>>() {}.type
                val rawMap = Gson().fromJson<Map<String, Int>>(calendar, type)
                rawMap?.entries?.associate { (tsStr, count) ->
                    val ts = tsStr.toLongOrNull() ?: 0L
                    val date = if (tsStr.length > 10) {
                        Instant.ofEpochMilli(ts).atZone(ZoneId.systemDefault()).toLocalDate()
                    } else {
                        Instant.ofEpochSecond(ts).atZone(ZoneId.systemDefault()).toLocalDate()
                    }
                    date to count
                } ?: emptyMap()
            }
        } catch (_: Exception) {
            emptyMap()
        }
    }

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }

    // GFG POTD Operations
    private fun loadGfgPotd() {
        viewModelScope.launch {
            getGfgPotdUseCase().collectLatest {
                _gfgPotdList.value = it
            }
        }
    }

    // Problems Operations
    fun loadProblems(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _isProblemsSyncing.value = true
            getAllProblemsUseCase(forceRefresh).collectLatest {
                _problems.value = it
                _isProblemsSyncing.value = false
            }
        }
    }

    private fun loadStriverSheet() {
        viewModelScope.launch {
            try {
                val striverDao = db.striverDao()
                if (striverDao.getCount() == 0) {
                    val jsonString = getApplication<Application>().assets.open("striver_sheet.json").bufferedReader().use { it.readText() }
                    val type = object : TypeToken<List<StriverProblem>>() {}.type
                    val problems: List<StriverProblem> = gson.fromJson(jsonString, type)
                    
                    val entities = problems.mapIndexed { index, it -> 
                        StriverProblemEntity(
                            id = it.id,
                            section = it.section,
                            subSection = it.subSection.ifEmpty { "General" },
                            title = it.title,
                            difficulty = it.difficulty,
                            solveUrl = it.resources.solve,
                            editorialUrl = it.resources.editorial,
                            postUrl = it.resources.postLink,
                            youtubeUrl = it.resources.youtube,
                            stepOrder = index + 1
                        )
                    }
                    striverDao.insertStriverProblems(entities)
                }
                
                val entities = striverDao.getAllStriverProblems()
                _striverProblems.value = entities
                _completedStriverIds.value = entities.filter { it.isCompleted }.map { it.id }.toSet()
            } catch (_: Exception) {
                // Log error
            }
        }
    }

    fun toggleStriverProblem(id: Int) {
        viewModelScope.launch {
            val currentSet = _completedStriverIds.value.toMutableSet()
            val isCompleted = !currentSet.contains(id)
            if (isCompleted) {
                currentSet.add(id)
            } else {
                currentSet.remove(id)
            }
            _completedStriverIds.value = currentSet
            db.striverDao().updateProgress(id, isCompleted)
            // Refresh list
            _striverProblems.value = db.striverDao().getAllStriverProblems()
        }
    }

    fun searchProblems(query: String) {
        viewModelScope.launch {
            if (query.isEmpty()) {
                repository.getAllProblems(false).collectLatest {
                    _problems.value = it.map { entity -> entity.toDomain() }
                }
            } else {
                _problems.value = searchProblemsUseCase(query)
            }
        }
    }

    fun selectProblem(problem: Problem) {
        _selectedProblem.value = problem
        viewModelScope.launch {
            getProblemDetailUseCase(problem.slug).collectLatest { detailedProblem ->
                if (detailedProblem != null) {
                    _selectedProblem.value = detailedProblem
                }
            }
        }
    }

    fun clearSelectedProblem() {
        _selectedProblem.value = null
    }

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
            } catch (_: Exception) {
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

    // Preference Operations
    fun setPreference(key: String, value: String) {
        viewModelScope.launch {
            db.preferenceDao().setPreference(AppPreferenceEntity(key, value))
        }
    }

    // Training Plan Operations
    private suspend fun checkOnboarding() {
        val lastUsername = db.preferenceDao().getPreference("last_username")
        if (lastUsername == null) {
            _uiState.value = UiState.Onboarding
        } else {
            fetchProfile(lastUsername)
        }
    }

    private suspend fun loadTrainingPlan() {
        try {
            val today = LocalDate.now().toString()
            val planEntity = db.trainingDao().getPlanByDate(today)
            if (planEntity != null) {
                val taskEntities = db.trainingDao().getTasksForPlan(today)
                val tasks = taskEntities.map { 
                    TrainingTask(
                        id = it.id,
                        title = it.title,
                        description = it.description,
                        category = it.category,
                        difficulty = it.difficulty,
                        estimatedTime = it.estimatedTime,
                        type = TaskType.valueOf(it.type),
                        isCompleted = it.isCompleted
                    )
                }
                _trainingPlan.value = DailyTrainingPlan(today, tasks)
            }
        } catch (_: Exception) {
        }
    }

    fun generateTrainingPlan(data: LeetCodeData) {
        viewModelScope.launch {
            val today = LocalDate.now().toString()
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

            val newPlan = DailyTrainingPlan(today, tasks)
            _trainingPlan.value = newPlan
            savePlanToDb(newPlan)
        }
    }

    fun addCustomTask(title: String, description: String, category: String, estimatedTime: Int) {
        viewModelScope.launch {
            val today = LocalDate.now().toString()
            val currentPlan = _trainingPlan.value ?: DailyTrainingPlan(today, emptyList())
            val newTask = TrainingTask(UUID.randomUUID().toString(), title, description, category, "Custom", estimatedTime, TaskType.CUSTOM)
            val updatedPlan = currentPlan.copy(tasks = currentPlan.tasks + newTask)
            _trainingPlan.value = updatedPlan
            
            db.trainingDao().insertPlan(TrainingPlanEntity(today))
            db.trainingDao().insertTask(TrainingTaskEntity(
                id = newTask.id,
                planDate = today,
                title = newTask.title,
                description = newTask.description,
                category = newTask.category,
                difficulty = newTask.difficulty,
                estimatedTime = newTask.estimatedTime,
                type = newTask.type.name,
                isCompleted = newTask.isCompleted
            ))
        }
    }

    fun removeTask(taskId: String) {
        viewModelScope.launch {
            val currentPlan = _trainingPlan.value ?: return@launch
            val updatedPlan = currentPlan.copy(tasks = currentPlan.tasks.filter { it.id != taskId })
            _trainingPlan.value = updatedPlan
            db.trainingDao().deleteTaskById(taskId)
        }
    }

    fun toggleTaskCompletion(taskId: String) {
        viewModelScope.launch {
            val currentPlan = _trainingPlan.value ?: return@launch
            val updatedTasks = currentPlan.tasks.map { if (it.id == taskId) it.copy(isCompleted = !it.isCompleted) else it }
            _trainingPlan.value = currentPlan.copy(tasks = updatedTasks)
            
            val entity = db.trainingDao().getTasksForPlan(currentPlan.date).find { it.id == taskId }
            if (entity != null) {
                db.trainingDao().updateTask(entity.copy(isCompleted = !entity.isCompleted))
            }
        }
    }

    private suspend fun savePlanToDb(plan: DailyTrainingPlan) {
        db.trainingDao().insertPlan(TrainingPlanEntity(plan.date))
        val taskEntities = plan.tasks.map { 
            TrainingTaskEntity(
                id = it.id,
                planDate = plan.date,
                title = it.title,
                description = it.description,
                category = it.category,
                difficulty = it.difficulty,
                estimatedTime = it.estimatedTime,
                type = it.type.name,
                isCompleted = it.isCompleted
            )
        }
        db.trainingDao().insertTasks(taskEntities)
    }

    fun saveUsernameAndFetch(username: String) {
        val error = validateUsernameUseCase(username)
        if (error != null) {
            _uiState.value = UiState.Error(error)
            return
        }
        viewModelScope.launch {
            db.preferenceDao().setPreference(AppPreferenceEntity("last_username", username))
            fetchProfile(username)
        }
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
        viewModelScope.launch {
            _uiState.value = UiState.Onboarding
        }
    }
}
