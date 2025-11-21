package com.mycompany.validator.core.model;

/**
 * Représente la source d'une propriété de configuration.
 */
public class PropertySource {
    
    private final String name;
    private final String location;
    private final SourceType type;
    
    public PropertySource(String name, String location, SourceType type) {
        this.name = name;
        this.location = location;
        this.type = type;
    }
    
    public String getName() {
        return name;
    }
    
    public String getLocation() {
        return location;
    }
    
    public SourceType getType() {
        return type;
    }
    
    /**
     * Types de sources de propriétés.
     */
    public enum SourceType {
        APPLICATION_PROPERTIES("application.properties"),
        APPLICATION_YAML("application.yml"),
        ENVIRONMENT_VARIABLE("Environment Variable"),
        SYSTEM_PROPERTY("System Property"),
        COMMAND_LINE("Command Line"),
        IMPORTED_FILE("Imported File"),
        UNKNOWN("Unknown");
        
        private final String displayName;
        
        SourceType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        @Override
        public String toString() {
            return displayName;
        }
    }
    
    @Override
    public String toString() {
        return String.format("PropertySource{name='%s', location='%s', type=%s}", 
                name, location, type);
    }
}
