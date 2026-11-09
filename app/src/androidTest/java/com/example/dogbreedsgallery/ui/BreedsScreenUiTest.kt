package com.example.dogbreedsgallery.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.dogbreedsgallery.data.local.BreedEntity
import com.example.dogbreedsgallery.ui.breeds.BreedCard
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BreedsScreenUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun breedCardDisplaysNameAndTemperament() {
        val breed = BreedEntity(
            id = 1,
            name = "Golden Retriever",
            temperament = "Friendly, Intelligent, Kind",
            origin = "UK",
            lifeSpan = "10-12 years",
            description = "Great family dog",
            imageUrl = "",
            isFavorite = false
        )

        composeTestRule.setContent {
            BreedCard(
                breed = breed,
                onClick = {},
                onFavoriteToggle = {}
            )
        }

        composeTestRule.onNodeWithText("Golden Retriever").assertIsDisplayed()
        composeTestRule.onNodeWithText("Friendly, Intelligent, Kind").assertIsDisplayed()
    }

    @Test
    fun breedCardDisplaysFavoriteIconAccordingToState() {
        val notFavoriteBreed = BreedEntity(
            id = 1,
            name = "Bulldog",
            temperament = "Calm",
            origin = "UK",
            lifeSpan = "10 years",
            description = "Desc",
            imageUrl = "",
            isFavorite = false
        )

        composeTestRule.setContent {
            BreedCard(
                breed = notFavoriteBreed,
                onClick = {},
                onFavoriteToggle = {}
            )
        }

        composeTestRule.onNodeWithContentDescription("Избранное").assertIsDisplayed()

        val favoriteBreed = notFavoriteBreed.copy(isFavorite = true)

        composeTestRule.setContent {
            BreedCard(
                breed = favoriteBreed,
                onClick = {},
                onFavoriteToggle = {}
            )
        }

        composeTestRule.onNodeWithContentDescription("Избранное").assertIsDisplayed()
    }
}
