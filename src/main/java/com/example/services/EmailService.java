package com.example.services;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendPasswordResetEmail(String toEmail, String newPassword) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Şifre Sıfırlama Talebi - Talep Yönetim Sistemi");
        message.setText("Merhaba,\n\nŞifre sıfırlama talebinde bulundunuz. Yeni geçici şifreniz: " + newPassword + "\n\nGiriş yaptıktan sonra profil sayfanızdan şifrenizi değiştirebilirsiniz.");
        
        mailSender.send(message);
    }

    public void sendRejectionEmail(String toEmail) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Kayıt Başvurunuz Hakkında - Talep Yönetim Sistemi");
        message.setText("Merhaba,\n\nMaalesef sistemimize yapmış olduğunuz kayıt başvurunuz onaylanmamıştır.\n\nİyi günler dileriz.");
        
        mailSender.send(message);
    }
}