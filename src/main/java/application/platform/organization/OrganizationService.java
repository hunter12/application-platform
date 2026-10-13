package application.platform.organization;

import static application.platform.common.ServiceSupport.checkVersion;
import static application.platform.common.ServiceSupport.ensureUnique;
import static application.platform.common.ServiceSupport.normalizeCode;
import static application.platform.common.ServiceSupport.require;
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
public class OrganizationService {

    private final OrganizationRepository repository;

    public OrganizationService(OrganizationRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<OrganizationSummary> findAll() {
        return repository.findAll(Sort.by("name")).stream().map(OrganizationSummary::of).toList();
    }

    @Transactional(readOnly = true)
    public Optional<OrganizationSummary> findById(UUID id) {
        return repository.findById(id).map(OrganizationSummary::of);
    }

    public OrganizationSummary save(OrganizationForm form) {
        String code = normalizeCode(form.code(), "Organization code");
        String name = requireText(form.name(), "Organization name");
        OrganizationType type = require(form.type(), "Organization type");

        Organization organization = form.id() == null ? null : load(form.id());
        if (organization != null) {
            checkVersion(organization, form.version());
        }
        ensureUnique(repository.countByCodeIncludingDeleted(code),
                organization != null && code.equals(organization.getCode()),
                "Organization code " + code + " is already used");

        Organization parent = form.parentId() == null ? null : load(form.parentId());
        if (organization != null) {
            // Moving a unit under itself or one of its own sub-units would turn the tree into a loop.
            for (Organization ancestor = parent; ancestor != null; ancestor = ancestor.getParent()) {
                if (ancestor.getId().equals(organization.getId())) {
                    throw new BusinessException("An organization cannot be placed under itself or its sub-units");
                }
            }
        }

        if (organization == null) {
            organization = new Organization(code, name, type, parent);
        } else {
            organization.setCode(code);
            organization.setName(name);
            organization.setType(type);
            organization.setParent(parent);
        }
        organization.setActive(form.active());
        return OrganizationSummary.of(repository.saveAndFlush(organization));
    }

    public void setActive(UUID id, boolean active) {
        load(id).setActive(active);
    }

    private Organization load(UUID id) {
        return repository.findById(id).orElseThrow(() -> new BusinessException("Organization not found"));
    }
}
