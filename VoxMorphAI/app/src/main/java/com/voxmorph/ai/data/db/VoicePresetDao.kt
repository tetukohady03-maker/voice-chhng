package com.voxmorph.ai.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.voxmorph.ai.data.model.VoicePreset
import kotlinx.coroutines.flow.Flow

@Dao
interface VoicePresetDao {
    @Query("SELECT * FROM voice_presets ORDER BY isFavorite DESC, isBuiltIn DESC, createdAt DESC")
    fun observeAll(): Flow<List<VoicePreset>>

    @Query("SELECT * FROM voice_presets WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): VoicePreset?

    @Query("SELECT COUNT(*) FROM voice_presets")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(preset: VoicePreset): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(presets: List<VoicePreset>)

    @Update
    suspend fun update(preset: VoicePreset)

    @Delete
    suspend fun delete(preset: VoicePreset)

    @Query("DELETE FROM voice_presets WHERE isBuiltIn = 0")
    suspend fun deleteAllCustomProfiles()

    @Query("DELETE FROM voice_presets WHERE isBuiltIn = 1")
    suspend fun deleteAllBuiltIn()
}
