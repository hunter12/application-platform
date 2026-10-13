package application.platform.views;

import java.util.List;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.PermitAll;

@Route(value = "", layout = MainLayout.class)
@PageTitle("Dashboard")
@PermitAll
public class DashboardView extends VerticalLayout {

    private static final List<String> RECENT_ACTIVITY = List.of(
            "New user \"jane.doe\" was added - 10 minutes ago",
            "Approval request #204 was submitted - 32 minutes ago",
            "Organization \"Branch C\" was updated - 1 hour ago",
            "Notification policy was changed - 3 hours ago");

    public DashboardView() {
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H2 heading = new H2("Dashboard");

        HorizontalLayout cards = new HorizontalLayout(
                statCard("Total Users", "128"),
                statCard("Active Users", "97"),
                statCard("Pending Approvals", "12"),
                statCard("Notifications", "5"));
        cards.setWidthFull();
        cards.getStyle().set("flex-wrap", "wrap");
        cards.getChildren().forEach(card -> cards.setFlexGrow(1, card));

        add(heading, cards, recentActivityCard());
    }

    private Card statCard(String label, String value) {
        Card card = new Card();
        card.setTitle(label);

        Span valueSpan = new Span(value);
        valueSpan.getStyle().set("font-size", "2rem").set("font-weight", "700");
        card.add(valueSpan);

        card.setMinWidth("180px");
        return card;
    }

    private Component recentActivityCard() {
        Card card = new Card();
        card.setTitle("Recent Activity");

        VerticalLayout list = new VerticalLayout();
        list.setPadding(false);
        list.setSpacing(false);

        RECENT_ACTIVITY.forEach(entry -> {
            Div row = new Div(new Span(entry));
            row.getStyle()
                    .set("padding", "0.5em 0")
                    .set("border-bottom", "1px solid #e5e5e5");
            list.add(row);
        });

        card.add(list);
        return card;
    }
}
