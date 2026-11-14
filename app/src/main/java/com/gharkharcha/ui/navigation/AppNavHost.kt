package com.gharkharcha.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.gharkharcha.data.entity.OwnerType
import com.gharkharcha.ui.analytics.HouseBreakdownScreen
import com.gharkharcha.ui.attendance.AttendanceScreen
import com.gharkharcha.ui.home.HomeScreen
import com.gharkharcha.ui.labour.LabourerEditScreen
import com.gharkharcha.ui.labour.LabourerLedgerScreen
import com.gharkharcha.ui.labour.LabourersScreen
import com.gharkharcha.ui.materials.MaterialEditScreen
import com.gharkharcha.ui.more.MoreScreen
import com.gharkharcha.ui.payments.PaymentEditScreen
import com.gharkharcha.ui.photos.CAMERA_RESULT_KEY
import com.gharkharcha.ui.photos.CameraScreen
import com.gharkharcha.ui.photos.PhotoViewerScreen
import com.gharkharcha.ui.projects.ProjectEditScreen
import com.gharkharcha.ui.projects.ProjectsScreen
import com.gharkharcha.ui.reports.ReportsScreen
import com.gharkharcha.ui.search.SearchScreen
import com.gharkharcha.ui.settings.SettingsScreen
import com.gharkharcha.ui.vendors.VendorEditScreen
import com.gharkharcha.ui.vendors.VendorsScreen
import com.gharkharcha.ui.works.WorkDetailScreen
import com.gharkharcha.ui.works.WorkEditScreen
import com.gharkharcha.ui.works.WorksScreen

/** Camera capture handed back to a form screen through its back-stack entry. */
private class CameraResult(val path: String?, val consume: () -> Unit)

@Composable
private fun NavBackStackEntry.cameraResult(): CameraResult {
    val path by savedStateHandle.getStateFlow<String?>(CAMERA_RESULT_KEY, null).collectAsStateWithLifecycle()
    return CameraResult(path) { savedStateHandle[CAMERA_RESULT_KEY] = null }
}

