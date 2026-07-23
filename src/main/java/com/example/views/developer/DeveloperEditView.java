package com.example.views.developer;

import com.example.base.ui.MainLayout;
import com.example.entities.Prioritization;
import com.example.entities.Request;
import com.example.entities.User;
import com.example.entities.Workflow;
import com.example.entities.WorkflowLog;
import com.example.enums.WorkflowStatus; 
import com.example.repositories.WorkflowLogRepository;
import com.example.services.PrioritizationService;
import com.example.services.RequestService;
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
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.VaadinSession;

import java.io.ByteArrayInputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Route(value = "dev/edit", layout = MainLayout.class)
public class DeveloperEditView extends VerticalLayout implements HasUrlParameter<Long> {

    private final PrioritizationService prioritizationService;
    private final WorkflowService workflowService;
    private final WorkflowLogService workflowLogService;
    private final RequestService requestService;
    private final WorkflowLogRepository workflowLogRepository;

    private Long requestId;
    private Request targetRequest;
    private boolean showingInternalTab = false;

    private final H2 titleLabel = new H2("İş Düzenleme Sayfası");
    private final VerticalLayout detailsCard = new VerticalLayout();
    private final TextArea devNotesArea = new TextArea("Mesaj");

    private final Div priorityBadge = new Div();
    private final Div scoreContainer = new Div();
    private final Div techScoreContainer = new Div();

    private final Tabs chatTabs = new Tabs();
    private final Tab customerTab = new Tab("Müşteri İletişimi");
    private final Tab internalTab = new Tab("İç Değerlendirme");

    private final VerticalLayout chatHistoryArea = new VerticalLayout();
    private final Upload fileUpload;
    private final MemoryBuffer fileBuffer = new MemoryBuffer();
    private String uploadedFileName = null;
    private byte[] uploadedFileBytes = null;
    
    private final Button sendChatBtn;
    
    private final VerticalLayout btnColumn = new VerticalLayout();

