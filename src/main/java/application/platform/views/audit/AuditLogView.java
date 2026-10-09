package application.platform.views.audit;

import java.util.List;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import application.platform.views.MainLayout;

import jakarta.annotation.security.PermitAll;

/**
 * Visual prototype of the Audit Log page. Read-only, static mock data
 * only - View Details opens a dialog with dummy values.
 */
@Route(value = "audit/log", layout = MainLayout.class)
@PageTitle("Audit Log")
@PermitAll
public class AuditLogView extends VerticalLayout {

    private static final String FILTER_ALL = "All";

    public AuditLogView() {
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H2 title = new H2("Audit Log");
        Span subtitle = new Span("Track system and user activities");
        subtitle.getStyle().set("color", "var(--lumo-secondary-text-color)");

        VerticalLayout header = new VerticalLayout(title, subtitle);
        header.setPadding(false);
        header.setSpacing(false);

        TextField search = new TextField();
        search.setPlaceholder("Search activity...");
        search.setClearButtonVisible(true);
        search.setPrefixComponent(VaadinIcon.SEARCH.create());

        ComboBox<String> userFilter = new ComboBox<>();
        userFilter.setItems(FILTER_ALL, "Abu Yazid", "John Smith", "Jane Doe", "Budi");
        userFilter.setValue(FILTER_ALL);
        userFilter.setPlaceholder("User");

        ComboBox<String> moduleFilter = new ComboBox<>();
        moduleFilter.setItems(FILTER_ALL, "Users", "Roles", "Organization", "Invoice");
        moduleFilter.setValue(FILTER_ALL);
        moduleFilter.setPlaceholder("Module");

        ComboBox<String> actionFilter = new ComboBox<>();
        actionFilter.setItems(FILTER_ALL, "CREATE", "UPDATE", "DELETE", "APPROVE");
        actionFilter.setValue(FILTER_ALL);
        actionFilter.setPlaceholder("Action");

        ComboBox<String> dateFilter = new ComboBox<>();
        dateFilter.setItems(FILTER_ALL, "17 Sep 2026", "16 Sep 2026");
        dateFilter.setValue(FILTER_ALL);
        dateFilter.setPlaceholder("Date");

        HorizontalLayout toolbar = new HorizontalLayout(search, userFilter, moduleFilter, actionFilter, dateFilter);
        toolbar.setWidthFull();
        toolbar.setAlignItems(HorizontalLayout.Alignment.CENTER);
        toolbar.setFlexGrow(1, search);

        Grid<MockAuditEntry> grid = new Grid<>();
        grid.setItems(MockAuditData.ALL);
        grid.addColumn(MockAuditEntry::dateTime).setHeader("Date & Time");
        grid.addColumn(MockAuditEntry::user).setHeader("User");
        grid.addColumn(MockAuditEntry::module).setHeader("Module");
        grid.addColumn(MockAuditEntry::action).setHeader("Action");
        grid.addColumn(MockAuditEntry::description).setHeader("Description");
        grid.addComponentColumn(entry -> statusLabel(entry.result())).setHeader("Status");
        grid.addComponentColumn(this::viewDetailsButton).setHeader("Details");
        grid.setSizeFull();

        search.addValueChangeListener(event -> grid.setItems(filter(event.getValue(), userFilter.getValue(),
                moduleFilter.getValue(), actionFilter.getValue(), dateFilter.getValue())));
        userFilter.addValueChangeListener(event -> grid.setItems(filter(search.getValue(), event.getValue(),
                moduleFilter.getValue(), actionFilter.getValue(), dateFilter.getValue())));
        moduleFilter.addValueChangeListener(event -> grid.setItems(filter(search.getValue(), userFilter.getValue(),
                event.getValue(), actionFilter.getValue(), dateFilter.getValue())));
        actionFilter.addValueChangeListener(event -> grid.setItems(filter(search.getValue(), userFilter.getValue(),
                moduleFilter.getValue(), event.getValue(), dateFilter.getValue())));
        dateFilter.addValueChangeListener(event -> grid.setItems(filter(search.getValue(), userFilter.getValue(),
                moduleFilter.getValue(), actionFilter.getValue(), event.getValue())));

        add(header, toolbar, grid);
        setFlexGrow(1, grid);
    }

    private List<MockAuditEntry> filter(String term, String user, String module, String action, String date) {
        String needle = term == null ? "" : term.trim().toLowerCase();
        return MockAuditData.ALL.stream()
                .filter(entry -> needle.isEmpty() || entry.description().toLowerCase().contains(needle))
                .filter(entry -> user == null || FILTER_ALL.equals(user) || entry.user().equals(user))
                .filter(entry -> module == null || FILTER_ALL.equals(module) || entry.module().equals(module))
                .filter(entry -> action == null || FILTER_ALL.equals(action) || entry.action().equals(action))
                .filter(entry -> date == null || FILTER_ALL.equals(date) || entry.dateTime().startsWith(date))
                .toList();
    }

    private Component statusLabel(String result) {
        Span label = new Span(result);
        boolean success = "Success".equals(result);
        label.getStyle()
                .set("padding", "0.15em 0.65em")
                .set("border-radius", "1em")
                .set("font-size", "0.85em")
                .set("font-weight", "600")
                .set("background", success ? "#e3f8ec" : "#fdecea")
                .set("color", success ? "#1a7f4b" : "#c0392b");
        return label;
    }

    private Component viewDetailsButton(MockAuditEntry entry) {
        return new Button("View Details", event -> showDetailDialog(entry));
    }

    private void showDetailDialog(MockAuditEntry entry) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Audit Detail");

        VerticalLayout content = new VerticalLayout(
                detailRow("Timestamp", entry.dateTime()),
                detailRow("User", entry.user()),
                detailRow("Module", entry.module()),
                detailRow("Action", entry.action()),
                detailRow("Description", entry.description()),
                detailRow("IP Address", entry.ipAddress()),
                detailRow("Result", entry.result()));
        content.setPadding(false);
        content.setSpacing(true);
        dialog.add(content);

        Button close = new Button("Close", event -> dialog.close());
        dialog.getFooter().add(close);

        dialog.open();
    }

    private Component detailRow(String label, String value) {
        H3 labelText = new H3(label);
        labelText.getStyle().set("margin", "0").set("font-size", "0.85em")
                .set("color", "var(--lumo-secondary-text-color)");
        Span valueText = new Span(value);
        valueText.getStyle().set("font-weight", "500");

        VerticalLayout row = new VerticalLayout(labelText, valueText);
        row.setPadding(false);
        row.setSpacing(false);
        return row;
    }
}
