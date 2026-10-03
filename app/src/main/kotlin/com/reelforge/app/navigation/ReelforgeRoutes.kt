package com.reelforge.app.navigation

/**
 * Navigation routes, named in one place so the pattern used by `composable` and
 * the string built when navigating cannot drift apart.
 */
object ReelforgeRoutes {
    const val HOME = "home"

    const val ARG_PROJECT_ID = "projectId"
    const val EDITOR_PATTERN = "editor/{$ARG_PROJECT_ID}"

    fun editor(projectId: String): String = "editor/$projectId"
}
