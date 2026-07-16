package com.example.views.sm;

import com.example.base.ui.MainLayout;
import com.example.entities.User;
import com.example.entities.Workflow;
import com.example.enums.UserRole;
import com.example.enums.WorkflowStatus;
import com.example.repositories.UserRepository;
import com.example.services.RequestService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;

import java.util.ArrayList;
import java.util.List;

@PageTitle("Ekip İş Takibi")
@Route(value = "sm/monitor", layout = MainLayout.class)
public class SMDeveloperMonitorView extends VerticalLayout {

    private final RequestService requestService;
    private final UserRepository userRepository;
    
    private final HorizontalLayout cardsLayout = new HorizontalLayout();
    private final Grid<Workflow> devJobsGrid = new Grid<>(Workflow.class, false);
    private final Span selectedDevLabel = new Span("Tüm Ekip İşleri");
    private final List<Div> cardComponents = new ArrayList<>();

    private List<Workflow> allDeptWorkflows = new ArrayList<>();
    private List<User> deptDevelopers = new ArrayList<>();
    private User currentSm;
    private User selectedDeveloper = null; 

    public SMDeveloperMonitorView(RequestService requestService, UserRepository userRepository) {
        this.requestService = requestService;
        this.userRepository = userRepository;
        
        this.currentSm = (User) VaadinSession.getCurrent().getAttribute("user");

        setSizeFull();
        setPadding(true);
        getStyle().set("background-color", "#f8fafc");

        VerticalLayout headerBanner = createHeaderBanner(
                "Ekip Yönetimi ve Canlı İş Takibi", 
                "Departmanınızdaki yazılımcıların üzerindeki aktif yükü ve performans geçmişini izleyin."
                );
        add(headerBanner);

        cardsLayout.setWidthFull();
        cardsLayout.setSpacing(true);
        cardsLayout.getStyle()
                .set("display", "flex")
                .set("flex-wrap", "wrap")
                .set("gap", "15px")
                .set("padding-bottom", "15px");
        add(cardsLayout);

        HorizontalLayout gridHeader = new HorizontalLayout();
        gridHeader.setWidthFull();
        gridHeader.setAlignItems(Alignment.CENTER);
        gridHeader.getStyle()
                .set("margin-top", "25px")
                .set("margin-bottom", "10px")
                .set("background", "#ffffff")
                .set("padding", "12px 18px")
                .set("border-radius", "8px 8px 0 0")
                .set("border", "1px solid #e2e8f0")
                .set("border-bottom", "none");

        selectedDevLabel.getStyle()
                .set("font-weight", "700")
                .set("font-size", "1.1rem")
                .set("color", "#1e293b");

        Button showAllBtn = new Button("Tüm Ekibi Göster", VaadinIcon.USERS.create(), e -> {
            selectedDeveloper = null;
            selectedDevLabel.setText("Tüm Ekip İşleri");
            clearCardSelections();
            filterGridByDeveloper();
        });
        showAllBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);

        gridHeader.add(selectedDevLabel, showAllBtn);
        gridHeader.setFlexGrow(1, selectedDevLabel);
        add(gridHeader);

        devJobsGrid.addColumn(w -> w.getRequest() != null ? w.getRequest().getId() : "-")
                .setHeader("Talep ID").setAutoWidth(true).setFlexGrow(0);
        
        devJobsGrid.addColumn(w -> w.getRequest() != null ? w.getRequest().getTitle() : "-")
                .setHeader("Başlık").setFlexGrow(2);

        devJobsGrid.addColumn(w -> w.getStatus() != null ? w.getStatus().toString() : "-")
                .setHeader("İş Akış Durumu").setAutoWidth(true);

        devJobsGrid.addColumn(w -> w.getCurrentAssignee() != null ? w.getCurrentAssignee().getName() : "Atanmamış")
                .setHeader("Sorumlu Yazılımcı").setAutoWidth(true);

        devJobsGrid.setSizeFull();
        devJobsGrid.getStyle()
                .set("border", "1px solid #e2e8f0")
                .set("border-radius", "0 0 8px 8px")
                .set("box-shadow", "0 4px 6px -1px rgba(0,0,0,0.05)");
        add(devJobsGrid);

