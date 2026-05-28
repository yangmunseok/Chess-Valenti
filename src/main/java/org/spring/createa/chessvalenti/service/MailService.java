package org.spring.createa.chessvalenti.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MailService {

  private final JavaMailSender mailSender;

  public void sendMail(String to, String subject, String text) {
    try {
      SimpleMailMessage message = new SimpleMailMessage();
      message.setTo(to);
      message.setSubject(subject);
      message.setText(text);
      mailSender.send(message);
      log.info("Email sent to {}", to);
    } catch (Exception e) {
      log.error("Failed to send email to {}", to, e);
      log.info("MOCK EMAIL CONTENT: \nTo: {}\nSubject: {}\nText: {}", to, subject, text);
    }
  }

  public void sendHtmlMail(String to, String subject, String htmlContent) {
    try {
      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
      helper.setTo(to);
      helper.setSubject(subject);
      helper.setText(htmlContent, true);
      mailSender.send(message);
      log.info("HTML Email sent to {}", to);
    } catch (Exception e) {
      log.error("Failed to send HTML email to {}", to, e);
      log.info("MOCK HTML EMAIL CONTENT: \nTo: {}\nSubject: {}\nHTML: {}", to, subject, htmlContent);
    }
  }
}
