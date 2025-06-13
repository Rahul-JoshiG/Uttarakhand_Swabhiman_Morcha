package com.rahuljoshi.uttarakhandswabhimanmorcha.services
import android.util.Log
import java.util.Properties
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.MessagingException
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

class GMailSender(
    private val user: String,
    private val password: String
) : Authenticator() {

    private val session: Session

    init {
        val props = Properties()
        props["mail.smtp.auth"] = "true"
        props["mail.smtp.starttls.enable"] = "true"
        props["mail.smtp.host"] = "smtp.gmail.com"
        props["mail.smtp.port"] = "587"
        props["mail.smtp.ssl.trust"] = "smtp.gmail.com"

        session = Session.getInstance(props, object : Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication {
                return PasswordAuthentication(user, password)
            }
        })
    }

    @Throws(MessagingException::class)
    fun sendMail(subject: String, body: String, recipient: String) {
        try {
            Log.d("GMailSender", "Preparing to send email to: $recipient")
            val message = MimeMessage(session)
            message.setFrom(InternetAddress(user))
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipient))
            message.subject = subject
            message.setText(body)
            
            Log.d("GMailSender", "Sending email...")
            Transport.send(message)
            Log.d("GMailSender", "Email sent successfully")
        } catch (e: MessagingException) {
            Log.e("GMailSender", "Failed to send email", e)
            throw e
        } catch (e: Exception) {
            Log.e("GMailSender", "Unexpected error while sending email", e)
            throw MessagingException("Failed to send email: ${e.message}")
        }
    }
}
