package com.example.views;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@PageTitle("Hoş Geldiniz | Müşteri Talep & İş Yönetim Sistemi")
@Route(value = "")
@AnonymousAllowed
public class HomeView extends VerticalLayout {

    public HomeView() {
        setSizeFull();
        setPadding(false);
        setSpacing(false);
        getStyle()
                .set("background", "linear-gradient(135deg, #f0f4f8 0%, #e2e8f0 100%)")
                .set("overflow-y", "auto");

        HorizontalLayout navbar = new HorizontalLayout();
        navbar.setWidthFull();
        navbar.setPadding(true);
        navbar.setAlignItems(Alignment.CENTER);
        navbar.setJustifyContentMode(JustifyContentMode.BETWEEN);
        navbar.getStyle()
                .set("padding", "20px 50px")
                .set("background", "white")
                .set("box-shadow", "0 2px 10px rgba(0, 0, 0, 0.05)");

        Image logo = new Image("logo.png", "Monad Logo");
        logo.setHeight("45px");

        Button loginBtn = new Button("Giriş Yap", VaadinIcon.SIGN_IN.create(), e -> UI.getCurrent().navigate("login"));
        loginBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        loginBtn.getStyle()
                .set("background-color", "#0066cc")
                .set("border-radius", "8px")
                .set("padding", "0 24px")
                .set("font-weight", "600");

        navbar.add(logo, loginBtn);

        VerticalLayout heroLayout = new VerticalLayout();
        heroLayout.setAlignItems(Alignment.CENTER);
        heroLayout.setJustifyContentMode(JustifyContentMode.CENTER);
        heroLayout.getStyle().set("padding", "50px 20px 30px 20px");

        H1 mainTitle = new H1("Müşteri Talep & İş Yönetim Sistemi");
        mainTitle.getStyle()
                .set("margin", "0 0 16px 0")
                .set("color", "#102a43")
                .set("font-weight", "800")
                .set("font-size", "2.5rem")
                .set("text-align", "center");

        Paragraph subTitle = new Paragraph("Müşterilerden gelen destek ve geliştirme taleplerinin uçtan uca yönetilmesini, önceliklendirilmesini ve yazılım ekipleri tarafından çözülmesini sağlayan web tabanlı iş akışı platformu.");
        subTitle.getStyle()
                .set("color", "#486581")
                .set("font-size", "1.1rem")
                .set("max-width", "750px")
                .set("text-align", "center")
                .set("margin", "0 0 28px 0")
                .set("line-height", "1.6");

        Button actionBtn = new Button("Sisteme Giriş Yapın", VaadinIcon.ARROW_RIGHT.create(), e -> UI.getCurrent().navigate("login"));
        actionBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        actionBtn.getStyle()
                .set("background-color", "#0066cc")
                .set("border-radius", "10px")
                .set("height", "50px")
                .set("font-size", "1.05rem")
                .set("font-weight", "600")
                .set("padding", "0 32px")
                .set("box-shadow", "0 10px 20px -5px rgba(0, 102, 204, 0.3)");

        heroLayout.add(mainTitle, subTitle, actionBtn);

        HorizontalLayout featuresLayout = new HorizontalLayout();
        featuresLayout.setWidthFull();
        featuresLayout.setMaxWidth("1100px");
        featuresLayout.setJustifyContentMode(JustifyContentMode.CENTER);
        featuresLayout.getStyle()
                .set("margin", "10px auto 40px auto")
                .set("gap", "24px")
                .set("padding", "0 20px");

        VerticalLayout card1 = createFeatureCard(
                VaadinIcon.RECORDS.create(),
                "Dinamik Talep Oluşturma",
                "Sistemlerinizde karşılaştığınız tüm süreçler ve ihtiyaçlar hakkında kolayca destek talebi oluşturun."
        );

        VerticalLayout card2 = createFeatureCard(
                VaadinIcon.TASKS.create(),
                "Yöneticiler ile Doğrudan İletişim",
                "Taleplerinizin en hızlı şekilde çözüme kavuşması için ilgili yönetim ve teknik ekiplerle doğrudan etkileşim kurun."
        );

        VerticalLayout card3 = createFeatureCard(
                VaadinIcon.CHAT.create(),
                "İlerleme Takibi",
                "Oluşturduğunuz taleplerin güncel durumunu ve geçirildiği aşamaları anlık olarak takip edin."
        );

        featuresLayout.add(card1, card2, card3);

        HorizontalLayout footer = new HorizontalLayout();
        footer.setWidthFull();
        footer.setJustifyContentMode(JustifyContentMode.CENTER);
        footer.getStyle()
                .set("padding", "24px")
                .set("background", "#102a43")
                .set("color", "#9fb3c8")
                .set("margin-top", "auto");

        Span footerText = new Span("© 2026 Monad Yazılım ve Danışmanlık. Tüm hakları saklıdır.");
        footerText.getStyle().set("font-size", "0.85rem");
        footer.add(footerText);

        add(navbar, heroLayout, featuresLayout, footer);
    }

    private VerticalLayout createFeatureCard(com.vaadin.flow.component.icon.Icon icon, String titleText, String descText) {
        VerticalLayout card = new VerticalLayout();
        card.setWidth("100%");
        card.getStyle()
                .set("background", "white")
                .set("padding", "32px 24px")
                .set("border-radius", "16px")
                .set("box-shadow", "0 15px 30px -5px rgba(0, 102, 204, 0.08)");

        VerticalLayout iconBadge = new VerticalLayout(icon);
        iconBadge.setAlignItems(Alignment.CENTER);
        iconBadge.setJustifyContentMode(JustifyContentMode.CENTER);
        iconBadge.setWidth("50px");
        iconBadge.setHeight("50px");
        iconBadge.getStyle()
                .set("background", "#e6f0fa")
                .set("color", "#0066cc")
                .set("border-radius", "12px")
                .set("margin-bottom", "16px");

        H3 title = new H3(titleText);
        title.getStyle()
                .set("margin", "0 0 8px 0")
                .set("color", "#102a43")
                .set("font-size", "1.15rem")
                .set("font-weight", "700");

        Paragraph desc = new Paragraph(descText);
        desc.getStyle()
                .set("margin", "0")
                .set("color", "#486581")
                .set("font-size", "0.9rem")
                .set("line-height", "1.5");

        card.add(iconBadge, title, desc);
        return card;
    }
}