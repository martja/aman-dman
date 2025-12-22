# AMAN-DMAN Client - Copilot Instructions

## Project Overview

AMAN-DMAN (Arrival Manager / Departure Manager) is an aviation traffic flow management application built with Kotlin and Java Swing. It provides air traffic controllers with tools to sequence and manage aircraft arrivals and departures at airports.

**Tech Stack:**
- **Language:** Kotlin 2.2.10
- **JVM:** Java 21
- **UI Framework:** Java Swing
- **Build Tool:** Gradle 8.x (multi-module)
- **HTTP Client:** OkHttp3
- **Serialization:** Jackson (JSON/YAML)
- **Time Management:** kotlinx-datetime + NTP synchronization
- **Testing:** JUnit 5, MockK

## Architecture

### MVP Pattern (Model-View-Presenter)

This project strictly follows the MVP architectural pattern with clear separation of concerns:

#### Module Structure
- **`app`** - Application entry point and dependency wiring
- **`model`** - Business logic, domain objects, data repositories, and services
- **`presenter`** - Mediates between View and Model, handles user interactions
- **`view`** - Swing UI components (passive, no business logic)
- **`common`** - Shared utilities and data classes

#### Interface Contracts

**ViewInterface** (`presenter/ViewInterface.kt`)
- Defines methods the Presenter calls to update the UI
- Direction: Presenter → View
- All UI updates must go through these methods

**PresenterInterface** (`presenter/PresenterInterface.kt`)
- Defines user action handlers
- Direction: View → Presenter
- All user interactions must be delegated to the Presenter

**Example:**
```kotlin
class MyView : ViewInterface {
    override lateinit var presenterInterface: PresenterInterface
    
    private fun onButtonClick() {
        // Always delegate to presenter
        presenterInterface.onSomeAction(data)
    }
}
```

### Dependency Injection

**Use manual constructor-based dependency injection** - no DI framework.

Dependencies are wired in `Main.kt`:
```kotlin
fun initializeApplication() {
    val view = AmanDmanMainFrame()
    val guiUpdater = GuiDataHandler()
    val presenter = Presenter(PlannerManager(), view, guiUpdater)
    guiUpdater.presenter = presenter
    view.openWindow()
}
```

## Kotlin Coding Standards

### General Principles
1. **Immutability by default** - prefer `val` over `var`
2. **Null safety** - avoid `!!`, use safe calls `?.` and Elvis operator `?:`
3. **Data classes** for value objects and DTOs
4. **Sealed classes** for type hierarchies (e.g., `TimelineEvent`, `RunwayEvent`)
5. **Extension functions** for utility methods

### Temporal Data
Always use **`kotlinx.datetime.Instant`** for timestamps, never `java.time.Instant`:
```kotlin
import kotlinx.datetime.Instant
import no.vaccsca.amandman.common.NtpClock

val now: Instant = NtpClock.now()
```

Use **`kotlin.time.Duration`** for durations:
```kotlin
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Duration.Companion.minutes

val delay = 30.seconds
```

### Concurrency
- Use **Kotlin Coroutines** for async operations
- Use **`Mutex`** for thread-safe state management in services:
```kotlin
private val mutex = Mutex()
private val state = State()

private suspend fun <T> withStateLock(block: State.() -> T): T =
    mutex.withLock { state.block() }
```

### Serialization
Use **Jackson** with custom modules for kotlinx types:

```kotlin
private val objectMapper = ObjectMapper().apply {
    registerModule(KotlinModule.Builder().build())
    registerModule(JavaTimeModule())
    registerModule(KotlinxInstantModule) // Custom module for Instant/Duration
    findAndRegisterModules()
}
```

**Custom Jackson module for kotlinx.datetime types:**
```kotlin
object KotlinxInstantModule : SimpleModule("KotlinxInstantModule") {
    init {
        // Instant serializer/deserializer using ISO-8601 format
        addSerializer(Instant::class.java, object : JsonSerializer<Instant>() {
            override fun serialize(value: Instant, gen: JsonGenerator, serializers: SerializerProvider) {
                gen.writeString(value.toString()) // ISO-8601
            }
        })
        addDeserializer(Instant::class.java, object : JsonDeserializer<Instant>() {
            override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Instant = 
                Instant.parse(p.text)
        })
        // Duration serializer/deserializer using ISO-8601 format
        addSerializer(Duration::class.java, object : JsonSerializer<Duration>() {
            override fun serialize(value: Duration, gen: JsonGenerator, serializers: SerializerProvider) {
                gen.writeString(value.toJavaDuration().toString())
            }
        })
        addDeserializer(Duration::class.java, object : JsonDeserializer<Duration>() {
            override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Duration {
                return java.time.Duration.parse(p.text).toKotlinDuration()
            }
        })
    }
}
```

## Swing UI Patterns

### Thread Safety
All UI updates must run on the Event Dispatch Thread (EDT):

```kotlin
private fun runOnUiThread(block: () -> Unit) {
    if (SwingUtilities.isEventDispatchThread()) {
        block()
    } else {
        SwingUtilities.invokeLater(block)
    }
}

override fun updateTimelineGroups(timelineGroups: List<TimelineGroup>) = runOnUiThread {
    // UI update code here
}
```

### Component Patterns
- **JLayeredPane** for overlaying components (timelines, labels)
- **Custom JPanel** subclasses for reusable UI components
- **Null layouts** (`layout = null`) with manual positioning for precise control
- **Dialog management** - store dialog references to prevent duplicates

