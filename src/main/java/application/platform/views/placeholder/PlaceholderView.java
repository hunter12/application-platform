package application.platform.views.placeholder;

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

public abstract class PlaceholderView extends VerticalLayout {

    protected PlaceholderView(String title, String message) {
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H2 heading = new H2(title);
        Paragraph body = new Paragraph(message);
        body.getStyle().set("color", "#666666");

        add(heading, body);
    }
}
