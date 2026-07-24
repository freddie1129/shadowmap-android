package com.gooludou.shadowplanner.presentation

import com.gooludou.shadowplanner.project.ProjectRepository
import com.gooludou.shadowplanner.project.ProjectSnapshot
import com.gooludou.shadowplanner.project.ProjectSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProjectListViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun projectChange_refreshesTheProjectList() = runTest(dispatcher) {
        val repository = FakeProjectRepository()
        val viewModel = ProjectListViewModel(repository)
        advanceUntilIdle()

        repository.projects += PROJECT_SUMMARY
        repository.changes.emit(Unit)
        advanceUntilIdle()

        assertEquals(listOf(PROJECT_SUMMARY), viewModel.projects.value)
    }

    private class FakeProjectRepository : ProjectRepository {
        val projects = mutableListOf<ProjectSummary>()
        val changes = MutableSharedFlow<Unit>()

        override fun listProjects(): List<ProjectSummary> = projects.toList()

        override fun projectChanges(): Flow<Unit> = changes

        override fun loadProject(id: String): ProjectSnapshot = error("Not used in this test")

        override fun saveProject(project: ProjectSnapshot) = Unit

        override fun deleteProject(id: String) = Unit
    }

    private companion object {
        val PROJECT_SUMMARY = ProjectSummary(
            id = "project-id",
            name = "Test project",
            locationLabel = "Brisbane QLD",
            updatedAt = 1_752_640_000_000L,
            fileName = "project-id.json"
        )
    }
}
