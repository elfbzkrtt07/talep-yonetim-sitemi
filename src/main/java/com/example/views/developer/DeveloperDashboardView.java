package com.example.views.developer;

import com.example.base.ui.MainLayout;
import com.example.entities.Prioritization;
import com.example.entities.User;
import com.example.entities.Workflow;
import com.example.repositories.WorkflowRepository;
import com.example.services.PrioritizationService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

import java.util.List;

@Route(value = "dev/dashboard", layout = MainLayout.class)
public class DeveloperDashboardView extends VerticalLayout {

    private final WorkflowRepository workflowRepository;
    private final PrioritizationService prioritizationService;
    private final Grid<Workflow> taskGrid = new Grid<>(Workflow.class, false);

    public DeveloperDashboardView(WorkflowRepository workflowRepository, PrioritizationService prioritizationService) {
        this.workflowRepository = workflowRepository;
        this.prioritizationService = prioritizationService;

        setSizeFull();
        setPadding(true);
        getStyle().set("background-color", "#f8fafc");

        VerticalLayout headerBanner = createHeaderBanner(
                "Hoşgeldiniz", 
                "Ürün sorumlusu tarafından size atanan müşteri talepleri talep skoruna göre sıralanmıştır."
        );
        add(headerBanner);

        taskGrid.addColumn(w -> w.getRequest().getId()).setHeader("ID").setAutoWidth(true);
        taskGrid.addColumn(w -> w.getRequest().getCustomer().getName()).setHeader("Müşteri").setAutoWidth(true);
        taskGrid.addColumn(w -> w.getRequest().getTitle()).setHeader("Başlık").setAutoWidth(true);
        
        taskGrid.addColumn(w -> {
            try {
                return prioritizationService.getPrioritizationById(w.getRequest().getId()).getPriorityScore(); 
            } catch(Exception e) { return "-"; }
        }).setHeader("Skor").setAutoWidth(true);

        taskGrid.addComponentColumn(workflow -> {
            Button editBtn = new Button("Düzenle", e -> 
                UI.getCurrent().navigate("dev/edit/" + workflow.getRequest().getId())
            );
            editBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            return editBtn;
        }).setHeader("İşlem");

        add(taskGrid);
        loadDeveloperTasks();
    }

    private void loadDeveloperTasks() {
        User currentUser = (User) com.vaadin.flow.server.VaadinSession.getCurrent().getAttribute("user");
        
        if (currentUser != null) {
            List<Workflow> assignedWorkflows = workflowRepository.findAssignedTasksByDeveloperId(currentUser.getId());
            taskGrid.setItems(assignedWorkflows);
        } else {
            Notification.show("Oturum açmış kullanıcı bulunamadı!").addThemeVariants(NotificationVariant.LUMO_ERROR);
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