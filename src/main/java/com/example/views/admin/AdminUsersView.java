package com.example.views.admin;

import com.example.base.ui.MainLayout;
import com.example.entities.Company;
import com.example.entities.Department;
import com.example.entities.User;
import com.example.enums.UserRole;
import com.example.services.CompanyService;
import com.example.services.DepartmentService;
import com.example.services.UserService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

@PageTitle("Kullanıcı Yönetimi")
@Route(value = "admin/users", layout = MainLayout.class)
public class AdminUsersView extends VerticalLayout {

    private final UserService userService;
    private final CompanyService companyService;
    private final DepartmentService departmentService;
    private final PasswordEncoder passwordEncoder;
    
    private final Grid<User> grid = new Grid<>(User.class, false);
    private final TextField searchField = new TextField();
    private final ComboBox<UserRole> roleFilter = new ComboBox<>();
    private final ComboBox<Boolean> statusFilter = new ComboBox<>();

    public AdminUsersView(UserService userService, CompanyService companyService, 
                          DepartmentService departmentService, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.companyService = companyService;
        this.departmentService = departmentService;
        this.passwordEncoder = passwordEncoder;

        setSizeFull();
        setPadding(true);
        setSpacing(true);
        getStyle().set("background-color", "#f1f5f9");

        add(createHeaderBanner("Kullanıcı Yönetimi", "Sistemdeki kullanıcıları görüntüleyin, yetkilendirin ve yeni kullanıcılar oluşturun."));

        grid.setSizeFull();
        grid.addColumn(User::getId).setHeader("ID").setAutoWidth(true).setFlexGrow(0);
        grid.addColumn(User::getName).setHeader("İsim").setFlexGrow(1);
        grid.addColumn(User::getEmail).setHeader("E-posta").setFlexGrow(1);
        
        grid.addColumn(user -> {
            if (user.getRole() != null && (user.getRole() == UserRole.ADMIN || user.getRole() == UserRole.PRODUCT_MANAGER)) {
                return "-";
            }
            if (user.getRole() != null && user.getRole() == UserRole.CUSTOMER && user.getCompany() != null) {
                return user.getCompany().getName();
            } else if (user.getDepartment() != null) {
                return user.getDepartment().getName();
            }
            return "-";
        }).setHeader("Şirket / Departman").setFlexGrow(1);

        grid.addColumn(user -> user.getRole() != null ? user.getRole().toString() : "-").setHeader("Rol").setAutoWidth(true);
        
        grid.addComponentColumn(user -> {
            Span badge = new Span(user.isApproved() ? "Onaylı" : "Onay Bekliyor");
            badge.getStyle()
                 .set("color", user.isApproved() ? "#15803d" : "#b45309")
                 .set("background-color", user.isApproved() ? "#dcfce7" : "#fef3c7")
                 .set("padding", "4px 8px")
                 .set("border-radius", "12px")
                 .set("font-weight", "500")
                 .set("font-size", "0.85rem");
            return badge;
        }).setHeader("Onay Durumu").setAutoWidth(true);

        grid.addComponentColumn(user -> {
            Button editBtn = new Button("İncele / Düzenle", VaadinIcon.EDIT.create());
            editBtn.addThemeVariants(ButtonVariant.LUMO_SMALL);
            editBtn.addClickListener(e -> openUserEditDialog(user));
            return editBtn;
        }).setHeader("İşlem").setAutoWidth(true);

        grid.getStyle().set("border", "none").set("border-radius", "8px").set("background", "white");
        
        searchField.setPlaceholder("İsim veya E-posta ara...");
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.addValueChangeListener(e -> applyFilters());
        searchField.setWidth("250px");

        roleFilter.setPlaceholder("Rol Filtrele");
        roleFilter.setItems(UserRole.values());
        roleFilter.setClearButtonVisible(true);
        roleFilter.addValueChangeListener(e -> applyFilters());
        roleFilter.setWidth("180px");

        statusFilter.setPlaceholder("Onay Durumu");
        statusFilter.setItems(true, false);
        statusFilter.setItemLabelGenerator(approved -> approved ? "Onaylı" : "Onay Bekliyor");
        statusFilter.setClearButtonVisible(true);
        statusFilter.addValueChangeListener(e -> applyFilters());
        statusFilter.setWidth("180px");

        HorizontalLayout filtersLayout = new HorizontalLayout(searchField, roleFilter, statusFilter);
        filtersLayout.setAlignItems(Alignment.CENTER);

        Button addUserBtn = new Button("Yeni Kullanıcı Ekle", VaadinIcon.PLUS.create());
        addUserBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addUserBtn.getStyle().set("background-color", "#0066cc").set("border-radius", "8px");
        addUserBtn.addClickListener(e -> openUserCreateDialog());

        HorizontalLayout toolbar = new HorizontalLayout(filtersLayout, addUserBtn);
        toolbar.setWidthFull();
        toolbar.setAlignItems(Alignment.CENTER);
        toolbar.setJustifyContentMode(JustifyContentMode.BETWEEN);
        toolbar.getStyle().set("margin-bottom", "10px");

        VerticalLayout card = new VerticalLayout(toolbar, grid);
        card.setSizeFull();
        card.setPadding(true);
        card.setSpacing(true);
        card.getStyle()
            .set("background", "white")
            .set("border-radius", "16px")
            .set("box-shadow", "0 10px 25px -5px rgba(0, 102, 204, 0.05)");
        
        add(card);

        refreshGrid();
    }

