# Merge Conflict Resolution Guide

## Conflict Location
**File**: `backend/pdf-service/pom.xml`  
**Line**: ~36-40

## Conflict Details

### Our Branch (copilot/check-compilation-errors-services)
```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-database-postgresql</artifactId>
    <!-- Must be 10.4.1+ as flyway-database-postgresql wasn't available in 9.22.3 -->
    <version>10.4.1</version>
</dependency>
```

### Main Branch
```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-database-postgresql</artifactId>
    <version>${flyway.version}</version>
</dependency>
```

## Resolution

**Choose our version** (10.4.1 with the comment).

### Why?

1. **Spring Boot 3.2.3** (parent POM) defines `flyway.version=9.22.3`
2. The `flyway-database-postgresql` artifact was **introduced in Flyway 10.x**
3. Using `${flyway.version}` (9.22.3) causes this error:
   ```
   Could not find artifact org.flywaydb:flyway-database-postgresql:jar:9.22.3
   ```

### How to Resolve

When GitHub shows the merge conflict, or if merging locally:

1. Edit `backend/pdf-service/pom.xml`
2. Replace the conflict section with:
   ```xml
   <dependency>
       <groupId>org.flywaydb</groupId>
       <artifactId>flyway-database-postgresql</artifactId>
       <!-- Must be 10.4.1+ as flyway-database-postgresql wasn't available in 9.22.3 -->
       <version>10.4.1</version>
   </dependency>
   ```
3. Mark the conflict as resolved
4. Complete the merge

### Verification

After resolving:
```bash
cd backend/pdf-service
mvn clean compile -DskipTests
# Should result in: BUILD SUCCESS
```

## Alternative Solution

If you prefer to use the property approach, update the parent `backend/pom.xml` to override Flyway version:

```xml
<properties>
    <flyway.version>10.4.1</flyway.version>
</properties>
```

Then use `${flyway.version}` in pdf-service/pom.xml. This affects all services using Flyway.
