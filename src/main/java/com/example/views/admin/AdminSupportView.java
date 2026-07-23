package com.example.views.admin;

import com.example.base.ui.MainLayout;
import com.example.entities.SupportRequest;
import com.example.enums.RequestStatus;
import com.example.services.SupportRequestService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.time.format.DateTimeFormatter;
import java.util.List;

@PageTitle("Destek Talepleri")
@Route(value = "admin/support", layout = MainLayout.class)
public class AdminSupportView extends VerticalLayout implements BeforeEnterObserver {

    private final SupportRequestService supportRequestService;
    private final Grid<SupportRequest> grid = new Grid<>(SupportRequest.class, false);
    
    private final TextField searchField = new TextField();
    private final ComboBox<RequestStatus> statusFilter = new ComboBox<>();
    
    private static final String USER_APPROVAL_PREFIX = "[KULLANICI ONAYI]";
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public AdminSupportView(SupportRequestService supportRequestService) {
        this.supportRequestService = supportRequestService;

        setSizeFull();
        setPadding(true);
        getStyle().set("background-color", "#f8fafc");

        VerticalLayout headerBanner = createHeaderBanner("Destek Talepleri");
        
        Paragraph desc = new Paragraph("Kullanıcılar tarafından iletilen sorunlar ve destek talepleri.");
        desc.getStyle()
                .set("color", "#64748b")
                .set("text-align", "center")
                .set("margin", "0 auto 25px auto");

        searchField.setPlaceholder("Gönderen veya Konu ara...");
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.addValueChangeListener(e -> applyFilters());
        searchField.setWidth("300px");

        statusFilter.setPlaceholder("Durum Filtrele");
        statusFilter.setItems(RequestStatus.PENDING, RequestStatus.COMPLETED, RequestStatus.REJECTED);
        statusFilter.setItemLabelGenerator(this::getStatusLabel);
        statusFilter.setClearButtonVisible(true);
        statusFilter.addValueChangeListener(e -> applyFilters());
        statusFilter.setWidth("200px");

        HorizontalLayout toolbar = new HorizontalLayout(searchField, statusFilter);
        toolbar.setWidth("calc(100% - 40px)");
        toolbar.setAlignItems(Alignment.CENTER);
        toolbar.getStyle().set("margin", "0 auto 10px auto");

        grid.addColumn(SupportRequest::getId).setHeader("ID").setWidth("80px").setFlexGrow(0);
        
        grid.addColumn(request -> {
            if (request.getSender() != null) {
                return request.getSender().getName();
            }
            return "Belirtilmemiş";
        }).setHeader("Gönderen");

        grid.addColumn(SupportRequest::getSubject).setHeader("Konu").setFlexGrow(2);
        
        grid.addColumn(request -> request.getCreatedAt() != null ? request.getCreatedAt().format(formatter) : "-")
            .setHeader("Tarih");
            
        grid.addColumn(request -> getStatusLabel(request.getStatus())).setHeader("Durum");

        grid.addComponentColumn(request -> {
            Button inspectBtn = new Button("İncele", VaadinIcon.SEARCH.create());
            inspectBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            inspectBtn.addClickListener(e -> openRequestDetailsDialog(request));
            return inspectBtn;
        }).setHeader("İşlem").setWidth("130px").setFlexGrow(0);

        grid.setWidth("calc(100% - 40px)");
        grid.getStyle().set("margin", "0 auto");
        grid.getStyle().set("border", "1px solid #cbd5e1").set("border-radius", "8px");

        add(headerBanner, desc, toolbar, grid);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        applyFilters();
    }

    private void applyFilters() {
        String searchTerm = searchField.getValue() != null ? searchField.getValue().trim().toLowerCase() : "";
        RequestStatus selectedStatus = statusFilter.getValue();

        List<SupportRequest> supportList = supportRequestService.getAllSupportRequests().stream()
                .filter(req -> req.getSubject() == null || !req.getSubject().startsWith(USER_APPROVAL_PREFIX))
                .filter(req -> {
                    boolean matchesSearch = searchTerm.isEmpty() ||
                            (req.getSender() != null && req.getSender().getName() != null && req.getSender().getName().toLowerCase().contains(searchTerm)) ||
                            (req.getSubject() != null && req.getSubject().toLowerCase().contains(searchTerm));

                    boolean matchesStatus = (selectedStatus == null) || (req.getStatus() == selectedStatus);

                    return matchesSearch && matchesStatus;
                })
                .toList();
                
        grid.setItems(supportList);
    }

    private void openRequestDetailsDialog(SupportRequest request) {
        Dialog dialog = new Dialog();
        dialog.setWidth("500px");
        dialog.setHeaderTitle("Destek Talebi Detayı");

        TextField senderField = new TextField("Gönderen");
        senderField.setValue(request.getSender() != null ? request.getSender().getName() : "Bilinmiyor");
        senderField.setReadOnly(true);
        senderField.setWidthFull();

        TextField dateField = new TextField("Tarih");
        dateField.setValue(request.getCreatedAt() != null ? request.getCreatedAt().format(formatter) : "-");
        dateField.setReadOnly(true);
        dateField.setWidthFull();

        HorizontalLayout row1 = new HorizontalLayout(senderField, dateField);
        row1.setWidthFull();

        TextField subjectField = new TextField("Konu");
        subjectField.setValue(request.getSubject() != null ? request.getSubject() : "");
        subjectField.setReadOnly(true);
        subjectField.setWidthFull();

        TextArea messageArea = new TextArea("Mesaj / Detay");
        messageArea.setValue(request.getMessage() != null ? request.getMessage() : "");
        messageArea.setReadOnly(true);
        messageArea.setWidthFull();
        messageArea.setMinHeight("150px");

        Button closeBtn = new Button("Kapat", e -> dialog.close());
        closeBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        Button resolveBtn = new Button("Tamamla", VaadinIcon.CHECK.create());
        resolveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
        resolveBtn.setEnabled(request.getStatus() != RequestStatus.COMPLETED);
        resolveBtn.addClickListener(e -> {
            request.setStatus(RequestStatus.COMPLETED);
            supportRequestService.saveSupportRequest(request);
            
            Notification.show("Talep çözüldü olarak işaretlendi!").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            applyFilters();
            dialog.close();
        });

        Button rejectButton = new Button("Reddet", VaadinIcon.CLOSE.create());
        rejectButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);
        rejectButton.setEnabled(request.getStatus() != RequestStatus.COMPLETED);
        rejectButton.addClickListener(e -> {
            request.setStatus(RequestStatus.REJECTED);
            supportRequestService.saveSupportRequest(request);
            
            Notification.show("Talep başarıyla reddedildi!").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            applyFilters();
            dialog.close();
        });

        HorizontalLayout footer = new HorizontalLayout(closeBtn, resolveBtn, rejectButton);
        footer.setWidthFull();
        footer.setJustifyContentMode(JustifyContentMode.END);

        VerticalLayout dialogLayout = new VerticalLayout(row1, subjectField, messageArea);
        dialogLayout.setPadding(false);
        dialogLayout.setSpacing(true);

        dialog.add(dialogLayout);
        dialog.getFooter().add(footer);
        dialog.open();
    }

    private String getStatusLabel(RequestStatus status) {
        if (status == null) return "-";
        switch (status) {
            case PENDING:
                return "Bekleniyor";
            case COMPLETED:
                return "Tamamlandı";
            case REJECTED:
                    return "Reddedildi";
            default:
                return status.toString();
        }
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