package application.platform.views.approval;

import java.util.List;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.contextmenu.SubMenu;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import application.platform.views.MainLayout;

import jakarta.annotation.security.PermitAll;

/**
 * Visual prototype of the Delegation page. Uses static mock data only -
 * Add/Edit/View/Cancel are UI affordances that do not persist anything.
 */
@Route(value = "approval/delegation", layout = MainLayout.class)
@PageTitle("Delegation")
@PermitAll
public class DelegationView extends VerticalLayout {

    private static final String FILTER_ALL = "All";

    public DelegationView() {
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H2 title = new H2("Delegation");
        Span subtitle = new Span("Manage temporary approval delegation");
        subtitle.getStyle().set("color", "var(--lumo-secondary-text-color)");

        VerticalLayout header = new VerticalLayout(title, subtitle);
        header.setPadding(false);
        header.setSpacing(false);

        TextField search = new TextField();
        search.setPlaceholder("Search delegation...");
        search.setClearButtonVisible(true);
        search.setPrefixComponent(VaadinIcon.SEARCH.create());

        ComboBox<String> statusFilter = new ComboBox<>();
        statusFilter.setItems(FILTER_ALL, "Scheduled", "Active", "Completed", "Cancelled");
        statusFilter.setValue(FILTER_ALL);
        statusFilter.setPlaceholder("Status");

        Button addDelegation = new Button("Add Delegation", VaadinIcon.PLUS.create(),
                event -> getUI().ifPresent(ui -> ui.navigate("approval/delegation/add")));

        HorizontalLayout toolbar = new HorizontalLayout(search, statusFilter, addDelegation);
        toolbar.setWidthFull();
        toolbar.setAlignItems(HorizontalLayout.Alignment.CENTER);
        toolbar.setFlexGrow(1, search);

        Grid<MockDelegation> grid = new Grid<>();
        grid.setItems(MockDelegationData.ALL);
        grid.addColumn(MockDelegation::delegator).setHeader("Delegator");
        grid.addColumn(MockDelegation::delegateTo).setHeader("Delegate To");
        grid.addColumn(MockDelegation::startDate).setHeader("Start Date");
        grid.addColumn(MockDelegation::endDate).setHeader("End Date");
        grid.addColumn(MockDelegation::reason).setHeader("Reason");
        grid.addComponentColumn(delegation -> statusLabel(delegation.status())).setHeader("Status");
        grid.addComponentColumn(this::actionMenu).setHeader("Action");
        grid.setSizeFull();

        search.addValueChangeListener(
                event -> grid.setItems(filter(event.getValue(), statusFilter.getValue())));
        statusFilter.addValueChangeListener(
                event -> grid.setItems(filter(search.getValue(), event.getValue())));

        add(header, toolbar, grid);
        setFlexGrow(1, grid);
    }

    private List<MockDelegation> filter(String term, String status) {
        String needle = term == null ? "" : term.trim().toLowerCase();
        return MockDelegationData.ALL.stream()
                .filter(delegation -> needle.isEmpty() || delegation.delegator().toLowerCase().contains(needle)
                        || delegation.delegateTo().toLowerCase().contains(needle))
                .filter(delegation -> status == null || FILTER_ALL.equals(status)
                        || delegation.status().equals(status))
                .toList();
    }

    private Component statusLabel(String status) {
        Span label = new Span(status);
        String background;
        String color;
        switch (status) {
            case "Active" -> {
                background = "#e3f8ec";
                color = "#1a7f4b";
            }
            case "Scheduled" -> {
                background = "#e8f0fe";
                color = "#1a56db";
            }
            case "Cancelled" -> {
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

    private Component actionMenu(MockDelegation delegation) {
        MenuBar menuBar = new MenuBar();
        menuBar.addThemeVariants(MenuBarVariant.LUMO_TERTIARY_INLINE);
        MenuItem actions = menuBar.addItem(VaadinIcon.ELLIPSIS_DOTS_V.create());
        SubMenu subMenu = actions.getSubMenu();
        subMenu.addItem("Edit", event -> showPrototypeNotice("Editing delegations"));
        subMenu.addItem("View", event -> showPrototypeNotice("Viewing delegations"));
        subMenu.addItem("Cancel", event -> showPrototypeNotice("Cancelling delegations"));
        return menuBar;
    }

    private void showPrototypeNotice(String action) {
        Notification.show(action + " is a UI prototype only - no data is persisted.", 3000,
                Notification.Position.BOTTOM_START);
    }
}
