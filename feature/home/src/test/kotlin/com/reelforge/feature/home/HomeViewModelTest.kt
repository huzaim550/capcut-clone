package com.reelforge.feature.home

import com.reelforge.core.common.AppError
import com.reelforge.core.common.IdGenerator
import com.reelforge.core.model.ProjectId
import com.reelforge.core.model.ProjectSummary
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun viewModel(repository: FakeProjectRepository) = HomeViewModel(
        repository = repository,
        idGenerator = IdGenerator { "generated-id" },
        now = { 1_000L },
    )

    @Test
    fun `uiState exposes repository summaries once loaded`() = runTest {
        val repository = FakeProjectRepository().apply {
            summaries.value = listOf(ProjectSummary(ProjectId("a"), "A", 0, 0))
        }
        val viewModel = viewModel(repository)

        val state = viewModel.uiState.first { !it.isLoading }

        assertEquals(listOf(ProjectId("a")), state.projects.map { it.id })
    }

    @Test
    fun `createProject saves with a generated id and asks the host to open it`() = runTest {
        val repository = FakeProjectRepository()
        val viewModel = viewModel(repository)

        viewModel.createProject()

        assertEquals(HomeEvent.OpenProject(ProjectId("generated-id")), viewModel.events.first())
        val saved = repository.saved.single()
        assertEquals(ProjectId("generated-id"), saved.id)
        assertEquals(HomeViewModel.DEFAULT_PROJECT_NAME, saved.name)
        assertEquals(1_000L, saved.createdAtMs)
        assertEquals(1_000L, saved.updatedAtMs)
    }

    @Test
    fun `a failed save surfaces a message instead of opening the project`() = runTest {
        val repository = FakeProjectRepository().apply {
            failSaveWith = AppError.Storage(detail = "disk full")
        }
        val viewModel = viewModel(repository)

        viewModel.createProject()

        assertTrue(viewModel.events.first() is HomeEvent.ShowMessage)
        assertTrue(repository.saved.isEmpty())
    }

    @Test
    fun `deleteProject removes the project through the repository`() = runTest {
        val repository = FakeProjectRepository().apply {
            summaries.value = listOf(ProjectSummary(ProjectId("a"), "A", 0, 0))
        }
        val viewModel = viewModel(repository)

        viewModel.deleteProject(ProjectId("a"))
        advanceUntilIdle()

        assertEquals(listOf(ProjectId("a")), repository.deleted)
    }
}
