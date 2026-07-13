package com.example.views.developer;

import com.example.base.ui.MainLayout;
import com.example.entities.Prioritization;
import com.example.entities.Request;
import com.example.entities.User;
import com.example.entities.WorkflowLog;
import com.example.services.PrioritizationService;
import com.example.services.WorkflowLogService;
import com.example.services.WorkflowService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.VaadinSession;

import java.io.ByteArrayInputStream;
import java.util.List;

@Route(value = "dev/edit", layout = MainLayout.class)
public class DeveloperEditView extends VerticalLayout implements HasUrlParameter<Long> {

    private final PrioritizationService prioritizationService;
    private final WorkflowService workflowService;
    private final WorkflowLogService workflowLogService;

    private Long requestId;
    private final H2 titleLabel = new H2("İş Düzenleme Sayfası");
    private final Span detailsContainer = new Span();
    private final TextArea devNotesArea = new TextArea("Mesaj");

    private final Div priorityBadge = new Div();
    private final Div scoreContainer = new Div();
    private final Div techScoreContainer = new Div();

    private final VerticalLayout chatHistoryArea = new VerticalLayout();
    private final Upload fileUpload;
    private final MemoryBuffer fileBuffer = new MemoryBuffer();
    private String uploadedFileName = null;
    private byte[] uploadedFileBytes = null;

