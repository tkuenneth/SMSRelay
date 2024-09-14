package de.thomaskuenneth.smsrelay

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony.Sms.Intents.getMessagesFromIntent
import android.util.Log
import java.util.Properties
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage
import kotlin.concurrent.thread

class Receiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            "android.provider.Telephony.SMS_RECEIVED" -> context.handleSms(intent)
            "android.provider.Telephony.MMS_RECEIVED" -> handleMms(intent)
            else -> {
                Log.e(TAG, "unexpected action")
            }
        }
    }

    private fun Context.handleSms(intent: Intent) {
        getMessagesFromIntent(intent)?.forEach { message ->
            sendEmail(
                subject = getString(
                    R.string.subject,
                    message.originatingAddress ?: getString(R.string.unknown_sender)
                ),
                text = message.messageBody
            )
        }
    }

    private fun handleMms(intent: Intent) {
    }

    private fun sendEmail(subject: String, text: String) {
        thread {
            val props = Properties()
            props["mail.smtp.host"] = BuildConfig.SMTP_HOST
            props["mail.smtp.port"] = BuildConfig.SMTP_PORT
            props["mail.smtp.auth"] = true
            props["mail.smtp.starttls.enable"] = true
            props["mail.smtp.socketFactory.class"] = "javax.net.ssl.SSLSocketFactory"
            val auth = object : Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication {
                    return PasswordAuthentication(
                        BuildConfig.SMTP_USERNAME, BuildConfig.SMTP_PASSWORD
                    )
                }
            }
            try {
                MimeMessage(Session.getInstance(props, auth)).run {
                    setFrom(InternetAddress(BuildConfig.SMTP_FROM))
                    setRecipients(
                        Message.RecipientType.TO, InternetAddress.parse(BuildConfig.SMTP_TO)
                    )
                    setSubject(subject)
                    setText(text)
                    Transport.send(this)
                }
            } catch (e: Exception) {
                Log.e(TAG, "sendEmail()", e)
            }
        }
    }

    companion object {
        val TAG: String = Receiver::class.java.simpleName
    }
}
