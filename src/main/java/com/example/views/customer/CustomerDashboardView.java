package com.example.views.customer;

import com.example.services.RequestService;
import com.example.services.WorkflowLogService;
import com.example.enums.RequestStatus;
import com.example.entities.Request;
import com.example.entities.User;
import com.example.entities.Prioritization;
import com.example.entities.WorkflowLog;
import com.example.repositories.PrioritizationRepository;
import com.example.repositories.WorkflowLogRepository;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.VaadinSession;
import com.example.base.ui.MainLayout;

import java.io.ByteArrayInputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Route(value = "customer/dashboard", layout = MainLayout.class)
public class CustomerDashboardView extends VerticalLayout implements BeforeEnterObserver {

    private final RequestService requestService;
    private final PrioritizationRepository prioritizationRepository;
    private final WorkflowLogRepository workflowLogRepository;
    private final WorkflowLogService workflowLogService;

    private final Grid<Request> requestGrid = new Grid<>(Request.class, false);
    
    private final H2 pageTitle = new H2("Hoşgeldiniz");
    private final Paragraph introParagraph = new Paragraph("Aşağıdaki tablodan mevcut taleplerinizi inceleyebilir, durumlarını takip edebilir veya düzenleyebilirsiniz.");

    // Dynamic fields used for holding uploads inside the active dialog
    private String uploadedFileName = null;
    private byte[] uploadedFileBytes = null;