    private void applyFilters() {
        String searchTerm = searchField.getValue() != null ? searchField.getValue().trim().toLowerCase() : "";
        UserRole selectedRole = roleFilter.getValue();
        Boolean selectedStatus = statusFilter.getValue();

        List<User> allUsers = userService.getAllUsers();

        List<User> filtered = allUsers.stream().filter(user -> {
            boolean matchesSearch = searchTerm.isEmpty() ||
                    (user.getName() != null && user.getName().toLowerCase().contains(searchTerm)) ||
                    (user.getEmail() != null && user.getEmail().toLowerCase().contains(searchTerm));

            boolean matchesRole = (selectedRole == null) || (user.getRole() == selectedRole);

            boolean matchesStatus = (selectedStatus == null) || (user.isApproved() == selectedStatus);

            return matchesSearch && matchesRole && matchesStatus;
        }).toList();

        grid.setItems(filtered);
    }

    private void openUserCreateDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Yeni Kullanıcı Oluştur");
        dialog.setWidth("450px");

        TextField nameField = new TextField("İsim Soyisim");
        nameField.setWidthFull();

        TextField emailField = new TextField("E-posta");
        emailField.setWidthFull();

        PasswordField passwordField = new PasswordField("Şifre");
        passwordField.setWidthFull();

        ComboBox<UserRole> roleCombo = new ComboBox<>("Rol");
        roleCombo.setItems(UserRole.values());
        roleCombo.setWidthFull();

        ComboBox<Company> companyCombo = new ComboBox<>("Şirket");
        companyCombo.setItems(companyService.getAllCompanies());
        companyCombo.setItemLabelGenerator(Company::getName);
        companyCombo.setWidthFull();

        ComboBox<Department> departmentCombo = new ComboBox<>("Departman");
        departmentCombo.setItems(departmentService.getAllDepartments());
        departmentCombo.setItemLabelGenerator(Department::getName);
        departmentCombo.setWidthFull();

        updateOrgFieldsVisibility(null, companyCombo, departmentCombo);
        roleCombo.addValueChangeListener(e -> updateOrgFieldsVisibility(e.getValue(), companyCombo, departmentCombo));

        ComboBox<String> approvalCombo = new ComboBox<>("Onay Durumu");
        approvalCombo.setItems("Onaylı", "Onay Bekliyor");
        approvalCombo.setValue("Onaylı");
        approvalCombo.setWidthFull();

