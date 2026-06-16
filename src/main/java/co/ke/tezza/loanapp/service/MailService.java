package co.ke.tezza.loanapp.service;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Properties;

import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import co.ke.tezza.loanapp.entity.MEmail;
import co.ke.tezza.loanapp.entity.MWFMail;
import co.ke.tezza.loanapp.repository.MEmailRepository;
import co.ke.tezza.loanapp.repository.MWFMailRepository;
import lombok.extern.slf4j.Slf4j;

@Service
@EnableScheduling
@Slf4j
public class MailService {

	@Autowired
	private MWFMailRepository wFMailRepository;

	@Autowired
	private MEmailRepository emailRepository;

	/**
	 * Email scheduler runs every 30 seconds
	 */
	@Scheduled(fixedRate = 30000)
	public void emailTask() {

		List<MWFMail> mailList = wFMailRepository.findByIsActiveAndProcessed(true, false);

		log.debug("📧 Mail scheduler processing {} emails", mailList.size());

		for (MWFMail mail : mailList) {

			try {
				sendEmail(mail);
			} catch (Exception e) {
				log.error("❌ Failed to send email ID: {}", mail.getWfmailId(), e);
			}
		}
	}

	/**
	 * Build dynamic mail sender per organization
	 */
	private JavaMailSenderImpl buildMailSender(MEmail emailConfig) {

		JavaMailSenderImpl sender = new JavaMailSenderImpl();

		sender.setHost(emailConfig.getHost());
		sender.setPort(emailConfig.getPort());

		sender.setUsername(emailConfig.getUsername());
		sender.setPassword(emailConfig.getPassword());

		Properties props = sender.getJavaMailProperties();

		props.put("mail.smtp.auth", String.valueOf(emailConfig.isSmtpAuth()));

		props.put("mail.smtp.ssl.enable", String.valueOf(emailConfig.isSslEnabled()));

		props.put("mail.smtp.starttls.enable", String.valueOf(emailConfig.isStarttlsEnabled()));

		props.put("mail.smtp.connectiontimeout", "5000");
		props.put("mail.smtp.timeout", "5000");
		props.put("mail.smtp.writetimeout", "5000");

		props.put("mail.smtp.ssl.trust", emailConfig.getHost());

		props.put("mail.debug", "true");

		return sender;
	}

	/**
	 * Send Email
	 */
	public void sendEmail(MWFMail mail) {

		try {

			/**
			 * Get organization email configuration
			 */
			MEmail email = emailRepository.findTop1ByIsActiveAndAdOrgID(true, mail.getAdOrgID());

			if (email == null) {

				log.error("❌ No active email configuration found for orgId: {}", mail.getAdOrgID());

				return;
			}

			if (email.getUsername() == null || email.getPassword() == null) {

				log.error("❌ Email username/password missing for orgId: {}", mail.getAdOrgID());

				return;
			}

			/**
			 * Build dynamic sender
			 */
			JavaMailSenderImpl mailSender = buildMailSender(email);

			/**
			 * Create Mime Message
			 */
			MimeMessage mimeMessage = mailSender.createMimeMessage();

			MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

			log.debug("📧 Sending email to: {}, using host: {}, port: {}", mail.getMailTo(), email.getHost(),
					email.getPort());

			/**
			 * Email Details
			 */
			mimeMessageHelper.setSubject(mail.getMailSubject());

			mimeMessageHelper.setFrom(new InternetAddress(email.getUsername(), email.getName()));

			mimeMessageHelper.setTo(mail.getMailTo());

			mimeMessageHelper.setReplyTo(email.getUsername());

			mimeMessageHelper.setText(mail.getMailContent(), true);

			/**
			 * Add Attachment
			 */
			if (mail.getFilePath() != null && !mail.getFilePath().trim().isEmpty()) {

				File attachmentFile = new File(mail.getFilePath());

				if (attachmentFile.exists() && attachmentFile.canRead()) {

					FileSystemResource fileResource = new FileSystemResource(attachmentFile);

					String attachmentName = mail.getAttachmentName() != null ? mail.getAttachmentName()
							: attachmentFile.getName();

					mimeMessageHelper.addAttachment(attachmentName, fileResource);

					log.debug("📎 Added attachment: {}", attachmentName);

				} else {

					log.warn("⚠️ Attachment file not found: {}", mail.getFilePath());
				}
			}

			/**
			 * Email Headers
			 */
			mimeMessage.setHeader("X-Priority", "1");

			mimeMessage.setHeader("X-Mailer", email.getName() + " Mailer");

			mimeMessage.setHeader("Precedence", "bulk");

			/**
			 * Send Email
			 */
			mailSender.send(mimeMessage);

			/**
			 * Update Status
			 */
			mail.setProcessed(true);

			log.info("✅ Email sent successfully to: {}", mail.getMailTo());

		} catch (MessagingException e) {

			log.error("❌ Messaging error sending email to: {}", mail.getMailTo(), e);

		} catch (UnsupportedEncodingException e) {

			log.error("❌ Unsupported encoding error for email to: {}", mail.getMailTo(), e);

		} catch (IOException e) {

			log.error("❌ IO error sending email to: {}", mail.getMailTo(), e);

		} catch (Exception e) {

			log.error("❌ Unexpected error sending email to: {}", mail.getMailTo(), e);

		} finally {

			/**
			 * Always save status
			 */
			wFMailRepository.save(mail);
		}
	}
}