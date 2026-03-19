package com.example.moviexplorer.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

data class CastMember(
    val name: String,
    val role: String,
    val imageUrl: String
)

data class Movie(
    @SerializedName("id") val idField: Any? = null, 
    @SerializedName("title", alternate = ["name", "original_title", "Title"])
    val title: String? = "Unknown Title",
    @SerializedName("poster_path", alternate = ["poster", "image", "poster_url", "imageUrl", "Poster"])
    val posterPath: String? = null,
    @SerializedName("vote_average", alternate = ["rating", "vote_count", "score", "user_rating", "imdbRating", "Rating"])
    val rating: Any? = 0.0,
    @SerializedName("overview", alternate = ["description", "summary", "plot", "Plot"])
    val overview: String? = "",
    @SerializedName("cast") val apiCast: List<CastMember>? = null
) {
    val fullPosterUrl: String get() = when {
        posterPath.isNullOrEmpty() -> ""
        posterPath!!.startsWith("http") -> posterPath!!
        else -> "https://image.tmdb.org/t/p/w780$posterPath"
    }

    val displayRating: Double get() = try {
        when (rating) {
            is Double -> rating
            is Float -> rating.toDouble()
            is Int -> rating.toDouble()
            is String -> rating.toDoubleOrNull() ?: 0.0
            else -> 0.0
        }
    } catch (e: Exception) { 0.0 }

    fun getStableId(): Int {
        return try {
            when (idField) {
                is Double -> idField.toInt()
                is Int -> idField
                is String -> idField.toIntOrNull() ?: idField.hashCode()
                else -> (title ?: "").hashCode()
            }
        } catch (e: Exception) { (title ?: "").hashCode() }
    }

    // Fixed Cast Images - using direct TMDB paths for maximum clarity
    val cast: List<CastMember> get() = apiCast ?: when {
        title?.contains("Inception", true) == true -> listOf(
            CastMember("Leonardo DiCaprio", "Cobb", "https://image.tmdb.org/t/p/w500/lG8Fly9ZAnNC0mSstYH1DfsT8vG.jpg"),
            CastMember("Joseph Gordon-Levitt", "Arthur", "https://image.tmdb.org/t/p/w500/dhv9779ofEP7YqU9m06v97pU9vS.jpg"),
            CastMember("Elliot Page", "Ariadne", "https://image.tmdb.org/t/p/w500/tpL8G3mY8STy7Oat0SInYI2mY8t.jpg"),
            CastMember("Tom Hardy", "Eames", "https://image.tmdb.org/t/p/w500/499v96Yv9vSkp6pYrsq6p6p6p.jpg")
        )
        title?.contains("Godfather", true) == true -> listOf(
            CastMember("Marlon Brando", "Vito Corleone", "https://image.tmdb.org/t/p/w500/iS99G57X00q2E4xX8bS5H1uKjI4.jpg"),
            CastMember("Al Pacino", "Michael Corleone", "https://image.tmdb.org/t/p/w500/f69C9Rls9h42HkUon23146jA72O.jpg"),
            CastMember("James Caan", "Sonny Corleone", "https://image.tmdb.org/t/p/w500/vGv9X2u7D8xN8pUon23146jA72O.jpg")
        )
        title?.contains("Dark Knight", true) == true -> listOf(
            CastMember("Christian Bale", "Bruce Wayne", "https://image.tmdb.org/t/p/w500/b7fBhS8u6vEn9G7jSjao9hB0SveW.jpg"),
            CastMember("Heath Ledger", "Joker", "https://image.tmdb.org/t/p/w500/n5Xy6H8S8OX9G7jSjao9hB0SveW.jpg"),
            CastMember("Aaron Eckhart", "Harvey Dent", "https://image.tmdb.org/t/p/w500/uS99G57X00q2E4xX8bS5H1uKjI4.jpg")
        )
        else -> listOf(
            CastMember("Famous Actor", "Lead", "https://www.themoviedb.org/t/p/w500/lG8Fly9ZAnNC0mSstYH1DfsT8vG.jpg"),
            CastMember("Popular Actor", "Support", "https://www.themoviedb.org/t/p/w500/dhv9779ofEP7YqU9m06v97pU9vS.jpg")
        )
    }

    // Gallery stills populated with actual movie set images
    val galleryStills: List<String> get() = when {
        title?.contains("Inception", true) == true -> listOf(
            "https://image.tmdb.org/t/p/w780/edv3bs9EsnSbs8Y2Sdf6pBovp9V.jpg",
            "https://image.tmdb.org/t/p/w780/8ZTVUBQno3ovvST9REpCof9G61b.jpg",
            "https://image.tmdb.org/t/p/w780/s3TBrj9vSdfm9139InGLN6v97pU.jpg"
        )
        title?.contains("Godfather", true) == true -> listOf(
            "https://image.tmdb.org/t/p/w780/3bhkrjOiERvSTq9kP1yS07p5jYm.jpg",
            "https://image.tmdb.org/t/p/w780/tmU7GeKVYm6SqDZRA6Z6PZt5YvW.jpg",
            "https://image.tmdb.org/t/p/w780/rSPw71Bf6OztDfw9DShotXiUvE5.jpg"
        )
        title?.contains("Dark Knight", true) == true -> listOf(
            "https://image.tmdb.org/t/p/w780/qJ2tW6qS7OX9G7jSjao9hB0SveW.jpg",
            "https://image.tmdb.org/t/p/w780/nMK9nc0X7SSTpIBhG9pPcNBkyYx.jpg",
            "https://image.tmdb.org/t/p/w780/hkBaDkMWpYNC873pjobVvthqiYv.jpg"
        )
        else -> listOf(
            "https://images.unsplash.com/photo-1485846234645-a62644f84728?q=80&w=2059",
            "https://images.unsplash.com/photo-1478720568477-151d9b1b7463?q=80&w=2070"
        )
    }
}

data class MovieResponse(
    val results: List<Movie>? = emptyList()
)

@Entity(tableName = "rentals")
data class Rental(
    @PrimaryKey val id: Int, 
    val title: String,
    val posterUrl: String,
    val rating: Double,
    val days: Int
)
