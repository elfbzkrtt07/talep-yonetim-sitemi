package com.example.views.pm;

import com.example.base.ui.MainLayout;
import com.example.entities.Department; 
import com.example.entities.Prioritization;
import com.example.entities.Request;
import com.example.entities.User;
import com.example.entities.WorkflowLog;
import com.example.enums.RequestStatus;
import com.example.enums.TaskType;
import com.example.repositories.DepartmentRepository;
import com.example.services.PrioritizationService;
import com.example.services.RequestService;
import com.example.services.WorkflowLogService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
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
import java.util.Optional;

@Route(value = "pm/inspect", layout = MainLayout.class)
public class PMInspectView extends VerticalLayout implements HasUrlParameter<Long> {

    private final RequestService requestService;
    private final PrioritizationService prioritizationService;
    private final DepartmentRepository departmentRepository;
    private final WorkflowLogService workflowLogService; // 🌟 Added for Group Chat Pipeline
    
    private Request targetRequest;
    private Long requestId;
    private boolean isUpdatingUi = false;

    private final Span detailsSpan = new Span();
    private final Select<Integer> impactSelect = new Select<>();
    private final Select<Integer> urgencySelect = new Select<>();
    private final Select<TaskType> typeSelect = new Select<>();
    private final Select<Department> departmentField = new Select<>();
    
    private final H1 scoreValue = new H1("0");
    private final Span priorityBadge = new Span("DÜŞÜK ÖNCELİKLİ");

    // 🌟 Group Chat Area Elements
    private final VerticalLayout chatHistoryArea = new VerticalLayout();
    private final TextArea chatInputArea = new TextArea("Mesaj");
    private final Upload fileUpload;
    private final MemoryBuffer fileBuffer = new MemoryBuffer();
    private String uploadedFileName = null;
    private byte[] uploadedFileBytes = null;

    public PMInspectView(RequestService requestService, 
                         PrioritizationService prioritizationService,
                         DepartmentRepository departmentRepository,
                         WorkflowLogService workflowLogService) {
        this.requestService = requestService;
        this.prioritizationService = prioritizationService;
        this.departmentRepository = departmentRepository;
        this.workflowLogService = workflowLogService;

        setSizeFull();
        setPadding(true);
        getStyle().set("background-color", "#f8fafc");

        H2 viewTitle = new H2("Talep İnceleme Sayfası");
        viewTitle.getStyle().set("margin-bottom", "20px");
        add(viewTitle);

        HorizontalLayout workspaceLayout = new HorizontalLayout();
        workspaceLayout.setWidthFull();
        workspaceLayout.setSpacing(true);

        // --- LEFT PANEL: Request Details & Scoring Fields ---
        VerticalLayout leftPanel = new VerticalLayout();
        leftPanel.setWidth("60%");
        leftPanel.setPadding(false);

        detailsSpan.getStyle().set("display", "block").set("margin-bottom", "20px");

        impactSelect.setLabel("İş Etkisi (Impact)");
        impactSelect.setItems(1, 2, 3, 4, 5);
        impactSelect.setValue(1);
        impactSelect.setWidthFull();

        urgencySelect.setLabel("Aciliyet (Urgency)");
        urgencySelect.setItems(1, 2, 3, 4, 5);
        urgencySelect.setValue(1);
        urgencySelect.setWidthFull();

        typeSelect.setLabel("İş Tipi (Task Type)");
        typeSelect.setItems(TaskType.values());
        if (TaskType.values().length > 0) typeSelect.setValue(TaskType.values()[0]);
        typeSelect.setWidthFull();

        departmentField.setLabel("İlgili Departman");
        departmentField.setPlaceholder("Ekiplerden birini seçiniz (A, B, C)...");
        departmentField.setWidthFull();
        departmentField.setItems(departmentRepository.findAll());
        departmentField.setItemLabelGenerator(Department::getName);
        
        leftPanel.add(detailsSpan, impactSelect, urgencySelect, typeSelect, departmentField);

        // --- RIGHT PANEL: Score & Chat Group Stream ---
        VerticalLayout rightPanel = new VerticalLayout();
        rightPanel.setWidth("40%");
        rightPanel.getStyle().set("background", "#eef2f7")
                            .set("border-radius", "12px")
                            .set("padding", "20px");

        HorizontalLayout scoreBox = new HorizontalLayout();
        scoreBox.setWidthFull();
        scoreBox.getStyle().set("background", "#ffffff").set("border-radius", "8px").set("padding", "15px").set("margin-bottom", "10px");
        scoreBox.setAlignItems(FlexComponent.Alignment.CENTER);
        scoreBox.setJustifyContentMode(FlexComponent.JustifyContentMode.AROUND);

        // Inside PMInspectView constructor:
        VerticalLayout scoreValLayout = new VerticalLayout();
        scoreValLayout.setPadding(false);
        scoreValLayout.setSpacing(false);
        scoreValLayout.setAlignItems(FlexComponent.Alignment.CENTER);

        Span scoreHeader = new Span("HESAPLANAN SKOR");
        scoreHeader.getStyle().set("font-size", "0.75rem").set("color", "#64748b").set("font-weight", "bold");

        Button downBtn = new Button(VaadinIcon.MINUS.create(), e -> {
            int currentScore = Integer.parseInt(scoreValue.getText());
            int newScore = Math.max(0, currentScore - 1);
            updateScoreBadgeVisuals(newScore);
        });
        downBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);

