package com.reelforge.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reelforge.core.common.IdGenerator
import com.reelforge.core.common.UuidGenerator
import com.reelforge.core.common.onFailure
import com.reelforge.core.common.onSuccess
import com.reelforge.core.database.ProjectRepository
import com.reelforge.core.model.Project
import com.reelforge.core.model.ProjectId
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Drives the project browser.
 *
 * The list is a live view of the repository, so creating or deleting a project
 * updates the screen without any manual refresh logic. Ids and the clock are
 * injected so tests can assert on values instead of whatever `UUID` and
 * `System.currentTimeMillis` happen to produce.
 */
class HomeViewModel(
    private val repository: ProjectRepository,
    private val idGenerator: IdGenerator = UuidGenerator,
    private val now: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    private val loadError = MutableStateFlow<String?>(null)

    val uiState: StateFlow<HomeUiState> = combine(
        repository.observeSummaries().catch {
            // A read failure is reported in the state rather than crashing the
            // screen; an empty list plus a message is a usable, honest degrade.
            loadError.value = "Couldn't load your projects."
            emit(emptyList())
        },
        loadError,
    ) { projects, error ->
        HomeUiState(isLoading = false, projects = projects, errorMessage = error)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = HomeUiState(),
    )

    private val _events = Channel<HomeEvent>(Channel.BUFFERED)
    val events: Flow<HomeEvent> = _events.receiveAsFlow()

    /** Creates an empty project, then asks the host to open it. */
    fun createProject() {
        viewModelScope.launch {
            val timestamp = now()
            val project = Project(
                id = ProjectId(idGenerator.newId()),
                name = DEFAULT_PROJECT_NAME,
                createdAtMs = timestamp,
                updatedAtMs = timestamp,
            )
            repository.save(project)
                .onSuccess { _events.send(HomeEvent.OpenProject(project.id)) }
                .onFailure { _events.send(HomeEvent.ShowMessage(it.userMessage)) }
        }
    }

    /** Deletes [id]. The list updates itself; only failure needs reporting. */
    fun deleteProject(id: ProjectId) {
        viewModelScope.launch {
            repository.delete(id).onFailure { _events.send(HomeEvent.ShowMessage(it.userMessage)) }
        }
    }

    companion object {
        const val DEFAULT_PROJECT_NAME = "Untitled project"
        private const val STOP_TIMEOUT_MS = 5_000L
    }
}
