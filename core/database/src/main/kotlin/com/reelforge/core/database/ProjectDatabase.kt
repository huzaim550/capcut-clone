package com.reelforge.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * The Room database. Version 1 is the initial schema; the exported JSON under
 * `schemas/` is the artefact future migrations are written against.
 *
 * Access is only through [ProjectStore]; this class exists so Room can generate
 * the implementation and so the app has one place to construct it.
 */
@Database(
    entities = [ProjectEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class ProjectDatabase : RoomDatabase() {

    abstract fun projectDao(): ProjectDao

    companion object {
        /** The on-disk database used by the app. */
        fun create(context: Context, name: String = "reelforge.db"): ProjectDatabase =
            Room.databaseBuilder(context.applicationContext, ProjectDatabase::class.java, name).build()

        /** An in-memory database for tests and previews. Nothing is persisted. */
        fun inMemory(context: Context): ProjectDatabase =
            Room.inMemoryDatabaseBuilder(context.applicationContext, ProjectDatabase::class.java).build()
    }
}
