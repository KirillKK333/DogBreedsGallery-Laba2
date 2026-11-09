package com.example.dogbreedsgallery.ui.breeds

import app.cash.turbine.test
import com.example.dogbreedsgallery.data.BreedRepository
import com.example.dogbreedsgallery.data.local.BreedEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BreedsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: BreedRepository
    private lateinit var viewModel: BreedsViewModel

    private val breedsFlow = MutableStateFlow<List<BreedEntity>>(emptyList())

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        every { repository.allBreeds } returns breedsFlow
        coEvery { repository.refreshBreeds() } returns Result.success(Unit)
        viewModel = BreedsViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun emptySearchQueryReturnsAllBreeds() = runTest {
        val sampleBreeds = listOf(
            BreedEntity(1, "Bulldog", "Gentle", "UK", "10 years", "Desc", "url", false),
            BreedEntity(2, "Beagle", "Friendly", "UK", "12 years", "Desc", "url", false)
        )

        viewModel.uiState.test {
            expectMostRecentItem() // Ждем начального состояния (пустой список)

            // Теперь, когда мы подписаны, обновляем данные в репозитории
            breedsFlow.value = sampleBreeds
            testDispatcher.scheduler.advanceUntilIdle() // Даем ViewModel время обработать изменение

            val state = expectMostRecentItem()
            assertEquals(2, state.breeds.size)
            assertEquals("", state.searchQuery)
        }
    }

    @Test
    fun searchFilteringCaseSensitive() = runTest {
        val sampleBreeds = listOf(
            BreedEntity(1, "Golden Retriever", "Friendly", "UK", "12 years", "Desc", "url", false),
            BreedEntity(2, "German Shepherd", "Alert", "Germany", "13 years", "Desc", "url", false),
            BreedEntity(3, "Bulldog", "Calm", "UK", "10 years", "Desc", "url", false)
        )

        breedsFlow.value = sampleBreeds

        viewModel.uiState.test {
            expectMostRecentItem() // Ждем начальной загрузки данных

            viewModel.onSearchQueryChange("Golden")
            testDispatcher.scheduler.advanceUntilIdle()

            val state = expectMostRecentItem()
            assertEquals(1, state.breeds.size)
            assertEquals("Golden Retriever", state.breeds[0].name)
        }
    }

    @Test
    fun searchFilteringIgnoreCase() = runTest {
        val sampleBreeds = listOf(
            BreedEntity(1, "Golden Retriever", "Friendly", "UK", "12 years", "Desc", "url", false),
            BreedEntity(2, "German Shepherd", "Alert", "Germany", "13 years", "Desc", "url", false)
        )

        breedsFlow.value = sampleBreeds

        viewModel.uiState.test {
            expectMostRecentItem()

            viewModel.onSearchQueryChange("golden")
            testDispatcher.scheduler.advanceUntilIdle()

            val state = expectMostRecentItem()
            assertEquals(1, state.breeds.size)
            assertEquals("Golden Retriever", state.breeds[0].name)
        }
    }

    @Test
    fun toggleFavoriteCallsRepository() = runTest {
        val breed = BreedEntity(1, "Bulldog", "Calm", "UK", "10 years", "Desc", "url", false)

        viewModel.toggleFavorite(breed)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { repository.toggleFavorite(1, false) }
    }

    @Test
    fun refreshBreedsHandlesError() = runTest {
        val errorMessage = "Network Error"
        coEvery { repository.refreshBreeds() } returns Result.failure(RuntimeException(errorMessage))

        // Создаем заново, чтобы init { refresh() } сработал с ошибкой
        val errorViewModel = BreedsViewModel(repository)

        errorViewModel.uiState.test {
            testDispatcher.scheduler.advanceUntilIdle()
            val state = expectMostRecentItem()
            assertEquals(errorMessage, state.error)
        }
    }
}