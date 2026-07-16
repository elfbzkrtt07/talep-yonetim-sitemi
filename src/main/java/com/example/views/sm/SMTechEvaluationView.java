package com.example.views.sm;

import com.example.base.ui.MainLayout;
import com.example.entities.Department;
import com.example.entities.Prioritization;
import com.example.entities.Request;
import com.example.entities.User;
import com.example.entities.WorkflowLog;
import com.example.repositories.UserRepository;
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

@Route(value = "sm/evaluate", layout = MainLayout.class)
public class SMTechEvaluationView extends VerticalLayout implements HasUrlParameter<Long> {

    private final RequestService requestService;
    private final PrioritizationService prioritizationService;
    private final UserRepository userRepository;
    private final WorkflowService workflowService;
    private final WorkflowLogService workflowLogService;
    private final WorkflowLogRepository workflowLogRepository;

    private Request targetRequest;
    private Prioritization currentPrioritization;
    private Long requestId;
    private boolean isUpdatingUi = false;
    private boolean showingInternalTab = false;

    private final VerticalLayout detailsCard = new VerticalLayout();
    private final H1 pmScoreDisplay = new H1("0");
    private final H1 techScoreDisplay = new H1("0");
    
    private final Select<Integer> techScoreSelect = new Select<>();
    private final Select<Integer> complexitySelect = new Select<>();
    private final Select<Integer> effortSelect = new Select<>();
    
    private final TextArea smCommentArea = new TextArea("Teknik Değerlendirme / Notlar");
    private final Select<User> developerSelect = new Select<>();

    private final Tabs chatTabs = new Tabs();
    private final Tab customerTab = new Tab("Müşteri İletişimi");
    private final Tab internalTab = new Tab("İç Değerlendirme");

    private final VerticalLayout chatHistoryArea = new VerticalLayout();
    private final TextArea chatInputArea = new TextArea("Mesaj");
    private final Upload fileUpload;
    private final MemoryBuffer fileBuffer = new MemoryBuffer();
    private String uploadedFileName = null;
    private byte[] uploadedFileBytes = null;
    
    private final Button sendChatBtn;