        Button saveBtn = new Button("Oluştur", VaadinIcon.CHECK.create(), e -> {
            if (nameField.isEmpty() || emailField.isEmpty() || passwordField.isEmpty() || roleCombo.isEmpty()) {
                Notification.show("Lütfen zorunlu alanları (İsim, E-posta, Şifre, Rol) doldurun!", 3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            User newUser = new User();
            newUser.setName(nameField.getValue());
            newUser.setEmail(emailField.getValue());
            
            newUser.setPassword(passwordField.getValue());
            
            UserRole selectedRole = roleCombo.getValue();
            
            if (selectedRole == UserRole.ADMIN || selectedRole == UserRole.PRODUCT_MANAGER) {
                newUser.setCompany(null);
                newUser.setDepartment(null);
            } else if (selectedRole == UserRole.CUSTOMER) {
                newUser.setCompany(companyCombo.getValue());
                newUser.setDepartment(null);
            } else {
                newUser.setDepartment(departmentCombo.getValue());
                newUser.setCompany(null);
            }
            
            try {
                User savedUser = userService.registerNewUser(newUser, selectedRole);
                
                boolean isApproved = "Onaylı".equals(approvalCombo.getValue());
                if (isApproved) {
                    savedUser.setApproved(true);
                    userService.updateUser(savedUser);
                }

                Notification.show("Yeni kullanıcı başarıyla oluşturuldu!", 3000, Notification.Position.BOTTOM_START)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                refreshGrid();
                dialog.close();
                
            } catch (Exception ex) {
                Notification.show("Hata: " + ex.getMessage(), 3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveBtn.getStyle().set("background-color", "#0066cc");

        Button cancelBtn = new Button("İptal", e -> dialog.close());

        VerticalLayout layout = new VerticalLayout(nameField, emailField, passwordField, roleCombo, companyCombo, departmentCombo, approvalCombo);
        layout.setPadding(false);
        layout.setSpacing(true);

        dialog.add(layout);
        dialog.getFooter().add(cancelBtn, saveBtn);
        dialog.open();
    }

    private void openUserEditDialog(User user) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Kullanıcı Detayları ve Onay");
        dialog.setWidth("450px");

        TextField nameField = new TextField("İsim");
        nameField.setValue(user.getName() != null ? user.getName() : "");
        nameField.setReadOnly(true);
        nameField.setWidthFull();

        TextField emailField = new TextField("E-posta");
        emailField.setValue(user.getEmail() != null ? user.getEmail() : "");
        emailField.setReadOnly(true);
        emailField.setWidthFull();

        ComboBox<UserRole> roleCombo = new ComboBox<>("Rol");
        roleCombo.setItems(UserRole.values());
        roleCombo.setValue(user.getRole());
        roleCombo.setWidthFull();

        ComboBox<Company> companyCombo = new ComboBox<>("Şirket");
        companyCombo.setItems(companyService.getAllCompanies());
        companyCombo.setItemLabelGenerator(Company::getName);
        companyCombo.setValue(user.getCompany());
        companyCombo.setWidthFull();

        ComboBox<Department> departmentCombo = new ComboBox<>("Departman");
        departmentCombo.setItems(departmentService.getAllDepartments());
        departmentCombo.setItemLabelGenerator(Department::getName);
        departmentCombo.setValue(user.getDepartment());
        departmentCombo.setWidthFull();

        updateOrgFieldsVisibility(roleCombo.getValue(), companyCombo, departmentCombo);
        roleCombo.addValueChangeListener(e -> updateOrgFieldsVisibility(e.getValue(), companyCombo, departmentCombo));

        ComboBox<String> approvalCombo = new ComboBox<>("Onay Durumu");
        approvalCombo.setItems("Onaylı", "Onay Bekliyor");
        approvalCombo.setValue(user.isApproved() ? "Onaylı" : "Onay Bekliyor");
        approvalCombo.setWidthFull();

        Button saveBtn = new Button("Değişiklikleri Kaydet", VaadinIcon.CHECK.create(), e -> {
            UserRole selectedRole = roleCombo.getValue();
            user.setRole(selectedRole);
            
            boolean isApproved = "Onaylı".equals(approvalCombo.getValue());
            user.setApproved(isApproved);
            
            if (selectedRole == UserRole.ADMIN || selectedRole == UserRole.PRODUCT_MANAGER) {
                user.setCompany(null);
                user.setDepartment(null);
            } else if (selectedRole == UserRole.CUSTOMER) {
                user.setCompany(companyCombo.getValue());
                user.setDepartment(null);
            } else {
                user.setDepartment(departmentCombo.getValue());
                user.setCompany(null);
            }
            
            userService.updateUser(user); 
            
            Notification.show("Kullanıcı güncellendi!", 3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            refreshGrid();
            dialog.close();
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveBtn.getStyle().set("background-color", "#0066cc");

        Button cancelBtn = new Button("İptal", e -> dialog.close());

        VerticalLayout layout = new VerticalLayout(nameField, emailField, roleCombo, companyCombo, departmentCombo, approvalCombo);
        layout.setPadding(false);
        layout.setSpacing(true);

        dialog.add(layout);
        dialog.getFooter().add(cancelBtn, saveBtn);
        dialog.open();
    }

    private void updateOrgFieldsVisibility(UserRole role, ComboBox<Company> companyCombo, ComboBox<Department> departmentCombo) {
        boolean isCustomer = (role == UserRole.CUSTOMER);
        boolean isAdminOrPm = (role == UserRole.ADMIN || role == UserRole.PRODUCT_MANAGER);

        companyCombo.setVisible(isCustomer);
        departmentCombo.setVisible(!isCustomer && !isAdminOrPm);
    }

    private void refreshGrid() {
        applyFilters();
    }

    private VerticalLayout createHeaderBanner(String titleText, String subtitleText) {
        VerticalLayout bannerLayout = new VerticalLayout();
        bannerLayout.setWidthFull();
        bannerLayout.setAlignItems(Alignment.CENTER); 
        bannerLayout.setJustifyContentMode(JustifyContentMode.CENTER);
        bannerLayout.setPadding(false);
        bannerLayout.setSpacing(true);
        bannerLayout.getStyle()
                .set("margin-top", "15px")
                .set("margin-bottom", "15px");

        H2 title = new H2(titleText);
        title.getStyle()
                .set("margin", "0")
                .set("font-size", "2rem") 
                .set("font-weight", "800") 
                .set("color", "#102a43");
        bannerLayout.add(title);

        if (subtitleText != null && !subtitleText.isEmpty()) {
            Span subtitle = new Span(subtitleText);
            subtitle.getStyle()
                    .set("margin-top", "4px") 
                    .set("font-size", "0.95rem")
                    .set("color", "#486581")
                    .set("text-align", "center");
            bannerLayout.add(subtitle);
        }

        return bannerLayout;
    }
}