package application.platform.views.modules;

import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import application.platform.views.MainLayout;

import jakarta.annotation.security.PermitAll;

/**
 * Visual prototype dashboard for Module 3. Hardcoded values only - no
 * business logic.
 */
@Route(value = "modules/module3", layout = MainLayout.class)
@PageTitle("Module 3")
@PermitAll
public class Module3View extends ModuleDashboardView {

    public Module3View() {
        super("Module 3", "Business module prototype", "Inactive", 12, 0, "10 Sep 2026 08:05");
    }
}
