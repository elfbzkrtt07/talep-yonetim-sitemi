package com.example.views.auth;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

public class WaitingApprovalView extends VerticalLayout {

    public WaitingApprovalView(String userName, String currentRole) {
        setSizeFull();
        setJustifyContentMode(JustifyContentMode.START);
        setAlignItems(Alignment.CENTER);
        getStyle().set("padding-top", "80px");
        addClassName("login-viewport-bg");

        Image logo = new Image(
            com.vaadin.flow.server.streams.DownloadHandler.forClassResource(getClass(), "/META-INF/resources/logo.png"), 
            "MONAD Logo"
        );
        logo.addClassName("center-brand-logo-waiting");

        H2 waitingTitle = new H2("Hesabınız Onay Bekliyor");
        Paragraph waitingMsg = new Paragraph("Merhaba sayın " + userName + ", kayıt başvurunuz sistem yöneticisine ulaştı.");

        Button backBtn = new Button("Giriş Ekranına Dön", e -> {
            UI.getCurrent().getPage().setLocation(currentRole + "/login/");
        });
        backBtn.addClassName("modern-btn-secondary");
        backBtn.getStyle().set("width", "240px").set("max-width", "240px");

        add(logo, waitingTitle, waitingMsg, backBtn);
    }
}