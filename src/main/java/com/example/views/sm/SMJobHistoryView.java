package com.example.views.sm;

import com.example.base.ui.MainLayout;
import com.example.entities.Company;
import com.example.entities.User;
import com.example.entities.Workflow;
import com.example.enums.UserRole;
import com.example.enums.WorkflowStatus;
import com.example.repositories.UserRepository;
import com.example.services.CompanyService;
import com.example.services.RequestService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@PageTitle("Departman İş Geçmişi")
@Route(value = "sm/history", layout = MainLayout.class)
public class SMJobHistoryView extends VerticalLayout {

    private final RequestService requestService;
    private final UserRepository userRepository;
    private final CompanyService companyService;

    private final Grid<Workflow> historyGrid = new Grid<>(Workflow.class, false);

    private final TextField searchField = new TextField("Genel Arama");
    private final ComboBox<Company> companyFilter = new ComboBox<>("Şirket / Hastane");
    private final ComboBox<WorkflowStatus> statusFilter = new ComboBox<>("Aşama Durumu");
    private final ComboBox<User> developerFilter = new ComboBox<>("Atanan Yazılımcı");
    private final ComboBox<String> financialImpactFilter = new ComboBox<>("Mali Etki");
    private final ComboBox<String> securityFilter = new ComboBox<>("KVKK / Güvenlik");

    private final DatePicker createdStartFilter = new DatePicker("Oluşturulma (Başlangıç)");
    private final DatePicker createdEndFilter = new DatePicker("Oluşturulma (Bitiş)");
    private final DatePicker deadlineStartFilter = new DatePicker("Deadline (Başlangıç)");
    private final DatePicker deadlineEndFilter = new DatePicker("Deadline (Bitiş)");

    private List<Workflow> allDepartmentWorkflows = new ArrayList<>();
    private User currentSm;

