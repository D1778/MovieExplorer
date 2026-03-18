package com.example.moviexplorer.data.repository

import com.example.moviexplorer.data.Movie
import com.example.moviexplorer.data.Rental
import com.example.moviexplorer.data.local.RentalDao
import com.example.moviexplorer.data.remote.MovieApiService
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow

class MovieRepository(
    private val apiService: MovieApiService,
    private val rentalDao: RentalDao
) {
    private val gson = Gson()

    suspend fun getMovies(): List<Movie> {
        return try {
            val jsonElement = apiService.getMoviesRaw()
            when {
                jsonElement.isJsonArray -> {
                    val type = object : TypeToken<List<Movie>>() {}.type
                    gson.fromJson(jsonElement, type)
                }
                jsonElement.isJsonObject -> {
                    val jsonObject = jsonElement.asJsonObject
                    val keys = listOf("results", "movies", "data", "items")
                    var list: List<Movie>? = null
                    for (key in keys) {
                        if (jsonObject.has(key) && jsonObject.get(key).isJsonArray) {
                            val type = object : TypeToken<List<Movie>>() {}.type
                            list = gson.fromJson(jsonObject.get(key), type)
                            break
                        }
                    }
                    list ?: emptyList()
                }
                else -> emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun getRentals(): Flow<List<Rental>> = rentalDao.getAllRentals()

    suspend fun addRental(rental: Rental) = rentalDao.insertRental(rental)

    suspend fun updateRental(rental: Rental) = rentalDao.updateRental(rental)

    suspend fun deleteRental(rental: Rental) = rentalDao.deleteRental(rental)
}
