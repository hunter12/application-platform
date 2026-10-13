package application.platform.views;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.provider.hierarchy.TreeDataProvider;
import com.vaadin.flow.spring.security.AuthenticationContext;

import application.platform.views.organization.AddOrganizationView;
import application.platform.views.organization.OrganizationHierarchyView;
import application.platform.views.organization.OrganizationView;
import application.platform.views.permissions.AddPermissionView;
import application.platform.views.permissions.PermissionView;
import application.platform.views.roles.AddRoleView;
import application.platform.views.roles.RoleView;
import application.platform.views.users.AddUserView;
import application.platform.views.users.UsersView;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("dev")
@WithMockUser(username = "admin")
class AdministrationViewsTest extends SpringBrowserlessTest {

    @MockitoBean
    private AuthenticationContext authenticationContext;

    @Test
    void listPagesShowSeededData() {
        navigate(UsersView.class);
        assertThat(gridSize()).isEqualTo(1);

        navigate(RoleView.class);
        assertThat(gridSize()).isEqualTo(6);

        navigate(PermissionView.class);
        assertThat(gridSize()).isEqualTo(12);

        navigate(OrganizationView.class);
        assertThat(gridSize()).isEqualTo(10);
    }

    @Test
    void hierarchyHasHeadOfficeAsSingleRoot() {
        navigate(OrganizationHierarchyView.class);

        TreeGrid<?> treeGrid = find(TreeGrid.class).single();
        TreeDataProvider<?> provider = (TreeDataProvider<?>) (Object) treeGrid.getDataProvider();
        assertThat(provider.getTreeData().getRootItems()).hasSize(1);
    }

    @Test
    void formsOpenForAddAndEdit() {
        navigate(AddUserView.class);
        navigate(AddRoleView.class);
        navigate(AddPermissionView.class);
        navigate(AddOrganizationView.class);

        navigate("users", UsersView.class);
        @SuppressWarnings("unchecked")
        Grid<application.platform.user.UserSummary> users = find(Grid.class).single();
        String adminId = users.getGenericDataView().getItems().findFirst().orElseThrow().id().toString();

        navigate("users/edit/" + adminId, AddUserView.class);
        navigate("users/edit/not-a-uuid", UsersView.class);
    }

    private long gridSize() {
        Grid<?> grid = find(Grid.class).single();
        return grid.getGenericDataView().getItems().count();
    }
}
