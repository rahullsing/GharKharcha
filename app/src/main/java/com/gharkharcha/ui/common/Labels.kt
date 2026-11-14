package com.gharkharcha.ui.common

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.gharkharcha.R
import com.gharkharcha.data.entity.AttendanceStatus
import com.gharkharcha.data.entity.LabourerEntity
import com.gharkharcha.data.entity.PaymentMode
import com.gharkharcha.data.entity.PaymentType
import com.gharkharcha.data.entity.Skill
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

@get:StringRes
val Skill.labelRes: Int
    get() = when (this) {
        Skill.ELECTRICIAN -> R.string.skill_electrician
        Skill.BRICK_MASON -> R.string.skill_brick_mason
        Skill.HELPER -> R.string.skill_helper
        Skill.PLUMBER -> R.string.skill_plumber
        Skill.CARPENTER -> R.string.skill_carpenter
        Skill.PAINTER -> R.string.skill_painter
        Skill.TILE_FITTER -> R.string.skill_tile_fitter
        Skill.WELDER -> R.string.skill_welder
        Skill.CUSTOM -> R.string.skill_custom
    }

/** The labourer's skill, using their own skill name when it is a custom one. */
@Composable
fun LabourerEntity.skillLabel(): String =
    if (skill == Skill.CUSTOM && customSkill.isNotBlank()) customSkill else stringResource(skill.labelRes)

/** Short chip label. Overtime is shown as a full day (plus an overtime note elsewhere). */
@get:StringRes
val AttendanceStatus.labelRes: Int
    get() = when (this) {
        AttendanceStatus.FULL_DAY, AttendanceStatus.OVERTIME -> R.string.attendance_full
        AttendanceStatus.HALF_DAY -> R.string.attendance_half
        AttendanceStatus.HOURS -> R.string.attendance_hours
        AttendanceStatus.ABSENT -> R.string.attendance_absent
    }

@get:StringRes
val PaymentMode.labelRes: Int
    get() = when (this) {
        PaymentMode.CASH -> R.string.payment_mode_cash
        PaymentMode.UPI -> R.string.payment_mode_upi
        PaymentMode.BANK -> R.string.payment_mode_bank
        PaymentMode.CHEQUE -> R.string.payment_mode_cheque
    }

@get:StringRes
val PaymentType.labelRes: Int
    get() = when (this) {
        PaymentType.WAGE -> R.string.payment_type_wage
        PaymentType.ADVANCE -> R.string.payment_type_advance
    }
