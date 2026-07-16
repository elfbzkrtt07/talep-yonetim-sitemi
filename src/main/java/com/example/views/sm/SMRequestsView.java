package com.example.views.sm;

import com.example.base.ui.MainLayout;
import com.example.entities.User;
import com.example.entities.Workflow;
import com.example.services.PrioritizationService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;

import java.util.Collections;
import java.util.List;

@Route(value = "sm/requests", layout = MainLayout.class)
public class SMRequestsView extends VerticalLayout implements BeforeEnterObserver {

    private final PrioritizationService prioritizationService; 
    private final Grid<Workflow> incomingGrid = new Grid<>(Workflow.class, false);

    public SMRequestsView(PrioritizationService prioritizationService) {
        this.prioritizationService = prioritizationService;

        setSizeFull();
        setPadding(true);
        getStyle().set("background-color", "#f8fafc");

        VerticalLayout headerBanner = createHeaderBanner(
                "Departman İş Akışları Havuzu", 
                "Ürün Yöneticisi tarafından departmanınıza yönlendirilen ve teknik değerlendirme bekleyen iş akışları."
                );
        add(headerBanner);

        incomingGrid.addColumn(w -> w.getRequest() != null ? w.getRequest().getId() : "-")
                .setHeader("ID").setWidth("80px").setFlexGrow(0);
        
        incomingGrid.addColumn(w -> (w.getRequest() != null && w.getRequest().getCustomer() != null) ? 
                w.getRequest().getCustomer().getName() : "-").setHeader("Müşteri");
        
        incomingGrid.addColumn(w -> w.getRequest() != null ? w.getRequest().getTitle() : "-")
                .setHeader("Başlık");
        
        incomingGrid.addColumn(Workflow::getStatus).setHeader("Akış Durumu");

        incomingGrid.addComponentColumn(workflow -> {
            Button evaluateBtn = new Button("Değerlendir", VaadinIcon.TOOLS.create());
            evaluateBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            evaluateBtn.addClickListener(e -> {
                if (workflow.getRequest() != null) {
                    getUI().ifPresent(ui -> ui.navigate("sm/evaluate/" + workflow.getRequest().getId()));
                }
            });
            return evaluateBtn;
        }).setHeader("İşlem").setWidth("150px").setFlexGrow(0);

        incomingGrid.setWidth("calc(100% - 40px)");
        incomingGrid.getStyle().set("margin", "0 auto");

        add(incomingGrid);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        User currentUser = (User) VaadinSession.getCurrent().getAttribute("user");
        
        if (currentUser == null || currentUser.getDepartment() == null) {
            incomingGrid.setItems(Collections.emptyList());
            return;
        }
        
        Long smDepartmentId = currentUser.getDepartment().getId();

        List<Workflow> departmentWorkflows = prioritizationService.getWorkflowsForDepartment(smDepartmentId);

        incomingGrid.setItems(departmentWorkflows);
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