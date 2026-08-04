package com.gooludou.shadowplanner.feature.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.net.toUri
import com.gooludou.shadowplanner.BuildConfig
import com.gooludou.shadowplanner.R

object FeedbackEmailHelper {
    fun buildIntent(context: Context): Intent {
        val body = buildString {
            appendLine(context.getString(R.string.feedback_greeting))
            appendLine(context.getString(R.string.feedback_placeholder))
            repeat(5) { appendLine() }
            appendLine("---")
            appendLine(context.getString(R.string.feedback_device_info_header))
            appendLine(
                context.getString(R.string.feedback_app_version, BuildConfig.VERSION_NAME)
            )
            appendLine(
                context.getString(
                    R.string.feedback_device,
                    Build.MANUFACTURER,
                    Build.MODEL
                )
            )
            appendLine(
                context.getString(R.string.feedback_android_version, Build.VERSION.RELEASE)
            )
        }
        val subject = context.getString(R.string.feedback_subject, BuildConfig.VERSION_NAME)
        val mailto = (
            "mailto:$CONTACT_EMAIL" +
                "?subject=${Uri.encode(subject)}" +
                "&body=${Uri.encode(body)}"
            ).toUri()

        return Intent(Intent.ACTION_SENDTO, mailto)
    }

    private const val CONTACT_EMAIL = "sunfinder.contact@gmail.com"
}
