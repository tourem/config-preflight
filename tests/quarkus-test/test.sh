#!/bin/bash

# Script to test Quarkus application with config-preflight

set -e

echo "=========================================="
echo "Testing Quarkus with config-preflight"
echo "=========================================="

# Determine the version to use
if [ -n "$1" ]; then
    VERSION="$1"
    echo "Using specified version: $VERSION"
else
    VERSION=$(mvn -q -Dexec.executable=echo -Dexec.args='${config-preflight.version}' --non-recursive exec:exec 2>/dev/null || echo "1.0.0-SNAPSHOT")
    echo "Using version from pom.xml: $VERSION"
fi

# Update the version in pom.xml if provided
if [ -n "$1" ]; then
    echo "Updating config-preflight version to: $VERSION"
    mvn versions:set-property -Dproperty=config-preflight.version -DnewVersion="$VERSION" -DgenerateBackupPoms=false
fi

echo ""
echo "Building project..."
mvn clean package -DskipTests -q

echo ""
echo "=========================================="
echo "Running test scenarios..."
echo "=========================================="

# Scenario 1: Missing database properties
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "SCENARIO 1: Missing database.password and database.timeout"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
QUARKUS_PROFILE=scenario1 java -jar target/quarkus-app/quarkus-run.jar 2>&1 | head -50 || true

# Scenario 2: Missing API properties
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "SCENARIO 2: Missing api.endpoint and api.cache-directory"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
QUARKUS_PROFILE=scenario2 java -jar target/quarkus-app/quarkus-run.jar 2>&1 | head -50 || true

# Scenario 3: Missing messaging properties
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "SCENARIO 3: Missing messaging.queue-name and messaging.connection-timeout"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
QUARKUS_PROFILE=scenario3 java -jar target/quarkus-app/quarkus-run.jar 2>&1 | head -50 || true

# Scenario 4: Multiple missing properties
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "SCENARIO 4: Multiple missing properties (6 total)"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
QUARKUS_PROFILE=scenario4 java -jar target/quarkus-app/quarkus-run.jar 2>&1 | head -50 || true

# Valid scenario
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "SCENARIO 5: All properties present (should succeed)"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
QUARKUS_PROFILE=valid java -jar target/quarkus-app/quarkus-run.jar 2>&1 | head -50 || true

echo ""
echo "=========================================="
echo "All scenarios completed!"
echo "=========================================="
echo ""
echo "Review the output above for config-preflight validation results."
