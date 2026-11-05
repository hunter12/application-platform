package application.platform.views.notifications;

import java.util.List;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import application.platform.common.BusinessException;
import application.platform.common.Dates;
import application.platform.notification.NotificationService;
import application.platform.notification.NotificationSummary;
import application.platform.notification.NotificationType;
import application.platform.views.MainLayout;
import application.platform.views.ViewNotifications;

import jakarta.annotation.security.PermitAll;

@Route(value = "notifications", layout = MainLayout.class)
@PageTitle("Notifications")
@PermitAll
public class NotificationView extends VerticalLayout {

    private static final String UNREAD = "Unread";
    private static final String READ = "Read";

    private final NotificationService notificationService;
    private final VerticalLayout list = new VerticalLayout();
    private final TextField search = new TextField();
    private final ComboBox<NotificationType> typeFilter = new ComboBox<>();
    private final ComboBox<String> statusFilter = new ComboBox<>();
    private final Button markAllAsRead = new Button("Mark all as read", VaadinIcon.CHECK.create());
    private List<NotificationSummary> notifications = List.of();

    public NotificationView(NotificationService notificationService) {
        this.notificationService = notificationService;
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H2 title = new H2("Notifications");
        Span subtitle = new Span("Your notifications");
        subtitle.getStyle().set("color", "var(--lumo-secondary-text-color)");

        VerticalLayout header = new VerticalLayout(title, subtitle);
        header.setPadding(false);
        header.setSpacing(false);

        search.setPlaceholder("Search notification...");
        search.setClearButtonVisible(true);
        search.setPrefixComponent(VaadinIcon.SEARCH.create());

        typeFilter.setItems(NotificationType.values());
        typeFilter.setItemLabelGenerator(NotificationType::getLabel);
        typeFilter.setPlaceholder("All types");
        typeFilter.setClearButtonVisible(true);

        statusFilter.setItems(UNREAD, READ);
        statusFilter.setPlaceholder("All statuses");
        statusFilter.setClearButtonVisible(true);

        markAllAsRead.addClickListener(event -> {
            notificationService.markAllAsRead();
            ViewNotifications.showSuccess("All notifications marked as read");
            refresh();
        });

        Button addNotification = new Button("Add Notification", VaadinIcon.PLUS.create(),
                event -> getUI().ifPresent(ui -> ui.navigate("notifications/add")));

        HorizontalLayout toolbar = new HorizontalLayout(search, typeFilter, statusFilter, markAllAsRead,
                addNotification);
        toolbar.setWidthFull();
        toolbar.setAlignItems(HorizontalLayout.Alignment.CENTER);
        toolbar.setFlexGrow(1, search);

        list.setPadding(false);
        list.setSpacing(true);
        list.setWidthFull();

        search.addValueChangeListener(event -> applyFilter());
        typeFilter.addValueChangeListener(event -> applyFilter());
        statusFilter.addValueChangeListener(event -> applyFilter());

        add(header, toolbar, list);
        setFlexGrow(1, list);
        refresh();
    }

    private void refresh() {
        notifications = notificationService.findMine();
        markAllAsRead.setEnabled(notifications.stream().anyMatch(notification -> !notification.read()));
        applyFilter();
        MainLayout layout = findAncestor(MainLayout.class);
        if (layout != null) {
            layout.refreshUnreadCount();
        }
    }

    private void applyFilter() {
        String needle = search.getValue() == null ? "" : search.getValue().trim().toLowerCase();
        NotificationType type = typeFilter.getValue();
        String status = statusFilter.getValue();
        List<NotificationSummary> filtered = notifications.stream()
                .filter(notification -> needle.isEmpty() || notification.title().toLowerCase().contains(needle)
                        || notification.message().toLowerCase().contains(needle))
                .filter(notification -> type == null || notification.type() == type)
                .filter(notification -> status == null || notification.read() == READ.equals(status))
                .toList();

        list.removeAll();
        if (filtered.isEmpty()) {
            Span empty = new Span(notifications.isEmpty() ? "You have no notifications" : "No matching notifications");
            empty.getStyle().set("color", "var(--lumo-secondary-text-color)");
            list.add(empty);
        }
        filtered.forEach(notification -> list.add(notificationCard(notification)));
    }

