package com.example.views.customer;

import com.example.services.RequestService;
import com.example.enums.RequestStatus;
import com.example.entities.Request;
import com.example.entities.User;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.example.base.ui.MainLayout;

import java.util.List;
import java.util.Map;

@Route(value = "customer/dashboard", layout = MainLayout.class)
public class CustomerDashboardView extends VerticalLayout implements BeforeEnterObserver {

    private final RequestService requestService;
    private final Grid<Request> requestGrid = new Grid<>(Request.class, false);
    
    private final H2 pageTitle = new H2("Hoşgeldiniz");
    private final Paragraph introParagraph = new Paragraph("Aşağıdaki tablodan mevcut taleplerinizi inceleyebilir, durumlarını takip edebilir veya düzenleyebilirsiniz.");

    public CustomerDashboardView(RequestService requestService) {
        this.requestService = requestService;

        setSizeFull();
        setPadding(true);
        getStyle().set("background-color", "#f8fafc");

        Image logo = new Image(
            com.vaadin.flow.server.streams.DownloadHandler.forClassResource(getClass(), "/META-INF/resources/logo.png"), 
            "MONAD Logo");
        logo.addClassName("center-brand-logo");
        logo.setHeight("70px");
        
        HorizontalLayout logoLayout = new HorizontalLayout(logo);
        logoLayout.setWidthFull();
        logoLayout.setJustifyContentMode(JustifyContentMode.CENTER);

        pageTitle.getStyle()
            .set("font-size", "2.5rem")
            .set("font-weight", "1000")
            .set("margin", "20px auto 10px auto") 
            .set("text-align", "center");
        pageTitle.setWidthFull();

        introParagraph.getStyle()
            .set("color", "#64748b")
            .set("margin", "0 auto 30px auto") 
            .set("text-align", "center");
        introParagraph.setWidthFull();

        HorizontalLayout actionBar = new HorizontalLayout();
        actionBar.setWidthFull();
        actionBar.getStyle().set("padding", "0 40px"); 

        Button createRequestBtn = new Button("Yeni Talep Oluştur", VaadinIcon.PLUS.create());
        createRequestBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        createRequestBtn.getStyle().set("margin-left", "auto");
        createRequestBtn.addClickListener(event -> openNewRequestModal());
        actionBar.add(createRequestBtn);

        requestGrid.addColumn(Request::getId).setHeader("ID").setWidth("80px").setFlexGrow(0);
        requestGrid.addColumn(Request::getTitle).setHeader("Başlık");
        requestGrid.addColumn(Request::getStatus).setHeader("Durum");
        requestGrid.addColumn(Request::getCreatedAt).setHeader("Tarih");
        
        requestGrid.addComponentColumn(request -> {
            Button editBtn = new Button("Düzenle", VaadinIcon.EDIT.create());
            editBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            editBtn.addClickListener(e -> openEditRequestModal(request));
            return editBtn;
        }).setHeader("İşlemler").setWidth("150px").setFlexGrow(0);
        
        requestGrid.setWidth("calc(100% - 80px)"); 
        requestGrid.getStyle().set("margin", "0 auto 20px auto"); 

        add(logoLayout, pageTitle, introParagraph, actionBar, requestGrid);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        User currentUser = (User) VaadinSession.getCurrent().getAttribute("user");
        if (currentUser == null) return;

        Map<String, List<String>> parameters = event.getLocation().getQueryParameters().getParameters();

        if (parameters.containsKey("filter") && "sent_back".equals(parameters.get("filter").get(0))) {
            pageTitle.setText("Geri Dönen Talepleriniz");
            introParagraph.setText("Yöneticiler tarafından revizyon istenen ve aksiyon almanız gereken talepler listelenmektedir.");
            
            List<Request> sentBackRequests = requestService.getAllRequests().stream()
                    .filter(r -> r.getStatus() == RequestStatus.SENT_BACK)
                    .toList();
            requestGrid.setItems(sentBackRequests);
            
        } else {
            pageTitle.setText("Hoşgeldiniz");
            introParagraph.setText("Aşağıdaki tablodan mevcut taleplerinizi inceleyebilir, durumlarını takip edebilir veya düzenleyebilirsiniz.");
            requestGrid.setItems(requestService.getAllRequests());
        }

        if (parameters.containsKey("action") && "new".equals(parameters.get("action").get(0))) {
            UI.getCurrent().access(this::openNewRequestModal);
        }
    }

