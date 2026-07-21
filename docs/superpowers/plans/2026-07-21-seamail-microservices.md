# Seamail Microservices Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Split the Seamail monolith into api-gateway + auth-service + mail-service + notification-service with Kafka, Flyway, Testcontainers, OpenAPI, and Prometheus/Zipkin/Grafana, per spec `docs/superpowers/specs/2026-07-21-seamail-microservices-reference-design.md`.

**Architecture:** Maven multi-module reactor under a root `com.seamail:seamail-parent`. auth-service issues RS256 JWTs (Nimbus) and exposes a JWKS endpoint; mail-service and notification-service are OAuth2 resource servers validating via JWKS. mail-service checks receivers via Feign against auth-service and publishes `EmailSentEvent` to Kafka AFTER_COMMIT; notification-service consumes idempotently and serves a REST feed. Spring Cloud Gateway is the single entry point on :8081.

**Tech Stack:** Java 21, Spring Boot 3.2.3, Spring Cloud 2023.0.3 (Gateway, OpenFeign), spring-kafka, spring-security-oauth2-jose/resource-server, Flyway, springdoc-openapi 2.3.0, Testcontainers with `@ServiceConnection`, Micrometer + Brave + Zipkin, Prometheus, Grafana.

## Global Constraints

- Java 21, Spring Boot exactly 3.2.3, Spring Cloud exactly 2023.0.3, springdoc exactly 2.3.0.
- Shell is Windows PowerShell 5.1: no bash syntax in commands. For file content replacement use `[IO.File]::ReadAllText` / `[IO.File]::WriteAllText($p, $c, [Text.UTF8Encoding]::new($false))` (UTF-8 without BOM).
- Shell scripts mounted into Linux containers (`*.sh`) must have LF line endings. `.gitattributes` enforces this.
- Never edit `.env` files. Templates (`.env.docker.example`) may be edited.
- Git commits ARE authorized for this execution: one focused commit per task, short imperative message, no period.
- DTOs only over the wire; entities are never serialized.
- Error contract: `ErrorResponse` / `ValidationErrorResponse` via `GlobalExceptionHandler` in every service; the 401 entry point body is exactly `{"message": "Unauthorized"}`.
- Every task ends with `mvn test` green in all touched modules (root reactor after Task 1).
- Testcontainers tasks (12) require Docker Desktop running.

---

### Task 1: Maven restructure - parent pom, rename module to mail-service

**Files:**
- Create: `pom.xml` (root)
- Rename: `backendemailservice/` -> `mail-service/` (all content)
- Modify: `mail-service/pom.xml`
- Modify: every `.java` under `mail-service/src` (package rename)
- Rename: `BackendemailserviceApplication.java` -> `MailServiceApplication.java`; `BackendemailserviceApplicationTests.java` -> `MailServiceApplicationTests.java`
- Modify: `mail-service/Dockerfile`

**Interfaces:**
- Produces: root reactor; package `com.seamail.mail`; class `com.seamail.mail.MailServiceApplication`. All later tasks build on these names.

- [ ] **Step 1: Create root aggregator pom**

Create `C:\Users\KimoStore\Desktop\Email-Service\pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.3</version>
        <relativePath/>
    </parent>
    <groupId>com.seamail</groupId>
    <artifactId>seamail-parent</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <packaging>pom</packaging>
    <name>seamail-parent</name>

    <modules>
        <module>mail-service</module>
    </modules>

    <properties>
        <java.version>21</java.version>
        <spring-cloud.version>2023.0.3</spring-cloud.version>
        <springdoc.version>2.3.0</springdoc.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-dependencies</artifactId>
                <version>${spring-cloud.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
            <dependency>
                <groupId>org.springdoc</groupId>
                <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
                <version>${springdoc.version}</version>
            </dependency>
            <dependency>
                <groupId>org.springdoc</groupId>
                <artifactId>springdoc-openapi-starter-webflux-ui</artifactId>
                <version>${springdoc.version}</version>
            </dependency>
        </dependencies>
    </dependencyManagement>
</project>
```

- [ ] **Step 2: Rename module directory and package directories**

Run from repo root:

```powershell
git mv backendemailservice mail-service
git mv mail-service\src\main\java\com\backendemailservice\backendemailservice mail-service\src\main\java\com\backendemailservice\mail
git mv mail-service\src\main\java\com\backendemailservice mail-service\src\main\java\com\seamail
git mv mail-service\src\test\java\com\backendemailservice\backendemailservice mail-service\src\test\java\com\backendemailservice\mail
git mv mail-service\src\test\java\com\backendemailservice mail-service\src\test\java\com\seamail
git mv mail-service\src\main\java\com\seamail\mail\BackendemailserviceApplication.java mail-service\src\main\java\com\seamail\mail\MailServiceApplication.java
git mv mail-service\src\test\java\com\seamail\mail\BackendemailserviceApplicationTests.java mail-service\src\test\java\com\seamail\mail\MailServiceApplicationTests.java
```

- [ ] **Step 3: Rewrite package declarations and class names in all Java files**

Run from repo root:

```powershell
Get-ChildItem -Recurse -Filter *.java mail-service\src | ForEach-Object {
    $p = $_.FullName
    $c = [IO.File]::ReadAllText($p)
    $c = $c -replace 'com\.backendemailservice\.backendemailservice', 'com.seamail.mail'
    $c = $c -replace 'BackendemailserviceApplicationTests', 'MailServiceApplicationTests'
    $c = $c -replace 'BackendemailserviceApplication', 'MailServiceApplication'
    [IO.File]::WriteAllText($p, $c, [Text.UTF8Encoding]::new($false))
}
```

- [ ] **Step 4: Update mail-service/pom.xml**

Replace the `<parent>` block and `<groupId>/<artifactId>/<name>` lines with:

```xml
	<parent>
		<groupId>com.seamail</groupId>
		<artifactId>seamail-parent</artifactId>
		<version>0.0.1-SNAPSHOT</version>
	</parent>
	<artifactId>mail-service</artifactId>
	<version>0.0.1-SNAPSHOT</version>
	<name>mail-service</name>
	<description>Seamail mail service</description>
```

(Delete the old `<groupId>com.backendemailservice</groupId>` line; keep `<properties><java.version>21</java.version></properties>` removed since the parent now defines it - deleting the module-level `<properties>` block entirely is correct.)

- [ ] **Step 5: Update mail-service/Dockerfile jar name**

Replace `backendemailservice.jar` with `app.jar` in both the COPY and ENTRYPOINT lines:

```dockerfile
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8083

ENTRYPOINT ["java", "-jar", "app.jar"]
```

(This Dockerfile is superseded by the root `Dockerfile.backend` in Task 6; keep it consistent anyway.)

- [ ] **Step 6: Verify build and tests**

Run from repo root: `mvn test`
Expected: BUILD SUCCESS; all existing mail tests pass under `com.seamail.mail`.

- [ ] **Step 7: Commit**

```powershell
git add -A
git commit -m "Restructure backend into mail-service module under seamail-parent reactor"
```

---

### Task 2: Flyway in mail-service

**Files:**
- Modify: `mail-service/pom.xml`
- Create: `mail-service/src/main/resources/db/migration/V1__create_emails_table.sql`
- Modify: `mail-service/src/main/resources/application.properties`
- Modify: `mail-service/src/test/resources/application-test.properties`
- Modify: `mail-service/src/main/resources/application-local.properties` (DB name note)

**Interfaces:**
- Produces: Flyway-managed `emails` + `email_id_seq` schema; `ddl-auto=validate` still enforced.

- [ ] **Step 1: Add Flyway dependencies to mail-service/pom.xml**

Insert before `</dependencies>`:

```xml
		<dependency>
			<groupId>org.flywaydb</groupId>
			<artifactId>flyway-core</artifactId>
		</dependency>
		<dependency>
			<groupId>org.flywaydb</groupId>
			<artifactId>flyway-mysql</artifactId>
		</dependency>
```

- [ ] **Step 2: Create V1 migration**

Create `mail-service/src/main/resources/db/migration/V1__create_emails_table.sql`:

```sql
-- emails schema for mail-service (replaces the mail slice of SQL Scripts/Tables.sql)
CREATE TABLE IF NOT EXISTS email_id_seq (
    next_val BIGINT NOT NULL
);

INSERT INTO email_id_seq (next_val) VALUES (1);

CREATE TABLE IF NOT EXISTS emails (
    email_id BIGINT NOT NULL PRIMARY KEY,
    sender VARCHAR(255) NOT NULL,
    receiver VARCHAR(255) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    body TEXT NOT NULL,
    priority VARCHAR(255) NOT NULL,
    date DATETIME NOT NULL,
    trash TINYINT(1) NOT NULL DEFAULT 0
);
```

- [ ] **Step 3: Enable Flyway in application.properties**

Append to `mail-service/src/main/resources/application.properties`:

```properties
# Flyway migrations
spring.flyway.enabled=true
spring.flyway.baseline-on-migrate=true
```

- [ ] **Step 4: Disable Flyway for the H2 test profile**

Append to `mail-service/src/test/resources/application-test.properties`:

```properties
# Flyway disabled for H2 slices (MySQL syntax in migrations); ddl-auto=create-drop covers tests
spring.flyway.enabled=false
```

- [ ] **Step 5: Verify**

Run: `mvn -pl mail-service test`
Expected: BUILD SUCCESS (Flyway disabled in tests; nothing else changes).

- [ ] **Step 6: Commit**

```powershell
git add -A
git commit -m "Add Flyway migrations to mail-service"
```

---

### Task 3: auth-service module (RS256 issuance, JWKS, users domain)

**Files:**
- Create: `auth-service/pom.xml`
- Create: `auth-service/Dockerfile`
- Create: `auth-service/src/main/java/com/seamail/auth/AuthServiceApplication.java`
- Copy from mail-service (main, package rewritten to `com.seamail.auth`):
  `entity/User.java`, `repository/UserRepository.java`,
  `service/IUserService.java`, `service/UserService.java`,
  `controller/AccessController.java`, `controller/OAuth2Controller.java`, `controller/UsersController.java`,
  `config/DiscordOAuthProperties.java`, `health/CustomRedisHealthIndicator.java`,
  `dto/UserRequestDto.java`, `dto/AuthResponseDto.java`, `dto/RefreshTokenRequestDto.java`,
  `dto/DiscordTicketRequestDto.java`, `dto/DiscordExchangeResponseDto.java`,
  `dto/ChangePasswordRequestDto.java`, `dto/UpdateLanguageRequestDto.java`, `dto/DeleteAccountRequestDto.java`,
  `exception/ApplicationException.java`, `exception/ErrorResponse.java`, `exception/ValidationErrorResponse.java`,
  `exception/GlobalExceptionHandler.java`, `exception/UserNotFoundException.java`,
  `exception/UserAlreadyExistsException.java`, `exception/InvalidEmailDomainException.java`,
  `exception/InvalidFileFormatException.java`
- Copy from mail-service (test, package rewritten): `controller/AccessControllerTest.java`,
  `controller/UsersControllerTest.java`, `service/UserServiceTest.java`,
  `repository/UserRepositoryTest.java`, `config/TestSecurityConfig.java`
- Create: `auth-service/src/main/java/com/seamail/auth/config/RsaKeyProvider.java`
- Create: `auth-service/src/main/java/com/seamail/auth/service/JwtService.java`
- Create: `auth-service/src/main/java/com/seamail/auth/controller/JwksController.java`
- Create: `auth-service/src/main/java/com/seamail/auth/controller/InternalUsersController.java`
- Create: `auth-service/src/main/java/com/seamail/auth/config/SecurityConfig.java`
- Create: `auth-service/src/main/resources/application.properties`, `application-local.properties`
- Create: `auth-service/src/test/resources/application-test.properties`
- Create: `auth-service/src/main/resources/db/migration/V1__create_users_table.sql`
- Modify: root `pom.xml` (add module)

**Interfaces:**
- Produces: `JwtService.generateToken(String email): String` (RS256, 30 min, claims `roles=["ROLE_USER"]`, `iss=seamail-auth-service`), `JwtService.generateRefreshToken(): String` (UUID).
- Produces: `GET /.well-known/jwks.json` -> JWK Set JSON (public key only, `kty=RSA`, `kid` present).
- Produces: `GET /internal/users/{email}/exists` -> 200 or 404 (empty body).
- Produces: property keys `jwt.private-key-path`, `jwt.public-key-path` (empty means ephemeral keypair).

- [ ] **Step 1: Create auth-service/pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
	<modelVersion>4.0.0</modelVersion>
	<parent>
		<groupId>com.seamail</groupId>
		<artifactId>seamail-parent</artifactId>
		<version>0.0.1-SNAPSHOT</version>
	</parent>
	<artifactId>auth-service</artifactId>
	<version>0.0.1-SNAPSHOT</version>
	<name>auth-service</name>
	<description>Seamail auth service</description>

	<dependencies>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-data-jpa</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-web</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-security</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.security</groupId>
			<artifactId>spring-security-oauth2-jose</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-data-redis</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-validation</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-actuator</artifactId>
		</dependency>
		<dependency>
			<groupId>org.flywaydb</groupId>
			<artifactId>flyway-core</artifactId>
		</dependency>
		<dependency>
			<groupId>org.flywaydb</groupId>
			<artifactId>flyway-mysql</artifactId>
		</dependency>
		<dependency>
			<groupId>com.mysql</groupId>
			<artifactId>mysql-connector-j</artifactId>
			<scope>runtime</scope>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-test</artifactId>
			<scope>test</scope>
		</dependency>
		<dependency>
			<groupId>org.springframework.security</groupId>
			<artifactId>spring-security-test</artifactId>
			<scope>test</scope>
		</dependency>
		<dependency>
			<groupId>com.h2database</groupId>
			<artifactId>h2</artifactId>
			<scope>test</scope>
		</dependency>
	</dependencies>

	<build>
		<plugins>
			<plugin>
				<groupId>org.springframework.boot</groupId>
				<artifactId>spring-boot-maven-plugin</artifactId>
			</plugin>
		</plugins>
	</build>
</project>
```

- [ ] **Step 2: Add module to root pom**

In root `pom.xml` `<modules>`, add above the mail line:

```xml
        <module>auth-service</module>
