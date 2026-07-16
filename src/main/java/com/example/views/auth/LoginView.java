package com.example.views.auth;

import com.example.entities.Department;
import com.example.entities.User;
import com.example.enums.UserRole;
import com.example.services.DepartmentService;
import com.example.services.UserService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;

@Route(value = "login", autoLayout = false)
public class LoginView extends VerticalLayout implements BeforeEnterObserver {

    private final UserService userService;
    private final DepartmentService departmentService;

    private final H2 loginTitle = new H2("Giriş Yap");
    private final H2 registerTitle = new H2("Hesap Oluştur");
    private final Paragraph loginSub = new Paragraph("Yönetim paneline erişmek için bilgilerinizi girin.");
    private final Paragraph registerSub = new Paragraph("Sisteme dahil olmak için formu doldurun.");

    private final Select<UserRole> roleSelect = new Select<>();

    public LoginView(UserService userService, DepartmentService departmentService) {
        this.userService = userService;
        this.departmentService = departmentService;

        injectCustomCSS();

        setSizeFull();
        setJustifyContentMode(JustifyContentMode.CENTER);
        setAlignItems(Alignment.CENTER);
        setSpacing(true);
        addClassName("login-viewport-bg");

        Image logo = new Image(
            com.vaadin.flow.server.streams.DownloadHandler.forClassResource(getClass(), "/META-INF/resources/logo.png"), 
            "MONAD Logo"
        );
        logo.addClassName("center-brand-logo");

        HorizontalLayout masterPanel = new HorizontalLayout();
        masterPanel.setPadding(false);
        masterPanel.setSpacing(false);
        masterPanel.setWidth("880px");
        masterPanel.setHeight("540px"); 
        masterPanel.addClassName("glass-master-card");

        VerticalLayout leftPanel = new VerticalLayout();
        leftPanel.setWidth("50%");
        leftPanel.setHeightFull();
        leftPanel.setJustifyContentMode(JustifyContentMode.CENTER);
        leftPanel.getStyle().set("padding", "50px");

        loginTitle.addClassName("premium-heading");
        loginSub.addClassName("premium-subtext");

        TextField loginEmail = new TextField("Kullanıcı Adı / E-posta");
        styleInputField(loginEmail, VaadinIcon.USER);

        PasswordField loginPassword = new PasswordField("Şifre");
        styleInputField(loginPassword, VaadinIcon.LOCK);
        
        Button loginBtn = new Button("Sisteme Giriş Yap");
        loginBtn.addClassName("modern-btn-primary");

        loginBtn.addClickListener(event -> {
            String inputEmail = loginEmail.getValue().trim();
            String inputPass = loginPassword.getValue();

            if (inputEmail.isEmpty() || inputPass.isEmpty()) {
                Notification.show("Lütfen tüm alanları doldurun.").addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            userService.findByEmail(inputEmail).ifPresentOrElse(user -> {
                if (user.getPassword().equals(inputPass)) {
                    if (user.isApproved()) {
                        Notification.show("Giriş Başarılı!").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                        com.vaadin.flow.server.VaadinSession.getCurrent().setAttribute("user", user);
                        UI.getCurrent().navigate(user.getRole().getUrlSegment() + "/dashboard");
                    } else {
                        this.removeAll();
                        this.add(new WaitingApprovalView(user.getName(), user.getRole().name().toLowerCase()));
                    }
                } else {
                    Notification.show("Hatalı Şifre!").addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            }, () -> Notification.show("Kullanıcı bulunamadı!").addThemeVariants(NotificationVariant.LUMO_ERROR));
        });

        leftPanel.add(loginTitle, loginSub, loginEmail, loginPassword, loginBtn);

        VerticalLayout rightPanel = new VerticalLayout();
        rightPanel.setWidth("50%");
        rightPanel.setHeightFull();
        rightPanel.setJustifyContentMode(JustifyContentMode.CENTER);
        rightPanel.addClassName("right-split-panel");

        registerTitle.addClassName("premium-heading");
        registerSub.addClassName("premium-subtext");

        roleSelect.setLabel("Rol Seçin");
        roleSelect.setItems(UserRole.values());
        roleSelect.setItemLabelGenerator(role -> role.toString()); 
        styleSelectorField(roleSelect, VaadinIcon.USER_CHECK); // Changed from FACTORY to USER_CHECK

        roleSelect.addValueChangeListener(e -> {
            UserRole selected = e.getValue();
        });

        TextField regName = new TextField("Tam İsim");
        styleInputField(regName, VaadinIcon.USER_CARD);

        TextField regEmail = new TextField("E-posta Adresi");
        styleInputField(regEmail, VaadinIcon.ENVELOPE);

        PasswordField regPass = new PasswordField("Şifre");
        styleInputField(regPass, VaadinIcon.PASSWORD);

        roleSelect.setValue(UserRole.CUSTOMER); 

        Button registerBtn = new Button("Kayıt Başvurusunu Tamamla");
        registerBtn.addClassName("modern-btn-secondary");

        registerBtn.addClickListener(event -> {
            if (regName.getValue().isEmpty() || regEmail.getValue().isEmpty() || regPass.getValue().isEmpty() || roleSelect.getValue() == null) {
                Notification.show("Zorunlu alanları doldurun!").addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            try {
                User newUser = new User();
                newUser.setName(regName.getValue().trim());
                newUser.setEmail(regEmail.getValue().trim());
                newUser.setPassword(regPass.getValue()); 

                UserRole targetRole = roleSelect.getValue();

                userService.registerNewUser(newUser, targetRole);
                
                Notification n = Notification.show("Kayıt Alındı! Onay bekleniyor.");
                n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                
                regName.clear(); regEmail.clear(); regPass.clear();
                roleSelect.setValue(UserRole.CUSTOMER); 
            } catch (Exception e) {
                Notification.show("Hata: " + e.getMessage()).addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        rightPanel.add(registerTitle, registerSub, roleSelect, regName, regEmail, regPass, registerBtn);
        masterPanel.add(leftPanel, rightPanel);
        add(logo, masterPanel);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        java.util.Map<String, java.util.List<String>> queryParams = event.getLocation().getQueryParameters().getParameters();

        if (queryParams.containsKey("approved") && "false".equals(queryParams.get("approved").get(0))) {
            this.removeAll();
            this.add(new WaitingApprovalView("Kullanıcı", "user"));
        }
    }

    private void styleInputField(TextField f, VaadinIcon icon) {
        f.setWidthFull();
        f.setPrefixComponent(icon.create());
        f.addClassName("custom-premium-input");
    }

    private void styleInputField(PasswordField f, VaadinIcon icon) {
        f.setWidthFull();
        f.setPrefixComponent(icon.create());
        f.addClassName("custom-premium-input");
    }

    private void styleSelectorField(Select<?> s, VaadinIcon icon) {
        s.setWidthFull();
        s.setPrefixComponent(icon.create());
        s.addClassName("custom-premium-input");
    }

    private void injectCustomCSS() {
        String css = "::selection { background: #026BAC; color: white; }"
            + ".login-viewport-bg {"
            + "  background: linear-gradient(135deg, #f1f5f9 0%, #e2e8f0 50%, #cbd5e1 100%) !important;"
            + "}"
            + ".center-brand-logo {"
            + "  height: 110px !important;"
            + "  margin-bottom: 25px !important;"
            + "  object-fit: contain !important;"
            + "}"
            + ".center-brand-logo-waiting {"
            + "  height: 110px !important;"
            + "  object-fit: contain !important;"
            + "  margin-bottom: 50px !important;" 
            + "}"
            + ".glass-master-card, vaadin-vertical-layout > div:nth-child(2) {"
            + "  background: #ffffff !important;"
            + "  border: 1px solid #e2e8f0 !important;"
            + "  border-radius: 24px !important;"
            + "  box-shadow: 0 25px 60px rgba(15, 23, 42, 0.06), 0 4px 12px rgba(2, 107, 172, 0.03) !important;"
            + "  margin-bottom: auto !important;"
            + "}"
            + ".right-split-panel {"
            + "  background: #f8fafc !important;"
            + "  border-left: 1px solid #e2e8f0 !important;"
            + "  padding: 50px !important;"
            + "}"
            + ".premium-heading {"
            + "  margin: 0 !important;"
            + "  font-size: 2.1rem !important;"
            + "  font-weight: 800 !important;"
            + "  letter-spacing: -0.04em !important;"
            + "  color: #0f172a !important;"
            + "}"
            + ".premium-subtext {"
            + "  margin: 6px 0 30px 0 !important;"
            + "  color: #64748b !important;"
            + "  font-size: 0.9rem !important;"
            + "}"
            + ".custom-premium-input {"
            + "  --vaadin-input-field-border-radius: 12px !important;"
            + "  --lumo-body-text-color: #0f172a !important;"
            + "  --lumo-secondary-text-color: #026BAC !important;"
            + "  --lumo-contrast-10pct: #f1f5f9 !important;"
            + "  border-radius: 12px !important;"
            + "  margin-bottom: 10px !important;"
            + "}"
            + ".modern-btn-primary {"
            + "  width: 100% !important;"
            + "  height: 48px !important;"
            + "  margin-top: 15px !important;"
            + "  font-weight: 600 !important;"
            + "  color: white !important;"
            + "  background: #026BAC !important;"
            + "  border-radius: 12px !important;"
            + "  box-shadow: 0 4px 15px rgba(2, 107, 172, 0.25) !important;"
            + "  border: none !important;"
            + "  transition: all 0.2s ease !important;"
            + "  cursor: pointer !important;"
            + "}"
            + ".modern-btn-primary:hover {"
            + "  background: #015285 !important;"
            + "  box-shadow: 0 6px 20px rgba(2, 107, 172, 0.4) !important;"
            + "  transform: translateY(-1px) !important;"
            + "}"
            + ".modern-btn-secondary {"
            + "  width: 100% !important;"
            + "  height: 48px !important;"
            + "  margin-top: 15px !important;"
            + "  font-weight: 600 !important;"
            + "  color: #026BAC !important;"
            + "  background: rgba(2, 107, 172, 0.04) !important;"
            + "  border: 1px solid rgba(2, 107, 172, 0.2) !important;"
            + "  border-radius: 12px !important;"
            + "  transition: all 0.2s ease !important;"
            + "  cursor: pointer !important;"
            + "}"
            + ".modern-btn-secondary:hover {"
            + "  background: rgba(2, 107, 172, 0.08) !important;"
            + "  border-color: #026BAC !important;"
            + "  transform: translateY(-1px) !important;"
            + "}"
            + "vaadin-dev-tools, .vaadin-development-mode-badge { display: none !important; }";

        UI.getCurrent().getElement().executeJs(
            "const style = document.createElement('style');" +
            "style.type = 'text/css';" +
            "style.innerHTML = window.atob('" + java.util.Base64.getEncoder().encodeToString(css.getBytes()) + "');" +
            "document.head.appendChild(style);"
        );
    }
}