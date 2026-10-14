package application.platform.module;

import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ModuleSeeder implements ApplicationRunner {

    private final AppModuleRepository moduleRepository;

    public ModuleSeeder(AppModuleRepository moduleRepository) {
        this.moduleRepository = moduleRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (moduleRepository.count() > 0) {
            return;
        }
        moduleRepository.saveAll(List.of(
                new AppModule("FINANCE", "Finance"),
                new AppModule("INVOICE", "Invoice"),
                new AppModule("STOCK", "Stock"),
                new AppModule("PRODUCT", "Product"),
                new AppModule("REPORT", "Report")));
    }
}
