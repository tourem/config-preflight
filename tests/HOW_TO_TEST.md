# Guide d'utilisation des projets de test Config Preflight

## 🎯 Objectif

Ces projets permettent de **démontrer et tester** le fonctionnement de config-preflight sur de vraies applications Spring Boot, Quarkus et Micronaut.

## 📋 Prérequis

1. Java 17 installé
2. Maven installé
3. Config-preflight installé localement :
   ```bash
   cd /path/to/config-preflight
   mvn clean install -DskipTests
   ```

## 🚀 Lancement des tests

### Option 1 : Tester un framework spécifique

```bash
# Spring Boot
cd tests/spring-boot-test
./test.sh

# Quarkus
cd tests/quarkus-test
./test.sh

# Micronaut
cd tests/micronaut-test
./test.sh
```

### Option 2 : Tester tous les frameworks

```bash
cd tests
./test-all.sh
```

### Option 3 : Tester avec une version spécifique

```bash
# Avec une version release
./test.sh 1.0.0

# Avec une version snapshot
./test.sh 1.0.1-SNAPSHOT
```

## 📊 Que fait le script de test ?

Le script `test.sh` de chaque projet :

1. **Build** le projet avec Maven
2. **Lance 5 scénarios** différents :
   - Scénario 1 : 2 propriétés database manquantes
   - Scénario 2 : 2 propriétés API manquantes
   - Scénario 3 : 2 propriétés messaging manquantes
   - Scénario 4 : 6 propriétés manquantes au total
   - Scénario 5 : Configuration valide (toutes les propriétés présentes)
3. **Affiche** les 50 premières lignes de sortie de chaque scénario
4. **Continue** même si un scénario échoue (pour voir tous les résultats)

## 🔍 Résultats attendus

### Avec config-preflight activé et fonctionnel

Pour les scénarios 1-4, vous devriez voir :

```
╔═══════════════════════════════════════════════════════════════╗
║           Configuration Validation Failed                     ║
╠═══════════════════════════════════════════════════════════════╣
║ The following configuration properties are missing:          ║
║                                                               ║
║  • database.password                                          ║
║  • database.timeout                                           ║
╚═══════════════════════════════════════════════════════════════╝
```

Pour le scénario 5, l'application devrait démarrer normalement.

### Sans config-preflight ou si non implémenté

- Les applications démarreront sans erreur
- Les propriétés manquantes auront des valeurs `null`
- Aucun rapport de validation ne sera affiché

## 🧪 Tester manuellement un scénario spécifique

### Spring Boot

```bash
cd tests/spring-boot-test
mvn clean package -DskipTests
java -jar target/*.jar --spring.profiles.active=scenario1
```

### Quarkus

```bash
cd tests/quarkus-test
mvn clean package -DskipTests
java -jar target/quarkus-app/quarkus-run.jar -Dquarkus.profile=scenario1
```

### Micronaut

```bash
cd tests/micronaut-test
mvn clean package -DskipTests
java -jar target/*.jar -Dmicronaut.environments=scenario1
```

## 📝 Modifier les scénarios

Les fichiers de configuration se trouvent dans `src/main/resources/` :

- **Spring Boot** : `application-scenario1.yml` à `application-valid.yml`
- **Quarkus** : `application-scenario1.properties` à `application-valid.properties`
- **Micronaut** : `application-scenario1.yml` à `application-valid.yml`

Pour ajouter un nouveau scénario :

1. Créer un nouveau fichier de configuration (ex: `application-scenario6.yml`)
2. Définir les propriétés manquantes
3. Ajouter le scénario dans le script `test.sh`

## 🐛 Dépannage

### L'application ne démarre pas

```bash
# Vérifier que config-preflight est bien installé
ls ~/.m2/repository/io/github/tourem/config-preflight-*/1.0.0-SNAPSHOT/

# Réinstaller si nécessaire
cd /path/to/config-preflight
mvn clean install -DskipTests
```

### Aucun message de validation n'apparaît

Cela signifie que :
- Config-preflight n'est pas encore implémenté
- Ou la dépendance n'est pas correctement chargée
- Ou les logs ne sont pas configurés

Vérifiez les dépendances dans le `pom.xml` :

```xml
<dependency>
    <groupId>io.github.tourem</groupId>
    <artifactId>config-preflight-spring-boot</artifactId>
    <version>${config-preflight.version}</version>
</dependency>
```

### Le JAR n'est pas créé

```bash
# Pour Spring Boot et Micronaut
mvn clean package -DskipTests

# Pour Quarkus
mvn clean package -DskipTests
# Le JAR est dans target/quarkus-app/quarkus-run.jar
```

## 📚 Fichiers de configuration

Chaque scénario a son propre fichier de configuration :

| Scénario | Propriétés manquantes | Fichier |
|----------|----------------------|---------|
| 1 | database.password, database.timeout | application-scenario1.* |
| 2 | api.endpoint, api.cache-directory | application-scenario2.* |
| 3 | messaging.queue-name, messaging.connection-timeout | application-scenario3.* |
| 4 | 6 propriétés (2 par config) | application-scenario4.* |
| 5 | Aucune (config valide) | application-valid.* |

## 💡 Conseils

1. **Lancez d'abord le scénario valide** pour vérifier que l'application fonctionne
2. **Comparez les sorties** entre les scénarios pour voir les différences
3. **Utilisez `grep`** pour filtrer les logs :
   ```bash
   ./test.sh | grep -i "validation\|missing\|error"
   ```
4. **Redirigez vers un fichier** pour analyser plus tard :
   ```bash
   ./test.sh > results.txt 2>&1
   ```

## 🎓 Comprendre les résultats

### Comportement attendu de config-preflight

1. **Au démarrage** : Scan de toutes les classes `@ConfigurationProperties`
2. **Détection** : Identification des propriétés non valorisées
3. **Rapport** : Affichage d'un rapport clair et lisible
4. **Blocage** : Empêche le démarrage si des propriétés critiques manquent

### Exemple de sortie attendue

```
20:45:23.456 [main] INFO  io.github.tourem.preflight - Starting configuration validation...
20:45:23.567 [main] WARN  io.github.tourem.preflight - Found 2 missing properties:
20:45:23.567 [main] WARN  io.github.tourem.preflight -   • database.password
20:45:23.567 [main] WARN  io.github.tourem.preflight -   • database.timeout
20:45:23.678 [main] ERROR io.github.tourem.preflight - Configuration validation failed!
```

## 🔗 Ressources

- [README principal](../README.md)
- [Documentation complète](../DOCUMENTATION.md)
- [Guide de test](./TESTING_GUIDE.md)
- [Résumé du projet](../PROJECT_SUMMARY.md)
