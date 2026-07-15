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
import com.example.repositories.WorkflowLogRepository;
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
import java.util.List;
import java.util.Optional;

@Route(value = "pm/inspect", layout = MainLayout.class)
public class PMInspectView extends VerticalLayout implements HasUrlParameter<Long> {

    private final RequestService requestService;
    private final PrioritizationService prioritizationService;
    private final DepartmentRepository departmentRepository;
    private final WorkflowLogService workflowLogService;
    private final WorkflowLogRepository workflowLogRepository;
    
    private Request targetRequest;
    private Long requestId;
    private boolean isUpdatingUi = false;
    private boolean showingInternalTab = false;

    private final VerticalLayout detailsCard = new VerticalLayout();
    private final Select<Integer> impactSelect = new Select<>();
    private final Select<Integer> urgencySelect = new Select<>();
    private final Select<TaskType> typeSelect = new Select<>();
    private final Select<Department> departmentField = new Select<>();
    
    private final H1 scoreValue = new H1("0");
    private final Span priorityBadge = new Span("DÜŞÜK ÖNCELİKLİ");

    private final Tabs chatTabs = new Tabs();
    private final Tab customerTab = new Tab("💬 Müşteri İletişimi");
    private final Tab internalTab = new Tab("🔒 İç Değerlendirme");

    private final VerticalLayout chatHistoryArea = new VerticalLayout();
    private final TextArea chatInputArea = new TextArea();
    private final Upload fileUpload;
    private final MemoryBuffer fileBuffer = new MemoryBuffer();
    private String uploadedFileName = null;
    private byte[] uploadedFileBytes = null;
    private final Button rejectBtn = new Button("Talebi Müşteriye İade Et", VaadinIcon.ARROW_BACKWARD.create());

