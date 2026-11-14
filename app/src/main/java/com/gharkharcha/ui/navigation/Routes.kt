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

// Labour
@Serializable data class LabourerEditRoute(val labourerId: Long = 0)
@Serializable data class LabourerLedgerRoute(val labourerId: Long)

/** [workId] 0 = pick a default work; [epochDay] null = today. */
@Serializable data class AttendanceRoute(val workId: Long = 0, val epochDay: Long? = null)

// Materials & vendors
@Serializable data class MaterialEditRoute(val workId: Long, val materialId: Long = 0)
@Serializable data object VendorsRoute
@Serializable data class VendorEditRoute(val vendorId: Long = 0)

// Photos
@Serializable data object CameraRoute

/**
 * Full-screen photos of one owner ([ownerType] + [ownerId]) or, when [workId] != 0, of a work and
 * all its materials. Opens at [startId].
 */
@Serializable data class PhotoViewerRoute(
    val ownerType: String = "",
    val ownerId: Long = 0,
    val workId: Long = 0,
    val startId: Long = 0,
)

/** [prefillPaise] > 0 pre-fills the amount ("Settle up"); [workId] pre-selects the work. */
@Serializable data class PaymentEditRoute(
    val labourerId: Long = 0,
    val paymentId: Long = 0,
    val prefillPaise: Long = 0,
    val workId: Long = 0,
)

// Analytics & search
@Serializable data class HouseBreakdownRoute(val projectId: Long)
@Serializable data object SearchRoute

// Reports & Exports
@Serializable data class ReportsRoute(
    val initialType: String = "HOUSE",
    val initialWorkId: Long = 0,
    val initialLabourerId: Long = 0,
)

// Settings
@Serializable data object SettingsRoute

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
