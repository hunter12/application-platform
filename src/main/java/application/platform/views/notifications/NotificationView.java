package application.platform.views.notifications;

import java.util.List;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import application.platform.views.MainLayout;

import jakarta.annotation.security.PermitAll;

@Route(value = "notifications", layout = MainLayout.class)
@PageTitle("Notifications")
@PermitAll
public class NotificationView extends VerticalLayout {

    private static final String FILTER_ALL = "All";

    private final VerticalLayout list = new VerticalLayout();

    public NotificationView() {
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H2 title = new H2("Notifications");
        Span subtitle = new Span("View system notifications");
        subtitle.getStyle().set("color", "var(--lumo-secondary-text-color)");

        VerticalLayout header = new VerticalLayout(title, subtitle);
        header.setPadding(false);
        header.setSpacing(false);

        TextField search = new TextField();
        search.setPlaceholder("Search notification...");
        search.setClearButtonVisible(true);
        search.setPrefixComponent(VaadinIcon.SEARCH.create());

        ComboBox<String> typeFilter = new ComboBox<>();
        typeFilter.setItems(FILTER_ALL, "Approval", "Delegation", "System", "Information");
        typeFilter.setValue(FILTER_ALL);
        typeFilter.setPlaceholder("Type");

        ComboBox<String> statusFilter = new ComboBox<>();
        statusFilter.setItems(FILTER_ALL, "Unread", "Read");
        statusFilter.setValue(FILTER_ALL);
        statusFilter.setPlaceholder("Status");

        Button addNotification = new Button("Add Notification", VaadinIcon.PLUS.create(),
                event -> getUI().ifPresent(ui -> ui.navigate("notifications/add")));

        HorizontalLayout toolbar = new HorizontalLayout(search, typeFilter, statusFilter, addNotification);
        toolbar.setWidthFull();
        toolbar.setAlignItems(HorizontalLayout.Alignment.CENTER);
        toolbar.setFlexGrow(1, search);

        list.setPadding(false);
        list.setSpacing(true);
        list.setWidthFull();

        search.addValueChangeListener(
                event -> renderList(filter(event.getValue(), typeFilter.getValue(), statusFilter.getValue())));
        typeFilter.addValueChangeListener(
                event -> renderList(filter(search.getValue(), event.getValue(), statusFilter.getValue())));
        statusFilter.addValueChangeListener(
                event -> renderList(filter(search.getValue(), typeFilter.getValue(), event.getValue())));

        renderList(MockNotificationData.ALL);

        add(header, toolbar, list);
        setFlexGrow(1, list);
    }

    private List<MockNotification> filter(String term, String type, String status) {
        String needle = term == null ? "" : term.trim().toLowerCase();
        return MockNotificationData.ALL.stream()
                .filter(notification -> needle.isEmpty() || notification.title().toLowerCase().contains(needle)
                        || notification.message().toLowerCase().contains(needle))
                .filter(notification -> type == null || FILTER_ALL.equals(type)
                        || notification.type().equals(type))
                .filter(notification -> status == null || FILTER_ALL.equals(status)
                        || notification.status().equals(status))
                .toList();
    }

    private void renderList(List<MockNotification> notifications) {
        list.removeAll();
        notifications.forEach(notification -> list.add(notificationCard(notification)));
    }

    private Component notificationCard(MockNotification notification) {
        Icon icon = typeIcon(notification.type());
        icon.setSize("1.25rem");

        H4 title = new H4(notification.title());
        title.getStyle().set("margin", "0");

        Paragraph message = new Paragraph(notification.message());
        message.getStyle().set("margin", "0.15em 0 0.35em 0").set("color", "var(--lumo-secondary-text-color)");

        Span typeBadge = new Span(notification.type());
        typeBadge.getStyle()
                .set("padding", "0.1em 0.6em")
                .set("border-radius", "1em")
                .set("font-size", "0.8em")
                .set("background", "#eef1fb")
                .set("color", "#3949ab");

        Span date = new Span(notification.date());
        date.getStyle().set("color", "var(--lumo-secondary-text-color)").set("font-size", "0.85em");

        HorizontalLayout meta = new HorizontalLayout(typeBadge, date);
        meta.setAlignItems(HorizontalLayout.Alignment.CENTER);
        meta.setSpacing(true);

        VerticalLayout content = new VerticalLayout(title, message, meta);
        content.setPadding(false);
        content.setSpacing(false);

        Button markAsRead = new Button("Mark as Read", event -> showPrototypeNotice("Marking notifications as read"));
        Button view = new Button("View", event -> showPrototypeNotice("Viewing notifications"));
        Button delete = new Button("Delete", event -> showPrototypeNotice("Deleting notifications"));
        delete.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);

        HorizontalLayout actions = new HorizontalLayout(markAsRead, view, delete);

        Span statusBadge = statusLabel(notification.status());

        HorizontalLayout row = new HorizontalLayout(icon, content, statusBadge, actions);
        row.setWidthFull();
        row.setAlignItems(HorizontalLayout.Alignment.START);
        row.setFlexGrow(1, content);
        row.getStyle()
                .set("padding", "0.75rem 1rem")
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("border-radius", "var(--lumo-border-radius-l)");

        return row;
    }

    private Icon typeIcon(String type) {
        return switch (type) {
            case "Approval" -> VaadinIcon.CHECK_CIRCLE.create();
            case "Delegation" -> VaadinIcon.USERS.create();
            case "System" -> VaadinIcon.COG.create();
            default -> VaadinIcon.BELL.create();
        };
    }

    private Span statusLabel(String status) {
        Span label = new Span(status);
        boolean unread = "Unread".equals(status);
        label.getStyle()
                .set("padding", "0.15em 0.65em")
                .set("border-radius", "1em")
                .set("font-size", "0.85em")
                .set("font-weight", "600")
                .set("background", unread ? "#e8f0fe" : "#eeeeee")
                .set("color", unread ? "#1a56db" : "#555555");
        return label;
    }

    private void showPrototypeNotice(String action) {
        Notification.show(action + " is a UI prototype only - no data is persisted.", 3000,
                Notification.Position.BOTTOM_START);
    }
}
