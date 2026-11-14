package com.gharkharcha.ui.common

import android.content.res.Resources
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable

/** Text that ViewModels can produce without a Context; resolved by the UI. */
@Immutable
sealed interface UiText {
    data class Res(@param:StringRes val id: Int, val args: List<Any> = emptyList()) : UiText
    data class Plural(@param:PluralsRes val id: Int, val count: Int) : UiText
    data class Raw(val value: String) : UiText

    fun asString(resources: Resources): String = when (this) {
        is Res -> resources.getString(id, *args.toTypedArray())
        is Plural -> resources.getQuantityString(id, count, count)
        is Raw -> value
    }

    companion object {
        fun of(@StringRes id: Int, vararg args: Any): UiText = Res(id, args.toList())
    }
}
