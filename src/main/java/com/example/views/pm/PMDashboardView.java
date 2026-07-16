package com.example.views.pm;

import com.example.base.ui.MainLayout;
import com.example.entities.Company;
import com.example.entities.Request;
import com.example.entities.User;
import com.example.entities.WorkflowLog;
import com.example.repositories.WorkflowLogRepository;
import com.example.services.CompanyService;
import com.example.services.RequestService;
import com.example.services.WorkflowLogService;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Route(value = "pm/dashboard", layout = MainLayout.class)
public class PMDashboardView extends VerticalLayout implements BeforeEnterObserver {

    private final RequestService requestService;
    private final WorkflowLogService workflowLogService;
    private final WorkflowLogRepository workflowLogRepository;
    private final CompanyService companyService;

    private final Button btnOverviewTab = new Button("Genel Durum", VaadinIcon.DASHBOARD.create());
    private final Button btnChatTab = new Button("Hızlı Sohbetler", VaadinIcon.CHAT.create());

    private final VerticalLayout overviewContainer = new VerticalLayout();
    private final HorizontalLayout chatContainer = new HorizontalLayout();

    private final VerticalLayout chatSidebar = new VerticalLayout();
    private final VerticalLayout chatArea = new VerticalLayout();
    private final Div chatHistoryContainer = new Div();
    private final TextField messageInput = new TextField();
    private final Button btnSend = new Button(VaadinIcon.PAPERPLANE.create());
    private final H3 currentChatTitle = new H3("Sohbet Seçin");
    private final Span currentChatStatus = new Span();
    private final Button btnGoToDetails = new Button("Detaylı İncele", VaadinIcon.EXTERNAL_LINK.create());
    private boolean showOnlyInternal = true;
    private final Button btnToggleFilter = new Button("Sadece Internal", VaadinIcon.EYE.create());

    private final H1 welcomeHeading = new H1("Hoşgeldiniz");
    private final VerticalLayout recentRequestsLayout = new VerticalLayout();
    private final VerticalLayout companiesLayout = new VerticalLayout();

    private List<Request> allRequests = new ArrayList<>();
    private Request selectedRequest = null;
    private User currentUser = null;

    public PMDashboardView(RequestService requestService, 
                           WorkflowLogService workflowLogService,
                           WorkflowLogRepository workflowLogRepository,
                           CompanyService companyService) {
        this.requestService = requestService;
        this.workflowLogService = workflowLogService;
        this.workflowLogRepository = workflowLogRepository;
        this.companyService = companyService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);
        getStyle().set("background-color", "#f8fafc").set("box-sizing", "border-box").set("overflow-x", "hidden");

        setupTabsLayout();
        setupOverviewTab();
        setupChatTabStructure();

