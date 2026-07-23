package com.example.views.pm;

import com.example.base.ui.MainLayout;
import com.example.entities.Company;
import com.example.entities.Workflow;
import com.example.enums.WorkflowStatus;
import com.example.services.CompanyService;
import com.example.services.RequestService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Route(value = "pm/workflows", layout = MainLayout.class)
public class PMWorkflowsView extends VerticalLayout implements BeforeEnterObserver {

    private final RequestService requestService;
    private final CompanyService companyService;

    private final Grid<Workflow> workflowGrid = new Grid<>(Workflow.class, false);
    
    private List<Workflow> allWorkflows = new ArrayList<>();
    
    private final TextField searchField = new TextField("Genel Arama");
    private final ComboBox<Company> companyFilter = new ComboBox<>("Şirket / Hastane");
    private final ComboBox<WorkflowStatus> statusFilter = new ComboBox<>("Aşama Durumu");
    private final ComboBox<String> financialImpactFilter = new ComboBox<>("Mali Etki");
    private final ComboBox<String> securityFilter = new ComboBox<>("KVKK / Güvenlik");
    
    private final DatePicker createdStartFilter = new DatePicker("Oluşturulma (Başlangıç)");
    private final DatePicker createdEndFilter = new DatePicker("Oluşturulma (Bitiş)");
    private final DatePicker deadlineStartFilter = new DatePicker("Deadline (Başlangıç)");
    private final DatePicker deadlineEndFilter = new DatePicker("Deadline (Bitiş)");

