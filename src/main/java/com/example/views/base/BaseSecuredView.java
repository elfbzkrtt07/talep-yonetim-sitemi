package com.example.views.base;

import com.example.entities.User;
import com.example.enums.UserRole;
import com.example.services.JwtService;
import com.example.services.UserService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import io.jsonwebtoken.Claims;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public abstract class BaseSecuredView extends VerticalLayout implements BeforeEnterObserver, ApplicationContextAware {

    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        applicationContext = context;
    }

    protected User currentUser;
    protected JwtService jwtService;
    protected UserService userService;

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (applicationContext != null) {
            if (jwtService == null) jwtService = applicationContext.getBean(JwtService.class);
            if (userService == null) userService = applicationContext.getBean(UserService.class);
        }

        UI ui = UI.getCurrent();

        ui.getPage().executeJs("return sessionStorage.getItem('auth_token');").then(String.class, token -> {

            if (token == null || token.isEmpty() || jwtService == null) {
                ui.access(this::showSessionExpiredDialog);
                return;
            }

            try {
                if (!jwtService.validateToken(token)) {
                    ui.access(this::showSessionExpiredDialog);
                    return;
                }

                Claims claims = jwtService.extractClaims(token);
                String email = claims.getSubject();

                if (userService != null && email != null) {
                    currentUser = userService.findByEmail(email).orElse(null);
                }

                if (currentUser == null) {
                    ui.access(this::showSessionExpiredDialog);
                    return;
                }

                String currentRoute = event.getLocation().getPath();
                String userRoleSegment = currentUser.getRole() != null ? currentUser.getRole().getUrlSegment() : "";

                if (!currentRoute.equals("profile") && !currentRoute.startsWith(userRoleSegment)) {
                    ui.access(() -> event.rerouteTo(userRoleSegment + "/dashboard"));
                    return;
                }

                ui.access(() -> {
                    onUserAuthenticated(event, currentUser);
                });

            } catch (Exception e) {
                ui.access(this::showSessionExpiredDialog);
            }
        });
    }

    private void showSessionExpiredDialog() {
        removeAll();

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Oturum Süresi Doldu");
        dialog.setCloseOnEsc(false);
        dialog.setCloseOnOutsideClick(false);

        dialog.add(new Span("Sunucu yeniden başlatıldı veya oturumunuz sona erdi. Lütfen tekrar giriş yapın."));

        Button loginButton = new Button("Giriş Sayfasına Dön", e -> {
            dialog.close();
            UI.getCurrent().getPage().executeJs("sessionStorage.removeItem('auth_token');");
            UI.getCurrent().navigate("login");
        });
        loginButton.getStyle().set("background-color", "var(--lumo-primary-color)");
        loginButton.getStyle().set("color", "white");

        dialog.getFooter().add(loginButton);
        
        add(dialog);
        dialog.open();
    }

    protected abstract void onUserAuthenticated(BeforeEnterEvent event, User user);
}