package com.example.views.developer;

import com.example.base.ui.MainLayout;
import com.example.entities.Company;
import com.example.entities.Request;
import com.example.entities.User;
import com.example.entities.Workflow;
import com.example.entities.WorkflowLog;
import com.example.repositories.WorkflowLogRepository;
import com.example.repositories.WorkflowRepository;
import com.example.services.CompanyService;
import com.example.services.PrioritizationService;
import com.example.services.WorkflowLogService;
import com.example.services.WorkflowService;
import com.example.views.base.BaseSecuredView;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.Route;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Route(value = "dev/dashboard", layout = MainLayout.class)
public class DeveloperDashboardView extends BaseSecuredView {

    private final WorkflowRepository workflowRepository;
    private final PrioritizationService prioritizationService;
    private final WorkflowLogService workflowLogService;
    private final WorkflowLogRepository workflowLogRepository;
    private final CompanyService companyService;
    private final WorkflowService workflowService;

    private final Button btnOverviewTab = new Button("Genel Durum", VaadinIcon.DASHBOARD.create());
    private final Button btnChatTab = new Button("Hızlı Sohbetler", VaadinIcon.CHAT.create());

    private final VerticalLayout overviewContainer = new VerticalLayout();
    private final HorizontalLayout chatContainer = new HorizontalLayout();

    private final VerticalLayout chatSidebar = new VerticalLayout();
    private final VerticalLayout chatListContainer = new VerticalLayout();
    private final VerticalLayout chatArea = new VerticalLayout();
    private final Div chatHistoryContainer = new Div();
    private final TextField messageInput = new TextField();
    private final Button btnSend = new Button(VaadinIcon.PAPERPLANE.create());
    private final H3 currentChatTitle = new H3("Sohbet Seçin");
    private final Span currentChatStatus = new Span();
    private final Button btnGoToDetails = new Button("Düzenle / İncele", VaadinIcon.EXTERNAL_LINK.create());

    private boolean showOnlyInternal = true;
    private final Button btnToggleFilter = new Button("Sadece Internal", VaadinIcon.EYE.create());

    private final H1 welcomeHeading = new H1("Hoşgeldiniz");
    private final Grid<Workflow> taskGrid = new Grid<>(Workflow.class, false);

    private final TextField searchField = new TextField("Genel Arama");
    private final ComboBox<Company> companyFilter = new ComboBox<>("Şirket / Hastane");
    private final ComboBox<String> financialImpactFilter = new ComboBox<>("Mali Etki");
    private final ComboBox<String> securityFilter = new ComboBox<>("KVKK / Güvenlik");

    private final DatePicker createdStartFilter = new DatePicker("Oluşturulma (Başlangıç)");
    private final DatePicker createdEndFilter = new DatePicker("Oluşturulma (Bitiş)");
    private final DatePicker deadlineStartFilter = new DatePicker("Deadline (Başlangıç)");
    private final DatePicker deadlineEndFilter = new DatePicker("Deadline (Bitiş)");

    private List<Workflow> assignedWorkflows = new ArrayList<>();
    private List<Request> allMyChatRequests = new ArrayList<>();
    private Request selectedRequest = null;

    public DeveloperDashboardView(WorkflowRepository workflowRepository, 
                                 PrioritizationService prioritizationService,
                                 WorkflowLogService workflowLogService,
                                 WorkflowLogRepository workflowLogRepository,
                                 CompanyService companyService,
                                 WorkflowService workflowService) {
        this.workflowRepository = workflowRepository;
        this.prioritizationService = prioritizationService;
        this.workflowLogService = workflowLogService;
        this.workflowLogRepository = workflowLogRepository;
        this.companyService = companyService;
        this.workflowService = workflowService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);
        getStyle().set("background-color", "#f8fafc").set("overflow-x", "hidden").set("box-sizing", "border-box");

        setupTabsLayout();
        setupFilterComponents();
        setupOverviewTab();
        setupChatTabStructure();

        add(overviewContainer);
        add(chatContainer);

