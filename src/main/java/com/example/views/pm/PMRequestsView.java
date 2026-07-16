package com.example.views.pm;

import com.example.base.ui.MainLayout;
import com.example.entities.Request;
import com.example.enums.RequestStatus;
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

@Route(value = "pm/requests", layout = MainLayout.class)
public class PMRequestsView extends VerticalLayout implements BeforeEnterObserver {

    private final RequestService requestService;
    private final Grid<Request> requestGrid = new Grid<>(Request.class, false);

    public PMRequestsView(RequestService requestService) {
        this.requestService = requestService;

        setSizeFull();
        setPadding(true);
        getStyle().set("background-color", "#f8fafc");

        VerticalLayout headerBanner = createHeaderBanner("Gelen Talepler");
        
        Paragraph desc = new Paragraph("Gözden geçirilip öncelik havuzuna veya iş akışına aktarılması beklenen talepler.");
        desc.getStyle()
                .set("color", "#64748b")
                .set("text-align", "center")
                .set("margin", "0 auto 25px auto");

        requestGrid.addColumn(Request::getId).setHeader("ID").setWidth("80px").setFlexGrow(0);
        
        requestGrid.addColumn(request -> {
            if (request.getCustomer() != null) {
                return request.getCustomer().getName();
            }
            return "Belirtilmemiş";
        }).setHeader("Müşteri");

        requestGrid.addColumn(Request::getTitle).setHeader("Başlık");
        requestGrid.addColumn(Request::getAffectedNo).setHeader("Etkilenen");
        requestGrid.addColumn(Request::getStatus).setHeader("Durum");

        requestGrid.addComponentColumn(request -> {
            Button inspectBtn = new Button("İncele", VaadinIcon.SEARCH.create());
            inspectBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            inspectBtn.addClickListener(e -> {
                getUI().ifPresent(ui -> ui.navigate("pm/inspect/" + request.getId()));
            });
            return inspectBtn;
        }).setHeader("İşlem").setWidth("130px").setFlexGrow(0);

        requestGrid.setWidth("calc(100% - 40px)");
        requestGrid.getStyle().set("margin", "0 auto");

        add(headerBanner, desc, requestGrid);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        List<Request> pendingList = requestService.getAllRequests().stream()
                .filter(r -> r.getStatus() == RequestStatus.PENDING)
                .toList();
        requestGrid.setItems(pendingList);
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