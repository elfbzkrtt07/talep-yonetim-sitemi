package com.example.views.sm;

import com.example.base.ui.MainLayout;
import com.example.entities.User;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Route(value = "sm/dashboard", layout = MainLayout.class)
public class SMDashboardView extends VerticalLayout {

    @PersistenceContext
    private EntityManager entityManager;

    public SMDashboardView(EntityManager entityManager) {
        this.entityManager = entityManager;

        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
        getStyle().set("background-color", "#f8fafc");

        Image logo = new Image(
            com.vaadin.flow.server.streams.DownloadHandler.forClassResource(getClass(), "/META-INF/resources/logo.png"), 
            "MONAD Logo");
        logo.setHeight("80px");
        logo.getStyle().set("margin-bottom", "10px");

        User sessionUser = (User) VaadinSession.getCurrent().getAttribute("user");
        String smName = "Yazılım Sorumlusu";
        String smDept = "Genel";

        if (sessionUser != null) {
            smName = sessionUser.getName();
            try {
                User managedUser = entityManager.createQuery(
                    "SELECT u FROM User u LEFT JOIN FETCH u.department WHERE u.id = :id", User.class)
                    .setParameter("id", sessionUser.getId())
                    .getSingleResult();
                
                if (managedUser.getDepartment() != null) {
                    smDept = managedUser.getDepartment().getName();
                }
            } catch (Exception ex) {
                smDept = "Genel";
            }
        }
        
        H1 welcomeHeading = new H1("Hoşgeldiniz, " + smName);
        welcomeHeading.getStyle()
            .set("font-size", "2.2rem")
            .set("font-weight", "800")
            .set("margin-bottom", "5px");

        HorizontalLayout cardRow = new HorizontalLayout();
        cardRow.setWidthFull();
        cardRow.setJustifyContentMode(JustifyContentMode.CENTER);
        cardRow.setSpacing(true);
        cardRow.getStyle().set("max-width", "800px").set("padding", "0 20px");

        Button inboxBtn = createDashboardShortcut("Gelen Talepler", VaadinIcon.LAPTOP, "sm/requests");
        Button analyticsBtn = createDashboardShortcut("Teknik Analiz", VaadinIcon.CHART_LINE, "sm/analytics");

        cardRow.add(inboxBtn, analyticsBtn);
        add(logo, welcomeHeading, cardRow);
    }

    private Button createDashboardShortcut(String label, VaadinIcon icon, String navigationPath) {
        Button button = new Button(label, icon.create());
        button.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        
        button.getStyle()
            .set("width", "200px")
            .set("height", "110px")
            .set("font-weight", "bold")
            .set("font-size", "1rem")
            .set("flex-direction", "column")
            .set("gap", "10px")
            .set("border-radius", "12px")
            .set("box-shadow", "0 4px 6px -1px rgb(0 0 0 / 0.1)");
            
        button.addClickListener(e -> UI.getCurrent().navigate(navigationPath));
        return button;
    }
}