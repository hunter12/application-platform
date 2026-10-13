package application.platform.views.approval;

import java.util.ArrayList;
import java.util.List;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import application.platform.views.MainLayout;

import jakarta.annotation.security.PermitAll;

@Route(value = "approval/workflows/add", layout = MainLayout.class)
@PageTitle("Add Approval Workflow")
@PermitAll
public class AddApprovalWorkflowView extends VerticalLayout {

    private static final List<String> APPROVER_ROLES = List.of("Super Admin", "Admin", "Director", "Manager", "SPV",
            "Staff");

    private record StepRow(HorizontalLayout layout, Span label) {
    }

    private final List<StepRow> stepRows = new ArrayList<>();
    private final VerticalLayout stepsContainer = new VerticalLayout();

    public AddApprovalWorkflowView() {
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H2 title = new H2("Add Approval Workflow");
        Span subtitle = new Span("Create a new approval workflow");
        subtitle.getStyle().set("color", "var(--lumo-secondary-text-color)");

        VerticalLayout header = new VerticalLayout(title, subtitle);
        header.setPadding(false);
        header.setSpacing(false);

        TextField name = new TextField("Workflow Name");

        ComboBox<String> module = new ComboBox<>("Module");
        module.setItems("Finance", "Invoice", "Stock", "Product", "Report");

        ComboBox<String> status = new ComboBox<>("Status");
        status.setItems("Active", "Inactive");
        status.setValue("Active");

        TextArea description = new TextArea("Description");

        FlexLayout fields = twoColumnSection(name, module, status, description);

        stepsContainer.setPadding(false);
        stepsContainer.setSpacing(true);
        addStepRow("SPV");
        addStepRow("Manager");
        addStepRow("Director");

        Button addStep = new Button("Add Step", VaadinIcon.PLUS.create(), event -> addStepRow(null));

        VerticalLayout stepsSection = new VerticalLayout(stepsContainer, addStep);
        stepsSection.setPadding(false);
        stepsSection.setSpacing(true);

        Button cancel = new Button("Cancel", event -> getUI().ifPresent(ui -> ui.navigate("approval/workflows")));

        Button save = new Button("Save Workflow", event -> {
            Notification.show("Saving workflows is a UI prototype only - no data is persisted.", 3000,
                    Notification.Position.BOTTOM_START);
            getUI().ifPresent(ui -> ui.navigate("approval/workflows"));
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout actions = new HorizontalLayout(cancel, save);
        actions.setWidthFull();
        actions.setJustifyContentMode(HorizontalLayout.JustifyContentMode.END);

        VerticalLayout form = new VerticalLayout(header,
                sectionHeading("Workflow Information"), fields,
                sectionHeading("Approval Steps"), stepsSection,
                actions);
        form.setPadding(false);
        form.setSpacing(true);
        form.setMaxWidth("960px");

        add(form);
    }

    private void addStepRow(String defaultRole) {
        ComboBox<String> approverRole = new ComboBox<>();
        approverRole.setItems(APPROVER_ROLES);
        approverRole.setPlaceholder("Approver Role");
        if (defaultRole != null) {
            approverRole.setValue(defaultRole);
        }
        approverRole.getStyle().set("flex", "1 1 220px").set("min-width", "200px");

        Span stepLabel = new Span();
        stepLabel.getStyle().set("font-weight", "600").set("min-width", "70px");

        HorizontalLayout row = new HorizontalLayout();
        row.setWidthFull();
        row.setAlignItems(HorizontalLayout.Alignment.CENTER);

        StepRow stepRow = new StepRow(row, stepLabel);
        Button remove = new Button(VaadinIcon.TRASH.create(), event -> removeStepRow(stepRow));

        row.add(stepLabel, approverRole, remove);
        row.setFlexGrow(1, approverRole);

        stepRows.add(stepRow);
        stepsContainer.add(row);
        renumberSteps();
    }

    private void removeStepRow(StepRow stepRow) {
        stepRows.remove(stepRow);
        stepsContainer.remove(stepRow.layout());
        renumberSteps();
    }

    private void renumberSteps() {
        for (int i = 0; i < stepRows.size(); i++) {
            stepRows.get(i).label().setText("Step " + (i + 1));
        }
    }

    private Component sectionHeading(String text) {
        H3 heading = new H3(text);
        heading.getStyle().set("margin-bottom", "0");

        VerticalLayout wrapper = new VerticalLayout(heading, new Hr());
        wrapper.setPadding(false);
        wrapper.setSpacing(false);
        return wrapper;
    }

    private FlexLayout twoColumnSection(Component... fields) {
        FlexLayout layout = new FlexLayout(fields);
        layout.setFlexWrap(FlexLayout.FlexWrap.WRAP);
        layout.getStyle().set("gap", "1rem");
        for (Component field : fields) {
            field.getElement().getStyle()
                    .set("flex", "1 1 320px")
                    .set("min-width", "280px");
        }
        return layout;
    }
}
