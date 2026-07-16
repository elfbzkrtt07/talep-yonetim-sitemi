package com.example.views.sm;

import com.example.base.ui.MainLayout;
import com.example.entities.User;
import com.example.entities.Workflow;
import com.example.enums.UserRole;
import com.example.enums.WorkflowStatus;
import com.example.repositories.UserRepository;
import com.example.services.RequestService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
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

import java.util.ArrayList;
import java.util.List;

@PageTitle("Departman İş Geçmişi")
@Route(value = "sm/history", layout = MainLayout.class)
public class SMJobHistoryView extends VerticalLayout {

    private final RequestService requestService;
    private final UserRepository userRepository;
    private final Grid<Workflow> historyGrid = new Grid<>(Workflow.class, false);
    
    private final TextField searchField = new TextField("Talep Ara (ID veya Başlık)");
    private final ComboBox<WorkflowStatus> statusFilter = new ComboBox<>("Aşama Durumu");
    private final ComboBox<User> developerFilter = new ComboBox<>("Atanan Yazılımcı");

    private List<Workflow> allDepartmentWorkflows = new ArrayList<>();
    private User currentSm;

    public SMJobHistoryView(RequestService requestService, UserRepository userRepository) {
        this.requestService = requestService;
        this.userRepository = userRepository;
        
        this.currentSm = (User) VaadinSession.getCurrent().getAttribute("user");

        setSizeFull();
        setPadding(true);
        getStyle().set("background-color", "#f8fafc");

        VerticalLayout headerBanner = createHeaderBanner(
                "Departman İş Geçmişi", 
                "Departmanınıza ait aktif, tamamlanan veya iade edilen tüm işlerin takibi ve filtresi."
                );
        add(headerBanner);

        HorizontalLayout filterLayout = new HorizontalLayout();
        filterLayout.setWidthFull();
        filterLayout.setSpacing(true);
        filterLayout.setAlignItems(Alignment.END);

        searchField.setPlaceholder("Yazmaya başlayın...");
        searchField.setClearButtonVisible(true);
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.setValueChangeTimeout(300);
        searchField.addValueChangeListener(e -> filterGridData());
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchField.setWidth("30%");

        statusFilter.setPlaceholder("Tüm Durumlar");
        statusFilter.setItems(WorkflowStatus.values());
        statusFilter.setClearButtonVisible(true);
        statusFilter.addValueChangeListener(e -> filterGridData());
        statusFilter.setWidth("25%");

        developerFilter.setPlaceholder("Tüm Yazılımcılar");
        developerFilter.setClearButtonVisible(true);
        developerFilter.setItemLabelGenerator(User::getName);
        developerFilter.addValueChangeListener(e -> filterGridData());
        developerFilter.setWidth("25%");

        if (currentSm != null && currentSm.getDepartment() != null) {
            List<User> departmentDevs = userRepository.findAll().stream()
                    .filter(u -> u.getRole() == UserRole.DEVELOPER
                            && u.getDepartment() != null 
                            && u.getDepartment().getId().equals(currentSm.getDepartment().getId()))
                    .toList();
            developerFilter.setItems(departmentDevs);
        }

        Button clearFiltersBtn = new Button("Filtreleri Temizle", VaadinIcon.REFRESH.create(), e -> {
            searchField.clear();
            statusFilter.clear();
            developerFilter.clear();
        });
        clearFiltersBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        filterLayout.add(searchField, statusFilter, developerFilter, clearFiltersBtn);
        add(filterLayout);

        historyGrid.addColumn(w -> w.getRequest() != null ? w.getRequest().getId() : "-")
                .setHeader("Talep ID").setAutoWidth(true).setFlexGrow(0);
        
        historyGrid.addColumn(w -> (w.getRequest() != null && w.getRequest().getCustomer() != null) ? w.getRequest().getCustomer().getName() : "-")
                .setHeader("Müşteri").setAutoWidth(true);
        
        historyGrid.addColumn(w -> w.getRequest() != null ? w.getRequest().getTitle() : "-")
                .setHeader("Başlık").setFlexGrow(2);

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
        add(historyGrid);

        loadDataFromServices();
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
        String searchTxt = searchField.getValue().trim().toLowerCase();
        WorkflowStatus selectedStatus = statusFilter.getValue();
        User selectedDev = developerFilter.getValue();

        List<Workflow> filtered = allDepartmentWorkflows.stream()
                .filter(w -> {
                    if (!searchTxt.isEmpty()) {
                        String idStr = w.getRequest() != null ? String.valueOf(w.getRequest().getId()) : "";
                        String titleStr = (w.getRequest() != null && w.getRequest().getTitle() != null) ? w.getRequest().getTitle().toLowerCase() : "";
                        if (!idStr.contains(searchTxt) && !titleStr.contains(searchTxt)) {
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
                .set("margin-top", "25px")
                .set("margin-bottom", "15px");

        H2 title = new H2(titleText);
        title.getStyle()
                .set("margin", "0")
                .set("font-size", "2.5rem") 
                .set("font-weight", "1000") 
                .set("color", "#0f172a")
                .set("text-align", "center");
        bannerLayout.add(title);

        if (subtitleText != null && !subtitleText.isEmpty()) {
            Span subtitle = new Span(subtitleText);
            subtitle.getStyle()
                    .set("margin-top", "8px") 
                    .set("font-size", "0.9rem")
                    .set("color", "#64748b")
                    .set("text-align", "center");
            bannerLayout.add(subtitle);
        }

        return bannerLayout;
    }
}