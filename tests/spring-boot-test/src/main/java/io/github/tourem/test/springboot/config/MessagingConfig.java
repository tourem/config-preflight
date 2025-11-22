package io.github.tourem.test.springboot.config;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@ConfigurationProperties(prefix = "messaging")
@Validated
public class MessagingConfig {
    @NotNull(message = "messaging.broker-url is required")
    private String brokerUrl;
    
    @NotNull(message = "messaging.queue-name is required")
    private String queueName;
    
    @NotNull(message = "messaging.username is required")
    private String username;
    
    @NotNull(message = "messaging.password is required")
    private String password;
    
    @NotNull(message = "messaging.connection-timeout is required")
    private Integer connectionTimeout;
    
    @NotNull(message = "messaging.auto-reconnect is required")
    private Boolean autoReconnect;

    // Getters and Setters
    public String getBrokerUrl() {
        return brokerUrl;
    }

    public void setBrokerUrl(String brokerUrl) {
        this.brokerUrl = brokerUrl;
    }

    public String getQueueName() {
        return queueName;
    }

    public void setQueueName(String queueName) {
        this.queueName = queueName;
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

    public Integer getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(Integer connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public Boolean getAutoReconnect() {
        return autoReconnect;
    }

    public void setAutoReconnect(Boolean autoReconnect) {
        this.autoReconnect = autoReconnect;
    }
}
