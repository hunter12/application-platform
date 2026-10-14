package application.platform.views.approval;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;

import application.platform.common.BusinessException;
import application.platform.delegation.DelegationForm;
import application.platform.delegation.DelegationService;
import application.platform.delegation.DelegationStatus;
import application.platform.delegation.DelegationSummary;
import application.platform.user.UserService;
import application.platform.user.UserSummary;
import application.platform.views.MainLayout;
import application.platform.views.UrlIds;
import application.platform.views.ViewNotifications;

import jakarta.annotation.security.PermitAll;

@Route(value = "approval/delegation/add", layout = MainLayout.class)
@RouteAlias(value = "approval/delegation/edit", layout = MainLayout.class)
@PageTitle("Add Delegation")
@PermitAll
public class AddDelegationView extends VerticalLayout implements HasUrlParameter<String> {

    private final DelegationService delegationService;
    private final List<UserSummary> activeUsers;

    private final H2 title = new H2("Add Delegation");
    private final Span subtitle = new Span("Create a temporary approval delegation");
    private final ComboBox<UserSummary> delegator = new ComboBox<>("Delegator");
    private final ComboBox<UserSummary> delegate = new ComboBox<>("Delegate To");
    private final DatePicker startDate = new DatePicker("Start Date");
    private final DatePicker endDate = new DatePicker("End Date");
    private final TextField reason = new TextField("Reason");

    /** The delegation being edited, or {@code null} when adding a new one. */
    private DelegationSummary editing;

    public AddDelegationView(DelegationService delegationService, UserService userService) {
        this.delegationService = delegationService;
        this.activeUsers = userService.findAll().stream().filter(UserSummary::active).toList();
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        subtitle.getStyle().set("color", "var(--lumo-secondary-text-color)");

        VerticalLayout header = new VerticalLayout(title, subtitle);
        header.setPadding(false);
        header.setSpacing(false);

        delegator.setRequiredIndicatorVisible(true);
        delegator.setItems(activeUsers);
        delegator.setItemLabelGenerator(AddDelegationView::userLabel);
        delegator.addValueChangeListener(event -> refreshDelegateChoices());

        delegate.setRequiredIndicatorVisible(true);
        delegate.setItemLabelGenerator(AddDelegationView::userLabel);
        delegate.setHelperText("Users in the delegator's organization or an organization above it");

        startDate.setRequiredIndicatorVisible(true);
        startDate.setMin(LocalDate.now());
        startDate.addValueChangeListener(event -> endDate.setMin(
                event.getValue() == null ? LocalDate.now() : event.getValue()));
        endDate.setRequiredIndicatorVisible(true);
        endDate.setMin(LocalDate.now());

        reason.setRequiredIndicatorVisible(true);
        reason.setPlaceholder("e.g. Annual leave, business trip");

        FlexLayout fields = twoColumnSection(delegator, delegate, startDate, endDate, reason);

        Button cancel = new Button("Cancel", event -> getUI().ifPresent(ui -> ui.navigate("approval/delegation")));

        Button save = new Button("Save Delegation", event -> save());
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout actions = new HorizontalLayout(cancel, save);
        actions.setWidthFull();
        actions.setJustifyContentMode(HorizontalLayout.JustifyContentMode.END);

        VerticalLayout form = new VerticalLayout(header, sectionHeading("Delegation Details"), fields, actions);
        form.setPadding(false);
        form.setSpacing(true);
        form.setMaxWidth("960px");

        add(form);
        refreshDelegateChoices();
    }

    @Override
    public void setParameter(BeforeEvent event, @OptionalParameter String parameter) {
        if (parameter == null) {
            return;
        }
        editing = UrlIds.parse(parameter).flatMap(delegationService::findById).orElse(null);
        if (editing == null) {
            ViewNotifications.showError("Delegation not found");
            event.forwardTo("approval/delegation");
            return;
        }
        if (editing.status() != DelegationStatus.SCHEDULED) {
            ViewNotifications.showError("Only scheduled delegations can be edited");
            event.forwardTo("approval/delegation");
            return;
        }

        title.setText("Edit Delegation");
        subtitle.setText("Update the delegation of " + editing.delegatorName());
        activeUsers.stream().filter(user -> user.id().equals(editing.delegatorId())).findFirst()
                .ifPresent(delegator::setValue);
        activeUsers.stream().filter(user -> user.id().equals(editing.delegateId())).findFirst()
                .ifPresent(delegate::setValue);
        startDate.setValue(editing.startDate());
        endDate.setValue(editing.endDate());
        reason.setValue(editing.reason());
    }

    private void refreshDelegateChoices() {
        UserSummary selectedDelegator = delegator.getValue();
        UserSummary selected = delegate.getValue();
        Set<UUID> eligible = selectedDelegator == null ? Set.of()
                : delegationService.findEligibleDelegateIds(selectedDelegator.id());
        List<UserSummary> choices = activeUsers.stream().filter(user -> eligible.contains(user.id())).toList();
        delegate.setItems(choices);
        if (selected != null && choices.contains(selected)) {
            delegate.setValue(selected);
        }
    }

    private void save() {
        DelegationForm form = new DelegationForm(
                editing == null ? null : editing.id(),
                editing == null ? null : editing.version(),
                delegator.getValue() == null ? null : delegator.getValue().id(),
                delegate.getValue() == null ? null : delegate.getValue().id(),
                startDate.getValue(),
                endDate.getValue(),
                reason.getValue());
        try {
            delegationService.save(form);
            ViewNotifications.showSuccess("Delegation saved");
            getUI().ifPresent(ui -> ui.navigate("approval/delegation"));
        } catch (BusinessException e) {
            ViewNotifications.showError(e.getMessage());
        }
    }

    private static String userLabel(UserSummary user) {
        return user.fullName() + " (" + user.organizationName() + ")";
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
