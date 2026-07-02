package com.example.views.developer;

import com.example.base.ui.MainLayout;
import com.example.entities.Prioritization;
import com.example.entities.Request;
import com.example.services.PrioritizationService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Route;

@Route(value = "dev/edit", layout = MainLayout.class)
public class DeveloperEditView extends VerticalLayout implements HasUrlParameter<Long> {

    private final PrioritizationService prioritizationService;

    private Long requestId;
    private final H2 titleLabel = new H2("İş Düzenleme Sayfası");
    private final Span detailsContainer = new Span();
    private final TextArea devNotesArea = new TextArea("Not");

    private final Div priorityBadge = new Div();
    private final Div scoreContainer = new Div();

    public DeveloperEditView(PrioritizationService prioritizationService) {
        this.prioritizationService = prioritizationService;

        setSizeFull();
        setPadding(true);
        getStyle().set("background-color", "#f8fafc");

        HorizontalLayout mainSplit = new HorizontalLayout();
        mainSplit.setWidthFull();
        mainSplit.setSpacing(true);

        VerticalLayout infoColumn = new VerticalLayout();
        infoColumn.setWidth("65%");
        infoColumn.setPadding(false);

        detailsContainer.getStyle().set("display", "block").set("width", "100%");
        
        HorizontalLayout visualBadgesRow = new HorizontalLayout();
        visualBadgesRow.setWidthFull();
        visualBadgesRow.setAlignItems(FlexComponent.Alignment.CENTER);

        priorityBadge.getStyle()
                .set("display", "flex")
                .set("align-items", "center")
                .set("justify-content", "center")
                .set("width", "220px")
                .set("height", "100px")
                .set("border-radius", "8px")
                .set("font-weight", "900")
                .set("font-size", "1.2rem");

        scoreContainer.getStyle().set("text-align", "center").set("margin-left", "40px");

        visualBadgesRow.add(priorityBadge, scoreContainer);
        infoColumn.add(titleLabel, detailsContainer, visualBadgesRow);

        VerticalLayout actionsColumn = new VerticalLayout();
        actionsColumn.setWidth("35%");
        actionsColumn.getStyle()
                .set("background", "#eef2f7")
                .set("border-radius", "8px")
                .set("padding", "20px");

        devNotesArea.setPlaceholder("İş hakkında eklemek istediğiniz notları ekleyebilirsiniz...");
        devNotesArea.setWidthFull();
        devNotesArea.setHeight("320px");

        HorizontalLayout btnRow = new HorizontalLayout();
        btnRow.setWidthFull();
        btnRow.setSpacing(true);

        Button completeBtn = new Button("TAMAMLA", e -> completeTaskJob());
        completeBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
        completeBtn.setWidthFull();

        Button sendBackBtn = new Button("GERİ GÖNDER", e -> sendTaskBackUpstream());
        sendBackBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);
        sendBackBtn.setWidthFull();

        btnRow.add(completeBtn, sendBackBtn);
        actionsColumn.add(devNotesArea, btnRow);

        mainSplit.add(infoColumn, actionsColumn);
        add(mainSplit);
    }

    @Override
    public void setParameter(BeforeEvent event, Long parameter) {
        this.requestId = parameter;
        try {
            Prioritization p = prioritizationService.getPrioritizationById(requestId);
            Request r = p.getRequest();

            detailsContainer.getElement().setProperty("innerHTML",
                "<div style='margin-bottom: 25px; font-size: 1.1rem; line-height: 1.6;'>" +
                "<p><b>Talep Başlığı:</b> " + r.getTitle() + "</p>" +
                "<p><b>Talep Açıklaması:</b> " + (r.getDescription() != null ? r.getDescription() : "-") + "</p><hr style='border:0; border-top:1px solid #e2e8f0; margin:20px 0;'/>" +
                "<p style='color:#475569;'><b>İş Etkisi (Impact):</b> " + p.getImpact() + "</p>" +
                "<p style='color:#475569;'><b>Aciliyet (Urgency):</b> " + p.getUrgency() + "</p>" +
                "<p style='color:#475569;'><b>Sorumlu Departman:</b> " + (p.getDepartment() != null ? p.getDepartment().getName() : "-") + "</p>" +
                "</div>"
            );

            scoreContainer.getElement().setProperty("innerHTML",
                "<span style='font-size:0.85rem; color:#64748b; font-weight:bold; display:block;'>HESAPLANAN SKOR</span>" +
                "<span style='font-size:3.5rem; font-weight:900; color:#1e3a8a;'>" + p.getPriorityScore() + "</span>"
            );

            if (p.getPriorityScore() >= 40) {
                priorityBadge.setText("YÜKSEK ÖNCELİKLİ");
                priorityBadge.getStyle().set("background-color", "#fee2e2").set("color", "#991b1b");
            } else if (p.getPriorityScore() >= 20) {
                priorityBadge.setText("ORTA ÖNCELİKLİ");
                priorityBadge.getStyle().set("background-color", "#fef3c7").set("color", "#92400e");
            } else {
                priorityBadge.setText("DÜŞÜK ÖNCELİKLİ");
                priorityBadge.getStyle().set("background-color", "#dcfce7").set("color", "#166534");
            }

        } catch (Exception ex) {
            Notification.show("Bu talep henüz atanmadı veya önceliklendirilmedi!").addThemeVariants(NotificationVariant.LUMO_ERROR);
            UI.getCurrent().navigate("dev/dashboard");
        }
    }

    private void completeTaskJob() {
        prioritizationService.completeDeveloperJob(requestId);
        Notification.show("İş başarıyla tamamlandı!").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        UI.getCurrent().navigate("dev/dashboard");
    }

    private void sendTaskBackUpstream() {
        prioritizationService.returnJobToTeamLeader(requestId);
        Notification.show("İş tekrar değerlendirilmesi için takım liderine geri gönderildi.").addThemeVariants(NotificationVariant.LUMO_WARNING);
        UI.getCurrent().navigate("dev/dashboard");
    }
}