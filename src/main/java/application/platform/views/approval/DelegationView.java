package application.platform.views.approval;

import java.util.List;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.contextmenu.SubMenu;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import application.platform.common.BusinessException;
import application.platform.common.Dates;
import application.platform.delegation.DelegationService;
import application.platform.delegation.DelegationStatus;
import application.platform.delegation.DelegationSummary;
import application.platform.views.MainLayout;
import application.platform.views.ViewNotifications;

import jakarta.annotation.security.PermitAll;

@Route(value = "approval/delegation", layout = MainLayout.class)
@PageTitle("Delegation")
@PermitAll
public class DelegationView extends VerticalLayout {

    private final DelegationService delegationService;
    private final Grid<DelegationSummary> grid = new Grid<>();
    private final TextField search = new TextField();
    private final ComboBox<DelegationStatus> statusFilter = new ComboBox<>();
    private List<DelegationSummary> delegations = List.of();

    public DelegationView(DelegationService delegationService) {
        this.delegationService = delegationService;
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H2 title = new H2("Delegation");
        Span subtitle = new Span("Manage temporary approval delegation");
        subtitle.getStyle().set("color", "var(--lumo-secondary-text-color)");

        VerticalLayout header = new VerticalLayout(title, subtitle);
        header.setPadding(false);
        header.setSpacing(false);

        search.setPlaceholder("Search delegation...");
        search.setClearButtonVisible(true);
        search.setPrefixComponent(VaadinIcon.SEARCH.create());

        statusFilter.setItems(DelegationStatus.values());
        statusFilter.setItemLabelGenerator(DelegationStatus::getLabel);
        statusFilter.setPlaceholder("All statuses");
        statusFilter.setClearButtonVisible(true);

        Button addDelegation = new Button("Add Delegation", VaadinIcon.PLUS.create(),
                event -> getUI().ifPresent(ui -> ui.navigate("approval/delegation/add")));

        HorizontalLayout toolbar = new HorizontalLayout(search, statusFilter, addDelegation);
        toolbar.setWidthFull();
        toolbar.setAlignItems(HorizontalLayout.Alignment.CENTER);
        toolbar.setFlexGrow(1, search);

        grid.addColumn(DelegationSummary::delegatorName).setHeader("Delegator");
        grid.addColumn(DelegationSummary::delegateName).setHeader("Delegate To");
        grid.addColumn(delegation -> Dates.format(delegation.startDate())).setHeader("Start Date");
        grid.addColumn(delegation -> Dates.format(delegation.endDate())).setHeader("End Date");
        grid.addColumn(DelegationSummary::reason).setHeader("Reason");
        grid.addComponentColumn(delegation -> statusLabel(delegation.status())).setHeader("Status");
        grid.addComponentColumn(this::actionMenu).setHeader("Action");
        grid.setSizeFull();

        search.addValueChangeListener(event -> applyFilter());
        statusFilter.addValueChangeListener(event -> applyFilter());

        add(header, toolbar, grid);
        setFlexGrow(1, grid);
        refresh();
    }

    private void refresh() {
        delegations = delegationService.findAll();
        applyFilter();
    }

    private void applyFilter() {
        String needle = search.getValue() == null ? "" : search.getValue().trim().toLowerCase();
        DelegationStatus status = statusFilter.getValue();
        grid.setItems(delegations.stream()
                .filter(delegation -> needle.isEmpty() || delegation.delegatorName().toLowerCase().contains(needle)
                        || delegation.delegateName().toLowerCase().contains(needle)
                        || delegation.reason().toLowerCase().contains(needle))
                .filter(delegation -> status == null || delegation.status() == status)
                .toList());
    }

    private Component statusLabel(DelegationStatus status) {
        Span label = new Span(status.getLabel());
        String background;
        String color;
        switch (status) {
            case ACTIVE -> {
                background = "#e3f8ec";
                color = "#1a7f4b";
            }
            case SCHEDULED -> {
                background = "#e8f0fe";
                color = "#1a56db";
            }
            case CANCELLED -> {
                background = "#fdecea";
                color = "#c0392b";
            }
            default -> {
                background = "#eeeeee";
                color = "#555555";
            }
        }
        label.getStyle()
                .set("padding", "0.15em 0.65em")
                .set("border-radius", "1em")
                .set("font-size", "0.85em")
                .set("font-weight", "600")
                .set("background", background)
                .set("color", color);
        return label;
    }

    private Component actionMenu(DelegationSummary delegation) {
        MenuBar menuBar = new MenuBar();
        menuBar.addThemeVariants(MenuBarVariant.LUMO_TERTIARY_INLINE);
        MenuItem actions = menuBar.addItem(VaadinIcon.ELLIPSIS_DOTS_V.create());
        SubMenu subMenu = actions.getSubMenu();
        if (delegation.status() == DelegationStatus.SCHEDULED) {
            subMenu.addItem("Edit",
                    event -> getUI().ifPresent(ui -> ui.navigate("approval/delegation/edit/" + delegation.id())));
        }
        subMenu.addItem("View", event -> openDetailDialog(delegation));
        if (delegation.status() == DelegationStatus.SCHEDULED || delegation.status() == DelegationStatus.ACTIVE) {
            subMenu.addItem("Cancel", event -> confirmCancel(delegation));
        }
        return menuBar;
    }

    private void openDetailDialog(DelegationSummary delegation) {
        FormLayout details = new FormLayout();
        details.addFormItem(new Span(delegation.delegatorName()), "Delegator");
        details.addFormItem(new Span(delegation.delegateName()), "Delegate To");
        details.addFormItem(new Span(Dates.format(delegation.startDate()) + " - "
                + Dates.format(delegation.endDate())), "Period");
        details.addFormItem(new Span(delegation.reason()), "Reason");
        details.addFormItem(statusLabel(delegation.status()), "Status");
        details.addFormItem(new Span(delegation.createdBy()), "Created By");
        if (delegation.cancelledDate() != null) {
            details.addFormItem(new Span(Dates.format(delegation.cancelledDate()) + " by "
                    + delegation.cancelledBy()), "Cancelled");
        }

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Delegation");
        dialog.setWidth("560px");
        dialog.add(details);
        dialog.getFooter().add(new Button("Close", event -> dialog.close()));
        dialog.open();
    }

    private void confirmCancel(DelegationSummary delegation) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Cancel delegation?");
        dialog.add(new Span(delegation.delegateName() + " will no longer approve on behalf of "
                + delegation.delegatorName() + ". This cannot be undone."));

        Button keep = new Button("Keep", event -> dialog.close());
        Button cancel = new Button("Cancel Delegation", event -> {
            try {
                delegationService.cancel(delegation.id());
                ViewNotifications.showSuccess("Delegation cancelled");
            } catch (BusinessException e) {
                ViewNotifications.showError(e.getMessage());
            }
            dialog.close();
            refresh();
        });
        cancel.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);
        dialog.getFooter().add(keep, cancel);
        dialog.open();
    }
}
