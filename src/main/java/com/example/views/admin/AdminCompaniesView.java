package com.example.views.admin;

import com.example.base.ui.MainLayout;
import com.example.entities.Company;
import com.example.entities.Department;
import com.example.services.CompanyService;
import com.example.services.DepartmentService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@PageTitle("Şirket ve Departman Yönetimi")
@Route(value = "admin/companies", layout = MainLayout.class)
public class AdminCompaniesView extends VerticalLayout {

    private final CompanyService companyService;
    private final DepartmentService departmentService;
    
    private final Grid<Company> companyGrid = new Grid<>(Company.class, false);
    private final Grid<Department> deptGrid = new Grid<>(Department.class, false);

    public AdminCompaniesView(CompanyService companyService, DepartmentService departmentService) {
        this.companyService = companyService;
        this.departmentService = departmentService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);
        getStyle().set("background-color", "#f8fafc");

        add(createHeaderBanner("Şirket ve Departman Yönetimi", "Sistemde kayıtlı müşteri şirketlerini ve departmanları yönetin."));

        HorizontalLayout mainSplit = new HorizontalLayout();
        mainSplit.setSizeFull();
        mainSplit.setSpacing(true);

        VerticalLayout companyColumn = createCardLayout();
        
        HorizontalLayout companyToolbar = new HorizontalLayout(new H3("Şirketler"));
        companyToolbar.setWidthFull();
        companyToolbar.setAlignItems(Alignment.CENTER);
        
        Button addCompanyBtn = new Button("Yeni Şirket", VaadinIcon.PLUS.create());
        addCompanyBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addCompanyBtn.addClickListener(e -> openCompanyDialog(new Company()));
        companyToolbar.addAndExpand(new Span()); 
        companyToolbar.add(addCompanyBtn);

        setupCompanyGrid();
        companyColumn.add(companyToolbar, companyGrid);

        VerticalLayout deptColumn = createCardLayout();
        
        HorizontalLayout deptToolbar = new HorizontalLayout(new H3("Departmanlar"));
        deptToolbar.setWidthFull();
        deptToolbar.setAlignItems(Alignment.CENTER);
        
        Button addDeptBtn = new Button("Yeni Departman", VaadinIcon.PLUS.create());
        addDeptBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addDeptBtn.addClickListener(e -> openDepartmentDialog(new Department()));
        deptToolbar.addAndExpand(new Span()); 
        deptToolbar.add(addDeptBtn);

        setupDepartmentGrid();
        deptColumn.add(deptToolbar, deptGrid);

        mainSplit.add(companyColumn, deptColumn);
        mainSplit.setFlexGrow(1, companyColumn, deptColumn);
        add(mainSplit);

