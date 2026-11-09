package com.example.dogbreedsgallery.ui.breeds

import app.cash.turbine.test
import com.example.dogbreedsgallery.data.BreedRepository
import com.example.dogbreedsgallery.data.local.BreedEntity
import io.mockk.coEvery
import io.mockk.coVerify
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
    private val repository: BreedRepository = mockk(relaxed = true)

    private val breedsFlow = MutableStateFlow<List<BreedEntity>>(emptyList())

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        coEvery { repository.allBreeds } returns breedsFlow
        coEvery { repository.refreshBreeds() } returns Result.success(Unit)
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

        val viewModel = BreedsViewModel(repository)

        viewModel.uiState.test {
            awaitItem() // initial state from init { refresh() }

            breedsFlow.value = sampleBreeds

            val emission = awaitItem()
            assertEquals(2, emission.breeds.size)
            assertEquals("", emission.searchQuery)

            viewModel.onSearchQueryChange("")
            val queryEmission = awaitItem()
            assertEquals(2, queryEmission.breeds.size)
            assertEquals("", queryEmission.searchQuery)

            cancelAndIgnoreRemainingEvents()
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
        val viewModel = BreedsViewModel(repository)

        viewModel.uiState.test {
            awaitItem() // initial

            viewModel.onSearchQueryChange("Golden")
            val emission = awaitItem()
            assertEquals(1, emission.breeds.size)
            assertEquals("Golden Retriever", emission.breeds[0].name)
            assertEquals("Golden", emission.searchQuery)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun searchFilteringIgnoreCase() = runTest {
        val sampleBreeds = listOf(
            BreedEntity(1, "Golden Retriever", "Friendly", "UK", "12 years", "Desc", "url", false),
            BreedEntity(2, "German Shepherd", "Alert", "Germany", "13 years", "Desc", "url", false),
            BreedEntity(3, "Bulldog", "Calm", "UK", "10 years", "Desc", "url", false)
        )

        breedsFlow.value = sampleBreeds
        val viewModel = BreedsViewModel(repository)

        viewModel.uiState.test {
            awaitItem() // initial

            viewModel.onSearchQueryChange("golden")
            val emission = awaitItem()
            assertEquals(1, emission.breeds.size)
            assertEquals("Golden Retriever", emission.breeds[0].name)
            assertEquals("golden", emission.searchQuery)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun toggleFavoriteCallsRepository() = runTest {
        val breed = BreedEntity(1, "Bulldog", "Calm", "UK", "10 years", "Desc", "url", false)
        val viewModel = BreedsViewModel(repository)

        viewModel.uiState.test {
            awaitItem() // initial

            viewModel.toggleFavorite(breed)

            coVerify(exactly = 1) { repository.toggleFavorite(1, false) }

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun refreshBreedsHandlesError() = runTest {
        val errorMessage = "Network Error"
        coEvery { repository.refreshBreeds() } returns Result.failure(RuntimeException(errorMessage))

        val viewModel = BreedsViewModel(repository)

        viewModel.uiState.test {
            val emission = awaitItem()
            assertEquals(errorMessage, emission.error)

            cancelAndIgnoreRemainingEvents()
        }
    }
}