```

- [ ] **Step 3: Create the application class and Dockerfile**

`auth-service/src/main/java/com/seamail/auth/AuthServiceApplication.java`:

```java
package com.seamail.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.Ordered;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableTransactionManagement(order = Ordered.LOWEST_PRECEDENCE) // transaction is the inner advice
public class AuthServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(AuthServiceApplication.class, args);
	}

}
```

`auth-service/Dockerfile` (superseded by root `Dockerfile.backend` in Task 6, kept for standalone builds):

```dockerfile
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn package -DskipTests
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
```

- [ ] **Step 4: Copy identity code from mail-service with package rewrite**

Run from repo root:

```powershell
$mainFiles = @(
  'entity\User.java',
  'repository\UserRepository.java',
  'service\IUserService.java',
  'service\UserService.java',
  'controller\AccessController.java',
  'controller\OAuth2Controller.java',
  'controller\UsersController.java',
  'config\DiscordOAuthProperties.java',
  'health\CustomRedisHealthIndicator.java',
  'dto\UserRequestDto.java',
  'dto\AuthResponseDto.java',
  'dto\RefreshTokenRequestDto.java',
  'dto\DiscordTicketRequestDto.java',
  'dto\DiscordExchangeResponseDto.java',
  'dto\ChangePasswordRequestDto.java',
  'dto\UpdateLanguageRequestDto.java',
  'dto\DeleteAccountRequestDto.java',
  'exception\ApplicationException.java',
  'exception\ErrorResponse.java',
  'exception\ValidationErrorResponse.java',
  'exception\GlobalExceptionHandler.java',
  'exception\UserNotFoundException.java',
  'exception\UserAlreadyExistsException.java',
  'exception\InvalidEmailDomainException.java',
  'exception\InvalidFileFormatException.java'
)
$testFiles = @(
  'controller\AccessControllerTest.java',
  'controller\UsersControllerTest.java',
  'service\UserServiceTest.java',
  'repository\UserRepositoryTest.java',
  'config\TestSecurityConfig.java'
)
foreach ($rel in $mainFiles) {
  $src = "mail-service\src\main\java\com\seamail\mail\$rel"
  $dst = "auth-service\src\main\java\com\seamail\auth\$rel"
  New-Item -ItemType Directory -Force (Split-Path $dst) | Out-Null
  $c = [IO.File]::ReadAllText($src) -replace 'com\.seamail\.mail', 'com.seamail.auth'
  [IO.File]::WriteAllText($dst, $c, [Text.UTF8Encoding]::new($false))
}
foreach ($rel in $testFiles) {
  $src = "mail-service\src\test\java\com\seamail\mail\$rel"
  $dst = "auth-service\src\test\java\com\seamail\auth\$rel"
  New-Item -ItemType Directory -Force (Split-Path $dst) | Out-Null
  $c = [IO.File]::ReadAllText($src) -replace 'com\.seamail\.mail', 'com.seamail.auth'
  [IO.File]::WriteAllText($dst, $c, [Text.UTF8Encoding]::new($false))
}
```

- [ ] **Step 5: Create RsaKeyProvider**

`auth-service/src/main/java/com/seamail/auth/config/RsaKeyProvider.java`:

```java
package com.seamail.auth.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.UUID;

// Provides the RSA keypair used to sign access tokens.
// Production-like setup loads PEM files via jwt.private-key-path / jwt.public-key-path.
// When unset, an ephemeral pair is generated so local dev needs no key management;
// the trade-off is that all tokens become invalid on restart.
@Component
public class RsaKeyProvider {

    private static final Logger log = LoggerFactory.getLogger(RsaKeyProvider.class);

    private final RSAPublicKey publicKey;
    private final RSAPrivateKey privateKey;
    private final String keyId;

    public RsaKeyProvider(
            @Value("${jwt.private-key-path:}") String privateKeyPath,
            @Value("${jwt.public-key-path:}") String publicKeyPath) throws Exception {
        if (privateKeyPath != null && !privateKeyPath.isBlank()
                && publicKeyPath != null && !publicKeyPath.isBlank()) {
            this.privateKey = loadPrivateKey(privateKeyPath);
            this.publicKey = loadPublicKey(publicKeyPath);
            this.keyId = UUID.randomUUID().toString();
            log.info("Loaded RSA keypair from configured PEM files (kid={})", keyId);
        } else {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair keyPair = generator.generateKeyPair();
            this.privateKey = (RSAPrivateKey) keyPair.getPrivate();
            this.publicKey = (RSAPublicKey) keyPair.getPublic();
            this.keyId = UUID.randomUUID().toString();
            log.warn("No RSA key files configured (jwt.private-key-path/jwt.public-key-path). "
                    + "Generated an ephemeral keypair - all tokens become invalid on restart.");
        }
    }

    private RSAPublicKey loadPublicKey(String path) throws Exception {
        String pem = Files.readString(Path.of(path))
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        byte[] der = Base64.getDecoder().decode(pem);
        return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(der));
    }

    private RSAPrivateKey loadPrivateKey(String path) throws Exception {
        String pem = Files.readString(Path.of(path))
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] der = Base64.getDecoder().decode(pem);
        return (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(der));
    }

    public RSAPublicKey getPublicKey() {
        return publicKey;
    }

    public RSAPrivateKey getPrivateKey() {
        return privateKey;
    }

    public String getKeyId() {
        return keyId;
    }
}
```

- [ ] **Step 6: Create JwtService**

`auth-service/src/main/java/com/seamail/auth/service/JwtService.java`:

```java
package com.seamail.auth.service;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.seamail.auth.config.RsaKeyProvider;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

// Issues RS256 access tokens (30 min) and opaque refresh tokens.
// Replaces the old HS256 JwtUtil: resource servers validate via the public JWKS,
// so no shared secret exists anywhere.
@Service
public class JwtService {

    private static final long ACCESS_TOKEN_EXPIRATION_SECONDS = 60 * 30;

    private final JwtEncoder jwtEncoder;

    public JwtService(RsaKeyProvider keyProvider) {
        RSAKey rsaKey = new RSAKey.Builder(keyProvider.getPublicKey())
                .privateKey(keyProvider.getPrivateKey())
                .keyID(keyProvider.getKeyId())
                .build();
        this.jwtEncoder = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(rsaKey)));
    }

    public String generateToken(String email) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("seamail-auth-service")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(ACCESS_TOKEN_EXPIRATION_SECONDS))
                .subject(email)
                .claim("roles", List.of("ROLE_USER"))
                .build();
        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    public String generateRefreshToken() {
        return UUID.randomUUID().toString();
    }
}
```

- [ ] **Step 7: Swap JwtUtil -> JwtService in auth's UserService and UserServiceTest**

In `auth-service/src/main/java/com/seamail/auth/service/UserService.java` apply two textual replacements:

- `com.seamail.auth.util.JwtUtil` -> `com.seamail.auth.service.JwtService` (import line)
- `JwtUtil jwtUtil` -> `JwtService jwtService` (field and constructor parameter)

(All call sites - `jwtUtil.generateToken(email)` and `jwtUtil.generateRefreshToken()` - keep working unchanged since `JwtService` exposes the same two method signatures.)

In `auth-service/src/test/java/com/seamail/auth/service/UserServiceTest.java` apply:

- `com.seamail.auth.util.JwtUtil` -> `com.seamail.auth.service.JwtService`
- `JwtUtil` -> `JwtService` (remaining occurrences: the `@Mock` field type)

- [ ] **Step 8: Create JwksController and InternalUsersController**

`auth-service/src/main/java/com/seamail/auth/controller/JwksController.java`:

```java
package com.seamail.auth.controller;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.seamail.auth.config.RsaKeyProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

// Public JWKS endpoint. Resource servers fetch the public key set from here
// to validate RS256 access tokens statelessly.
@RestController
public class JwksController {

    private final RsaKeyProvider keyProvider;

    public JwksController(RsaKeyProvider keyProvider) {
        this.keyProvider = keyProvider;
    }

    @GetMapping("/.well-known/jwks.json")
    public Map<String, Object> jwks() {
        RSAKey jwk = new RSAKey.Builder(keyProvider.getPublicKey())
                .keyID(keyProvider.getKeyId())
                .build();
        return new JWKSet(jwk).toJSONObject();
    }
}
```

`auth-service/src/main/java/com/seamail/auth/controller/InternalUsersController.java`:

```java
package com.seamail.auth.controller;

import com.seamail.auth.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

// Service-to-service endpoint, reachable only inside the container network:
// the gateway deliberately has no route for /internal/**.
@RestController
public class InternalUsersController {

    private final UserRepository userRepository;

    public InternalUsersController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/internal/users/{email}/exists")
    public ResponseEntity<Void> existsByEmail(@PathVariable String email) {
        return userRepository.findByEmail(email).isPresent()
                ? ResponseEntity.ok().build()
                : ResponseEntity.notFound().build();
    }
}
```

- [ ] **Step 9: Create auth SecurityConfig**

`auth-service/src/main/java/com/seamail/auth/config/SecurityConfig.java`:

```java
package com.seamail.auth.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(DiscordOAuthProperties.class)
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    // Local decoder: auth-service validates its own tokens without an HTTP self-call.
    @Bean
    public JwtDecoder jwtDecoder(RsaKeyProvider keyProvider) {
        return NimbusJwtDecoder.withPublicKey(keyProvider.getPublicKey()).build();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/api/v1/sign-in", "/api/v1/sign-up",
                        "/api/v1/auth/discord", "/api/v1/auth/discord/state",
                        "/api/v1/auth/refresh", "/api/v1/auth/exchange",
                        "/.well-known/jwks.json", "/internal/**",
                        "/actuator/health/**", "/v3/api-docs/**").permitAll()
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpStatus.UNAUTHORIZED.value());
                    response.setContentType("application/json");
                    response.getWriter().write("{\"message\": \"Unauthorized\"}");
                }));
        return http.build();
    }
}
```

- [ ] **Step 10: Update UsersController principal resolution**

In `auth-service/src/main/java/com/seamail/auth/controller/UsersController.java`, replace every occurrence of:

```java
@AuthenticationPrincipal String authenticatedEmail
```

with:

```java
@AuthenticationPrincipal(expression = "subject") String authenticatedEmail
```

(5 occurrences: deleteAccount, changePassword, updateLanguage, uploadProfilePicture, getProfilePicture.)

- [ ] **Step 11: Trim auth GlobalExceptionHandler**

In `auth-service/src/main/java/com/seamail/auth/exception/GlobalExceptionHandler.java`, replace the not-found handler:

```java
    @ExceptionHandler({UserNotFoundException.class, EmailNotFoundException.class,
                        ReceiverNotFoundException.class})
```

with:

```java
    @ExceptionHandler(UserNotFoundException.class)
```

and delete the now-unused imports `com.seamail.auth.exception.EmailNotFoundException` and `com.seamail.auth.exception.ReceiverNotFoundException` (they are same-package classes, so only remove them from the annotation; no import lines exist for same-package types - just fix the annotation).

- [ ] **Step 12: Update auth controller tests to resource-server style**

In `auth-service/src/test/java/com/seamail/auth/controller/AccessControllerTest.java`:

- Delete the line `@MockBean private JwtUtil jwtUtil;` and the import `com.seamail.auth.util.JwtUtil`.

In `auth-service/src/test/java/com/seamail/auth/controller/UsersControllerTest.java`:

- Delete `@MockBean private JwtUtil jwtUtil;`, the `JwtUtil` import, the `SecurityContextHolder`/`UsernamePasswordAuthenticationToken`/`SimpleGrantedAuthority` imports, and the entire `setUpSecurity()`/`tearDownSecurity()` methods (plus their `@BeforeEach`/`@AfterEach` annotations).
- Add import: `import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;`
- On EVERY `mockMvc.perform(...)` call, add `.with(jwt().jwt(j -> j.subject(TEST_EMAIL)))` as the first request post-processor, EXCEPT test methods whose name contains `DifferentUser` (or which set the principal to `OTHER_EMAIL`) - those use `.with(jwt().jwt(j -> j.subject(OTHER_EMAIL)))`.

Example transformation:

```java
// before
mockMvc.perform(delete("/api/v1/delete-account")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"email\":\"" + TEST_EMAIL + "\"}"))
        .andExpect(status().isUnauthorized());

// after
mockMvc.perform(delete("/api/v1/delete-account")
        .with(jwt().jwt(j -> j.subject(OTHER_EMAIL)))
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"email\":\"" + TEST_EMAIL + "\"}"))
        .andExpect(status().isUnauthorized());
```

- [ ] **Step 13: Create auth resources**

`auth-service/src/main/resources/application.properties`:

```properties
# Server
server.port=8082

# Database Configuration
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.datasource.url=jdbc:mysql://db:3306/${DB_NAME}?sessionVariables=sql_mode='NO_ENGINE_SUBSTITUTION'&jdbcCompliantTruncation=false
spring.datasource.username=${DB_USER}
spring.datasource.password=${DB_PASSWORD}

# JPA Configuration
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.open-in-view=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect

# Flyway migrations
spring.flyway.enabled=true
spring.flyway.baseline-on-migrate=true

# Frontend origin (used to build the Discord OAuth redirect target)
cors.allowed.origin=${CORS_ALLOWED_ORIGIN}

# JWT signing keys (empty -> ephemeral keypair generated at startup)
jwt.private-key-path=${JWT_PRIVATE_KEY_PATH:}
jwt.public-key-path=${JWT_PUBLIC_KEY_PATH:}