    private void openNewRequestModal() {
        Dialog modal = new Dialog();
        modal.setHeaderTitle("Yeni Destek/Geliştirme Talebi Oluştur");
        modal.setCloseOnOutsideClick(false);

        TextField titleField = new TextField("Talep Başlığı");
        titleField.setWidthFull();
        titleField.setPlaceholder("Lütfen kısa bir başlık yazın...");

        com.vaadin.flow.component.textfield.IntegerField affectedNoField = new com.vaadin.flow.component.textfield.IntegerField("Etkilenen Sayısı / No");
        affectedNoField.setWidthFull();
        affectedNoField.setMin(0);
        affectedNoField.setPlaceholder("Kaç kişi/sistem etkileniyor?");

        TextArea descriptionField = new TextArea("Talep Detayı ve Açıklama");
        descriptionField.setWidthFull();
        descriptionField.setHeight("150px");
        descriptionField.setPlaceholder("Talebinizin detaylarını buraya girin...");

        com.vaadin.flow.component.upload.receivers.MemoryBuffer buffer = new com.vaadin.flow.component.upload.receivers.MemoryBuffer();
        com.vaadin.flow.component.upload.Upload uploadField = new com.vaadin.flow.component.upload.Upload(buffer);
        uploadField.setMaxFileSize(5 * 1024 * 1024);
        uploadField.setUploadButton(new Button("Dosya Ekle", VaadinIcon.UPLOAD.create()));
        uploadField.setDropLabel(new Paragraph("Veya dosyayı buraya sürükleyin"));

        VerticalLayout modalBody = new VerticalLayout(titleField, affectedNoField, descriptionField, uploadField);
        modalBody.setPadding(false);
        modalBody.setSpacing(true);
        modalBody.setWidth("500px");

        Button cancelBtn = new Button("İptal Et", e -> modal.close());
        Button submitBtn = new Button("Oluştur", VaadinIcon.PAPERPLANE.create());
        submitBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        submitBtn.addClickListener(e -> {
            String title = titleField.getValue().trim();
            String description = descriptionField.getValue().trim();
            Integer affectedNo = affectedNoField.getValue();

            if (title.isEmpty() || description.isEmpty() || affectedNo == null) {
                com.vaadin.flow.component.notification.Notification.show("Lütfen tüm alanları doldurun!")
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }
            
            User currentUser = (User) VaadinSession.getCurrent().getAttribute("user");
            if (currentUser == null) {
                com.vaadin.flow.component.notification.Notification.show("Oturum zaman aşımına uğradı. Yeniden giriş yapın.")
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            Request newRequest = new Request(currentUser, title, description, affectedNo);
            
            if (!buffer.getFileName().isEmpty()) {
                try {
                    newRequest.setFileName(buffer.getFileName());
                    newRequest.setFileData(buffer.getInputStream().readAllBytes());
                } catch (java.io.IOException ex) {
                    com.vaadin.flow.component.notification.Notification.show("Dosya okunamadı: " + ex.getMessage())
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                    return;
                }
            }

            requestService.submitRequest(newRequest); 
            com.vaadin.flow.component.notification.Notification.show("Talep başarıyla oluşturuldu.")
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            
            modal.close();
            refreshGridItems(); 
        });

        modal.add(modalBody);
        cancelBtn.getStyle().set("margin-left", "auto"); 
        modal.getFooter().add(cancelBtn, submitBtn);
        modal.open();
    }

    private void openEditRequestModal(Request request) {
        Dialog modal = new Dialog();
        modal.setHeaderTitle("Destek/Geliştirme Talebini Düzenle");
        modal.setCloseOnOutsideClick(false);

        TextField titleField = new TextField("Talep Başlığı");
        titleField.setWidthFull();
        titleField.setValue(request.getTitle() != null ? request.getTitle() : "");

        com.vaadin.flow.component.textfield.IntegerField affectedNoField = new com.vaadin.flow.component.textfield.IntegerField("Etkilenen Sayısı / No");
        affectedNoField.setWidthFull();
        affectedNoField.setMin(0);
        affectedNoField.setValue(request.getAffectedNo() != null ? request.getAffectedNo() : 0);

        TextArea descriptionField = new TextArea("Talep Detayı ve Açıklama");
        descriptionField.setWidthFull();
        descriptionField.setHeight("150px");
        descriptionField.setValue(request.getDescription() != null ? request.getDescription() : "");

        HorizontalLayout downloadSection = new HorizontalLayout();
        downloadSection.setWidthFull();
        downloadSection.setAlignItems(Alignment.CENTER);

        if (request.getFileName() != null && !request.getFileName().isEmpty() && request.getFileData() != null) {
            com.vaadin.flow.server.StreamResource streamResource = new com.vaadin.flow.server.StreamResource(
                request.getFileName(), 
                () -> new java.io.ByteArrayInputStream(request.getFileData())
            );
            com.vaadin.flow.component.html.Anchor downloadLink = new com.vaadin.flow.component.html.Anchor(streamResource, "");
            downloadLink.getElement().setAttribute("download", true); 

            Button downloadBtn = new Button("Mevcut Dosyayı İndir (" + request.getFileName() + ")", VaadinIcon.DOWNLOAD.create());
            downloadBtn.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_TERTIARY);
            downloadLink.add(downloadBtn);
            downloadSection.add(downloadLink);
        } else {
            com.vaadin.flow.component.html.Span noFileSpan = new com.vaadin.flow.component.html.Span("Ekli dosya bulunmuyor.");
            noFileSpan.getStyle().set("color", "var(--lumo-secondary-text-color)");
            downloadSection.add(noFileSpan);
        }

        com.vaadin.flow.component.upload.receivers.MemoryBuffer buffer = new com.vaadin.flow.component.upload.receivers.MemoryBuffer();
        com.vaadin.flow.component.upload.Upload uploadField = new com.vaadin.flow.component.upload.Upload(buffer);
        uploadField.setMaxFileSize(5 * 1024 * 1024);
        
        if (request.getFileName() != null && !request.getFileName().isEmpty()) {
            uploadField.setUploadButton(new Button("Dosyayı Değiştir", VaadinIcon.UPLOAD.create()));
        } else {
            uploadField.setUploadButton(new Button("Dosya Ekle", VaadinIcon.UPLOAD.create()));
        }
        uploadField.setDropLabel(new Paragraph("Yeni bir dosya sürükleyerek eskisini değiştirebilirsiniz"));

        VerticalLayout modalBody = new VerticalLayout(titleField, affectedNoField, descriptionField, downloadSection, uploadField);
        modalBody.setPadding(false);
        modalBody.setSpacing(true);
        modalBody.setWidth("500px");

        Button deleteBtn = new Button("Talebi Sil", VaadinIcon.TRASH.create(), e -> {
            requestService.deleteRequest(request);
            com.vaadin.flow.component.notification.Notification.show("Talep başarıyla silindi.")
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            modal.close();
            refreshGridItems();
        });
        deleteBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);