    public SMTechEvaluationView(RequestService requestService, 
                                PrioritizationService prioritizationService,
                                UserRepository userRepository,
                                WorkflowService workflowService,
                                WorkflowLogService workflowLogService,
                                WorkflowLogRepository workflowLogRepository) {
        this.requestService = requestService;
        this.prioritizationService = prioritizationService;
        this.userRepository = userRepository;
        this.workflowService = workflowService;
        this.workflowLogService = workflowLogService;
        this.workflowLogRepository = workflowLogRepository;

        setSizeFull();
        setPadding(true);
        getStyle().set("background-color", "#f8fafc");

        H2 viewTitle = new H2("Teknik Değerlendirme Penceresi");
        viewTitle.getStyle().set("margin-bottom", "20px");
        add(viewTitle);

        HorizontalLayout workspaceLayout = new HorizontalLayout();
        workspaceLayout.setWidthFull();
        workspaceLayout.setSpacing(true);

        VerticalLayout leftPanel = new VerticalLayout();
        leftPanel.setWidth("60%");
        leftPanel.setPadding(false);

        techScoreSelect.setLabel("Temel Mimari Zorluk (Base Tech Score)");
        techScoreSelect.setItems(1, 2, 3, 4, 5);
        techScoreSelect.setValue(1);
        techScoreSelect.setWidthFull();

        complexitySelect.setLabel("Sistem Bağımlılık Karmaşıklığı (Complexity)");
        complexitySelect.setItems(1, 2, 3, 4, 5);
        complexitySelect.setValue(1);
        complexitySelect.setWidthFull();

        effortSelect.setLabel("Tahmini Geliştirme Eforu (Effort)");
        effortSelect.setItems(1, 2, 3, 4, 5);
        effortSelect.setValue(1);
        effortSelect.setWidthFull();

        smCommentArea.setPlaceholder("Mimari zorluklar, bağımlılıklar veya tahmini efor notları giriniz...");
        smCommentArea.setWidthFull();
        smCommentArea.setHeight("80px");

        developerSelect.setLabel("Geliştirici Ata (Select Developer)");
        developerSelect.setPlaceholder("Ekipten bir yazılımcı seçiniz...");
        developerSelect.setWidthFull();
        developerSelect.setItemLabelGenerator(User::getName);

        leftPanel.add(detailsCard, techScoreSelect, complexitySelect, effortSelect, smCommentArea, developerSelect);

        VerticalLayout rightPanel = new VerticalLayout();
        rightPanel.setWidth("40%");
        rightPanel.getStyle().set("background", "#eef2f7")
                            .set("border-radius", "12px")
                            .set("padding", "20px");

        HorizontalLayout metricCardRow = new HorizontalLayout();
        metricCardRow.setWidthFull();
        metricCardRow.getStyle().set("background", "#ffffff").set("border-radius", "8px").set("padding", "10px").set("margin-bottom", "10px");
        
        VerticalLayout pmCard = new VerticalLayout();
        pmCard.setPadding(false); pmCard.setSpacing(false); pmCard.setAlignItems(FlexComponent.Alignment.CENTER);
        Span pmHeader = new Span("PM SKORU");
        pmHeader.getStyle().set("font-size", "0.7rem").set("color", "#64748b").set("font-weight", "bold");
        pmScoreDisplay.getStyle().set("margin", "0").set("font-size", "2rem").set("color", "#1e3a8a");
        pmCard.add(pmHeader, pmScoreDisplay);

        VerticalLayout techCard = new VerticalLayout();
        techCard.setPadding(false); 
        techCard.setSpacing(false); 
        techCard.setAlignItems(FlexComponent.Alignment.CENTER);
        
        Span techHeader = new Span("HESAPLANAN TEK. SKOR");
        techHeader.getStyle().set("font-size", "0.7rem").set("color", "#64748b").set("font-weight", "bold");

        Button decTechBtn = new Button(VaadinIcon.MINUS.create(), e -> {
            int currentTechScore = Integer.parseInt(techScoreDisplay.getText());
            int newTechScore = Math.max(0, currentTechScore - 1);
            techScoreDisplay.setText(String.valueOf(newTechScore));
        });
        decTechBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);

