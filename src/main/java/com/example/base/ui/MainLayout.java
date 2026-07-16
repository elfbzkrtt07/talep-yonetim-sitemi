package com.example.base.ui;

import com.example.entities.User;
import com.example.enums.UserRole;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.VaadinSession;
import com.example.views.HomeView;
import com.example.views.customer.CustomerDashboardView;

public class MainLayout extends AppLayout {

    public MainLayout() {
        createHeader();
        createDrawer();
    }

    private void createHeader() {
        H1 logo = new H1("TALEP YÖNETİM SİSTEMİ");
        logo.getStyle().set("font-size", "var(--lumo-font-size-l)")
                      .set("margin", "0 var(--lumo-space-m)")
                      .set("font-weight", "bold");

        User currentUser = (User) VaadinSession.getCurrent().getAttribute("user");
        String userName = currentUser != null ? currentUser.getName() : "Misafir Kullanıcı";
        String userEmail = currentUser != null ? currentUser.getEmail() : "";

        Avatar avatar = new Avatar(userName);
        avatar.getStyle().set("cursor", "pointer");

        HorizontalLayout profileWrapper = new HorizontalLayout(new Span(userName), avatar);
        profileWrapper.setAlignItems(FlexComponent.Alignment.CENTER);
        profileWrapper.setSpacing(true);
        profileWrapper.getStyle().set("margin-right", "var(--lumo-space-m)");

        // Dropdown menu attached to the profile wrapper
        ContextMenu profileMenu = new ContextMenu(profileWrapper);
        profileMenu.setOpenOnClick(true);

        profileMenu.addItem("Profil Ayarları", e -> {
            Notification.show("Profil ayarları yakında eklenecek.");
        });
        
        profileMenu.addItem("Sistem Seçenekleri", e -> {
            Notification.show("Sistem ayarları ayarlanıyor...");
        });
        
        profileMenu.addSeparator();
        profileMenu.addItem("Çıkış", e -> {
            VaadinSession.getCurrent().close();
            UI.getCurrent().getPage().setLocation("/login");
        });

        HorizontalLayout header = new HorizontalLayout(new DrawerToggle(), logo, profileWrapper);
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.setWidthFull();
        
        header.expand(logo); 
        
        header.getStyle().set("background-color", "var(--lumo-base-color)")
                        .set("border-bottom", "1px solid var(--lumo-contrast-10pct)")
                        .set("padding", "0 var(--lumo-space-m)");

        addToNavbar(header);
    }

    private void createDrawer() {
        VerticalLayout menuContainer = new VerticalLayout();
        menuContainer.setSpacing(true);
        menuContainer.setPadding(true);

        User currentUser = (User) VaadinSession.getCurrent().getAttribute("user");

        if (currentUser == null || currentUser.getRole() == null) {
            menuContainer.add(new Span("Menü yüklenemedi: Oturum Yok"));
            addToDrawer(menuContainer);
            return;
        }

        UserRole role = currentUser.getRole();

        switch (role) {

            case CUSTOMER:
                RouterLink customerDash = new RouterLink("Taleplerim", CustomerDashboardView.class);
                RouterLink newRequestShortcut = new RouterLink("Yeni Talep Aç", CustomerDashboardView.class);
                newRequestShortcut.setQueryParameters(com.vaadin.flow.router.QueryParameters.simple(java.util.Map.of("action", "new")));
                
                RouterLink returnedRequests = new RouterLink("Geri Dönen Taleplerim", CustomerDashboardView.class);
                returnedRequests.setQueryParameters(com.vaadin.flow.router.QueryParameters.simple(java.util.Map.of("filter", "sent_back")));
                
                menuContainer.add(customerDash, newRequestShortcut, returnedRequests);
                break;

            case PRODUCT_MANAGER:
                RouterLink pmHome = new RouterLink("Ana Sayfa", com.example.views.pm.PMDashboardView.class);
                RouterLink pmRequests = new RouterLink("Gelen Talepler", com.example.views.pm.PMRequestsView.class); 
                RouterLink pmPriorities = new RouterLink("Öncelik Havuzu", com.example.views.pm.PMPrioritizationsView.class);
                RouterLink pmWorkflows = new RouterLink("İş Akışları", com.example.views.pm.PMWorkflowsView.class);
                RouterLink pmSentBack = new RouterLink("Geri Gönderilenler", com.example.views.pm.PMSentBackView.class);
                // RouterLink pmAnalytics = new RouterLink("Analitik Raporlar", com.example.views.pm.PMAnalyticsView.class);
                
                menuContainer.add(pmHome, pmRequests, pmPriorities, pmWorkflows, pmSentBack/*, pmAnalytics);*/ );
                break;

            case DEVELOPER:
                RouterLink devTasks = new RouterLink("Bana Atanan İşler", com.example.views.developer.DeveloperDashboardView.class);
                RouterLink devCompleted = new RouterLink("Tamamlanan İşler", com.example.views.developer.DeveloperCompletedView.class);
                menuContainer.add(devTasks, devCompleted);
                break;

            case SOFTWARE_MANAGER:
                RouterLink smHome = new RouterLink("Ana Sayfa", com.example.views.sm.SMDashboardView.class);
                RouterLink smInbox = new RouterLink("Departmana Atanan İşler", com.example.views.sm.SMRequestsView.class);
                RouterLink smSentBack = new RouterLink("Geri Gönderilen İş Akışları", com.example.views.sm.SMSentBackView.class);
                RouterLink smDeptHistory = new RouterLink("Departman İş Geçmişi", com.example.views.sm.SMJobHistoryView.class);
                RouterLink smDevMonitor = new RouterLink("Ekip Yönetimi", com.example.views.sm.SMDeveloperMonitorView.class);
                menuContainer.add(smHome, smInbox, smSentBack, smDeptHistory, smDevMonitor/* , smAnalytics*/);
                break;

            default:
                menuContainer.add(new Span("Bilinmeyen Rol Menüsü"));
                break;
        }

        addToDrawer(menuContainer);
    }
}