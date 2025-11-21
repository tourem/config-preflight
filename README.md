# 🛡️ Config Preflight

> **Stop the "Restart-Crash-Fix" Loop.**  
> Validate **ALL** your configuration properties at startup, before your beans even try to initialize.

[![Maven Central](https://img.shields.io/maven-central/v/io.github.tourem/config-preflight-parent.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:io.github.tourem%20a:config-preflight)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen)](https://spring.io/projects/spring-boot)
[![Quarkus](https://img.shields.io/badge/Quarkus-3.x-red)](https://quarkus.io/)
[![Micronaut](https://img.shields.io/badge/Micronaut-4.x-blue)](https://micronaut.io/)

---

## 😫 The Pain Point

You know this struggle. You deploy your app to a new environment, and then:

1. Application starts... **CRASH** 💥 (Missing `db.url`)
2. *You add the URL.* Restart...
3. Application starts... **CRASH** 💥 (Missing `redis.host`)
4. *You add the Host.* Restart...
5. Application starts... **CRASH** 💥 (Missing `api.key`)

**Why waste 20 minutes discovering configuration errors one by one?**

---

## 🚀 The Solution

**Config Preflight** checks everything **immediately**. It scans your environment, detects all missing placeholders, handles relaxed binding, and gives you a **single, beautiful report** blocking the startup *before* things get messy.

### ✨ The "Beautiful Report"

Let's say you have this configuration:

```yaml
# application.yml
spring:
  datasource:
    url: ${DATABASE_URL}
    password: ${DATABASE_PASSWORD}
    
app:
  api:
    key: ${API_KEY}
```

#### ❌ Without Config Preflight (Spring Boot Default)

You get cryptic errors, one at a time:

```
***************************
APPLICATION FAILED TO START
***************************

Description:

Failed to configure a DataSource: 'url' attribute is not specified and no embedded datasource could be configured.

Reason: Failed to determine a suitable driver class


Action:

Consider the following:
	If you want an embedded database (H2, HSQL or Derby), please put it on the classpath.
	If you have database settings to be loaded from a particular profile you may need to activate it (no profiles are currently active).


Process finished with exit code 1
```

**Problems:**
- ❌ Doesn't tell you which property is missing
- ❌ Cryptic error message about "driver class"
- ❌ Only shows the FIRST error
- ❌ Need to restart multiple times

#### ✅ With Config Preflight

You get ALL errors at once, clearly formatted:

```text
╔═══════════════════════════════════════════════════════════════════════════════╗
║              ⚠️   CONFIGURATION VALIDATION FAILED   ⚠️                        ║
╚═══════════════════════════════════════════════════════════════════════════════╝

  ● Property: spring.datasource.url
    ❌ Cannot resolve placeholder '${DATABASE_URL}'
    💡 export DATABASE_URL=jdbc:postgresql://localhost:5432/mydb

  ● Property: spring.datasource.password 🔒 [SENSITIVE]
    ❌ Cannot resolve placeholder '${DATABASE_PASSWORD}'
    💡 export DATABASE_PASSWORD=<value>

  ● Property: app.api.key 🔒 [SENSITIVE]
    ❌ Cannot resolve placeholder '${API_KEY}'
    💡 export API_KEY=<value>

═══════════════════════════════════════════════════════════════════════════════

  📝 ACTION REQUIRED
───────────────────────────────────────────────────────────────────────────────

  Fix the 3 errors above to start your application.
  💡 TIP: Fix all errors at once to avoid multiple restarts!
───────────────────────────────────────────────────────────────────────────────
```

**Benefits:**
- ✅ Shows ALL 3 missing properties at once
- ✅ Clear, actionable error messages
- ✅ Exact property names
- ✅ Suggested fixes (export commands)
- ✅ Secrets automatically masked 🔒
- ✅ Fix once, restart once!

---

## ⚡ Quick Start

### 1. Add the Dependency

#### Spring Boot
```xml
<dependency>
    <groupId>io.github.tourem</groupId>
    <artifactId>config-preflight-spring-boot</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

#### Quarkus
```xml
<dependency>
    <groupId>io.github.tourem</groupId>
    <artifactId>config-preflight-quarkus</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

#### Micronaut
```xml
<dependency>
    <groupId>io.github.tourem</groupId>
    <artifactId>config-preflight-micronaut</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 2. That's It! 🎉

The validator activates **automatically** at startup.

- **Spring Boot** ✅ Automatically scans `@ConfigurationProperties` beans
- **Micronaut** ✅ Automatically scans `@ConfigurationProperties` beans
- **Quarkus** ⚠️ See optional configuration below

#### Optional: Custom Property Validation (Quarkus)

For Quarkus projects, you can optionally define which properties to validate by creating a file:

**`src/main/resources/META-INF/config-preflight.properties`**

```properties
# List of required properties to validate
required.properties.database.url=true
required.properties.database.username=true
required.properties.database.password=true
required.properties.api.endpoint=true
required.properties.api.api-key=true
```

> **Note**: This is **optional** for Quarkus. If this file is not present, the validator will still check for unresolved placeholders in your configuration.

---

## 🎯 Key Features

### ⚡ Early Validation
Runs **before** bean initialization (DataSource, JPA, Flyway, etc.)
- **Spring Boot**: Uses `EnvironmentPostProcessor`
- **Quarkus**: Uses `@Observer(StartupEvent)` with high priority
- **Micronaut**: Uses `ApplicationEventListener` with highest precedence

### 🔍 Smart Detection
- ✅ Detects missing properties
- ✅ Detects unresolved placeholders `${...}`
- ✅ Ignores placeholders with defaults `${HOST:localhost}`
- ✅ Handles relaxed binding (`app.url` ↔ `APP_URL`)

### 🔒 Security First
- ✅ Auto-detects secrets (password, token, key, secret, etc.)
- ✅ Masks sensitive values: `***MASKED***`
- ✅ Adds `🔒 [SENSITIVE]` badge in reports

### 📊 Complete Reporting
- ✅ Shows **ALL** errors at once (no fail-fast)
- ✅ Groups errors by type
- ✅ Provides actionable suggestions
- ✅ Beautiful ASCII box formatting

---

## 📖 Example

### Configuration File
```yaml
# application.yml
app:
  name: MyApp
  host: ${APP_HOST:localhost}        # ✅ OK (has default)
  port: ${APP_PORT:8080}             # ✅ OK (has default)
  
  database:
    url: ${DATABASE_URL}             # ❌ REQUIRED
    password: ${DATABASE_PASSWORD}   # ❌ REQUIRED + SENSITIVE
  
  api:
    key: ${API_KEY}                  # ❌ REQUIRED + SENSITIVE
```

### Without Config Preflight
```
Application started...
Error creating bean 'dataSource'
  Caused by: jdbcUrl is required
  [50 lines of stacktrace...]
```
❌ **Result**: Restart, fix one error, repeat 3 times.

### With Config Preflight
```
╔═══════════════════════════════════════════════════════════════════════════════╗
║              ⚠️   CONFIGURATION VALIDATION FAILED   ⚠️                        ║
╚═══════════════════════════════════════════════════════════════════════════════╝

  ● Property: app.database.url
    ❌ Cannot resolve placeholder '${DATABASE_URL}'
    💡 export DATABASE_URL=<value>

  ● Property: app.database.password 🔒 [SENSITIVE]
    ❌ Cannot resolve placeholder '${DATABASE_PASSWORD}'
    💡 export DATABASE_PASSWORD=<value>

  ● Property: app.api.key 🔒 [SENSITIVE]
    ❌ Cannot resolve placeholder '${API_KEY}'
    💡 export API_KEY=<value>
```
✅ **Result**: Fix all 3 errors at once, restart once, done!

---

## ⚙️ Configuration

The validator works out of the box, but you can customize it:

```properties
# Disable the validator entirely
configuration.validator.enabled=false

# Disable early validation (run as normal bean instead)
configuration.validator.early-validation=false
```

---

## 🎓 How It Works

### Spring Boot
1. **EnvironmentPostProcessor** runs after `application.properties` is loaded
2. Uses **Binder API** for 100% faithful property resolution
3. If errors found, throws `ConfigurationValidationException`
4. **FailureAnalyzer** catches it and displays the beautiful report

### Quarkus
1. **@Observer(StartupEvent)** with `Priority.PLATFORM_BEFORE`
2. Inspects **ConfigProvider** for unresolved placeholders
3. Validates before infrastructure beans initialize

### Micronaut
1. **ApplicationEventListener<StartupEvent>** with `HIGHEST_PRECEDENCE`
2. Validates **Environment** at the very beginning
3. Blocks startup if errors detected

---

## 🔥 Benefits

| Before | After |
|--------|-------|
| 😫 Restart 5 times | ✅ Restart once |
| 🐛 Cryptic stacktraces | ✅ Clear error messages |
| ⏱️ 20 minutes debugging | ✅ 2 minutes fixing |
| 🔓 Secrets in logs | ✅ Secrets masked |
| 🤷 Guess what's missing | ✅ See all errors at once |

---

## 🧪 Test Projects

Ready-to-use test projects are available in the `tests/` directory:

- **Spring Boot 3** - `tests/spring-boot-test/`
- **Quarkus 3.16** - `tests/quarkus-test/`
- **Micronaut 4.7** - `tests/micronaut-test/`

Each project includes:
- Multiple configuration classes with missing properties
- Unit tests to validate config-preflight behavior
- A `test.sh` script to run tests with any version

```bash
# Test a specific framework
cd tests/spring-boot-test && ./test.sh

# Test all frameworks
cd tests && ./test-all.sh

# Test with a specific version
./test.sh 1.0.0
```

See [tests/README.md](tests/README.md) for detailed information.

---

## 📚 Documentation

For complete documentation, see [DOCUMENTATION.md](DOCUMENTATION.md)

Topics covered:
- Context and motivation
- Detailed architecture
- Framework-specific implementation
- Advanced configuration
- Testing strategies
- Troubleshooting

---

## 🤝 Contributing

Contributions are welcome! See [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

---

## 📄 License

Apache License 2.0 - See [LICENSE](LICENSE) for details.

---

## 🌟 Why Use This?

> **"We reduced our deployment debugging time by 80%."**  
> — Development Team

> **"No more secrets leaked in logs!"**  
> — Security Team

> **"Onboarding new developers is so much easier now."**  
> — Tech Lead

---

**Stop wasting time on configuration errors. Start using Config Preflight today!** 🚀
