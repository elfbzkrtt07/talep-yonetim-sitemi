package com.example.views.developer;

import com.example.base.ui.MainLayout;
import com.example.entities.Request;
import com.example.entities.User;
import com.example.entities.Workflow;
import com.example.services.WorkflowService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;

import java.time.format.DateTimeFormatter;
import java.util.List;

@PageTitle("Tamamlanan İşlerim")
@Route(value = "dev/completed", layout = MainLayout.class)
public class DeveloperCompletedView extends VerticalLayout {

    private final WorkflowService workflowService;
    private final Grid<Workflow> completedGrid = new Grid<>(Workflow.class, false);

    public DeveloperCompletedView(WorkflowService workflowService) {
        this.workflowService = workflowService;

        setSizeFull();
        setPadding(true);
        getStyle().set("background-color", "#f8fafc");

        VerticalLayout headerBanner = createHeaderBanner(
                "Tamamlanan İş Geçmişiniz", 
                "Başarıyla sonuçlandırıp teslim ettiğiniz talepler listelenmektedir."
        );
        add(headerBanner);

        completedGrid.addColumn(w -> w.getRequest().getId()).setHeader("Talep ID").setAutoWidth(true).setFlexGrow(0);
        completedGrid.addColumn(w -> w.getRequest().getCustomer() != null ? w.getRequest().getCustomer().getName() : "Bireysel").setHeader("Müşteri").setAutoWidth(true);
        completedGrid.addColumn(w -> w.getRequest().getTitle()).setHeader("Başlık").setSortable(true).setFlexGrow(2);

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
        
        add(completedGrid);

        loadGridData();
    }

    private void loadGridData() {
        User currentUser = (User) VaadinSession.getCurrent().getAttribute("user");
        if (currentUser != null) {
            List<Workflow> completedWorkflows = workflowService.getCompletedJobsForDeveloper(currentUser);
            completedGrid.setItems(completedWorkflows);
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