    public PMInspectView(RequestService requestService, 
                         PrioritizationService prioritizationService,
                         DepartmentRepository departmentRepository,
                         WorkflowLogService workflowLogService,
                         WorkflowLogRepository workflowLogRepository) {
        this.requestService = requestService;
        this.prioritizationService = prioritizationService;
        this.departmentRepository = departmentRepository;
        this.workflowLogService = workflowLogService;
        this.workflowLogRepository = workflowLogRepository;

        setSizeFull();
        setPadding(true);
        getStyle().set("background-color", "#f8fafc");

        H2 viewTitle = new H2("Talep İnceleme Sayfası");
        viewTitle.getStyle().set("margin-bottom", "20px");
        add(viewTitle);

        HorizontalLayout workspaceLayout = new HorizontalLayout();
        workspaceLayout.setWidthFull();
        workspaceLayout.setSpacing(true);

        VerticalLayout leftPanel = new VerticalLayout();
        leftPanel.setWidth("60%");
        leftPanel.setPadding(false);

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
        
        Button highestPriorityBtn = new Button("Highest Priority", e -> setHighestPriority());
        highestPriorityBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        highestPriorityBtn.setWidth("100px");
        highestPriorityBtn.getStyle()
            .set("background-color", "#c12a2a")
            .set("margin-bottom", "15px");

        leftPanel.add(detailsCard, impactSelect, urgencySelect, typeSelect, departmentField, highestPriorityBtn);

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

        chatTabs.add(customerTab, internalTab);
        chatTabs.setWidthFull();
        chatTabs.addSelectedChangeListener(event -> {
            showingInternalTab = event.getSelectedTab().equals(internalTab);
            rejectBtn.setVisible(!showingInternalTab);
            refreshChatHistoryWindow();
        });

        chatHistoryArea.setWidthFull();
        chatHistoryArea.setHeight("220px");
        chatHistoryArea.getStyle()
                .set("overflow-y", "auto")
                .set("background", "#ffffff")
                .set("border", "1px solid #cbd5e1")
                .set("border-radius", "8px")
                .set("padding", "12px");

        rejectBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
        rejectBtn.setWidthFull();
        rejectBtn.addClickListener(e -> openRejectReasonDialog());

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

        rightPanel.add(scoreBox, chatTabs, chatHistoryArea, rejectBtn, chatInputArea, fileUpload, sendChatBtn, new Hr(), saveBtn);
        workspaceLayout.add(leftPanel, rightPanel);
        add(workspaceLayout);

        HorizontalLayout actionRow = new HorizontalLayout();
        actionRow.setWidthFull();
        actionRow.getStyle().set("margin-top", "20px");

        Button backBtn = new Button("Geri Dön", VaadinIcon.ARROW_LEFT.create(), e -> UI.getCurrent().navigate("pm/requests"));
        actionRow.add(backBtn);
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

            H2 titleHeader = new H2(targetRequest.getTitle());
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

            Span descLabel = new Span("TALEP AÇIKLAMASI");
            descLabel.getStyle().set("font-size", "0.75rem").set("font-weight", "800").set("color", "#64748b").set("margin-bottom", "4px");

            Span descBody = new Span(targetRequest.getDescription() != null ? targetRequest.getDescription() : "Açıklama belirtilmemiş.");
            descBody.getStyle()
                    .set("font-size", "0.95rem")
                    .set("color", "#334155")
                    .set("white-space", "pre-wrap")
                    .set("word-break", "break-word")
                    .set("overflow-wrap", "anywhere");
            descContainer.add(descLabel, descBody);

            HorizontalLayout badgesRow = new HorizontalLayout();
            badgesRow.setSpacing(true);

            Span affectedBadge = new Span();
            affectedBadge.getStyle().set("padding", "6px 12px").set("border-radius", "9999px").set("font-size", "0.8rem").set("font-weight", "700");
            if (targetRequest.getAffectedNo() != null) {
                affectedBadge.setText(targetRequest.getAffectedNo() + " Kullanıcı Etkileniyor");
                affectedBadge.getStyle().set("background-color", "#e0f2fe").set("color", "#0369a1");
            } else {
                affectedBadge.setText("Etkilenen Sayısı Belirtilmemiş");
                affectedBadge.getStyle().set("background-color", "#f1f5f9").set("color", "#475569");
            }

            Span companyBadge = new Span();
            companyBadge.getStyle().set("padding", "6px 12px").set("border-radius", "9999px").set("font-size", "0.8rem").set("font-weight", "700");
            if (targetRequest.getCustomer() != null && targetRequest.getCustomer().getCompany() != null) {
                String companyName = targetRequest.getCustomer().getCompany().getName();
                double companyScore = targetRequest.getCustomer().getCompany().getCompanyScore();
                companyBadge.setText(companyName + " (Score: " + companyScore + ")");
                companyBadge.getStyle().set("background-color", "#f0fdf4").set("color", "#166534");
            } else {
                companyBadge.setText("Bireysel Müşteri");
                companyBadge.getStyle().set("background-color", "#f1f5f9").set("color", "#475569");
            }

            badgesRow.add(affectedBadge, companyBadge);
            detailsCard.add(titleHeader, descContainer, badgesRow);
            
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
            } finally {
                isUpdatingUi = false;
                refreshCalculatedScoreText();
                refreshChatHistoryWindow();
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

        List<WorkflowLog> chatLogs = workflowLogRepository.findByRequestIdWithUser(requestId).stream()
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
                String text = log.getLogText();
                if (text.contains("[MÜŞTERİYE İADE EDİLDİ]")) {
                    text = text.replace("[MÜŞTERİYE İADE EDİLDİ]: Gerekçe:", "").trim();
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
    }

    private void submitPmChatMessage() {
        User currentUser = (User) VaadinSession.getCurrent().getAttribute("user");
        if (currentUser == null) return;

        if (chatInputArea.isEmpty() && uploadedFileBytes == null) {
            Notification.show("Boş mesaj gönderilemez!").addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        try {
            WorkflowLog log = new WorkflowLog();
            log.setRequest(targetRequest);
            log.setUser(currentUser);
            log.setLogText(chatInputArea.getValue() != null ? chatInputArea.getValue().trim() : null);
            log.setFromStatus(targetRequest.getStatus().name());
            log.setToStatus(targetRequest.getStatus().name());
            log.setInternal(showingInternalTab);

            if (uploadedFileBytes != null && uploadedFileName != null) {
                log.setFileName(uploadedFileName);
                log.setFileBytes(uploadedFileBytes);
            }

            workflowLogRepository.save(log);

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

        if (score > 100) {
            priorityBadge.setText("YÖNETİCİ MÜDAHALESİ");
            priorityBadge.getStyle().set("background", "#f19595").set("color", "#780303");
        } else if (score >= 60) {
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

            if (currentUser != null) {
                workflowLogService.saveChatComment(requestId, "[MÜŞTERİYE İADE EDİLDİ]: Gerekçe: " + note, currentUser, null, null);
            }

            requestService.updateRequestStatus(targetRequest.getId(), RequestStatus.SENT_BACK);
            
            Notification.show("Talep müşteriye geri gönderildi.").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            
            rejectDialog.close();
            UI.getCurrent().navigate("pm/requests");
        });
        submitReject.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);

        rejectDialog.getFooter().add(new Button("İptal", c -> rejectDialog.close()), submitReject);
        rejectDialog.open();
    }

    private void setHighestPriority() {
        updateScoreBadgeVisuals(1000);
    }
}