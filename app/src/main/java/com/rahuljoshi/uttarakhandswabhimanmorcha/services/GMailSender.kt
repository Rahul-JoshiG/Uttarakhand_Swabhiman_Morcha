package com.rahuljoshi.uttarakhandswabhimanmorcha.services
import java.util.Properties
import javax.mail.Authenticator
import javax.mail.Message
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

        session = Session.getInstance(props, object : Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication {
                return PasswordAuthentication(user, password)
            }
        })
    }

    fun sendMail(subject: String, body: String, recipient: String) {
        val message = MimeMessage(session)
        message.setFrom(InternetAddress(user))
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipient))
        message.subject = subject
        message.setText(body)
        Transport.send(message)
    }
}
