package application.platform.module;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AppModuleRepository extends JpaRepository<AppModule, UUID> {

    Optional<AppModule> findByCode(String code);
}