        Button cancelBtn = new Button("İptal Et", e -> modal.close());
        Button submitBtn = new Button("Güncelle", VaadinIcon.CHECK.create());
        submitBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        submitBtn.addClickListener(e -> {
            String title = titleField.getValue().trim();
            String description = descriptionField.getValue().trim();
            Integer affectedNo = affectedNoField.getValue();

            if (title.isEmpty() || description.isEmpty() || affectedNo == null) {
                com.vaadin.flow.component.notification.Notification.show("Lütfen tüm alanları doldurun!")
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            request.setTitle(title);
            request.setDescription(description);
            request.setAffectedNo(affectedNo);
            request.setUpdatedAt(java.time.LocalDateTime.now()); 

            if (!buffer.getFileName().isEmpty()) {
                try {
                    request.setFileName(buffer.getFileName());
                    request.setFileData(buffer.getInputStream().readAllBytes());
                } catch (java.io.IOException ex) {
                    com.vaadin.flow.component.notification.Notification.show("Dosya güncellenirken hata oluştu: " + ex.getMessage())
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                    return;
                }
            }

            requestService.updateRequest(request);
            com.vaadin.flow.component.notification.Notification.show("Talep başarıyla güncellendi.")
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            
            modal.close();
            refreshGridItems(); 
        });

        modal.add(modalBody);
        cancelBtn.getStyle().set("margin-left", "auto"); 
        modal.getFooter().add(deleteBtn, cancelBtn, submitBtn);
        modal.open();
    }

    private void refreshGridItems() {
        if ("Geri Dönen Talepleriniz".equals(pageTitle.getText())) {
            requestGrid.setItems(requestService.getAllRequests().stream()
                    .filter(r -> r.getStatus() == RequestStatus.SENT_BACK)
                    .toList());
        } else {
            requestGrid.setItems(requestService.getAllRequests());
        }
    }
}

// sonar küp