    public DeveloperEditView(PrioritizationService prioritizationService, 
                             WorkflowService workflowService, 
                             WorkflowLogService workflowLogService,
                             RequestService requestService,
                             WorkflowLogRepository workflowLogRepository) {
        this.prioritizationService = prioritizationService;
        this.workflowService = workflowService;
        this.workflowLogService = workflowLogService;
        this.requestService = requestService;
        this.workflowLogRepository = workflowLogRepository;

        setSizeFull();
        setPadding(true);
        getStyle().set("background-color", "#f8fafc");

        HorizontalLayout mainSplit = new HorizontalLayout();
        mainSplit.setWidthFull();
        mainSplit.setSpacing(true);

        VerticalLayout infoColumn = new VerticalLayout();
        infoColumn.setWidth("60%");
        infoColumn.setPadding(false);
        
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
        infoColumn.add(titleLabel, detailsCard, visualBadgesRow);

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

        devNotesArea.setWidthFull();
        devNotesArea.setHeight("70px");

        fileUpload = new Upload(fileBuffer);
        fileUpload.setMaxFiles(1);
        fileUpload.setAcceptedFileTypes(".pdf", ".png", ".jpg", ".docx", ".xlsx");
        fileUpload.getStyle().set("margin-bottom", "10px");
        fileUpload.addSucceededListener(event -> {
            try {
                this.uploadedFileName = event.getFileName();
                this.uploadedFileBytes = fileBuffer.getInputStream().readAllBytes();
                Notification.show("Dosya eklendi: " + uploadedFileName).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (Exception ex) {
                Notification.show("Dosya okunamadı!").addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        sendChatBtn = new Button("Mesaj Gönder", e -> submitGroupChatMessage());
        sendChatBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        sendChatBtn.setWidthFull();
        sendChatBtn.getStyle().set("margin-bottom", "10px");

        chatTabs.add(customerTab, internalTab);
        chatTabs.setWidthFull();
        chatTabs.addSelectedChangeListener(event -> {
            showingInternalTab = event.getSelectedTab().equals(internalTab);
            
            if (showingInternalTab) {
                devNotesArea.setReadOnly(false);
                devNotesArea.setPlaceholder("Ekiple paylaşmak için bir şeyler yazın...");
                fileUpload.setVisible(true);
                sendChatBtn.setVisible(true);
            } else {
                devNotesArea.setReadOnly(true);
                devNotesArea.setValue(""); 
                devNotesArea.setPlaceholder("Müşteri kanalı ekipler için salt okunurdur.");
                fileUpload.setVisible(false);
                sendChatBtn.setVisible(false);
            }
            refreshChatHistoryWindow();
        });

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
        
        actionsColumn.add(
            chatTabs, 
            chatHistoryArea, 
            devNotesArea, 
            fileUpload, 
            sendChatBtn, 
            new Hr(), 
            btnColumn
        );

        mainSplit.add(infoColumn, actionsColumn);
        add(mainSplit);

        Button backBtn = new Button("Geri Dön", VaadinIcon.ARROW_LEFT.create(), e -> UI.getCurrent().navigate("dev/dashboard"));
        add(backBtn);

        devNotesArea.setReadOnly(true);
        devNotesArea.setPlaceholder("Müşteri kanalı ekipler için salt okunurdur.");
        fileUpload.setVisible(false);
        sendChatBtn.setVisible(false);
    }

    @Override
    public void setParameter(BeforeEvent event, Long parameter) {
        this.requestId = parameter;
        
        Request r = requestService.getRequestById(requestId);
        if (r == null) {
            Notification.show("Talep kaydi bulunamadi!").addThemeVariants(NotificationVariant.LUMO_ERROR);
            UI.getCurrent().navigate("dev/dashboard");
            return;
        }
        this.targetRequest = r;

        detailsCard.setWidthFull();
        detailsCard.setPadding(true);
        detailsCard.setSpacing(true);
        detailsCard.getStyle()
                .set("background-color", "#ffffff")
                .set("border", "1px solid #e2e8f0")
                .set("border-radius", "12px")
                .set("box-shadow", "0 4px 6px -1px rgba(0,0,0,0.05), 0 2px 4px -2px rgba(0,0,0,0.05)")
                .set("margin-bottom", "20px")
                .set("align-items", "stretch")
                .set("max-width", "100%")
                .set("box-sizing", "border-box");

        detailsCard.removeAll();

        H2 titleHeader = new H2(r.getTitle());
        titleHeader.getStyle()
                .set("margin", "0")
                .set("font-size", "1.4rem")
                .set("font-weight", "800")
                .set("color", "#0f172a")
                .set("word-break", "break-word")
                .set("overflow-wrap", "anywhere");

        VerticalLayout descContainer = new VerticalLayout();
        descContainer.setPadding(false);
        descContainer.setSpacing(false);
        descContainer.getStyle().set("margin-top", "4px");

        Span descLabel = new Span("MUSTERI TALEP ACIKLAMASI");
        descLabel.getStyle().set("font-size", "0.75rem").set("font-weight", "800").set("color", "#64748b").set("margin-bottom", "4px");

        Span descBody = new Span(r.getDescription() != null ? r.getDescription() : "Aciklama belirtilmemis.");
        descBody.getStyle()
                .set("font-size", "0.95rem")
                .set("color", "#334155")
                .set("white-space", "pre-wrap")
                .set("word-break", "break-word")
                .set("overflow-wrap", "anywhere");
        descContainer.add(descLabel, descBody);

        HorizontalLayout badgesRow = new HorizontalLayout();
        badgesRow.setSpacing(true);
        badgesRow.getStyle().set("flex-wrap", "wrap").set("gap", "6px").set("margin-top", "8px");

        if (r.getAffectedNo() != null && r.getAffectedNo() > 0) {
            Span affectedBadge = new Span(r.getAffectedNo() + " Etkilenen");
            affectedBadge.getStyle()
                    .set("padding", "6px 12px")
                    .set("border-radius", "9999px")
                    .set("font-size", "0.8rem")
                    .set("font-weight", "700")
                    .set("background-color", "#e0f2fe")
                    .set("color", "#0369a1");
            badgesRow.add(affectedBadge);
        }

        if (r.getDeadline() != null) {
            Span deadlineBadge = new Span("Deadline: " + r.getDeadline().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
            deadlineBadge.getStyle()
                    .set("padding", "6px 12px")
                    .set("border-radius", "9999px")
                    .set("font-size", "0.8rem")
                    .set("font-weight", "700")
                    .set("background-color", "#fef3c7")
                    .set("color", "#b45309");
            badgesRow.add(deadlineBadge);
        }

        if (Boolean.TRUE.equals(r.getIsSecurityRisk())) {
            Span securityBadge = new Span("KVKK / Güvenlik Riski");
            securityBadge.getStyle()
                    .set("padding", "6px 12px")
                    .set("border-radius", "9999px")
                    .set("font-size", "0.8rem")
                    .set("font-weight", "700")
                    .set("background-color", "#fee2e2")
                    .set("color", "#991b1b");
            badgesRow.add(securityBadge);
        }

        if (r.getFinancialImpact() != null && !r.getFinancialImpact().isBlank() && !"Etkisi Yok".equalsIgnoreCase(r.getFinancialImpact())) {
            Span financialBadge = new Span("Mali Etki: " + r.getFinancialImpact());
            financialBadge.getStyle()
                    .set("padding", "6px 12px")
                    .set("border-radius", "9999px")
                    .set("font-size", "0.8rem")
                    .set("font-weight", "700")
                    .set("background-color", "#f0fdf4")
                    .set("color", "#166534");
            badgesRow.add(financialBadge);
        }

        Span companyBadge = new Span();
        companyBadge.getStyle().set("padding", "6px 12px").set("border-radius", "9999px").set("font-size", "0.8rem").set("font-weight", "700");
        if (r.getCustomer() != null && r.getCustomer().getCompany() != null) {
            String companyName = r.getCustomer().getCompany().getName();
            double companyScore = r.getCustomer().getCompany().getCompanyScore();
            companyBadge.setText(companyName + " (Score: " + companyScore + ")");
            companyBadge.getStyle().set("background-color", "#f0fdf4").set("color", "#166534");
        } else {
            companyBadge.setText("Bireysel");
            companyBadge.getStyle().set("background-color", "#f1f5f9").set("color", "#475569");
        }
        badgesRow.add(companyBadge);

        detailsCard.add(titleHeader, descContainer, badgesRow);

        try {
            Prioritization p = prioritizationService.getPrioritizationById(requestId);

            scoreContainer.getElement().setProperty("innerHTML",
                "<span style='font-size:0.75rem; color:#64748b; font-weight:800; letter-spacing: 0.05em; margin-bottom: 2px;'>HESAPLANAN SKOR</span>" +
                "<span style='font-size:2.8rem; font-weight:900; color:#1e3a8a; line-height: 1.1;'>" + p.getPriorityScore() + "</span>"
            );

            if (p.getPriorityScore() > 100) {
                priorityBadge.setText("YÖNETİCİ MÜDAHALESİ");
                priorityBadge.getStyle().set("background", "#f19595").set("color", "#780303");
            } else if (p.getPriorityScore() >= 60) {
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

            Workflow wf = workflowService.getWorkflowByRequestId(requestId);
            if (wf != null && wf.getStatus() == WorkflowStatus.COMPLETED) {
                btnColumn.setVisible(false);
                fileUpload.setVisible(false);
                sendChatBtn.setVisible(false);
            }

        } catch (Exception ex) {
            scoreContainer.getElement().setProperty("innerHTML",
                "<span style='font-size:0.75rem; color:#64748b; font-weight:800; margin-bottom: 2px;'>HESAPLANAN SKOR</span>" +
                "<span style='font-size:2rem; font-weight:900; color:#64748b;'>-</span>"
            );
            techScoreContainer.getElement().setProperty("innerHTML",
                "<span style='font-size:0.75rem; color:#64748b; font-weight:800; margin-bottom: 2px;'>TEKNİK SKOR</span>" +
                "<span style='font-size:2rem; font-weight:900; color:#64748b;'>-</span>"
            );
            priorityBadge.setText("ATANMAMIS");
            priorityBadge.getStyle().set("background-color", "#f1f5f9").set("color", "#475569");
            
            Notification.show("Bu talep henüz atanmadı veya önceliklendirilmedi!").addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void refreshChatHistoryWindow() {
        chatHistoryArea.removeAll();
        User currentUser = (User) VaadinSession.getCurrent().getAttribute("user");
        
        if (showingInternalTab) {
            chatHistoryArea.getStyle().set("background", "#fffbeb"); 
        } else {
            chatHistoryArea.getStyle().set("background", "#ffffff"); 
        }

        List<WorkflowLog> chatLogs = workflowLogService.getChatHistoryForRequest(requestId).stream()
                .filter(log -> log.isInternal() == showingInternalTab)
                .sorted((l1, l2) -> l1.getCreatedAt().compareTo(l2.getCreatedAt()))
                .toList();
        
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
                Div textDiv = new Div(new Span(log.getLogText()));
                textDiv.getStyle().set("margin-top", "4px").set("white-space", "pre-wrap");
                bubble.add(textDiv);
            }

            if (log.getFileName() != null && log.getFileBytes() != null) {
                String name = log.getFileName();
                byte[] bytes = log.getFileBytes();
                
                StreamResource resource = new StreamResource(name, () -> new ByteArrayInputStream(bytes));
                Anchor downloadAnchor = new Anchor(resource, name);
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
            WorkflowLog log = new WorkflowLog();
            log.setRequest(targetRequest);
            log.setUser(currentUser);
            log.setLogText(devNotesArea.getValue() != null ? devNotesArea.getValue().trim() : null);
            log.setFromStatus(targetRequest.getStatus().name());
            log.setToStatus(targetRequest.getStatus().name());
            log.setInternal(showingInternalTab);

            if (uploadedFileBytes != null && uploadedFileName != null) {
                log.setFileName(uploadedFileName);
                log.setFileBytes(uploadedFileBytes);
            }

            workflowLogRepository.save(log);

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

        prioritizationService.completeDeveloperJob(requestId, currentUser);
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