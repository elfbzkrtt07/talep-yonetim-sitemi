package com.example.views.pm;

import com.example.base.ui.MainLayout;
import com.example.entities.Department; 
import com.example.entities.Prioritization;
import com.example.entities.Request;
import com.example.enums.RequestStatus;
import com.example.enums.TaskType;
import com.example.repositories.DepartmentRepository;
import com.example.services.PrioritizationService;
import com.example.services.RequestService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Route;

import java.util.Optional;

@Route(value = "pm/inspect", layout = MainLayout.class)
public class PMInspectView extends VerticalLayout implements HasUrlParameter<Long> {

    private final RequestService requestService;
    private final PrioritizationService prioritizationService;
    private final DepartmentRepository departmentRepository;
    private Request targetRequest;
    
    private boolean isUpdatingUi = false;

    private final Span detailsSpan = new Span();
    private final Select<Integer> impactSelect = new Select<>();
    private final Select<Integer> urgencySelect = new Select<>();
    private final Select<TaskType> typeSelect = new Select<>();
    
    private final Select<Department> departmentField = new Select<>();
    
    private final H1 scoreValue = new H1("0");
    private final Span priorityBadge = new Span("DÜŞÜK ÖNCELİKLİ");

    public PMInspectView(RequestService requestService, 
                          PrioritizationService prioritizationService,
                          DepartmentRepository departmentRepository) {
        this.requestService = requestService;
        this.prioritizationService = prioritizationService;
        this.departmentRepository = departmentRepository;

        setSizeFull();
        setPadding(true);
        getStyle().set("background-color", "#f8fafc");

        H2 viewTitle = new H2("Talep İnceleme Sayfası");
        viewTitle.getStyle().set("margin-bottom", "20px");
        add(viewTitle);

        HorizontalLayout workspaceLayout = new HorizontalLayout();
        workspaceLayout.setWidthFull();
        workspaceLayout.setSpacing(true);

        VerticalLayout leftPanel = new VerticalLayout();
        leftPanel.setWidth("65%");
        leftPanel.setPadding(false);

        detailsSpan.getStyle().set("display", "block").set("margin-bottom", "20px");

        impactSelect.setLabel("İş Etkisi (Impact)");
        impactSelect.setItems(1, 2, 3, 4, 5);
        impactSelect.setValue(1);
        impactSelect.setWidthFull();

        urgencySelect.setLabel("Aciliyet (Urgency)");
        urgencySelect.setItems(1, 2, 3, 4, 5);
        urgencySelect.setValue(1);
        urgencySelect.setWidthFull();

        typeSelect.setLabel("İş Tipi (Task Type)");
        typeSelect.setItems(TaskType.values());
        if (TaskType.values().length > 0) typeSelect.setValue(TaskType.values()[0]);
        typeSelect.setWidthFull();

        departmentField.setLabel("İlgili Departman");
        departmentField.setPlaceholder("Ekiplerden birini seçiniz (A, B, C)...");
        departmentField.setWidthFull();
        departmentField.setItems(departmentRepository.findAll());
        departmentField.setItemLabelGenerator(Department::getName);
        leftPanel.add(detailsSpan, impactSelect, urgencySelect, typeSelect, departmentField);

        VerticalLayout rightPanel = new VerticalLayout();
        rightPanel.setWidth("35%");
        rightPanel.getStyle().set("background", "#f1f5f9")
                            .set("border-radius", "12px")
                            .set("padding", "30px")
                            .set("text-align", "center");

        Span scoreHeader = new Span("HESAPLANAN SKOR");
        scoreHeader.getStyle().set("font-size", "0.85rem").set("color", "#64748b").set("font-weight", "bold");

        scoreValue.getStyle().set("margin", "15px 0").set("font-size", "4rem").set("font-weight", "bold");

        priorityBadge.getStyle().set("padding", "8px 16px").set("border-radius", "20px")
                             .set("font-weight", "bold").set("font-size", "0.85rem");

        Button saveBtn = new Button("KAYDET / ONAYLA", VaadinIcon.CHECK.create());
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveBtn.setWidthFull();
        saveBtn.getStyle().set("margin-top", "20px");
        saveBtn.addClickListener(e -> savePrioritizationData());

        rightPanel.add(scoreHeader, scoreValue, priorityBadge, saveBtn);
        workspaceLayout.add(leftPanel, rightPanel);
        add(workspaceLayout);

        HorizontalLayout actionRow = new HorizontalLayout();
        actionRow.setWidthFull();
        actionRow.getStyle().set("margin-top", "20px");

        Button backBtn = new Button("Geri Dön", VaadinIcon.ARROW_LEFT.create(), e -> UI.getCurrent().navigate("pm/requests"));
        
        Button rejectBtn = new Button("Müşteriye Geri Gönder", VaadinIcon.ARROW_BACKWARD.create(), e -> openRejectReasonDialog());
        rejectBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
        rejectBtn.getStyle().set("margin-left", "auto");

        actionRow.add(backBtn, rejectBtn);
        add(actionRow);

        impactSelect.addValueChangeListener(e -> {
            if (!isUpdatingUi) refreshCalculatedScoreText();
        });
        urgencySelect.addValueChangeListener(e -> {
            if (!isUpdatingUi) refreshCalculatedScoreText();
        });
    }

