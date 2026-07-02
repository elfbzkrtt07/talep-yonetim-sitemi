package com.example.views.pm;

import com.example.base.ui.MainLayout;
import com.example.entities.Workflow;
import com.example.services.RequestService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;

import java.util.List;

@Route(value = "pm/workflows", layout = MainLayout.class)
public class PMWorkflowsView extends VerticalLayout implements BeforeEnterObserver {

    private final RequestService requestService;
    private final Grid<Workflow> workflowGrid = new Grid<>(Workflow.class, false);

    public PMWorkflowsView(RequestService requestService) {
        this.requestService = requestService;

        setSizeFull();
        setPadding(true);
        getStyle().set("background-color", "#f8fafc");

        H2 title = new H2("İş Akışları");
        title.getStyle().set("margin", "10px var(--lumo-space-m) 5px var(--lumo-space-m)");

        Paragraph desc = new Paragraph("İş akışına dönüştürülen ve geliştirme ekipleri tarafından işlenen taleplerin güncel durumları.");
        desc.getStyle().set("color", "#64748b").set("margin", "0 var(--lumo-space-m) 20px var(--lumo-space-m)");

        // Grid Configuration mapped to your Workflow entity schema
        workflowGrid.addColumn(w -> w.getRequest() != null ? w.getRequest().getId() : "-")
                .setHeader("ID").setWidth("80px").setFlexGrow(0);

        workflowGrid.addColumn(w -> {
            if (w.getRequest() != null && w.getRequest().getCustomer() != null) {
                return w.getRequest().getCustomer().getName();
            }
            return "-";
        }).setHeader("Müşteri");

        workflowGrid.addColumn(w -> w.getRequest() != null ? w.getRequest().getTitle() : "-")
                .setHeader("Başlık");

        workflowGrid.addColumn(w -> w.getCurrentAssignee() != null ? w.getCurrentAssignee().getName() : "Atanmadı")
                .setHeader("Atanan Yazılımcı");

        workflowGrid.addColumn(Workflow::getStatus).setHeader("Aşama Durumu");

        // Action button to inspect timeline log details
        workflowGrid.addComponentColumn(workflow -> {
            Button inspectBtn = new Button("İncele", VaadinIcon.SEARCH.create());
            inspectBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            inspectBtn.addClickListener(e -> {
                if (workflow.getRequest() != null) {
                    getUI().ifPresent(ui -> ui.navigate("pm/inspect/" + workflow.getRequest().getId()));
                }
            });
            return inspectBtn;
        }).setHeader("İşlem").setWidth("120px").setFlexGrow(0);

        workflowGrid.setWidth("calc(100% - 40px)");
        workflowGrid.getStyle().set("margin", "0 auto");

        add(title, desc, workflowGrid);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        // Connects straight to the active pipeline tracking array query we wrote in Step 1
        List<com.example.entities.Workflow> activeTrackingPool = requestService.getAllWorkflows(); 
        workflowGrid.setItems(activeTrackingPool);
    }
}