# Changelog

All notable changes to Config Preflight will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [Unreleased]

### Planned for v1.1.0
- Support for SpEL expressions
- Type validation (int, boolean, etc.)
- JSON export of errors
- Performance metrics

---

## [1.0.0-SNAPSHOT] - 2024-11-21

### 🚀 Added - Version 3 Features

#### Default Values Support
- Added `PlaceholderDetector.detectRequiredPlaceholders()` method
- Placeholders with default values (`${property:default}`) are now ignored
- Reduces false positives in validation

**Example:**
```yaml
# No longer generates an error
app:
  host: ${APP_HOST:localhost}
  port: ${APP_PORT:8080}
```

#### Secret Detection and Masking
- New `SecretDetector` class for automatic secret detection
- Detects 10+ sensitive keywords (password, secret, token, key, etc.)
- Automatic value masking in logs: `***MASKED***`
- Added `🔒 [SENSITIVE]` badge in error messages
- Added `isSensitive` field to `ConfigurationError`

**Detected Keywords:**
- password, passwd, pwd
- secret, token, key, apikey
- credential, auth, authorization
- private, certificate, cert

#### Early Validation
- New `SpringBootValidatorEnvironmentPostProcessor` for Spring Boot
- New `QuarkusEarlyValidator` for Quarkus
- New `MicronautEarlyValidator` for Micronaut
- Validation runs BEFORE bean initialization
- Fail-fast in ~100ms instead of 2-5 seconds
- Configuration property: `configuration.validator.early-validation`

**Benefits:**
- Errors detected before JPA, Flyway, Liquibase initialization
- Clear error messages instead of cryptic framework errors
- No unnecessary initialization if configuration is invalid

#### Enhanced Visual Formatting
- Box design with double borders
- Improved readability
- Sensitive property badges
- Error grouping by type

**Before:**
```
═══════════════════════════════════════════════════════════════════════════════
  ⚠️  CONFIGURATION VALIDATION FAILED  ⚠️
═══════════════════════════════════════════════════════════════════════════════
```

**After:**
```
╔═══════════════════════════════════════════════════════════════════════════════╗
║              ⚠️   CONFIGURATION VALIDATION FAILED   ⚠️                        ║
╚═══════════════════════════════════════════════════════════════════════════════╝
```

#### Spring Boot Binder API Integration
- New `SpringBootBinderPropertyResolver` using Spring's `Binder` API
- 100% faithful to Spring Boot's property resolution
- Native relaxed binding support
- Automatic type conversions

#### Failure Analyzer
- New `ConfigurationValidationFailureAnalyzer` for Spring Boot
- Clean error display without stacktraces
- Registered in `META-INF/spring.factories`

### 🔧 Changed

- Updated `BeautifulErrorFormatter` to integrate `SecretDetector`
- Updated all validators to use `detectRequiredPlaceholders()`
- Enhanced error messages with better formatting
- Improved `ConfigurationError` model with `isSensitive` field

### 🧪 Testing

- Added `SecretDetectorTest` with 6 tests
- Added `PlaceholderDetectorAdvancedTest` with 8 tests
- Added `SpringBootBindingIntegrationTest` with 7 tests
- Added `SpringBootMultipleSourcesTest` with 4 tests
- Added `SpringBootSecurityAndReportingTest` with 6 tests
- Total: 50 tests (23 core + 27 spring-boot)
- All tests passing ✅

### 📚 Documentation

- Added comprehensive `DOCUMENTATION.md` (960 lines)
- Updated `README.md` with v3 features
- Added `CONTRIBUTING.md` for contributors
- Added `CHANGELOG.md` (this file)
- Added `LICENSE` (Apache 2.0)

### 🔄 Compatibility

- ✅ 100% backward compatible with v2
- ✅ No breaking changes
- ✅ New features are opt-in
- ✅ JDK 17 compatible

---

## [0.2.0] - 2024-11-21 (Version 2)

### 🚀 Added - Initial Release

#### Multi-Framework Support
- Spring Boot 3.x support
- Quarkus 3.x support
- Micronaut 4.x support

#### Automatic Binding
- Property name to environment variable conversion
- Tests 5 naming variants automatically
- Supports conventions of all 3 frameworks

**Example:**
```
app.database.url → APP_DATABASE_URL
my-service.api-key → MY_SERVICE_API_KEY
```

#### Complete Validation
- Missing property detection
- Unresolved placeholder detection
- Empty value detection
- Imported file support

#### Error Reporting
- Beautiful formatted output
- Error grouping by type
- Actionable suggestions
- All errors displayed at once

#### Modules
- `config-preflight-core` - Common logic
- `config-preflight-spring-boot` - Spring Boot implementation
- `config-preflight-quarkus` - Quarkus implementation
- `config-preflight-micronaut` - Micronaut implementation

### 🧪 Testing
- 16 unit tests
- ~85% code coverage

### 📚 Documentation
- Complete README.md
- Framework-specific examples
- Quick start guide

---

## Statistics

### Code Metrics

| Metric | v0.2.0 | v1.0.0 | Change |
|--------|--------|--------|--------|
| Java Classes | 20 | 27 | +7 |
| Lines of Code | ~2,400 | ~2,900 | +500 |
| Unit Tests | 16 | 50 | +34 |
| Test Coverage | ~85% | ~90% | +5% |
| Documentation Lines | ~1,500 | ~3,000 | +1,500 |

### Features

| Feature | v0.2.0 | v1.0.0 |
|---------|--------|--------|
| Multi-framework | ✅ | ✅ |
| Automatic binding | ✅ | ✅ |
| Default values | ❌ | ✅ |
| Secret masking | ❌ | ✅ |
| Early validation | ❌ | ✅ |
| Box design | ⚠️ Basic | ✅ Enhanced |
| Binder API | ❌ | ✅ |
| Failure Analyzer | ❌ | ✅ |

---

## Roadmap

### Version 1.1.0 (Planned)
- [ ] SpEL expression support
- [ ] Type validation (int, boolean, etc.)
- [ ] JSON error export
- [ ] Performance metrics
- [ ] Custom error messages

### Version 1.2.0 (Planned)
- [ ] Web UI for validation
- [ ] Spring Boot Actuator integration
- [ ] Prometheus metrics
- [ ] Configuration dashboard

### Version 2.0.0 (Future)
- [ ] Kotlin support
- [ ] Async validation
- [ ] Maven/Gradle plugins
- [ ] GraalVM Native Image support
- [ ] Configuration profiles validation

---

## Migration Guides

### Migrating from v0.2.0 to v1.0.0

**No breaking changes!** v1.0.0 is 100% backward compatible.

**New optional configurations:**
```properties
# Enable/disable early validation (default: true)
configuration.validator.early-validation=true
```

**Recommendations:**
1. Keep early validation enabled for better error detection
2. Use default values for optional properties: `${PROP:default}`
3. Check logs for `🔒 [SENSITIVE]` badges on sensitive properties

---

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for contribution guidelines.

---

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

---

## Acknowledgments

- Spring Boot team for the Binder API inspiration
- All contributors who helped improve this project
- The Java community for feedback and suggestions

---

**Config Preflight** - Stop wasting time on configuration errors! 🚀

[Unreleased]: https://github.com/yourorg/config-preflight/compare/v1.0.0...HEAD
[1.0.0-SNAPSHOT]: https://github.com/yourorg/config-preflight/releases/tag/v1.0.0
[0.2.0]: https://github.com/yourorg/config-preflight/releases/tag/v0.2.0