        Button upBtn = new Button(VaadinIcon.PLUS.create(), e -> {
            int currentScore = Integer.parseInt(scoreValue.getText());
            int newScore = Math.min(100, currentScore + 1);
            updateScoreBadgeVisuals(newScore);
        });
        upBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);

        scoreValue.getStyle().set("margin", "0 10px").set("font-size", "3rem").set("font-weight", "bold");

        HorizontalLayout interactiveScoreRow = new HorizontalLayout(downBtn, scoreValue, upBtn);
        interactiveScoreRow.setAlignItems(FlexComponent.Alignment.CENTER);

        scoreValLayout.add(scoreHeader, interactiveScoreRow);

        priorityBadge.getStyle().set("padding", "8px 16px").set("border-radius", "20px").set("font-weight", "bold").set("font-size", "0.85rem");
        scoreBox.add(scoreValLayout, priorityBadge);

        chatHistoryArea.setWidthFull();
        chatHistoryArea.setHeight("240px");
        chatHistoryArea.getStyle()
                .set("overflow-y", "auto")
                .set("background", "#ffffff")
                .set("border", "1px solid #cbd5e1")
                .set("border-radius", "8px")
                .set("padding", "12px");

        chatInputArea.setPlaceholder("Mesajınızı buraya yazın...");
        chatInputArea.setWidthFull();
        chatInputArea.setHeight("65px");
        chatInputArea.getStyle().set("margin-bottom", "5px");

        fileUpload = new Upload(fileBuffer);
        fileUpload.setMaxFiles(1);
        fileUpload.setAcceptedFileTypes(".pdf", ".png", ".jpg", ".docx", ".xlsx");
        fileUpload.getStyle().set("margin-bottom", "10px");
        fileUpload.addSucceededListener(event -> {
            try {
                this.uploadedFileName = event.getFileName();
                this.uploadedFileBytes = fileBuffer.getInputStream().readAllBytes();
                Notification.show("Dosya hazır: " + uploadedFileName).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (Exception ex) {
                Notification.show("Dosya okunamadı!").addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        Button sendChatBtn = new Button("Mesaj Gönder", e -> submitPmChatMessage());
        sendChatBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        sendChatBtn.setWidthFull();
        sendChatBtn.getStyle().set("margin-bottom", "10px");

        Button saveBtn = new Button("KAYDET / HAVUZA ONAYLA", VaadinIcon.CHECK.create());
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
        saveBtn.setWidthFull();
        saveBtn.addClickListener(e -> savePrioritizationData());

        Span chatTitle = new Span("Talep İletişim Grubu");
        chatTitle.getStyle().set("font-weight", "bold").set("font-size", "1.1rem").set("margin-bottom", "5px");

        rightPanel.add(scoreBox, chatTitle, chatHistoryArea, chatInputArea, fileUpload, sendChatBtn, new Hr(), saveBtn);
        workspaceLayout.add(leftPanel, rightPanel);
        add(workspaceLayout);

        // --- FOOTER BUTTON ROW ---
        HorizontalLayout actionRow = new HorizontalLayout();
        actionRow.setWidthFull();
        actionRow.getStyle().set("margin-top", "20px");

        Button backBtn = new Button("Geri Dön", VaadinIcon.ARROW_LEFT.create(), e -> UI.getCurrent().navigate("pm/requests"));
        
        Button rejectBtn = new Button("Müşteriye Geri Gönder", VaadinIcon.ARROW_BACKWARD.create(), e -> openRejectReasonDialog());
        rejectBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
        rejectBtn.getStyle().set("margin-left", "auto");

        actionRow.add(backBtn, rejectBtn);
        add(actionRow);

        impactSelect.addValueChangeListener(e -> {
            if (!isUpdatingUi) refreshCalculatedScoreText();
        });
        
        urgencySelect.addValueChangeListener(e -> {
            if (!isUpdatingUi) refreshCalculatedScoreText();
        });

        typeSelect.addValueChangeListener(e -> {
            if (!isUpdatingUi) refreshCalculatedScoreText();
        });
    }

    @Override
    public void setParameter(BeforeEvent event, Long parameter) {
        this.requestId = parameter;
        Optional<Request> requestOpt = requestService.getAllRequests().stream()
                .filter(r -> r.getId().equals(parameter))
                .findFirst();

        if (requestOpt.isPresent()) {
            this.targetRequest = requestOpt.get();
            updateUiContent();
        } else {
            Notification.show("Talep kaydı bulunamadı!").addThemeVariants(NotificationVariant.LUMO_ERROR);
            UI.getCurrent().navigate("pm/requests");
        }
    }

    private void updateUiContent() {
        isUpdatingUi = true;

        detailsSpan.getElement().setProperty("innerHTML", 
            "<h3><b>Talep Başlığı:</b> " + targetRequest.getTitle() + "</h3>" +
            "<p style='color:#475569;'><b>Açıklama:</b> " + (targetRequest.getDescription() != null ? targetRequest.getDescription() : "-") + "</p>" +
            "<span style='font-size:0.9rem; color:#64748b;'><b>Etkilenen Sayısı:</b> " + (targetRequest.getAffectedNo() != null ? targetRequest.getAffectedNo() : "Belirtilmemiş") + "</span>"
        );

        try {
            Prioritization existingPrior = prioritizationService.getPrioritizationById(targetRequest.getId());
            
            impactSelect.setValue(existingPrior.getImpact());
            urgencySelect.setValue(existingPrior.getUrgency());
            typeSelect.setValue(existingPrior.getTaskType());
            departmentField.setValue(existingPrior.getDepartment());
            
            updateScoreBadgeVisuals(existingPrior.getPriorityScore());
            
        } catch (Exception ex) {
            impactSelect.setValue(1);
            urgencySelect.setValue(1);
            if (TaskType.values().length > 0) typeSelect.setValue(TaskType.values()[0]);
            departmentField.setValue(null);
            
            updateScoreBadgeVisuals(4);
        } finally {
            isUpdatingUi = false;
            refreshChatHistoryWindow();
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

    private void submitPmChatMessage() {
        User currentUser = (User) VaadinSession.getCurrent().getAttribute("user");
        if (currentUser == null) return;

        if (chatInputArea.isEmpty() && uploadedFileBytes == null) {
            Notification.show("Boş mesaj gönderilemez!").addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        try {
            workflowLogService.saveChatComment(
                requestId, 
                chatInputArea.getValue(), 
                currentUser, 
                uploadedFileName, 
                uploadedFileBytes
            );

            chatInputArea.clear();
            fileUpload.clearFileList();
            this.uploadedFileBytes = null;
            this.uploadedFileName = null;
            
            refreshChatHistoryWindow();
            Notification.show("Mesajınız iletildi.").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } catch (Exception ex) {
            Notification.show("Hata: " + ex.getMessage()).addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void refreshCalculatedScoreText() {
        if (targetRequest == null) return;
        
        int score = prioritizationService.calculatePreviewScore(
            targetRequest,
            impactSelect.getValue(),
            urgencySelect.getValue(),
            typeSelect.getValue()
        );
        
        updateScoreBadgeVisuals(score);
    }

    private void updateScoreBadgeVisuals(int score) {
        scoreValue.setText(String.valueOf(score));

        if (score >= 60) {
            priorityBadge.setText("YÜKSEK ÖNCELİKLİ");
            priorityBadge.getStyle().set("background", "#fee2e2").set("color", "#ef4444");
        } else if (score >= 24) {
            priorityBadge.setText("ORTA ÖNCELİKLİ");
            priorityBadge.getStyle().set("background", "#fef3c7").set("color", "#d97706");
        } else {
            priorityBadge.setText("DÜŞÜK ÖNCELİKLİ");
            priorityBadge.getStyle().set("background", "#dcfce7").set("color", "#22c55e");
        }
    }

    private void savePrioritizationData() {
        Department chosenDept = departmentField.getValue();
        if (chosenDept == null) {
            Notification.show("Lütfen ilgili departmanı belirtin!").addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        User currentUser = (User) VaadinSession.getCurrent().getAttribute("user");
        targetRequest.setStatus(RequestStatus.APPROVED);
        int calculatedScoreInt = Integer.parseInt(scoreValue.getText());
        
        Prioritization prioritization = new Prioritization(
                targetRequest,
                urgencySelect.getValue(),
                impactSelect.getValue(),
                typeSelect.getValue(),
                chosenDept,
                calculatedScoreInt
        );
        prioritization.setId(targetRequest.getId());

        // 🌟 Log the systematic task pool approval entry to the chat chain history thread
        if (currentUser != null) {
            workflowLogService.saveChatComment(requestId, "[TALEP ONAYLANDI]: Talep incelendi ve öncelik havuzuna aktarıldı. Skor: " + calculatedScoreInt, currentUser, null, null);
        }

        requestService.approveAndPrioritizeRequest(targetRequest, prioritization);
        Notification.show("Talep başarıyla onaylandı ve öncelik havuzuna işlendi.").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        UI.getCurrent().navigate("pm/requests");
    }

    private void openRejectReasonDialog() {
        Dialog rejectDialog = new Dialog();
        rejectDialog.setHeaderTitle("Müşteriye İade Gerekçesi");

        TextArea feedbackArea = new TextArea("Revizyon / Eksik Bilgi Notu");
        feedbackArea.setPlaceholder("Lütfen müşterinin neyi düzeltmesi gerektiğini detaylıca yazın...");
        feedbackArea.setWidth("400px");
        feedbackArea.setHeight("120px");
        rejectDialog.add(feedbackArea);

        Button submitReject = new Button("İade Et", e -> {
            String note = feedbackArea.getValue().trim();
            if (note.isEmpty()) {
                Notification.show("Gerekçe boş bırakılamaz!").addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            User currentUser = (User) VaadinSession.getCurrent().getAttribute("user");
            targetRequest.setStatus(RequestStatus.SENT_BACK);

            if (currentUser != null) {
                workflowLogService.saveChatComment(requestId, "[MÜŞTERİYE İADE EDİLDİ]: Gerekçe: " + note, currentUser, null, null);
            }

            requestService.updateRequest(targetRequest);
            Notification.show("Talep müşteriye geri gönderildi.").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            
            rejectDialog.close();
            UI.getCurrent().navigate("pm/requests");
        });
        submitReject.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);

        rejectDialog.getFooter().add(new Button("İptal", c -> rejectDialog.close()), submitReject);
        rejectDialog.open();
    }
}