### Look and Feel
The app uses **JTattoo HiFi** Look and Feel configured in `Main.kt`:
```kotlin
HiFiLookAndFeel.setCurrentTheme(Properties().apply {
    put("logoString", "")
    put("backgroundPattern", "off")
})
UIManager.setLookAndFeel("com.jtattoo.plaf.hifi.HiFiLookAndFeel")
```

## Service Layer Patterns

### Master/Slave Architecture
The application supports distributed operation with master/slave roles:

- **PlannerServiceMaster** - Calculates sequences, manages state
- **PlannerServiceSlave** - Receives updates from master via HTTP
- **PlannerManager** - Registry for airport-specific services

### Repository Pattern
- **SettingsRepository** - YAML configuration loading (singleton object)
- **WeatherDataRepository** - GRIB weather data
- **CdmClient** - Collaborative Decision Making integration

### HTTP Communication
Use **OkHttp3** for all HTTP operations:

```kotlin
private val httpClient = OkHttpClient()

httpClient.newCall(request).execute().use { response ->
    val body = response.body.string()
    objectMapper.readValue(body, MyClass::class.java)
}
```

**Always use `.use { }` to ensure response cleanup.**

### State Management
Encapsulate mutable state in private data classes:

```kotlin
private data class State(
    var arrivalsCache: List<RunwayArrivalEvent> = emptyList(),
    var sequence: Sequence = Sequence(emptyList()),
    var minimumSpacingNm: Double = 3.0
)
```

## Domain Terminology

### Aviation Concepts
- **ICAO** - 4-letter airport code (e.g., ENGM, ENBR)
- **STAR** - Standard Terminal Arrival Route
- **Runway** - Landing/takeoff surface (e.g., "01L", "19R")
- **Sequence** - Ordered list of aircraft arrivals
- **Spacing** - Minimum distance between aircraft (nautical miles)
- **Timeline** - Visual representation of scheduled events
- **AMAN** - Arrival Manager
- **DMAN** - Departure Manager

### Code Conventions
- Use `airportIcao: String` for airport identifiers
- Use `callsign: String` for aircraft identifiers
- Use `minimumSpacingNm: Double` for spacing (nautical miles)
- Use `scheduledTime: Instant` for event timestamps

### External Integrations
- **EuroScope** - ATC client integration via plugin
- **NTP** - Network Time Protocol for synchronized time
- **CDM** - Collaborative Decision Making data exchange ([EuroScope plugin](https://github.com/rpuig2001/CDM))
- **GRIB** - Weather data format (netCDF library). Data is fetched from NOAA.

## Configuration

### YAML-based Settings
All configuration is in `config/` directory:
- `settings.yaml` - Application settings
- `airports.yaml` - Airport definitions
- `aircraft-performance.yaml` - Aircraft type data
- `stars/*.yaml` - STAR route definitions

Access via **SettingsRepository**:
```kotlin
val settings = SettingsRepository.getSettings()
val airport = SettingsRepository.getAirportData().find { it.icao == "ENGM" }
```

### Schema Validation
YAML files have corresponding `.schema.yaml` files for validation.

## Testing

### Test Framework
- **JUnit 5** for test structure
- **MockK** for mocking

### Test Patterns
```kotlin
@Test
fun `test arrival sequence calculation`() {
    // Given
    val arrivals = listOf(...)
    
    // When
    val sequence = service.calculateSequence(arrivals)
    
    // Then
    assertEquals(expected, sequence)
}
```

## Common Patterns

### Observer Pattern (Data Updates)
Use **DataUpdateListener** interface for reactive updates:

```kotlin
interface DataUpdateListener {
    fun onTimelineEventsUpdated(airportIcao: String, events: List<TimelineEvent>)
    fun onWeatherDataUpdated(airportIcao: String, weather: VerticalWeatherProfile?)
}
```

### Error Handling
- Use `try-catch` for expected exceptions
- Create custom exceptions in `model/domain/exception/`
- Show errors to user via `view.showErrorMessage(message)`

### Naming Conventions
- **Classes:** PascalCase (e.g., `AmanDmanMainFrame`)
- **Functions:** camelCase (e.g., `onReloadSettingsRequested`)
- **Properties:** camelCase (e.g., `minimumSpacingNm`)
- **Constants:** UPPER_SNAKE_CASE (e.g., `SESSION_ID_HEADER`)
- **Packages:** lowercase (e.g., `no.vaccsca.amandman.view`)

## Build & Deployment

### Gradle Tasks
- `./gradlew build` - Build all modules
- `./gradlew shadowJar` - Create fat JAR with dependencies
- `./gradlew test` - Run all tests

### Version Management
Version is managed in `gradle.properties` and injected into JAR manifest.

## Best Practices Summary

1. ✅ Always use MVP interfaces for View-Presenter communication
2. ✅ Use manual dependency injection in Main.kt
3. ✅ Prefer `kotlinx.datetime.Instant` over `java.time.Instant`
4. ✅ Use `runOnUiThread` for all UI updates
5. ✅ Use Mutex for thread-safe state in services
6. ✅ Use Jackson with custom modules for serialization
7. ✅ Use OkHttp3 with `.use { }` for HTTP calls
8. ✅ Create data classes for DTOs and value objects
9. ✅ Use sealed classes for type hierarchies
10. ✅ Access NTP-synced time via `NtpClock.now()`

