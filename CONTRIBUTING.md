# Contributing to Config Preflight

Thank you for your interest in contributing to Config Preflight! This document provides guidelines and instructions for contributing.

---

## 📋 Table of Contents

1. [Code of Conduct](#code-of-conduct)
2. [How to Contribute](#how-to-contribute)
3. [Development Setup](#development-setup)
4. [Coding Standards](#coding-standards)
5. [Testing](#testing)
6. [Pull Request Process](#pull-request-process)
7. [Reporting Issues](#reporting-issues)

---

## 📜 Code of Conduct

### Our Pledge

We are committed to providing a welcoming and inclusive environment for all contributors.

### Our Standards

**Positive behavior includes:**
- Using welcoming and inclusive language
- Being respectful of differing viewpoints
- Gracefully accepting constructive criticism
- Focusing on what is best for the community

**Unacceptable behavior includes:**
- Harassment, trolling, or derogatory comments
- Publishing others' private information
- Other conduct which could reasonably be considered inappropriate

---

## 🤝 How to Contribute

### Types of Contributions

We welcome various types of contributions:

- 🐛 **Bug Reports**: Report issues you encounter
- ✨ **Feature Requests**: Suggest new features
- 📝 **Documentation**: Improve or add documentation
- 💻 **Code**: Fix bugs or implement features
- 🧪 **Tests**: Add or improve test coverage

### Before You Start

1. **Check existing issues** to avoid duplicates
2. **Discuss major changes** by opening an issue first
3. **Read this guide** completely

---

## 🛠️ Development Setup

### Prerequisites

- **JDK 17** or higher
- **Maven 3.6+**
- **Git**
- IDE (IntelliJ IDEA, Eclipse, or VS Code recommended)

### Clone the Repository

```bash
git clone https://github.com/yourorg/config-preflight.git
cd config-preflight
```

### Build the Project

```bash
# Clean and compile
mvn clean compile

# Run tests
mvn test

# Install locally
mvn clean install
```

### Project Structure

```
config-preflight/
├── config-preflight-core/          # Framework-agnostic logic
├── config-preflight-spring-boot/   # Spring Boot integration
├── config-preflight-quarkus/       # Quarkus integration
└── config-preflight-micronaut/     # Micronaut integration
```

---

## 📏 Coding Standards

### Java Code Style

- **Java Version**: JDK 17
- **Indentation**: 4 spaces (no tabs)
- **Line Length**: Max 120 characters
- **Naming Conventions**:
  - Classes: `PascalCase`
  - Methods: `camelCase`
  - Constants: `UPPER_SNAKE_CASE`
  - Packages: `lowercase`

### Code Quality

- ✅ Write clean, readable code
- ✅ Add Javadoc for public APIs
- ✅ Follow SOLID principles
- ✅ Avoid code duplication
- ✅ Use meaningful variable names

### Example

```java
/**
 * Detects placeholders in property values.
 * 
 * @param value The property value to analyze
 * @return List of detected placeholder names
 */
public List<String> detectPlaceholders(String value) {
    if (value == null || value.isEmpty()) {
        return Collections.emptyList();
    }
    
    // Implementation...
}
```

---

## 🧪 Testing

### Writing Tests

- **Unit Tests**: Required for all new code
- **Test Framework**: JUnit 5
- **Assertions**: AssertJ preferred
- **Coverage**: Aim for 80%+ coverage

### Test Structure

```java
@DisplayName("PlaceholderDetector Tests")
class PlaceholderDetectorTest {
    
    private PlaceholderDetector detector;
    
    @BeforeEach
    void setUp() {
        detector = new PlaceholderDetector();
    }
    
    @Test
    @DisplayName("Should detect placeholder without default value")
    void testDetectPlaceholder() {
        // Given
        String value = "${app.url}";
        
        // When
        List<String> placeholders = detector.detectPlaceholders(value);
        
        // Then
        assertThat(placeholders)
            .hasSize(1)
            .containsExactly("app.url");
    }
}
```

### Running Tests

```bash
# Run all tests
mvn test

# Run tests for specific module
mvn test -pl config-preflight-core

# Run with coverage
mvn clean test jacoco:report
```

---

## 🔄 Pull Request Process

### 1. Fork and Branch

```bash
# Fork the repository on GitHub
# Clone your fork
git clone https://github.com/yourusername/config-preflight.git

# Create a feature branch
git checkout -b feature/your-feature-name
```

### 2. Make Changes

- Write your code
- Add tests
- Update documentation
- Ensure all tests pass

### 3. Commit

Use clear, descriptive commit messages:

```bash
# Good commit messages
git commit -m "Add support for default values in placeholders"
git commit -m "Fix secret masking for nested properties"
git commit -m "Update documentation for v3 features"

# Bad commit messages
git commit -m "fix bug"
git commit -m "update"
```

### 4. Push and Create PR

```bash
# Push to your fork
git push origin feature/your-feature-name

# Create Pull Request on GitHub
```

### 5. PR Guidelines

Your PR should:

- ✅ Have a clear title and description
- ✅ Reference related issues (e.g., "Fixes #123")
- ✅ Include tests for new features
- ✅ Update documentation if needed
- ✅ Pass all CI checks
- ✅ Have no merge conflicts

### PR Template

```markdown
## Description
Brief description of the changes

## Type of Change
- [ ] Bug fix
- [ ] New feature
- [ ] Breaking change
- [ ] Documentation update

## Testing
- [ ] Unit tests added/updated
- [ ] All tests pass
- [ ] Manual testing performed

## Checklist
- [ ] Code follows project style
- [ ] Documentation updated
- [ ] No breaking changes (or documented)
```

---

## 🐛 Reporting Issues

### Before Reporting

1. **Search existing issues** to avoid duplicates
2. **Use the latest version** to ensure the bug still exists
3. **Gather information** about your environment

### Bug Report Template

```markdown
## Bug Description
Clear description of the bug

## Steps to Reproduce
1. Step 1
2. Step 2
3. Step 3

## Expected Behavior
What should happen

## Actual Behavior
What actually happens

## Environment
- Config Preflight version: 1.0.0-SNAPSHOT
- Framework: Spring Boot 3.2.0
- JDK version: 17
- OS: macOS / Linux / Windows

## Additional Context
Any other relevant information
```

### Feature Request Template

```markdown
## Feature Description
Clear description of the proposed feature

## Use Case
Why is this feature needed?

## Proposed Solution
How should it work?

## Alternatives Considered
Other approaches you've thought about

## Additional Context
Any other relevant information
```

---

## 📚 Documentation

### Documentation Standards

- Write clear, concise documentation
- Include code examples
- Update README.md for major features
- Add inline comments for complex logic

### Documentation Files

- `README.md` - Main documentation
- `DOCUMENTATION.md` - Complete guide
- `CHANGELOG.md` - Version history
- Javadoc - API documentation

---

## 🏆 Recognition

Contributors will be:

- Listed in the project's contributors
- Mentioned in release notes for significant contributions
- Credited in the CHANGELOG

---

## 📞 Getting Help

- **Questions**: Open a GitHub Discussion
- **Issues**: Create a GitHub Issue
- **Chat**: Join our community (if applicable)

---

## 📄 License

By contributing, you agree that your contributions will be licensed under the Apache License 2.0.

---

## 🙏 Thank You!

Thank you for contributing to Config Preflight! Your efforts help make this project better for everyone.

---

**Happy Coding!** 🚀
