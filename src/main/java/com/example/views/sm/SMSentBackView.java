package com.example.views.sm;

import com.example.base.ui.MainLayout;
import com.example.entities.User;
import com.example.entities.Workflow;
import com.example.enums.WorkflowStatus;
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
import java.util.ArrayList;
import java.util.List;

@PageTitle("Geri Gönderilen İşlerim")
@Route(value = "sm/sent-back", layout = MainLayout.class)
public class SMSentBackView extends VerticalLayout {

    private final WorkflowService workflowService;
    private final Grid<Workflow> completedGrid = new Grid<>(Workflow.class, false);
    
    private List<Workflow> allSentBackWorkflows = new ArrayList<>();
    
    private final Button btnToMe = new Button("Bana Geri Gönderilenler", VaadinIcon.ARROW_CIRCLE_LEFT.create());
    private final Button btnToPM = new Button("PM'e Gönderdiklerim", VaadinIcon.ARROW_CIRCLE_RIGHT.create());
    
    private boolean showingToMe = true;

    public SMSentBackView(WorkflowService workflowService) {
        this.workflowService = workflowService;

        setSizeFull();
        setPadding(true);
        getStyle().set("background-color", "#f8fafc");

        VerticalLayout headerBanner = createHeaderBanner(
                "Geri Gönderilen İşler", 
                "Yazılımcı tarafından tarafınıza veya tarafınızdan PM'e iade edilen işlerin takibi."
                );
        add(headerBanner);

        HorizontalLayout toggleLayout = new HorizontalLayout(btnToMe, btnToPM);
        toggleLayout.setSpacing(true);
        toggleLayout.getStyle().set("margin-bottom", "15px");

        btnToMe.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        btnToPM.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        btnToMe.addClickListener(e -> {
            showingToMe = true;
            updateButtonVisuals();
            filterAndPopulateGrid();
        });

        btnToPM.addClickListener(e -> {
            showingToMe = false;
            updateButtonVisuals();
            filterAndPopulateGrid();
        });

        add(toggleLayout);

        completedGrid.addColumn(w -> w.getRequest().getId()).setHeader("Talep ID").setAutoWidth(true).setFlexGrow(0);
        completedGrid.addColumn(w -> w.getRequest().getCustomer() != null ? w.getRequest().getCustomer().getName() : "Bireysel").setHeader("Müşteri").setAutoWidth(true);
        completedGrid.addColumn(w -> w.getRequest().getTitle()).setHeader("Başlık").setSortable(true).setFlexGrow(2);

        completedGrid.addColumn(workflow -> {
            if (workflow.getStatus() != null) {
                return workflow.getStatus().toString();
            }
            return "-";
        }).setHeader("Aşama Durumu").setAutoWidth(true);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        completedGrid.addColumn(w -> w.getUpdatedAt() != null ? w.getUpdatedAt().format(formatter) : "-")
                .setHeader("Geri Gönderilme Tarihi").setSortable(true).setAutoWidth(true);

        completedGrid.addComponentColumn(workflow -> {
            Button actionBtn = new Button();
            if (workflow.getStatus() == WorkflowStatus.SENT_BACK_TO_SM) {
                actionBtn.setText("Yeniden Değerlendir");
                actionBtn.setIcon(VaadinIcon.EDIT.create());
                actionBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);
                actionBtn.addClickListener(e -> {
                    UI.getCurrent().navigate("sm/evaluate/" + workflow.getRequest().getId());
                });
            } else {
                actionBtn.setText("Detay");
                actionBtn.setIcon(VaadinIcon.EYE.create());
                actionBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
                actionBtn.addClickListener(e -> {
                    UI.getCurrent().navigate("sm/evaluate/" + workflow.getRequest().getId());
                });
            }
            return actionBtn;
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
            allSentBackWorkflows = workflowService.getSentBackRequestsForSM(currentUser);
            filterAndPopulateGrid();
        }
    }

    private void filterAndPopulateGrid() {
        List<Workflow> filteredList;
        if (showingToMe) {
            filteredList = allSentBackWorkflows.stream()
                    .filter(w -> w.getStatus() == WorkflowStatus.SENT_BACK_TO_SM)
                    .toList();
        } else {
            filteredList = allSentBackWorkflows.stream()
                    .filter(w -> w.getStatus() == WorkflowStatus.SENT_BACK_TO_PM)
                    .toList();
        }
        completedGrid.setItems(filteredList);
    }

    private void updateButtonVisuals() {
        if (showingToMe) {
            btnToMe.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            btnToMe.removeThemeVariants(ButtonVariant.LUMO_TERTIARY);
            
            btnToPM.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            btnToPM.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
        } else {
            btnToPM.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            btnToPM.removeThemeVariants(ButtonVariant.LUMO_TERTIARY);
            
            btnToMe.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            btnToMe.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
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