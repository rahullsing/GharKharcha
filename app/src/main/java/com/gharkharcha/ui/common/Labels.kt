package com.gharkharcha.ui.common

import androidx.annotation.StringRes
import com.gharkharcha.R
import com.gharkharcha.data.entity.WorkStatus
import com.gharkharcha.data.entity.WorkType

/** Enum → user-visible string resources. */

@get:StringRes
val WorkType.labelRes: Int
    get() = when (this) {
        WorkType.ELECTRICAL -> R.string.work_type_electrical
        WorkType.PLUMBING -> R.string.work_type_plumbing
        WorkType.BRICK_MASONRY -> R.string.work_type_brick_masonry
        WorkType.PAINTING -> R.string.work_type_painting
        WorkType.TILING -> R.string.work_type_tiling
        WorkType.CARPENTRY -> R.string.work_type_carpentry
        WorkType.CUSTOM -> R.string.work_type_custom
    }

/** Suggested work name when a type is picked; null for CUSTOM. */
@get:StringRes
val WorkType.defaultNameRes: Int?
    get() = when (this) {
        WorkType.ELECTRICAL -> R.string.work_type_default_name_electrical
        WorkType.PLUMBING -> R.string.work_type_default_name_plumbing
        WorkType.BRICK_MASONRY -> R.string.work_type_default_name_brick_masonry
        WorkType.PAINTING -> R.string.work_type_default_name_painting
        WorkType.TILING -> R.string.work_type_default_name_tiling
        WorkType.CARPENTRY -> R.string.work_type_default_name_carpentry
        WorkType.CUSTOM -> null
    }

@get:StringRes
val WorkStatus.labelRes: Int
    get() = when (this) {
        WorkStatus.PLANNED -> R.string.work_status_planned
        WorkStatus.IN_PROGRESS -> R.string.work_status_in_progress
        WorkStatus.DONE -> R.string.work_status_done
        WorkStatus.ON_HOLD -> R.string.work_status_on_hold
    }
