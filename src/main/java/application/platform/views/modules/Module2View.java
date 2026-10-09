package application.platform.views.modules;

import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import application.platform.views.MainLayout;

import jakarta.annotation.security.PermitAll;

/**
 * Visual prototype dashboard for Module 2. Hardcoded values only - no
 * business logic.
 */
@Route(value = "modules/module2", layout = MainLayout.class)
@PageTitle("Module 2")
@PermitAll
public class Module2View extends ModuleDashboardView {

    public Module2View() {
        super("Module 2", "Business module prototype", "Active", 74, 3, "16 Sep 2026 15:20");
    }
}
