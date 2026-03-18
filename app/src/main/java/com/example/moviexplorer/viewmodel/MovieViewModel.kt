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
    val movies: StateFlow<List<Movie>> = _movies.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val rentals: StateFlow<List<Rental>> = repository.getRentals()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        fetchMovies()
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
            Movie(3, "The Dark Knight", "/qJ2tW6qS7OX9G7jSjao9hB0SveW.jpg", 9.0, "Batman raises the stakes in his war on crime...")
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
