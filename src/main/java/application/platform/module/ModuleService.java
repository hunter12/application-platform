package application.platform.module;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ModuleService {

    private final AppModuleRepository moduleRepository;

    public ModuleService(AppModuleRepository moduleRepository) {
        this.moduleRepository = moduleRepository;
    }

    public List<ModuleSummary> findAll() {
        return moduleRepository.findAll(Sort.by("name")).stream().map(ModuleSummary::of).toList();
    }
}
