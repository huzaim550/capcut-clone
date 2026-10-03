package com.reelforge.app

import android.content.Context
import com.reelforge.core.database.ProjectRepository
import com.reelforge.core.database.createProjectRepository

/**
 * The app's object graph, built by hand.
 *
 * At this size a DI framework would be all ceremony and no payoff: there is one
 * database, one repository, and one place that constructs them. If the graph
 * grows teeth, this is the seam to replace with generated wiring -- nothing else
 * in the app refers to the concrete types.
 */
class AppContainer(context: Context) {

    val projectRepository: ProjectRepository = createProjectRepository(context.applicationContext)
}
