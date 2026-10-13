package application.platform.views.modules;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

abstract class ModuleDashboardView extends VerticalLayout {

    protected ModuleDashboardView(String title, String subtitle, String status, int totalRecords, int pendingItems,
            String lastActivity) {
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H2 titleHeading = new H2(title);
        Span subtitleText = new Span(subtitle);
        subtitleText.getStyle().set("color", "var(--lumo-secondary-text-color)");

        VerticalLayout header = new VerticalLayout(titleHeading, subtitleText);
        header.setPadding(false);
        header.setSpacing(false);

        HorizontalLayout cards = new HorizontalLayout(
                card("Module Status", status),
                card("Total Records", String.valueOf(totalRecords)),
                card("Pending Items", String.valueOf(pendingItems)),
                card("Last Activity", lastActivity));
        cards.setWidthFull();
        cards.getStyle().set("flex-wrap", "wrap");

        add(header, cards);
    }

    private Component card(String label, String value) {
        Span labelText = new Span(label);
        labelText.getStyle().set("font-size", "0.85em").set("color", "var(--lumo-secondary-text-color)");

        H3 valueText = new H3(value);
        valueText.getStyle().set("margin", "0.2em 0 0 0");

        VerticalLayout card = new VerticalLayout(labelText, valueText);
        card.setPadding(true);
        card.setSpacing(false);
        card.getStyle()
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("border-radius", "var(--lumo-border-radius-l)")
                .set("flex", "1 1 200px")
                .set("min-width", "180px");
        return card;
    }
}
