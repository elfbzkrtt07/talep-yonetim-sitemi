package com.example.views.developer;

import com.example.base.ui.MainLayout;
import com.example.entities.Company;
import com.example.entities.Request;
import com.example.entities.User;
import com.example.entities.Workflow;
import com.example.services.CompanyService;
import com.example.services.WorkflowService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
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

@PageTitle("Tamamlanan İşlerim")
@Route(value = "dev/completed", layout = MainLayout.class)
public class DeveloperCompletedView extends VerticalLayout {

    private final WorkflowService workflowService;
    private final CompanyService companyService;

    private final Grid<Workflow> completedGrid = new Grid<>(Workflow.class, false);

    private final TextField searchField = new TextField("Genel Arama");
    private final ComboBox<Company> companyFilter = new ComboBox<>("Şirket / Hastane");
    private final ComboBox<String> financialImpactFilter = new ComboBox<>("Mali Etki");
    private final ComboBox<String> securityFilter = new ComboBox<>("KVKK / Güvenlik");

    private final DatePicker createdStartFilter = new DatePicker("Oluşturulma (Başlangıç)");
    private final DatePicker createdEndFilter = new DatePicker("Oluşturulma (Bitiş)");
    private final DatePicker deadlineStartFilter = new DatePicker("Deadline (Başlangıç)");
    private final DatePicker deadlineEndFilter = new DatePicker("Deadline (Bitiş)");

    private List<Workflow> allCompletedWorkflows = new ArrayList<>();

    public DeveloperCompletedView(WorkflowService workflowService, CompanyService companyService) {
        this.workflowService = workflowService;
        this.companyService = companyService;

        setSizeFull();
        setPadding(true);
        getStyle().set("background-color", "#f8fafc");

        VerticalLayout headerBanner = createHeaderBanner(
                "Tamamlanan İş Geçmişiniz", 
                "Başarıyla sonuçlandırıp teslim ettiğiniz talepler listelenmektedir."
        );

        setupFilterComponents();
        VerticalLayout filterCard = createFilterBar();

        completedGrid.addColumn(w -> w.getRequest().getId()).setHeader("Talep ID").setAutoWidth(true).setFlexGrow(0);
        
        completedGrid.addColumn(w -> {
            if (w.getRequest() != null && w.getRequest().getCustomer() != null && w.getRequest().getCustomer().getCompany() != null) {
                return w.getRequest().getCustomer().getCompany().getName();
            }
            return "Bireysel";
        }).setHeader("Şirket / Hastane").setAutoWidth(true);

        completedGrid.addColumn(w -> w.getRequest().getCustomer() != null ? w.getRequest().getCustomer().getName() : "Bireysel").setHeader("Müşteri").setAutoWidth(true);
        completedGrid.addColumn(w -> w.getRequest().getTitle()).setHeader("Başlık").setSortable(true).setFlexGrow(2);

        completedGrid.addColumn(w -> {
            if (w.getRequest() == null || w.getRequest().getCreatedAt() == null) return "-";
            return w.getRequest().getCreatedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
        }).setHeader("Oluşturulma Tarihi").setAutoWidth(true);

        completedGrid.addColumn(w -> {
            if (w.getRequest() == null || w.getRequest().getDeadline() == null) return "-";
            return w.getRequest().getDeadline().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
        }).setHeader("Target Deadline").setAutoWidth(true);

        completedGrid.addComponentColumn(w -> {
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

        completedGrid.addComponentColumn(workflow -> {
            Button inspectBtn = new Button("Detayları İncele", VaadinIcon.EYE.create());
            inspectBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            inspectBtn.addClickListener(e -> {
                UI.getCurrent().navigate("dev/edit/" + workflow.getRequest().getId());
            });
            return inspectBtn;
        }).setHeader("İşlemler").setAutoWidth(true);

        completedGrid.setSizeFull();
        completedGrid.getStyle()
                .set("border", "1px solid #cbd5e1")
                .set("border-radius", "8px")
                .set("box-shadow", "0 1px 3px rgba(0,0,0,0.05)");
        
        add(headerBanner, filterCard, completedGrid);

        loadGridData();
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
        companyFilter.setItems(companyService.getAllCompanies());
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

        List<Workflow> filtered = allCompletedWorkflows.stream().filter(w -> {
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

        completedGrid.setItems(filtered);
    }

    private void loadGridData() {
        User currentUser = (User) VaadinSession.getCurrent().getAttribute("user");
        if (currentUser != null) {
            allCompletedWorkflows = workflowService.getCompletedJobsForDeveloper(currentUser);
            applyFilters();
        }
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