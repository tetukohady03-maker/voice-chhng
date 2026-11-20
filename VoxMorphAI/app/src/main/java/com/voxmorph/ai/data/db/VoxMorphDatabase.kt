package com.voxmorph.ai.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.voxmorph.ai.data.model.VoicePreset

@Database(entities = [VoicePreset::class], version = 1, exportSchema = false)
abstract class VoxMorphDatabase : RoomDatabase() {
    abstract fun voicePresetDao(): VoicePresetDao

    companion object {
        @Volatile
        private var INSTANCE: VoxMorphDatabase? = null

        fun getInstance(context: Context): VoxMorphDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    VoxMorphDatabase::class.java,
                    "voxmorph_ai.db"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
    }
}
