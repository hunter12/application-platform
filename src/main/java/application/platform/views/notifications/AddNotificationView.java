package application.platform.views.notifications;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import application.platform.common.BusinessException;
import application.platform.notification.Notification;
import application.platform.notification.NotificationForm;
import application.platform.notification.NotificationService;
import application.platform.notification.NotificationType;
import application.platform.user.UserService;
import application.platform.user.UserSummary;
import application.platform.views.MainLayout;
import application.platform.views.ViewNotifications;

import jakarta.annotation.security.PermitAll;

@Route(value = "notifications/add", layout = MainLayout.class)
@PageTitle("Add Notification")
@PermitAll
public class AddNotificationView extends VerticalLayout {

    private final NotificationService notificationService;

    private final TextField titleField = new TextField("Title");
    private final ComboBox<NotificationType> type = new ComboBox<>("Notification Type");
    private final ComboBox<UserSummary> recipient = new ComboBox<>("Recipient");
    private final TextArea message = new TextArea("Message");

    public AddNotificationView(NotificationService notificationService, UserService userService) {
        this.notificationService = notificationService;
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H2 title = new H2("Add Notification");
        Span subtitle = new Span("Send an announcement to one user");
        subtitle.getStyle().set("color", "var(--lumo-secondary-text-color)");

        VerticalLayout header = new VerticalLayout(title, subtitle);
        header.setPadding(false);
        header.setSpacing(false);

        titleField.setRequiredIndicatorVisible(true);
        titleField.setMaxLength(Notification.TITLE_MAX_LENGTH);

        type.setRequiredIndicatorVisible(true);
        type.setItems(NotificationType.manualTypes());
        type.setItemLabelGenerator(NotificationType::getLabel);
        type.setValue(NotificationType.INFORMATION);

        recipient.setRequiredIndicatorVisible(true);
        recipient.setItems(userService.findAll().stream().filter(UserSummary::active).toList());
        recipient.setItemLabelGenerator(user -> user.fullName() + " (" + user.organizationName() + ")");

        message.setRequiredIndicatorVisible(true);
        message.setMaxLength(Notification.MESSAGE_MAX_LENGTH);
        message.setHelperText("Up to " + Notification.MESSAGE_MAX_LENGTH + " characters");
        message.setMinHeight("8rem");

        FlexLayout fields = twoColumnSection(titleField, type, recipient, message);

        Button cancel = new Button("Cancel", event -> getUI().ifPresent(ui -> ui.navigate("notifications")));

        Button save = new Button("Send Notification", event -> save());
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout actions = new HorizontalLayout(cancel, save);
        actions.setWidthFull();
        actions.setJustifyContentMode(HorizontalLayout.JustifyContentMode.END);

        VerticalLayout form = new VerticalLayout(header, sectionHeading("Notification Information"), fields,
                actions);
        form.setPadding(false);
        form.setSpacing(true);
        form.setMaxWidth("960px");

        add(form);
    }

    private void save() {
        NotificationForm form = new NotificationForm(
                recipient.getValue() == null ? null : recipient.getValue().id(),
                type.getValue(),
                titleField.getValue(),
                message.getValue());
        try {
            notificationService.send(form);
            ViewNotifications.showSuccess("Notification sent to " + recipient.getValue().fullName());
            getUI().ifPresent(ui -> ui.navigate("notifications"));
        } catch (BusinessException e) {
            ViewNotifications.showError(e.getMessage());
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
