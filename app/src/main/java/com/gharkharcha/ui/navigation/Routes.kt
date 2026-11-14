package com.gharkharcha.ui.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.gharkharcha.R
import kotlinx.serialization.Serializable

// Top-level destinations (bottom bar / rail).
@Serializable data object HomeRoute
@Serializable data object WorksRoute
@Serializable data object LabourRoute
@Serializable data object MoreRoute

// Projects
@Serializable data object ProjectsRoute
@Serializable data class ProjectEditRoute(val projectId: Long = 0)

// Works
@Serializable data class WorkDetailRoute(val workId: Long)
@Serializable data class WorkEditRoute(val projectId: Long, val workId: Long = 0)

enum class TopLevelDestination(
    val route: Any,
    @param:DrawableRes val icon: Int,
    @param:StringRes val label: Int,
) {
    HOME(HomeRoute, R.drawable.ic_home, R.string.nav_home),
    WORKS(WorksRoute, R.drawable.ic_build, R.string.nav_works),
    LABOUR(LabourRoute, R.drawable.ic_groups, R.string.nav_labour),
    MORE(MoreRoute, R.drawable.ic_more_horiz, R.string.nav_more),
}