# Discord OAuth2 Configuration
discord.client-id=${DISCORD_CLIENT_ID}
discord.client-secret=${DISCORD_CLIENT_SECRET}
discord.token-url=https://discord.com/api/oauth2/token
discord.api-url=https://discord.com/api
discord.redirect-uri=${DISCORD_REDIRECT_URI:http://localhost:8081/api/v1/auth/discord}

# Redis Configuration
spring.data.redis.host=${REDIS_HOST}
spring.data.redis.port=${REDIS_PORT}
spring.data.redis.username=${REDIS_USERNAME:}
spring.data.redis.password=${REDIS_PASSWORD:}

# Actuator
management.endpoints.web.exposure.include=health
management.endpoint.health.show-details=when-authorized
management.endpoint.health.group.liveness.include=livenessState
management.endpoint.health.group.readiness.include=readinessState,db,redis
management.endpoint.health.probes.enabled=true
```

`auth-service/src/main/resources/application-local.properties`:

```properties
# Overrides for local dev (localhost MySQL/Redis, other services on host ports)
spring.datasource.url=jdbc:mysql://localhost:3307/${DB_NAME}?sessionVariables=sql_mode='NO_ENGINE_SUBSTITUTION'&jdbcCompliantTruncation=false
cors.allowed.origin=http://localhost:8080
spring.data.redis.host=localhost
spring.data.redis.port=6379
logging.level.org.hibernate.SQL=DEBUG
logging.level.org.hibernate.orm.jdbc.bind=TRACE
```

`auth-service/src/test/resources/application-test.properties`:

```properties
# Connecting to H2 Database
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.url=jdbc:h2:mem:authdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
spring.datasource.username=sa
spring.datasource.password=

# CORS Configuration
cors.allowed.origin=http://localhost:8080
server.port=8082

# Configuring JPA for H2
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect

# Flyway disabled for H2 slices (MySQL syntax in migrations)
spring.flyway.enabled=false

# Discord OAuth2 (dummy values for testing)
discord.client-id=test_discord_client_id
discord.client-secret=test_discord_client_secret
discord.token-url=https://discord.com/api/oauth2/token
discord.api-url=https://discord.com/api
discord.redirect-uri=http://localhost:8081/api/v1/auth/discord

# Redis (dummy values as Redis is not used by slices)
spring.data.redis.host=localhost
spring.data.redis.port=6379
spring.data.redis.username=dummy
spring.data.redis.password=dummy

# In-memory cache for tests
spring.cache.type=simple
```

`auth-service/src/main/resources/db/migration/V1__create_users_table.sql`:

```sql
-- users schema for auth-service (replaces the users slice of SQL Scripts/Tables.sql)
CREATE TABLE IF NOT EXISTS users (
    email VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    language VARCHAR(255),
    profile_picture LONGBLOB,
    PRIMARY KEY (email)
);
```

- [ ] **Step 14: Verify**

Run from repo root: `mvn test`
Expected: BUILD SUCCESS for seamail-parent, auth-service, mail-service. (mail-service is untouched and still passes; auth-service tests pass with the JwtService rename and jwt() post-processor rewrite.)

- [ ] **Step 15: Commit**

```powershell
git add -A
git commit -m "Add auth-service with RS256 token issuance and JWKS endpoint"
```

---

### Task 4: Strip mail-service to a resource server with Feign receiver check

**Files:**
- Delete from mail-service (main): `entity/User.java`, `repository/UserRepository.java`,
  `service/IUserService.java`, `service/UserService.java`, `service/CustomUserDetailsService.java`,
  `controller/AccessController.java`, `controller/OAuth2Controller.java`, `controller/UsersController.java`,
  `config/DiscordOAuthProperties.java`, `config/CorsConfig.java`, `filter/JwtFilter.java`, `util/JwtUtil.java`,
  `dto/UserRequestDto.java`, `dto/AuthResponseDto.java`, `dto/RefreshTokenRequestDto.java`,
  `dto/DiscordTicketRequestDto.java`, `dto/DiscordExchangeResponseDto.java`,
  `dto/ChangePasswordRequestDto.java`, `dto/UpdateLanguageRequestDto.java`, `dto/DeleteAccountRequestDto.java`,
  `exception/UserNotFoundException.java`, `exception/UserAlreadyExistsException.java`,
  `exception/InvalidEmailDomainException.java`, `exception/InvalidFileFormatException.java`
- Delete from mail-service (test): `controller/AccessControllerTest.java`, `controller/UsersControllerTest.java`,
  `service/UserServiceTest.java`, `repository/UserRepositoryTest.java`, `filter/JwtFilterTest.java`
- Modify: `mail-service/pom.xml`
- Modify: `mail-service/src/main/java/com/seamail/mail/MailServiceApplication.java`
- Create: `mail-service/src/main/java/com/seamail/mail/client/AuthUserClient.java`
- Modify: `mail-service/src/main/java/com/seamail/mail/config/SecurityConfig.java` (full rewrite)
- Modify: `mail-service/src/main/java/com/seamail/mail/service/EmailService.java`
- Modify: `mail-service/src/main/java/com/seamail/mail/controller/EmailsController.java`
- Modify: `mail-service/src/main/java/com/seamail/mail/exception/GlobalExceptionHandler.java`
- Modify: `mail-service/src/main/resources/application.properties`, `application-local.properties`, test properties
- Modify: `mail-service/src/test/java/com/seamail/mail/controller/EmailsControllerTest.java`
- Modify: `mail-service/src/test/java/com/seamail/mail/service/EmailServiceTest.java`
- Modify: `mail-service/src/test/java/com/seamail/mail/integration/FullFlowIntegrationTest.java` (rewrite)

**Interfaces:**
- Consumes: `GET {auth.service.url}/internal/users/{email}/exists` from Task 3.
- Produces: `AuthUserClient.assertUserExists(String email): void` (Feign; 404 -> `FeignException.NotFound`).
- Produces: principal contract `Jwt.getSubject()` = caller email, read via `@AuthenticationPrincipal(expression = "subject")`.

- [ ] **Step 1: Delete moved/obsolete files**

Run from repo root:

```powershell
$deleteMain = @(
  'entity\User.java', 'repository\UserRepository.java',
  'service\IUserService.java', 'service\UserService.java', 'service\CustomUserDetailsService.java',
  'controller\AccessController.java', 'controller\OAuth2Controller.java', 'controller\UsersController.java',
  'config\DiscordOAuthProperties.java', 'config\CorsConfig.java',
  'filter\JwtFilter.java', 'util\JwtUtil.java',
  'dto\UserRequestDto.java', 'dto\AuthResponseDto.java', 'dto\RefreshTokenRequestDto.java',
  'dto\DiscordTicketRequestDto.java', 'dto\DiscordExchangeResponseDto.java',
  'dto\ChangePasswordRequestDto.java', 'dto\UpdateLanguageRequestDto.java', 'dto\DeleteAccountRequestDto.java',
  'exception\UserNotFoundException.java', 'exception\UserAlreadyExistsException.java',
  'exception\InvalidEmailDomainException.java', 'exception\InvalidFileFormatException.java'
)
$deleteTest = @(
  'controller\AccessControllerTest.java', 'controller\UsersControllerTest.java',
  'service\UserServiceTest.java', 'repository\UserRepositoryTest.java', 'filter\JwtFilterTest.java'
)
foreach ($rel in $deleteMain) { git rm "mail-service\src\main\java\com\seamail\mail\$rel" }
foreach ($rel in $deleteTest) { git rm "mail-service\src\test\java\com\seamail\mail\$rel" }
```

- [ ] **Step 2: Update mail-service/pom.xml dependencies**

Delete these dependency blocks: all three `io.jsonwebtoken` (jjwt-api, jjwt-impl, jjwt-jackson) and `org.springframework.security:spring-security-oauth2-client`. Add:

```xml
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.cloud</groupId>
			<artifactId>spring-cloud-starter-openfeign</artifactId>
		</dependency>
```

- [ ] **Step 3: Enable Feign on the application class**

In `MailServiceApplication.java`, add the import `org.springframework.cloud.openfeign.EnableFeignClients` and annotate:

```java
@SpringBootApplication
@EnableFeignClients
@EnableTransactionManagement(order = Ordered.LOWEST_PRECEDENCE) // transaction is the inner advice
public class MailServiceApplication {
```

- [ ] **Step 4: Create AuthUserClient**

`mail-service/src/main/java/com/seamail/mail/client/AuthUserClient.java`:

```java
package com.seamail.mail.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

// Calls auth-service inside the container network (the gateway has no /internal/** route).
// A 404 response surfaces as FeignException.NotFound.
@FeignClient(name = "auth-service", url = "${auth.service.url}")
public interface AuthUserClient {

    @GetMapping("/internal/users/{email}/exists")
    void assertUserExists(@PathVariable String email);
}
```

- [ ] **Step 5: Rewrite mail SecurityConfig as a resource server**

Full replacement of `mail-service/src/main/java/com/seamail/mail/config/SecurityConfig.java`:

```java
package com.seamail.mail.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

// Stateless resource server: tokens are validated against auth-service's JWKS endpoint
// (spring.security.oauth2.resourceserver.jwt.jwk-set-uri). The key set is fetched lazily
// on first token validation and cached by the decoder.
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/actuator/health/**", "/v3/api-docs/**").permitAll()
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpStatus.UNAUTHORIZED.value());
                    response.setContentType("application/json");
                    response.getWriter().write("{\"message\": \"Unauthorized\"}");
                }));
        return http.build();
    }
}
```

- [ ] **Step 6: Rewire EmailService to the Feign client**

In `mail-service/src/main/java/com/seamail/mail/service/EmailService.java`:

- Replace import `com.seamail.mail.repository.UserRepository` with `com.seamail.mail.client.AuthUserClient`.
- Replace the field `private final UserRepository userRepository;` with `private final AuthUserClient authUserClient;`
- Replace the constructor with:

```java
    public EmailService(EmailRepository repository, AuthUserClient authUserClient) {
        this.repository = repository;
        this.authUserClient = authUserClient;
    }
```

- Replace the body check in `sendEmail`:

```java
        if (userRepository.findByEmail(request.getReceiver()).isEmpty()) {
            throw new ReceiverNotFoundException("Receiver not found");
        }
```

with:

```java
        try {
            authUserClient.assertUserExists(request.getReceiver());
        } catch (FeignException.NotFound ex) {
            throw new ReceiverNotFoundException("Receiver not found");
        } catch (FeignException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Authentication service unavailable");
        }
```

- Add import `feign.FeignException;`.

- [ ] **Step 7: Update EmailsController principals**

In `EmailsController.java`, replace every occurrence of `@AuthenticationPrincipal String email` with `@AuthenticationPrincipal(expression = "subject") String email` and `@AuthenticationPrincipal String senderEmail` with `@AuthenticationPrincipal(expression = "subject") String senderEmail` (7 handler methods total).

- [ ] **Step 8: Trim mail GlobalExceptionHandler**

- Replace the not-found handler annotation `@ExceptionHandler({UserNotFoundException.class, EmailNotFoundException.class, ReceiverNotFoundException.class})` with `@ExceptionHandler({EmailNotFoundException.class, ReceiverNotFoundException.class})`.
- Delete the entire `handleConflict` method (`@ExceptionHandler(UserAlreadyExistsException.class)`).
- Delete the entire `handleBadRequest` method (`@ExceptionHandler({InvalidEmailDomainException.class, InvalidFileFormatException.class})`).

- [ ] **Step 9: Update mail properties**

In `mail-service/src/main/resources/application.properties`:

- Delete the `jwt.secret=...` line, all five `discord.*` lines, and the `cors.allowed.origin` line.
- Change `server.port=8081` to `server.port=8083`.
- Change `spring.data.redis.username=${REDIS_USERNAME}` to `spring.data.redis.username=${REDIS_USERNAME:}` and password likewise.
- Append:

```properties
# Resource server: validate tokens against auth-service's public keys
spring.security.oauth2.resourceserver.jwt.jwk-set-uri=${JWKS_URI:http://localhost:8082/.well-known/jwks.json}

# auth-service internal API (Feign)
auth.service.url=${AUTH_SERVICE_URL:http://localhost:8082}
spring.cloud.openfeign.client.config.default.connect-timeout=2000
spring.cloud.openfeign.client.config.default.read-timeout=3000
```

In `application-local.properties`, delete the `cors.allowed.origin` line (the gateway owns CORS now).

In `mail-service/src/test/resources/application-test.properties`, delete `jwt.secret=...` and all `discord.*` lines, and append:

```properties
# Resource server decoder is lazy; never contacted in slices (jwt() post-processor used)
spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:8082/.well-known/jwks.json
auth.service.url=http://localhost:8082
```

- [ ] **Step 10: Rewrite EmailsControllerTest to jwt() style**

Apply the same transformation recipe as Task 3 Step 12: delete the `JwtUtil` `@MockBean` + import, delete `setUpSecurity()`/`tearDownSecurity()` + security-context imports, add `import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;`, and add `.with(jwt().jwt(j -> j.subject(TEST_EMAIL)))` to every `mockMvc.perform(...)`. `@Import(TestSecurityConfig.class)` stays.

- [ ] **Step 11: Update EmailServiceTest**

- Replace `@Mock private UserRepository userRepository;` with `@Mock private AuthUserClient authUserClient;` (import `com.seamail.mail.client.AuthUserClient`; remove `UserRepository`, `User` imports).
- In `shouldSendEmailWhenReceiverExists`: delete the `when(userRepository.findByEmail(...))` stubbing (the Feign void call does nothing by default).
- In `shouldThrowReceiverNotFoundWhenReceiverDoesNotExist`: replace the stubbing with:

```java
        doThrow(FeignException.NotFound.class).when(authUserClient).assertUserExists("missing@seamail.com");
```

(add imports `feign.FeignException` and `static org.mockito.Mockito.doThrow` if not already covered by the wildcard).

- [ ] **Step 12: Rewrite FullFlowIntegrationTest**

Full replacement of `mail-service/src/test/java/com/seamail/mail/integration/FullFlowIntegrationTest.java`:

```java
package com.seamail.mail.integration;

import com.seamail.mail.client.AuthUserClient;
import com.seamail.mail.repository.EmailRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Full mail flow against the real security config (resource server) with the
// jwt() post-processor standing in for a token; the receiver check is mocked
// at the Feign boundary. Replaced by Testcontainers MailFlowIT in Task 12.
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class FullFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EmailRepository emailRepository;

    @MockBean
    private AuthUserClient authUserClient;

    @BeforeEach
    public void setup() {
        emailRepository.deleteAll();
    }

    @Test
    public void testUserSendEmailAndReceiverChecksInbox() throws Exception {
        String senderEmail = "sender@seamail.com";
        String receiverEmail = "receiver@seamail.com";

        mockMvc.perform(post("/api/v1/send-email")
                .with(jwt().jwt(j -> j.subject(senderEmail)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{" +
                        "\"receiver\":\"" + receiverEmail + "\"," +
                        "\"subject\":\"Integration Test\"," +
                        "\"body\":\"Hello from the full flow test!\"," +
                        "\"priority\":\"1\"" +
                        "}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/inbox")
                .with(jwt().jwt(j -> j.subject(receiverEmail))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].sender").value(senderEmail))
                .andExpect(jsonPath("$.content[0].subject").value("Integration Test"));
    }
}
```

- [ ] **Step 13: Verify**

Run from repo root: `mvn test`
Expected: BUILD SUCCESS both modules. If `MailServiceApplicationTests` (contextLoads) fails because a bean reference is unresolved, the most likely cause is a leftover import of a deleted class - grep for `UserRepository|JwtUtil|JwtFilter|IUserService` under `mail-service/src` and remove stragglers.

- [ ] **Step 14: Commit**

```powershell
git add -A
git commit -m "Convert mail-service to OAuth2 resource server with Feign receiver check"
```

---

### Task 5: Compose infrastructure - schemas per service, local Redis

**Files:**
- Create: `db/init/01-schemas.sh` (LF line endings)
- Create/Modify: `.gitattributes`
- Modify: `docker-compose.yml`

**Interfaces:**
- Produces: schemas `seamail_auth`, `seamail_mail`, `seamail_notifications`; users `seamail_auth_user`, `seamail_mail_user`, `seamail_notification_user` with grants scoped to their own schema.
- Produces: compose services `db` (MySQL :3307) and `redis` (:6379, no auth).

- [ ] **Step 1: Create .gitattributes for LF shell scripts**

Create/append to repo-root `.gitattributes`:

```
*.sh text eol=lf
```

- [ ] **Step 2: Create the schema init script**

Create `db/init/01-schemas.sh` (write with LF endings only; the file-write tool output is LF):

```bash
#!/bin/bash
# Creates one schema + one scoped user per service (database-per-service on a single MySQL).
# Passwords come from the compose environment; they are local-dev credentials only.
set -e
mysql -u root -p"$MYSQL_ROOT_PASSWORD" <<-EOSQL
    CREATE SCHEMA IF NOT EXISTS $AUTH_DB_NAME;
    CREATE SCHEMA IF NOT EXISTS $MAIL_DB_NAME;
    CREATE SCHEMA IF NOT EXISTS $NOTIFICATION_DB_NAME;

    CREATE USER IF NOT EXISTS '$AUTH_DB_USER'@'%' IDENTIFIED BY '$AUTH_DB_PASSWORD';
    CREATE USER IF NOT EXISTS '$MAIL_DB_USER'@'%' IDENTIFIED BY '$MAIL_DB_PASSWORD';
    CREATE USER IF NOT EXISTS '$NOTIFICATION_DB_USER'@'%' IDENTIFIED BY '$NOTIFICATION_DB_PASSWORD';

    GRANT ALL PRIVILEGES ON $AUTH_DB_NAME.* TO '$AUTH_DB_USER'@'%';
    GRANT ALL PRIVILEGES ON $MAIL_DB_NAME.* TO '$MAIL_DB_USER'@'%';
    GRANT ALL PRIVILEGES ON $NOTIFICATION_DB_NAME.* TO '$NOTIFICATION_DB_USER'@'%';
    FLUSH PRIVILEGES;
EOSQL
```

- [ ] **Step 3: Rewrite docker-compose.yml db service and add redis**

Replace the `db` service and remove the old `SQL Scripts/Tables.sql` mount; the compose file at the end of this task:

```yaml
services:

  # -- MySQL Database --------------------------------
  db:
    image: mysql:8.0
    container_name: mysql_db
    restart: always
    environment:
      MYSQL_ROOT_PASSWORD: ${DB_ROOT_PASSWORD}
      AUTH_DB_NAME: ${AUTH_DB_NAME}
      AUTH_DB_USER: ${AUTH_DB_USER}
      AUTH_DB_PASSWORD: ${AUTH_DB_PASSWORD}
      MAIL_DB_NAME: ${MAIL_DB_NAME}
      MAIL_DB_USER: ${MAIL_DB_USER}
      MAIL_DB_PASSWORD: ${MAIL_DB_PASSWORD}
      NOTIFICATION_DB_NAME: ${NOTIFICATION_DB_NAME}
      NOTIFICATION_DB_USER: ${NOTIFICATION_DB_USER}
      NOTIFICATION_DB_PASSWORD: ${NOTIFICATION_DB_PASSWORD}
    volumes:
      - mysql_data:/var/lib/mysql
      - ./db/init:/docker-entrypoint-initdb.d
    ports:
      - "3307:3306"
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      timeout: 5s
      retries: 5

  # -- Redis (refresh tokens, inbox cache) -----------
  redis:
    image: redis:7-alpine
    container_name: redis_cache
    restart: always
    ports:
      - "6379:6379"
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 3s
      retries: 5

volumes:
  mysql_data:
```

(The `version: '3.8'` line is obsolete in compose v2 - delete it. The backend/frontend services return in Task 6.)

- [ ] **Step 4: Verify infra comes up and schemas exist**

Requires a matching `.env.docker` (the user owns env files; if values are missing, ask the user to confirm `.env.docker` has the new keys printed in Task 9 Step 2). Run:

```powershell
docker compose --env-file .env.docker up -d db redis
docker exec mysql_db mysql -u root -p"$env:DB_ROOT_PASSWORD" -e "SHOW DATABASES;"
```

Expected: `seamail_auth`, `seamail_mail`, `seamail_notifications` in the list. (If the volume already exists from the old setup, the init script does not re-run; wipe it once with `docker compose --env-file .env.docker down -v` first - confirm with the user before deleting the volume.)

- [ ] **Step 5: Commit**

```powershell
git add -A
git commit -m "Add per-service MySQL schemas and local Redis to compose"
```

---

### Task 6: api-gateway module + compose wiring

**Files:**
- Create: `api-gateway/pom.xml`
- Create: `api-gateway/Dockerfile`
- Create: `api-gateway/src/main/java/com/seamail/gateway/ApiGatewayApplication.java`
- Create: `api-gateway/src/main/resources/application.yml`
- Create: `Dockerfile.backend` (root, parameterized)
- Modify: `docker-compose.yml` (add auth-service, mail-service, api-gateway, frontend changes)
- Modify: `frontend-email-service/nginx.conf` (proxy target rename)
- Modify: root `pom.xml` (add module)

**Interfaces:**
- Consumes: auth :8082, mail :8083 (notification :8084 arrives in Task 8; route is defined now).
- Produces: single entry `:8081` with routes; CORS handled only at gateway; `/internal/**` unrouted.

- [ ] **Step 1: Create api-gateway/pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
	<modelVersion>4.0.0</modelVersion>
	<parent>
		<groupId>com.seamail</groupId>
		<artifactId>seamail-parent</artifactId>
		<version>0.0.1-SNAPSHOT</version>
	</parent>
	<artifactId>api-gateway</artifactId>
	<version>0.0.1-SNAPSHOT</version>
	<name>api-gateway</name>
	<description>Seamail API gateway</description>

	<dependencies>
		<dependency>
			<groupId>org.springframework.cloud</groupId>
			<artifactId>spring-cloud-starter-gateway</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-actuator</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-test</artifactId>
			<scope>test</scope>
		</dependency>
	</dependencies>

	<build>
		<plugins>
			<plugin>
				<groupId>org.springframework.boot</groupId>
				<artifactId>spring-boot-maven-plugin</artifactId>
			</plugin>
		</plugins>
	</build>
</project>
```

Add `<module>api-gateway</module>` to the root pom (first in the list).

- [ ] **Step 2: Create the gateway application class and config**

`api-gateway/src/main/java/com/seamail/gateway/ApiGatewayApplication.java`:

```java
package com.seamail.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ApiGatewayApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApiGatewayApplication.class, args);
	}

}
```

`api-gateway/src/main/resources/application.yml`:

```yaml
server:
  port: 8081

spring:
  application:
    name: api-gateway
  cloud:
    gateway:
      globalcors:
        cors-configurations:
          '[/**]':
            allowed-origins: "${CORS_ALLOWED_ORIGIN:http://localhost}"
            allowed-methods: [GET, POST, PUT, DELETE, OPTIONS]
            allowed-headers: "*"
            allow-credentials: true
      # Route order matters: the most specific prefixes must come before the mail catch-all.
      routes:
        - id: auth-well-known
          uri: ${AUTH_SERVICE_URL:http://localhost:8082}
          predicates:
            - Path=/.well-known/**
        - id: auth-api
          uri: ${AUTH_SERVICE_URL:http://localhost:8082}
          predicates:
            - Path=/api/v1/sign-in,/api/v1/sign-up,/api/v1/auth/**,/api/v1/change-password,/api/v1/update-language,/api/v1/delete-account,/api/v1/*/profile-picture
        - id: notification-api
          uri: ${NOTIFICATION_SERVICE_URL:http://localhost:8084}
          predicates:
            - Path=/api/v1/notifications/**
        - id: mail-api
          uri: ${MAIL_SERVICE_URL:http://localhost:8083}
          predicates:
            - Path=/api/v1/**

management:
  endpoints:
    web:
      exposure:
        include: health
```

- [ ] **Step 3: Create the parameterized backend Dockerfile**

Create root `Dockerfile.backend`:

```dockerfile
# Multi-module build: context must be the repo root so the reactor parent pom resolves.
# Build a single module with: docker build -f Dockerfile.backend --build-arg MODULE=auth-service .
FROM maven:3.9-eclipse-temurin-21 AS builder
ARG MODULE
WORKDIR /app
COPY pom.xml .
COPY api-gateway/pom.xml api-gateway/
COPY auth-service/pom.xml auth-service/
COPY mail-service/pom.xml mail-service/
COPY notification-service/pom.xml notification-service/
RUN mvn -pl ${MODULE} -am dependency:go-offline -B
COPY ${MODULE}/src ${MODULE}/src
RUN mvn -pl ${MODULE} -am package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
ARG MODULE
COPY --from=builder /app/${MODULE}/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```

(The `notification-service/pom.xml` COPY fails until Task 8 creates it - compose builds gateway/auth/mail only in this task, but the Dockerfile COPY needs the file to exist. Create a placeholder-free solution: run Task 6's docker builds only after Task 8, OR temporarily comment that COPY line and restore it in Task 8. Choose: comment it now, restore in Task 8 Step 10.)

`api-gateway/Dockerfile`: same standalone pattern as auth-service's (for completeness).

- [ ] **Step 4: Add app services to docker-compose.yml**

Insert after the `redis` service:

```yaml
  # -- auth-service ----------------------------------
  auth-service:
    build:
      context: .
      dockerfile: Dockerfile.backend
      args:
        MODULE: auth-service
    container_name: auth_service
    restart: always
    depends_on:
      db:
        condition: service_healthy
      redis:
        condition: service_healthy
    environment:
      DB_NAME: ${AUTH_DB_NAME}
      DB_USER: ${AUTH_DB_USER}
      DB_PASSWORD: ${AUTH_DB_PASSWORD}
      REDIS_HOST: redis
      REDIS_PORT: 6379
      REDIS_USERNAME: ""
      REDIS_PASSWORD: ""
      CORS_ALLOWED_ORIGIN: ${CORS_ALLOWED_ORIGIN}
      DISCORD_CLIENT_ID: ${DISCORD_CLIENT_ID}
      DISCORD_CLIENT_SECRET: ${DISCORD_CLIENT_SECRET}
      DISCORD_REDIRECT_URI: ${DISCORD_REDIRECT_URI}
      JWT_PRIVATE_KEY_PATH: ${JWT_PRIVATE_KEY_PATH:-}
      JWT_PUBLIC_KEY_PATH: ${JWT_PUBLIC_KEY_PATH:-}
    ports:
      - "8082:8082"
    healthcheck:
      test: ["CMD-SHELL", "wget -qO- http://localhost:8082/actuator/health | grep -q UP || exit 1"]
      interval: 15s
      timeout: 5s
      retries: 10

  # -- mail-service ----------------------------------
  mail-service:
    build:
      context: .
      dockerfile: Dockerfile.backend
      args:
        MODULE: mail-service
    container_name: mail_service
    restart: always
    depends_on:
      db:
        condition: service_healthy
      redis:
        condition: service_healthy
      auth-service:
        condition: service_started
    environment:
      DB_NAME: ${MAIL_DB_NAME}
      DB_USER: ${MAIL_DB_USER}
      DB_PASSWORD: ${MAIL_DB_PASSWORD}
      REDIS_HOST: redis
      REDIS_PORT: 6379
      REDIS_USERNAME: ""
      REDIS_PASSWORD: ""
      AUTH_SERVICE_URL: http://auth-service:8082
      JWKS_URI: http://auth-service:8082/.well-known/jwks.json
    ports:
      - "8083:8083"
    healthcheck:
      test: ["CMD-SHELL", "wget -qO- http://localhost:8083/actuator/health | grep -q UP || exit 1"]
      interval: 15s
      timeout: 5s
      retries: 10

  # -- api-gateway (single entry point) ---------------
  api-gateway:
    build:
      context: .
      dockerfile: Dockerfile.backend
      args:
        MODULE: api-gateway
    container_name: api_gateway
    restart: always
    depends_on:
      auth-service:
        condition: service_started
      mail-service:
        condition: service_started
    environment:
      CORS_ALLOWED_ORIGIN: ${CORS_ALLOWED_ORIGIN}
      AUTH_SERVICE_URL: http://auth-service:8082
      MAIL_SERVICE_URL: http://mail-service:8083
      NOTIFICATION_SERVICE_URL: http://notification-service:8084
    ports:
      - "8081:8081"
```

Change the `frontend` service: `depends_on: - api-gateway` (replace `- backend`).

- [ ] **Step 5: Point the frontend nginx proxy at the gateway**

In `frontend-email-service/nginx.conf`, replace both occurrences of `proxy_pass http://spring_backend:8081;` with `proxy_pass http://api_gateway:8081;` (one active, one in the commented production block - update both for consistency).

- [ ] **Step 6: Verify build and unit tests**

Run: `mvn test`
Expected: BUILD SUCCESS for all three modules (gateway has no tests; compiles).

- [ ] **Step 7: Commit**

```powershell
git add -A
git commit -m "Add Spring Cloud Gateway as single entry point with compose wiring"
```

---

### Task 7: Kafka producer in mail-service

**Files:**
- Modify: `docker-compose.yml` (add kafka, wire mail env)
- Modify: `mail-service/pom.xml`
- Create: `mail-service/src/main/java/com/seamail/mail/event/EmailSentEvent.java`
- Create: `mail-service/src/main/java/com/seamail/mail/messaging/EmailEventListener.java`
- Modify: `mail-service/src/main/java/com/seamail/mail/service/EmailService.java`
- Modify: `mail-service/src/main/resources/application.properties`
- Modify: `mail-service/src/test/resources/application-test.properties`
- Modify: `mail-service/src/test/java/com/seamail/mail/service/EmailServiceTest.java`
- Modify: `mail-service/src/test/java/com/seamail/mail/integration/FullFlowIntegrationTest.java`

**Interfaces:**
- Produces: topic `email.sent`; payload `EmailSentEvent(String eventId, String eventType, String occurredAt, Long emailId, String sender, String receiver, String subject)` JSON; key = receiver.
- Produces: `EmailService` publishes via `ApplicationEventPublisher`; `EmailEventListener.onEmailSent(EmailSentEvent)` sends AFTER_COMMIT.

- [ ] **Step 1: Add Kafka to docker-compose.yml**

Insert after `redis`:

```yaml
  # -- Kafka (single broker, KRaft - no Zookeeper) ----
  kafka:
    image: confluentinc/cp-kafka:7.6.1
    container_name: kafka_broker
    restart: always
    ports:
      - "9092:9092"
    environment:
      KAFKA_NODE_ID: 1
      KAFKA_PROCESS_ROLES: broker,controller
      KAFKA_CONTROLLER_QUORUM_VOTERS: 1@kafka:29092
      KAFKA_LISTENERS: PLAINTEXT://kafka:29092,CONTROLLER://kafka:29093,PLAINTEXT_HOST://0.0.0.0:9092
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:29092,PLAINTEXT_HOST://localhost:9092
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT
      KAFKA_CONTROLLER_LISTENER_NAMES: CONTROLLER
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_AUTO_CREATE_TOPICS_ENABLE: "true"
      CLUSTER_ID: MkU3OEVBNTcwNTJENDM2Qk
    healthcheck:
      test: ["CMD-SHELL", "kafka-broker-api-versions --bootstrap-server kafka:29092 || exit 1"]
      interval: 15s
      timeout: 10s
      retries: 10
```

In `mail-service` environment add: `KAFKA_BOOTSTRAP_SERVERS: kafka:29092`, and add `kafka: condition: service_healthy` to its `depends_on`.

- [ ] **Step 2: Add spring-kafka to mail-service/pom.xml**

```xml
		<dependency>
			<groupId>org.springframework.kafka</groupId>
			<artifactId>spring-kafka</artifactId>
		</dependency>
```

and in the test scope area:

```xml
		<dependency>
			<groupId>org.springframework.kafka</groupId>
			<artifactId>spring-kafka-test</artifactId>
			<scope>test</scope>
		</dependency>
```

- [ ] **Step 3: Create the event record**

`mail-service/src/main/java/com/seamail/mail/event/EmailSentEvent.java`:

```java
package com.seamail.mail.event;

// Contract for the email.sent Kafka topic.
// occurredAt is an ISO-8601 string to keep JSON serialization trivial and explicit.
public record EmailSentEvent(
        String eventId,
        String eventType,
        String occurredAt,
        Long emailId,
        String sender,
        String receiver,
        String subject
) {}
```

- [ ] **Step 4: Create the AFTER_COMMIT listener**

`mail-service/src/main/java/com/seamail/mail/messaging/EmailEventListener.java`:

```java
package com.seamail.mail.messaging;

import com.seamail.mail.event.EmailSentEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

// Publishes domain events only after the DB transaction commits, so consumers
// never see events for rolled-back emails. Deliberately not a full outbox table:
// a broker hiccup between commit and send can drop an event (at-most-once) -
// the trade-off is documented in the README talking points.
@Component
public class EmailEventListener {

    private static final Logger log = LoggerFactory.getLogger(EmailEventListener.class);

    private final KafkaTemplate<String, EmailSentEvent> kafkaTemplate;
    private final String topic;

    public EmailEventListener(KafkaTemplate<String, EmailSentEvent> kafkaTemplate,
                              @Value("${kafka.topic.email-sent:email.sent}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEmailSent(EmailSentEvent event) {
        kafkaTemplate.send(topic, event.receiver(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish event {} to {}", event.eventId(), topic, ex);
                    } else {
                        log.info("Published event {} to {}", event.eventId(), topic);
                    }
                });
    }
}
```

- [ ] **Step 5: Publish the event from EmailService**

In `EmailService.java`:

- Add fields and constructor params:

```java
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    public EmailService(EmailRepository repository, AuthUserClient authUserClient,
                        org.springframework.context.ApplicationEventPublisher eventPublisher) {
        this.repository = repository;
        this.authUserClient = authUserClient;
        this.eventPublisher = eventPublisher;
    }
```

- At the end of `sendEmail`, after `repository.save(email);` add:

```java
        eventPublisher.publishEvent(new com.seamail.mail.event.EmailSentEvent(
                java.util.UUID.randomUUID().toString(),
                "EMAIL_SENT",
                java.time.Instant.now().toString(),
                email.getEmailID(),
                senderEmail,
                request.getReceiver(),
                request.getSubject()
        ));
```

- [ ] **Step 6: Kafka producer properties**

Append to `mail-service/src/main/resources/application.properties`:

```properties
# Kafka producer
spring.kafka.bootstrap-servers=${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer
spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer
kafka.topic.email-sent=email.sent
```

Append to `mail-service/src/test/resources/application-test.properties`:

```properties
# Kafka is mocked in tests (@MockBean KafkaTemplate); bootstrap value only satisfies autoconfig
spring.kafka.bootstrap-servers=localhost:9092
kafka.topic.email-sent=email.sent
```

- [ ] **Step 7: Update tests for the new constructor/dependency**

In `EmailServiceTest.java`:

- Add `@Mock private org.springframework.context.ApplicationEventPublisher eventPublisher;` (import or fully-qualified).
- In `shouldSendEmailWhenReceiverExists`, after the existing asserts add:

```java
        verify(eventPublisher).publishEvent(any(com.seamail.mail.event.EmailSentEvent.class));
```

In `FullFlowIntegrationTest.java`:

- Add `@MockBean private KafkaTemplate<String, EmailSentEvent> kafkaTemplate;` with imports `com.seamail.mail.event.EmailSentEvent` and `org.springframework.kafka.core.KafkaTemplate`.
- In `setup()` add (so the listener's `whenComplete` never sees a null future):

```java
        org.mockito.Mockito.lenient()
                .when(kafkaTemplate.send(org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.any(EmailSentEvent.class)))
                .thenReturn(java.util.concurrent.CompletableFuture.completedFuture(null));
```

- [ ] **Step 8: Verify**

Run: `mvn test`
Expected: BUILD SUCCESS all modules.

- [ ] **Step 9: Commit**

```powershell
git add -A
git commit -m "Publish EmailSentEvent to Kafka after mail transaction commit"
```

---

### Task 8: notification-service module

**Files:**
- Create: `notification-service/pom.xml`, `Dockerfile`
- Create: `notification-service/src/main/java/com/seamail/notification/NotificationServiceApplication.java`
- Create: `notification-service/src/main/java/com/seamail/notification/entity/Notification.java`
- Create: `notification-service/src/main/java/com/seamail/notification/repository/NotificationRepository.java`
- Create: `notification-service/src/main/java/com/seamail/notification/event/EmailSentEvent.java`
- Create: `notification-service/src/main/java/com/seamail/notification/messaging/EmailSentConsumer.java`
- Create: `notification-service/src/main/java/com/seamail/notification/config/KafkaConsumerConfig.java`
- Create: `notification-service/src/main/java/com/seamail/notification/service/NotificationService.java`
- Create: `notification-service/src/main/java/com/seamail/notification/controller/NotificationController.java`
- Create: `notification-service/src/main/java/com/seamail/notification/dto/NotificationResponseDto.java`, `dto/UnreadCountDto.java`
- Create: `notification-service/src/main/java/com/seamail/notification/config/SecurityConfig.java`
- Create: `notification-service/src/main/java/com/seamail/notification/exception/{ApplicationException,ErrorResponse,ValidationErrorResponse,GlobalExceptionHandler,NotificationNotFoundException}.java`
- Create: `notification-service/src/main/resources/application.properties`, `application-local.properties`, `db/migration/V1__create_notifications_table.sql`
- Create: `notification-service/src/test/resources/application-test.properties`
- Create: `notification-service/src/test/java/com/seamail/notification/config/TestSecurityConfig.java`
- Create: `notification-service/src/test/java/com/seamail/notification/service/NotificationServiceTest.java`
- Create: `notification-service/src/test/java/com/seamail/notification/controller/NotificationControllerTest.java`
- Create: `notification-service/src/test/java/com/seamail/notification/repository/NotificationRepositoryTest.java`
- Modify: root `pom.xml` (add module), `docker-compose.yml` (add service), `Dockerfile.backend` (restore notification COPY)

**Interfaces:**
- Consumes: topic `email.sent`, group `notification-service`; DLT `email.sent.DLT`.
- Produces: `GET /api/v1/notifications?page&size` -> `Page<NotificationResponseDto>`; `GET /api/v1/notifications/unread-count` -> `{"count": n}`; `POST /api/v1/notifications/{id}/read` -> 204, 404 when not found or foreign-owned.
- `NotificationResponseDto(Long id, String type, Long sourceEmailId, String subjectSnapshot, String senderSnapshot, boolean read, LocalDateTime createdAt)`.

- [ ] **Step 1: pom, application class, Dockerfile, root module entry**

`notification-service/pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
	<modelVersion>4.0.0</modelVersion>
	<parent>
		<groupId>com.seamail</groupId>
		<artifactId>seamail-parent</artifactId>
		<version>0.0.1-SNAPSHOT</version>
	</parent>
	<artifactId>notification-service</artifactId>
	<version>0.0.1-SNAPSHOT</version>
	<name>notification-service</name>
	<description>Seamail notification service</description>

	<dependencies>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-data-jpa</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-web</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-security</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-validation</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-actuator</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.kafka</groupId>
			<artifactId>spring-kafka</artifactId>
		</dependency>
		<dependency>
			<groupId>org.flywaydb</groupId>
			<artifactId>flyway-core</artifactId>
		</dependency>
		<dependency>
			<groupId>org.flywaydb</groupId>
			<artifactId>flyway-mysql</artifactId>
		</dependency>
		<dependency>
			<groupId>com.mysql</groupId>
			<artifactId>mysql-connector-j</artifactId>
			<scope>runtime</scope>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-test</artifactId>
			<scope>test</scope>
		</dependency>
		<dependency>
			<groupId>org.springframework.security</groupId>
			<artifactId>spring-security-test</artifactId>
			<scope>test</scope>
		</dependency>
		<dependency>
			<groupId>org.springframework.kafka</groupId>
			<artifactId>spring-kafka-test</artifactId>
			<scope>test</scope>
		</dependency>
		<dependency>
			<groupId>com.h2database</groupId>
			<artifactId>h2</artifactId>
			<scope>test</scope>
		</dependency>
	</dependencies>

	<build>
		<plugins>
			<plugin>
				<groupId>org.springframework.boot</groupId>
				<artifactId>spring-boot-maven-plugin</artifactId>
			</plugin>
		</plugins>
	</build>
</project>
```

Add `<module>notification-service</module>` to the root pom (after mail-service).

`notification-service/src/main/java/com/seamail/notification/NotificationServiceApplication.java`:

```java
package com.seamail.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.Ordered;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableTransactionManagement(order = Ordered.LOWEST_PRECEDENCE) // transaction is the inner advice
public class NotificationServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(NotificationServiceApplication.class, args);
	}

}
```

`notification-service/Dockerfile`: same standalone pattern as auth-service (EXPOSE 8084).

- [ ] **Step 2: Entity, repository, DTOs**

`entity/Notification.java`:

```java
package com.seamail.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true, length = 64)
    private String eventId;

    @Column(name = "recipient_email", nullable = false)
    private String recipientEmail;

    @Column(name = "type", nullable = false, length = 32)
    private String type;

    @Column(name = "source_email_id", nullable = false)
    private Long sourceEmailId;

    @Column(name = "subject_snapshot", nullable = false)
    private String subjectSnapshot;

    @Column(name = "sender_snapshot", nullable = false)
    private String senderSnapshot;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected Notification() {
    }

    public Notification(String eventId, String recipientEmail, String type, Long sourceEmailId,
                        String subjectSnapshot, String senderSnapshot, LocalDateTime createdAt) {
        this.eventId = eventId;
        this.recipientEmail = recipientEmail;
        this.type = type;
        this.sourceEmailId = sourceEmailId;
        this.subjectSnapshot = subjectSnapshot;
        this.senderSnapshot = senderSnapshot;
        this.read = false;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getEventId() {
        return eventId;
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public String getType() {
        return type;
    }

    public Long getSourceEmailId() {
        return sourceEmailId;
    }

    public String getSubjectSnapshot() {
        return subjectSnapshot;
    }

    public String getSenderSnapshot() {
        return senderSnapshot;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
```

`repository/NotificationRepository.java`:

```java
package com.seamail.notification.repository;

import com.seamail.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByRecipientEmailOrderByCreatedAtDesc(String recipientEmail, Pageable pageable);

    long countByRecipientEmailAndReadFalse(String recipientEmail);

    boolean existsByEventId(String eventId);
}
```

`dto/NotificationResponseDto.java`:

```java
package com.seamail.notification.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public record NotificationResponseDto(
        Long id,
        String type,
        Long sourceEmailId,
        String subjectSnapshot,
        String senderSnapshot,
        boolean read,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime createdAt
) {}
```

`dto/UnreadCountDto.java`:

```java
package com.seamail.notification.dto;

public record UnreadCountDto(long count) {}
```

- [ ] **Step 3: Consumer-side event record and Kafka consumer**

`event/EmailSentEvent.java`:

```java
package com.seamail.notification.event;

// Consumer-side view of the email.sent contract (mail-service's event record).
// Duplicated intentionally: services share contracts, not code.
public record EmailSentEvent(
        String eventId,
        String eventType,
        String occurredAt,
        Long emailId,
        String sender,
        String receiver,
        String subject
) {}
```

`messaging/EmailSentConsumer.java`:

```java
package com.seamail.notification.messaging;

import com.seamail.notification.event.EmailSentEvent;
import com.seamail.notification.service.NotificationService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class EmailSentConsumer {

    private final NotificationService notificationService;

    public EmailSentConsumer(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @KafkaListener(topics = "${kafka.topic.email-sent:email.sent}", groupId = "notification-service")
    public void onEmailSent(EmailSentEvent event) {
        notificationService.recordEmailSent(event);
    }
}
```

`config/KafkaConsumerConfig.java`:

```java
package com.seamail.notification.config;

import com.seamail.notification.event.EmailSentEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOff;

@Configuration
public class KafkaConsumerConfig {

    // After the backoff budget is exhausted, the record is published to email.sent.DLT
    // instead of blocking the partition forever (poison-pill handling).
    @Bean
    public DefaultErrorHandler errorHandler(KafkaTemplate<Object, Object> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate);
        ExponentialBackOff backOff = new ExponentialBackOff(1000L, 2.0);
        backOff.setMaxElapsedTime(5000L); // ~3 retries: 1s, 2s, 4s
        return new DefaultErrorHandler(recoverer, backOff);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, EmailSentEvent> kafkaListenerContainerFactory(
            ConsumerFactory<String, EmailSentEvent> consumerFactory,
            DefaultErrorHandler errorHandler) {
        ConcurrentKafkaListenerContainerFactory<String, EmailSentEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(errorHandler);
        factory.getContainerProperties().setObservationEnabled(true); // Kafka spans in traces
        return factory;
    }
}
```

- [ ] **Step 4: NotificationService**

`service/NotificationService.java`:

```java
package com.seamail.notification.service;

import com.seamail.notification.dto.NotificationResponseDto;
import com.seamail.notification.entity.Notification;
import com.seamail.notification.event.EmailSentEvent;
import com.seamail.notification.exception.NotificationNotFoundException;
import com.seamail.notification.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository repository;

    public NotificationService(NotificationRepository repository) {
        this.repository = repository;
    }

    // Idempotent consumer: Kafka delivers at-least-once, so duplicates are expected.
    // First line of defense is the existsByEventId check; the unique constraint on
    // event_id closes the race between two concurrent deliveries of the same event.
    @Transactional
    public void recordEmailSent(EmailSentEvent event) {
        if (repository.existsByEventId(event.eventId())) {
            log.info("Duplicate event {} - already recorded, skipping", event.eventId());
            return;
        }
        try {
            repository.save(new Notification(
                    event.eventId(),
                    event.receiver(),
                    "EMAIL_SENT",
                    event.emailId(),
                    event.subject(),
                    event.sender(),
                    LocalDateTime.now()
            ));
        } catch (DataIntegrityViolationException ex) {
            log.info("Duplicate event {} detected via unique constraint - skipping", event.eventId());
        }
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponseDto> getNotifications(String recipientEmail, Pageable pageable) {
        return repository.findByRecipientEmailOrderByCreatedAtDesc(recipientEmail, pageable)
                .map(this::toDto);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(String recipientEmail) {
        return repository.countByRecipientEmailAndReadFalse(recipientEmail);
    }

    @Transactional
    public void markAsRead(Long id, String recipientEmail) {
        Notification notification = repository.findById(id)
                .orElseThrow(() -> new NotificationNotFoundException("Notification not found: " + id));
        if (!notification.getRecipientEmail().equals(recipientEmail)) {
            // 404 (not 403) on purpose: do not leak the existence of other users' notifications
            throw new NotificationNotFoundException("Notification not found: " + id);
        }
        notification.setRead(true);
        repository.save(notification);
    }

    private NotificationResponseDto toDto(Notification n) {
        return new NotificationResponseDto(
                n.getId(),
                n.getType(),
                n.getSourceEmailId(),
                n.getSubjectSnapshot(),
                n.getSenderSnapshot(),
                n.isRead(),
                n.getCreatedAt()
        );
    }
}
```

- [ ] **Step 5: Controller**

`controller/NotificationController.java`:

```java
package com.seamail.notification.controller;

import com.seamail.notification.dto.NotificationResponseDto;
import com.seamail.notification.dto.UnreadCountDto;
import com.seamail.notification.service.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
@Validated
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<Page<NotificationResponseDto>> getNotifications(
            @AuthenticationPrincipal(expression = "subject") String email,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(notificationService.getNotifications(email, pageable));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<UnreadCountDto> getUnreadCount(
            @AuthenticationPrincipal(expression = "subject") String email) {
        return ResponseEntity.ok(new UnreadCountDto(notificationService.getUnreadCount(email)));
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(
            @AuthenticationPrincipal(expression = "subject") String email,
            @PathVariable Long id) {
        notificationService.markAsRead(id, email);
        return ResponseEntity.noContent().build();
    }
}
```

- [ ] **Step 6: Exceptions and error contract**

`exception/ApplicationException.java`, `exception/ErrorResponse.java`, `exception/ValidationErrorResponse.java`: copy verbatim from `mail-service/src/main/java/com/seamail/mail/exception/` with the package changed to `com.seamail.notification.exception`.

`exception/NotificationNotFoundException.java`:

```java
package com.seamail.notification.exception;

public class NotificationNotFoundException extends ApplicationException {

    public NotificationNotFoundException(String message) {
        super("NOTIFICATION_NOT_FOUND", message);
    }
}
```

`exception/GlobalExceptionHandler.java`: copy from mail-service's version with package changed and the domain handler reduced to:

```java
    @ExceptionHandler(NotificationNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ApplicationException ex,
                                                         HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(HttpStatus.NOT_FOUND, ex.getErrorCode(),
                        ex.getMessage(), request.getRequestURI()));
    }
```

(Keep the validation, malformed-request, type-mismatch, missing-parameter, data-integrity, ResponseStatusException, and catch-all handlers exactly as in mail-service's copy.)

- [ ] **Step 7: SecurityConfig**

`config/SecurityConfig.java`: identical to mail-service's Task 4 Step 5 version, package `com.seamail.notification.config`.

- [ ] **Step 8: Resources**

`notification-service/src/main/resources/application.properties`:

```properties
# Server
server.port=8084

# Database Configuration
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.datasource.url=jdbc:mysql://db:3306/${DB_NAME}?sessionVariables=sql_mode='NO_ENGINE_SUBSTITUTION'&jdbcCompliantTruncation=false
spring.datasource.username=${DB_USER}
spring.datasource.password=${DB_PASSWORD}

# JPA Configuration
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.open-in-view=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect

# Flyway migrations
spring.flyway.enabled=true
spring.flyway.baseline-on-migrate=true

# Resource server: validate tokens against auth-service's public keys
spring.security.oauth2.resourceserver.jwt.jwk-set-uri=${JWKS_URI:http://localhost:8082/.well-known/jwks.json}

# Kafka consumer
spring.kafka.bootstrap-servers=${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
spring.kafka.consumer.group-id=notification-service
spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer
spring.kafka.consumer.value-deserializer=org.springframework.kafka.support.serializer.JsonDeserializer
spring.kafka.consumer.properties.spring.json.use.type.headers=false
spring.kafka.consumer.properties.spring.json.value.default.type=com.seamail.notification.event.EmailSentEvent
spring.kafka.consumer.auto-offset-reset=earliest
kafka.topic.email-sent=email.sent

# Kafka producer (only used by the dead-letter recoverer)
spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer
spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer

# Actuator
management.endpoints.web.exposure.include=health
management.endpoint.health.show-details=when-authorized
management.endpoint.health.group.liveness.include=livenessState
management.endpoint.health.group.readiness.include=readinessState,db
management.endpoint.health.probes.enabled=true
```

`application-local.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3307/${DB_NAME}?sessionVariables=sql_mode='NO_ENGINE_SUBSTITUTION'&jdbcCompliantTruncation=false
logging.level.org.hibernate.SQL=DEBUG
```

`db/migration/V1__create_notifications_table.sql`:

```sql
CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT NOT NULL AUTO_INCREMENT,
    event_id VARCHAR(64) NOT NULL,
    recipient_email VARCHAR(255) NOT NULL,
    type VARCHAR(32) NOT NULL,
    source_email_id BIGINT NOT NULL,
    subject_snapshot VARCHAR(255) NOT NULL,
    sender_snapshot VARCHAR(255) NOT NULL,
    is_read TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uc_notifications_event_id UNIQUE (event_id),
    INDEX idx_notifications_recipient_read (recipient_email, is_read)
);
```

`notification-service/src/test/resources/application-test.properties`:

```properties
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.url=jdbc:h2:mem:notificationdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
spring.datasource.username=sa
spring.datasource.password=
server.port=8084
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect
spring.flyway.enabled=false
spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:8082/.well-known/jwks.json
spring.kafka.bootstrap-servers=localhost:9092
spring.kafka.consumer.group-id=notification-service-test
spring.kafka.listener.auto-startup=false
kafka.topic.email-sent=email.sent
spring.cache.type=simple
```

- [ ] **Step 9: Tests**

`src/test/java/com/seamail/notification/config/TestSecurityConfig.java`: copy from mail-service's test config, package `com.seamail.notification.config`.

`service/NotificationServiceTest.java`:

```java
package com.seamail.notification.service;

import com.seamail.notification.dto.NotificationResponseDto;
import com.seamail.notification.entity.Notification;
import com.seamail.notification.event.EmailSentEvent;
import com.seamail.notification.exception.NotificationNotFoundException;
import com.seamail.notification.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository repository;

    @InjectMocks
    private NotificationService notificationService;

    private EmailSentEvent sampleEvent() {
        return new EmailSentEvent("evt-1", "EMAIL_SENT", "2026-07-21T10:00:00Z",
                42L, "sender@seamail.com", "receiver@seamail.com", "Hello");
    }

    @Test
    void shouldRecordNotificationForNewEvent() {
        when(repository.existsByEventId("evt-1")).thenReturn(false);

        notificationService.recordEmailSent(sampleEvent());

        verify(repository).save(any(Notification.class));
    }

    @Test
    void shouldSkipDuplicateEvent() {
        when(repository.existsByEventId("evt-1")).thenReturn(true);

        notificationService.recordEmailSent(sampleEvent());

        verify(repository, never()).save(any());
    }

    @Test
    void shouldSkipWhenUniqueConstraintFiresOnRace() {
        when(repository.existsByEventId("evt-1")).thenReturn(false);
        when(repository.save(any(Notification.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate event_id"));

        assertDoesNotThrow(() -> notificationService.recordEmailSent(sampleEvent()));
    }

    @Test
    void shouldReturnUnreadCount() {
        when(repository.countByRecipientEmailAndReadFalse("receiver@seamail.com")).thenReturn(3L);

        assertEquals(3L, notificationService.getUnreadCount("receiver@seamail.com"));
    }

    @Test
    void shouldReturnPaginatedFeed() {
        Notification n = new Notification("evt-1", "receiver@seamail.com", "EMAIL_SENT",
                42L, "Hello", "sender@seamail.com", LocalDateTime.now());
        when(repository.findByRecipientEmailOrderByCreatedAtDesc(any(String.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(n)));

        Page<NotificationResponseDto> page =
                notificationService.getNotifications("receiver@seamail.com", Pageable.unpaged());

        assertEquals(1, page.getContent().size());
        assertEquals("Hello", page.getContent().get(0).subjectSnapshot());
    }

    @Test
    void shouldMarkAsReadWhenOwner() {
        Notification n = new Notification("evt-1", "receiver@seamail.com", "EMAIL_SENT",
                42L, "Hello", "sender@seamail.com", LocalDateTime.now());
        when(repository.findById(1L)).thenReturn(Optional.of(n));

        notificationService.markAsRead(1L, "receiver@seamail.com");

        assertTrue(n.isRead());
        verify(repository).save(n);
    }

    @Test
    void shouldThrow404WhenMarkingForeignNotification() {
        Notification n = new Notification("evt-1", "receiver@seamail.com", "EMAIL_SENT",
                42L, "Hello", "sender@seamail.com", LocalDateTime.now());
        when(repository.findById(1L)).thenReturn(Optional.of(n));

        assertThrows(NotificationNotFoundException.class,
                () -> notificationService.markAsRead(1L, "other@seamail.com"));
    }

    @Test
    void shouldThrow404WhenNotificationMissing() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotificationNotFoundException.class,
                () -> notificationService.markAsRead(99L, "receiver@seamail.com"));
    }
}
```

`controller/NotificationControllerTest.java`:

```java
package com.seamail.notification.controller;

import com.seamail.notification.config.TestSecurityConfig;
import com.seamail.notification.dto.NotificationResponseDto;
import com.seamail.notification.exception.NotificationNotFoundException;
import com.seamail.notification.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NotificationController.class)
@Import(TestSecurityConfig.class)
class NotificationControllerTest {

    private static final String TEST_EMAIL = "receiver@seamail.com";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificationService notificationService;

    @Test
    void shouldReturnPaginatedFeed() throws Exception {
        NotificationResponseDto dto = new NotificationResponseDto(1L, "EMAIL_SENT", 42L,
                "Hello", "sender@seamail.com", false, LocalDateTime.now());
        when(notificationService.getNotifications(eq(TEST_EMAIL), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(dto)));

        mockMvc.perform(get("/api/v1/notifications")
                .with(jwt().jwt(j -> j.subject(TEST_EMAIL))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].subjectSnapshot").value("Hello"));
    }

    @Test
    void shouldReturnUnreadCount() throws Exception {
        when(notificationService.getUnreadCount(TEST_EMAIL)).thenReturn(2L);

        mockMvc.perform(get("/api/v1/notifications/unread-count")
                .with(jwt().jwt(j -> j.subject(TEST_EMAIL))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(2));
    }

    @Test
    void shouldReturn204WhenMarkingRead() throws Exception {
        mockMvc.perform(post("/api/v1/notifications/1/read")
                .with(jwt().jwt(j -> j.subject(TEST_EMAIL))))
                .andExpect(status().isNoContent());

        verify(notificationService).markAsRead(1L, TEST_EMAIL);
    }

    @Test
    void shouldReturn404WhenNotificationNotFound() throws Exception {
        doThrow(new NotificationNotFoundException("Notification not found: 9"))
                .when(notificationService).markAsRead(9L, TEST_EMAIL);

        mockMvc.perform(post("/api/v1/notifications/9/read")
                .with(jwt().jwt(j -> j.subject(TEST_EMAIL))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOTIFICATION_NOT_FOUND"));
    }
}
```

`repository/NotificationRepositoryTest.java`:

```java
package com.seamail.notification.repository;

import com.seamail.notification.entity.Notification;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class NotificationRepositoryTest {

    @Autowired
    private NotificationRepository repository;

    private Notification sample(String eventId, String recipient) {
        return new Notification(eventId, recipient, "EMAIL_SENT", 42L,
                "Hello", "sender@seamail.com", LocalDateTime.now());
    }

    @Test
    void shouldEnforceUniqueEventId() {
        repository.saveAndFlush(sample("evt-dup", "receiver@seamail.com"));

        assertThrows(DataIntegrityViolationException.class,
                () -> repository.saveAndFlush(sample("evt-dup", "receiver@seamail.com")));
    }

    @Test
    void shouldCountUnreadPerRecipient() {
        repository.save(sample("evt-a", "receiver@seamail.com"));
        Notification read = sample("evt-b", "receiver@seamail.com");
        read.setRead(true);
        repository.save(read);
        repository.save(sample("evt-c", "other@seamail.com"));
        repository.flush();

        assertEquals(1L, repository.countByRecipientEmailAndReadFalse("receiver@seamail.com"));
    }

    @Test
    void shouldPageRecipientFeed() {
        repository.save(sample("evt-a", "receiver@seamail.com"));
        repository.save(sample("evt-b", "receiver@seamail.com"));
        repository.flush();

        assertEquals(2, repository
                .findByRecipientEmailOrderByCreatedAtDesc("receiver@seamail.com", Pageable.unpaged())
                .getContent().size());
    }
}
```

- [ ] **Step 10: Compose service + Dockerfile.backend restore**

Add to `docker-compose.yml` after mail-service:

```yaml
  # -- notification-service ---------------------------
  notification-service:
    build:
      context: .
      dockerfile: Dockerfile.backend
      args:
        MODULE: notification-service
    container_name: notification_service
    restart: always
    depends_on:
      db:
        condition: service_healthy
      kafka:
        condition: service_healthy
      auth-service:
        condition: service_started
    environment:
      DB_NAME: ${NOTIFICATION_DB_NAME}
      DB_USER: ${NOTIFICATION_DB_USER}
      DB_PASSWORD: ${NOTIFICATION_DB_PASSWORD}
      KAFKA_BOOTSTRAP_SERVERS: kafka:29092
      JWKS_URI: http://auth-service:8082/.well-known/jwks.json
    ports:
      - "8084:8084"
    healthcheck:
      test: ["CMD-SHELL", "wget -qO- http://localhost:8084/actuator/health | grep -q UP || exit 1"]
      interval: 15s
      timeout: 5s
      retries: 10
```

In `Dockerfile.backend`, restore the line `COPY notification-service/pom.xml notification-service/` if it was commented in Task 6 Step 3.

- [ ] **Step 11: Verify**

Run: `mvn test`
Expected: BUILD SUCCESS all four modules.

- [ ] **Step 12: Commit**

```powershell
git add -A
git commit -m "Add notification-service consuming email.sent with idempotent feed"
```

---

### Task 9: Env templates + full-stack smoke verification

**Files:**
- Modify: `.env.docker.example`
- Modify: `docker-compose.yml` (frontend depends_on already set in Task 6; final review)

**Interfaces:**
- Produces: the complete env contract every service expects.

- [ ] **Step 1: Rewrite .env.docker.example**

Full replacement content:

```properties
# MySQL (root + one schema/user per service)
DB_ROOT_PASSWORD=your_db_root_password
AUTH_DB_NAME=seamail_auth
AUTH_DB_USER=seamail_auth_user
AUTH_DB_PASSWORD=your_auth_db_password
MAIL_DB_NAME=seamail_mail
MAIL_DB_USER=seamail_mail_user
MAIL_DB_PASSWORD=your_mail_db_password
NOTIFICATION_DB_NAME=seamail_notifications
NOTIFICATION_DB_USER=seamail_notification_user
NOTIFICATION_DB_PASSWORD=your_notification_db_password

# Redis (local container, no auth)
REDIS_HOST=redis
REDIS_PORT=6379
REDIS_USERNAME=
REDIS_PASSWORD=

# JWT signing (leave empty for an ephemeral dev keypair)
# Generate stable keys with:
#   openssl genrsa -out keys/private.pem 2048
#   openssl rsa -in keys/private.pem -pubout -out keys/public.pem
# and mount ./keys into auth-service, setting the paths to /keys/private.pem and /keys/public.pem
JWT_PRIVATE_KEY_PATH=
JWT_PUBLIC_KEY_PATH=

# Frontend / OAuth
CORS_ALLOWED_ORIGIN=http://localhost
DISCORD_CLIENT_ID=your_discord_client_id
DISCORD_CLIENT_SECRET=your_discord_client_secret
DISCORD_REDIRECT_URI=http://localhost:8081/api/v1/auth/discord

# Frontend build args (unchanged)
VITE_BACKEND_API_URL=/api/v1
VITE_DISCORD_REDIRECT_URI=http://localhost:8081/api/v1/auth/discord
VITE_CLIENT_ID=your_discord_client_id
VITE_TERMS_OF_USE_URL=your_terms_of_use_url
VITE_PRIVACY_POLICY_URL=your_privacy_policy_url
```

Ask the user to sync the same keys into their real `.env.docker` (never edit it yourself).

- [ ] **Step 2: Full-stack smoke**

Run:

```powershell
docker compose --env-file .env.docker up --build -d
docker compose --env-file .env.docker ps
```

Expected: db, redis_cache, kafka_broker, auth_service, mail_service, notification_service, api_gateway, react_frontend all Up (healthy where defined).

Then the end-to-end sequence (PowerShell):

```powershell
# sign-up two users
Invoke-RestMethod -Method Post -Uri http://localhost:8081/api/v1/sign-up -ContentType 'application/json' -Body '{"email":"alice@seamail.com","password":"pass1234"}'
Invoke-RestMethod -Method Post -Uri http://localhost:8081/api/v1/sign-up -ContentType 'application/json' -Body '{"email":"bob@seamail.com","password":"pass1234"}'

# sign-in as alice, capture the access token
$login = Invoke-RestMethod -Method Post -Uri http://localhost:8081/api/v1/sign-in -ContentType 'application/json' -Body '{"email":"alice@seamail.com","password":"pass1234"}'
$aliceToken = $login.accessToken

# alice sends bob an email
$headers = @{ Authorization = "Bearer $aliceToken" }
Invoke-RestMethod -Method Post -Uri http://localhost:8081/api/v1/send-email -Headers $headers -ContentType 'application/json' -Body '{"receiver":"bob@seamail.com","subject":"Hi Bob","body":"first cross-service email","priority":"1"}'

# bob signs in, sees the email in his inbox
$bobLogin = Invoke-RestMethod -Method Post -Uri http://localhost:8081/api/v1/sign-in -ContentType 'application/json' -Body '{"email":"bob@seamail.com","password":"pass1234"}'
$bobHeaders = @{ Authorization = "Bearer $($bobLogin.accessToken)" }
Invoke-RestMethod -Uri 'http://localhost:8081/api/v1/inbox' -Headers $bobHeaders | ConvertTo-Json -Depth 5

# bob has one unread notification produced via Kafka
Start-Sleep -Seconds 3
Invoke-RestMethod -Uri 'http://localhost:8081/api/v1/notifications/unread-count' -Headers $bobHeaders

# no token -> 401 with the contract body
try { Invoke-RestMethod -Uri 'http://localhost:8081/api/v1/inbox' } catch { $_.Exception.Response.StatusCode }
```

Expected: inbox `content` has 1 entry from alice; unread-count `{"count":1}`; the last call reports 401.

If anything fails: `docker logs auth_service`, `docker logs mail_service`, `docker logs notification_service`, `docker logs api_gateway` - fix and re-run. Do not proceed until the smoke passes.

- [ ] **Step 3: Commit**

```powershell
git add -A
git commit -m "Update env template and verify full-stack smoke through gateway"
```

---

### Task 10: Observability - Prometheus metrics, Zipkin tracing, Grafana

**Files:**
- Modify: `mail-service/pom.xml`, `auth-service/pom.xml`, `notification-service/pom.xml`, `api-gateway/pom.xml`
- Modify: `mail-service/src/main/resources/application.properties`
- Modify: `auth-service/src/main/resources/application.properties`
- Modify: `notification-service/src/main/resources/application.properties`
- Modify: `api-gateway/src/main/resources/application.yml`
- Create: `mail-service/src/main/java/com/seamail/mail/config/KafkaTracingConfig.java`
- Create: `observability/prometheus.yml`
- Create: `observability/grafana/datasources/datasource.yml`
- Create: `observability/grafana/dashboards/dashboard.yml`
- Create: `observability/grafana/dashboards/seamail-overview.json`
- Modify: `docker-compose.yml` (zipkin, prometheus, grafana + env on app services)

**Interfaces:**
- Produces: `/actuator/prometheus` on all 4 modules; traces to Zipkin (`ZIPKIN_ENDPOINT` env, default `http://localhost:9411`); log pattern with `%X{traceId}`/`%X{spanId}`.

- [ ] **Step 1: Add observability dependencies to all four module poms**

Add to each of the four `pom.xml` files:

```xml
		<dependency>
			<groupId>io.micrometer</groupId>
			<artifactId>micrometer-registry-prometheus</artifactId>
		</dependency>
		<dependency>
			<groupId>io.micrometer</groupId>
			<artifactId>micrometer-tracing-bridge-brave</artifactId>
		</dependency>
		<dependency>
			<groupId>io.zipkin.reporter2</groupId>
			<artifactId>zipkin-reporter-brave</artifactId>
		</dependency>
```

- [ ] **Step 2: Observability properties for the three MVC services**

Append to each of `mail-service`, `auth-service`, `notification-service` `src/main/resources/application.properties`:

```properties
# Observability
spring.application.name=mail-service
management.endpoints.web.exposure.include=health,info,prometheus
management.tracing.sampling.probability=1.0
management.zipkin.tracing.endpoint=${ZIPKIN_ENDPOINT:http://localhost:9411}/api/v2/spans
logging.pattern.level=%5p [${spring.application.name:},%X{traceId:-},%X{spanId:-}]
```

(Set `spring.application.name` per service: `mail-service`, `auth-service`, `notification-service`.)

- [ ] **Step 3: Gateway observability config**

In `api-gateway/src/main/resources/application.yml`, replace the `management:` block with:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
  tracing:
    sampling:
      probability: 1.0
  zipkin:
    tracing:
      endpoint: ${ZIPKIN_ENDPOINT:http://localhost:9411}/api/v2/spans

logging:
  pattern:
    level: "%5p [${spring.application.name:},%X{traceId:-},%X{spanId:-}]"
```

- [ ] **Step 4: Enable Kafka producer tracing in mail-service**

`mail-service/src/main/java/com/seamail/mail/config/KafkaTracingConfig.java`:

```java
package com.seamail.mail.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;

// Enables Micrometer observation on the producer so the email.sent publish
// appears as a span between mail-service and the consumer.
@Configuration
public class KafkaTracingConfig {

    public KafkaTracingConfig(KafkaTemplate<?, ?> kafkaTemplate) {
        kafkaTemplate.setObservationEnabled(true);
    }
}
```

(The consumer side was already enabled in Task 8 Step 3 via `factory.getContainerProperties().setObservationEnabled(true)`.)

- [ ] **Step 5: Prometheus scrape config**

`observability/prometheus.yml`:

```yaml
global:
  scrape_interval: 5s

scrape_configs:
  - job_name: api-gateway
    metrics_path: /actuator/prometheus
    static_configs:
      - targets: ["api-gateway:8081"]
  - job_name: auth-service
    metrics_path: /actuator/prometheus
    static_configs:
      - targets: ["auth-service:8082"]
  - job_name: mail-service
    metrics_path: /actuator/prometheus
    static_configs:
      - targets: ["mail-service:8083"]
  - job_name: notification-service
    metrics_path: /actuator/prometheus
    static_configs:
      - targets: ["notification-service:8084"]
```

- [ ] **Step 6: Grafana provisioning**

`observability/grafana/datasources/datasource.yml`:

```yaml
apiVersion: 1
datasources:
  - name: Prometheus
    uid: prometheus
    type: prometheus
    access: proxy
    url: http://prometheus:9090
    isDefault: true
```

`observability/grafana/dashboards/dashboard.yml`:

```yaml
apiVersion: 1
providers:
  - name: default
    folder: ''
    type: file
    options:
      path: /etc/grafana/provisioning/dashboards
```

`observability/grafana/dashboards/seamail-overview.json`:

```json
{
  "uid": "seamail-overview",
  "title": "Seamail Overview",
  "schemaVersion": 39,
  "version": 1,
  "refresh": "5s",
  "time": { "from": "now-15m", "to": "now" },
  "panels": [
    {
      "id": 1,
      "type": "timeseries",
      "title": "HTTP requests/sec by service",
      "datasource": { "type": "prometheus", "uid": "prometheus" },
      "gridPos": { "h": 8, "w": 12, "x": 0, "y": 0 },
      "targets": [
        {
          "refId": "A",
          "expr": "sum by (application) (rate(http_server_requests_seconds_count[1m]))",
          "datasource": { "type": "prometheus", "uid": "prometheus" }
        }
      ],
      "fieldConfig": { "defaults": {}, "overrides": [] },
      "options": {
        "legend": { "displayMode": "list", "placement": "bottom" },
        "tooltip": { "mode": "multi" }
      }
    },
    {
      "id": 2,
      "type": "timeseries",
      "title": "HTTP p95 latency (s)",
      "datasource": { "type": "prometheus", "uid": "prometheus" },
      "gridPos": { "h": 8, "w": 12, "x": 12, "y": 0 },
      "targets": [
        {
          "refId": "A",
          "expr": "histogram_quantile(0.95, sum by (application, le) (rate(http_server_requests_seconds_bucket[1m])))",
          "datasource": { "type": "prometheus", "uid": "prometheus" }
        }
      ],
      "fieldConfig": { "defaults": {}, "overrides": [] },
      "options": {
        "legend": { "displayMode": "list", "placement": "bottom" },
        "tooltip": { "mode": "multi" }
      }
    },
    {
      "id": 3,
      "type": "timeseries",
      "title": "JVM memory used (bytes)",
      "datasource": { "type": "prometheus", "uid": "prometheus" },
      "gridPos": { "h": 8, "w": 24, "x": 0, "y": 8 },
      "targets": [
        {
          "refId": "A",
          "expr": "sum by (application) (jvm_memory_used_bytes)",
          "datasource": { "type": "prometheus", "uid": "prometheus" }
        }
      ],
      "fieldConfig": { "defaults": {}, "overrides": [] },
      "options": {
        "legend": { "displayMode": "list", "placement": "bottom" },
        "tooltip": { "mode": "multi" }
      }
    }
  ]
}
```

- [ ] **Step 7: Compose services**

Add to `docker-compose.yml`:

```yaml
  # -- Zipkin (distributed tracing) -------------------
  zipkin:
    image: openzipkin/zipkin-slim
    container_name: zipkin_tracing
    restart: always
    ports:
      - "9411:9411"

  # -- Prometheus (metrics) ----------------------------
  prometheus:
    image: prom/prometheus
    container_name: prometheus_metrics
    restart: always
    volumes:
      - ./observability/prometheus.yml:/etc/prometheus/prometheus.yml
    ports:
      - "9090:9090"

  # -- Grafana (dashboards) ----------------------------
  grafana:
    image: grafana/grafana
    container_name: grafana_dashboards
    restart: always
    environment:
      GF_SECURITY_ADMIN_USER: admin
      GF_SECURITY_ADMIN_PASSWORD: admin
      GF_USERS_DEFAULT_THEME: light
    volumes:
      - ./observability/grafana/datasources:/etc/grafana/provisioning/datasources
      - ./observability/grafana/dashboards:/etc/grafana/provisioning/dashboards
    ports:
      - "3000:3000"
```

Add `ZIPKIN_ENDPOINT: http://zipkin:9411` to the environment of `auth-service`, `mail-service`, `notification-service`, and `api-gateway`.

- [ ] **Step 8: Verify**

Run: `mvn test` (all green), then:

```powershell
docker compose --env-file .env.docker up --build -d
```

Repeat the Task 9 smoke send, then:

- `Invoke-RestMethod http://localhost:8083/actuator/prometheus` returns metric lines containing `http_server_requests_seconds`.
- Open `http://localhost:9411`, run a search: one trace for the send spans `api-gateway`, `mail-service`, `kafka email.sent publish`, `notification-service`.
- Open `http://localhost:3000` (admin/admin): the "Seamail Overview" dashboard shows data.

- [ ] **Step 9: Commit**

```powershell
git add -A
git commit -m "Add Prometheus metrics, Zipkin tracing, and Grafana dashboard"
```

---

### Task 11: OpenAPI docs + gateway aggregation

**Files:**
- Modify: `auth-service/pom.xml`, `mail-service/pom.xml`, `notification-service/pom.xml`, `api-gateway/pom.xml`
- Create: `auth-service/src/main/java/com/seamail/auth/config/OpenApiConfig.java`
- Create: `mail-service/src/main/java/com/seamail/mail/config/OpenApiConfig.java`
- Create: `notification-service/src/main/java/com/seamail/notification/config/OpenApiConfig.java`
- Modify: `api-gateway/src/main/resources/application.yml`

**Interfaces:**
- Produces: `/v3/api-docs` per service (permitAll, already allowed in each SecurityConfig); Swagger UI at gateway `:8081/swagger-ui.html` listing all three services.

- [ ] **Step 1: Add springdoc dependencies**

Add to auth/mail/notification poms:

```xml
		<dependency>
			<groupId>org.springdoc</groupId>
			<artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
		</dependency>
```

Add to api-gateway pom:

```xml
		<dependency>
			<groupId>org.springdoc</groupId>
			<artifactId>springdoc-openapi-starter-webflux-ui</artifactId>
		</dependency>
```

- [ ] **Step 2: OpenAPI config per service**

`OpenApiConfig.java` (same shape in all three services; adjust package and title):

```java
package com.seamail.mail.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info().title("Seamail Mail Service API").version("v1"))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components().addSecuritySchemes("bearerAuth",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
```

Titles: "Seamail Auth Service API", "Seamail Mail Service API", "Seamail Notification Service API".

- [ ] **Step 3: Gateway aggregation routes**

Append to the gateway `spring.cloud.gateway.routes` list:

```yaml
        - id: auth-docs
          uri: ${AUTH_SERVICE_URL:http://localhost:8082}
          predicates:
            - Path=/v3/api-docs/auth
          filters:
            - RewritePath=/v3/api-docs/auth, /v3/api-docs
        - id: mail-docs
          uri: ${MAIL_SERVICE_URL:http://localhost:8083}
          predicates:
            - Path=/v3/api-docs/mail
          filters:
            - RewritePath=/v3/api-docs/mail, /v3/api-docs
        - id: notification-docs
          uri: ${NOTIFICATION_SERVICE_URL:http://localhost:8084}
          predicates:
            - Path=/v3/api-docs/notification
          filters:
            - RewritePath=/v3/api-docs/notification, /v3/api-docs
```

and add to the gateway `application.yml`:

```yaml
springdoc:
  swagger-ui:
    urls:
      - name: auth-service
        url: /v3/api-docs/auth
      - name: mail-service
        url: /v3/api-docs/mail
      - name: notification-service
        url: /v3/api-docs/notification
```

- [ ] **Step 4: Verify**

`mvn test` green; stack up; `http://localhost:8081/swagger-ui.html` shows the three services in the top-right selector; `Invoke-RestMethod http://localhost:8081/v3/api-docs/mail` returns the mail OpenAPI JSON.

- [ ] **Step 5: Commit**

```powershell
git add -A
git commit -m "Add OpenAPI docs with gateway-aggregated Swagger UI"
```

---

### Task 12: Testcontainers integration tests

**Files:**
- Modify: `auth-service/pom.xml`, `mail-service/pom.xml`, `notification-service/pom.xml` (testcontainers deps)
- Create: `auth-service/src/test/resources/application-it.properties`
- Create: `mail-service/src/test/resources/application-it.properties`
- Create: `notification-service/src/test/resources/application-it.properties`
- Create: `auth-service/src/test/java/com/seamail/auth/integration/AuthFlowIT.java`
- Create: `mail-service/src/test/java/com/seamail/mail/integration/MailFlowIT.java`
- Delete: `mail-service/src/test/java/com/seamail/mail/integration/FullFlowIntegrationTest.java`
- Create: `notification-service/src/test/java/com/seamail/notification/integration/NotificationFlowIT.java`

**Interfaces:**
- Consumes: everything from Tasks 3-8. `@ServiceConnection` supplies MySQL/Kafka connection details, which override `spring.datasource.*` / `spring.kafka.bootstrap-servers` properties.

- [ ] **Step 1: Add Testcontainers dependencies**

Add to auth-service pom:

```xml
		<dependency>
			<groupId>org.testcontainers</groupId>
			<artifactId>junit-jupiter</artifactId>
			<scope>test</scope>
		</dependency>
		<dependency>
			<groupId>org.testcontainers</groupId>
			<artifactId>mysql</artifactId>
			<scope>test</scope>
		</dependency>
```

Add to mail-service and notification-service poms: the same two plus:

```xml
		<dependency>
			<groupId>org.testcontainers</groupId>
			<artifactId>kafka</artifactId>
			<scope>test</scope>
		</dependency>
```

- [ ] **Step 2: it-profile properties**

`auth-service/src/test/resources/application-it.properties`:

```properties
# Datasource values are placeholders only - Testcontainers @ServiceConnection overrides them
spring.datasource.url=jdbc:mysql://localhost:3306/unused
spring.datasource.username=unused
spring.datasource.password=unused
cors.allowed.origin=http://localhost:8080
discord.client-id=test_discord_client_id
discord.client-secret=test_discord_client_secret
discord.token-url=https://discord.com/api/oauth2/token
discord.api-url=https://discord.com/api
discord.redirect-uri=http://localhost:8081/api/v1/auth/discord
spring.cache.type=simple
```

`mail-service/src/test/resources/application-it.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/unused
spring.datasource.username=unused
spring.datasource.password=unused
auth.service.url=http://localhost:8082
spring.cache.type=simple
kafka.topic.email-sent=email.sent
```

`notification-service/src/test/resources/application-it.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/unused
spring.datasource.username=unused
spring.datasource.password=unused
kafka.topic.email-sent=email.sent
```

- [ ] **Step 3: AuthFlowIT**

`auth-service/src/test/java/com/seamail/auth/integration/AuthFlowIT.java`:

```java
package com.seamail.auth.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// End-to-end auth flow against real MySQL (Flyway migrations applied) and real Redis:
// sign-up -> sign-in -> authenticated call with the RS256 token -> refresh rotation
// -> old refresh token rejected -> JWKS endpoint exposes the public key.
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("it")
@Testcontainers
class AuthFlowIT {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>(DockerImageName.parse("mysql:8.0"));

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
        registry.add("spring.data.redis.username", () -> "");
        registry.add("spring.data.redis.password", () -> "");
    }

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void signupSigninRefreshRotationAndJwks() throws Exception {
        String email = "flow@seamail.com";
        String password = "pass1234";
        String credentials = "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";

        // sign-up -> 201 with a token pair
        mockMvc.perform(post("/api/v1/sign-up")
                .contentType(MediaType.APPLICATION_JSON)
                .content(credentials))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists());

        // sign-in -> 200 with a token pair
        MvcResult login = mockMvc.perform(post("/api/v1/sign-in")
                .contentType(MediaType.APPLICATION_JSON)
                .content(credentials))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode tokens = objectMapper.readTree(login.getResponse().getContentAsString());
        String accessToken = tokens.get("accessToken").asText();
        String refreshToken = tokens.get("refreshToken").asText();

        // the RS256 access token authenticates a protected call (local decoder path)
        mockMvc.perform(put("/api/v1/update-language")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"language\":\"de\"}"))
                .andExpect(status().isNoContent());

        // refresh rotates: old token is consumed, a new pair is issued
        MvcResult refreshed = mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode rotated = objectMapper.readTree(refreshed.getResponse().getContentAsString());
        assertNotEquals(refreshToken, rotated.get("refreshToken").asText());

        // the consumed refresh token is rejected on reuse
        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isUnauthorized());

        // JWKS endpoint publishes the RSA public key
        mockMvc.perform(get("/.well-known/jwks.json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keys[0].kty").value("RSA"))
                .andExpect(jsonPath("$.keys[0].kid").exists());
    }
}
```

- [ ] **Step 4: MailFlowIT (replaces FullFlowIntegrationTest)**

Delete `FullFlowIntegrationTest.java` (`git rm`). Create `mail-service/src/test/java/com/seamail/mail/integration/MailFlowIT.java`:

```java
package com.seamail.mail.integration;

import com.seamail.mail.client.AuthUserClient;
import com.seamail.mail.event.EmailSentEvent;
import com.seamail.mail.repository.EmailRepository;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Real MySQL (Flyway) + real Kafka: send an email through the API, verify inbox delivery
// and exactly one EmailSentEvent on the topic. The Feign receiver check is mocked at the
// boundary; the other side of the Kafka contract is covered by NotificationFlowIT.
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("it")
@Testcontainers
class MailFlowIT {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>(DockerImageName.parse("mysql:8.0"));

    @Container
    @ServiceConnection
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EmailRepository emailRepository;

    @MockBean
    private AuthUserClient authUserClient;

    @BeforeEach
    void setup() {
        emailRepository.deleteAll();
    }

    @Test
    void sendEmailPersistsAndPublishesExactlyOneEvent() throws Exception {
        String senderEmail = "sender@seamail.com";
        String receiverEmail = "receiver@seamail.com";

        mockMvc.perform(post("/api/v1/send-email")
                .with(jwt().jwt(j -> j.subject(senderEmail)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{" +
                        "\"receiver\":\"" + receiverEmail + "\"," +
                        "\"subject\":\"Integration Test\"," +
                        "\"body\":\"Hello from MailFlowIT!\"," +
                        "\"priority\":\"1\"" +
                        "}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/inbox")
                .with(jwt().jwt(j -> j.subject(receiverEmail))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].sender").value(senderEmail))
                .andExpect(jsonPath("$.content[0].subject").value("Integration Test"));

        Map<String, Object> consumerProps =
                KafkaTestUtils.consumerProps(kafka.getBootstrapServers(), "mail-flow-it", "true");
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        try (Consumer<String, EmailSentEvent> consumer = new DefaultKafkaConsumerFactory<>(
                consumerProps, new StringDeserializer(),
                new JsonDeserializer<>(EmailSentEvent.class, false)).createConsumer()) {
            consumer.subscribe(List.of("email.sent"));
            ConsumerRecords<String, EmailSentEvent> records =
                    KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(15));

            assertEquals(1, records.count());
            EmailSentEvent event = records.iterator().next().value();
            assertEquals("EMAIL_SENT", event.eventType());
            assertEquals(senderEmail, event.sender());
            assertEquals(receiverEmail, event.receiver());
            assertEquals("Integration Test", event.subject());
            assertNotNull(event.eventId());
            assertNotNull(event.emailId());
        }
    }
}
```

- [ ] **Step 5: NotificationFlowIT**

`notification-service/src/test/java/com/seamail/notification/integration/NotificationFlowIT.java`:

```java
package com.seamail.notification.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seamail.notification.event.EmailSentEvent;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.apache.kafka.common.serialization.StringSerializer;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Real MySQL (Flyway) + real Kafka: the same event delivered twice yields exactly one
// notification (idempotent consumer), which then drives the feed, unread count,
// mark-read, and the foreign-owner 404.
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("it")
@Testcontainers
class NotificationFlowIT {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>(DockerImageName.parse("mysql:8.0"));

    @Container
    @ServiceConnection
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void consumesEventIdempotentlyAndExposesFeed() throws Exception {
        String receiver = "receiver@seamail.com";
        EmailSentEvent event = new EmailSentEvent(
                UUID.randomUUID().toString(), "EMAIL_SENT", Instant.now().toString(),
                42L, "sender@seamail.com", receiver, "Hello from Kafka");

        Map<String, Object> producerProps = KafkaTestUtils.producerProps(kafka.getBootstrapServers());
        producerProps.put(ProducerConfig.ACKS_CONFIG, "all");
        KafkaTemplate<String, EmailSentEvent> template = new KafkaTemplate<>(
                new DefaultKafkaProducerFactory<>(producerProps, new StringSerializer(),
                        new JsonSerializer<>()));

        // at-least-once delivery means the same event can arrive twice
        template.send("email.sent", receiver, event).get(10, TimeUnit.SECONDS);
        template.send("email.sent", receiver, event).get(10, TimeUnit.SECONDS);

        long notificationId = awaitSingleNotification(receiver);

        mockMvc.perform(get("/api/v1/notifications/unread-count")
                .with(jwt().jwt(j -> j.subject(receiver))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1));

        // a foreign user gets a 404, not a 403 (no existence leak)
        mockMvc.perform(post("/api/v1/notifications/" + notificationId + "/read")
                .with(jwt().jwt(j -> j.subject("intruder@seamail.com"))))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/v1/notifications/" + notificationId + "/read")
                .with(jwt().jwt(j -> j.subject(receiver))))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/notifications/unread-count")
                .with(jwt().jwt(j -> j.subject(receiver))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(0));
    }

    // Polls the feed until the consumer has processed the event (or fails after 15s).
    private long awaitSingleNotification(String receiver) throws Exception {
        long deadline = System.currentTimeMillis() + 15000;
        while (System.currentTimeMillis() < deadline) {
            MvcResult result = mockMvc.perform(get("/api/v1/notifications")
                    .with(jwt().jwt(j -> j.subject(receiver))))
                    .andExpect(status().isOk())
                    .andReturn();
            JsonNode content = objectMapper.readTree(result.getResponse().getContentAsString())
                    .get("content");
            if (content.size() == 1) {
                return content.get(0).get("id").asLong();
            }
            Thread.sleep(500);
        }
        fail("Notification feed did not contain exactly one entry within 15s");
        return -1; // unreachable
    }
}
```

- [ ] **Step 6: Verify**

Requires Docker Desktop running. Run from repo root: `mvn test`
Expected: BUILD SUCCESS; the three ITs start containers (first run pulls `mysql:8.0`, `redis:7-alpine`, `confluentinc/cp-kafka:7.6.1` - slow), all green.

- [ ] **Step 7: Commit**

```powershell
git add -A
git commit -m "Add Testcontainers integration tests for auth, mail, and notification flows"
```

---

### Task 13: Documentation - README talking points, AGENTS.md, root docs

**Files:**
- Modify: `README.md`
- Modify: `AGENTS.md`

**Interfaces:** none (docs only).

- [ ] **Step 1: README architecture section**

Replace the README architecture/diagram section with:

````markdown
## Architecture

```mermaid
flowchart LR
    FE[React SPA / nginx] --> GW[api-gateway :8081]
    GW --> AUTH[auth-service :8082]
    GW --> MAIL[mail-service :8083]
    GW --> NOTIF[notification-service :8084]
    MAIL -->|Feign: receiver exists?| AUTH
    MAIL -->|EmailSentEvent| K[(Kafka topic: email.sent)]
    K --> NOTIF
    AUTH --> DB1[(MySQL: seamail_auth)]
    MAIL --> DB2[(MySQL: seamail_mail)]
    NOTIF --> DB3[(MySQL: seamail_notifications)]
    AUTH --> R[(Redis)]
    MAIL --> R
    AUTH -. JWKS public keys .-> MAIL
    AUTH -. JWKS public keys .-> NOTIF
```

| Service | Responsibility |
|---|---|
| api-gateway | Single entry point, routing, CORS, Swagger UI aggregation (Spring Cloud Gateway) |
| auth-service | Sign-up/sign-in, RS256 access tokens + JWKS, rotating refresh tokens, Discord OAuth, user profile |
| mail-service | Inbox/outbox/trashbox, sorting/filtering, Redis inbox cache, publishes `email.sent` events |
| notification-service | Consumes `email.sent` idempotently, REST notification feed with unread counts |
```
````

- [ ] **Step 2: README interview talking points**

Append a new section:

```markdown
## Interview Talking Points

- **JWKS over shared secrets**: auth-service signs RS256 tokens with a private key and publishes only the public key at `/.well-known/jwks.json`; mail/notification validate statelessly with no shared secret. Key rotation only requires a new `kid`.
- **AFTER_COMMIT events vs transactional outbox**: `EmailSentEvent` is published via `@TransactionalEventListener(AFTER_COMMIT)` so consumers never see rolled-back writes. A full outbox table would upgrade at-most-once to at-least-once; the consumer is already idempotent, so the outbox is the only missing piece.
- **Idempotent consumer + DLT**: Kafka delivers at-least-once, so notification-service dedupes on a unique `event_id` (check-then-insert plus the unique constraint for races) and routes poison pills to `email.sent.DLT` after bounded exponential backoff.
- **Refresh-token rotation race**: `redisTemplate.delete(key)` is the atomic claim - only one concurrent refresh wins; the loser is rejected with 401.
- **Cache eviction**: inbox pages are cached per user+page+size and evicted wholesale on any mutation, trading fine-grained invalidation for simplicity and correctness.
- **Flyway over ddl-auto**: schema is versioned SQL per service; `ddl-auto=validate` remains as a drift detector between entities and migrations.
- **Testcontainers over H2**: integration tests run the real MySQL/Redis/Kafka, so Flyway migrations, JSON Kafka serialization, and Redis rotation logic are tested against the same engines as production.
- **One action, one trace**: a send-email request produces a single Zipkin trace crossing gateway -> mail-service -> Kafka -> notification-service, with trace/span IDs in every log line.
```

Also update the README features/API table: add the three notification endpoints (`GET /api/v1/notifications`, `GET /api/v1/notifications/unread-count`, `POST /api/v1/notifications/{id}/read`) and note that `GET /.well-known/jwks.json` is public.

- [ ] **Step 3: AGENTS.md update**

Update `AGENTS.md` to reflect the new reality, editing these sections:

- Top paragraph becomes: "Multi-package full-stack app: a Maven multi-module Spring Boot backend (`api-gateway/`, `auth-service/`, `mail-service/`, `notification-service/` under root aggregator `pom.xml`) and a React 18 + Vite frontend (`frontend-email-service/`). Root also holds `docker-compose.yml`, `Dockerfile.backend` (parameterized per-module build), `db/init/`, and `observability/`."
- Commands/Backend becomes:

```markdown
### Backend (root)
- `mvn test` - run the full reactor test suite (all four modules). Requires Docker running for the Testcontainers ITs (`*IT.java`).
- `mvn -pl mail-service test` - run one module. `mvn -pl mail-service -Dtest=ClassName test` for a single class.
- `mvn -pl mail-service spring-boot:run -Dspring-boot.run.profiles=local` - run one service locally (needs its dependencies up: `docker compose --env-file .env.docker up -d db redis kafka auth-service`).
```

- "Spring profiles" section: note each service has its own `application.properties` + `local`/`test`/`it` profiles; `it` is used by Testcontainers ITs and leaves the datasource to `@ServiceConnection`.
- "Env files and loading": per-service DB vars (`AUTH_DB_*`, `MAIL_DB_*`, `NOTIFICATION_DB_*`); `JWT_PRIVATE_KEY_PATH`/`JWT_PUBLIC_KEY_PATH` empty means ephemeral dev keypair; `KAFKA_BOOTSTRAP_SERVERS`, `JWKS_URI`, service URLs.
- "Schema and JPA": schema source of truth is now per-service Flyway (`src/main/resources/db/migration`); `SQL Scripts/Tables.sql` is retired (kept in git history); `ddl-auto=validate` still guards drift; H2 slices disable Flyway.
- "Backend architecture notes" additions: auth-service issues RS256 JWTs (Nimbus) and exposes `/.well-known/jwks.json`; mail/notification are resource servers reading the caller email via `@AuthenticationPrincipal(expression = "subject")`; mail validates receivers through `AuthUserClient` (Feign) and publishes `EmailSentEvent` AFTER_COMMIT; notification-service consumes idempotently (`event_id` unique) with DLT; gateway routes `/internal/**` nowhere; CORS only at the gateway; ports 8081-8084.
- Observability note: Prometheus on :9090, Grafana on :3000 (admin/admin), Zipkin on :9411, sampling 1.0 in dev.
- Testing notes: add the IT classes and that they need Docker; slices keep H2 + `TestSecurityConfig` + `jwt()` post-processor.

- [ ] **Step 4: Verify docs render**

Skim `README.md` in a Markdown preview (mermaid block intact), and confirm no AGENTS.md statement contradicts the code (spot-check: ports, module names, commands).

- [ ] **Step 5: Commit**

```powershell
git add -A
git commit -m "Document microservices architecture and interview talking points"
```

---

## Self-Review Notes

- Spec coverage: all 9 spec phases map to Tasks 1-13 (restructure=1-2, auth=3-4, gateway=5-6, kafka/notification=7-8, smoke/env=9, observability=10, openapi=11, testcontainers=12, docs=13).
- Type consistency: `EmailSentEvent(eventId, eventType, occurredAt[String], emailId, sender, receiver, subject)` is identical in producer (Task 7), consumer (Task 8), and ITs (Task 12). `AuthUserClient.assertUserExists` matches `InternalUsersController.existsByEmail`. `NotificationResponseDto` fields match `NotificationService.toDto` and `NotificationControllerTest`.