    private Component notificationCard(NotificationSummary notification) {
        Icon icon = typeIcon(notification.type());
        icon.setSize("1.25rem");

        H4 title = new H4(notification.title());
        title.getStyle().set("margin", "0");

        Paragraph message = new Paragraph(notification.message());
        message.getStyle()
                .set("margin", "0.15em 0 0.35em 0")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("overflow", "hidden")
                .set("text-overflow", "ellipsis")
                .set("white-space", "nowrap")
                .set("max-width", "60ch");

        Span date = new Span(Dates.format(notification.createdDate()) + " · " + senderLabel(notification));
        date.getStyle().set("color", "var(--lumo-secondary-text-color)").set("font-size", "0.85em");

        HorizontalLayout meta = new HorizontalLayout(typeBadge(notification.type()), date);
        meta.setAlignItems(HorizontalLayout.Alignment.CENTER);
        meta.setSpacing(true);

        VerticalLayout content = new VerticalLayout(title, message, meta);
        content.setPadding(false);
        content.setSpacing(false);

        HorizontalLayout actions = new HorizontalLayout();
        if (!notification.read()) {
            actions.add(new Button("Mark as Read", event -> markAsRead(notification)));
        }
        actions.add(new Button("View", event -> openDetailDialog(notification)));
        Button delete = new Button("Delete", event -> confirmDelete(notification));
        delete.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
        actions.add(delete);

        HorizontalLayout row = new HorizontalLayout(icon, content, statusLabel(notification.read()), actions);
        row.setWidthFull();
        row.setAlignItems(HorizontalLayout.Alignment.START);
        row.setFlexGrow(1, content);
        row.getStyle()
                .set("padding", "0.75rem 1rem")
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("border-radius", "var(--lumo-border-radius-l)");
        if (!notification.read()) {
            row.getStyle().set("border-left", "4px solid #1a56db");
        }

        return row;
    }

    private void markAsRead(NotificationSummary notification) {
        try {
            notificationService.markAsRead(notification.id());
        } catch (BusinessException e) {
            ViewNotifications.showError(e.getMessage());
        }
        refresh();
    }

    private void openDetailDialog(NotificationSummary notification) {
        Paragraph message = new Paragraph(notification.message());
        message.getStyle().set("white-space", "pre-wrap");

        Span meta = new Span(notification.type().getLabel() + " · " + Dates.format(notification.createdDate())
                + " · " + senderLabel(notification));
        meta.getStyle().set("color", "var(--lumo-secondary-text-color)").set("font-size", "0.85em");

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(notification.title());
        dialog.setWidth("560px");
        dialog.add(meta, message);
        dialog.getFooter().add(new Button("Close", event -> dialog.close()));
        dialog.open();

        if (!notification.read()) {
            markAsRead(notification);
        }
    }

    private void confirmDelete(NotificationSummary notification) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Delete notification?");
        dialog.add(new Span("\"" + notification.title() + "\" will be removed from your notifications."));

        Button keep = new Button("Keep", event -> dialog.close());
        Button delete = new Button("Delete", event -> {
            try {
                notificationService.delete(notification.id());
                ViewNotifications.showSuccess("Notification deleted");
            } catch (BusinessException e) {
                ViewNotifications.showError(e.getMessage());
            }
            dialog.close();
            refresh();
        });
        delete.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);
        dialog.getFooter().add(keep, delete);
        dialog.open();
    }

    private static String senderLabel(NotificationSummary notification) {
        return notification.senderName() == null ? "System" : "From " + notification.senderName();
    }

    private static Icon typeIcon(NotificationType type) {
        return switch (type) {
            case APPROVAL -> VaadinIcon.CHECK_CIRCLE.create();
            case DELEGATION -> VaadinIcon.USERS.create();
            case SYSTEM -> VaadinIcon.COG.create();
            case INFORMATION -> VaadinIcon.BELL.create();
        };
    }

    private static Span typeBadge(NotificationType type) {
        Span badge = new Span(type.getLabel());
        badge.getStyle()
                .set("padding", "0.1em 0.6em")
                .set("border-radius", "1em")
                .set("font-size", "0.8em")
                .set("background", "#eef1fb")
                .set("color", "#3949ab");
        return badge;
    }

    private static Span statusLabel(boolean read) {
        Span label = new Span(read ? READ : UNREAD);
        label.getStyle()
                .set("padding", "0.15em 0.65em")
                .set("border-radius", "1em")
                .set("font-size", "0.85em")
                .set("font-weight", "600")
                .set("background", read ? "#eeeeee" : "#e8f0fe")
                .set("color", read ? "#555555" : "#1a56db");
        return label;
    }
}
