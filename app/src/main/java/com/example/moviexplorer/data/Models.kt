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
    val cast: List<CastMember> = listOf(
        CastMember("Actor A", "Lead Role", "https://i.pravatar.cc/150?u=a"),
        CastMember("Actor B", "Supporting", "https://i.pravatar.cc/150?u=b"),
        CastMember("Actor C", "Director", "https://i.pravatar.cc/150?u=c"),
        CastMember("Actor D", "Producer", "https://i.pravatar.cc/150?u=d")
    ),
    // Extra scrollable content: Movie stills/Gallery
    val galleryStills: List<String> = listOf(
        "https://images.unsplash.com/photo-1485846234645-a62644f84728?q=80&w=2059&auto=format&fit=crop",
        "https://images.unsplash.com/photo-1478720568477-151d9b1b7463?q=80&w=2070&auto=format&fit=crop",
        "https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?q=80&w=2070&auto=format&fit=crop",
        "https://images.unsplash.com/photo-1536440136628-849c177e76a1?q=80&w=2000&auto=format&fit=crop"
    )
) {
    val fullPosterUrl: String get() = when {
        posterPath.isNullOrEmpty() -> ""
        posterPath!!.startsWith("http") -> posterPath!!
        else -> "https://image.tmdb.org/t/p/w500$posterPath"
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
