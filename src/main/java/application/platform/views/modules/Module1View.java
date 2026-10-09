package application.platform.views.modules;

import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import application.platform.views.MainLayout;

import jakarta.annotation.security.PermitAll;

/**
 * Visual prototype dashboard for Module 1. Hardcoded values only - no
 * business logic.
 */
@Route(value = "modules/module1", layout = MainLayout.class)
@PageTitle("Module 1")
@PermitAll
public class Module1View extends ModuleDashboardView {

    public Module1View() {
        super("Module 1", "Business module prototype", "Active", 128, 6, "17 Sep 2026 10:30");
    }
}