        showOverviewTab();
    }

    @Override
    protected void onUserAuthenticated(BeforeEnterEvent event, User user) {
        this.currentUser = user;
        companyFilter.setItems(companyService.getAllCompanies());
        loadDeveloperData();
    }

    private void setupFilterComponents() {
        searchField.setPlaceholder("ID, Başlık, Detay veya Müşteri...");
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.addValueChangeListener(e -> applyFilters());
        searchField.setClearButtonVisible(true);
        searchField.setWidthFull();

        companyFilter.setPlaceholder("Tüm Şirketler");
        companyFilter.setItemLabelGenerator(Company::getName);
        companyFilter.setClearButtonVisible(true);
        companyFilter.addValueChangeListener(e -> applyFilters());
        companyFilter.setWidthFull();

        financialImpactFilter.setPlaceholder("Tümü");
        financialImpactFilter.setItems("Etkisi Yok", "Düşük Zarar", "Orta Düzey Zarar", "Kritik / İş Durdurucu Zarar");
        financialImpactFilter.setClearButtonVisible(true);
        financialImpactFilter.addValueChangeListener(e -> applyFilters());
        financialImpactFilter.setWidthFull();

        securityFilter.setPlaceholder("Tümü");
        securityFilter.setItems("Sadece Güvenlik / KVKK Riski Olanlar", "Risksiz Olanlar");
        securityFilter.setClearButtonVisible(true);
        securityFilter.addValueChangeListener(e -> applyFilters());
        securityFilter.setWidthFull();

        createdStartFilter.addValueChangeListener(e -> applyFilters());
        createdStartFilter.setWidthFull();
        createdEndFilter.addValueChangeListener(e -> applyFilters());
        createdEndFilter.setWidthFull();

        deadlineStartFilter.addValueChangeListener(e -> applyFilters());
        deadlineStartFilter.setWidthFull();
        deadlineEndFilter.addValueChangeListener(e -> applyFilters());
        deadlineEndFilter.setWidthFull();
    }

    private VerticalLayout createFilterBar() {
        VerticalLayout filterFieldsContainer = new VerticalLayout();
        filterFieldsContainer.setPadding(false);
        filterFieldsContainer.setSpacing(true);
        filterFieldsContainer.setVisible(false);

        HorizontalLayout mainRow = new HorizontalLayout(searchField, companyFilter, financialImpactFilter, securityFilter);
        mainRow.setWidthFull();
        mainRow.setSpacing(true);

        HorizontalLayout datesRow = new HorizontalLayout(createdStartFilter, createdEndFilter, deadlineStartFilter, deadlineEndFilter);
        datesRow.setWidthFull();
        datesRow.setSpacing(true);

        Button clearFiltersBtn = new Button("Filtreleri Temizle", VaadinIcon.CLOSE_CIRCLE.create(), e -> clearAllFilters());
        clearFiltersBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);

        HorizontalLayout clearRow = new HorizontalLayout(clearFiltersBtn);
        clearRow.setWidthFull();
        clearRow.setJustifyContentMode(JustifyContentMode.END);

        filterFieldsContainer.add(mainRow, datesRow, clearRow);

        Span filterTitle = new Span("Filtre Seçenekleri");
        filterTitle.getStyle()
                .set("font-weight", "600")
                .set("font-size", "0.9rem")
                .set("color", "#475569");

        Button toggleBtn = new Button("Filtreleri Göster", VaadinIcon.CHEVRON_DOWN.create());
        toggleBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);

        toggleBtn.addClickListener(e -> {
            boolean isVisible = filterFieldsContainer.isVisible();
            filterFieldsContainer.setVisible(!isVisible);
            toggleBtn.setText(isVisible ? "Filtreleri Göster" : "Filtreleri Gizle");
            toggleBtn.setIcon(isVisible ? VaadinIcon.CHEVRON_DOWN.create() : VaadinIcon.CHEVRON_UP.create());
        });

        HorizontalLayout filterHeaderRow = new HorizontalLayout(filterTitle, toggleBtn);
        filterHeaderRow.setWidthFull();
        filterHeaderRow.setAlignItems(Alignment.CENTER);
        filterHeaderRow.setJustifyContentMode(JustifyContentMode.BETWEEN);

        VerticalLayout filterCard = new VerticalLayout(filterHeaderRow, filterFieldsContainer);
        filterCard.setWidthFull();
        filterCard.setPadding(true);
        filterCard.setSpacing(true);
        filterCard.getStyle()
                .set("background", "#ffffff")
                .set("border-radius", "12px")
                .set("margin-bottom", "16px")
                .set("box-shadow", "0 1px 3px rgba(0,0,0,0.04)")
                .set("border", "1px solid #e2e8f0");

        return filterCard;
    }

    private void clearAllFilters() {
        searchField.clear();
        companyFilter.clear();
        financialImpactFilter.clear();
        securityFilter.clear();
        createdStartFilter.clear();
        createdEndFilter.clear();
        deadlineStartFilter.clear();
        deadlineEndFilter.clear();
        applyFilters();
    }

    private void applyFilters() {
        String query = searchField.getValue() != null ? searchField.getValue().trim().toLowerCase() : "";
        Company selectedCompany = companyFilter.getValue();
        String selectedFinancial = financialImpactFilter.getValue();
        String selectedSec = securityFilter.getValue();

        LocalDate createdStart = createdStartFilter.getValue();
        LocalDate createdEnd = createdEndFilter.getValue();
        LocalDate deadlineStart = deadlineStartFilter.getValue();
        LocalDate deadlineEnd = deadlineEndFilter.getValue();

        List<Workflow> filtered = assignedWorkflows.stream().filter(w -> {
            Request r = w.getRequest();
            if (r == null) return false;

            boolean matchesQuery = query.isEmpty() ||
                    (String.valueOf(r.getId()).contains(query)) ||
                    (r.getTitle() != null && r.getTitle().toLowerCase().contains(query)) ||
                    (r.getDescription() != null && r.getDescription().toLowerCase().contains(query)) ||
                    (r.getCustomer() != null && r.getCustomer().getName() != null && r.getCustomer().getName().toLowerCase().contains(query));

            boolean matchesCompany = (selectedCompany == null) ||
                    (r.getCustomer() != null && r.getCustomer().getCompany() != null && r.getCustomer().getCompany().getId().equals(selectedCompany.getId()));

            boolean matchesFinancial = (selectedFinancial == null) ||
                    (r.getFinancialImpact() != null && r.getFinancialImpact().equalsIgnoreCase(selectedFinancial));

            boolean matchesSecurity = true;
            if ("Sadece Güvenlik / KVKK Riski Olanlar".equals(selectedSec)) {
                matchesSecurity = Boolean.TRUE.equals(r.getIsSecurityRisk());
            } else if ("Risksiz Olanlar".equals(selectedSec)) {
                matchesSecurity = !Boolean.TRUE.equals(r.getIsSecurityRisk());
            }

            boolean matchesCreated = true;
            if (r.getCreatedAt() != null) {
                LocalDate createdDate = r.getCreatedAt().toLocalDate();
                if (createdStart != null && createdDate.isBefore(createdStart)) matchesCreated = false;
                if (createdEnd != null && createdDate.isAfter(createdEnd)) matchesCreated = false;
            } else if (createdStart != null || createdEnd != null) {
                matchesCreated = false;
            }

            boolean matchesDeadline = true;
            if (r.getDeadline() != null) {
                LocalDate deadline = r.getDeadline();
                if (deadlineStart != null && deadline.isBefore(deadlineStart)) matchesDeadline = false;
                if (deadlineEnd != null && deadline.isAfter(deadlineEnd)) matchesDeadline = false;
            } else if (deadlineStart != null || deadlineEnd != null) {
                matchesDeadline = false;
            }

            return matchesQuery && matchesCompany && matchesFinancial && matchesSecurity && matchesCreated && matchesDeadline;
        }).toList();

        taskGrid.setItems(filtered);
    }

    private void loadDeveloperData() {
        if (currentUser != null) {
            welcomeHeading.setText("Hoşgeldiniz, " + currentUser.getName());
            
            assignedWorkflows = workflowRepository.findAssignedTasksByDeveloperId(currentUser.getId());
            applyFilters();

            List<Workflow> completedWorkflows = workflowService.getCompletedJobsForDeveloper(currentUser);

            List<Workflow> combinedWorkflows = new ArrayList<>();
            combinedWorkflows.addAll(assignedWorkflows);
            combinedWorkflows.addAll(completedWorkflows);

            this.allMyChatRequests = combinedWorkflows.stream()
                    .map(Workflow::getRequest)
                    .filter(java.util.Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());

            buildChatSidebar(allMyChatRequests);
        } else {
            Notification.show("Oturum açmış kullanıcı bulunamadı!").addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void setupTabsLayout() {
        HorizontalLayout tabLayout = new HorizontalLayout(btnOverviewTab, btnChatTab);
        tabLayout.setSpacing(false);
        tabLayout.getStyle()
                .set("border-bottom", "1px solid #e2e8f0")
                .set("margin-bottom", "15px");

        btnOverviewTab.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        btnChatTab.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        btnOverviewTab.addClickListener(e -> showOverviewTab());
        btnChatTab.addClickListener(e -> showChatTab());

        add(tabLayout);
    }

    private void updateTabStyles(Button activeBtn, Button passiveBtn) {
        activeBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        activeBtn.removeThemeVariants(ButtonVariant.LUMO_TERTIARY);
        activeBtn.getStyle().set("background-color", "#0066cc");
        
        passiveBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        passiveBtn.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
        passiveBtn.getStyle().remove("background-color");
    }

    private void showOverviewTab() {
        updateTabStyles(btnOverviewTab, btnChatTab);
        overviewContainer.setVisible(true);
        chatContainer.setVisible(false);
    }

    private void showChatTab() {
        updateTabStyles(btnChatTab, btnOverviewTab);
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
        
        welcomeHeading.getStyle().set("margin-top", "10px").set("margin-bottom", "5px").set("font-size", "2rem").set("color", "#0f172a");
        Span subtitle = new Span("Ürün sorumlusu tarafından size atanan müşteri talepleri talep skoruna göre sıralanmıştır.");
        subtitle.getStyle().set("color", "#64748b").set("font-size", "0.95rem").set("margin-bottom", "20px");
        
        headerLayout.add(welcomeHeading, subtitle);

        taskGrid.addColumn(w -> w.getRequest().getId()).setHeader("ID").setAutoWidth(true);

        taskGrid.addColumn(w -> {
            if (w.getRequest() != null && w.getRequest().getCustomer() != null && w.getRequest().getCustomer().getCompany() != null) {
                return w.getRequest().getCustomer().getCompany().getName();
            }
            return "Bireysel";
        }).setHeader("Şirket / Hastane").setAutoWidth(true);

        taskGrid.addColumn(w -> w.getRequest().getCustomer().getName()).setHeader("Müşteri").setAutoWidth(true);
        taskGrid.addColumn(w -> w.getRequest().getTitle()).setHeader("Başlık").setAutoWidth(true);
        
        taskGrid.addColumn(w -> {
            if (w.getRequest() == null || w.getRequest().getCreatedAt() == null) return "-";
            return w.getRequest().getCreatedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
        }).setHeader("Oluşturulma Tarihi").setAutoWidth(true);

        taskGrid.addColumn(w -> {
            if (w.getRequest() == null || w.getRequest().getDeadline() == null) return "-";
            return w.getRequest().getDeadline().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
        }).setHeader("Target Deadline").setAutoWidth(true);

        taskGrid.addComponentColumn(w -> {
            HorizontalLayout flags = new HorizontalLayout();
            flags.setSpacing(true);

            if (w.getRequest() != null) {
                if (Boolean.TRUE.equals(w.getRequest().getIsSecurityRisk())) {
                    Span secBadge = new Span("KVKK");
                    secBadge.getStyle()
                            .set("background-color", "#fee2e2")
                            .set("color", "#991b1b")
                            .set("padding", "2px 6px")
                            .set("border-radius", "4px")
                            .set("font-size", "0.75rem")
                            .set("font-weight", "bold");
                    flags.add(secBadge);
                }

                if (w.getRequest().getFinancialImpact() != null && !w.getRequest().getFinancialImpact().isBlank() && !"Etkisi Yok".equalsIgnoreCase(w.getRequest().getFinancialImpact())) {
                    Span finBadge = new Span("Mali");
                    finBadge.getStyle()
                            .set("background-color", "#fef3c7")
                            .set("color", "#92400e")
                            .set("padding", "2px 6px")
                            .set("border-radius", "4px")
                            .set("font-size", "0.75rem")
                            .set("font-weight", "bold");
                    flags.add(finBadge);
                }
            }

            return flags;
        }).setHeader("Riskler").setAutoWidth(true);

        taskGrid.addColumn(w -> {
            try {
                return prioritizationService.getPrioritizationById(w.getRequest().getId()).getPriorityScore(); 
            } catch(Exception e) { return "-"; }
        }).setHeader("Skor").setAutoWidth(true);

        taskGrid.addComponentColumn(workflow -> {
            Button editBtn = new Button("Düzenle", e -> 
                UI.getCurrent().navigate("dev/edit/" + workflow.getRequest().getId())
            );
            editBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            editBtn.getStyle().set("background-color", "#0066cc");
            return editBtn;
        }).setHeader("İşlem");

        taskGrid.setSizeFull();

        overviewContainer.add(headerLayout, createFilterBar(), taskGrid);
    }

    private void setupChatTabStructure() {
        chatContainer.setSizeFull();
        chatContainer.setSpacing(false);
        chatContainer.setPadding(false);
        chatContainer.getStyle()
                .set("border", "1px solid #cbd5e1")
                .set("border-radius", "16px")
                .set("background-color", "#ffffff")
                .set("overflow", "hidden")
                .set("box-shadow", "0 10px 25px -5px rgba(0, 102, 204, 0.05)")
                .set("box-sizing", "border-box");

        chatSidebar.setWidth("350px");
        chatSidebar.setHeightFull();
        chatSidebar.setPadding(true);
        chatSidebar.setSpacing(true);
        chatSidebar.getStyle()
                .set("border-right", "1px solid #cbd5e1")
                .set("box-sizing", "border-box");

        TextField chatSearchField = new TextField();
        chatSearchField.setWidthFull();
        chatSearchField.setPlaceholder("Sohbet ara (Aktif ve Tamamlanan)...");
        chatSearchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        chatSearchField.setValueChangeMode(ValueChangeMode.EAGER);
        chatSearchField.addValueChangeListener(e -> filterChatSidebar(e.getValue()));

        chatListContainer.setPadding(false);
        chatListContainer.setSpacing(true);
        chatListContainer.setWidthFull();

        Scroller scroller = new Scroller(chatListContainer);
        scroller.setSizeFull();

        chatSidebar.add(chatSearchField, scroller);

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
                UI.getCurrent().navigate("dev/edit/" + selectedRequest.getId());
            }
        });

        btnToggleFilter.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);
        btnToggleFilter.setVisible(false);
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
        titleGroup.setSpacing(true);

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
        btnSend.getStyle().set("background-color", "#0066cc");
        btnSend.addClickListener(e -> sendChatMessage());

        chatFooter.add(messageInput, btnSend);

        chatArea.add(chatHeader, chatScroller, chatFooter);
        chatContainer.add(chatSidebar, chatArea);
    }

    private void buildChatSidebar(List<Request> requests) {
        chatListContainer.removeAll();

        if (requests == null || requests.isEmpty()) {
            Span emptySpan = new Span("Sohbet bulunamadı.");
            emptySpan.getStyle().set("color", "#94a3b8").set("font-size", "0.85rem").set("padding", "10px");
            chatListContainer.add(emptySpan);
            return;
        }

        for (Request req : requests) {
            Div card = createChatSidebarCard(req);
            chatListContainer.add(card);
        }
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

        chatListContainer.getChildren().forEach(child -> child.getElement().getStyle().set("background-color", "transparent"));
        cardComponent.getStyle().set("background-color", "#e6f0fa");

        currentChatTitle.setText("#" + req.getId() + " " + req.getTitle());
        currentChatStatus.setText(req.getStatus() != null ? req.getStatus().toString() : "Bilinmiyor");
        btnGoToDetails.setVisible(true);
        btnToggleFilter.setVisible(true);
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
            Span emptyMsg = new Span("Henüz bu filtreye uygun yazışma bulunmuyor.");
            emptyMsg.getStyle().set("color", "#94a3b8").set("font-size", "0.9rem").set("margin", "auto");
            chatHistoryContainer.add(emptyMsg);
            return;
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");

        for (WorkflowLog log : logs) {
            Div bubbleWrapper = new Div();
            bubbleWrapper.setWidthFull();
            bubbleWrapper.getStyle().set("display", "flex").set("box-sizing", "border-box");

            Div bubble = new Div();
            bubble.getStyle()
                    .set("padding", "10px 14px")
                    .set("border-radius", "12px")
                    .set("max-width", "70%")
                    .set("background-color", "#ffffff")
                    .set("box-shadow", "0 1px 2px rgba(0,0,0,0.05)")
                    .set("display", "flex")
                    .set("flex-direction", "column")
                    .set("box-sizing", "border-box");

            User senderUser = log.getUser();
            Span sender = new Span((senderUser != null ? senderUser.getName() : "Sistem"));
            sender.getStyle().set("font-size", "0.75rem").set("font-weight", "bold").set("color", "#475569").set("margin-bottom", "4px");

            Span content = new Span(log.getLogText());
            content.getStyle().set("font-size", "0.85rem").set("word-break", "break-word");

            Span time = new Span(log.getCreatedAt() != null ? log.getCreatedAt().format(formatter) : "");
            time.getStyle().set("font-size", "0.65rem").set("color", "#94a3b8").set("align-self", "flex-end").set("margin-top", "4px");

            bubble.add(sender, content, time);

            boolean isMe = senderUser != null && currentUser != null && senderUser.getId().equals(currentUser.getId());
            bubbleWrapper.getStyle().set("justify-content", isMe ? "flex-end" : "flex-start");
            if (isMe) bubble.getStyle().set("background-color", "#e6f0fa"); 

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
        List<Request> filteredRequests = this.allMyChatRequests;

        if (filterText != null && !filterText.trim().isEmpty()) {
            String lower = filterText.toLowerCase();
            filteredRequests = filteredRequests.stream()
                    .filter(r -> (r.getTitle() != null && r.getTitle().toLowerCase().contains(lower)) ||
                            String.valueOf(r.getId()).contains(lower))
                    .toList();
        }
        buildChatSidebar(filteredRequests);
    }
}