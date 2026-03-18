package com.example.moviexplorer.data.remote

import com.google.gson.JsonElement
import retrofit2.http.GET

interface MovieApiService {
    @GET("movies")
    suspend fun getMoviesRaw(): JsonElement
}
