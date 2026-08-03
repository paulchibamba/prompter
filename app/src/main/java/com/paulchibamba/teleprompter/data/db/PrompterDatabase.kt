package com.paulchibamba.teleprompter.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ScriptEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class PrompterDatabase : RoomDatabase() {

    abstract fun scriptDao(): ScriptDao

    companion object {
        const val NAME = "prompter.db"

        fun build(context: Context): PrompterDatabase =
            Room.databaseBuilder(context.applicationContext, PrompterDatabase::class.java, NAME)
                .build()
    }
}
