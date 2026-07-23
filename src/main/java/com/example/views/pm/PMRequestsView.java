package com.example.views.pm;

import com.example.base.ui.MainLayout;
import com.example.entities.Company;
import com.example.entities.Request;
import com.example.enums.RequestStatus;
import com.example.services.CompanyService;
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

@Route(value = "pm/requests", layout = MainLayout.class)
public class PMRequestsView extends VerticalLayout implements BeforeEnterObserver {

    private final RequestService requestService;
    private final CompanyService companyService;

    private final Grid<Request> requestGrid = new Grid<>(Request.class, false);

    private final TextField searchField = new TextField("Genel Arama");
    private final ComboBox<Company> companyFilter = new ComboBox<>("Şirket / Hastane");
    private final ComboBox<String> financialImpactFilter = new ComboBox<>("Mali Etki");
    private final ComboBox<String> securityFilter = new ComboBox<>("KVKK / Güvenlik");
    
    private final DatePicker createdStartFilter = new DatePicker("Oluşturulma (Başlangıç)");
    private final DatePicker createdEndFilter = new DatePicker("Oluşturulma (Bitiş)");
    private final DatePicker deadlineStartFilter = new DatePicker("Deadline (Başlangıç)");
    private final DatePicker deadlineEndFilter = new DatePicker("Deadline (Bitiş)");

    private List<Request> allPendingRequests = new ArrayList<>();

    public PMRequestsView(RequestService requestService, CompanyService companyService) {
        this.requestService = requestService;
        this.companyService = companyService;

        setSizeFull();
        setPadding(true);
        getStyle().set("background-color", "#f8fafc");

        VerticalLayout headerBanner = createHeaderBanner("Gelen Talepler");
        
        Paragraph desc = new Paragraph("Gözden geçirilip öncelik havuzuna veya iş akışına aktarılması beklenen talepler.");
        desc.getStyle()
                .set("color", "#64748b")
                .set("text-align", "center")
                .set("margin", "0 auto 15px auto");

        setupFilterComponents();
        VerticalLayout filterCard = createFilterBar();

        requestGrid.addColumn(Request::getId).setHeader("ID").setWidth("70px").setFlexGrow(0);

        requestGrid.addColumn(request -> {
            if (request.getCustomer() != null && request.getCustomer().getCompany() != null) {
                return request.getCustomer().getCompany().getName();
            }
            return "Bireysel";
        }).setHeader("Şirket / Hastane").setFlexGrow(1);

        requestGrid.addColumn(request -> {
            if (request.getCustomer() != null) {
                return request.getCustomer().getName();
            }
            return "Belirtilmemiş";
        }).setHeader("Müşteri").setFlexGrow(1);

        requestGrid.addColumn(Request::getTitle).setHeader("Başlık").setFlexGrow(2);

        requestGrid.addColumn(request -> request.getAffectedNo() != null ? request.getAffectedNo() : 0)
                .setHeader("Etkilenen").setWidth("90px").setFlexGrow(0);

        requestGrid.addColumn(request -> {
            if (request.getCreatedAt() == null) return "-";
            return request.getCreatedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
        }).setHeader("Oluşturulma Tarihi").setAutoWidth(true);

        requestGrid.addColumn(request -> {
            if (request.getDeadline() == null) return "-";
            return request.getDeadline().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
        }).setHeader("Target Deadline").setAutoWidth(true);

        requestGrid.addComponentColumn(request -> {
            HorizontalLayout flags = new HorizontalLayout();
            flags.setSpacing(true);

            if (Boolean.TRUE.equals(request.getIsSecurityRisk())) {
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

            if (request.getFinancialImpact() != null && !request.getFinancialImpact().isBlank() && !"Etkisi Yok".equalsIgnoreCase(request.getFinancialImpact())) {
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

            return flags;
        }).setHeader("Riskler").setAutoWidth(true);

        requestGrid.addColumn(Request::getStatus).setHeader("Durum").setAutoWidth(true);

        requestGrid.addComponentColumn(request -> {
            Button inspectBtn = new Button("İncele", VaadinIcon.SEARCH.create());
            inspectBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
            inspectBtn.addClickListener(e -> {
                getUI().ifPresent(ui -> ui.navigate("pm/inspect/" + request.getId()));
            });
            return inspectBtn;
        }).setHeader("İşlem").setWidth("110px").setFlexGrow(0);

        requestGrid.setWidth("calc(100% - 40px)");
        requestGrid.getStyle()
                .set("margin", "0 auto")
                .set("background-color", "#ffffff")
                .set("border-radius", "12px")
                .set("border", "1px solid #e2e8f0")
                .set("box-shadow", "0 1px 3px rgba(0,0,0,0.02)");

        add(headerBanner, desc, filterCard, requestGrid);
    }

    private void setupFilterComponents() {
        searchField.setPlaceholder("Başlık, detay veya müşteri...");
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

        List<Request> filtered = allPendingRequests.stream().filter(r -> {
            boolean matchesQuery = query.isEmpty() ||
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

        requestGrid.setItems(filtered);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        companyFilter.setItems(companyService.getAllCompanies());

        this.allPendingRequests = requestService.getAllRequests().stream()
                .filter(r -> r.getStatus() == RequestStatus.PENDING)
                .toList();

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