        showOverviewTab();
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        currentUser = (User) VaadinSession.getCurrent().getAttribute("user");
        if (currentUser == null) {
            event.rerouteTo("login");
            return;
        }
        loadDashboardData();
    }

    private void loadDashboardData() {
        allRequests = requestService.getAllRequests();
        buildOverviewTab();
        buildChatSidebar(allRequests);
    }

    private void setupTabsLayout() {
        HorizontalLayout tabLayout = new HorizontalLayout(btnOverviewTab, btnChatTab);
        tabLayout.setSpacing(true);
        tabLayout.getStyle().set("margin-bottom", "5px");

        btnOverviewTab.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        btnChatTab.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        btnOverviewTab.addClickListener(e -> showOverviewTab());
        btnChatTab.addClickListener(e -> showChatTab());

        add(tabLayout);
    }

    private void showOverviewTab() {
        btnOverviewTab.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        btnOverviewTab.removeThemeVariants(ButtonVariant.LUMO_TERTIARY);
        btnChatTab.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        btnChatTab.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);

        overviewContainer.setVisible(true);
        chatContainer.setVisible(false);
    }

    private void showChatTab() {
        btnChatTab.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        btnChatTab.removeThemeVariants(ButtonVariant.LUMO_TERTIARY);
        btnOverviewTab.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        btnOverviewTab.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);

        overviewContainer.setVisible(false);
        chatContainer.setVisible(true);
    }

    private void setupOverviewTab() {
        overviewContainer.setSizeFull();
        overviewContainer.setPadding(false);
        overviewContainer.setSpacing(true);
        overviewContainer.getStyle().set("box-sizing", "border-box");

        VerticalLayout headerLayout = new VerticalLayout();
        headerLayout.setWidthFull();
        headerLayout.setAlignItems(Alignment.CENTER);
        
        Image logo = new Image("logo.png", "MONAD Logo");
        logo.setHeight("80px");
        welcomeHeading.getStyle().set("margin-top", "10px").set("margin-bottom", "20px");
        
        headerLayout.add(logo, welcomeHeading);

        HorizontalLayout rowLayout = new HorizontalLayout();
        rowLayout.setWidthFull();
        rowLayout.setSpacing(true);
        rowLayout.getStyle().set("box-sizing", "border-box");

        recentRequestsLayout.setWidth("58%");
        recentRequestsLayout.setPadding(false);
        recentRequestsLayout.setSpacing(true);
        recentRequestsLayout.getStyle().set("box-sizing", "border-box");
        
        H3 recentTitle = new H3("Son Güncellenen Talepler");
        recentTitle.getStyle().set("margin", "0 0 10px 0").set("color", "#0f172a");
        recentRequestsLayout.add(recentTitle);

        companiesLayout.setWidth("40%");
        companiesLayout.setPadding(false);
        companiesLayout.setSpacing(true);
        companiesLayout.getStyle().set("box-sizing", "border-box");
        
        H3 companiesTitle = new H3("Şirketler & Puanlar");
        companiesTitle.getStyle().set("margin", "0 0 10px 0").set("color", "#0f172a");
        companiesLayout.add(companiesTitle);

        rowLayout.add(recentRequestsLayout, companiesLayout);
        overviewContainer.add(headerLayout, rowLayout);
        add(overviewContainer);
    }

    private void buildOverviewTab() {
        if (currentUser != null) {
            welcomeHeading.setText("Hoşgeldiniz, " + currentUser.getName());
        }

        while (recentRequestsLayout.getComponentCount() > 1) {
            recentRequestsLayout.remove(recentRequestsLayout.getComponentAt(recentRequestsLayout.getComponentCount() - 1));
        }

        List<Request> recent = allRequests.stream()
                .sorted((r1, r2) -> {
                    if (r1.getUpdatedAt() == null) return 1;
                    if (r2.getUpdatedAt() == null) return -1;
                    return r2.getUpdatedAt().compareTo(r1.getUpdatedAt());
                })
                .limit(5)
                .toList();

        for (Request req : recent) {
            HorizontalLayout row = new HorizontalLayout();
            row.setWidthFull();
            row.setPadding(true);
            row.setAlignItems(Alignment.CENTER);
            row.getStyle()
                    .set("background-color", "#ffffff")
                    .set("border", "1px solid #e2e8f0")
                    .set("border-radius", "8px")
                    .set("box-shadow", "0 1px 2px rgba(0,0,0,0.02)")
                    .set("box-sizing", "border-box");

            Span id = new Span("#" + req.getId());
            id.getStyle().set("font-weight", "bold").set("color", "#3b82f6").set("width", "60px");

            Span title = new Span(req.getTitle() != null ? req.getTitle() : "Başlıksız Talep");
            title.getStyle().set("font-weight", "600").set("color", "#1e293b");

            Span status = new Span(req.getStatus() != null ? req.getStatus().toString() : "-");
            status.getStyle()
                    .set("font-size", "0.75rem")
                    .set("padding", "2px 8px")
                    .set("border-radius", "12px")
                    .set("background-color", "#f1f5f9")
                    .set("color", "#475569");

            Button btnGo = new Button(VaadinIcon.ARROW_RIGHT.create());
            btnGo.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            btnGo.addClickListener(e -> UI.getCurrent().navigate("pm/inspect/" + req.getId()));

            row.add(id, title, status, btnGo);
            row.setFlexGrow(1, title);
            recentRequestsLayout.add(row);
        }

        while (companiesLayout.getComponentCount() > 1) {
            companiesLayout.remove(companiesLayout.getComponentAt(companiesLayout.getComponentCount() - 1));
        }
        
        List<Company> allCompanies = companyService.getAllCompanies(); 
        
        for (Company comp : allCompanies) {
            HorizontalLayout companyRow = new HorizontalLayout();
            companyRow.setAlignItems(Alignment.CENTER);
            companyRow.setWidthFull();
            companyRow.getStyle()
                    .set("background-color", "#ffffff")
                    .set("padding", "10px")
                    .set("border-radius", "8px")
                    .set("border", "1px solid #e2e8f0")
                    .set("box-sizing", "border-box");
            
            Span compName = new Span(comp.getName() != null ? comp.getName() : "İsimsiz Şirket");
            compName.getStyle().set("font-weight", "600");
            
            NumberField scoreField = new NumberField();
            scoreField.setValue(comp.getCompanyScore() != null ? comp.getCompanyScore() : 50.0);
            scoreField.setWidth("90px");
            
            Button saveBtn = new Button("Kaydet", VaadinIcon.CHECK.create());
            saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
            saveBtn.addClickListener(e -> {
                comp.setCompanyScore(scoreField.getValue());
                companyService.updateCompany(comp); 
                Notification.show(comp.getName() + " puanı güncellendi!", 3000, Notification.Position.TOP_CENTER);
            });
            
            companyRow.add(compName, scoreField, saveBtn);
            companyRow.setFlexGrow(1, compName);
            companiesLayout.add(companyRow);
        }
    }

    private void setupChatTabStructure() {
        chatContainer.setSizeFull();
        chatContainer.setSpacing(false);
        chatContainer.setPadding(false);
        chatContainer.getStyle()
                .set("border", "1px solid #cbd5e1")
                .set("border-radius", "8px")
                .set("background-color", "#ffffff")
                .set("overflow", "hidden")
                .set("box-sizing", "border-box");

        chatSidebar.setWidth("350px");
        chatSidebar.setHeightFull();
        chatSidebar.setPadding(true);
        chatSidebar.setSpacing(true);
        chatSidebar.getStyle()
                .set("border-right", "1px solid #cbd5e1")
                .set("box-sizing", "border-box");

        TextField searchField = new TextField();
        searchField.setWidthFull();
        searchField.setPlaceholder("Sohbet ara...");
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchField.setValueChangeMode(ValueChangeMode.EAGER);
        searchField.addValueChangeListener(e -> filterChatSidebar(e.getValue()));

        chatSidebar.add(searchField);

        chatArea.setHeightFull();
        chatArea.setSpacing(false);
        chatArea.setPadding(false);
        chatArea.getStyle()
                .set("flex", "1")
                .set("background-color", "#f1f5f9")
                .set("overflow", "hidden")
                .set("box-sizing", "border-box");

        HorizontalLayout chatHeader = new HorizontalLayout();
        chatHeader.setWidthFull();
        chatHeader.setHeight("65px");
        chatHeader.setPadding(true);
        chatHeader.setAlignItems(Alignment.CENTER);
        chatHeader.getStyle()
                .set("background-color", "#ffffff")
                .set("border-bottom", "1px solid #cbd5e1")
                .set("box-sizing", "border-box");

        currentChatTitle.getStyle().set("margin", "0").set("font-size", "1.05rem").set("font-weight", "700");
        currentChatStatus.getStyle()
                .set("font-size", "0.75rem")
                .set("padding", "3px 8px")
                .set("border-radius", "12px")
                .set("background-color", "#e2e8f0")
                .set("color", "#475569");

        btnGoToDetails.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        btnGoToDetails.setVisible(false);
        btnGoToDetails.addClickListener(e -> {
            if (selectedRequest != null) {
                UI.getCurrent().navigate("pm/inspect/" + selectedRequest.getId());
            }
        });

        btnToggleFilter.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_CONTRAST);
        btnToggleFilter.addClickListener(e -> {
            showOnlyInternal = !showOnlyInternal;
            btnToggleFilter.setText(showOnlyInternal ? "Sadece Internal" : "Sadece External");
            btnToggleFilter.addThemeVariants(showOnlyInternal ? ButtonVariant.LUMO_PRIMARY : ButtonVariant.LUMO_SUCCESS);
            if (selectedRequest != null) {
                loadChatHistory(selectedRequest.getId());
            }
        });

        HorizontalLayout titleGroup = new HorizontalLayout(currentChatTitle, currentChatStatus, btnToggleFilter);
        titleGroup.setAlignItems(Alignment.CENTER);

        chatHeader.add(titleGroup, btnGoToDetails);
        chatHeader.setFlexGrow(1, titleGroup);

        chatHistoryContainer.setSizeFull();
        chatHistoryContainer.getStyle()
                .set("padding", "20px")
                .set("overflow-y", "auto")
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("gap", "12px")
                .set("box-sizing", "border-box");

        Scroller chatScroller = new Scroller(chatHistoryContainer);
        chatScroller.setSizeFull();
        chatScroller.getStyle()
                .set("background-color", "#f8fafc")
                .set("box-sizing", "border-box");

        HorizontalLayout chatFooter = new HorizontalLayout();
        chatFooter.setWidthFull();
        chatFooter.setPadding(true);
        chatFooter.getStyle()
                .set("background-color", "#ffffff")
                .set("border-top", "1px solid #cbd5e1")
                .set("box-sizing", "border-box");

        messageInput.setWidthFull();
        messageInput.setPlaceholder("Bir mesaj yazın...");
        messageInput.setEnabled(false);
        messageInput.getStyle().set("box-sizing", "border-box");
        messageInput.addKeyPressListener(Key.ENTER, e -> sendChatMessage());

        btnSend.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        btnSend.setEnabled(false);
        btnSend.addClickListener(e -> sendChatMessage());

        chatFooter.add(messageInput, btnSend);

        chatArea.add(chatHeader, chatScroller, chatFooter);
        chatContainer.add(chatSidebar, chatArea);

        add(chatContainer);
    }

    private void buildChatSidebar(List<Request> requests) {
        while (chatSidebar.getComponentCount() > 1) {
            chatSidebar.remove(chatSidebar.getComponentAt(chatSidebar.getComponentCount() - 1));
        }

        VerticalLayout scrollContainer = new VerticalLayout();
        scrollContainer.setPadding(false);
        scrollContainer.setSpacing(true);

        Scroller scroller = new Scroller(scrollContainer);
        scroller.setSizeFull();

        for (Request req : requests) {
            Div card = createChatSidebarCard(req);
            scrollContainer.add(card);
        }

        chatSidebar.add(scroller);
    }

    private Div createChatSidebarCard(Request req) {
        Div card = new Div();
        card.setWidthFull();
        card.getStyle()
                .set("padding", "12px")
                .set("border-radius", "8px")
                .set("cursor", "pointer")
                .set("border-bottom", "1px solid #f1f5f9")
                .set("transition", "background-color 0.2s")
                .set("box-sizing", "border-box");

        card.addClickListener(e -> selectChatRequest(req, card));

        Span idAndTitle = new Span("#" + req.getId() + " - " + (req.getTitle() != null ? req.getTitle() : "Başlıksız"));
        idAndTitle.getStyle()
                .set("font-weight", "bold")
                .set("font-size", "0.9rem")
                .set("color", "#1e293b")
                .set("display", "block");

        Span status = new Span(req.getStatus() != null ? req.getStatus().toString() : "UNKNOWN");
        status.getStyle()
                .set("font-size", "0.7rem")
                .set("color", "#64748b")
                .set("display", "block")
                .set("margin-top", "4px");

        card.add(idAndTitle, status);
        return card;
    }

    private void selectChatRequest(Request req, Div cardComponent) {
        this.selectedRequest = req;

        chatSidebar.getChildren()
                .filter(c -> c instanceof Scroller)
                .forEach(scroller -> ((Scroller) scroller).getContent().getChildren()
                        .forEach(child -> child.getElement().getStyle().set("background-color", "transparent")));

        cardComponent.getStyle().set("background-color", "#f1f5f9");

        currentChatTitle.setText("#" + req.getId() + " " + req.getTitle());
        currentChatStatus.setText(req.getStatus() != null ? req.getStatus().toString() : "Bilinmiyor");
        btnGoToDetails.setVisible(true);
        messageInput.setEnabled(true);
        btnSend.setEnabled(true);

        loadChatHistory(req.getId());
    }

    private void loadChatHistory(Long requestId) {
        chatHistoryContainer.removeAll();
        
        List<WorkflowLog> logs = workflowLogRepository.findByRequestIdWithUser(requestId).stream()
                    .filter(log -> showOnlyInternal ? log.isInternal() : !log.isInternal())
                    .sorted((l1, l2) -> {
                        if (l1.getCreatedAt() == null) return 1;
                        if (l2.getCreatedAt() == null) return -1;
                        return l1.getCreatedAt().compareTo(l2.getCreatedAt());
                    })
                    .toList();
                    
        if (logs.isEmpty()) {
            Span emptyMsg = new Span("Henüz konuşma geçmişi bulunmuyor.");
            emptyMsg.getStyle().set("color", "#94a3b8").set("font-size", "0.9rem").set("margin", "auto");
            chatHistoryContainer.add(emptyMsg);
            return;
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");

        for (WorkflowLog log : logs) {
            Div bubbleWrapper = new Div();
            bubbleWrapper.setWidthFull();
            bubbleWrapper.getStyle()
                    .set("display", "flex")
                    .set("box-sizing", "border-box");

            Div bubble = new Div();
            bubble.getStyle()
                    .set("padding", "10px 14px")
                    .set("border-radius", "12px")
                    .set("max-width", "70%")
                    .set("box-shadow", "0 1px 2px rgba(0,0,0,0.05)")
                    .set("display", "flex")
                    .set("flex-direction", "column")
                    .set("box-sizing", "border-box");

            if (log.isInternal()) {
                bubble.getStyle().set("border", "1px solid #fef3c7"); 
            }

            User senderUser = log.getUser();
            Span sender = new Span(senderUser != null ? senderUser.getName() : "Sistem");
            sender.getStyle().set("font-size", "0.75rem").set("font-weight", "bold").set("margin-bottom", "4px");

            Span content = new Span(log.getLogText());
            content.getStyle().set("font-size", "0.85rem").set("word-break", "break-word");

            Span time = new Span(log.getCreatedAt() != null ? log.getCreatedAt().format(formatter) : "");
            time.getStyle().set("font-size", "0.65rem").set("color", "#94a3b8").set("align-self", "flex-end").set("margin-top", "4px");

            bubble.add(sender, content, time);

            boolean isMe = senderUser != null && currentUser != null && senderUser.getId().equals(currentUser.getId());
            if (isMe) {
                bubbleWrapper.getStyle().set("justify-content", "flex-end");
                bubble.getStyle().set("background-color", "#d9fdd3").set("color", "#111b21");
                sender.getStyle().set("color", "#008069");
            } else {
                bubbleWrapper.getStyle().set("justify-content", "flex-start");
                bubble.getStyle().set("background-color", "#ffffff").set("color", "#111b21");
                sender.getStyle().set("color", "#3b82f6");
            }

            bubbleWrapper.add(bubble);
            chatHistoryContainer.add(bubbleWrapper);
        }

        chatHistoryContainer.getElement().executeJs("this.scrollTop = this.scrollHeight;");
    }

    private void sendChatMessage() {
        String text = messageInput.getValue().trim();
        if (text.isEmpty() || selectedRequest == null) return;

        workflowLogService.saveChatComment(
                selectedRequest.getId(), 
                text, 
                currentUser, 
                null, 
                null
        );

        messageInput.clear();
        loadChatHistory(selectedRequest.getId());
    }

    private void filterChatSidebar(String filterText) {
        if (filterText == null || filterText.trim().isEmpty()) {
            buildChatSidebar(allRequests);
        } else {
            String lower = filterText.toLowerCase();
            List<Request> filtered = allRequests.stream()
                    .filter(r -> (r.getTitle() != null && r.getTitle().toLowerCase().contains(lower)) ||
                            String.valueOf(r.getId()).contains(lower))
                    .toList();
            buildChatSidebar(filtered);
        }
    }
}