    public CustomerDashboardView(RequestService requestService, 
                                 PrioritizationRepository prioritizationRepository,
                                 WorkflowLogRepository workflowLogRepository,
                                 WorkflowLogService workflowLogService) {
        this.requestService = requestService;
        this.prioritizationRepository = prioritizationRepository;
        this.workflowLogRepository = workflowLogRepository;
        this.workflowLogService = workflowLogService;

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
        logoLayout.setJustifyContentMode(JustifyContentMode.CENTER);

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
    public void beforeEnter(BeforeEnterEvent event) {
        User currentUser = (User) VaadinSession.getCurrent().getAttribute("user");
        if (currentUser == null) return;

        Map<String, List<String>> parameters = event.getLocation().getQueryParameters().getParameters();

        if (parameters.containsKey("filter") && "sent_back".equals(parameters.get("filter").get(0))) {
            pageTitle.setText("Geri Dönen Talepleriniz");
            introParagraph.setText("Yöneticiler tarafından revizyon istenen ve aksiyon almanız gereken talepler listelenmektedir.");
            
            List<Request> sentBackRequests = requestService.getAllRequests().stream()
                    .filter(r -> r.getStatus() == RequestStatus.SENT_BACK)
                    .toList();
            requestGrid.setItems(sentBackRequests);
            
        } else {
            pageTitle.setText("Hoşgeldiniz");
            introParagraph.setText("Aşağıdaki tablodan mevcut taleplerinizi inceleyebilir, durumlarını takip edebilir veya düzenleyebilirsiniz.");
            requestGrid.setItems(requestService.getAllRequests());
        }

        if (parameters.containsKey("action") && "new".equals(parameters.get("action").get(0))) {
            UI.getCurrent().access(this::openNewRequestModal);
        }
    }

    private void openNewRequestModal() {
        Dialog modal = new Dialog();
        modal.setHeaderTitle("Yeni Destek/Geliştirme Talebi Oluştur");
        modal.setCloseOnOutsideClick(false);

        TextField titleField = new TextField("Talep Başlığı");
        titleField.setWidthFull();
        titleField.setPlaceholder("Lütfen kısa bir başlık yazın...");

        com.vaadin.flow.component.textfield.IntegerField affectedNoField = new com.vaadin.flow.component.textfield.IntegerField("Etkilenen Sayısı / No");
        affectedNoField.setWidthFull();
        affectedNoField.setMin(0);
        affectedNoField.setPlaceholder("Kaç kişi/sistem etkileniyor?");

        TextArea descriptionField = new TextArea("Talep Detayı ve Açıklama");
        descriptionField.setWidthFull();
        descriptionField.setHeight("150px");
        descriptionField.setPlaceholder("Talebinizin detaylarını buraya girin...");

        com.vaadin.flow.component.upload.receivers.MemoryBuffer buffer = new com.vaadin.flow.component.upload.receivers.MemoryBuffer();
        com.vaadin.flow.component.upload.Upload uploadField = new com.vaadin.flow.component.upload.Upload(buffer);
        uploadField.setMaxFileSize(5 * 1024 * 1024);
        uploadField.setUploadButton(new Button("Dosya Ekle", VaadinIcon.UPLOAD.create()));
        uploadField.setDropLabel(new Paragraph("Veya dosyayı buraya sürükleyin"));

        VerticalLayout modalBody = new VerticalLayout(titleField, affectedNoField, descriptionField, uploadField);
        modalBody.setPadding(false);
        modalBody.setSpacing(true);
        modalBody.setWidth("500px");

        Button cancelBtn = new Button("İptal Et", e -> modal.close());
        Button submitBtn = new Button("Oluştur", VaadinIcon.PAPERPLANE.create());
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
            
            User currentUser = (User) VaadinSession.getCurrent().getAttribute("user");
            if (currentUser == null) {
                Notification.show("Oturum zaman aşımına uğradı. Yeniden giriş yapın.")
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            Request newRequest = new Request(currentUser, title, description, affectedNo);
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

            // Create a public log entry on creation to tie the attachment to the request timeline
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

        // Reset temporary attachment buffers
        uploadedFileName = null;
        uploadedFileBytes = null;

        VerticalLayout modalBody = new VerticalLayout();
        modalBody.setPadding(false);
        modalBody.setSpacing(true);
        modalBody.setWidth("500px");

        // PM Feedback Banner (extracted from non-internal logs)
        if (request.getStatus() == RequestStatus.SENT_BACK) {
            String feedbackNote = null;

            if (workflowLogRepository != null) {
                List<com.example.entities.WorkflowLog> logs = workflowLogRepository.findByRequestIdWithUser(request.getId()).stream()
                        .sorted((l1, l2) -> l2.getCreatedAt().compareTo(l1.getCreatedAt()))
                        .toList();

                for (com.example.entities.WorkflowLog log : logs) {
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

        com.vaadin.flow.component.textfield.IntegerField affectedNoField = new com.vaadin.flow.component.textfield.IntegerField("Etkilenen Sayısı / No");
        affectedNoField.setWidthFull();
        affectedNoField.setMin(0);
        affectedNoField.setValue(request.getAffectedNo() != null ? request.getAffectedNo() : 0);

        TextArea descriptionField = new TextArea("Talep Detayı ve Açıklama");
        descriptionField.setWidthFull();
        descriptionField.setHeight("120px");
        descriptionField.setValue(request.getDescription() != null ? request.getDescription() : "");

        modalBody.add(titleField, affectedNoField, descriptionField);

        // 💬 Customer Chat History Area (Formatted like PMInspectView)
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

        User currentUser = (User) VaadinSession.getCurrent().getAttribute("user");

        // Filter out internal communication logs
        List<com.example.entities.WorkflowLog> customerVisibleLogs = workflowLogRepository.findByRequestIdWithUser(request.getId()).stream()
                .filter(log -> !log.isInternal()) 
                .sorted((l1, l2) -> l1.getCreatedAt().compareTo(l2.getCreatedAt())) 
                .toList();

        for (com.example.entities.WorkflowLog log : customerVisibleLogs) {
            HorizontalLayout row = new HorizontalLayout();
            row.setWidthFull();
            
            Div bubble = new Div();
            bubble.setMaxWidth("80%");
            bubble.getStyle()
                    .set("padding", "10px 14px")
                    .set("border-radius", "12px")
                    .set("font-size", "0.9rem")
                    .set("line-height", "1.4");

            boolean isMe = currentUser != null && log.getUser().getId().equals(currentUser.getId());
            String roleStr = log.getUser().getRole().toString();

            if (isMe) {
                row.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
                bubble.getStyle().set("background-color", "#dcfce7").set("color", "#14532d").set("border-bottom-right-radius", "2px");
                
                Span label = new Span("Siz (" + roleStr + ")");
                label.getStyle().set("font-weight", "bold").set("display", "block").set("margin-bottom", "4px");
                bubble.add(label);
            } else {
                row.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
                bubble.getStyle().set("background-color", "#f1f5f9").set("color", "#1e293b").set("border-bottom-left-radius", "2px");
                
                Span label = new Span(log.getUser().getName() + " (" + roleStr + ")");
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

        // Input and Send Controls for the Chat section
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

        // Delete action button
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

            // Log update event in communication flow
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

    private void refreshGridItems() {
        if ("Geri Dönen Talepleriniz".equals(pageTitle.getText())) {
            requestGrid.setItems(requestService.getAllRequests().stream()
                    .filter(r -> r.getStatus() == RequestStatus.SENT_BACK)
                    .toList());
        } else {
            requestGrid.setItems(requestService.getAllRequests());
        }
    }
}