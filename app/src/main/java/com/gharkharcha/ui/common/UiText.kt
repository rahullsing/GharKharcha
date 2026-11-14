package com.gharkharcha.ui.common

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.res.stringResource

/** Text that ViewModels can produce without a Context; resolved in Compose. */
@Immutable
sealed interface UiText {
    data class Res(@param:StringRes val id: Int, val args: List<Any> = emptyList()) : UiText
    data class Raw(val value: String) : UiText

    @Composable
    fun resolve(): String = when (this) {
        is Res -> stringResource(id, *args.toTypedArray())
        is Raw -> value
    }

    companion object {
        fun of(@StringRes id: Int, vararg args: Any): UiText = Res(id, args.toList())
    }
}
