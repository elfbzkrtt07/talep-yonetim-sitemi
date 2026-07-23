package com.example.views.customer;

import com.example.base.ui.MainLayout;
import com.example.entities.Request;
import com.example.entities.User;
import com.example.entities.WorkflowLog;
import com.example.enums.RequestStatus;
import com.example.repositories.WorkflowLogRepository;
import com.example.services.RequestService;
import com.example.services.SystemSettingService;
import com.example.services.WorkflowLogService;
import com.example.views.auth.SystemSettingsView;
import com.example.views.base.BaseSecuredView;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;

import java.io.ByteArrayInputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Route(value = "customer/dashboard", layout = MainLayout.class)
public class CustomerDashboardView extends BaseSecuredView {

    private final RequestService requestService;
    private final WorkflowLogRepository workflowLogRepository;
    private final WorkflowLogService workflowLogService;
    private final SystemSettingService systemSettingService;

    private final Grid<Request> requestGrid = new Grid<>(Request.class, false);
    
    private final H2 pageTitle = new H2("Hoşgeldiniz");
    private final Paragraph introParagraph = new Paragraph("Aşağıdaki tablodan mevcut taleplerinizi inceleyebilir, durumlarını takip edebilir veya düzenleyebilirsiniz.");

    private String uploadedFileName = null;
    private byte[] uploadedFileBytes = null;

    public CustomerDashboardView(RequestService requestService, 
                                 WorkflowLogRepository workflowLogRepository,
                                 WorkflowLogService workflowLogService,
                                 SystemSettingService systemSettingService) {
        this.requestService = requestService;
        this.workflowLogRepository = workflowLogRepository;
        this.workflowLogService = workflowLogService;
        this.systemSettingService = systemSettingService;

        setSizeFull();
        setPadding(true);
        getStyle().set("background-color", "#f8fafc");

        Image logo = new Image(
            com.vaadin.flow.server.streams.DownloadHandler.forClassResource(getClass(), "/META-INF/resources/logo.png"), 
            "MONAD Logo");
        logo.addClassName("center-brand-logo");
        logo.setHeight("70px");
        
        HorizontalLayout logoLayout = new HorizontalLayout(logo);
        logoLayout.setWidthFull();
        logoLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

        pageTitle.getStyle()
            .set("font-size", "2.5rem")
            .set("font-weight", "1000")
            .set("margin", "20px auto 10px auto") 
            .set("text-align", "center");
        pageTitle.setWidthFull();

        introParagraph.getStyle()
            .set("color", "#64748b")
            .set("margin", "0 auto 30px auto") 
            .set("text-align", "center");
        introParagraph.setWidthFull();

        HorizontalLayout actionBar = new HorizontalLayout();
        actionBar.setWidthFull();
        actionBar.getStyle().set("padding", "0 40px"); 

        Button createRequestBtn = new Button("Yeni Talep Oluştur", VaadinIcon.PLUS.create());
        createRequestBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        createRequestBtn.getStyle().set("margin-left", "auto");
        createRequestBtn.addClickListener(event -> openNewRequestModal());
        actionBar.add(createRequestBtn);

        requestGrid.addColumn(Request::getId).setHeader("ID").setWidth("80px").setFlexGrow(0);
        requestGrid.addColumn(Request::getTitle).setHeader("Başlık");
        requestGrid.addColumn(request -> request.getStatus() != null ? request.getStatus().toString() : "").setHeader("Durum"); 
        
        requestGrid.addColumn(request -> {
            if (request.getCreatedAt() == null) {
                return "";
            }
            return request.getCreatedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
        }).setHeader("Tarih");
        
        requestGrid.addComponentColumn(request -> {
            Button editBtn = new Button("Düzenle", VaadinIcon.EDIT.create());
            editBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            editBtn.addClickListener(e -> openEditRequestModal(request));
            return editBtn;
        }).setHeader("İşlemler").setWidth("150px").setFlexGrow(0);
        
        requestGrid.setWidth("calc(100% - 80px)"); 
        requestGrid.getStyle().set("margin", "0 auto 20px auto"); 

        add(logoLayout, pageTitle, introParagraph, actionBar, requestGrid);
    }

