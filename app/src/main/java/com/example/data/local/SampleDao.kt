package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SampleDao {

    @Query("SELECT * FROM rice_samples WHERE researcherUid = :uid ORDER BY updatedAtMillis DESC")
    fun observeAllSamples(uid: String): Flow<List<RiceSampleEntity>>

    @Query(
        "SELECT * FROM rice_samples WHERE researcherUid = :uid AND riceVariety = :variety ORDER BY updatedAtMillis DESC"
    )
    fun observeSamplesByVariety(uid: String, variety: String): Flow<List<RiceSampleEntity>>

    @Query("SELECT * FROM rice_samples WHERE id = :id LIMIT 1")
    fun observeSampleById(id: Long): Flow<RiceSampleEntity?>

    @Query("SELECT * FROM rice_samples WHERE id = :id LIMIT 1")
    suspend fun getSampleById(id: Long): RiceSampleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSample(sample: RiceSampleEntity): Long

    @Update
    suspend fun updateSample(sample: RiceSampleEntity)

    @Query("DELETE FROM rice_samples WHERE id = :id")
    suspend fun deleteSampleById(id: Long)
}
