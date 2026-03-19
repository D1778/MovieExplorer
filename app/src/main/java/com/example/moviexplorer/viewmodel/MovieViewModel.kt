package com.example.moviexplorer.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moviexplorer.data.Movie
import com.example.moviexplorer.data.Rental
import com.example.moviexplorer.data.repository.MovieRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MovieViewModel(private val repository: MovieRepository) : ViewModel() {

    private val _movies = MutableStateFlow<List<Movie>>(emptyList())
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.HIGH_RATING) // Default to HIGH_RATING
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    val movies: StateFlow<List<Movie>> = combine(_movies, _searchQuery, _sortOrder) { movies, query, sort ->
        var filtered = if (query.isEmpty()) {
            movies
        } else {
            movies.filter { it.title?.contains(query, ignoreCase = true) == true }
        }

        when (sort) {
            SortOrder.HIGH_RATING -> filtered.sortedByDescending { it.displayRating }
            SortOrder.LOW_RATING -> filtered.sortedBy { it.displayRating }
            SortOrder.NONE -> filtered
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val rentals: StateFlow<List<Rental>> = repository.getRentals()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        fetchMovies()
    }

    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun onSortOrderChange(newSort: SortOrder) {
        _sortOrder.value = newSort // Removed toggle logic to ensure selection is active
    }

    fun fetchMovies() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _movies.value = repository.getMovies()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadMockData() {
        _movies.value = listOf(
            Movie(1, "Inception", "/edv3bs9EsnSbs8Y2Sdf6pBovp9V.jpg", 8.8, "A thief who steals corporate secrets..."),
            Movie(2, "Interstellar", "/gEU2QniE6EwfVDxCzs25asSSTr7.jpg", 8.6, "A team of explorers travel through a wormhole..."),
            Movie(3, "The Dark Knight", "/qJ2tW6qS7OX9G7jSjao9hB0SveW.jpg", 9.0, "Batman raises the stakes in his war on crime..."),
            Movie(4, "The Godfather", "/3bhkrjOiERvSTq9kP1yS07p5jYm.jpg", 9.2, "The aging patriarch of an organized crime dynasty transfers control of his clandestine empire to his reluctant son."),
            Movie(5, "The Shawshank Redemption", "/q6y0Go1tsYKoH6n607Mv0SfszJu.jpg", 9.3, "Two imprisoned men bond over a number of years, finding solace and eventual redemption through acts of common decency.")
        )
    }

    fun rentMovie(movie: Movie) {
        viewModelScope.launch {
            try {
                val rental = Rental(
                    id = movie.getStableId(),
                    title = movie.title ?: "Unknown",
                    posterUrl = movie.fullPosterUrl,
                    rating = movie.displayRating,
                    days = 1
                )
                repository.addRental(rental)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateRentalDays(rental: Rental, increment: Boolean) {
        viewModelScope.launch {
            val newDays = if (increment) rental.days + 1 else (rental.days - 1).coerceAtLeast(1)
            repository.updateRental(rental.copy(days = newDays))
        }
    }

    fun removeRental(rental: Rental) {
        viewModelScope.launch {
            repository.deleteRental(rental)
        }
    }
}

enum class SortOrder {
    NONE, HIGH_RATING, LOW_RATING
}
