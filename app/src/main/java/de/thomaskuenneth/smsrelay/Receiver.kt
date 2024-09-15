package de.thomaskuenneth.smsrelay

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.provider.Telephony.Mms
import android.provider.Telephony.Sms.Intents.getMessagesFromIntent
import android.util.Log
import java.lang.Thread.sleep
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
            Telephony.Sms.Intents.SMS_RECEIVED_ACTION -> context.handleSms(intent)
            Telephony.Sms.Intents.WAP_PUSH_RECEIVED_ACTION -> context.handleMms()

            else -> {
                Log.e(TAG, "unexpected action")
            }
        }
    }

    private fun Context.handleSms(intent: Intent) {
        getMessagesFromIntent(intent)?.forEach { message ->
            sendEmail(
                subject = getSubject(message.originatingAddress),
                text = message.messageBody
            )
        }
    }

    private fun Context.handleMms() {
        // This is hacky; we need to find a way to connect the id from the intent with
        // the content provider
        thread {
            sleep(3000)
            getLatestMMS { subject, text -> sendEmail(subject = subject, text = text) }
        }
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

fun Context.getLatestMMS(callback: (String, String) -> Unit) {
    var text = getString(R.string.unknown)
    var subject = getString(R.string.unknown)
    contentResolver.query(
        Mms.CONTENT_URI,
        arrayOf(Mms._ID),
        null,
        null,
        Mms.Inbox.DEFAULT_SORT_ORDER
    )?.let { cursorInbox ->
        if (cursorInbox.moveToNext()) {
            val messageId = cursorInbox.getString(0)
            contentResolver.query(
                Mms.Part.CONTENT_URI,
                arrayOf(Mms.Part.MSG_ID, Mms.Part.CONTENT_TYPE, Mms.Part.TEXT),
                "${Mms.Part.MSG_ID} = ? AND ${Mms.Part.CONTENT_TYPE} = ?",
                arrayOf(messageId, "text/plain"),
                null
            )?.let { cursorPart ->
                if (cursorPart.moveToNext()) {
                    val indexText = cursorPart.getColumnIndex(Mms.Part.TEXT)
                    text = cursorPart.getString(indexText)
                }
                contentResolver.query(
                    Mms.Addr.getAddrUriForMessage(messageId),
                    arrayOf(Mms.Addr.ADDRESS),
                    null,
                    null,
                    null
                )?.let { cursorAdr ->
                    if (cursorAdr.moveToNext()) {
                        subject = getSubject(cursorAdr.getString(0))
                    }
                    cursorAdr.close()
                }
                cursorPart.close()
            }
        }
        cursorInbox.close()
    }
    callback(subject, text)
}

private fun Context.getSubject(address: String?) = getString(
    R.string.subject,
    if (address?.isNotEmpty() == true) address else getString(R.string.unknown_sender)
)