        Button incTechBtn = new Button(VaadinIcon.PLUS.create(), e -> {
            int currentTechScore = Integer.parseInt(techScoreDisplay.getText());
            int newTechScore = Math.min(100, currentTechScore + 1);
            techScoreDisplay.setText(String.valueOf(newTechScore));
        });
        incTechBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);

        techScoreDisplay.getStyle().set("margin", "0 10px").set("font-size", "2rem").set("color", "#d97706");

        HorizontalLayout interactiveTechRow = new HorizontalLayout(decTechBtn, techScoreDisplay, incTechBtn);
        interactiveTechRow.setAlignItems(FlexComponent.Alignment.CENTER);

        techCard.add(techHeader, interactiveTechRow);
        
        metricCardRow.add(pmCard, techCard);

        sendChatBtn = new Button("Mesaj Gönder", e -> submitSmChatMessage());
        sendChatBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        sendChatBtn.setWidthFull();
        sendChatBtn.getStyle().set("margin-bottom", "10px");

        Button submitBtn = new Button("TEKNİK ANALİZİ TAMAMLA", VaadinIcon.PAPERPLANE.create());
        submitBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
        submitBtn.setWidthFull();
        submitBtn.addClickListener(e -> saveTechnicalEvaluationData());

        Button returnBtn = new Button("Ürün Yöneticisine Geri Gönder", VaadinIcon.RECYCLE.create());
        returnBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
        returnBtn.setWidthFull();
        returnBtn.addClickListener(e -> sendWorkflowBackToProductManager());

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
                Notification.show("Dosya yükleme hatası!").addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        chatTabs.add(customerTab, internalTab);
        chatTabs.setWidthFull();
        chatTabs.addSelectedChangeListener(event -> {
            showingInternalTab = event.getSelectedTab().equals(internalTab);
            
            if (showingInternalTab) {
                chatInputArea.setReadOnly(false);
                chatInputArea.setPlaceholder("Ekiple paylaşmak için bir şeyler yazın...");
                fileUpload.setVisible(true);
                sendChatBtn.setVisible(true);
            } else {
                chatInputArea.setReadOnly(true);
                chatInputArea.setValue(""); 
                chatInputArea.setPlaceholder("Müşteri kanalı ekipler için salt okunurdur.");
                fileUpload.setVisible(false);
                sendChatBtn.setVisible(false);
            }
            
            refreshChatHistoryWindow();
        });

        chatHistoryArea.setWidthFull();
        chatHistoryArea.setHeight("200px");
        chatHistoryArea.getStyle()
                .set("overflow-y", "auto")
                .set("background", "#ffffff")
                .set("border", "1px solid #cbd5e1")
                .set("border-radius", "8px")
                .set("padding", "12px");

        chatInputArea.setWidthFull();
        chatInputArea.setHeight("60px");
        chatInputArea.getStyle().set("margin-bottom", "5px");

        Span chatTitle = new Span("Talep İletişim Grubu");
        chatTitle.getStyle().set("font-weight", "bold").set("font-size", "1rem").set("margin-bottom", "5px");

        rightPanel.add(metricCardRow, chatTabs, chatHistoryArea, chatInputArea, fileUpload, sendChatBtn, new Hr(), submitBtn, returnBtn);       
        workspaceLayout.add(leftPanel, rightPanel);
        add(workspaceLayout);

        Button backBtn = new Button("Geri Dön", VaadinIcon.ARROW_LEFT.create(), e -> UI.getCurrent().navigate("sm/requests"));
        add(backBtn);

        techScoreSelect.addValueChangeListener(e -> { if (!isUpdatingUi) recalculateTechnicalScoreText(); });
        complexitySelect.addValueChangeListener(e -> { if (!isUpdatingUi) recalculateTechnicalScoreText(); });
        effortSelect.addValueChangeListener(e -> { if (!isUpdatingUi) recalculateTechnicalScoreText(); });

        chatInputArea.setReadOnly(true);
        chatInputArea.setPlaceholder("Müşteri kanalı ekipler için salt okunurdur.");
        fileUpload.setVisible(false);
        sendChatBtn.setVisible(false);
    }

    @Override
    public void setParameter(BeforeEvent event, Long parameter) {
        this.requestId = parameter;
        Optional<Request> requestOpt = requestService.getAllRequests().stream()
                .filter(r -> r.getId().equals(parameter))
                .findFirst();

        if (requestOpt.isPresent()) {
            this.targetRequest = requestOpt.get();
            try {
                this.currentPrioritization = prioritizationService.getPrioritizationById(targetRequest.getId());
                populateViewContent();
            } catch (Exception ex) {
                Notification.show("Önceliklendirme kaydı bulunamadı!").addThemeVariants(NotificationVariant.LUMO_ERROR);
                UI.getCurrent().navigate("sm/requests");
            }
        } else {
            Notification.show("Talep kaydı bulunamadı!").addThemeVariants(NotificationVariant.LUMO_ERROR);
            UI.getCurrent().navigate("sm/requests");
        }
    }

    private void populateViewContent() {
        isUpdatingUi = true;
        Department activeDept = currentPrioritization.getDepartment();
        String deptNameStr = (activeDept != null) ? activeDept.getName() : "Belirtilmemis";

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

        Span descLabel = new Span("TALEP ACIKLAMASI");
        descLabel.getStyle().set("font-size", "0.75rem").set("font-weight", "800").set("color", "#64748b").set("margin-bottom", "4px");

        Span descBody = new Span(targetRequest.getDescription() != null ? targetRequest.getDescription() : "Aciklama belirtilmemis.");
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
            affectedBadge.setText(targetRequest.getAffectedNo() + " Etkilenen");
            affectedBadge.getStyle().set("background-color", "#e0f2fe").set("color", "#0369a1");
        } else {
            affectedBadge.setText("Belirtilmemis");
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
            companyBadge.setText("Bireysel Musteri");
            companyBadge.getStyle().set("background-color", "#f1f5f9").set("color", "#475569");
        }

        Span deptBadge = new Span("Departman: " + deptNameStr);
        deptBadge.getStyle().set("padding", "6px 12px").set("border-radius", "9999px").set("font-size", "0.8rem").set("font-weight", "700").set("background-color", "#faf5ff").set("color", "#6b21a8");

        badgesRow.add(affectedBadge, companyBadge, deptBadge);
        detailsCard.add(titleHeader, descContainer, badgesRow);
        
        pmScoreDisplay.setText(String.valueOf(currentPrioritization.getPriorityScore()));

        boolean hasSavedTechnicalEvaluation = false;

        if (currentPrioritization.getSmTechnicalScore() != null) {
            techScoreDisplay.setText(String.valueOf(currentPrioritization.getSmTechnicalScore()));
            techScoreSelect.setValue(Math.min(5, Math.max(1, currentPrioritization.getSmTechnicalScore() / 10))); 
            hasSavedTechnicalEvaluation = true;
        }

        if (activeDept != null) {
            List<User> eligibleDevelopers = userRepository.findByRoleAndDepartmentId("DEVELOPER", activeDept.getId());
            developerSelect.setItems(eligibleDevelopers);
        }

        isUpdatingUi = false;
        
        if (!hasSavedTechnicalEvaluation) {
            recalculateTechnicalScoreText();
        }

        refreshChatHistoryWindow();
    }
        
    private void recalculateTechnicalScoreText() {
        int baseTech = techScoreSelect.getValue();
        int complexity = complexitySelect.getValue();
        int effort = effortSelect.getValue();
        
        int totalTechCalculatedScore = (baseTech + complexity) * effort * 2;
        techScoreDisplay.setText(String.valueOf(totalTechCalculatedScore));
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

    private void submitSmChatMessage() {
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
            Notification.show("Mesaj iletildi.").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } catch (Exception ex) {
            Notification.show("Hata: " + ex.getMessage()).addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    protected void saveTechnicalEvaluationData() {
        User assignedDev = developerSelect.getValue();
        if (assignedDev == null) {
            Notification.show("Lütfen bu iş akışını yönetecek bir geliştirici seçin!").addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        User currentUser = (User) VaadinSession.getCurrent().getAttribute("user");
        int finalTechScore = Integer.parseInt(techScoreDisplay.getText());

        if (currentUser != null) {
            workflowLogService.saveChatComment(requestId, "[TEKNİK ANALİZ TAMAMLANDI]: Teknik değerlendirme bitti. Hesaplanan Zorluk Skoru: " + finalTechScore + ". Görev şu yazılımcıya atandı: " + assignedDev.getName(), currentUser, null, null);
        }

        prioritizationService.completeTechnicalEvaluation(
                targetRequest.getId(),
                finalTechScore,
                smCommentArea.getValue().trim(),
                assignedDev
        );

        Notification.show("Teknik analiz kaydedildi ve iş akışı geliştiriciye atandı.").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        UI.getCurrent().navigate("sm/requests");
    }

    private void sendWorkflowBackToProductManager() {
        String reason = smCommentArea.getValue().trim();
        if (reason.isEmpty()) {
            Notification.show("Lütfen iade gerekçenizi teknik notlar alanına yazın!").addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        User currentUser = (User) VaadinSession.getCurrent().getAttribute("user");
        int finalTechScore = Integer.parseInt(techScoreDisplay.getText());

        if (currentUser != null) {
            workflowLogService.saveChatComment(requestId, "[PM'E GERİ GÖNDERİLDİ]: Gerekçe Notu: " + reason, currentUser, null, null);
        }

        workflowService.rejectBackToPM(targetRequest.getId(), finalTechScore, reason);
        
        Notification.show("Talep revize edilmesi için Ürün Yöneticisine geri gönderildi.").addThemeVariants(NotificationVariant.LUMO_PRIMARY);
        UI.getCurrent().navigate("sm/requests");
    }
}