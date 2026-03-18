package com.example.moviexplorer.data.local

import androidx.room.*
import com.example.moviexplorer.data.Rental
import kotlinx.coroutines.flow.Flow

@Dao
interface RentalDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRental(rental: Rental)

    @Query("SELECT * FROM rentals")
    fun getAllRentals(): Flow<List<Rental>>

    @Update
    suspend fun updateRental(rental: Rental)

    @Delete
    suspend fun deleteRental(rental: Rental)
    
    @Query("SELECT * FROM rentals")
    suspend fun getAllRentalsSync(): List<Rental>
}