    @Override
    protected void onUserAuthenticated(BeforeEnterEvent event, User user) {
        this.currentUser = user;
        Map<String, List<String>> parameters = event.getLocation().getQueryParameters().getParameters();

        List<Request> userRequests = requestService.getAllRequests().stream()
                .filter(r -> r.getCustomer() != null && r.getCustomer().getId().equals(currentUser.getId()))
                .toList();

        if (parameters.containsKey("filter") && "sent_back".equals(parameters.get("filter").get(0))) {
            pageTitle.setText("Geri Dönen Talepleriniz");
            introParagraph.setText("Yöneticiler tarafından revizyon istenen ve aksiyon almanız gereken talepler listelenmektedir.");
            
            List<Request> sentBackRequests = userRequests.stream()
                    .filter(r -> r.getStatus() == RequestStatus.SENT_BACK)
                    .toList();
            requestGrid.setItems(sentBackRequests);
            
        } else {
            pageTitle.setText("Hoşgeldiniz, " + currentUser.getName());
            introParagraph.setText("Aşağıdaki tablodan mevcut taleplerinizi inceleyebilir, durumlarını takip edebilir veya düzenleyebilirsiniz.");
            requestGrid.setItems(userRequests);
        }

        if (parameters.containsKey("action") && "new".equals(parameters.get("action").get(0))) {
            UI.getCurrent().access(this::openNewRequestModal);
        }
    }

    private void refreshGridItems() {
        if (currentUser == null) return;

        List<Request> userRequests = requestService.getAllRequests().stream()
                .filter(r -> r.getCustomer() != null && r.getCustomer().getId().equals(currentUser.getId()))
                .toList();

        if ("Geri Dönen Talepleriniz".equals(pageTitle.getText())) {
            requestGrid.setItems(userRequests.stream()
                    .filter(r -> r.getStatus() == RequestStatus.SENT_BACK)
                    .toList());
        } else {
            requestGrid.setItems(userRequests);
        }
    }

    private void openNewRequestModal() {
        Dialog modal = new Dialog();
        modal.setHeaderTitle("Yeni Destek/Geliştirme Talebi Oluştur");
        modal.setCloseOnOutsideClick(false);

        TextField titleField = new TextField("Talep Başlığı");
        titleField.setWidthFull();
        titleField.setPlaceholder("Lütfen kısa bir başlık yazın...");

        TextArea descriptionField = new TextArea("Talep Detayı ve Açıklama");
        descriptionField.setWidthFull();
        descriptionField.setHeight("130px");
        descriptionField.setPlaceholder("Talebinizin detaylarını buraya girin...");

        IntegerField affectedNoField = new IntegerField("Etkilenen Sayısı / No");
        affectedNoField.setWidthFull();
        affectedNoField.setMin(0);
        affectedNoField.setPlaceholder("Kaç kişi/sistem etkileniyor?");
        affectedNoField.setVisible(false);

        DatePicker deadlineField = new DatePicker("Hedeflenen Tarih (Deadline)");
        deadlineField.setWidthFull();
        deadlineField.setVisible(false);

        Checkbox securityCheckbox = new Checkbox("Kritik: KVKK İhlali veya Güvenlik Zafiyeti İçeriyor");
        securityCheckbox.getStyle().set("color", "var(--lumo-error-text-color)").set("font-weight", "bold");
        securityCheckbox.setVisible(false);

        Select<String> financialImpactField = new Select<>();
        financialImpactField.setLabel("Mali / Finansal Etki Seviyesi");
        financialImpactField.setItems("Etkisi Yok", "Düşük Zarar", "Orta Düzey Zarar", "Kritik / İş Durdurucu Zarar");
        financialImpactField.setValue("Etkisi Yok");
        financialImpactField.setWidthFull();
        financialImpactField.setVisible(false);

        HorizontalLayout optionalButtonsLayout = new HorizontalLayout();
        optionalButtonsLayout.getStyle()
                .set("flex-wrap", "wrap")
                .set("gap", "8px")
                .set("margin-top", "10px")
                .set("margin-bottom", "10px");

        if (systemSettingService.isFeatureEnabled(SystemSettingsView.KEY_AFFECTED_NO, true)) {
            Button btnAffected = new Button("+ Etkilenen Sayısı", VaadinIcon.USERS.create());
            btnAffected.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_CONTRAST);
            btnAffected.addClickListener(e -> {
                affectedNoField.setVisible(!affectedNoField.isVisible());
                btnAffected.setText(affectedNoField.isVisible() ? "- Etkilenen Sayısı" : "+ Etkilenen Sayısı");
            });
            optionalButtonsLayout.add(btnAffected);
        }

