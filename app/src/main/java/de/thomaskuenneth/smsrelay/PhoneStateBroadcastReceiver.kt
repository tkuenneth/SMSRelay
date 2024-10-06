package de.thomaskuenneth.smsrelay

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.telephony.TelephonyManager

const val KEY_INCOMING_NUMBER = "incomingNumber"

class PhoneStateBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (TelephonyManager.ACTION_PHONE_STATE_CHANGED == intent?.action) {
            val prefs = context.getSharedPreferences(MainActivity.TAG, MODE_PRIVATE)
            when (intent.getStringExtra(TelephonyManager.EXTRA_STATE)) {
                TelephonyManager.EXTRA_STATE_RINGING -> {
                    prefs.edit().putString(KEY_INCOMING_NUMBER,
                        with(intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)) {
                            if (this.isNullOrEmpty()) {
                                context.getString(R.string.unknown)
                            } else this
                        }).apply()
                }

                TelephonyManager.EXTRA_STATE_IDLE -> {
                    val incoming = prefs.getString(KEY_INCOMING_NUMBER, "") ?: ""
                    prefs.edit().putString(KEY_INCOMING_NUMBER, "").apply()
                    if (incoming.isNotEmpty()) {
                        sendEmail(
                            subject = context.getSubject(R.string.subject_missed_call, incoming),
                            text = "",
                            images = emptyList()
                        )
                    }
                }

                else -> {}
            }
        }
    }
}
