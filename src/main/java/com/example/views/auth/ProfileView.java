package com.example.views.auth;

import com.example.base.ui.MainLayout;
import com.example.entities.SupportRequest;
import com.example.entities.User;
import com.example.enums.RequestStatus;
import com.example.enums.UserRole;
import com.example.services.SupportRequestService;
import com.example.services.UserService;
import com.example.views.base.BaseSecuredView;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import org.springframework.security.crypto.password.PasswordEncoder;

@PageTitle("Profilim")
@Route(value = "profile", layout = MainLayout.class)
public class ProfileView extends BaseSecuredView {

    private final UserService userService;
    private final SupportRequestService supportRequestService;
    private final PasswordEncoder passwordEncoder;

    public ProfileView(UserService userService, SupportRequestService supportRequestService, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.supportRequestService = supportRequestService;
        this.passwordEncoder = passwordEncoder;

        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
        getStyle().set("background", "linear-gradient(135deg, #f0f4f8 0%, #e2e8f0 100%)");
    }

    @Override
    protected void onUserAuthenticated(BeforeEnterEvent event, User user) {
        removeAll();

        VerticalLayout headerSection = new VerticalLayout();
        headerSection.setAlignItems(Alignment.CENTER);
        headerSection.setPadding(false);
        headerSection.setSpacing(false);
        headerSection.getStyle().set("margin-bottom", "20px");
        
        H2 title = new H2("Profil Bilgilerim");
        title.getStyle().set("margin", "0").set("color", "#102a43").set("font-weight", "700").set("font-size", "1.75rem");
        
        Paragraph subtitle = new Paragraph("Hesap bilgilerinizi görüntüleyin, güncelleyin ve şifrenizi yönetin.");
        subtitle.getStyle().set("color", "#486581").set("margin", "6px 0 0 0").set("font-size", "0.95rem");
        
        headerSection.add(title, subtitle);

        VerticalLayout card = new VerticalLayout();
        card.setMaxWidth("650px");
        card.setWidthFull();
        card.getStyle()
                .set("background", "white")
                .set("border-radius", "16px")
                .set("padding", "40px")
                .set("box-shadow", "0 15px 30px -5px rgba(0, 102, 204, 0.08), 0 8px 10px -6px rgba(0, 102, 204, 0.04)");
        card.setSpacing(true);
        card.setAlignItems(Alignment.STRETCH);

        TextField nameField = new TextField("Ad Soyad");
        nameField.setValue(user.getName() != null ? user.getName() : "");
        nameField.setWidthFull();

        TextField emailField = new TextField("E-posta");
        emailField.setValue(user.getEmail() != null ? user.getEmail() : "");
        emailField.setWidthFull();

        HorizontalLayout rowLayout = new HorizontalLayout();
        rowLayout.setWidthFull();
        rowLayout.setSpacing(true);
        rowLayout.setPadding(false);

        TextField roleField = new TextField("Rol");
        roleField.setValue(user.getRole() != null ? user.getRole().toString() : "");
        roleField.setReadOnly(true);
        roleField.setWidthFull();

        TextField orgField = new TextField("Şirket / Departman");
        String orgName = "-";
        if (user.getRole() == UserRole.CUSTOMER && user.getCompany() != null) {
            orgName = user.getCompany().getName();
        } else if (user.getDepartment() != null) {
            orgName = user.getDepartment().getName();
        }
        orgField.setValue(orgName);
        orgField.setReadOnly(true);
        orgField.setWidthFull();
        
        rowLayout.add(roleField, orgField);

        Paragraph passTitle = new Paragraph("Şifre Değiştirme");
        passTitle.getStyle().set("color", "#102a43").set("font-weight", "600").set("margin", "10px 0 0 0").set("font-size", "1rem");

        PasswordField currentPassField = new PasswordField("Mevcut Şifre");
        currentPassField.setWidthFull();

        PasswordField newPassField = new PasswordField("Yeni Şifre");
        newPassField.setWidthFull();

        Button saveButton = new Button("Değişiklikleri Kaydet", VaadinIcon.CHECK.create());
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.getStyle().set("background-color", "#0066cc").set("border-radius", "8px");
        saveButton.addClickListener(e -> {
            user.setName(nameField.getValue());
            user.setEmail(emailField.getValue());

            if (!newPassField.isEmpty()) {
                if (currentPassField.isEmpty() || !passwordEncoder.matches(currentPassField.getValue(), user.getPassword())) {
                    Notification.show("Mevcut şifre hatalı!", 3000, Notification.Position.MIDDLE)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                    return;
                }
                user.setPassword(passwordEncoder.encode(newPassField.getValue()));
            }

            userService.updateUser(user);
            VaadinSession.getCurrent().setAttribute("user", user); 

            Notification.show("Profil başarıyla güncellendi!", 3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            currentPassField.clear();
            newPassField.clear();
        });

        Button contactAdminButton = new Button("Yöneticiye Ulaşın", VaadinIcon.ENVELOPE_O.create());
        contactAdminButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        contactAdminButton.getStyle().set("border-radius", "8px");
        contactAdminButton.addClickListener(e -> openContactAdminModal(user));

        HorizontalLayout buttonLayout = new HorizontalLayout(contactAdminButton, saveButton);
        buttonLayout.setWidthFull();
        buttonLayout.setJustifyContentMode(JustifyContentMode.BETWEEN);
        buttonLayout.setPadding(false);
        buttonLayout.getStyle().set("margin-top", "15px");

        card.add(nameField, emailField, rowLayout, passTitle, currentPassField, newPassField, buttonLayout);
        
        add(headerSection, card);
    }

    private void openContactAdminModal(User currentUser) {
        Dialog modal = new Dialog();
        modal.setHeaderTitle("Yöneticiye Destek Talebi Gönder");
        modal.setCloseOnOutsideClick(false);

        TextField subjectField = new TextField("Konu / Başlık");
        subjectField.setWidthFull();
        subjectField.setPlaceholder("Talebinizin konusu...");

        TextArea messageField = new TextArea("Mesajınız");
        messageField.setWidthFull();
        messageField.setHeight("150px");
        messageField.setPlaceholder("Yöneticiye iletmek istediğiniz mesajı buraya yazın...");

        VerticalLayout modalBody = new VerticalLayout(subjectField, messageField);
        modalBody.setPadding(false);
        modalBody.setSpacing(true);
        modalBody.setWidth("450px");

        Button cancelBtn = new Button("İptal Et", e -> modal.close());
        Button submitBtn = new Button("Gönder", VaadinIcon.PAPERPLANE.create());
        submitBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        submitBtn.getStyle().set("background-color", "#0066cc").set("border-radius", "8px");

        submitBtn.addClickListener(e -> {
            String subject = subjectField.getValue().trim();
            String message = messageField.getValue().trim();

            if (subject.isEmpty() || message.isEmpty()) {
                Notification.show("Lütfen tüm alanları doldurun!", 3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            try {
                SupportRequest supportRequest = new SupportRequest();
                supportRequest.setSender(currentUser);
                supportRequest.setSubject(subject);
                supportRequest.setMessage(message);
                supportRequest.setStatus(RequestStatus.PENDING);
                supportRequest.setCreatedAt(java.time.LocalDateTime.now());
                supportRequest.setUpdatedAt(java.time.LocalDateTime.now());

                supportRequestService.saveSupportRequest(supportRequest);

                Notification.show("Destek talebiniz yöneticiye iletildi.", 4000, Notification.Position.BOTTOM_START)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

                modal.close();
            } catch (Exception ex) {
                Notification.show("Hata oluştu: " + ex.getMessage(), 4000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        modal.add(modalBody);
        cancelBtn.getStyle().set("margin-left", "auto");
        modal.getFooter().add(cancelBtn, submitBtn);
        modal.open();
    }
}