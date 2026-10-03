package com.reelforge.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.reelforge.core.database.ProjectRepository

/**
 * Builds [HomeViewModel] from the repository the host provides.
 *
 * Hand-written rather than using a DI framework: there is exactly one dependency,
 * and a factory this small is easier to read than the annotation processing it
 * would replace.
 */
class HomeViewModelFactory(
    private val repository: ProjectRepository,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        return HomeViewModel(repository) as T
    }
}
