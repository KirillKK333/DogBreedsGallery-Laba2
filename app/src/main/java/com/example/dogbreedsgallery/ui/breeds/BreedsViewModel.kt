package com.example.dogbreedsgallery.ui.breeds

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dogbreedsgallery.data.BreedRepository
import com.example.dogbreedsgallery.data.local.BreedEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BreedsUiState(
    val breeds: List<BreedEntity> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val temperamentQuery: String = ""
)

class BreedsViewModel(
    private val repository: BreedRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _temperamentQuery = MutableStateFlow("")
    private val _isRefreshing = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)
    private val _isLoading = MutableStateFlow(false)

    val uiState: StateFlow<BreedsUiState> = combine(
        repository.allBreeds,
        _searchQuery,
        _temperamentQuery,
        _isRefreshing,
        _error,
        _isLoading
    ) { values ->
        val breeds = values[0] as List<BreedEntity>
        val query = values[1] as String
        val temperament = values[2] as String
        val refreshing = values[3] as Boolean
        val error = values[4] as String?
        val loading = values[5] as Boolean

        val filtered = filterBreeds(breeds, query, temperament)

        BreedsUiState(
            breeds = filtered,
            isLoading = loading,
            isRefreshing = refreshing,
            error = error,
            searchQuery = query,
            temperamentQuery = temperament
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BreedsUiState()
    )

    private fun filterBreeds(
        breeds: List<BreedEntity>,
        query: String,
        temperament: String
    ): List<BreedEntity> {
        var filtered = if (query.isBlank()) breeds
        else breeds.filter { it.name.contains(query, ignoreCase = true) }

        if (temperament.isNotBlank()) {
            filtered = filtered.filter { it.temperament.contains(temperament, ignoreCase = true) }
        }
        return filtered
    }

    init {
        refresh()
    }

    fun onSearchQueryChange(q: String) { _searchQuery.value = q }

    fun filterByTemperament(temperament: String) {
        _temperamentQuery.value = temperament
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            _error.value = null
            val result = repository.refreshBreeds()
            if (result.isFailure) {
                _error.value = result.exceptionOrNull()?.message ?: "Ошибка загрузки"
            }
            _isRefreshing.value = false
        }
    }

    fun toggleFavorite(breed: BreedEntity) {
        viewModelScope.launch {
            repository.toggleFavorite(breed.id, breed.isFavorite)
        }
    }
}
