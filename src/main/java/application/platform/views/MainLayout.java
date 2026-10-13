package application.platform.views;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.spring.security.AuthenticationContext;

import jakarta.annotation.security.PermitAll;

import application.platform.views.approval.ApprovalRuleView;
import application.platform.views.approval.ApprovalWorkflowView;
import application.platform.views.approval.DelegationView;
import application.platform.views.audit.AuditLogView;
import application.platform.views.modules.Module1View;
import application.platform.views.modules.Module2View;
import application.platform.views.modules.Module3View;
import application.platform.views.notifications.NotificationPolicyView;
import application.platform.views.notifications.NotificationView;
import application.platform.views.organization.OrganizationHierarchyView;
import application.platform.views.organization.OrganizationView;
import application.platform.views.permissions.PermissionView;
import application.platform.views.roles.RoleView;
import application.platform.views.users.UsersView;

@PermitAll
public class MainLayout extends AppLayout implements RouterLayout {

    public MainLayout(AuthenticationContext authenticationContext) {
        addToNavbar(buildHeader(authenticationContext));
        addToDrawer(new Scroller(buildNavigation()));
    }

    private HorizontalLayout buildHeader(AuthenticationContext authenticationContext) {
        DrawerToggle toggle = new DrawerToggle();

        H1 appTitle = new H1("Application Platform");
        appTitle.getStyle().set("font-size", "1.25rem").set("margin", "0");

        Div spacer = new Div();
        spacer.getStyle().set("flex-grow", "1");

        String username = authenticationContext.getPrincipalName().orElse("User");
        Avatar avatar = new Avatar(username);
        Span usernameLabel = new Span(username);

        Button logout = new Button("Log out", event -> authenticationContext.logout());

        HorizontalLayout userArea = new HorizontalLayout(avatar, usernameLabel, logout);
        userArea.setAlignItems(HorizontalLayout.Alignment.CENTER);
        userArea.setSpacing(true);

        HorizontalLayout header = new HorizontalLayout(toggle, appTitle, spacer, userArea);
        header.setWidthFull();
        header.setAlignItems(HorizontalLayout.Alignment.CENTER);
        header.setPadding(true);
        return header;
    }

    private SideNav buildNavigation() {
        SideNav nav = new SideNav();

        nav.addItem(new SideNavItem("Dashboard", DashboardView.class));

        SideNavItem administration = new SideNavItem("Administration");
        administration.addItem(new SideNavItem("Users", UsersView.class));
        administration.addItem(new SideNavItem("Roles", RoleView.class));
        administration.addItem(new SideNavItem("Permissions", PermissionView.class));
        nav.addItem(administration);

        SideNavItem organization = new SideNavItem("Organization");
        organization.addItem(new SideNavItem("Organizations", OrganizationView.class));
        organization.addItem(new SideNavItem("Organization Hierarchy", OrganizationHierarchyView.class));
        nav.addItem(organization);

        SideNavItem approval = new SideNavItem("Approval");
        approval.addItem(new SideNavItem("Workflows", ApprovalWorkflowView.class));
        approval.addItem(new SideNavItem("Approval Rules", ApprovalRuleView.class));
        approval.addItem(new SideNavItem("Delegation", DelegationView.class));
        nav.addItem(approval);

        SideNavItem notification = new SideNavItem("Notification");
        notification.addItem(new SideNavItem("Notifications", NotificationView.class));
        notification.addItem(new SideNavItem("Notification Policy", NotificationPolicyView.class));
        nav.addItem(notification);

        SideNavItem audit = new SideNavItem("Audit");
        audit.addItem(new SideNavItem("Audit Log", AuditLogView.class));
        nav.addItem(audit);

        SideNavItem businessModules = new SideNavItem("Business Modules");
        businessModules.addItem(new SideNavItem("Module 1", Module1View.class));
        businessModules.addItem(new SideNavItem("Module 2", Module2View.class));
        businessModules.addItem(new SideNavItem("Module 3", Module3View.class));
        nav.addItem(businessModules);

        return nav;
    }
}
