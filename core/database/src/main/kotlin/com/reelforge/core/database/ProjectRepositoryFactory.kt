package com.reelforge.core.database

import android.content.Context

/**
 * Builds the production [ProjectRepository] without exposing Room.
 *
 * [ProjectDatabase] and `ProjectDao` are public so Room can generate them, but
 * their supertypes come from `implementation`-scoped dependencies. If a caller
 * outside this module referenced `ProjectDatabase` directly it would have to
 * depend on Room itself, which is exactly the leak this factory prevents. Callers
 * get the repository interface and nothing more.
 */
fun createProjectRepository(context: Context): ProjectRepository =
    ProjectStore(ProjectDatabase.create(context.applicationContext).projectDao())
