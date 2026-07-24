package com.gooludou.shadowplanner.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gooludou.shadowplanner.project.ProjectRepository
import com.gooludou.shadowplanner.project.ProjectSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

@HiltViewModel
class ProjectListViewModel @Inject constructor(private val repository: ProjectRepository) :
    ViewModel() {
    private val _projects = MutableStateFlow<List<ProjectSummary>>(emptyList())
    val projects: StateFlow<List<ProjectSummary>> = _projects.asStateFlow()

    fun refresh() {
        viewModelScope.launch { _projects.value = repository.listProjects() }
    }

    fun deleteProject(id: String) {
        viewModelScope.launch {
            runCatching {
                repository.deleteProject(id)
                repository.listProjects()
            }.onSuccess { projects ->
                _projects.value = projects
            }
        }
    }

    init {
        viewModelScope.launch {
            repository.projectChanges()
                .onStart { emit(Unit) }
                .collect { _projects.value = repository.listProjects() }
        }
    }
}
