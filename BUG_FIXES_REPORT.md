# Sico IoC Framework - Bug Fixes Report

## Summary

This report documents 3 significant bugs that were identified and fixed in the Sico IoC framework codebase. The bugs range from thread safety issues to logic errors in scope handling and potential naming collisions.

## Bug #1: Thread Safety Issue in SchedulerService

### **Severity:** High
### **Type:** Concurrency Bug / Thread Safety Issue

**Location:** `sico/src/main/java/dev/hogoshi/sico/scheduler/SchedulerService.java:21`

**Description:**
The `scheduledTasks` map in `SchedulerService` was using a non-thread-safe `HashMap`, but multiple threads could access it concurrently when scheduling and canceling tasks through the scheduler service.

**Problematic Code:**
```java
private final Map<String, ScheduledFuture<?>> scheduledTasks = new HashMap<>();
```

**Issues This Could Cause:**
1. **Data Corruption:** Concurrent modifications could corrupt the internal hash table structure
2. **Infinite Loops:** Corrupted linked lists in hash buckets could cause infinite loops
3. **Lost Tasks:** Race conditions could cause scheduled tasks to be lost or not properly canceled
4. **JVM Crashes:** In extreme cases, memory corruption could lead to JVM crashes

**Fix Applied:**
```java
private final Map<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();
```

**Import Updated:**
```java
import java.util.concurrent.ConcurrentHashMap;  // Added
// Removed: import java.util.HashMap;
```

**Impact:** This fix ensures thread-safe access to the scheduled tasks map, preventing race conditions and data corruption in multi-threaded environments.

---

## Bug #2: Inconsistent Prototype Scope Handling

### **Severity:** High
### **Type:** Logic Error / Architectural Bug

**Location:** `sico/src/main/java/dev/hogoshi/sico/container/DefaultContainer.java:150-180`

**Description:**
The container's `resolve()` method had inconsistent logic for prototype-scoped beans. Prototype instances were being stored in the singleton `components` map during registration, but then when retrieving them, the code would create new instances. This violated the prototype pattern where each resolve call should return a new instance.

**Problematic Code:**
```java
// In resolve() method
Object component = components.get(clazz);
if (component != null) {
    Scope.Scopes scope = determineComponentScope(clazz);
    if (scope.equals(Scope.Scopes.PROTOTYPE)) {
        try {
            return clazz.cast(createNewInstance(clazz));
        } catch (Exception e) {
            throw new RuntimeException("Error creating prototype instance for class: " + clazz.getName(), e);
        }
    }
    return clazz.cast(component);
}

// In register() method
components.put(clazz, instance);  // This stores ALL beans, including prototypes
```

**Issues This Could Cause:**
1. **Memory Leaks:** Prototype instances stored in singleton map would never be garbage collected
2. **Incorrect Behavior:** The first prototype instance would be kept around unnecessarily
3. **Performance Issues:** Unnecessary memory usage and potential for large numbers of unused prototype instances

**Fix Applied:**

**In `resolve()` method:**
```java
// Check if this is a prototype-scoped component
Scope.Scopes scope = determineComponentScope(clazz);
if (scope.equals(Scope.Scopes.PROTOTYPE)) {
    // Always create new instances for prototype scope
    if (isComponent(clazz) && !processingClasses.contains(clazz)) {
        try {
            return clazz.cast(createNewInstance(clazz));
        } catch (Exception e) {
            throw new RuntimeException("Error creating prototype instance for class: " + clazz.getName(), e);
        }
    }
    return null;
}

// For singleton scope, check the components map
Object component = components.get(clazz);
if (component != null) {
    return clazz.cast(component);
}
```

**In `register()` method:**
```java
// Only store singleton-scoped beans in the components map
if (scope.equals(Scope.Scopes.SINGLETON)) {
    components.put(clazz, instance);
}
```

**Impact:** This fix ensures proper prototype scope behavior where each call to `resolve()` returns a new instance, while singleton instances are properly cached and reused.

---

## Bug #3: Component Name Collision Vulnerability

### **Severity:** Medium
### **Type:** Logic Error / Naming Conflict

**Location:** `sico/src/main/java/dev/hogoshi/sico/container/DefaultContainer.java:446-448`

**Description:**
The `determineComponentName()` method generated component names based only on the simple class name, ignoring the package. This could cause naming collisions when two classes from different packages have the same name.

**Problematic Code:**
```java
String simpleName = clazz.getSimpleName();
return Character.toLowerCase(simpleName.charAt(0)) + simpleName.substring(1);
```

**Example Collision Scenario:**
- `com.example.model.User` → component name: "user"
- `com.example.dto.User` → component name: "user" (collision!)

**Issues This Could Cause:**
1. **Bean Overwriting:** The second bean registration would overwrite the first
2. **Unpredictable Behavior:** Which bean gets resolved would depend on registration order
3. **Difficult Debugging:** Name collisions could be hard to track down in large applications

**Fix Applied:**
```java
String simpleName = clazz.getSimpleName();
String baseName = Character.toLowerCase(simpleName.charAt(0)) + simpleName.substring(1);

// Check if this name already exists and if so, include package to avoid collisions
if (namedComponents.containsKey(baseName) || beanDefinitions.containsKey(baseName)) {
    String packageName = clazz.getPackage() != null ? clazz.getPackage().getName() : "";
    String[] packageParts = packageName.split("\\.");
    String qualifiedName = packageParts.length > 0 ? 
        packageParts[packageParts.length - 1] + "." + baseName : baseName;
    
    // If still conflicts, use fully qualified name
    if (namedComponents.containsKey(qualifiedName) || beanDefinitions.containsKey(qualifiedName)) {
        return clazz.getName().replace(".", "_").replace("$", "_");
    }
    return qualifiedName;
}

return baseName;
```

**Collision Resolution Strategy:**
1. **First Attempt:** Use simple name (e.g., "user")
2. **Second Attempt:** Use package + simple name (e.g., "model.user")
3. **Final Fallback:** Use fully qualified name (e.g., "com_example_model_User")

**Impact:** This fix prevents component name collisions while maintaining readable names in most cases, ensuring predictable bean resolution behavior.

---

## Testing Results

All existing tests continue to pass after applying these fixes:

```
BUILD SUCCESSFUL in 13s
3 actionable tasks: 2 executed, 1 up-to-date
17 tests completed successfully
```

The fixes are backward compatible and don't break any existing functionality while addressing the identified security and reliability issues.

## Recommendations

1. **Add More Tests:** Consider adding specific test cases for:
   - Concurrent scheduler operations
   - Prototype scope behavior verification
   - Component name collision scenarios

2. **Code Review:** Implement stricter code review processes to catch thread safety issues

3. **Static Analysis:** Use tools like SpotBugs or SonarQube to automatically detect concurrency and logic issues

4. **Documentation:** Update documentation to clarify prototype scope behavior and component naming rules