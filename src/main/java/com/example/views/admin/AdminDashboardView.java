package com.example.views.admin;

import com.example.base.ui.MainLayout;
import com.example.entities.SupportRequest;
import com.example.entities.User;
import com.example.enums.RequestStatus;
import com.example.services.EmailService;
import com.example.services.SupportRequestService;
import com.example.services.UserService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.time.format.DateTimeFormatter;
import java.util.List;

@PageTitle("Yönetici Paneli")
@Route(value = "admin/dashboard", layout = MainLayout.class)
public class AdminDashboardView extends VerticalLayout {

    private final SupportRequestService supportRequestService;
    private final UserService userService;
    private final EmailService emailService;

    private final Grid<SupportRequest> userApprovalsGrid = new Grid<>(SupportRequest.class, false);
    private final Grid<SupportRequest> customerSupportGrid = new Grid<>(SupportRequest.class, false);

    private static final String USER_APPROVAL_PREFIX = "[KULLANICI ONAYI]";
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public AdminDashboardView(SupportRequestService supportRequestService, UserService userService, EmailService emailService) {
        this.supportRequestService = supportRequestService;
        this.userService = userService;
        this.emailService = emailService;

        setSizeFull();
        setPadding(true);
        getStyle().set("background-color", "#f8fafc");

        VerticalLayout headerBanner = createHeaderBanner(
                "Yönetici Paneli", 
                "Sistemdeki özel kullanıcı onay taleplerini ve müşteri destek taleplerini buradan takip edebilirsiniz."
        );
        add(headerBanner);

        H3 approvalsTitle = new H3("Kullanıcı Onay Talepleri");
        approvalsTitle.getStyle().set("margin-top", "20px").set("margin-bottom", "10px");
        
        userApprovalsGrid.addColumn(SupportRequest::getId).setHeader("ID").setAutoWidth(true).setFlexGrow(0);
        userApprovalsGrid.addColumn(req -> req.getSender() != null ? req.getSender().getName() : "Bilinmiyor").setHeader("Kullanıcı").setAutoWidth(true);
        userApprovalsGrid.addColumn(SupportRequest::getSubject).setHeader("Konu").setFlexGrow(2);
        userApprovalsGrid.addColumn(req -> req.getCreatedAt() != null ? req.getCreatedAt().format(formatter) : "-")
                .setHeader("Tarih").setAutoWidth(true);

        userApprovalsGrid.addComponentColumn(req -> {
            Button manageBtn = new Button("İncele", VaadinIcon.EYE.create());
            manageBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);
            manageBtn.addClickListener(e -> openUserApprovalDialog(req));
            return manageBtn;
        }).setHeader("İşlem").setAutoWidth(true);
        
        userApprovalsGrid.setHeight("250px");
        userApprovalsGrid.getStyle().set("border", "1px solid #cbd5e1").set("border-radius", "8px");

        H3 supportTitle = new H3("Müşteri Destek Talepleri");
        supportTitle.getStyle().set("margin-top", "30px").set("margin-bottom", "10px");

        customerSupportGrid.addColumn(SupportRequest::getId).setHeader("Talep ID").setAutoWidth(true).setFlexGrow(0);
        customerSupportGrid.addColumn(req -> req.getSender() != null ? req.getSender().getName() : "Bilinmiyor").setHeader("Gönderen").setAutoWidth(true);
        customerSupportGrid.addColumn(SupportRequest::getSubject).setHeader("Konu").setFlexGrow(2);
        customerSupportGrid.addColumn(req -> req.getStatus() != null ? req.getStatus().toString() : "-").setHeader("Durum").setAutoWidth(true);
        customerSupportGrid.addColumn(req -> req.getCreatedAt() != null ? req.getCreatedAt().format(formatter) : "-")
                .setHeader("Tarih").setAutoWidth(true);

        customerSupportGrid.addComponentColumn(req -> {
            Button inspectBtn = new Button("İncele", VaadinIcon.EYE.create());
            inspectBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            inspectBtn.addClickListener(e -> openCustomerSupportDialog(req)); 
            return inspectBtn;
        }).setHeader("İşlem").setAutoWidth(true);

