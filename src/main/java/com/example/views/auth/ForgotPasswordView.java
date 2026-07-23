package com.example.views.auth;

import com.example.entities.User;
import com.example.services.EmailService;
import com.example.services.UserService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

@PageTitle("Şifremi Unuttum")
@Route(value = "forgot-password")
public class ForgotPasswordView extends VerticalLayout {

    public ForgotPasswordView(UserService userService, EmailService emailService, PasswordEncoder passwordEncoder) {
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
        getStyle().set("background", "linear-gradient(135deg, #f0f4f8 0%, #d9e2ec 100%)");

        VerticalLayout card = new VerticalLayout();
        card.setMaxWidth("420px");
        card.setWidthFull();
        card.getStyle()
                .set("background", "white")
                .set("border-radius", "16px")
                .set("padding", "40px 30px")
                .set("box-shadow", "0 15px 30px -5px rgba(0, 102, 204, 0.1), 0 8px 10px -6px rgba(0, 102, 204, 0.05)");
        card.setSpacing(false);
        card.setAlignItems(Alignment.CENTER);

        VerticalLayout iconBadge = new VerticalLayout(VaadinIcon.KEY_O.create());
        iconBadge.setAlignItems(Alignment.CENTER);
        iconBadge.setJustifyContentMode(JustifyContentMode.CENTER);
        iconBadge.setWidth("56px");
        iconBadge.setHeight("56px");
        iconBadge.getStyle()
                .set("background", "#e6f0fa")
                .set("color", "#0066cc")
                .set("border-radius", "14px")
                .set("margin-bottom", "20px");

        H3 title = new H3("Şifrenizi mi unuttunuz?");
        title.getStyle().set("margin", "0 0 8px 0").set("color", "#102a43").set("font-weight", "700");
        
        Paragraph subtitle = new Paragraph("Kayıtlı e-posta adresinizi girin, size yeni bir geçici şifre gönderelim.");
        subtitle.getStyle()
                .set("color", "#486581")
                .set("font-size", "0.95rem")
                .set("text-align", "center")
                .set("margin", "0 0 24px 0");

        TextField emailField = new TextField("E-posta Adresi");
        emailField.setPlaceholder("ornek@sirket.com");
        emailField.setWidthFull();
        emailField.getStyle().set("margin-bottom", "20px");

        Button submitBtn = new Button("Şifreyi Sıfırla", VaadinIcon.PAPERPLANE.create());
        submitBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        submitBtn.setWidthFull();
        submitBtn.getStyle()
                .set("height", "48px")
                .set("background-color", "#0066cc")
                .set("border-radius", "8px")
                .set("font-weight", "600");

        Button backBtn = new Button("Giriş Ekranına Dön", VaadinIcon.ARROW_LEFT.create(), e -> getUI().ifPresent(ui -> ui.navigate("login")));
        backBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        backBtn.setWidthFull();
        backBtn.getStyle().set("margin-top", "12px").set("color", "#627d98");

        card.add(iconBadge, title, subtitle, emailField, submitBtn, backBtn);
        add(card);
    }
}