package com.example.views.auth;

import com.example.entities.Company;
import com.example.entities.Department;
import com.example.entities.SupportRequest;
import com.example.entities.User;
import com.example.services.CompanyService;
import com.example.services.DepartmentService;
import com.example.services.SupportRequestService;
import com.example.services.UserService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;

import java.util.List;

public class WaitingApprovalView extends VerticalLayout {

    private static final String USER_APPROVAL_PREFIX = "[KULLANICI ONAYI]";

    public WaitingApprovalView(User currentUser, String currentRole, 
                               DepartmentService departmentService, 
                               CompanyService companyService, 
                               SupportRequestService supportRequestService,
                               UserService userService) {
        
        String userName = currentUser != null ? currentUser.getName() : "Kullanıcı";

        setSizeFull();
        setJustifyContentMode(JustifyContentMode.START);
        setAlignItems(Alignment.CENTER);
        getStyle().set("padding-top", "80px");
        addClassName("login-viewport-bg");

        Image logo = new Image(
            com.vaadin.flow.server.streams.DownloadHandler.forClassResource(getClass(), "/META-INF/resources/logo.png"), 
            "MONAD Logo"
        );
        logo.addClassName("center-brand-logo-waiting");

        H2 waitingTitle = new H2("Hesabınız Onay Bekliyor");
        Paragraph waitingMsg = new Paragraph("Merhaba sayın " + userName + ", lütfen kayıt işlemini tamamlamak için aşağıdaki formu doldurun.");

        Button backBtn = new Button("Giriş Ekranına Dön", e -> {
            UI.getCurrent().getPage().setLocation("/login/");
        });
        backBtn.addClassName("modern-btn-secondary");
        backBtn.getStyle().set("width", "240px").set("max-width", "240px").set("margin-top", "10px");

        SupportRequest existingRequest = null;
        if (currentUser != null && supportRequestService != null) {
            existingRequest = supportRequestService.getAllSupportRequests().stream()
                    .filter(req -> req.getSender() != null && req.getSender().getId().equals(currentUser.getId()))
                    .filter(req -> req.getSubject() != null && req.getSubject().contains(USER_APPROVAL_PREFIX))
                    .reduce((first, second) -> second)
                    .orElse(null);
        }

        if (existingRequest != null) {
            String statusName = existingRequest.getStatus() != null ? existingRequest.getStatus().name() : "";

            if ("REJECTED".equalsIgnoreCase(statusName)) {
                waitingTitle.setText("Başvurunuz Reddedildi");
                waitingTitle.getStyle().set("color", "var(--lumo-error-text-color)");
                waitingMsg.setText("Maalesef sistemimize yapmış olduğunuz kayıt başvurunuz yönetici tarafından reddedilmiştir.");
            } else {
                waitingTitle.setText("Başvurunuz İncelemede");
                waitingTitle.getStyle().set("color", "#0f172a");
                waitingMsg.setText("Kayıt başvurunuz sistem yöneticisi tarafından kontrol ediliyor. Lütfen daha sonra tekrar deneyin.");
            }

            add(logo, waitingTitle, waitingMsg, backBtn);
            return; 
        }

        ComboBox<Department> departmentComboBox = new ComboBox<>("Departman Seçimi");
        departmentComboBox.setItemLabelGenerator(dept -> dept.getName() != null ? dept.getName() : "");
        departmentComboBox.setWidth("240px");

        ComboBox<Company> companyComboBox = new ComboBox<>("Şirket Seçimi");
        companyComboBox.setItemLabelGenerator(comp -> comp.getName() != null ? comp.getName() : "");
        companyComboBox.setWidth("240px");

        final boolean isCustomer;
        final boolean isProductManager;
        
        if (currentUser != null && currentUser.getRole() != null) {
            isCustomer = "CUSTOMER".equalsIgnoreCase(currentUser.getRole().name());
            isProductManager = "PRODUCT_MANAGER".equalsIgnoreCase(currentUser.getRole().name());
        } else {
            isCustomer = "CUSTOMER".equalsIgnoreCase(currentRole);
            isProductManager = "PRODUCT_MANAGER".equalsIgnoreCase(currentRole);
        }

        if (isProductManager) {
            companyComboBox.setVisible(false);
            departmentComboBox.setVisible(false);
        } else if (isCustomer) {
            if (companyService != null) {
                try {
                    List<Company> companies = companyService.getAllCompanies();
                    companyComboBox.setItems(companies);
                } catch (Exception e) {
                    Notification.show("Şirketler yüklenirken hata oluştu.");
                }
            }
            companyComboBox.setVisible(true);
            departmentComboBox.setVisible(false);
        } else {
            if (departmentService != null) {
                try {
                    List<Department> departments = departmentService.getAllDepartments();
                    departmentComboBox.setItems(departments);
                } catch (Exception e) {
                    Notification.show("Departmanlar yüklenirken hata oluştu.");
                }
            }
            departmentComboBox.setVisible(true);
            companyComboBox.setVisible(false);
        }

        TextArea reasonTextArea = new TextArea("Katılım Nedeni");
        reasonTextArea.setPlaceholder("Neden katılmak istiyorsunuz?");
        reasonTextArea.setWidth("240px");
        reasonTextArea.setHeight("100px");

        Button sendRequestBtn = new Button("Onay Talebi Gönder", e -> {
            if (currentUser == null) {
                Notification.show("Kullanıcı bilgisi bulunamadı!")
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            String details = reasonTextArea.getValue();
            if (details == null || details.trim().isEmpty()) {
                Notification.show("Lütfen bir katılım nedeni girin.")
                        .addThemeVariants(NotificationVariant.LUMO_WARNING);
                return;
            }

            StringBuilder fullMessage = new StringBuilder();
            fullMessage.append("Rol: ").append(currentUser.getRole() != null ? currentUser.getRole().name() : currentRole).append("\n");
            
            if (isCustomer) {
                Company selectedCompany = companyComboBox.getValue();
                if (selectedCompany == null) {
                    Notification.show("Lütfen bir şirket seçin.").addThemeVariants(NotificationVariant.LUMO_WARNING);
                    return;
                }
                fullMessage.append("Seçilen Şirket: ").append(selectedCompany.getName()).append("\n");
                currentUser.setCompany(selectedCompany);
            } else if (!isProductManager){
                Department selectedDept = departmentComboBox.getValue();
                if (selectedDept == null) {
                    Notification.show("Lütfen bir departman seçin.").addThemeVariants(NotificationVariant.LUMO_WARNING);
                    return;
                }
                fullMessage.append("Seçilen Departman: ").append(selectedDept.getName()).append("\n");
                currentUser.setDepartment(selectedDept);
            }
            
            fullMessage.append("Açıklama: ").append(details);

            userService.updateUser(currentUser);

            SupportRequest supportRequest = new SupportRequest(
                currentUser, 
                USER_APPROVAL_PREFIX + " " + userName, 
                fullMessage.toString()
            );

            supportRequestService.saveSupportRequest(supportRequest);

            Notification.show("Talebiniz başarıyla gönderildi!", 3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            
            this.removeAll();
            waitingTitle.setText("Başvurunuz İncelemede");
            waitingTitle.getStyle().set("color", "#0f172a");
            waitingMsg.setText("Kayıt başvurunuz sistem yöneticisi tarafından kontrol ediliyor. Lütfen daha sonra tekrar deneyin.");
            this.add(logo, waitingTitle, waitingMsg, backBtn);
        });
        
        sendRequestBtn.addClassName("modern-btn-primary");
        sendRequestBtn.getStyle().set("width", "240px").set("max-width", "240px");

        add(logo, waitingTitle, waitingMsg, departmentComboBox, companyComboBox, reasonTextArea, sendRequestBtn, backBtn);
    }
}