package com.example.views.pm;

import com.example.base.ui.MainLayout;
import com.example.entities.Company;
import com.example.entities.Department;
import com.example.entities.Prioritization;
import com.example.enums.WorkflowStatus;
import com.example.services.CompanyService;
import com.example.services.DepartmentService;
import com.example.services.PrioritizationService;
import com.example.services.RequestService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import jakarta.persistence.EntityManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Route(value = "pm/prioritizations", layout = MainLayout.class)
public class PMPrioritizationsView extends VerticalLayout implements BeforeEnterObserver {

    private final PrioritizationService prioritizationService;
    private final RequestService requestService;
    private final CompanyService companyService;
    private final DepartmentService departmentService;
    private final EntityManager entityManager;
    private final TransactionTemplate transactionTemplate;

    private final Grid<Prioritization> prioritizationGrid = new Grid<>(Prioritization.class, false);
    private List<Prioritization> allPrioritizations = new ArrayList<>();

    private final TextField searchField = new TextField("Genel Arama");
    private final ComboBox<Company> companyFilter = new ComboBox<>("Şirket / Hastane");
    private final ComboBox<Department> departmentFilter = new ComboBox<>("Departman");
    private final ComboBox<String> financialImpactFilter = new ComboBox<>("Mali Etki");
    private final ComboBox<String> securityFilter = new ComboBox<>("KVKK / Güvenlik");

    private final DatePicker createdStartFilter = new DatePicker("Oluşturulma (Başlangıç)");
    private final DatePicker createdEndFilter = new DatePicker("Oluşturulma (Bitiş)");
    private final DatePicker deadlineStartFilter = new DatePicker("Deadline (Başlangıç)");
    private final DatePicker deadlineEndFilter = new DatePicker("Deadline (Bitiş)");