    public DeveloperEditView(PrioritizationService prioritizationService, 
                             WorkflowService workflowService, 
                             WorkflowLogService workflowLogService) {
        this.prioritizationService = prioritizationService;
        this.workflowService = workflowService;
        this.workflowLogService = workflowLogService;

        setSizeFull();
        setPadding(true);
        getStyle().set("background-color", "#f8fafc");

        HorizontalLayout mainSplit = new HorizontalLayout();
        mainSplit.setWidthFull();
        mainSplit.setSpacing(true);

        VerticalLayout infoColumn = new VerticalLayout();
        infoColumn.setWidth("60%");
        infoColumn.setPadding(false);

        detailsContainer.getStyle().set("display", "block").set("width", "100%");
        
        HorizontalLayout visualBadgesRow = new HorizontalLayout();
        visualBadgesRow.setWidthFull();
        visualBadgesRow.setAlignItems(FlexComponent.Alignment.CENTER);

        priorityBadge.getStyle()
                .set("display", "flex")
                .set("align-items", "center")
                .set("justify-content", "center")
                .set("width", "220px")
                .set("height", "100px")
                .set("border-radius", "8px")
                .set("font-weight", "900")
                .set("font-size", "1.2rem");

        scoreContainer.getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("align-items", "center")
                .set("justify-content", "center")
                .set("width", "220px")
                .set("height", "100px")
                .set("border-radius", "8px")
                .set("background-color", "#f1f5f9")
                .set("border", "1px solid #e2e8f0")
                .set("margin-left", "20px");

        techScoreContainer.getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("align-items", "center")
                .set("justify-content", "center")
                .set("width", "220px")
                .set("height", "100px")
                .set("border-radius", "8px")
                .set("background-color", "#f1f5f9")
                .set("border", "1px solid #e2e8f0")
                .set("margin-left", "20px");

        visualBadgesRow.add(priorityBadge, scoreContainer, techScoreContainer);
        infoColumn.add(titleLabel, detailsContainer, visualBadgesRow);

        VerticalLayout actionsColumn = new VerticalLayout();
        actionsColumn.setWidth("40%");
        actionsColumn.getStyle()
                .set("background", "#eef2f7")
                .set("border-radius", "12px")
                .set("padding", "20px");

        chatHistoryArea.setWidthFull();
        chatHistoryArea.setHeight("280px");
        chatHistoryArea.getStyle()
                .set("overflow-y", "auto")
                .set("background", "#ffffff")
                .set("border", "1px solid #cbd5e1")
                .set("border-radius", "8px")
                .set("padding", "12px");

        devNotesArea.setPlaceholder("Ekibe bir mesaj yazın veya dosya ekleyin...");
        devNotesArea.setWidthFull();
        devNotesArea.setHeight("70px");

        fileUpload = new Upload(fileBuffer);
        fileUpload.setMaxFiles(1);
        fileUpload.setAcceptedFileTypes(".pdf", ".png", ".jpg", ".docx", ".xlsx");
        fileUpload.addSucceededListener(event -> {
            try {
                this.uploadedFileName = event.getFileName();
                this.uploadedFileBytes = fileBuffer.getInputStream().readAllBytes();
                Notification.show("Dosya eklendi: " + uploadedFileName).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (Exception ex) {
                Notification.show("Dosya okunamadı!").addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        Button sendChatBtn = new Button("Mesaj Gönder", e -> submitGroupChatMessage());
        sendChatBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        sendChatBtn.setWidthFull();

        VerticalLayout btnColumn = new VerticalLayout();
        btnColumn.setWidthFull();
        btnColumn.setSpacing(true);
        btnColumn.setPadding(false);

        Button completeBtn = new Button("İŞİ TAMAMLA", e -> completeTaskJob());
        completeBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
        completeBtn.setWidthFull();

        Button sendBackBtn = new Button("GERİ GÖNDER", e -> sendWorkflowBackToSoftwareManager());
        sendBackBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);
        sendBackBtn.setWidthFull();

        btnColumn.add(completeBtn, sendBackBtn);
        
        // Assemble Right Column
        actionsColumn.add(
            new Span("Talep İletişim Grubu"), 
            chatHistoryArea, 
            devNotesArea, 
            fileUpload, 
            sendChatBtn, 
            new Hr(), 
            btnColumn
        );

        mainSplit.add(infoColumn, actionsColumn);
        add(mainSplit);
    }

    @Override
    public void setParameter(BeforeEvent event, Long parameter) {
        this.requestId = parameter;
        try {
            Prioritization p = prioritizationService.getPrioritizationById(requestId);
            Request r = p.getRequest();

            detailsContainer.getElement().setProperty("innerHTML",
                "<div style='margin-bottom: 25px; font-size: 1.1rem; line-height: 1.6;'>" +
                "<p><b>Talep Başlığı:</b> " + r.getTitle() + "</p>" +
                "<p><b>Talep Açıklaması:</b> " + (r.getDescription() != null ? r.getDescription() : "-") + "</p><hr style='border:0; border-top:1px solid #e2e8f0; margin:20px 0;'/>" +
                "<p style='color:#475569;'><b>İş Etkisi (Impact):</b> " + p.getImpact() + "</p>" +
                "<p style='color:#475569;'><b>Aciliyet (Urgency):</b> " + p.getUrgency() + "</p>" +
                "<p style='color:#475569;'><b>Sorumlu Departman:</b> " + (p.getDepartment() != null ? p.getDepartment().getName() : "-") + "</p>" +
                "</div>"
            );

            scoreContainer.getElement().setProperty("innerHTML",
                "<span style='font-size:0.75rem; color:#64748b; font-weight:800; letter-spacing: 0.05em; margin-bottom: 2px;'>HESAPLANAN SKOR</span>" +
                "<span style='font-size:2.8rem; font-weight:900; color:#1e3a8a; line-height: 1.1;'>" + p.getPriorityScore() + "</span>"
            );

            if (p.getPriorityScore() >= 40) {
                priorityBadge.setText("YÜKSEK ÖNCELİKLİ");
                priorityBadge.getStyle().set("background-color", "#fee2e2").set("color", "#991b1b");
            } else if (p.getPriorityScore() >= 20) {
                priorityBadge.setText("ORTA ÖNCELİKLİ");
                priorityBadge.getStyle().set("background-color", "#fef3c7").set("color", "#92400e");
            } else {
                priorityBadge.setText("DÜŞÜK ÖNCELİKLİ");
                priorityBadge.getStyle().set("background-color", "#dcfce7").set("color", "#166534");
            }

            techScoreContainer.getElement().setProperty("innerHTML",
                "<span style='font-size:0.75rem; color:#d97706; font-weight:800; letter-spacing: 0.05em; margin-bottom: 2px;'>TEKNİK SKOR</span>" +
                "<span style='font-size:2.8rem; font-weight:900; color:#1e3a8a; line-height: 1.1;'>" + p.getSmTechnicalScore() + "</span>"
            );
            refreshChatHistoryWindow();

        } catch (Exception ex) {
            Notification.show("Bu talep henüz atanmadı veya önceliklendirilmedi!").addThemeVariants(NotificationVariant.LUMO_ERROR);
            UI.getCurrent().navigate("dev/dashboard");
        }
    }

    private void refreshChatHistoryWindow() {
        chatHistoryArea.removeAll();
        User currentUser = (User) VaadinSession.getCurrent().getAttribute("user");
        
        List<WorkflowLog> chatLogs = workflowLogService.getChatHistoryForRequest(requestId);
        
        for (WorkflowLog log : chatLogs) {
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
                bubble.add(new Span("Siz (" + roleStr + "):"));
            } else {
                row.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
                bubble.getStyle().set("background-color", "#f1f5f9").set("color", "#1e293b").set("border-bottom-left-radius", "2px");
                bubble.add(new Span("" + log.getUser().getName() + " (" + roleStr + "):"));
            }

            if (log.getLogText() != null && !log.getLogText().isEmpty()) {
                Div textDiv = new Div(new Span(log.getLogText()));
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
    }

    private void submitGroupChatMessage() {
        User currentUser = (User) VaadinSession.getCurrent().getAttribute("user");
        if (currentUser == null) return;

        if (devNotesArea.isEmpty() && uploadedFileBytes == null) {
            Notification.show("Boş mesaj gönderilemez!").addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        try {
            workflowLogService.saveChatComment(
                requestId, 
                devNotesArea.getValue(), 
                currentUser, 
                uploadedFileName, 
                uploadedFileBytes
            );

            devNotesArea.clear();
            fileUpload.clearFileList();
            this.uploadedFileBytes = null;
            this.uploadedFileName = null;
            
            refreshChatHistoryWindow();
            Notification.show("Mesajınız iletildi.").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } catch (Exception ex) {
            Notification.show("Hata: " + ex.getMessage()).addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void completeTaskJob() {
        String msg = devNotesArea.getValue().trim();
        User currentUser = (User) VaadinSession.getCurrent().getAttribute("user");
        
        if (currentUser != null && !msg.isEmpty()) {
            workflowLogService.saveChatComment(requestId, "[İŞ TAMAMLANDI]: " + msg, currentUser, null, null);
        }

        prioritizationService.completeDeveloperJob(requestId);
        Notification.show("İş başarıyla tamamlandı!").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        UI.getCurrent().navigate("dev/dashboard");
    }

    private void sendWorkflowBackToSoftwareManager() {
        String msg = devNotesArea.getValue().trim();
        User currentUser = (User) VaadinSession.getCurrent().getAttribute("user");

        if (currentUser != null) {
            workflowLogService.saveChatComment(requestId, "[İŞ GERİ GÖNDERİLDİ]: " + msg, currentUser, null, null);
        }

        workflowService.rejectBackToSM(requestId, msg);
        Notification.show("İş tekrar değerlendirilmesi için takım liderine geri gönderildi.").addThemeVariants(NotificationVariant.LUMO_WARNING);
        UI.getCurrent().navigate("dev/dashboard");
    }
}