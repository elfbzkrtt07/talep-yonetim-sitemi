package com.example.views.pm;

import com.example.base.ui.MainLayout;
import com.example.entities.User;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;

@Route(value = "pm/dashboard", layout = MainLayout.class)
public class PMDashboardView extends VerticalLayout {

    public PMDashboardView() {
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
        getStyle().set("background-color", "#f8fafc");

        Image logo = new Image(
            com.vaadin.flow.server.streams.DownloadHandler.forClassResource(getClass(), "/META-INF/resources/logo.png"), 
            "MONAD Logo");
        logo.setHeight("80px");
        logo.getStyle().set("margin-bottom", "10px");

        User currentUser = (User) VaadinSession.getCurrent().getAttribute("user");
        String userName = currentUser != null ? currentUser.getName() : "Ürün Sorumlusu";
        
        H1 welcomeHeading = new H1("Hoşgeldiniz, " + userName);
        welcomeHeading.getStyle()
            .set("font-size", "2.2rem")
            .set("font-weight", "800")
            .set("margin-bottom", "30px");

        HorizontalLayout cardRow = new HorizontalLayout();
        cardRow.setWidthFull();
        cardRow.setJustifyContentMode(JustifyContentMode.CENTER);
        cardRow.setSpacing(true);
        cardRow.getStyle().set("max-width", "900px").set("padding", "0 20px");

        Button requestsBtn = createDashboardShortcut("Gelen Talepler", VaadinIcon.INBOX, "pm/requests");
        
        Button prioritiesBtn = createDashboardShortcut("Öncelik Havuzu", VaadinIcon.LIST_OL, "pm/prioritizations");
        
        Button workflowsBtn = createDashboardShortcut("İş Akışları", VaadinIcon.CONNECT, "pm/workflows");
        
        Button analyticsBtn = createDashboardShortcut("Analitik Raporlar", VaadinIcon.CHART, "pm/analytics");

        cardRow.add(requestsBtn, prioritiesBtn, workflowsBtn, analyticsBtn);

        add(logo, welcomeHeading, cardRow);
    }


    private Button createDashboardShortcut(String label, VaadinIcon icon, String navigationPath) {
        Button button = new Button(label, icon.create());
        button.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        
        button.getStyle()
            .set("width", "180px")
            .set("height", "100px")
            .set("font-weight", "bold")
            .set("font-size", "1rem")
            .set("flex-direction", "column")
            .set("gap", "8px")
            .set("border-radius", "12px")
            .set("box-shadow", "0 4px 6px -1px rgb(0 0 0 / 0.1)");
            
        button.addClickListener(e -> UI.getCurrent().navigate(navigationPath));
        return button;
    }
}