    public PMPrioritizationsView(PrioritizationService prioritizationService, 
                                 RequestService requestService, 
                                 CompanyService companyService,
                                 DepartmentService departmentService,
                                 EntityManager entityManager,
                                 TransactionTemplate transactionTemplate) {
        this.prioritizationService = prioritizationService;
        this.requestService = requestService;
        this.companyService = companyService;
        this.departmentService = departmentService;
        this.entityManager = entityManager;
        this.transactionTemplate = transactionTemplate;

        setSizeFull();
        setPadding(true);
        getStyle().set("background-color", "#f8fafc");

        VerticalLayout headerBanner = createHeaderBanner("Önceliklendirme Havuzu");
        
        Paragraph desc = new Paragraph("Onaylanan talepler, hesaplanan öncelik skorlarına göre yüksekten düşüğe doğru sıralanmıştır.");
        desc.getStyle()
                .set("color", "#64748b")
                .set("text-align", "center")
                .set("margin", "0 auto 15px auto");

        setupFilterComponents();
        VerticalLayout filterCard = createFilterBar();

        prioritizationGrid.addColumn(p -> p.getId() != null ? p.getId() : "-")
                .setHeader("ID").setWidth("70px").setFlexGrow(0);
        
        prioritizationGrid.addColumn(p -> {
            try {
                if (p.getRequest() != null && p.getRequest().getCustomer() != null && p.getRequest().getCustomer().getCompany() != null) {
                    return p.getRequest().getCustomer().getCompany().getName();
                }
            } catch (Exception ignored) {}
            return "Bireysel";
        }).setHeader("Şirket / Hastane").setSortable(true).setFlexGrow(1);

        prioritizationGrid.addColumn(p -> {
            try {
                if (p.getRequest() != null && p.getRequest().getCustomer() != null) {
                    return p.getRequest().getCustomer().getName();
                }
            } catch (Exception ex) {
                return "Müşteri Bilgisi Yüklenemedi"; 
            }
            return "-";
        }).setHeader("Müşteri").setSortable(true).setFlexGrow(1);
        
        prioritizationGrid.addColumn(p -> {
            try {
                if (p.getRequest() != null) {
                    return p.getRequest().getTitle();
                }
            } catch (Exception ex) {
                return "Başlık Yüklenemedi";
            }
            return "-";
        }).setHeader("Başlık").setSortable(true).setFlexGrow(2);
        
        prioritizationGrid.addColumn(prioritization -> prioritization.getDepartment() != null ? prioritization.getDepartment().getName() : "").setHeader("Departman").setSortable(true);
        prioritizationGrid.addColumn(Prioritization::getTaskType).setHeader("İş Tipi").setSortable(true);
        
        prioritizationGrid.addColumn(p -> {
            if (p.getRequest() == null || p.getRequest().getCreatedAt() == null) return "-";
            return p.getRequest().getCreatedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
        }).setHeader("Oluşturulma Tarihi").setAutoWidth(true);

        prioritizationGrid.addColumn(p -> {
            if (p.getRequest() == null || p.getRequest().getDeadline() == null) return "-";
            return p.getRequest().getDeadline().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
        }).setHeader("Target Deadline").setAutoWidth(true);

        prioritizationGrid.addComponentColumn(p -> {
            HorizontalLayout flags = new HorizontalLayout();
            flags.setSpacing(true);

            if (p.getRequest() != null) {
                if (Boolean.TRUE.equals(p.getRequest().getIsSecurityRisk())) {
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

                if (p.getRequest().getFinancialImpact() != null && !p.getRequest().getFinancialImpact().isBlank() && !"Etkisi Yok".equalsIgnoreCase(p.getRequest().getFinancialImpact())) {
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

        prioritizationGrid.addColumn(Prioritization::getPriorityScore)
                .setHeader("Skor")
                .setSortable(true)
                .setWidth("90px").setFlexGrow(0);

        prioritizationGrid.addComponentColumn(prioritization -> {
            HorizontalLayout actionWrapper = new HorizontalLayout();
            actionWrapper.setSpacing(true);

            Button detailBtn = new Button("İncele", VaadinIcon.EYE.create());
            detailBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
            detailBtn.addClickListener(e -> {
                getUI().ifPresent(ui -> ui.navigate("pm/inspect/" + prioritization.getId()));
            });

            Button convertBtn = new Button("İş Akışına Çevir", VaadinIcon.CONNECT.create());
            convertBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_SMALL);
            
            convertBtn.addClickListener(e -> {
                transactionTemplate.execute(status -> {
                    requestService.convertPrioritizationToWorkflow(prioritization);

                    entityManager.createQuery(
                        "UPDATE Workflow w SET w.status = :newStatus WHERE w.request.id = :reqId")
                        .setParameter("newStatus", WorkflowStatus.APPROVED_BY_PM)
                        .setParameter("reqId", prioritization.getId())
                        .executeUpdate();

                    return null;
                });

                Notification.show("Talep başarıyla iş akışına dönüştürüldü.")
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                refreshGridData();
            });

            actionWrapper.add(detailBtn, convertBtn);
            return actionWrapper;
        }).setHeader("İşlemler").setWidth("260px").setFlexGrow(0);

        prioritizationGrid.setWidth("calc(100% - 40px)");
        prioritizationGrid.getStyle()
                .set("margin", "0 auto")
                .set("background-color", "#ffffff")
                .set("border-radius", "12px")
                .set("border", "1px solid #e2e8f0")
                .set("box-shadow", "0 1px 3px rgba(0,0,0,0.02)");

        add(headerBanner, desc, filterCard, prioritizationGrid);
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

        departmentFilter.setPlaceholder("Tüm Departmanlar");
        departmentFilter.setItemLabelGenerator(Department::getName);
        departmentFilter.setClearButtonVisible(true);
        departmentFilter.addValueChangeListener(e -> applyFilters());
        departmentFilter.setWidthFull();

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

        HorizontalLayout row1 = new HorizontalLayout(searchField, companyFilter, departmentFilter);
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
        filterCard.setWidth("calc(100% - 40px)");
        filterCard.setPadding(true);
        filterCard.setSpacing(true);
        filterCard.getStyle()
                .set("background", "#ffffff")
                .set("border-radius", "12px")
                .set("margin", "0 auto 16px auto")
                .set("box-shadow", "0 1px 3px rgba(0,0,0,0.04)")
                .set("border", "1px solid #e2e8f0");

        return filterCard;
    }

    private void clearAllFilters() {
        searchField.clear();
        companyFilter.clear();
        departmentFilter.clear();
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
        Department selectedDepartment = departmentFilter.getValue();
        String selectedFinancial = financialImpactFilter.getValue();
        String selectedSec = securityFilter.getValue();

        LocalDate createdStart = createdStartFilter.getValue();
        LocalDate createdEnd = createdEndFilter.getValue();
        LocalDate deadlineStart = deadlineStartFilter.getValue();
        LocalDate deadlineEnd = deadlineEndFilter.getValue();

        List<Prioritization> filtered = allPrioritizations.stream().filter(p -> {
            boolean matchesQuery = query.isEmpty() ||
                    (p.getId() != null && String.valueOf(p.getId()).contains(query)) ||
                    (p.getRequest() != null && p.getRequest().getTitle() != null && p.getRequest().getTitle().toLowerCase().contains(query)) ||
                    (p.getRequest() != null && p.getRequest().getDescription() != null && p.getRequest().getDescription().toLowerCase().contains(query)) ||
                    (p.getRequest() != null && p.getRequest().getCustomer() != null && p.getRequest().getCustomer().getName() != null && p.getRequest().getCustomer().getName().toLowerCase().contains(query));

            boolean matchesCompany = (selectedCompany == null) ||
                    (p.getRequest() != null && p.getRequest().getCustomer() != null && p.getRequest().getCustomer().getCompany() != null && p.getRequest().getCustomer().getCompany().getId().equals(selectedCompany.getId()));

            boolean matchesDepartment = (selectedDepartment == null) ||
                    (p.getDepartment() != null && p.getDepartment().getId().equals(selectedDepartment.getId()));

            boolean matchesFinancial = (selectedFinancial == null) ||
                    (p.getRequest() != null && p.getRequest().getFinancialImpact() != null && p.getRequest().getFinancialImpact().equalsIgnoreCase(selectedFinancial));

            boolean matchesSecurity = true;
            if ("Sadece Güvenlik / KVKK Riski Olanlar".equals(selectedSec)) {
                matchesSecurity = p.getRequest() != null && Boolean.TRUE.equals(p.getRequest().getIsSecurityRisk());
            } else if ("Risksiz Olanlar".equals(selectedSec)) {
                matchesSecurity = p.getRequest() != null && !Boolean.TRUE.equals(p.getRequest().getIsSecurityRisk());
            }

            boolean matchesCreated = true;
            if (p.getRequest() != null && p.getRequest().getCreatedAt() != null) {
                LocalDate createdDate = p.getRequest().getCreatedAt().toLocalDate();
                if (createdStart != null && createdDate.isBefore(createdStart)) matchesCreated = false;
                if (createdEnd != null && createdDate.isAfter(createdEnd)) matchesCreated = false;
            } else if (createdStart != null || createdEnd != null) {
                matchesCreated = false;
            }

            boolean matchesDeadline = true;
            if (p.getRequest() != null && p.getRequest().getDeadline() != null) {
                LocalDate deadline = p.getRequest().getDeadline();
                if (deadlineStart != null && deadline.isBefore(deadlineStart)) matchesDeadline = false;
                if (deadlineEnd != null && deadline.isAfter(deadlineEnd)) matchesDeadline = false;
            } else if (deadlineStart != null || deadlineEnd != null) {
                matchesDeadline = false;
            }

            return matchesQuery && matchesCompany && matchesDepartment && matchesFinancial && matchesSecurity && matchesCreated && matchesDeadline;
        }).toList();

        prioritizationGrid.setItems(filtered);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        companyFilter.setItems(companyService.getAllCompanies());
        departmentFilter.setItems(departmentService.getAllDepartments());
        refreshGridData();
    }

    private void refreshGridData() {
        allPrioritizations = prioritizationService.getAllUnconvertedpPrioritizations();
        allPrioritizations.sort((p1, p2) -> p2.getPriorityScore().compareTo(p1.getPriorityScore()));
        applyFilters();
    }

    private VerticalLayout createHeaderBanner(String titleText) {
        VerticalLayout bannerLayout = new VerticalLayout();
        bannerLayout.setWidthFull();
        bannerLayout.setAlignItems(Alignment.CENTER); 
        bannerLayout.setJustifyContentMode(JustifyContentMode.CENTER);
        bannerLayout.setPadding(false);
        bannerLayout.getStyle()
                .set("margin-top", "20px")
                .set("margin-bottom", "5px");

        H2 title = new H2(titleText);
        title.getStyle()
                .set("margin", "0")
                .set("font-size", "2.2rem") 
                .set("font-weight", "900") 
                .set("color", "#0f172a")
                .set("text-align", "center");

        bannerLayout.add(title);
        return bannerLayout;
    }
}