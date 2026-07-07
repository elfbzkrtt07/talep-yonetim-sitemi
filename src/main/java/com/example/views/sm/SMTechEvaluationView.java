package com.example.views.sm;

import com.example.base.ui.MainLayout;
import com.example.entities.Department;
import com.example.entities.Prioritization;
import com.example.entities.Request;
import com.example.entities.User;
import com.example.repositories.UserRepository;
import com.example.services.PrioritizationService;
import com.example.services.RequestService;
import com.example.services.WorkflowService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
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

import java.util.List;
import java.util.Optional;

@Route(value = "sm/evaluate", layout = MainLayout.class)
public class SMTechEvaluationView extends VerticalLayout implements HasUrlParameter<Long> {

    private final RequestService requestService;
    private final PrioritizationService prioritizationService;
    private final UserRepository userRepository;
    private final WorkflowService workflowService;

    private Request targetRequest;
    private Prioritization currentPrioritization;

    private final Span detailsSpan = new Span();
    private final H1 pmScoreDisplay = new H1("0");
    private final Select<Integer> techScoreSelect = new Select<>();
    private final TextArea smCommentArea = new TextArea("Teknik Değerlendirme / Notlar");
    private final Select<User> developerSelect = new Select<>();

    public SMTechEvaluationView(RequestService requestService, 
                                PrioritizationService prioritizationService,
                                UserRepository userRepository,
                                WorkflowService workflowService
                            ) {
        this.requestService = requestService;
        this.prioritizationService = prioritizationService;
        this.userRepository = userRepository;
        this.workflowService = workflowService;

        setSizeFull();
        setPadding(true);
        getStyle().set("background-color", "#f8fafc");

        H2 viewTitle = new H2("Teknik Değerlendirme Penceresi");
        viewTitle.getStyle().set("margin-bottom", "20px");
        add(viewTitle);

        HorizontalLayout workspaceLayout = new HorizontalLayout();
        workspaceLayout.setWidthFull();
        workspaceLayout.setSpacing(true);

        VerticalLayout leftPanel = new VerticalLayout();
        leftPanel.setWidth("65%");
        leftPanel.setPadding(false);

        detailsSpan.getStyle().set("display", "block").set("margin-bottom", "20px");

        techScoreSelect.setLabel("Teknik Puan / Zorluk Skoru (Technical Score)");
        techScoreSelect.setItems(1, 2, 3, 4, 5);
        techScoreSelect.setValue(1);
        techScoreSelect.setWidthFull();

        smCommentArea.setPlaceholder("Mimari zorluklar, bağımlılıklar veya tahmini efor notları giriniz...");
        smCommentArea.setWidthFull();
        smCommentArea.setHeight("100px");

        developerSelect.setLabel("Geliştirici Ata (Select Developer)");
        developerSelect.setPlaceholder("Ekipten bir yazılımcı seçiniz...");
        developerSelect.setWidthFull();
        developerSelect.setItemLabelGenerator(User::getName);

        leftPanel.add(detailsSpan, techScoreSelect, smCommentArea, developerSelect);

        VerticalLayout rightPanel = new VerticalLayout();
        rightPanel.setWidth("35%");
        rightPanel.getStyle().set("background", "#f1f5f9")
                            .set("border-radius", "12px")
                            .set("padding", "30px")
                            .set("text-align", "center");

        Span scoreHeader = new Span("ÜRÜN YÖNETİCİSİ ÖNCELİK SKORU");
        scoreHeader.getStyle().set("font-size", "0.80rem").set("color", "#475569").set("font-weight", "bold");

        pmScoreDisplay.getStyle().set("margin", "20px 0").set("font-size", "4.5rem").set("font-weight", "900").set("color", "#1e3a8a");

        Button submitBtn = new Button("TEKNİK ANALİZİ TAMAMLA", VaadinIcon.PAPERPLANE.create());
        submitBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
        submitBtn.setWidthFull();
        submitBtn.getStyle().set("margin-top", "20px");
        submitBtn.addClickListener(e -> saveTechnicalEvaluationData());

        Button returnBtn = new Button("Ürün Yöneticisine Geri Gönder", VaadinIcon.RECYCLE.create());
        returnBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
        returnBtn.setWidthFull();
        returnBtn.getStyle().set("margin-top", "10px");
        returnBtn.addClickListener(e -> sendWorkflowBackToProductManager());

        rightPanel.add(scoreHeader, pmScoreDisplay, submitBtn, returnBtn);
        workspaceLayout.add(leftPanel, rightPanel);
        add(workspaceLayout);

        Button backBtn = new Button("Geri Dön", VaadinIcon.ARROW_LEFT.create(), e -> UI.getCurrent().navigate("sm/requests"));
        add(backBtn);
    }

    @Override
    public void setParameter(BeforeEvent event, Long parameter) {
        Optional<Request> requestOpt = requestService.getAllRequests().stream()
                .filter(r -> r.getId().equals(parameter))
                .findFirst();

        if (requestOpt.isPresent()) {
            this.targetRequest = requestOpt.get();
            try {
                this.currentPrioritization = prioritizationService.getPrioritizationById(targetRequest.getId());
                populateViewContent();
            } catch (Exception ex) {
                Notification.show("Önceliklendirme kaydı bulunamadığı için teknik analiz başlatılamaz!").addThemeVariants(NotificationVariant.LUMO_ERROR);
                UI.getCurrent().navigate("sm/requests");
            }
        } else {
            Notification.show("Talep kaydı bulunamadı!").addThemeVariants(NotificationVariant.LUMO_ERROR);
            UI.getCurrent().navigate("sm/requests");
        }
    }

    private void populateViewContent() {
        Department activeDept = currentPrioritization.getDepartment();
        String deptNameStr = (activeDept != null) ? activeDept.getName() : "Belirtilmemiş";

        detailsSpan.getElement().setProperty("innerHTML", 
            "<h3><b>Talep Başlığı:</b> " + targetRequest.getTitle() + "</h3>" +
            "<p><b>Açıklama:</b> " + (targetRequest.getDescription() != null ? targetRequest.getDescription() : "-") + "</p>" +
            "<span style='color:#0369a1; font-weight:bold;'>Sorumlu Departman: " + deptNameStr + "</span>"
        );

        pmScoreDisplay.setText(String.valueOf(currentPrioritization.getPriorityScore()));

        if (activeDept != null) {
            List<User> eligibleDevelopers = userRepository.findByRoleAndDepartmentId("DEVELOPER", activeDept.getId());
            developerSelect.setItems(eligibleDevelopers);
        }
    }

    protected void saveTechnicalEvaluationData() {
        User assignedDev = developerSelect.getValue();
        if (assignedDev == null) {
            Notification.show("Lütfen bu iş akışını yönetecek bir geliştirici seçin!").addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        prioritizationService.completeTechnicalEvaluation(
                targetRequest.getId(),
                techScoreSelect.getValue(),
                smCommentArea.getValue().trim(),
                assignedDev
        );

        Notification.show("Teknik analiz kaydedildi ve iş akışı geliştiriciye atandı.").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        UI.getCurrent().navigate("sm/requests");
    }

    private void sendWorkflowBackToProductManager() {
        workflowService.rejectBackToPM(targetRequest.getId(), techScoreSelect.getValue(), smCommentArea.getValue().trim());
        
        Notification.show("Talep revize edilmesi için Ürün Yöneticisine geri gönderildi.")
                .addThemeVariants(NotificationVariant.LUMO_PRIMARY);
        UI.getCurrent().navigate("sm/requests");
    }
}