package com.example.dogbreedsgallery.integration

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.example.dogbreedsgallery.data.BreedRepository
import com.example.dogbreedsgallery.data.local.AppDatabase
import com.example.dogbreedsgallery.data.local.BreedDao
import com.example.dogbreedsgallery.data.local.BreedEntity
import com.example.dogbreedsgallery.data.remote.DogApiService
import com.example.dogbreedsgallery.ui.breeds.BreedsViewModel
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class BreedIntegrationTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var db: AppDatabase
    private lateinit var dao: BreedDao
    private lateinit var repository: BreedRepository
    private val api: DogApiService = mockk(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.breedDao()
        repository = BreedRepository(dao, api)
    }

    @After
    fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    @Test
    fun testRoomAndRepositoryFlow() = runTest {
        val breed = BreedEntity(
            id = 1,
            name = "Labrador",
            temperament = "Friendly",
            origin = "Canada",
            lifeSpan = "10-12 years",
            description = "Good boy",
            imageUrl = "url",
            isFavorite = false
        )

        repository.allBreeds.test {
            assertEquals(emptyList<BreedEntity>(), awaitItem())

            dao.insertBreeds(listOf(breed))

            val items = awaitItem()
            assertEquals(1, items.size)
            assertEquals("Labrador", items[0].name)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun testRepositoryToggleFavoriteUpdatesRoom() = runTest {
        val breed = BreedEntity(
            id = 2,
            name = "Poodle",
            temperament = "Smart",
            origin = "France",
            lifeSpan = "12-15 years",
            description = "Smart dog",
            imageUrl = "url",
            isFavorite = false
        )

        dao.insertBreeds(listOf(breed))

        repository.favoriteBreeds.test {
            assertEquals(emptyList<BreedEntity>(), awaitItem())

            repository.toggleFavorite(2, false)

            val favorites = awaitItem()
            assertEquals(1, favorites.size)
            assertTrue(favorites[0].isFavorite)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun testViewModelWithRealRepositoryAndRoom() = runTest {
        val breed = BreedEntity(
            id = 3,
            name = "Husky",
            temperament = "Energetic",
            origin = "Siberia",
            lifeSpan = "12-14 years",
            description = "Sled dog",
            imageUrl = "url",
            isFavorite = false
        )

        dao.insertBreeds(listOf(breed))

        val viewModel = BreedsViewModel(repository)

        viewModel.uiState.test {
            // Ждем, пока ViewModel прогрузит данные из Room
            testDispatcher.scheduler.advanceUntilIdle()

            // Игнорируем пустые состояния (initial value), ждем, пока придут данные
            var state = awaitItem()
            while (state.breeds.isEmpty()) {
                state = awaitItem()
            }

            // Проверяем, что данные пришли правильные
            assertEquals(1, state.breeds.size)
            assertEquals("Husky", state.breeds[0].name)
        }
    }
}
