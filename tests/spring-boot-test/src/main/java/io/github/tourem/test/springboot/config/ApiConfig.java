package io.github.tourem.test.springboot.config;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@ConfigurationProperties(prefix = "api")
@Validated
public class ApiConfig {
    @NotNull(message = "api.endpoint is required")
    private String endpoint;
    
    @NotNull(message = "api.api-key is required")
    private String apiKey;
    
    @NotNull(message = "api.retry-count is required")
    private Integer retryCount;
    
    @NotNull(message = "api.enable-cache is required")
    private Boolean enableCache;
    
    @NotNull(message = "api.cache-directory is required")
    private String cacheDirectory;

    // Getters and Setters
    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public Boolean getEnableCache() {
        return enableCache;
    }

    public void setEnableCache(Boolean enableCache) {
        this.enableCache = enableCache;
    }

    public String getCacheDirectory() {
        return cacheDirectory;
    }

    public void setCacheDirectory(String cacheDirectory) {
        this.cacheDirectory = cacheDirectory;
    }
}