        if (systemSettingService.isFeatureEnabled(SystemSettingsView.KEY_DEADLINE, true)) {
            Button btnDeadline = new Button("+ Target Deadline", VaadinIcon.CALENDAR.create());
            btnDeadline.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_CONTRAST);
            btnDeadline.addClickListener(e -> {
                deadlineField.setVisible(!deadlineField.isVisible());
                btnDeadline.setText(deadlineField.isVisible() ? "- Target Deadline" : "+ Target Deadline");
            });
            optionalButtonsLayout.add(btnDeadline);
        }

        if (systemSettingService.isFeatureEnabled(SystemSettingsView.KEY_SECURITY_RISK, true)) {
            Button btnSecurity = new Button("+ Güvenlik / KVKK Riski", VaadinIcon.SHIELD.create());
            btnSecurity.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
            btnSecurity.addClickListener(e -> {
                securityCheckbox.setVisible(!securityCheckbox.isVisible());
                if (!securityCheckbox.isVisible()) securityCheckbox.setValue(false);
                btnSecurity.setText(securityCheckbox.isVisible() ? "- Güvenlik / KVKK Riski" : "+ Güvenlik / KVKK Riski");
            });
            optionalButtonsLayout.add(btnSecurity);
        }

        if (systemSettingService.isFeatureEnabled(SystemSettingsView.KEY_FINANCIAL_IMPACT, true)) {
            Button btnFinancial = new Button("+ Mali Etki", VaadinIcon.DOLLAR.create());
            btnFinancial.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_CONTRAST);
            btnFinancial.addClickListener(e -> {
                financialImpactField.setVisible(!financialImpactField.isVisible());
                btnFinancial.setText(financialImpactField.isVisible() ? "- Mali Etki" : "+ Mali Etki");
            });
            optionalButtonsLayout.add(btnFinancial);
        }

        MemoryBuffer buffer = new MemoryBuffer();
        Upload uploadField = new Upload(buffer);
        uploadField.setMaxFileSize(5 * 1024 * 1024);
        uploadField.setUploadButton(new Button("Dosya Ekle", VaadinIcon.UPLOAD.create()));
        uploadField.setDropLabel(new Paragraph("Veya dosyayı buraya sürükleyin"));

        VerticalLayout modalBody = new VerticalLayout(
            titleField, 
            descriptionField, 
            optionalButtonsLayout, 
            affectedNoField, 
            deadlineField, 
            securityCheckbox, 
            financialImpactField, 
            uploadField
        );
        modalBody.setPadding(false);
        modalBody.setSpacing(true);
        modalBody.setWidth("550px");

        Button cancelBtn = new Button("İptal Et", e -> modal.close());
        Button submitBtn = new Button("Oluştur", VaadinIcon.PAPERPLANE.create());
        submitBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        submitBtn.addClickListener(e -> {
            String title = titleField.getValue().trim();
            String description = descriptionField.getValue().trim();

            if (title.isEmpty() || description.isEmpty()) {
                Notification.show("Başlık ve açıklama alanları zorunludur!")
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }
            
            if (currentUser == null) {
                Notification.show("Oturum zaman aşımına uğradı. Yeniden giriş yapın.")
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            Request newRequest = new Request();
            newRequest.setCustomer(currentUser);
            newRequest.setTitle(title);
            newRequest.setDescription(description);
            
            if (affectedNoField.isVisible() && affectedNoField.getValue() != null) {
                newRequest.setAffectedNo(affectedNoField.getValue());
            }
            if (deadlineField.isVisible() && deadlineField.getValue() != null) {
                newRequest.setDeadline(deadlineField.getValue());
            }
            if (securityCheckbox.isVisible() && securityCheckbox.getValue()) {
                newRequest.setIsSecurityRisk(true);
            }
            if (financialImpactField.isVisible() && !financialImpactField.getValue().isBlank()) {
                newRequest.setFinancialImpact(financialImpactField.getValue().trim());
            }

            Request savedRequest = requestService.submitRequest(newRequest); 

            String fileName = null;
            byte[] fileBytes = null;

            if (!buffer.getFileName().isEmpty()) {
                try {
                    fileName = buffer.getFileName();
                    fileBytes = buffer.getInputStream().readAllBytes();
                } catch (java.io.IOException ex) {
                    Notification.show("Dosya okunamadı: " + ex.getMessage())
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                    return;
                }
            }

            workflowLogService.saveChatComment(
                savedRequest.getId(), 
                "[TALEP OLUŞTURULDU]: " + description, 
                currentUser, 
                fileName, 
                fileBytes
            );

            Notification.show("Talep başarıyla oluşturuldu.")
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            
            modal.close();
            refreshGridItems(); 
        });

        modal.add(modalBody);
        cancelBtn.getStyle().set("margin-left", "auto"); 
        modal.getFooter().add(cancelBtn, submitBtn);
        modal.open();
    }

    private void openEditRequestModal(Request request) {
        Dialog modal = new Dialog();
        modal.setHeaderTitle("Destek/Geliştirme Talebini Düzenle");
        modal.setCloseOnOutsideClick(false);

        uploadedFileName = null;
        uploadedFileBytes = null;

        VerticalLayout modalBody = new VerticalLayout();
        modalBody.setPadding(false);
        modalBody.setSpacing(true);
        modalBody.setWidth("500px");

        if (request.getStatus() == RequestStatus.SENT_BACK) {
            String feedbackNote = null;

            if (workflowLogRepository != null) {
                List<WorkflowLog> logs = workflowLogRepository.findByRequestIdWithUser(request.getId()).stream()
                        .sorted((l1, l2) -> l2.getCreatedAt().compareTo(l1.getCreatedAt()))
                        .toList();

                for (WorkflowLog log : logs) {
                    String logText = log.getLogText();
                    if (logText != null && logText.contains("[MÜŞTERİYE İADE EDİLDİ]")) {
                        feedbackNote = logText.replace("[MÜŞTERİYE İADE EDİLDİ]: Gerekçe:", "").trim();
                        break; 
                    }
                }
            }

            if (feedbackNote != null && !feedbackNote.isEmpty()) {
                TextArea pmNoteField = new TextArea("Yönetici Geri Bildirimi / Revizyon Notu");
                pmNoteField.setWidthFull();
                pmNoteField.setValue(feedbackNote);
                pmNoteField.setReadOnly(true);
                pmNoteField.getStyle()
                    .set("border", "1px solid #fca5a5")
                    .set("background-color", "#fef2f2")
                    .set("color", "#991b1b")
                    .set("border-radius", "8px")
                    .set("padding", "5px");
                modalBody.add(pmNoteField);
            }
        }

        TextField titleField = new TextField("Talep Başlığı");
        titleField.setWidthFull();
        titleField.setValue(request.getTitle() != null ? request.getTitle() : "");

        IntegerField affectedNoField = new IntegerField("Etkilenen Sayısı / No");
        affectedNoField.setWidthFull();
        affectedNoField.setMin(0);
        affectedNoField.setValue(request.getAffectedNo() != null ? request.getAffectedNo() : 0);

        TextArea descriptionField = new TextArea("Talep Detayı ve Açıklama");
        descriptionField.setWidthFull();
        descriptionField.setHeight("120px");
        descriptionField.setValue(request.getDescription() != null ? request.getDescription() : "");

        modalBody.add(titleField, affectedNoField, descriptionField);

        VerticalLayout chatSection = new VerticalLayout();
        chatSection.setPadding(false);
        chatSection.setSpacing(true);
        chatSection.setWidthFull();

        Span chatTitle = new Span("Talep İletişim Geçmişi");
        chatTitle.getStyle()
            .set("font-weight", "bold")
            .set("font-size", "1.1rem")
            .set("margin-top", "10px");

        VerticalLayout chatHistoryArea = new VerticalLayout();
        chatHistoryArea.setWidthFull();
        chatHistoryArea.setHeight("220px");
        chatHistoryArea.getStyle()
                .set("overflow-y", "auto")
                .set("background", "#ffffff")
                .set("border", "1px solid #cbd5e1")
                .set("border-radius", "8px")
                .set("padding", "12px");

        List<WorkflowLog> customerVisibleLogs = workflowLogRepository.findByRequestIdWithUser(request.getId()).stream()
                .filter(log -> !log.isInternal()) 
                .sorted((l1, l2) -> l1.getCreatedAt().compareTo(l2.getCreatedAt())) 
                .toList();

        for (WorkflowLog log : customerVisibleLogs) {
            HorizontalLayout row = new HorizontalLayout();
            row.setWidthFull();
            
            Div bubble = new Div();
            bubble.setMaxWidth("80%");
            bubble.getStyle()
                    .set("padding", "10px 14px")
                    .set("border-radius", "12px")
                    .set("font-size", "0.9rem")
                    .set("line-height", "1.4");

            boolean isMe = currentUser != null && log.getUser() != null && log.getUser().getId().equals(currentUser.getId());
            String roleStr = log.getUser() != null ? log.getUser().getRole().toString() : "";

            if (isMe) {
                row.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
                bubble.getStyle().set("background-color", "#dcfce7").set("color", "#14532d").set("border-bottom-right-radius", "2px");
                
                Span label = new Span("Siz (" + roleStr + ")");
                label.getStyle().set("font-weight", "bold").set("display", "block").set("margin-bottom", "4px");
                bubble.add(label);
            } else {
                row.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
                bubble.getStyle().set("background-color", "#f1f5f9").set("color", "#1e293b").set("border-bottom-left-radius", "2px");
                
                Span label = new Span((log.getUser() != null ? log.getUser().getName() : "Sistem") + " (" + roleStr + ")");
                label.getStyle().set("font-weight", "bold").set("display", "block").set("margin-bottom", "4px");
                bubble.add(label);
            }

            if (log.getLogText() != null && !log.getLogText().isEmpty()) {
                String text = log.getLogText();
                if (text.contains("[MÜŞTERİYE İADE EDİLDİ]")) {
                    text = text.replace("[MÜŞTERİYE İADE EDİLDİ]: Gerekçe:", "").trim();
                } else if (text.contains("[TALEP OLUŞTURULDU]")) {
                    text = text.replace("[TALEP OLUŞTURULDU]:", "").trim();
                } else if (text.contains("[TALEP GÜNCELLENDİ]")) {
                    text = text.replace("[TALEP GÜNCELLENDİ]", "").trim();
                }

                Div textDiv = new Div(new Span(text));
                textDiv.getStyle().set("margin-top", "4px").set("white-space", "pre-wrap");
                bubble.add(textDiv);
            }

            if (log.getFileName() != null && log.getFileBytes() != null) {
                String name = log.getFileName();
                byte[] bytes = log.getFileBytes();
                
                StreamResource resource = new StreamResource(name, () -> new ByteArrayInputStream(bytes));
                Anchor downloadAnchor = new Anchor(resource, "📁 " + name);
                downloadAnchor.getElement().setAttribute("download", true);
                downloadAnchor.getStyle().set("display", "block").set("margin-top", "6px").set("font-weight", "bold").set("color", isMe ? "#166534" : "#1d4ed8");
                bubble.add(downloadAnchor);
            }

            row.add(bubble);
            chatHistoryArea.add(row);
        }
        chatHistoryArea.getElement().executeJs("this.scrollTop = this.scrollHeight;");

        TextArea chatInputArea = new TextArea();
        chatInputArea.setPlaceholder("Mesajınızı yazın...");
        chatInputArea.setWidthFull();
        chatInputArea.setHeight("65px");
        chatInputArea.getStyle().set("margin-bottom", "5px");

        MemoryBuffer chatFileBuffer = new MemoryBuffer();
        Upload chatFileUpload = new Upload(chatFileBuffer);
        chatFileUpload.setMaxFiles(1);
        chatFileUpload.setAcceptedFileTypes(".pdf", ".png", ".jpg", ".docx", ".xlsx");
        chatFileUpload.getStyle().set("margin-bottom", "10px");
        chatFileUpload.addSucceededListener(event -> {
            try {
                this.uploadedFileName = event.getFileName();
                this.uploadedFileBytes = chatFileBuffer.getInputStream().readAllBytes();
                Notification.show("Dosya hazır: " + uploadedFileName).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (Exception ex) {
                Notification.show("Dosya okunamadı!").addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        Button sendChatBtn = new Button("Mesaj Gönder", e -> {
            if (currentUser == null) return;
            if (chatInputArea.isEmpty() && uploadedFileBytes == null) {
                Notification.show("Boş mesaj gönderilemez!").addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            try {
                workflowLogService.saveChatComment(
                    request.getId(), 
                    chatInputArea.getValue(), 
                    currentUser, 
                    uploadedFileName, 
                    uploadedFileBytes
                );

                chatInputArea.clear();
                chatFileUpload.clearFileList();
                this.uploadedFileBytes = null;
                this.uploadedFileName = null;
                
                modal.close();
                openEditRequestModal(request);
                Notification.show("Mesajınız iletildi.").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (Exception ex) {
                Notification.show("Hata: " + ex.getMessage()).addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        sendChatBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        sendChatBtn.setWidthFull();

        chatSection.add(chatTitle, chatHistoryArea, chatInputArea, chatFileUpload, sendChatBtn);
        modalBody.add(chatSection);

        Button deleteBtn = new Button("Talebi Sil", VaadinIcon.TRASH.create(), e -> {
            RequestStatus status = request.getStatus();
            
            if (status == RequestStatus.APPROVED || status == RequestStatus.COMPLETED) {
                Notification.show("Onaylanmış veya tamamlanmış talepler silinemez!")
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            requestService.deleteRequest(request);
            Notification.show("Talep başarıyla silindi.")
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            modal.close();
            refreshGridItems();
        });
        
        deleteBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
        deleteBtn.setEnabled(request.getStatus() != RequestStatus.APPROVED 
                    && request.getStatus() != RequestStatus.COMPLETED);

        Button cancelBtn = new Button("İptal Et", e -> modal.close());
        Button submitBtn = new Button("Güncelle", VaadinIcon.CHECK.create());
        submitBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        submitBtn.addClickListener(e -> {
            String title = titleField.getValue().trim();
            String description = descriptionField.getValue().trim();
            Integer affectedNo = affectedNoField.getValue();

            if (title.isEmpty() || description.isEmpty() || affectedNo == null) {
                Notification.show("Lütfen tüm alanları doldurun!")
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            request.setTitle(title);
            request.setDescription(description);
            request.setAffectedNo(affectedNo);
            request.setUpdatedAt(java.time.LocalDateTime.now()); 

            String fileName = null;
            byte[] fileBytes = null;

            if (!chatFileBuffer.getFileName().isEmpty()) {
                try {
                    fileName = chatFileBuffer.getFileName();
                    fileBytes = chatFileBuffer.getInputStream().readAllBytes();
                } catch (java.io.IOException ex) {
                    Notification.show("Dosya güncellenirken hata oluştu: " + ex.getMessage())
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                    return;
                }
            }

            requestService.updateRequest(request);

            workflowLogService.saveChatComment(
                request.getId(), 
                "[TALEP GÜNCELLENDİ] Müşteri talebini güncelledi.", 
                currentUser, 
                fileName, 
                fileBytes
            );

            Notification.show("Talep başarıyla güncellendi.")
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            
            modal.close();
            refreshGridItems(); 
        });

        modal.add(modalBody);
        cancelBtn.getStyle().set("margin-left", "auto"); 
        modal.getFooter().add(deleteBtn, cancelBtn, submitBtn);
        modal.open();
    }
}