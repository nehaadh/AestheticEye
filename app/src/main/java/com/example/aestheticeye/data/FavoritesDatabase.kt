package com.example.aestheticeye.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoritesDao {
    @Query("SELECT * FROM favorites ORDER BY createdAt DESC")
    fun getAllFavorites(): Flow<List<FavoriteScene>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(scene: FavoriteScene): Long

    @Delete
    suspend fun delete(scene: FavoriteScene)

    @Query("DELETE FROM favorites WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("SELECT * FROM favorites WHERE id = :id")
    suspend fun getById(id: Int): FavoriteScene?
}

@Database(entities = [FavoriteScene::class], version = 2, exportSchema = false)
abstract class FavoritesDatabase : RoomDatabase() {
    abstract fun favoritesDao(): FavoritesDao

    companion object {
        @Volatile private var INSTANCE: FavoritesDatabase? = null

        fun getInstance(context: android.content.Context): FavoritesDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FavoritesDatabase::class.java,
                    "favorites_db"
                )
                    // Wipes and recreates the DB if schema changes —
                    // safe to use here since this is a new feature with no existing user data.
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}