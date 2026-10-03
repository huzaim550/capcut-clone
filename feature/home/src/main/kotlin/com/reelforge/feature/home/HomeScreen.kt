package com.reelforge.feature.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.reelforge.core.common.TimeUtils
import com.reelforge.core.database.ProjectRepository
import com.reelforge.core.designsystem.EmptyState
import com.reelforge.core.designsystem.ReelforgeIcons
import com.reelforge.core.designsystem.ReelforgeTheme
import com.reelforge.core.model.ProjectId
import com.reelforge.core.model.ProjectSummary

/**
 * The public entry point of the browser. The host supplies the repository and a
 * callback for "open this project"; the feature owns the ViewModel, the list, and
 * the empty state.
 */
@Composable
fun HomeRoute(
    repository: ProjectRepository,
    onOpenProject: (ProjectId) -> Unit,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModelFactory(repository)),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is HomeEvent.OpenProject -> onOpenProject(event.id)
                is HomeEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { snackbarHostState.showSnackbar(it) }
    }

    HomeScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onCreateProject = viewModel::createProject,
        onDeleteProject = viewModel::deleteProject,
    )
}

/** Stateless browser surface, so it can be previewed and reasoned about in isolation. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: HomeUiState,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onCreateProject: () -> Unit = {},
    onDeleteProject: (ProjectId) -> Unit = {},
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Projects") }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateProject) {
                Icon(ReelforgeIcons.Add, contentDescription = "New project")
            }
        },
    ) { contentPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
            when {
                state.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

                state.projects.isEmpty() -> EmptyState(
                    title = "No projects yet",
                    message = "Tap + to start your first edit.",
                    icon = ReelforgeIcons.Movie,
                    modifier = Modifier.align(Alignment.Center),
                )

                else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.projects, key = { it.id.value }) { project ->
                        ProjectRow(project = project, onDelete = { onDeleteProject(project.id) })
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun ProjectRow(project: ProjectSummary, onDelete: () -> Unit) {
    // Computed once per project change rather than on every recomposition.
    val editedAgo = remember(project.updatedAtMs) {
        TimeUtils.formatDurationFriendly(System.currentTimeMillis() - project.updatedAtMs)
    }
    ListItem(
        headlineContent = { Text(project.name) },
        supportingContent = { Text("Edited $editedAgo ago") },
        trailingContent = {
            IconButton(onClick = onDelete) {
                Icon(ReelforgeIcons.Delete, contentDescription = "Delete ${project.name}")
            }
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    ReelforgeTheme {
        HomeScreen(
            state = HomeUiState(
                isLoading = false,
                projects = listOf(
                    ProjectSummary(ProjectId("1"), "Holiday cut", 0, 0),
                    ProjectSummary(ProjectId("2"), "Untitled project", 0, 0),
                ),
            ),
        )
    }
}
