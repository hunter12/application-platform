package application.platform.permission;

import static application.platform.common.ServiceSupport.checkVersion;
import static application.platform.common.ServiceSupport.ensureUnique;
import static application.platform.common.ServiceSupport.normalizeCode;
import static application.platform.common.ServiceSupport.optionalText;
import static application.platform.common.ServiceSupport.requireText;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import application.platform.common.BusinessException;

@Service
@Transactional
public class PermissionService {

    private final PermissionRepository repository;

    public PermissionService(PermissionRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<PermissionSummary> findAll() {
        return repository.findAll(Sort.by("code")).stream().map(PermissionSummary::of).toList();
    }

    @Transactional(readOnly = true)
    public Optional<PermissionSummary> findById(UUID id) {
        return repository.findById(id).map(PermissionSummary::of);
    }

    /**
     * Creates or updates a permission. The code is fixed once created,
     * because it is what authorization checks refer to.
     */
    public PermissionSummary save(PermissionForm form) {
        String name = requireText(form.name(), "Permission name");
        String module = requireText(form.module(), "Module");
        String action = requireText(form.action(), "Action");

        Permission permission;
        if (form.id() == null) {
            String code = normalizeCode(form.code(), "Permission code");
            ensureUnique(repository.countByCodeIncludingDeleted(code), false,
                    "Permission code " + code + " is already used");
            permission = new Permission(code, name, module, action, optionalText(form.description()));
        } else {
            permission = load(form.id());
            checkVersion(permission, form.version());
            permission.setName(name);
            permission.setModule(module);
            permission.setAction(action);
            permission.setDescription(optionalText(form.description()));
        }
        permission.setActive(form.active());
        return PermissionSummary.of(repository.saveAndFlush(permission));
    }

    public void setActive(UUID id, boolean active) {
        load(id).setActive(active);
    }

    private Permission load(UUID id) {
        return repository.findById(id).orElseThrow(() -> new BusinessException("Permission not found"));
    }
}
