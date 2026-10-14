package application.platform.module;

import java.util.UUID;

public record ModuleSummary(UUID id, String code, String name, boolean active) {

    static ModuleSummary of(AppModule module) {
        return new ModuleSummary(module.getId(), module.getCode(), module.getName(), module.isActive());
    }
}