    public PMWorkflowsView(RequestService requestService, CompanyService companyService) {
        this.requestService = requestService;
        this.companyService = companyService;

        setSizeFull();
        setPadding(true);
        getStyle().set("background-color", "#f8fafc");

        VerticalLayout headerBanner = createHeaderBanner("İş Akışları");

        Paragraph desc = new Paragraph("İş akışına dönüştürülen ve geliştirme ekipleri tarafından işlenen taleplerin güncel durumları.");
        desc.getStyle()
                .set("color", "#64748b")
                .set("text-align", "center")
                .set("margin", "0 auto 15px auto");

        setupFilterComponents();
        VerticalLayout filterCard = createFilterBar();

        workflowGrid.addColumn(w -> w.getRequest() != null ? w.getRequest().getId() : "-")
                .setHeader("ID").setWidth("70px").setFlexGrow(0);

        workflowGrid.addColumn(w -> {
            if (w.getRequest() != null && w.getRequest().getCustomer() != null && w.getRequest().getCustomer().getCompany() != null) {
                return w.getRequest().getCustomer().getCompany().getName();
            }
            return "Bireysel";
        }).setHeader("Şirket / Hastane").setSortable(true).setFlexGrow(1);

        workflowGrid.addColumn(w -> {
            if (w.getRequest() != null && w.getRequest().getCustomer() != null) {
                return w.getRequest().getCustomer().getName();
            }
            return "-";
        }).setHeader("Müşteri").setSortable(true).setFlexGrow(1);

        workflowGrid.addColumn(w -> w.getRequest() != null ? w.getRequest().getTitle() : "-")
                .setHeader("Başlık").setSortable(true).setFlexGrow(2);

        workflowGrid.addComponentColumn(w -> {
            HorizontalLayout devRow = new HorizontalLayout();
            devRow.setAlignItems(Alignment.CENTER);
            devRow.setSpacing(true);
            
            if (w.getCurrentAssignee() != null) {
                Span devIcon = new Span(VaadinIcon.USER.create());
                devIcon.getStyle().set("color", "#0284c7");
                Span devName = new Span(w.getCurrentAssignee().getName());
                devRow.add(devIcon, devName);
            } else {
                Span unassigned = new Span("Atanmadı");
                unassigned.getStyle().set("color", "#94a3b8").set("font-style", "italic");
                devRow.add(unassigned);
            }
            return devRow;
        }).setHeader("Atanan Yazılımcı").setSortable(true);

        workflowGrid.addColumn(w -> {
            if (w.getRequest() == null || w.getRequest().getCreatedAt() == null) return "-";
            return w.getRequest().getCreatedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
        }).setHeader("Oluşturulma Tarihi").setAutoWidth(true);

        workflowGrid.addColumn(w -> {
            if (w.getRequest() == null || w.getRequest().getDeadline() == null) return "-";
            return w.getRequest().getDeadline().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
        }).setHeader("Target Deadline").setAutoWidth(true);

        workflowGrid.addComponentColumn(w -> {
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

        workflowGrid.addColumn(workflow -> {
            if (workflow.getStatus() != null) {
                return workflow.getStatus().toString();
            }
            return "-";
        }).setHeader("Aşama Durumu").setSortable(true).setAutoWidth(true);

        workflowGrid.addComponentColumn(workflow -> {
            Button inspectBtn = new Button("İncele", VaadinIcon.EYE.create());
            inspectBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            inspectBtn.addClickListener(e -> {
                if (workflow.getRequest() != null) {
                    UI.getCurrent().navigate("pm/inspect/" + workflow.getRequest().getId());
                }
            });
            return inspectBtn;
        }).setHeader("İşlem").setWidth("120px").setFlexGrow(0);

        workflowGrid.setWidth("calc(100% - 40px)");
        workflowGrid.getStyle()
                .set("margin", "0 auto")
                .set("background-color", "#ffffff")
                .set("border-radius", "12px")
                .set("border", "1px solid #e2e8f0")
                .set("box-shadow", "0 1px 3px rgba(0,0,0,0.02)");

        workflowGrid.setPartNameGenerator(workflow -> {
            if (workflow.getStatus() != null) {
                switch (workflow.getStatus()) {
                    case COMPLETED:
                        return "row-completed";
                    case SENT_BACK_TO_PM:
                        return "row-sent-back-pm";
                    case SENT_BACK_TO_SM:
                        return "row-sent-back-sm";
                    case DEVELOPMENT:
                        return "row-development";
                    default:
                        return "row-default";
                }
            }
            return "row-default";
        });

        add(headerBanner, desc, filterCard, workflowGrid);
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
        companyFilter.setClearButtonVisible(true);
        companyFilter.addValueChangeListener(e -> filterGridData());
        companyFilter.setWidthFull();

        statusFilter.setPlaceholder("Tüm Aşamalar");
        statusFilter.setItems(WorkflowStatus.values());
        statusFilter.setClearButtonVisible(true);
        statusFilter.addValueChangeListener(e -> filterGridData());
        statusFilter.setWidthFull();

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

        HorizontalLayout mainRow = new HorizontalLayout(searchField, companyFilter, statusFilter, financialImpactFilter, securityFilter);
        mainRow.setWidthFull();
        mainRow.setSpacing(true);

        HorizontalLayout datesRow = new HorizontalLayout(createdStartFilter, createdEndFilter, deadlineStartFilter, deadlineEndFilter);
        datesRow.setWidthFull();
        datesRow.setSpacing(true);

        Button clearFiltersBtn = new Button("Filtreleri Temizle", VaadinIcon.CLOSE_CIRCLE.create(), e -> clearAllFilters());
        clearFiltersBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);

        HorizontalLayout clearRow = new HorizontalLayout(clearFiltersBtn);
        clearRow.setWidthFull();
        clearRow.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

        filterFieldsContainer.add(mainRow, datesRow, clearRow);

        Span filterTitle = new Span("Filtre Seçenekleri");
        filterTitle.getStyle()
                .set("font-weight", "600")
                .set("font-size", "0.9rem")
                .set("color", "#475569");

        Button toggleBtn = new Button("Filtreleri Gizle", VaadinIcon.CHEVRON_UP.create());
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
        statusFilter.clear();
        financialImpactFilter.clear();
        securityFilter.clear();
        createdStartFilter.clear();
        createdEndFilter.clear();
        deadlineStartFilter.clear();
        deadlineEndFilter.clear();
        filterGridData();
    }

    private void filterGridData() {
        String searchTerm = searchField.getValue() != null ? searchField.getValue().trim().toLowerCase() : "";
        Company selectedCompany = companyFilter.getValue();
        WorkflowStatus selectedStatus = statusFilter.getValue();
        String selectedFinancial = financialImpactFilter.getValue();
        String selectedSec = securityFilter.getValue();

        LocalDate createdStart = createdStartFilter.getValue();
        LocalDate createdEnd = createdEndFilter.getValue();
        LocalDate deadlineStart = deadlineStartFilter.getValue();
        LocalDate deadlineEnd = deadlineEndFilter.getValue();

        List<Workflow> filteredList = allWorkflows.stream().filter(w -> {
            boolean matchesSearch = searchTerm.isEmpty()
                    || (w.getRequest() != null && String.valueOf(w.getRequest().getId()).contains(searchTerm))
                    || (w.getRequest() != null && w.getRequest().getTitle() != null && w.getRequest().getTitle().toLowerCase().contains(searchTerm))
                    || (w.getRequest() != null && w.getRequest().getDescription() != null && w.getRequest().getDescription().toLowerCase().contains(searchTerm))
                    || (w.getRequest() != null && w.getRequest().getCustomer() != null && w.getRequest().getCustomer().getName() != null && w.getRequest().getCustomer().getName().toLowerCase().contains(searchTerm));

            boolean matchesCompany = (selectedCompany == null)
                    || (w.getRequest() != null && w.getRequest().getCustomer() != null && w.getRequest().getCustomer().getCompany() != null && w.getRequest().getCustomer().getCompany().getId().equals(selectedCompany.getId()));

            boolean matchesStatus = (selectedStatus == null) || (w.getStatus() == selectedStatus);

            boolean matchesFinancial = (selectedFinancial == null)
                    || (w.getRequest() != null && w.getRequest().getFinancialImpact() != null && w.getRequest().getFinancialImpact().equalsIgnoreCase(selectedFinancial));

            boolean matchesSecurity = true;
            if ("Sadece Güvenlik / KVKK Riski Olanlar".equals(selectedSec)) {
                matchesSecurity = w.getRequest() != null && Boolean.TRUE.equals(w.getRequest().getIsSecurityRisk());
            } else if ("Risksiz Olanlar".equals(selectedSec)) {
                matchesSecurity = w.getRequest() != null && !Boolean.TRUE.equals(w.getRequest().getIsSecurityRisk());
            }

            boolean matchesCreated = true;
            if (w.getRequest() != null && w.getRequest().getCreatedAt() != null) {
                LocalDate createdDate = w.getRequest().getCreatedAt().toLocalDate();
                if (createdStart != null && createdDate.isBefore(createdStart)) matchesCreated = false;
                if (createdEnd != null && createdDate.isAfter(createdEnd)) matchesCreated = false;
            } else if (createdStart != null || createdEnd != null) {
                matchesCreated = false;
            }

            boolean matchesDeadline = true;
            if (w.getRequest() != null && w.getRequest().getDeadline() != null) {
                LocalDate deadline = w.getRequest().getDeadline();
                if (deadlineStart != null && deadline.isBefore(deadlineStart)) matchesDeadline = false;
                if (deadlineEnd != null && deadline.isAfter(deadlineEnd)) matchesDeadline = false;
            } else if (deadlineStart != null || deadlineEnd != null) {
                matchesDeadline = false;
            }

            return matchesSearch && matchesCompany && matchesStatus && matchesFinancial && matchesSecurity && matchesCreated && matchesDeadline;
        }).toList();

        workflowGrid.setItems(filteredList);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        companyFilter.setItems(companyService.getAllCompanies());
        allWorkflows = requestService.getAllWorkflows(); 
        filterGridData();
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