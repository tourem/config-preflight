package com.mycompany.validator.core.model;

/**
 * Représente une erreur de configuration détectée.
 */
public class ConfigurationError {
    
    private final ErrorType type;
    private final String propertyName;
    private final String errorMessage;
    private final String suggestion;
    private final PropertySource source;
    private final boolean isSensitive;
    
    private ConfigurationError(Builder builder) {
        this.type = builder.type;
        this.propertyName = builder.propertyName;
        this.errorMessage = builder.errorMessage;
        this.suggestion = builder.suggestion;
        this.source = builder.source;
        this.isSensitive = builder.isSensitive;
    }
    
    public ErrorType getType() {
        return type;
    }
    
    public String getPropertyName() {
        return propertyName;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public String getSuggestion() {
        return suggestion;
    }
    
    public PropertySource getSource() {
        return source;
    }
    
    public boolean isSensitive() {
        return isSensitive;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private ErrorType type;
        private String propertyName;
        private String errorMessage;
        private String suggestion;
        private PropertySource source;
        private boolean isSensitive;
        
        public Builder type(ErrorType type) {
            this.type = type;
            return this;
        }
        
        public Builder propertyName(String propertyName) {
            this.propertyName = propertyName;
            return this;
        }
        
        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }
        
        public Builder suggestion(String suggestion) {
            this.suggestion = suggestion;
            return this;
        }
        
        public Builder source(PropertySource source) {
            this.source = source;
            return this;
        }
        
        public Builder isSensitive(boolean isSensitive) {
            this.isSensitive = isSensitive;
            return this;
        }
        
        public ConfigurationError build() {
            if (type == null) {
                throw new IllegalStateException("ErrorType is required");
            }
            if (errorMessage == null || errorMessage.isEmpty()) {
                throw new IllegalStateException("Error message is required");
            }
            return new ConfigurationError(this);
        }
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ConfigurationError{");
        sb.append("type=").append(type);
        sb.append(", propertyName='").append(propertyName).append('\'');
        sb.append(", message='").append(errorMessage).append('\'');
        if (source != null) {
            sb.append(", source=").append(source.getName());
        }
        sb.append('}');
        return sb.toString();
    }
}