        loadDataAndBuildCards();
    }

    private void loadDataAndBuildCards() {
        if (currentSm == null || currentSm.getDepartment() == null) return;
        Long deptId = currentSm.getDepartment().getId();

        deptDevelopers = userRepository.findAll().stream()
                .filter(u -> u.getRole() == UserRole.DEVELOPER
                        && u.getDepartment() != null 
                        && u.getDepartment().getId().equals(deptId))
                .toList();

        allDeptWorkflows = requestService.getAllWorkflows().stream()
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

        cardsLayout.removeAll();
        cardComponents.clear();

        for (User dev : deptDevelopers) {
            long activeJobs = allDeptWorkflows.stream()
                    .filter(w -> w.getCurrentAssignee() != null 
                            && w.getCurrentAssignee().getId().equals(dev.getId()) 
                            && w.getStatus() == WorkflowStatus.DEVELOPMENT)
                    .count();

            long completedJobs = allDeptWorkflows.stream()
                    .filter(w -> w.getCurrentAssignee() != null 
                            && w.getCurrentAssignee().getId().equals(dev.getId()) 
                            && w.getStatus() == WorkflowStatus.COMPLETED)
                    .count();

            Div card = new Div();
            card.getStyle()
                    .set("background", "#ffffff")
                    .set("border", "1px solid #e2e8f0")
                    .set("border-radius", "12px")
                    .set("padding", "20px")
                    .set("width", "240px")
                    .set("box-shadow", "0 4px 6px -1px rgba(0,0,0,0.05)")
                    .set("cursor", "pointer")
                    .set("transition", "transform 0.2s, box-shadow 0.2s, border-color 0.2s");

            card.getElement().addEventListener("mouseover", e -> {
                card.getStyle().set("transform", "translateY(-4px)");
                card.getStyle().set("box-shadow", "0 10px 15px -3px rgba(0,0,0,0.1)");
            });
            card.getElement().addEventListener("mouseout", e -> {
                if (selectedDeveloper != dev) {
                    card.getStyle().set("transform", "none");
                    card.getStyle().set("box-shadow", "0 4px 6px -1px rgba(0,0,0,0.05)");
                }
            });

            cardComponents.add(card);

            card.addClickListener(e -> {
                selectedDeveloper = dev;
                selectedDevLabel.setText(dev.getName() + " Üzerindeki İşler");
                highlightSelectedCard(card);
                filterGridByDeveloper();
            });

            H4 devName = new H4(dev.getName());
            devName.getStyle()
                    .set("margin", "0 0 12px 0")
                    .set("font-size", "1.05rem")
                    .set("font-weight", "700")
                    .set("color", "#0f172a");

            HorizontalLayout statusRow = new HorizontalLayout();
            statusRow.setWidthFull();
            statusRow.setJustifyContentMode(JustifyContentMode.START);
            statusRow.setSpacing(true);

            Span activeBadge = new Span("Aktif: " + activeJobs);
            activeBadge.getStyle()
                    .set("background-color", "#dbeafe")
                    .set("color", "#1e40af")
                    .set("padding", "4px 10px")
                    .set("border-radius", "20px")
                    .set("font-size", "0.75rem")
                    .set("font-weight", "700");

            Span completedBadge = new Span("Biten: " + completedJobs);
            completedBadge.getStyle()
                    .set("background-color", "#d1fae5")
                    .set("color", "#065f46")
                    .set("padding", "4px 10px")
                    .set("border-radius", "20px")
                    .set("font-size", "0.75rem")
                    .set("font-weight", "700");

            statusRow.add(activeBadge, completedBadge);
            card.add(devName, statusRow);
            cardsLayout.add(card);
        }

        filterGridByDeveloper();
    }

    private void highlightSelectedCard(Div selectedCard) {
        clearCardSelections();
        selectedCard.getStyle()
                .set("border-color", "#3b82f6")
                .set("border-width", "2px")
                .set("transform", "translateY(-4px)")
                .set("box-shadow", "0 10px 15px -3px rgba(59, 130, 246, 0.15)");
    }

    private void clearCardSelections() {
        for (Div card : cardComponents) {
            card.getStyle()
                    .set("border-color", "#e2e8f0")
                    .set("border-width", "1px")
                    .set("transform", "none")
                    .set("box-shadow", "0 4px 6px -1px rgba(0,0,0,0.05)");
        }
    }

    private void filterGridByDeveloper() {
        if (selectedDeveloper == null) {
            List<Workflow> assignedWorkflows = allDeptWorkflows.stream()
                    .filter(w -> w.getCurrentAssignee() != null)
                    .toList();
            devJobsGrid.setItems(assignedWorkflows);
        } else {
            List<Workflow> devWorkflows = allDeptWorkflows.stream()
                    .filter(w -> w.getCurrentAssignee() != null 
                            && w.getCurrentAssignee().getId().equals(selectedDeveloper.getId()))
                    .toList();
            devJobsGrid.setItems(devWorkflows);
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