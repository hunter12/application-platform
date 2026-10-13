package application.platform.views.notifications;

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

@Route(value = "notifications/policy", layout = MainLayout.class)
@PageTitle("Notification Policy")
@PermitAll
public class NotificationPolicyView extends VerticalLayout {

    private static final String FILTER_ALL = "All";

    public NotificationPolicyView() {
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H2 title = new H2("Notification Policy");
        Span subtitle = new Span("Configure notification delivery rules");
        subtitle.getStyle().set("color", "var(--lumo-secondary-text-color)");

        VerticalLayout header = new VerticalLayout(title, subtitle);
        header.setPadding(false);
        header.setSpacing(false);

        TextField search = new TextField();
        search.setPlaceholder("Search policy...");
        search.setClearButtonVisible(true);
        search.setPrefixComponent(VaadinIcon.SEARCH.create());

        ComboBox<String> statusFilter = new ComboBox<>();
        statusFilter.setItems(FILTER_ALL, "Active", "Inactive");
        statusFilter.setValue(FILTER_ALL);
        statusFilter.setPlaceholder("Status");

        Button addPolicy = new Button("Add Policy", VaadinIcon.PLUS.create(),
                event -> getUI().ifPresent(ui -> ui.navigate("notifications/policy/add")));

        HorizontalLayout toolbar = new HorizontalLayout(search, statusFilter, addPolicy);
        toolbar.setWidthFull();
        toolbar.setAlignItems(HorizontalLayout.Alignment.CENTER);
        toolbar.setFlexGrow(1, search);

        Grid<MockPolicy> grid = new Grid<>();
        grid.setItems(MockPolicyData.ALL);
        grid.addColumn(MockPolicy::name).setHeader("Policy Name");
        grid.addColumn(MockPolicy::event).setHeader("Event");
        grid.addColumn(MockPolicy::channel).setHeader("Channel");
        grid.addColumn(MockPolicy::recipient).setHeader("Recipient");
        grid.addComponentColumn(policy -> statusLabel(policy.status())).setHeader("Status");
        grid.addComponentColumn(this::actionMenu).setHeader("Action");
        grid.setSizeFull();

        search.addValueChangeListener(event -> grid.setItems(filter(event.getValue(), statusFilter.getValue())));
        statusFilter.addValueChangeListener(event -> grid.setItems(filter(search.getValue(), event.getValue())));

        add(header, toolbar, grid);
        setFlexGrow(1, grid);
    }

    private List<MockPolicy> filter(String term, String status) {
        String needle = term == null ? "" : term.trim().toLowerCase();
        return MockPolicyData.ALL.stream()
                .filter(policy -> needle.isEmpty() || policy.name().toLowerCase().contains(needle))
                .filter(policy -> status == null || FILTER_ALL.equals(status) || policy.status().equals(status))
                .toList();
    }

    private Component statusLabel(String status) {
        Span label = new Span(status);
        boolean active = "Active".equals(status);
        label.getStyle()
                .set("padding", "0.15em 0.65em")
                .set("border-radius", "1em")
                .set("font-size", "0.85em")
                .set("font-weight", "600")
                .set("background", active ? "#e3f8ec" : "#eeeeee")
                .set("color", active ? "#1a7f4b" : "#555555");
        return label;
    }

    private Component actionMenu(MockPolicy policy) {
        MenuBar menuBar = new MenuBar();
        menuBar.addThemeVariants(MenuBarVariant.LUMO_TERTIARY_INLINE);
        MenuItem actions = menuBar.addItem(VaadinIcon.ELLIPSIS_DOTS_V.create());
        SubMenu subMenu = actions.getSubMenu();
        subMenu.addItem("Edit", event -> showPrototypeNotice("Editing notification policies"));
        subMenu.addItem("Deactivate", event -> showPrototypeNotice("Deactivating notification policies"));
        return menuBar;
    }

    private void showPrototypeNotice(String action) {
        Notification.show(action + " is a UI prototype only - no data is persisted.", 3000,
                Notification.Position.BOTTOM_START);
    }
}