    @Override
    public void setParameter(BeforeEvent event, Long parameter) {
        Optional<Request> requestOpt = requestService.getAllRequests().stream()
                .filter(r -> r.getId().equals(parameter))
                .findFirst();

        if (requestOpt.isPresent()) {
            this.targetRequest = requestOpt.get();
            updateUiContent();
        } else {
            Notification.show("Talep kaydı bulunamadı!").addThemeVariants(NotificationVariant.LUMO_ERROR);
            UI.getCurrent().navigate("pm/requests");
        }
    }

    private void updateUiContent() {
        isUpdatingUi = true;

        detailsSpan.getElement().setProperty("innerHTML", 
            "<h3><b>Talep Başlığı:</b> " + targetRequest.getTitle() + "</h3>" +
            "<p style='color:#475569;'><b>Açıklama:</b> " + (targetRequest.getDescription() != null ? targetRequest.getDescription() : "-") + "</p>" +
            "<span style='font-size:0.9rem; color:#64748b;'><b>Etkilenen Sayısı:</b> " + (targetRequest.getAffectedNo() != null ? targetRequest.getAffectedNo() : "Belirtilmemiş") + "</span>"
        );

        try {
            Prioritization existingPrior = prioritizationService.getPrioritizationById(targetRequest.getId());
            
            impactSelect.setValue(existingPrior.getImpact());
            urgencySelect.setValue(existingPrior.getUrgency());
            typeSelect.setValue(existingPrior.getTaskType());
            
            departmentField.setValue(existingPrior.getDepartment());
            
            updateScoreBadgeVisuals(existingPrior.getPriorityScore());
            
        } catch (Exception ex) {
            impactSelect.setValue(1);
            urgencySelect.setValue(1);
            if (TaskType.values().length > 0) typeSelect.setValue(TaskType.values()[0]);
            
            departmentField.setValue(null);
            
            updateScoreBadgeVisuals(4);
        } finally {
            isUpdatingUi = false;
        }
    }

    private void refreshCalculatedScoreText() {
        int impact = impactSelect.getValue();
        int urgency = urgencySelect.getValue();
        int score = impact * urgency * 4;
        updateScoreBadgeVisuals(score);
    }

    private void updateScoreBadgeVisuals(int score) {
        scoreValue.setText(String.valueOf(score));

        if (score >= 60) {
            priorityBadge.setText("YÜKSEK ÖNCELİKLİ");
            priorityBadge.getStyle().set("background", "#fee2e2").set("color", "#ef4444");
        } else if (score >= 24) {
            priorityBadge.setText("ORTA ÖNCELİKLİ");
            priorityBadge.getStyle().set("background", "#fef3c7").set("color", "#d97706");
        } else {
            priorityBadge.setText("DÜŞÜK ÖNCELİKLİ");
            priorityBadge.getStyle().set("background", "#dcfce7").set("color", "#22c55e");
        }
    }

    private void savePrioritizationData() {
        Department chosenDept = departmentField.getValue();
        if (chosenDept == null) {
            Notification.show("Lütfen ilgili departmanı belirtin!").addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        targetRequest.setStatus(RequestStatus.APPROVED);
        int calculatedScoreInt = Integer.parseInt(scoreValue.getText());
        
        Prioritization prioritization = new Prioritization(
                targetRequest,
                urgencySelect.getValue(),
                impactSelect.getValue(),
                typeSelect.getValue(),
                chosenDept,
                calculatedScoreInt
        );
        prioritization.setId(targetRequest.getId());

        requestService.approveAndPrioritizeRequest(targetRequest, prioritization);
        Notification.show("Talep başarıyla onaylandı ve öncelik havuzuna işlendi.").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        UI.getCurrent().navigate("pm/requests");
    }

    private void openRejectReasonDialog() {
        Dialog rejectDialog = new Dialog();
        rejectDialog.setHeaderTitle("Müşteriye İade Gerekçesi");

        TextArea feedbackArea = new TextArea("Revizyon / Eksik Bilgi Notu");
        feedbackArea.setPlaceholder("Lütfen müşterinin neyi düzeltmesi gerektiğini detaylıca yazın...");
        feedbackArea.setWidth("400px");
        feedbackArea.setHeight("120px");
        rejectDialog.add(feedbackArea);

        Button submitReject = new Button("İade Et", e -> {
            String note = feedbackArea.getValue().trim();
            if (note.isEmpty()) {
                Notification.show("Gerekçe boş bırakılamaz!").addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            targetRequest.setStatus(RequestStatus.SENT_BACK);
            requestService.updateRequest(targetRequest);
            Notification.show("Talep müşteriye geri gönderildi.").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            
            rejectDialog.close();
            UI.getCurrent().navigate("pm/requests");
        });
        submitReject.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);

        rejectDialog.getFooter().add(new Button("İptal", c -> rejectDialog.close()), submitReject);
        rejectDialog.open();
    }
}