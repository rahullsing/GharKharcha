package com.gharkharcha.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.gharkharcha.ui.home.HomeScreen
import com.gharkharcha.ui.attendance.AttendanceScreen
import com.gharkharcha.ui.labour.LabourerEditScreen
import com.gharkharcha.ui.labour.LabourerLedgerScreen
import com.gharkharcha.ui.labour.LabourersScreen
import com.gharkharcha.ui.payments.PaymentEditScreen
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
    val addLabourer = { navController.navigate(LabourerEditRoute()) }
    val openLabourer: (Long) -> Unit = { navController.navigate(LabourerLedgerRoute(it)) }
    val openPayment: (Long, Long) -> Unit = { labourerId, paymentId ->
        navController.navigate(PaymentEditRoute(labourerId = labourerId, paymentId = paymentId))
    }

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
            LabourersScreen(
                onAddProject = addProject,
                onManageProjects = manageProjects,
                onAddLabourer = addLabourer,
                onOpenLabourer = openLabourer,
                onMarkAttendance = { navController.navigate(AttendanceRoute()) },
            )
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
                onMarkAttendance = { navController.navigate(AttendanceRoute(workId = it)) },
                onOpenLabourer = openLabourer,
                onOpenPayment = openPayment,
            )
        }
        composable<WorkEditRoute> {
            WorkEditScreen(onDone = back)
        }

        composable<LabourerEditRoute> {
            LabourerEditScreen(
                onBack = back,
                onSaved = { id, isNew ->
                    // A new labourer opens their ledger; an edit just returns.
                    if (isNew) {
                        navController.navigate(LabourerLedgerRoute(id)) {
                            popUpTo<LabourerEditRoute> { inclusive = true }
                        }
                    } else {
                        navController.popBackStack()
                    }
                },
            )
        }
        composable<LabourerLedgerRoute> {
            LabourerLedgerScreen(
                onBack = back,
                onEdit = { navController.navigate(LabourerEditRoute(it)) },
                onAddPayment = { id, prefill -> navController.navigate(PaymentEditRoute(labourerId = id, prefillPaise = prefill)) },
                onOpenPayment = openPayment,
                onOpenDay = { workId, date -> navController.navigate(AttendanceRoute(workId, date.toEpochDay())) },
            )
        }
        composable<AttendanceRoute> {
            AttendanceScreen(onBack = back, onAddLabourer = addLabourer)
        }
        composable<PaymentEditRoute> {
            PaymentEditScreen(onDone = back)
        }
    }
}