    public SMJobHistoryView(RequestService requestService, UserRepository userRepository, CompanyService companyService) {
        this.requestService = requestService;
        this.userRepository = userRepository;
        this.companyService = companyService;

        this.currentSm = (User) VaadinSession.getCurrent().getAttribute("user");

        setSizeFull();
        setPadding(true);
        getStyle().set("background-color", "#f8fafc");

        VerticalLayout headerBanner = createHeaderBanner(
                "Departman İş Geçmişi", 
                "Departmanınıza ait aktif, tamamlanan veya iade edilen tüm işlerin takibi ve filtresi."
        );
        add(headerBanner);

        setupFilterComponents();
        VerticalLayout filterCard = createFilterBar();

        historyGrid.addColumn(w -> w.getRequest() != null ? w.getRequest().getId() : "-")
                .setHeader("Talep ID").setAutoWidth(true).setFlexGrow(0);

        historyGrid.addColumn(w -> {
            if (w.getRequest() != null && w.getRequest().getCustomer() != null && w.getRequest().getCustomer().getCompany() != null) {
                return w.getRequest().getCustomer().getCompany().getName();
            }
            return "Bireysel";
        }).setHeader("Şirket / Hastane").setAutoWidth(true);
        
        historyGrid.addColumn(w -> (w.getRequest() != null && w.getRequest().getCustomer() != null) ? w.getRequest().getCustomer().getName() : "-")
                .setHeader("Müşteri").setAutoWidth(true);
        
        historyGrid.addColumn(w -> w.getRequest() != null ? w.getRequest().getTitle() : "-")
                .setHeader("Başlık").setFlexGrow(2);

        historyGrid.addColumn(w -> {
            if (w.getRequest() == null || w.getRequest().getCreatedAt() == null) return "-";
            return w.getRequest().getCreatedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
        }).setHeader("Oluşturulma Tarihi").setAutoWidth(true);

        historyGrid.addColumn(w -> {
            if (w.getRequest() == null || w.getRequest().getDeadline() == null) return "-";
            return w.getRequest().getDeadline().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
        }).setHeader("Target Deadline").setAutoWidth(true);

        historyGrid.addComponentColumn(w -> {
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

        historyGrid.addColumn(w -> w.getStatus() != null ? w.getStatus().toString() : "-")
                .setHeader("Aşama Durumu").setAutoWidth(true);

        historyGrid.addColumn(w -> w.getCurrentAssignee() != null ? w.getCurrentAssignee().getName() : "Atanmamış")
                .setHeader("Atanan Yazılımcı").setAutoWidth(true);

        historyGrid.addComponentColumn(workflow -> {
            Button inspectBtn = new Button("Detay", VaadinIcon.EYE.create());
            inspectBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            inspectBtn.addClickListener(e -> {
                if (workflow.getRequest() != null) {
                    UI.getCurrent().navigate("sm/evaluate/" + workflow.getRequest().getId());
                }
            });
            return inspectBtn;
        }).setHeader("İşlem").setAutoWidth(true);

        historyGrid.setSizeFull();
        historyGrid.getStyle()
                .set("border", "1px solid #cbd5e1")
                .set("border-radius", "8px")
                .set("box-shadow", "0 1px 3px rgba(0,0,0,0.05)");

        add(filterCard, historyGrid);

        loadDataFromServices();
    }

    private void setupFilterComponents() {
        searchField.setPlaceholder("ID, Başlık, Detay veya Müşteri...");
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.addValueChangeListener(e -> filterGridData());
        searchField.setClearButtonVisible(true);
        searchField.setWidthFull();

        companyFilter.setPlaceholder("Tüm Şirketler");
        companyFilter.setItemLabelGenerator(Company::getName);
        companyFilter.setItems(companyService.getAllCompanies());
        companyFilter.setClearButtonVisible(true);
        companyFilter.addValueChangeListener(e -> filterGridData());
        companyFilter.setWidthFull();

        statusFilter.setPlaceholder("Tüm Durumlar");
        statusFilter.setItems(WorkflowStatus.values());
        statusFilter.setClearButtonVisible(true);
        statusFilter.addValueChangeListener(e -> filterGridData());
        statusFilter.setWidthFull();

        developerFilter.setPlaceholder("Tüm Yazılımcılar");
        developerFilter.setClearButtonVisible(true);
        developerFilter.setItemLabelGenerator(User::getName);
        developerFilter.addValueChangeListener(e -> filterGridData());
        developerFilter.setWidthFull();

        if (currentSm != null && currentSm.getDepartment() != null) {
            List<User> departmentDevs = userRepository.findAll().stream()
                    .filter(u -> u.getRole() == UserRole.DEVELOPER
                            && u.getDepartment() != null 
                            && u.getDepartment().getId().equals(currentSm.getDepartment().getId()))
                    .toList();
            developerFilter.setItems(departmentDevs);
        }

        financialImpactFilter.setPlaceholder("Tümü");
        financialImpactFilter.setItems("Etkisi Yok", "Düşük Zarar", "Orta Düzey Zarar", "Kritik / İş Durdurucu Zarar");
        financialImpactFilter.setClearButtonVisible(true);
        financialImpactFilter.addValueChangeListener(e -> filterGridData());
        financialImpactFilter.setWidthFull();

        securityFilter.setPlaceholder("Tümü");
        securityFilter.setItems("Sadece Güvenlik / KVKK Riski Olanlar", "Risksiz Olanlar");
        securityFilter.setClearButtonVisible(true);
        securityFilter.addValueChangeListener(e -> filterGridData());
        securityFilter.setWidthFull();

        createdStartFilter.addValueChangeListener(e -> filterGridData());
        createdStartFilter.setWidthFull();
        createdEndFilter.addValueChangeListener(e -> filterGridData());
        createdEndFilter.setWidthFull();

        deadlineStartFilter.addValueChangeListener(e -> filterGridData());
        deadlineStartFilter.setWidthFull();
        deadlineEndFilter.addValueChangeListener(e -> filterGridData());
        deadlineEndFilter.setWidthFull();
    }

    private VerticalLayout createFilterBar() {
        VerticalLayout filterFieldsContainer = new VerticalLayout();
        filterFieldsContainer.setPadding(false);
        filterFieldsContainer.setSpacing(true);

        filterFieldsContainer.setVisible(false);

        HorizontalLayout row1 = new HorizontalLayout(searchField, companyFilter, statusFilter, developerFilter);
        row1.setWidthFull();
        row1.setSpacing(true);

        HorizontalLayout row2 = new HorizontalLayout(financialImpactFilter, securityFilter, createdStartFilter, createdEndFilter);
        row2.setWidthFull();
        row2.setSpacing(true);

        HorizontalLayout row3 = new HorizontalLayout(deadlineStartFilter, deadlineEndFilter);
        row3.setWidthFull();
        row3.setSpacing(true);

        Button clearFiltersBtn = new Button("Filtreleri Temizle", VaadinIcon.CLOSE_CIRCLE.create(), e -> clearAllFilters());
        clearFiltersBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);

        HorizontalLayout clearRow = new HorizontalLayout(clearFiltersBtn);
        clearRow.setWidthFull();
        clearRow.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

        filterFieldsContainer.add(row1, row2, row3, clearRow);

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
        filterHeaderRow.setAlignItems(FlexComponent.Alignment.CENTER);
        filterHeaderRow.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

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
        statusFilter.clear();
        developerFilter.clear();
        financialImpactFilter.clear();
        securityFilter.clear();
        createdStartFilter.clear();
        createdEndFilter.clear();
        deadlineStartFilter.clear();
        deadlineEndFilter.clear();
        filterGridData();
    }

    private void loadDataFromServices() {
        if (currentSm == null || currentSm.getDepartment() == null) return;
        Long deptId = currentSm.getDepartment().getId();

        allDepartmentWorkflows = requestService.getAllWorkflows().stream()
                .filter(w -> {
                    if (w.getRequest() == null) return false;
                    try {
                        var prio = requestService.getAllPrioritizationsSorted().stream()
                                .filter(p -> p.getRequest().getId().equals(w.getRequest().getId()))
                                .findFirst().orElse(null);
                        return prio != null && prio.getDepartment() != null && prio.getDepartment().getId().equals(deptId);
                    } catch (Exception ex) {
                        return false;
                    }
                })
                .toList();

        filterGridData();
    }

    private void filterGridData() {
        String searchTxt = searchField.getValue() != null ? searchField.getValue().trim().toLowerCase() : "";
        Company selectedCompany = companyFilter.getValue();
        WorkflowStatus selectedStatus = statusFilter.getValue();
        User selectedDev = developerFilter.getValue();
        String selectedFinancial = financialImpactFilter.getValue();
        String selectedSec = securityFilter.getValue();

        LocalDate createdStart = createdStartFilter.getValue();
        LocalDate createdEnd = createdEndFilter.getValue();
        LocalDate deadlineStart = deadlineStartFilter.getValue();
        LocalDate deadlineEnd = deadlineEndFilter.getValue();

        List<Workflow> filtered = allDepartmentWorkflows.stream()
                .filter(w -> {
                    if (!searchTxt.isEmpty()) {
                        String idStr = w.getRequest() != null ? String.valueOf(w.getRequest().getId()) : "";
                        String titleStr = (w.getRequest() != null && w.getRequest().getTitle() != null) ? w.getRequest().getTitle().toLowerCase() : "";
                        String descStr = (w.getRequest() != null && w.getRequest().getDescription() != null) ? w.getRequest().getDescription().toLowerCase() : "";
                        String customerStr = (w.getRequest() != null && w.getRequest().getCustomer() != null && w.getRequest().getCustomer().getName() != null) ? w.getRequest().getCustomer().getName().toLowerCase() : "";

                        if (!idStr.contains(searchTxt) && !titleStr.contains(searchTxt) && !descStr.contains(searchTxt) && !customerStr.contains(searchTxt)) {
                            return false;
                        }
                    }

                    if (selectedCompany != null) {
                        if (w.getRequest() == null || w.getRequest().getCustomer() == null || w.getRequest().getCustomer().getCompany() == null ||
                                !w.getRequest().getCustomer().getCompany().getId().equals(selectedCompany.getId())) {
                            return false;
                        }
                    }

                    if (selectedStatus != null && w.getStatus() != selectedStatus) {
                        return false;
                    }

                    if (selectedDev != null) {
                        if (w.getCurrentAssignee() == null || !w.getCurrentAssignee().getId().equals(selectedDev.getId())) {
                            return false;
                        }
                    }

                    if (selectedFinancial != null) {
                        if (w.getRequest() == null || w.getRequest().getFinancialImpact() == null || !w.getRequest().getFinancialImpact().equalsIgnoreCase(selectedFinancial)) {
                            return false;
                        }
                    }

                    if ("Sadece Güvenlik / KVKK Riski Olanlar".equals(selectedSec)) {
                        if (w.getRequest() == null || !Boolean.TRUE.equals(w.getRequest().getIsSecurityRisk())) return false;
                    } else if ("Risksiz Olanlar".equals(selectedSec)) {
                        if (w.getRequest() != null && Boolean.TRUE.equals(w.getRequest().getIsSecurityRisk())) return false;
                    }

                    if (w.getRequest() != null && w.getRequest().getCreatedAt() != null) {
                        LocalDate createdDate = w.getRequest().getCreatedAt().toLocalDate();
                        if (createdStart != null && createdDate.isBefore(createdStart)) return false;
                        if (createdEnd != null && createdDate.isAfter(createdEnd)) return false;
                    } else if (createdStart != null || createdEnd != null) {
                        return false;
                    }

                    if (w.getRequest() != null && w.getRequest().getDeadline() != null) {
                        LocalDate deadline = w.getRequest().getDeadline();
                        if (deadlineStart != null && deadline.isBefore(deadlineStart)) return false;
                        if (deadlineEnd != null && deadline.isAfter(deadlineEnd)) return false;
                    } else if (deadlineStart != null || deadlineEnd != null) {
                        return false;
                    }

                    return true;
                })
                .toList();

        historyGrid.setItems(filtered);
    }

    private VerticalLayout createHeaderBanner(String titleText, String subtitleText) {
        VerticalLayout bannerLayout = new VerticalLayout();
        bannerLayout.setWidthFull();
        bannerLayout.setAlignItems(Alignment.CENTER); 
        bannerLayout.setJustifyContentMode(JustifyContentMode.CENTER);
        bannerLayout.setPadding(false);
        bannerLayout.setSpacing(true);
        bannerLayout.getStyle()
                .set("margin-top", "20px")
                .set("margin-bottom", "10px");

        H2 title = new H2(titleText);
        title.getStyle()
                .set("margin", "0")
                .set("font-size", "2.2rem") 
                .set("font-weight", "900") 
                .set("color", "#0f172a")
                .set("text-align", "center");
        bannerLayout.add(title);

        if (subtitleText != null && !subtitleText.isEmpty()) {
            Span subtitle = new Span(subtitleText);
            subtitle.getStyle()
                    .set("margin-top", "4px") 
                    .set("font-size", "0.9rem")
                    .set("color", "#64748b")
                    .set("text-align", "center");
            bannerLayout.add(subtitle);
        }

        return bannerLayout;
    }
}