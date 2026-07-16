package com.example.views.pm;

import com.example.base.ui.MainLayout;
import com.example.entities.Prioritization;
import com.example.enums.WorkflowStatus;
import com.example.services.PrioritizationService;
import com.example.services.RequestService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import jakarta.persistence.EntityManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

@Route(value = "pm/prioritizations", layout = MainLayout.class)
public class PMPrioritizationsView extends VerticalLayout implements BeforeEnterObserver {

    private final PrioritizationService prioritizationService;
    private final RequestService requestService;
    private final EntityManager entityManager;
    private final Grid<Prioritization> prioritizationGrid = new Grid<>(Prioritization.class, false);

    private final TransactionTemplate transactionTemplate;

    public PMPrioritizationsView(PrioritizationService prioritizationService, 
                                 RequestService requestService, 
                                 EntityManager entityManager,
                                 TransactionTemplate transactionTemplate) {
        this.prioritizationService = prioritizationService;
        this.requestService = requestService;
        this.entityManager = entityManager;
        this.transactionTemplate = transactionTemplate;

        setSizeFull();
        setPadding(true);
        getStyle().set("background-color", "#f8fafc");

        VerticalLayout headerBanner = createHeaderBanner("Önceliklendirme Havuzu");
        
        Paragraph desc = new Paragraph("Onaylanan talepler, hesaplanan öncelik skorlarına göre yüksekten düşüğe doğru sıralanmıştır.");
        desc.getStyle()
                .set("color", "#64748b")
                .set("text-align", "center")
                .set("margin", "0 auto 25px auto");

        prioritizationGrid.addColumn(p -> p.getId() != null ? p.getId() : "-")
                .setHeader("ID").setWidth("80px").setFlexGrow(0);
        
        prioritizationGrid.addColumn(p -> {
            try {
                if (p.getRequest() != null && p.getRequest().getCustomer() != null) {
                    return p.getRequest().getCustomer().getName();
                }
            } catch (Exception ex) {
                return "Müşteri Bilgisi Yüklenemedi"; 
            }
            return "-";
        }).setHeader("Müşteri");
        
        prioritizationGrid.addColumn(p -> {
            try {
                if (p.getRequest() != null) {
                    return p.getRequest().getTitle();
                }
            } catch (Exception ex) {
                return "Başlık Yüklenemedi";
            }
            return "-";
        }).setHeader("Başlık");
        
        prioritizationGrid.addColumn(prioritization -> prioritization.getDepartment() != null ? prioritization.getDepartment().getName() : "").setHeader("Departman");
        prioritizationGrid.addColumn(Prioritization::getTaskType).setHeader("İş Tipi");
        prioritizationGrid.addColumn(Prioritization::getPriorityScore)
                .setHeader("Skor")
                .setWidth("100px").setFlexGrow(0);

        prioritizationGrid.addComponentColumn(prioritization -> {
            HorizontalLayout actionWrapper = new HorizontalLayout();
            actionWrapper.setSpacing(true);

            Button detailBtn = new Button("İncele", VaadinIcon.EYE.create());
            detailBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            detailBtn.addClickListener(e -> {
                getUI().ifPresent(ui -> ui.navigate("pm/inspect/" + prioritization.getId()));
            });

            Button convertBtn = new Button("İş Akışına Çevir", VaadinIcon.CONNECT.create());
            convertBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
            
            convertBtn.addClickListener(e -> {
                transactionTemplate.execute(status -> {
                    requestService.convertPrioritizationToWorkflow(prioritization);

                    entityManager.createQuery(
                        "UPDATE Workflow w SET w.status = :newStatus WHERE w.request.id = :reqId")
                        .setParameter("newStatus", WorkflowStatus.APPROVED_BY_PM)
                        .setParameter("reqId", prioritization.getId())
                        .executeUpdate();

                    return null;
                });

                Notification.show("Talep başarıyla iş akışına dönüştürüldü.")
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                refreshGridData();
            });

            actionWrapper.add(detailBtn, convertBtn);
            return actionWrapper;
        }).setHeader("İşlemler").setWidth("300px").setFlexGrow(0);

        prioritizationGrid.setWidth("calc(100% - 40px)");
        prioritizationGrid.getStyle().set("margin", "0 auto");

        add(headerBanner, desc, prioritizationGrid);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        refreshGridData();
    }

    private void refreshGridData() {
        List<Prioritization> activeBacklog = prioritizationService.getAllUnconvertedpPrioritizations();
        activeBacklog.sort((p1, p2) -> p2.getPriorityScore().compareTo(p1.getPriorityScore()));
        prioritizationGrid.setItems(activeBacklog);
    }

    private VerticalLayout createHeaderBanner(String titleText) {
        VerticalLayout bannerLayout = new VerticalLayout();
        bannerLayout.setWidthFull();
        bannerLayout.setAlignItems(Alignment.CENTER); 
        bannerLayout.setJustifyContentMode(JustifyContentMode.CENTER);
        bannerLayout.setPadding(false);
        bannerLayout.getStyle()
                .set("margin-top", "25px")
                .set("margin-bottom", "10px");

        H2 title = new H2(titleText);
        title.getStyle()
                .set("margin", "0")
                .set("font-size", "2.5rem") 
                .set("font-weight", "1000") 
                .set("color", "#0f172a")
                .set("text-align", "center");

        bannerLayout.add(title);
        return bannerLayout;
    }
}