@Composable
fun AppNavHost(
    navController: NavHostController,
    onNavigateTopLevel: (TopLevelDestination) -> Unit,
    onOpenWizard: () -> Unit = {},
) {
    val back: () -> Unit = { navController.popBackStack() }
    val addProject = { navController.navigate(ProjectEditRoute()) }
    val manageProjects = { navController.navigate(ProjectsRoute) }
    val addLabourer = { navController.navigate(LabourerEditRoute()) }
    val openLabourer: (Long) -> Unit = { navController.navigate(LabourerLedgerRoute(it)) }
    val openPayment: (Long, Long) -> Unit = { labourerId, paymentId ->
        navController.navigate(PaymentEditRoute(labourerId = labourerId, paymentId = paymentId))
    }
    val openCamera = { navController.navigate(CameraRoute) }

    NavHost(navController = navController, startDestination = HomeRoute) {
        composable<HomeRoute> {
            HomeScreen(
                onAddProject = addProject,
                onManageProjects = manageProjects,
                onOpenWork = { navController.navigate(WorkDetailRoute(it)) },
                onSeeAllWorks = { onNavigateTopLevel(TopLevelDestination.WORKS) },
                onAddWork = { projectId -> navController.navigate(WorkEditRoute(projectId = projectId)) },
                onOpenSearch = { navController.navigate(SearchRoute) },
                onOpenBreakdown = { projectId -> navController.navigate(HouseBreakdownRoute(projectId)) },
                onAddMaterial = { workId -> navController.navigate(MaterialEditRoute(workId = workId)) },
                onMarkAttendance = { workId -> navController.navigate(AttendanceRoute(workId = workId)) },
                onAddPayment = { workId -> navController.navigate(PaymentEditRoute(workId = workId)) },
                onOpenLabourer = openLabourer,
                onOpenPayment = openPayment,
                onOpenMaterial = { workId, materialId ->
                    navController.navigate(MaterialEditRoute(workId = workId, materialId = materialId))
                },
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
            MoreScreen(
                onOpenProjects = manageProjects,
                onOpenVendors = { navController.navigate(VendorsRoute) },
                onOpenReports = { navController.navigate(ReportsRoute()) },
                onOpenSettings = { navController.navigate(SettingsRoute) },
                onOpenWizard = onOpenWizard,
            )
        }

        composable<ProjectsRoute> {
            ProjectsScreen(
                onBack = back,
                onAddProject = addProject,
                onEditProject = { navController.navigate(ProjectEditRoute(it)) },
            )
        }
        composable<ProjectEditRoute> { entry ->
            val camera = entry.cameraResult()
            ProjectEditScreen(
                onDone = back,
                onOpenCamera = openCamera,
                capturedPhoto = camera.path,
                onCaptureConsumed = camera.consume,
            )
        }

        composable<WorkDetailRoute> {
            WorkDetailScreen(
                onBack = back,
                onEdit = { work -> navController.navigate(WorkEditRoute(projectId = work.projectId, workId = work.id)) },
                onMarkAttendance = { navController.navigate(AttendanceRoute(workId = it)) },
                onOpenLabourer = openLabourer,
                onOpenPayment = openPayment,
                onAddPayment = { workId -> navController.navigate(PaymentEditRoute(workId = workId)) },
                onAddMaterial = { navController.navigate(MaterialEditRoute(workId = it)) },
                onOpenMaterial = { navController.navigate(MaterialEditRoute(workId = it.workId, materialId = it.id)) },
                onOpenPhoto = { workId, attachmentId ->
                    navController.navigate(PhotoViewerRoute(workId = workId, startId = attachmentId))
                },
                onExportReport = { workId ->
                    navController.navigate(ReportsRoute(initialType = "WORK", initialWorkId = workId))
                },
            )
        }
        composable<WorkEditRoute> {
            WorkEditScreen(onDone = back)
        }
        composable<MaterialEditRoute> { entry ->
            val camera = entry.cameraResult()
            MaterialEditScreen(
                onDone = back,
                onOpenCamera = openCamera,
                capturedPhoto = camera.path,
                onCaptureConsumed = camera.consume,
            )
        }

        composable<LabourerEditRoute> { entry ->
            val camera = entry.cameraResult()
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
                onOpenCamera = openCamera,
                capturedPhoto = camera.path,
                onCaptureConsumed = camera.consume,
            )
        }
        composable<LabourerLedgerRoute> {
            LabourerLedgerScreen(
                onBack = back,
                onEdit = { navController.navigate(LabourerEditRoute(it)) },
                onAddPayment = { id, prefill -> navController.navigate(PaymentEditRoute(labourerId = id, prefillPaise = prefill)) },
                onOpenPayment = openPayment,
                onOpenDay = { workId, date -> navController.navigate(AttendanceRoute(workId, date.toEpochDay())) },
                onOpenPhoto = { labourerId ->
                    navController.navigate(PhotoViewerRoute(ownerType = OwnerType.LABOURER.name, ownerId = labourerId))
                },
                onExportStatement = { labourerId ->
                    navController.navigate(ReportsRoute(initialType = "LABOURER", initialLabourerId = labourerId))
                },
            )
        }
        composable<AttendanceRoute> {
            AttendanceScreen(onBack = back, onAddLabourer = addLabourer)
        }
        composable<PaymentEditRoute> { entry ->
            val camera = entry.cameraResult()
            PaymentEditScreen(
                onDone = back,
                onOpenCamera = openCamera,
                capturedPhoto = camera.path,
                onCaptureConsumed = camera.consume,
            )
        }

        composable<VendorsRoute> {
            VendorsScreen(
                onBack = back,
                onAdd = { navController.navigate(VendorEditRoute()) },
                onOpen = { navController.navigate(VendorEditRoute(it)) },
            )
        }
        composable<VendorEditRoute> {
            VendorEditScreen(onDone = back)
        }

        composable<CameraRoute> {
            CameraScreen(
                onBack = back,
                onCaptured = { path ->
                    navController.previousBackStackEntry?.savedStateHandle?.set(CAMERA_RESULT_KEY, path)
                    navController.popBackStack()
                },
            )
        }
        composable<PhotoViewerRoute> {
            PhotoViewerScreen(onBack = back)
        }

        composable<HouseBreakdownRoute> {
            HouseBreakdownScreen(
                onBack = back,
                onOpenWork = { navController.navigate(WorkDetailRoute(it)) },
                onOpenLabourer = openLabourer,
                onOpenVendor = { navController.navigate(VendorEditRoute(it)) },
            )
        }
        composable<SearchRoute> {
            SearchScreen(
                onBack = back,
                onOpenWork = { navController.navigate(WorkDetailRoute(it)) },
                onOpenLabourer = openLabourer,
                onOpenMaterial = { workId, materialId ->
                    navController.navigate(MaterialEditRoute(workId = workId, materialId = materialId))
                },
                onOpenPayment = openPayment,
                onOpenVendor = { navController.navigate(VendorEditRoute(it)) },
            )
        }
        composable<ReportsRoute> {
            ReportsScreen(onBack = back)
        }
        composable<SettingsRoute> {
            SettingsScreen(onBack = back)
        }
    }
}
