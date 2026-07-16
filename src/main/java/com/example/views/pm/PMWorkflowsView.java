package com.example.views.pm;

import com.example.base.ui.MainLayout;
import com.example.entities.Workflow;
import com.example.enums.WorkflowStatus;
import com.example.services.RequestService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;

import java.util.ArrayList;
import java.util.List;

@Route(value = "pm/workflows", layout = MainLayout.class)
public class PMWorkflowsView extends VerticalLayout implements BeforeEnterObserver {

    private final RequestService requestService;
    private final Grid<Workflow> workflowGrid = new Grid<>(Workflow.class, false);
    
    private List<Workflow> allWorkflows = new ArrayList<>();
    
    private final TextField searchField = new TextField();
    private final ComboBox<WorkflowStatus> statusFilter = new ComboBox<>();

    public PMWorkflowsView(RequestService requestService) {
        this.requestService = requestService;

        setSizeFull();
        setPadding(true);
        getStyle().set("background-color", "#f8fafc");

        VerticalLayout headerBanner = createHeaderBanner("İş Akışları");

        Paragraph desc = new Paragraph("İş akışına dönüştürülen ve geliştirme ekipleri tarafından işlenen taleplerin güncel durumları.");
        desc.getStyle()
                .set("color", "#64748b")
                .set("text-align", "center")
                .set("margin", "0 auto 30px auto");

        HorizontalLayout filterLayout = new HorizontalLayout();
        filterLayout.setWidth("calc(100% - 40px)");
        filterLayout.getStyle().set("margin", "0 auto 15px auto");
        filterLayout.setSpacing(true);

        searchField.setPlaceholder("ID, Başlık veya Müşteri ara...");
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchField.setClearButtonVisible(true);
        
        filterLayout.addAndExpand(searchField);
        
        searchField.setValueChangeMode(ValueChangeMode.EAGER);
        searchField.addValueChangeListener(e -> filterGridData());

        statusFilter.setPlaceholder("Aşama Durumu Seçin...");
        statusFilter.setItems(WorkflowStatus.values());
        statusFilter.setClearButtonVisible(true);
        statusFilter.setWidth("250px");
        statusFilter.addValueChangeListener(e -> filterGridData());

        filterLayout.add(statusFilter);

        workflowGrid.addColumn(w -> w.getRequest() != null ? w.getRequest().getId() : "-")
                .setHeader("ID").setWidth("80px").setFlexGrow(0);

        workflowGrid.addColumn(w -> {
            if (w.getRequest() != null && w.getRequest().getCustomer() != null) {
                return w.getRequest().getCustomer().getName();
            }
            return "-";
        }).setHeader("Müşteri").setSortable(true);

        workflowGrid.addColumn(w -> w.getRequest() != null ? w.getRequest().getTitle() : "-")
                .setHeader("Başlık").setSortable(true);

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
        workflowGrid.getStyle().set("margin", "0 auto")
                             .set("border", "1px solid #cbd5e1")
                             .set("border-radius", "8px")
                             .set("box-shadow", "0 1px 3px rgba(0,0,0,0.05)");

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

        add(headerBanner, desc, filterLayout, workflowGrid);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        allWorkflows = requestService.getAllWorkflows(); 
        workflowGrid.setItems(allWorkflows);
    }

    private void filterGridData() {
        String searchTerm = searchField.getValue() != null ? searchField.getValue().trim().toLowerCase() : "";
        WorkflowStatus selectedStatus = statusFilter.getValue();

        List<Workflow> filteredList = allWorkflows.stream().filter(w -> {
            boolean matchesStatus = (selectedStatus == null) || (w.getStatus() == selectedStatus);

            boolean matchesSearch = searchTerm.isEmpty() 
                || (w.getRequest() != null && String.valueOf(w.getRequest().getId()).contains(searchTerm))
                || (w.getRequest() != null && w.getRequest().getTitle() != null && w.getRequest().getTitle().toLowerCase().contains(searchTerm))
                || (w.getRequest() != null && w.getRequest().getCustomer() != null && w.getRequest().getCustomer().getName() != null && w.getRequest().getCustomer().getName().toLowerCase().contains(searchTerm));

            return matchesStatus && matchesSearch;
        }).toList();

        workflowGrid.setItems(filteredList);
    }

    private VerticalLayout createHeaderBanner(String titleText) {
        VerticalLayout bannerLayout = new VerticalLayout();
        bannerLayout.setWidthFull();
        bannerLayout.setAlignItems(Alignment.CENTER); 
        bannerLayout.setJustifyContentMode(JustifyContentMode.CENTER);
        bannerLayout.setPadding(false);
        bannerLayout.getStyle()
                .set("margin-top", "25px")
                .set("margin-bottom", "10px");

        H2 title = new H2(titleText);
        title.getStyle()
                .set("margin", "0")
                .set("font-size", "2.5rem") 
                .set("font-weight", "1000") 
                .set("color", "#0f172a")
                .set("text-align", "center");

        bannerLayout.add(title);
        return bannerLayout;
    }
}