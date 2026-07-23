package com.example.views.pm;

import com.example.base.ui.MainLayout;
import com.example.entities.User;
import com.example.entities.Workflow;
import com.example.enums.WorkflowStatus;
import com.example.services.RequestService;
import com.example.views.base.BaseSecuredView;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.ArrayList;
import java.util.List;

@PageTitle("Geri Gönderilen İş Akışları")
@Route(value = "pm/sent-back", layout = MainLayout.class)
public class PMSentBackView extends BaseSecuredView {

    private final RequestService requestService;
    private final Grid<Workflow> sentBackGrid = new Grid<>(Workflow.class, false);
    
    private List<Workflow> allWorkflows = new ArrayList<>();
    
    private final Button btnToMe = new Button("Bana Geri Gönderilenler", VaadinIcon.ARROW_CIRCLE_LEFT.create());
    private final Button btnToCustomer = new Button("Müşteriye Gönderdiklerim", VaadinIcon.ARROW_CIRCLE_RIGHT.create());
    
    private boolean showingToMe = true; 

    public PMSentBackView(RequestService requestService) {
        this.requestService = requestService;

        setSizeFull();
        setPadding(true);
        getStyle().set("background-color", "#f8fafc");

        VerticalLayout headerBanner = createHeaderBanner(
                "Geri Gönderilen İş Akışları", 
                "Süreç içerisinde taraflar arasında iade edilen işlerin takibi."
        );

        HorizontalLayout toggleLayout = new HorizontalLayout(btnToMe, btnToCustomer);
        toggleLayout.setSpacing(true);
        toggleLayout.getStyle().set("margin", "0 auto 15px auto");

        btnToMe.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        btnToCustomer.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        btnToMe.addClickListener(e -> {
            showingToMe = true;
            updateButtonVisuals();
            filterAndPopulateGrid();
        });

        btnToCustomer.addClickListener(e -> {
            showingToMe = false;
            updateButtonVisuals();
            filterAndPopulateGrid();
        });

        add(headerBanner, toggleLayout);

        sentBackGrid.addColumn(w -> w.getRequest() != null ? w.getRequest().getId() : "-")
                .setHeader("Talep ID").setAutoWidth(true).setFlexGrow(0);
        
        sentBackGrid.addColumn(w -> (w.getRequest() != null && w.getRequest().getCustomer() != null) ? w.getRequest().getCustomer().getName() : "-")
                .setHeader("Müşteri").setAutoWidth(true);
        
        sentBackGrid.addColumn(w -> w.getRequest() != null ? w.getRequest().getTitle() : "-")
                .setHeader("Başlık").setFlexGrow(2);

        sentBackGrid.addColumn(workflow -> {
            if (workflow.getStatus() != null) {
                return workflow.getStatus().toString();
            } else if (workflow.getRequest() != null && workflow.getRequest().getStatus() != null) {
                return workflow.getRequest().getStatus().toString();
            }
            return "-";
        }).setHeader("Aşama Durumu").setAutoWidth(true);

        sentBackGrid.addComponentColumn(workflow -> {
            Button inspectBtn = new Button("İncele", VaadinIcon.EYE.create());
            inspectBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            inspectBtn.addClickListener(e -> {
                if (workflow.getRequest() != null) {
                    UI.getCurrent().navigate("pm/inspect/" + workflow.getRequest().getId());
                }
            });
            return inspectBtn;
        }).setHeader("İşlem").setAutoWidth(true);

        sentBackGrid.setWidth("calc(100% - 40px)");
        sentBackGrid.getStyle()
                .set("margin", "0 auto 40px auto")
                .set("border", "1px solid #cbd5e1")
                .set("border-radius", "8px")
                .set("box-shadow", "0 1px 3px rgba(0,0,0,0.05)");
        
        add(sentBackGrid);
    }

    @Override
    protected void onUserAuthenticated(BeforeEnterEvent event, User user) {
        loadDataFromService();
    }

    private void loadDataFromService() {
        allWorkflows = new ArrayList<>(requestService.getAllWorkflows());
        
        List<com.example.entities.Request> rawSentBackRequests = requestService.getAllRequests().stream()
                .filter(r -> r.getStatus() == com.example.enums.RequestStatus.SENT_BACK)
                .toList();

        for (com.example.entities.Request req : rawSentBackRequests) {
            boolean alreadyExists = allWorkflows.stream()
                    .anyMatch(w -> w.getRequest() != null && w.getRequest().getId().equals(req.getId()));
            
            if (!alreadyExists) {
                Workflow fakeWf = new Workflow();
                fakeWf.setRequest(req);
                fakeWf.setStatus(WorkflowStatus.REVISION_REQUIRED); 
                allWorkflows.add(fakeWf);
            }
        }
        
        filterAndPopulateGrid();
    }

    private void filterAndPopulateGrid() {
        List<Workflow> filteredList;
        if (showingToMe) {
            filteredList = allWorkflows.stream()
                    .filter(w -> w.getStatus() == WorkflowStatus.SENT_BACK_TO_PM)
                    .toList();
        } else {
            filteredList = allWorkflows.stream()
                    .filter(w -> w.getStatus() == WorkflowStatus.REVISION_REQUIRED)
                    .toList();
        }
        sentBackGrid.setItems(filteredList);
    }

    private void updateButtonVisuals() {
        if (showingToMe) {
            btnToMe.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            btnToMe.removeThemeVariants(ButtonVariant.LUMO_TERTIARY);
            
            btnToCustomer.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            btnToCustomer.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
        } else {
            btnToCustomer.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            btnToCustomer.removeThemeVariants(ButtonVariant.LUMO_TERTIARY);
            
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