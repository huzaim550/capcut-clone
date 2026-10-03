package com.reelforge.feature.home

import com.reelforge.core.model.ProjectId
import com.reelforge.core.model.ProjectSummary

/**
 * Everything the browser renders. Held as one immutable value so a recomposition
 * is always a pure function of a single snapshot -- there is no way for
 * `projects` and `isLoading` to disagree about what is on screen.
 */
data class HomeUiState(
    val isLoading: Boolean = true,
    val projects: List<ProjectSummary> = emptyList(),
    val errorMessage: String? = null,
)

/** One-shot things that happen once and should not survive a recomposition. */
sealed interface HomeEvent {
    /** The project was created successfully; the host should navigate to it. */
    data class OpenProject(val id: ProjectId) : HomeEvent

    /** Something went wrong and the user should see a transient message. */
    data class ShowMessage(val message: String) : HomeEvent
}
