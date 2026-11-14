package com.gharkharcha.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri

/** Tap-to-call and WhatsApp. Both return false when no app can handle the action. */
object ContactIntents {

    /** Opens the dialer with the number filled in (no CALL_PHONE permission needed). */
    fun dial(context: Context, phone: String): Boolean =
        start(context, Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", phone.trim(), null)))

    fun whatsApp(context: Context, phone: String): Boolean {
        val number = whatsAppNumber(phone) ?: return false
        return start(context, Intent(Intent.ACTION_VIEW, "https://wa.me/$number".toUri()))
    }

    /** Digits with country code; a bare 10-digit number is assumed to be Indian (+91). */
    fun whatsAppNumber(phone: String): String? {
        val digits = phone.filter(Char::isDigit).trimStart('0')
        return when {
            digits.length < 10 -> null
            digits.length == 10 -> "91$digits"
            else -> digits
        }
    }

    private fun start(context: Context, intent: Intent): Boolean = try {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
}
