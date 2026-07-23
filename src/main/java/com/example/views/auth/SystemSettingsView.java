package com.example.views.auth;

import com.example.base.ui.MainLayout;
import com.example.services.SystemSettingService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@PageTitle("Sistem Seçenekleri")
@Route(value = "settings", layout = MainLayout.class)
public class SystemSettingsView extends VerticalLayout {

    public static final String KEY_AFFECTED_NO = "OPTIONAL_FIELD_AFFECTED_NO";
    public static final String KEY_DEADLINE = "OPTIONAL_FIELD_DEADLINE";
    public static final String KEY_SECURITY_RISK = "OPTIONAL_FIELD_SECURITY_RISK";
    public static final String KEY_FINANCIAL_IMPACT = "OPTIONAL_FIELD_FINANCIAL_IMPACT";

    private final SystemSettingService settingService;

    public SystemSettingsView(SystemSettingService settingService) {
        this.settingService = settingService;

        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
        getStyle().set("background", "linear-gradient(135deg, #f0f4f8 0%, #e2e8f0 100%)");

        VerticalLayout card = new VerticalLayout();
        card.setMaxWidth("550px");
        card.setWidthFull();
        card.setSpacing(true);
        card.getStyle()
                .set("background", "white")
                .set("padding", "40px")
                .set("border-radius", "16px")
                .set("box-shadow", "0 15px 30px -5px rgba(0, 102, 204, 0.08), 0 8px 10px -6px rgba(0, 102, 204, 0.04)");

        VerticalLayout headerLayout = new VerticalLayout();
        headerLayout.setPadding(false);
        headerLayout.setSpacing(false);

        H2 title = new H2("Talep Formu Dinamik Alanlar");
        title.getStyle().set("margin", "0").set("color", "#102a43").set("font-weight", "700").set("font-size", "1.6rem");
        
        Paragraph subtitle = new Paragraph("Müşterilerin talep açarken görebileceği opsiyonel parametreleri yönetin.");
        subtitle.getStyle().set("color", "#486581").set("margin", "4px 0 20px 0").set("font-size", "0.95rem");
        
        headerLayout.add(title, subtitle);

        Checkbox affectedNoCheck = new Checkbox("Etkilenen Sayısı / Kişi Alanı Aktif");
        Checkbox deadlineCheck = new Checkbox("Target Deadline (Hedef Tarih) Alanı Aktif");
        Checkbox securityCheck = new Checkbox("KVKK / Güvenlik Riski Alanı Aktif");
        Checkbox financialCheck = new Checkbox("Mali / Finansal Etki Alanı Aktif");

        affectedNoCheck.setValue(settingService.isFeatureEnabled(KEY_AFFECTED_NO, true));
        deadlineCheck.setValue(settingService.isFeatureEnabled(KEY_DEADLINE, true));
        securityCheck.setValue(settingService.isFeatureEnabled(KEY_SECURITY_RISK, true));
        financialCheck.setValue(settingService.isFeatureEnabled(KEY_FINANCIAL_IMPACT, true));

        Button saveBtn = new Button("Ayarları Kaydet", VaadinIcon.SLIDERS.create(), e -> {
            settingService.setFeatureEnabled(KEY_AFFECTED_NO, affectedNoCheck.getValue());
            settingService.setFeatureEnabled(KEY_DEADLINE, deadlineCheck.getValue());
            settingService.setFeatureEnabled(KEY_SECURITY_RISK, securityCheck.getValue());
            settingService.setFeatureEnabled(KEY_FINANCIAL_IMPACT, financialCheck.getValue());

            Notification.show("Sistem seçenekleri güncellendi!", 3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveBtn.setWidthFull();
        saveBtn.getStyle().set("background-color", "#0066cc").set("border-radius", "8px").set("margin-top", "15px");

        card.add(
                headerLayout,
                affectedNoCheck, 
                deadlineCheck, 
                securityCheck, 
                financialCheck, 
                saveBtn
        );

        add(card);
    }
}