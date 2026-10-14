package com.kimmeldevs.margaritaone.push

import android.content.Intent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Where a tapped notification should take the user. */
sealed interface PushTarget {
    data class Report(val reportId: String) : PushTarget
    data object Bulletin : PushTarget

    companion object {
        const val EXTRA_REPORT_ID = "push_report_id"
        const val EXTRA_POST_ID = "push_post_id"

        private val _pending = MutableStateFlow<PushTarget?>(null)

        /** Set by MainActivity from the notification's intent; the signed-in navigation consumes it. */
        val pending: StateFlow<PushTarget?> = _pending.asStateFlow()

        fun handle(intent: Intent?) {
            val extras = intent ?: return
            val target = extras.getStringExtra(EXTRA_REPORT_ID)?.let(::Report)
                ?: extras.getStringExtra(EXTRA_POST_ID)?.let { Bulletin }
                ?: return
            // So a configuration change doesn't open it again.
            extras.removeExtra(EXTRA_REPORT_ID)
            extras.removeExtra(EXTRA_POST_ID)
            _pending.value = target
        }

        fun consume() {
            _pending.value = null
        }
    }
}
