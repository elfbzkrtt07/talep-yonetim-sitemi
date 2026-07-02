package com.example.views;

import com.example.base.ui.MainLayout;
import com.example.entities.Company;
import com.example.services.CompanyService;
import com.example.services.DepartmentService;
import com.example.services.RequestService;
import com.example.services.UserService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;

@Route(value = "", layout = MainLayout.class)
public class HomeView extends VerticalLayout {

    private final CompanyService companyService;
    private final Paragraph companyCountLabel = new Paragraph();

    // 1. Spring automatically wires all your custom services here
    public HomeView(CompanyService companyService, 
                    DepartmentService departmentService,
                    UserService userService, 
                    RequestService requestService) {
        
        this.companyService = companyService;
        
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        add(new H1("🚀 Full-Stack Layer Verification Panel"));
        add(new Paragraph("This screen interacts dynamically with your Spring Services, Repositories, and Oracle database."));

        // --- SECTION A: READ CHECKS ---
        add(new H3("📊 Read Operations Verification"));
        try {
            // Testing your read service layers
            int companiesSize = companyService.getAllCompanies().size();
            int departmentsSize = departmentService.getAllDepartments().size();
            int usersSize = userService.getAllUsers().size();
            int requestsSize = requestService.getAllRequests().size();

            add(new Paragraph("✅ Company Layer: " + companiesSize + " rows found."));
            add(new Paragraph("✅ Department Layer: " + departmentsSize + " rows found."));
            add(new Paragraph("✅ User Layer: " + usersSize + " rows found."));
            add(new Paragraph("✅ Request Layer: " + requestsSize + " rows found."));
            
            updateCompanyLabelText();
        } catch (Exception e) {
            Paragraph errorLabel = new Paragraph("❌ Read Check Failed: " + e.getMessage());
            errorLabel.getStyle().set("color", "red");
            add(errorLabel);
        }

        // --- SECTION B: WRITE CHECKS ---
        add(new H3("💾 Write Operations Verification"));
        add(new Paragraph("Type a name below to test saving data through your Transactional Service layer down into Oracle:"));

        TextField nameInput = new TextField("Company Name");
        nameInput.setPlaceholder("Enter company name...");

        Button saveButton = new Button("Register via CompanyService");
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        
        // This triggers your service layer dynamically upon clicking
        saveButton.addClickListener(event -> {
            String inputName = nameInput.getValue().trim();
            if (inputName.isEmpty()) {
                Notification.show("Please enter a valid name").addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            try {
                Company newCompany = new Company();
                newCompany.setName(inputName);
                newCompany.setCompanyScore(5.0); // Set default test score

                // Call your exact transactional registration logic!
                companyService.registerNewCompany(newCompany);

                Notification notification = Notification.show("🎉 Successfully persisted to Oracle via CompanyService!");
                notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                
                nameInput.clear();
                updateCompanyLabelText(); // Refresh display counter label
            } catch (Exception e) {
                Notification.show("❌ Save Failed: " + e.getMessage(), 5000, Notification.Position.MIDDLE)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        HorizontalLayout formLayout = new HorizontalLayout(nameInput, saveButton);
        formLayout.setVerticalComponentAlignment(Alignment.AUTO, saveButton);
        
        add(formLayout, companyCountLabel);
    }

    private void updateCompanyLabelText() {
        int freshCount = companyService.getAllCompanies().size();
        companyCountLabel.setText("📈 Total stored companies tracking counter: " + freshCount);
        companyCountLabel.getStyle().set("font-weight", "bold");
    }
}