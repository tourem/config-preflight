package io.github.tourem.test.quarkus.config;

import io.smallrye.config.ConfigMapping;
import jakarta.validation.constraints.NotNull;

@ConfigMapping(prefix = "database")
public interface DatabaseConfig {
    @NotNull(message = "database.url is required")
    String url();
    
    @NotNull(message = "database.username is required")
    String username();
    
    @NotNull(message = "database.password is required")
    String password();
    
    @NotNull(message = "database.max-connections is required")
    Integer maxConnections();
    
    @NotNull(message = "database.timeout is required")
    Long timeout();
}