        customerSupportGrid.setHeight("300px");
        customerSupportGrid.getStyle().set("border", "1px solid #cbd5e1").set("border-radius", "8px");

        add(approvalsTitle, userApprovalsGrid, supportTitle, customerSupportGrid);

        loadDashboardData();
    }

    private void loadDashboardData() {
        List<SupportRequest> allRequests = supportRequestService.getAllSupportRequests();

        List<SupportRequest> userApprovals = allRequests.stream()
                .filter(req -> req.getSubject() != null && req.getSubject().startsWith(USER_APPROVAL_PREFIX))
                .filter(req -> req.getStatus() == RequestStatus.PENDING)
                .toList();
        userApprovalsGrid.setItems(userApprovals);

        List<SupportRequest> customerSupport = allRequests.stream()
                .filter(req -> req.getSubject() == null || !req.getSubject().startsWith(USER_APPROVAL_PREFIX))
                .filter(req -> req.getStatus() == RequestStatus.PENDING)
                .toList();
        customerSupportGrid.setItems(customerSupport);
    }
    
    private void openUserApprovalDialog(SupportRequest request) {
        Dialog dialog = new Dialog();
        dialog.setWidth("500px");
        dialog.setCloseOnEsc(true);
        dialog.setCloseOnOutsideClick(true);

        H3 title = new H3("Kullanıcı Onay Talebi");
        title.getStyle().set("margin-top", "0");

        TextField senderField = new TextField("Gönderen");
        senderField.setValue(request.getSender() != null ? request.getSender().getName() : "Bilinmiyor");
        senderField.setReadOnly(true);
        senderField.setWidthFull();

        TextField dateField = new TextField("Tarih");
        dateField.setValue(request.getCreatedAt() != null ? request.getCreatedAt().format(formatter) : "-");
        dateField.setReadOnly(true);
        dateField.setWidthFull();

        HorizontalLayout row1 = new HorizontalLayout(senderField, dateField);
        row1.setWidthFull();

        TextField subjectField = new TextField("Konu");
        subjectField.setValue(request.getSubject() != null ? request.getSubject() : "");
        subjectField.setReadOnly(true);
        subjectField.setWidthFull();

        TextArea messageArea = new TextArea("Katılım Sebebi / Form Detayları");
        messageArea.setValue(request.getMessage() != null ? request.getMessage() : "");
        messageArea.setReadOnly(true);
        messageArea.setWidthFull();
        messageArea.setMinHeight("150px");

        Button closeButton = new Button("Kapat", e -> dialog.close());
        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        Button rejectButton = new Button("Reddet", VaadinIcon.CLOSE.create(), e -> {
            try {
                User sender = request.getSender();
                if (sender != null) {
                    userService.rejectUser(sender.getId());
                }

                request.setStatus(RequestStatus.COMPLETED);
                supportRequestService.saveSupportRequest(request);

                Notification.show("Kullanıcı başvurusu reddedildi.")
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);

                loadDashboardData();
                dialog.close();
            } catch (Exception ex) {
                Notification.show("Hata: " + ex.getMessage())
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        rejectButton.addThemeVariants(ButtonVariant.LUMO_ERROR);

        Button approveButton = new Button("Onayla", VaadinIcon.CHECK.create(), e -> {
            try {
                if (request.getSender() != null) {
                    userService.approveUser(request.getSender().getId());
                }
                
                request.setStatus(RequestStatus.COMPLETED);
                supportRequestService.saveSupportRequest(request);
                
                Notification.show("Kullanıcı başarıyla onaylandı!")
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                        
                loadDashboardData();
                dialog.close();
            } catch (Exception ex) {
                Notification.show("Hata: " + ex.getMessage())
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        approveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);

        HorizontalLayout footer = new HorizontalLayout(closeButton, rejectButton, approveButton);
        footer.setWidthFull();
        footer.setJustifyContentMode(JustifyContentMode.END);

        VerticalLayout dialogLayout = new VerticalLayout(title, row1, subjectField, messageArea, footer);
        dialogLayout.setPadding(false);
        dialogLayout.setSpacing(true);

        dialog.add(dialogLayout);
        dialog.open();
    }

    private void openCustomerSupportDialog(SupportRequest request) {
        Dialog dialog = new Dialog();
        dialog.setWidth("500px");
        dialog.setCloseOnEsc(true);
        dialog.setCloseOnOutsideClick(true);

        H3 title = new H3("Destek Talebi Detayı");
        title.getStyle().set("margin-top", "0");

        TextField senderField = new TextField("Gönderen");
        senderField.setValue(request.getSender() != null ? request.getSender().getName() : "Bilinmiyor");
        senderField.setReadOnly(true);
        senderField.setWidthFull();

        TextField dateField = new TextField("Tarih");
        dateField.setValue(request.getCreatedAt() != null ? request.getCreatedAt().format(formatter) : "-");
        dateField.setReadOnly(true);
        dateField.setWidthFull();

        HorizontalLayout row1 = new HorizontalLayout(senderField, dateField);
        row1.setWidthFull();

        TextField subjectField = new TextField("Konu");
        subjectField.setValue(request.getSubject() != null ? request.getSubject() : "");
        subjectField.setReadOnly(true);
        subjectField.setWidthFull();

        TextArea messageArea = new TextArea("Mesaj / Detay");
        messageArea.setValue(request.getMessage() != null ? request.getMessage() : "");
        messageArea.setReadOnly(true);
        messageArea.setWidthFull();
        messageArea.setMinHeight("150px");

        Button closeButton = new Button("Kapat", e -> dialog.close());
        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        Button actionButton = new Button("Talebe Git", VaadinIcon.ARROW_RIGHT.create());
        actionButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        actionButton.addClickListener(e -> {
            dialog.close();
            UI.getCurrent().navigate("admin/support");
        });

        Button completeButton = new Button("Çözüldü İşaretle", VaadinIcon.CHECK.create());
        completeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
        completeButton.addClickListener(e -> {
            try {
                request.setStatus(RequestStatus.COMPLETED);
                supportRequestService.saveSupportRequest(request);
                
                Notification.show("Talep çözüldü olarak işaretlendi!")
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                        
                loadDashboardData();
                dialog.close();
            } catch (Exception ex) {
                Notification.show("Hata: " + ex.getMessage())
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        HorizontalLayout footer = new HorizontalLayout(closeButton, actionButton, completeButton);
        footer.setWidthFull();
        footer.setJustifyContentMode(JustifyContentMode.END);

        VerticalLayout dialogLayout = new VerticalLayout(title, row1, subjectField, messageArea, footer);
        dialogLayout.setPadding(false);
        dialogLayout.setSpacing(true);

        dialog.add(dialogLayout);
        dialog.open();
    }

    private VerticalLayout createHeaderBanner(String titleText, String subtitleText) {
        VerticalLayout bannerLayout = new VerticalLayout();
        bannerLayout.setWidthFull();
        bannerLayout.setAlignItems(Alignment.CENTER); 
        bannerLayout.setJustifyContentMode(JustifyContentMode.CENTER);
        bannerLayout.setPadding(false);
        bannerLayout.setSpacing(true);
        bannerLayout.getStyle()
                .set("margin-top", "25px")
                .set("margin-bottom", "15px");

        H2 title = new H2(titleText);
        title.getStyle()
                .set("margin", "0")
                .set("font-size", "2.5rem") 
                .set("font-weight", "1000") 
                .set("color", "#0f172a")
                .set("text-align", "center");
        bannerLayout.add(title);

        if (subtitleText != null && !subtitleText.isEmpty()) {
            Span subtitle = new Span(subtitleText);
            subtitle.getStyle()
                    .set("margin-top", "8px") 
                    .set("font-size", "0.9rem")
                    .set("color", "#64748b")
                    .set("text-align", "center");
            bannerLayout.add(subtitle);
        }

        return bannerLayout;
    }
}