        refreshGrids();
    }

    private VerticalLayout createCardLayout() {
        VerticalLayout card = new VerticalLayout();
        card.setSizeFull();
        card.setPadding(true);
        card.setSpacing(true);
        card.getStyle()
            .set("background", "white")
            .set("border", "1px solid #cbd5e1")
            .set("border-radius", "8px")
            .set("box-shadow", "0 1px 3px 0 rgba(0, 0, 0, 0.1)");
        return card;
    }

    private VerticalLayout createHeaderBanner(String titleText, String subtitleText) {
        VerticalLayout bannerLayout = new VerticalLayout();
        bannerLayout.setWidthFull();
        bannerLayout.setAlignItems(Alignment.CENTER); 
        bannerLayout.setJustifyContentMode(JustifyContentMode.CENTER);
        bannerLayout.setPadding(false);
        bannerLayout.setSpacing(true);
        bannerLayout.getStyle()
                .set("margin-top", "25px")
                .set("margin-bottom", "15px");

        H2 title = new H2(titleText);
        title.getStyle()
                .set("margin", "0")
                .set("font-size", "2.5rem") 
                .set("font-weight", "1000") 
                .set("color", "#0f172a")
                .set("text-align", "center");
        bannerLayout.add(title);

        if (subtitleText != null && !subtitleText.isEmpty()) {
            Span subtitle = new Span(subtitleText);
            subtitle.getStyle()
                    .set("margin-top", "8px") 
                    .set("font-size", "0.9rem")
                    .set("color", "#64748b")
                    .set("text-align", "center");
            bannerLayout.add(subtitle);
        }

        return bannerLayout;
    }

    private void setupCompanyGrid() {
        companyGrid.setSizeFull();
        companyGrid.addColumn(Company::getId).setHeader("ID").setAutoWidth(true).setFlexGrow(0);
        companyGrid.addColumn(Company::getName).setHeader("Şirket Adı").setFlexGrow(1);
        
        companyGrid.addComponentColumn(company -> {
            boolean active = company.isActive();
            Span badge = new Span(active ? "Aktif" : "Pasif");
            badge.getStyle()
                 .set("color", active ? "#15803d" : "#b45309")
                 .set("background-color", active ? "#dcfce7" : "#fef3c7")
                 .set("padding", "4px 8px")
                 .set("border-radius", "12px")
                 .set("font-weight", "500")
                 .set("font-size", "0.85rem");
            return badge;
        }).setHeader("Durum").setAutoWidth(true);

        companyGrid.addComponentColumn(company -> {
            Button editBtn = new Button("Düzenle", VaadinIcon.EDIT.create());
            editBtn.addThemeVariants(ButtonVariant.LUMO_SMALL);
            editBtn.addClickListener(e -> openCompanyDialog(company));
            return editBtn;
        }).setHeader("İşlem").setAutoWidth(true);

        companyGrid.getStyle().set("border", "none");
    }

    private void setupDepartmentGrid() {
        deptGrid.setSizeFull();
        deptGrid.addColumn(Department::getId).setHeader("ID").setAutoWidth(true).setFlexGrow(0);
        deptGrid.addColumn(Department::getName).setHeader("Departman Adı").setFlexGrow(1);
        
        deptGrid.addComponentColumn(dept -> {
            boolean active = dept.isActive();
            Span badge = new Span(active ? "Aktif" : "Pasif");
            badge.getStyle()
                 .set("color", active ? "#15803d" : "#b45309")
                 .set("background-color", active ? "#dcfce7" : "#fef3c7")
                 .set("padding", "4px 8px")
                 .set("border-radius", "12px")
                 .set("font-weight", "500")
                 .set("font-size", "0.85rem");
            return badge;
        }).setHeader("Durum").setAutoWidth(true);

        deptGrid.addComponentColumn(dept -> {
            Button editBtn = new Button("Düzenle", VaadinIcon.EDIT.create());
            editBtn.addThemeVariants(ButtonVariant.LUMO_SMALL);
            editBtn.addClickListener(e -> openDepartmentDialog(dept));
            return editBtn;
        }).setHeader("İşlem").setAutoWidth(true);

        deptGrid.getStyle().set("border", "none");
    }

    private void openCompanyDialog(Company company) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(company.getId() == null ? "Yeni Şirket Ekle" : "Şirket Düzenle");
        dialog.setWidth("400px");

        TextField nameField = new TextField("Şirket Adı");
        nameField.setValue(company.getName() != null ? company.getName() : "");
        nameField.setWidthFull();

        IntegerField scoreField = new IntegerField("Şirket Skoru");
        scoreField.setValue(company.getCompanyScore() != null ? company.getCompanyScore().intValue() : 0);
        scoreField.setWidthFull();

        RadioButtonGroup<Boolean> statusGroup = new RadioButtonGroup<>("Sistem Durumu");
        statusGroup.setItems(true, false);
        statusGroup.setItemLabelGenerator(active -> active ? "Aktif" : "Pasif (Devre Dışı)");
        statusGroup.setValue(company.getId() == null || company.isActive());

        Button saveBtn = new Button("Kaydet", e -> {
            if (nameField.isEmpty()) {
                Notification.show("Şirket adı boş olamaz!").addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }
            company.setName(nameField.getValue());
            company.setCompanyScore((double) scoreField.getValue());
            company.setActive(statusGroup.getValue());
            
            if (company.getId() == null) {
                companyService.registerNewCompany(company);
            } else {
                companyService.updateCompany(company); 
            }
            
            Notification.show("Şirket kaydedildi!").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            refreshGrids();
            dialog.close();
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button cancelBtn = new Button("İptal", e -> dialog.close());

        VerticalLayout layout = new VerticalLayout(nameField, scoreField, statusGroup);
        layout.setPadding(false);
        layout.setSpacing(false);
        layout.getThemeList().add("spacing-s");

        dialog.add(layout);
        dialog.getFooter().add(cancelBtn, saveBtn);
        dialog.open();
    }

    private void openDepartmentDialog(Department department) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(department.getId() == null ? "Yeni Departman Ekle" : "Departman Düzenle");
        dialog.setWidth("400px");

        TextField nameField = new TextField("Departman Adı");
        nameField.setValue(department.getName() != null ? department.getName() : "");
        nameField.setWidthFull();

        RadioButtonGroup<Boolean> statusGroup = new RadioButtonGroup<>("Sistem Durumu");
        statusGroup.setItems(true, false);
        statusGroup.setItemLabelGenerator(active -> active ? "Aktif" : "Pasif (Devre Dışı)");
        statusGroup.setValue(department.getId() == null || department.isActive());

        Button saveBtn = new Button("Kaydet", e -> {
            if (nameField.isEmpty()) {
                Notification.show("Departman adı boş olamaz!").addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }
            department.setName(nameField.getValue());
            department.setActive(statusGroup.getValue());
            
            if (department.getId() == null) {
                departmentService.registerNewDepartment(department); 
            } else {
                departmentService.updateDepartment(department); 
            }
            
            Notification.show("Departman kaydedildi!").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            refreshGrids();
            dialog.close();
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button cancelBtn = new Button("İptal", e -> dialog.close());

        VerticalLayout layout = new VerticalLayout(nameField, statusGroup);
        layout.setPadding(false);
        layout.setSpacing(false);
        layout.getThemeList().add("spacing-s");

        dialog.add(layout);
        dialog.getFooter().add(cancelBtn, saveBtn);
        dialog.open();
    }

    private void refreshGrids() {
        companyGrid.setItems(companyService.getAllCompanies());
        deptGrid.setItems(departmentService.getAllDepartments()); 
    }
}