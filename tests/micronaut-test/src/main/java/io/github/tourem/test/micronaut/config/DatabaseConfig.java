package io.github.tourem.test.micronaut.config;

import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.context.annotation.Context;
import io.micronaut.core.annotation.Introspected;
import jakarta.inject.Singleton;
import jakarta.validation.constraints.NotNull;

@Context
@Singleton
@ConfigurationProperties("database")
@Introspected
public class DatabaseConfig {
    @NotNull(message = "database.url is required")
    private String url;
    
    @NotNull(message = "database.username is required")
    private String username;
    
    @NotNull(message = "database.password is required")
    private String password;
    
    @NotNull(message = "database.max-connections is required")
    private Integer maxConnections;
    
    @NotNull(message = "database.timeout is required")
    private Long timeout;

    // Getters and Setters
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Integer getMaxConnections() {
        return maxConnections;
    }

    public void setMaxConnections(Integer maxConnections) {
        this.maxConnections = maxConnections;
    }

    public Long getTimeout() {
        return timeout;
    }

    public void setTimeout(Long timeout) {
        this.timeout = timeout;
    }

    @Override
    public String toString() {
        return "DatabaseConfig{" +
                "url='" + url + '\'' +
                ", username='" + username + '\'' +
                ", password='" + (password != null ? "***" : "null") + '\'' +
                ", maxConnections=" + maxConnections +
                ", timeout=" + timeout +
                '}';
    }
}
