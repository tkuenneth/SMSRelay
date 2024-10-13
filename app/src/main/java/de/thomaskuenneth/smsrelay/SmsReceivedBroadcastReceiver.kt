package de.thomaskuenneth.smsrelay

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.provider.Telephony
import android.provider.Telephony.Mms
import android.provider.Telephony.Sms.Intents.getMessagesFromIntent
import android.telephony.PhoneNumberUtils
import android.util.Log
import androidx.annotation.StringRes
import de.thomaskuenneth.smsrelay.SmsReceivedBroadcastReceiver.Companion.TAG
import java.io.ByteArrayInputStream
import java.lang.Thread.sleep
import java.util.Properties
import javax.activation.DataHandler
import javax.mail.Authenticator
import javax.mail.FolderNotFoundException
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart
import javax.mail.util.ByteArrayDataSource
import kotlin.concurrent.thread


class SmsReceivedBroadcastReceiver : BroadcastReceiver() {
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
                subject = getSubject(R.string.subject_new_message, message.originatingAddress),
                text = message.messageBody,
                images = emptyList()
            )
        }
    }

    private fun Context.handleMms() {
        // This is hacky; we need to find a way to connect the id from the intent with
        // the content provider
        thread {
            sleep(3000)
            getLatestMMS { subject, text, images ->
                sendEmail(
                    subject = subject, text = text, images = images
                )
            }
        }
    }

    companion object {
        val TAG: String = SmsReceivedBroadcastReceiver::class.java.simpleName
    }
}

fun sendEmail(subject: String, text: String, images: List<Pair<ByteArray, String>>) {
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
                val multipart = MimeMultipart()
                val bodyPart = MimeBodyPart()
                bodyPart.setText(text)
                multipart.addBodyPart(bodyPart)
                images.forEachIndexed { index, pair ->
                    ByteArrayInputStream(pair.first).use { stream ->
                        val messageBodyPart = MimeBodyPart()
                        val mimeType = pair.second
                        val ext = mimeType.split("/")[1]
                        messageBodyPart.setDataHandler(
                            DataHandler(
                                ByteArrayDataSource(
                                    stream, mimeType
                                )
                            )
                        )
                        messageBodyPart.fileName = "image_${1 + index}.$ext"
                        multipart.addBodyPart(messageBodyPart)
                    }
                }
                setContent(multipart)
                Transport.send(this)
            }
        } catch (e: Exception) {
            Log.e(TAG, "sendEmail()", e)
        }
    }
}

fun Context.getLatestMMS(callback: (String, String, List<Pair<ByteArray, String>>) -> Unit) {
    var text = getString(R.string.unknown)
    var subject = getString(R.string.unknown)
    val images = mutableListOf<Pair<ByteArray, String>>()
    contentResolver.query(
        Mms.CONTENT_URI, arrayOf(Mms._ID), null, null, Mms.Inbox.DEFAULT_SORT_ORDER
    )?.use { cursorInbox ->
        if (cursorInbox.moveToNext()) {
            val messageId = cursorInbox.getString(0)
            contentResolver.query(
                Mms.Part.CONTENT_URI,
                arrayOf(Mms.Part.MSG_ID, Mms.Part.CONTENT_TYPE, Mms.Part.TEXT),
                "${Mms.Part.MSG_ID} = ? AND ${Mms.Part.CONTENT_TYPE} = ?",
                arrayOf(messageId, "text/plain"),
                null
            )?.use { cursorPart ->
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
                )?.use { cursorAdr ->
                    if (cursorAdr.moveToNext()) {
                        subject = getSubject(R.string.subject_new_message, cursorAdr.getString(0))
                    }
                    contentResolver.query(
                        Mms.Part.CONTENT_URI,
                        arrayOf(Mms._ID, Mms.Part.MSG_ID, Mms.Part.CONTENT_TYPE),
                        "${Mms.Part.MSG_ID} = ? AND ${Mms.Part.CONTENT_TYPE} LIKE ?",
                        arrayOf(messageId, "image/%"),
                        null
                    )?.use { cursorPartImage ->
                        while (cursorPartImage.moveToNext()) {
                            val partId = cursorPartImage.getString(0)
                            val contentType = cursorPartImage.getString(2)
                            try {
                                contentResolver.openInputStream(
                                    Uri.withAppendedPath(
                                        Mms.Part.CONTENT_URI, partId
                                    )
                                ).use {
                                    it?.readBytes()
                                        ?.let { bytes -> images.add(Pair(bytes, contentType)) }
                                }
                            } catch (e: FolderNotFoundException) {
                                Log.e(TAG, "openInputStream()", e)
                            }
                        }
                    }
                }
            }
        }
    }
    callback(subject, text, images)
}

fun Context.getSubject(@StringRes subject: Int, address: String?) = getString(
    subject,
    if (address?.isNotEmpty() == true) getName(address) else getString(R.string.unknown_sender)
)

@SuppressLint("Range")
fun Context.getName(phoneNumber: String): String {
    val projection = arrayOf(
        ContactsContract.CommonDataKinds.Phone.NUMBER,
        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
    )
    val normalizedIncoming = PhoneNumberUtils.normalizeNumber(phoneNumber)
    contentResolver.query(
        ContactsContract.CommonDataKinds.Phone.CONTENT_URI, projection, null, null, null
    )?.use { cursor ->
        while (cursor.moveToNext()) {
            val number =
                cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
            val normalizedCurrent = PhoneNumberUtils.normalizeNumber(number)
            if (normalizedIncoming.contains(normalizedCurrent) || normalizedCurrent.contains(
                    normalizedIncoming
                )
            ) {
                return cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
            }
        }
    }
    return phoneNumber
}
