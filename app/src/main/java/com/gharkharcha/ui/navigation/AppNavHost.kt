package com.gharkharcha.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.gharkharcha.ui.home.HomeScreen
import com.gharkharcha.ui.labour.LabourersScreen
import com.gharkharcha.ui.more.MoreScreen
import com.gharkharcha.ui.projects.ProjectEditScreen
import com.gharkharcha.ui.projects.ProjectsScreen
import com.gharkharcha.ui.works.WorkDetailScreen
import com.gharkharcha.ui.works.WorkEditScreen
import com.gharkharcha.ui.works.WorksScreen

@Composable
fun AppNavHost(
    navController: NavHostController,
    onNavigateTopLevel: (TopLevelDestination) -> Unit,
) {
    val back: () -> Unit = { navController.popBackStack() }
    val addProject = { navController.navigate(ProjectEditRoute()) }
    val manageProjects = { navController.navigate(ProjectsRoute) }

    NavHost(navController = navController, startDestination = HomeRoute) {
        composable<HomeRoute> {
            HomeScreen(
                onAddProject = addProject,
                onManageProjects = manageProjects,
                onOpenWork = { navController.navigate(WorkDetailRoute(it)) },
                onSeeAllWorks = { onNavigateTopLevel(TopLevelDestination.WORKS) },
                onAddWork = { projectId -> navController.navigate(WorkEditRoute(projectId = projectId)) },
            )
        }
        composable<WorksRoute> {
            WorksScreen(
                onAddProject = addProject,
                onManageProjects = manageProjects,
                onOpenWork = { navController.navigate(WorkDetailRoute(it)) },
                onAddWork = { projectId -> navController.navigate(WorkEditRoute(projectId = projectId)) },
                onEditWork = { work -> navController.navigate(WorkEditRoute(projectId = work.projectId, workId = work.id)) },
            )
        }
        composable<LabourRoute> {
            LabourersScreen(onAddProject = addProject, onManageProjects = manageProjects)
        }
        composable<MoreRoute> {
            MoreScreen(onOpenProjects = manageProjects)
        }

        composable<ProjectsRoute> {
            ProjectsScreen(
                onBack = back,
                onAddProject = addProject,
                onEditProject = { navController.navigate(ProjectEditRoute(it)) },
            )
        }
        composable<ProjectEditRoute> {
            ProjectEditScreen(onDone = back)
        }

        composable<WorkDetailRoute> {
            WorkDetailScreen(
                onBack = back,
                onEdit = { work -> navController.navigate(WorkEditRoute(projectId = work.projectId, workId = work.id)) },
            )
        }
        composable<WorkEditRoute> {
            WorkEditScreen(onDone = back)
        }
    }
}
