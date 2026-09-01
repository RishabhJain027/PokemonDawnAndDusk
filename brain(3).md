# Pokémon Dawn & Dusk — Brain / Master Implementation Plan

## 0. Source of truth

Reference repository:
`https://github.com/lucasvegi/PokemonGoCloneOffline.git`

Game name: **Pokémon Dawn & Dusk**

Project intent: Build a modern, original Pokémon-inspired location/AR adventure using the legacy repository as a technical reference and gameplay-flow blueprint. The new implementation should not copy the old security model, API secrets, deprecated Android stack, or branded production assets without the appropriate rights.

Default branch: `master`.

The repository is a didactic native Android Java project that reproduces selected Pokémon GO-style flows: authentication, GPS/map exploration, nearby Pokémon spawning, AR-style capture using camera + gyroscope + touch, trainer profile, Pokédex, and capture-history map.

The original repository is old: `compileSdkVersion 23`, `minSdkVersion 17`, `targetSdkVersion 21`, Android Support Library 23.1.1, and Google Play Services 8.3.0. Treat the original project as a legacy reference, not as the recommended modern dependency baseline.

## 1. Clone and inspect

### Clone
```bash
git clone https://github.com/lucasvegi/PokemonGoCloneOffline.git
cd PokemonGoCloneOffline
git checkout master
```

### Open
Open the cloned folder in Android Studio.

### First objective
Do **not** start by rewriting the app. First establish a reproducible baseline:
1. Open the project.
2. Let Gradle resolve the legacy dependencies.
3. Record all build errors and missing SDK/dependencies.
4. Determine whether the project can run on an emulator/device.
5. Only after the baseline is understood, migrate or rebuild individual pieces.

## 2. What the original app contains

The README describes these core features:
- Register trainer and avatar gender.
- Login/logout.
- Search for Pokémon while moving on the map.
- Random Pokémon appear inside a dynamic radius around the trainer.
- Pokémon are respawned periodically.
- Clicking a nearby Pokémon opens an augmented-reality-style capture screen.
- Capture screen uses camera, gyroscope, sound and touch-controlled Poké Ball movement.
- Trainer profile and capture history.
- Pokédex containing the first 151 Pokémon.
- Pokémon details and map locations where captures happened.

The README also identifies the technical areas intentionally demonstrated: Android lifecycle, activities/fragments, layouts/views, threads, HTTP/JSON, SQLite, intents, camera, audio, Google Maps, geolocation, touch interaction, gyroscope and architecture/design patterns.

## 3. Legacy architecture map

The repository is organized primarily around:

```text
app/
  src/main/
    AndroidManifest.xml
    java/teste/lucasvegi/pokemongooffline/
      Controller/
      Model/
      Util/
      View/
    res/
    assets/
```

### Controller layer
Important screens/classes:
- `SplashActivity`
- `LoginActivity`
- `CadastrarActivity`
- `MapActivity`
- `CapturaActivity`
- `PerfilActivity`
- `PokedexActivity`
- `DetalhesPokedexActivity`
- `MapCapturasActivity`

### Model layer
Important domain classes include:
- `ControladoraFachadaSingleton`
- `Usuario`
- `Pokemon`
- `Tipo`
- `Aparecimento`
- `PokemonCapturado`

### Util layer
Contains database, random/time helpers and shared application utilities.

### View layer
Contains adapters and camera-related UI helpers such as `AdapterPokedex` and `CameraPreview`.

## 4. Runtime application flow

```text
APP START
   |
   v
SplashActivity
   |
   v
LoginActivity <-------- Logout from Profile
   |  \
   |   \-- Register --> CadastrarActivity
   |
   | successful authentication
   v
MapActivity
   |
   +--> GPS / LocationManager
   |
   +--> Player marker
   |
   +--> Periodic spawn algorithm
   |       |
   |       +--> calculate lat/lng bounding box
   |       +--> choose Pokémon by rarity/category
   |       +--> assign random coordinates
   |       +--> redraw map markers
   |
   +--> Tap nearby Pokémon marker
   |       |
   |       +--> calculate trainer-to-Pokémon distance
   |       +--> reject if outside interaction radius
   |       +--> otherwise open CapturaActivity
   |
   +--> Profile
   +--> Pokédex
   +--> Capture-history map

CapturaActivity
   |
   +--> Camera preview
   +--> Gyroscope listener
   +--> Pokémon image
   +--> Poké Ball touch/throw interaction
   +--> Capture success/failure
   |
   v
Usuario.capturar(Aparecimento)
   |
   +--> persist capture
   +--> update in-memory capture collection
   +--> return to map / continue game

PokedexActivity
   |
   +--> list all Pokémon
   +--> show captured count
   +--> open details only for captured species
   |
   v
DetalhesPokedexActivity
   |
   +--> species info
   +--> capture locations/history
   |
   v
MapCapturasActivity
```

## 5. Authentication flow

### Login
1. User enters username and password.
2. `LoginActivity` calls the facade login operation.
3. On success, open `MapActivity` and finish the login screen.
4. On failure, display an invalid-credentials message.

### Registration
1. User opens registration from login.
2. Collect name, username, password, confirmation and avatar gender.
3. Validate required fields and password confirmation.
4. Create the user through the facade.
5. Enter the map after successful registration.

### Modern implementation recommendation
For a new version, replace plaintext local password handling with proper authentication and hashed password storage. Never copy the legacy credential approach into production.

## 6. Player/location subsystem

### Inputs
- GPS/device location.
- Current player identity.
- Map state.

### Responsibilities
- Subscribe to location updates.
- Keep current player coordinates.
- Render the trainer marker.
- Center the map on first valid location.
- Trigger nearby-Pokémon spawning.

### Original behavior
The legacy `MapActivity` requests location updates and updates the trainer marker when location changes.

The original implementation computes a bounding box around the player using approximately a 0.3 km radius, then supplies min/max latitude and longitude to the spawn algorithm.

## 7. Pokémon spawn algorithm

### Inputs
```text
playerLatitude
playerLongitude
spawnRadius
pokemon pools by rarity
current time
random values
```

### Bounding box
1. Convert the chosen radius into a latitude delta.
2. Convert the radius into a longitude delta using the cosine of latitude.
3. Compute:
   - `minLat = lat - deltaLat`
   - `maxLat = lat + deltaLat`
   - `minLon = lon - deltaLon`
   - `maxLon = lon + deltaLon`

### Spawn selection
The original `ControladoraFachadaSingleton.sorteiaAparecimentos(...)` partitions Pokémon into categories:
- `C` = common
- `I` = uncommon
- `R` = rare
- `L` = legendary

It then creates an array of up to 10 `Aparecimento` objects, assigning a Pokémon and random coordinates inside the current bounding box.

The current legacy code uses a time/random heuristic to decide whether a legendary appears; it is intentionally game-like rather than scientifically meaningful.

### Better algorithm for a new app
Use a deterministic, configurable spawn service:

```text
SpawnScheduler
  -> read player location
  -> build spawn area
  -> select spawn table
  -> weighted random species selection
  -> enforce duplicate/spawn-distance rules
  -> generate spawn TTL
  -> persist only if persistence is required
  -> publish SpawnEvent
  -> Map UI renders markers
```

Recommended rarity weights example:
```text
Common    70%
Uncommon  20%
Rare       9%
Legendary  1%
```

Keep these as configuration, not hard-coded logic.

## 8. Spawn lifecycle

For a modern implementation each `Spawn` should have:
- `id`
- `pokemonId`
- `latitude`
- `longitude`
- `spawnedAt`
- `expiresAt`
- `isCaptured`

Flow:
```text
spawn created
   -> shown on map
   -> player moves / timer ticks
   -> spawn may refresh
   -> player taps marker
   -> validate distance + expiry
   -> open capture encounter
   -> mark captured or remove
```

The original README says Pokémon respawn every 3 minutes; the code uses a periodic background thread with a one-minute interval while the actual respawn loop redraws the map after each cycle.

## 9. Map interaction algorithm

### Marker click
1. Determine whether clicked marker is the player marker.
2. Retrieve the associated `Aparecimento`.
3. Compute trainer-to-Pokémon distance.
4. Compare with interaction threshold.
5. If too far, show distance feedback.
6. If close enough:
   - pause map audio;
   - package the `Aparecimento` object;
   - launch `CapturaActivity`;
   - remove the marker from the map.

### Data structure
The original app maintains a mapping:
```text
Map<Marker, Aparecimento>
```
so map markers can be translated back into game-domain spawn objects.

Modern equivalent:
```text
Map<MarkerId, SpawnId>
```
or let map markers carry a stable spawn ID in their tag/metadata.

## 10. Capture encounter flow

`CapturaActivity` is the real-time mini-game.

### Startup
1. Open camera preview.
2. Start the gyroscope sensor listener.
3. Read the selected `Aparecimento` from the intent.
4. Resolve the Pokémon.
5. Display the Pokémon image/name.
6. Show whether the species is new or already known.
7. Start battle/capture audio.

### Sensor algorithm
The original implementation integrates gyroscope angular velocity over time to estimate movement:
```text
angularVelocity * 57.2958 * dt -> degrees
accumulate X/Y/Z
```
Then:
- Y movement affects Pokémon image X position.
- X movement affects Pokémon image Y position.
- Z movement rotates the Pokémon image.
- Small changes are filtered to reduce sensor noise.

### Touch/Poké Ball subsystem
The original class tracks:
```text
xStart, yStart
xEnd, yEnd
touchStartTime, touchEndTime
deltaX, deltaY
velocityX, velocityY
```
Use these values to calculate throw direction and throw speed.

### Modern capture algorithm
```text
onTouchDown
   -> record pointer + timestamp
onTouchUp
   -> compute dx, dy, duration
   -> compute velocity
   -> launch projectile
   -> update projectile physics
   -> collision/intersection test
   -> capture success probability
   -> resolve encounter
```

Keep capture rules independent from rendering.

## 11. Capture resolution

A clean design separates the rules from Android UI:

```text
CaptureEngine
  input:
    pokemon
    throwPower
    throwAccuracy
    playerLevel
    encounterModifiers
  output:
    CAPTURED
    ESCAPED
    MISSED
```

On capture:
```text
CaptureEngine -> CaptureRepository
                -> update player collection
                -> store timestamp
                -> store coordinates
                -> emit CaptureCompleted
```

The legacy `Usuario.capturar(...)` stores:
- username
- Pokémon ID
- capture timestamp
- latitude
- longitude

and also updates the in-memory map of captured Pokémon.

## 12. Local data model

The legacy database contains a Pokémon catalog and user/capture data. At minimum the conceptual schema should be:

```text
Pokemon
--------
id
name
category
photoAsset
iconAsset

Type
--------
id
name

PokemonType
--------
pokemonId
typeId

User
--------
userId
login
passwordHash
name
avatarGender
avatarAsset
createdAt

Capture
--------
id
userId
pokemonId
latitude
longitude
capturedAt
```

Optional new tables:
```text
Spawn
Inventory
Item
Achievement
Session
Settings
```

## 13. Repository/service architecture to build

Do not keep all logic inside Activities.

Recommended structure:
```text
ui/
  splash/
  auth/
  map/
  capture/
  profile/
  pokedex/
  capturehistory/

domain/
  model/
  usecase/
    LoginUser
    RegisterUser
    GenerateSpawns
    StartEncounter
    ResolveCapture
    ListPokedex
    GetCaptureHistory

data/
  local/
  repository/
  mapper/

services/
  LocationService
  SpawnScheduler
  SensorService
  AudioService

core/
  Result
  TimeProvider
  RandomProvider
  GeoUtils
```

The goal is to make the gameplay rules testable without Android UI.

## 14. Step-by-step integration plan

### Phase 1 — Baseline
```text
clone repository
-> open in Android Studio
-> resolve SDK/dependencies
-> build
-> install
-> document baseline issues
```

### Phase 2 — Modern shell
Create a clean modern Android application shell.

Integrate in this order:
1. App theme and navigation.
2. Splash/startup.
3. Local persistence.
4. Authentication.
5. Map.
6. Location permissions/service.
7. Spawn engine.
8. Map marker rendering.
9. Capture encounter.
10. Camera integration.
11. Gyroscope integration.
12. Throw physics/capture engine.
13. Trainer profile.
14. Pokédex.
15. Capture-history map.
16. Audio and polish.

Do not integrate camera, GPS, sensors and persistence simultaneously. Each layer should have a working checkpoint.

## 15. Recommended build checkpoints

### Checkpoint A
```text
App launches
Splash -> Login
```

### Checkpoint B
```text
Login/Register -> Map
```

### Checkpoint C
```text
Map + location permission + player marker
```

### Checkpoint D
```text
Map + generated local spawns
```

### Checkpoint E
```text
Marker click + distance validation
```

### Checkpoint F
```text
Capture screen + camera
```

### Checkpoint G
```text
Gyroscope + touch throw
```

### Checkpoint H
```text
Successful capture -> persistence
```

### Checkpoint I
```text
Pokédex + capture statistics
```

### Checkpoint J
```text
Capture-history locations + final polish
```

## 16. Permissions and platform integration

The legacy manifest declares camera, GPS/network and gyroscope requirements and the Google Maps API key.

For a modern rebuild, request runtime permissions only when needed:
- Location when opening the map.
- Camera when starting an encounter.

Check hardware support at runtime rather than assuming every device has a gyroscope/camera capability.

Never commit a real production API key directly to source control. Use build-time/local configuration and restrict the key by package/application identity and API.

## 17. Map service integration

The original project uses Google Maps Android APIs and Google Play Services.

Modern flow:
```text
Map screen opens
-> initialize map SDK
-> verify map configuration
-> request location permission
-> obtain last/current location
-> move camera to player
-> subscribe to location changes
-> send location into SpawnScheduler
-> render spawn markers
```

## 18. Sensor integration

### Gyroscope
```text
Capture screen visible
-> register sensor listener
-> receive samples
-> filter noise
-> integrate angular velocity
-> update game-world orientation
-> unregister listener on pause
```

### Camera
```text
Capture screen visible
-> request camera permission
-> initialize camera preview
-> overlay game objects
-> release camera resources when leaving encounter
```

Use the modern CameraX stack for a rebuild rather than copying the legacy `android.hardware.Camera` implementation.

## 19. Navigation state machine

Use explicit navigation states:

```text
STARTUP
  -> AUTHENTICATED? -> MAP
  -> NOT_AUTHENTICATED -> LOGIN

LOGIN
  -> LOGIN_SUCCESS -> MAP
  -> REGISTER -> REGISTER

REGISTER
  -> SUCCESS -> MAP
  -> BACK -> LOGIN

MAP
  -> POKEMON_TAP -> ENCOUNTER
  -> PROFILE -> PROFILE
  -> POKEDEX -> POKEDEX
  -> CAPTURE_HISTORY -> CAPTURE_HISTORY
  -> LOGOUT -> LOGIN

ENCOUNTER
  -> CAPTURE_SUCCESS -> MAP
  -> ESCAPED/MISSED -> MAP
```

## 20. Error handling strategy

Every integration must define failures explicitly.

Examples:
```text
Location disabled
-> show enable-location message

Location permission denied
-> show limited/manual mode

Map unavailable
-> show retry state

Camera permission denied
-> cannot start AR encounter

Gyroscope missing
-> use touch-only fallback

Database error
-> fail safely and do not lose UI state

Spawn expired
-> remove marker and show encounter-expired message
```

## 21. Testing algorithm

### Unit tests
Test:
- Geo distance calculations.
- Bounding-box calculations.
- Spawn weighting.
- Spawn duplicate prevention.
- Spawn expiration.
- Throw velocity.
- Capture probability.
- Pokédex captured counts.

### Integration tests
Test:
```text
login -> map
register -> map
map -> encounter
encounter -> capture -> database
map -> pokedex
pokedex -> details
profile -> logout
```

### Device tests
Test on:
- emulator with mock location;
- physical device with GPS;
- device with gyroscope;
- device without gyroscope;
- denied camera/location permissions;
- screen rotation/lifecycle transitions;
- background/foreground transitions.

## 22. Performance rules

Never run heavy work on the main UI thread.

Separate:
```text
UI thread
location callback
spawn calculation
database operations
sensor processing
image loading
```

The legacy code uses a background `Thread` for the periodic spawn loop. A modern implementation should use lifecycle-aware background scheduling/coroutines rather than a raw infinite thread.

## 23. Security and production hardening

The original project is educational. Before production:
- remove embedded secrets/API keys;
- hash passwords;
- validate all external inputs;
- do not trust client-generated capture results in a multiplayer game;
- add authentication/session expiry;
- add server-side anti-cheat validation if competitive gameplay is introduced;
- protect map/service keys;
- avoid logging sensitive user data.

## 24. Suggested modern technology stack

For a fresh rebuild while preserving the gameplay concept:

```text
Kotlin
Jetpack Compose or XML UI
Navigation
ViewModel
Room
Coroutines / Flow
DataStore
CameraX
Google Maps SDK or another map provider
Fused Location Provider
SensorManager / sensor APIs
WorkManager where background scheduling is required
```

The original Java architecture should be treated as a functional blueprint; do not mechanically port every class 1:1.

## 25. Feature-by-feature implementation order

### 1. Authentication
Deliver:
- login screen
- register screen
- session state

### 2. Player profile
Deliver:
- username
- display name
- avatar
- basic capture statistics

### 3. Map
Deliver:
- permissions
- player position
- map camera
- player marker

### 4. Spawn engine
Deliver:
- spawn configuration
- weighted selection
- bounding-box coordinate generation
- spawn refresh
- marker state

### 5. Encounter validation
Deliver:
- tap marker
- calculate distance
- enforce interaction radius
- open encounter

### 6. Capture scene
Deliver:
- camera background
- Pokémon overlay
- touch throw
- basic collision

### 7. Gyroscope
Deliver:
- orientation updates
- smoothing
- fallback when unavailable

### 8. Persistence
Deliver:
- save captures
- save coordinates/time
- restore collection on app restart

### 9. Pokédex
Deliver:
- 151-species catalog
- captured/uncaptured state
- detail view

### 10. History map
Deliver:
- captured Pokémon markers
- species selection/filter

## 26. Definition of done

A feature is complete when:
1. UI works.
2. Domain logic is separated from UI.
3. Data persists correctly.
4. Permission/lifecycle errors are handled.
5. Unit tests cover core algorithms.
6. Device/emulator behavior is verified.
7. Logs contain no secrets.
8. The feature survives background/foreground transitions.
9. The feature does not block the main thread.
10. Documentation is updated.

## 27. Practical first commands

```bash
# Clone
git clone https://github.com/lucasvegi/PokemonGoCloneOffline.git
cd PokemonGoCloneOffline

# Verify repository
 git remote -v
 git branch -a
 git log --oneline -5

# Open in Android Studio
# Then run Gradle sync and inspect build errors.
```

## 28. Important legacy-source notes

- The README says the project is primarily educational and was not intended as a commercial end-user product.
- The README identifies the original feature scope rather than a full Pokémon GO implementation.
- The manifest contains an API key; do not reuse that key for a new production application.
- The legacy app uses deprecated/old Android APIs. For a new build, preserve the gameplay behavior but replace outdated platform components.

## 29. Implementation principle

Build the project as a sequence of vertical slices rather than integrating every system at once:

```text
Auth slice
  -> Map slice
    -> Spawn slice
      -> Encounter slice
        -> Capture slice
          -> Persistence slice
            -> Pokédex slice
              -> History slice
                -> Polish
```

Every slice should compile, run and be testable before the next integration begins.


---

# 30. Pokémon Dawn & Dusk — Master Product Blueprint

## 30.1 Game vision

**Pokémon Dawn & Dusk** is a location-based mobile adventure in which the player explores the real world, discovers creatures around their current location, enters encounter scenes, performs an interactive capture, builds a collection, and reviews exploration history.

The core loop is:

```text
OPEN APP
  ↓
AUTH / LOAD PLAYER
  ↓
GET LOCATION
  ↓
GENERATE NEARBY CREATURES
  ↓
EXPLORE + WALK
  ↓
DISCOVER CREATURE
  ↓
DISTANCE VALIDATION
  ↓
ENCOUNTER
  ↓
AIM / THROW / SENSOR INTERACTION
  ↓
CAPTURE RESULT
  ↓
SAVE CAPTURE + LOCATION + TIME
  ↓
UPDATE COLLECTION / CODEX
  ↓
RETURN TO MAP
  ↓
CONTINUE EXPLORING
```

The design goal is to preserve the important technical lessons from the source repository while restructuring them into a maintainable modern application.

## 30.2 Product modules

```text
Pokémon Dawn & Dusk
│
├── Startup
│   └── Splash / bootstrap / session restore
│
├── Account
│   ├── Login
│   ├── Registration
│   └── Logout
│
├── Player
│   ├── Profile
│   ├── Avatar
│   └── Statistics
│
├── Exploration
│   ├── Map
│   ├── GPS
│   ├── Player position
│   ├── Nearby spawn system
│   └── Marker interaction
│
├── Encounter
│   ├── Camera
│   ├── Sensor input
│   ├── Creature rendering
│   ├── Throw physics
│   └── Capture rules
│
├── Collection
│   ├── Codex
│   ├── Creature details
│   ├── Capture history
│   └── Capture map
│
├── Data
│   ├── Local database
│   ├── Repositories
│   └── Optional remote API
│
└── Platform services
    ├── Maps
    ├── Location
    ├── Camera
    ├── Sensors
    ├── Audio
    └── Background scheduling
```

## 30.3 Source-to-new-system mapping

Use the legacy repository as a reference map:

| Legacy component | Pokémon Dawn & Dusk replacement |
|---|---|
| `SplashActivity` | Startup/splash screen + session bootstrap |
| `LoginActivity` | Auth Login screen + ViewModel |
| `CadastrarActivity` | Registration screen + validation |
| `MapActivity` | Exploration map screen + map ViewModel |
| `CapturaActivity` | Encounter screen + Capture engine |
| `PerfilActivity` | Player profile screen |
| `PokedexActivity` | Codex screen |
| `DetalhesPokedexActivity` | Creature detail screen |
| `MapCapturasActivity` | Capture-history map |
| `ControladoraFachadaSingleton` | Use cases + repositories/services |
| `Usuario` | Player/User domain model |
| `Pokemon` | Creature species/domain model |
| `Aparecimento` | Spawn domain model |
| `PokemonCapturado` | Capture domain model |
| `BancoDadosSingleton` | Room database + DAOs |
| `CameraPreview` | CameraX preview layer |
| `LocationManager` logic | Fused location service |
| Raw background thread | Coroutine/lifecycle-aware scheduler |

## 30.4 End-to-end state machine

```text
                    ┌─────────────────────┐
                    │       STARTUP       │
                    └──────────┬──────────┘
                               │
                    restore session?
                      /               \
                    yes               no
                     │                 │
                     v                 v
                   MAP               LOGIN
                                      │
                           ┌──────────┴──────────┐
                           │                     │
                        success               register
                           │                     │
                           v                     v
                          MAP                REGISTER
                                                 │
                                               success
                                                 │
                                                 v
                                                MAP
                                                 │
                                     nearby creature tapped
                                                 │
                                                 v
                                           DISTANCE CHECK
                                          /               \
                                       fail              pass
                                        │                  │
                                   stay on map            v
                                                    ENCOUNTER
                                                   /    |    \
                                               miss  flee  captured
                                                 \      |      /
                                                  \     |     /
                                                   v    v    v
                                                         MAP
```

## 30.5 Initialization algorithm

```pseudo
function bootApp():
    initializeLogging()
    loadLocalConfiguration()
    initializeDatabase()
    loadStaticCreatureCatalog()
    restoreSession()

    if session.isAuthenticated:
        openMap()
    else:
        openLogin()
```

Do not request every permission at startup. Permissions should be contextual.

## 30.6 Authentication algorithm

```pseudo
function login(username, password):
    validateRequiredFields(username, password)
    account = authRepository.authenticate(username, password)

    if account == null:
        return INVALID_CREDENTIALS

    session.save(account.id)
    return SUCCESS
```

For registration:

```pseudo
function register(input):
    validateName(input.name)
    validateUsername(input.username)
    validatePassword(input.password)
    validatePasswordMatch(input.password, input.confirmPassword)

    if authRepository.usernameExists(input.username):
        return USERNAME_EXISTS

    account = createAccount(input)
    session.save(account.id)
    return SUCCESS
```

## 30.7 Permission algorithm

```pseudo
MAP SCREEN
   ↓
Is location permission granted?
   ├─ NO → explain why → request permission
   │        ├─ granted → start location
   │        └─ denied → limited map/manual state
   └─ YES → start location
```

Encounter:

```pseudo
ENCOUNTER SCREEN
   ↓
Is camera permission granted?
   ├─ NO → request permission
   │        ├─ granted → camera preview
   │        └─ denied → touch-only fallback/error state
   └─ YES → camera preview
```

## 30.8 Geolocation algorithm

```pseudo
function onLocationUpdate(location):
    currentLocation = location
    updatePlayerMarker(location)
    updateMapCameraIfNeeded(location)

    if shouldRefreshSpawns(location):
        spawnScheduler.refresh(location)
```

Use a movement threshold plus a timer to avoid unnecessary refreshes.

Recommended rule:

```text
refresh if:
    elapsed >= spawnRefreshInterval
    OR movedDistance >= movementRefreshDistance
```

## 30.9 Spawn-area algorithm

For an Earth-based map, avoid assuming a constant longitude distance everywhere.

```pseudo
function buildSpawnBounds(latitude, longitude, radiusMeters):
    deltaLat = metersToLatitudeDegrees(radiusMeters)
    deltaLon = metersToLongitudeDegrees(radiusMeters, latitude)

    return Bounds(
        minLat = latitude - deltaLat,
        maxLat = latitude + deltaLat,
        minLon = longitude - deltaLon,
        maxLon = longitude + deltaLon
    )
```

For better fairness, production logic should ultimately use a geospatial distance function rather than treating the rectangle as a perfect circle.

## 30.10 Spawn-selection algorithm

Use weighted random selection instead of hard-coded time tricks.

Example:

```text
Common      70
Uncommon    20
Rare         9
Legendary    1
```

Algorithm:

```pseudo
function generateSpawns(playerLocation, config):
    bounds = buildSpawnBounds(
        playerLocation.lat,
        playerLocation.lon,
        config.spawnRadius
    )

    targetCount = randomInt(config.minSpawns, config.maxSpawns)
    spawns = []

    while spawns.size < targetCount:
        rarity = weightedChoice(config.rarityWeights)
        species = weightedChoice(speciesPool[rarity])
        coordinate = randomPoint(bounds)

        if tooCloseToAnotherSpawn(coordinate, spawns):
            continue

        spawns.add(
            Spawn(
                id = newId(),
                speciesId = species.id,
                latitude = coordinate.lat,
                longitude = coordinate.lon,
                spawnedAt = now(),
                expiresAt = now() + config.spawnTtl
            )
        )

    return spawns
```

## 30.11 Spawn validity algorithm

Every marker must be validated again before entering an encounter.

```pseudo
function canInteract(spawn, playerLocation):
    if spawn.isExpired(now()):
        return false

    distance = geoDistance(playerLocation, spawn.location)
    return distance <= config.interactionRadius
```

This prevents stale map markers from creating invalid encounters.

## 30.12 Map marker lifecycle

```text
Spawn created
   ↓
SpawnState.ACTIVE
   ↓
Map marker displayed
   ↓
 ┌───────────────┬────────────────┐
 │               │                │
 tapped        expired          captured elsewhere
 │               │                │
 ↓               ↓                ↓
validate      remove marker     remove marker
 │
 ├─ too far → remain active
 │
 └─ valid → start encounter
```

## 30.13 Encounter initialization

```pseudo
function startEncounter(spawnId):
    spawn = spawnRepository.get(spawnId)
    player = playerRepository.getCurrentPlayer()

    if !canInteract(spawn, player.location):
        return TOO_FAR

    if spawn.isExpired(now()):
        return EXPIRED

    encounter = Encounter(
        id = newId(),
        spawnId = spawn.id,
        speciesId = spawn.speciesId,
        startedAt = now()
    )

    return encounter
```

## 30.14 Sensor algorithm

The source repository demonstrates gyroscope-driven movement. For Dawn & Dusk, isolate sensor processing from the UI.

```pseudo
onSensorSample(x, y, z, dt):
    deltaX = x * RAD_TO_DEG * dt
    deltaY = y * RAD_TO_DEG * dt
    deltaZ = z * RAD_TO_DEG * dt

    orientation.x += deltaX
    orientation.y += deltaY
    orientation.z += deltaZ

    orientation = lowPassFilter(orientation)
    encounterRenderer.updateOrientation(orientation)
```

Add clamping so the rendered target cannot move permanently outside the playable frame.

## 30.15 Throw algorithm

```pseudo
onPointerDown(x, y, timestamp):
    touch.start = Point(x, y)
    touch.startTime = timestamp

onPointerUp(x, y, timestamp):
    dx = x - touch.start.x
    dy = y - touch.start.y
    duration = max(timestamp - touch.startTime, MIN_THROW_TIME)

    velocityX = dx / duration
    velocityY = dy / duration

    projectile = createThrow(
        origin = touch.start,
        velocity = Vector(velocityX, velocityY)
    )

    physics.launch(projectile)
```

## 30.16 Capture-resolution algorithm

Separate the rules from animation:

```pseudo
function resolveCapture(encounter, throwResult):
    accuracyScore = calculateAccuracy(throwResult)
    powerScore = calculatePower(throwResult)
    timingScore = calculateTiming(throwResult)

    baseChance = captureBaseChance(encounter.speciesId)
    bonus = accuracyBonus(accuracyScore)
         + powerBonus(powerScore)
         + timingBonus(timingScore)

    finalChance = clamp(baseChance + bonus, 0, 1)

    if random() < finalChance:
        return CAPTURED

    if random() < escapeChance(encounter):
        return ESCAPED

    return MISSED
```

Keep these rules configurable so game balancing can be tuned without rewriting rendering code.

## 30.17 Capture transaction

Capture should be treated as a single logical transaction:

```text
Resolve CAPTURED
      ↓
Begin transaction
      ↓
Create Capture record
      ↓
Mark/consume Spawn
      ↓
Update player collection
      ↓
Commit
      ↓
Publish CaptureCompleted
      ↓
Refresh map + codex + stats
```

If any required step fails, do not claim the capture succeeded.

## 30.18 Local persistence flow

Recommended Room entities:

```text
PlayerEntity
CreatureEntity
CreatureTypeEntity
SpawnEntity
CaptureEntity
SessionEntity
```

Relationships:

```text
Player 1 ──── * Capture
Creature 1 ── * Capture
Creature * ─── * Type
Player 1 ──── * Spawn (only if player-specific persistence is needed)
```

Capture record:

```text
id
playerId
creatureId
latitude
longitude
capturedAt
encounterId
```

## 30.19 Optional online architecture

The source project is primarily local/didactic. Dawn & Dusk can remain offline-first while adding a server later.

Recommended evolution:

```text
Mobile App
   │
   ├── Local Room DB
   │       └── source of fast gameplay reads
   │
   └── Repository
           └── optional Sync layer
                    │
                    v
                REST/JSON API
                    │
                    v
                 Backend DB
```

For online mode, server-side validation is required for authoritative events such as captures, inventory changes and competitive rewards.

## 30.20 Event-driven integration

Use domain events to decouple features:

```text
LocationUpdated
SpawnRefreshRequested
SpawnCreated
SpawnExpired
EncounterStarted
ThrowReleased
CaptureResolved
CaptureCompleted
PlayerLoggedOut
```

Example:

```text
LocationService
   → LocationUpdated
      → SpawnScheduler
         → SpawnCreated
            → MapViewModel
               → MapMarkerRenderer
```

Capture:

```text
CaptureEngine
   → CaptureCompleted
      ├→ CaptureRepository
      ├→ PlayerStats
      ├→ CodexViewModel
      └→ MapViewModel
```

## 30.21 Screen-by-screen build workflow

### Screen 1 — Splash

Purpose:
- initialize app state;
- restore session;
- initialize database/catalog.

Exit conditions:
```text
authenticated → Map
not authenticated → Login
```

### Screen 2 — Login

Integrate:
- username input;
- password input;
- login use case;
- validation;
- navigation to register.

### Screen 3 — Register

Integrate:
- name;
- username;
- password;
- confirmation;
- avatar/gender selection;
- account creation.

### Screen 4 — Main Map

Integrate in this exact order:

```text
Map SDK
→ location permission
→ player location
→ player marker
→ camera movement
→ spawn scheduler
→ spawn markers
→ marker tap
→ distance validation
```

### Screen 5 — Encounter

Integrate in this exact order:

```text
static creature image
→ camera preview
→ touch input
→ projectile motion
→ collision
→ capture result
→ persistence
→ navigation back
```

Only after the touch version works should gyroscope motion be added.

### Screen 6 — Profile

Integrate:
- player information;
- species captured count;
- total capture count;
- exploration stats;
- logout.

### Screen 7 — Codex

Integrate:
- complete species catalog;
- captured/unseen status;
- list/grid;
- progress counter.

### Screen 8 — Creature Details

Integrate:
- species data;
- capture count;
- types;
- first/last capture;
- capture locations.

### Screen 9 — Capture History Map

Integrate:
- historical capture markers;
- creature filter;
- marker detail.

## 30.22 Integration order — exact sequence

Follow this sequence and do not skip ahead:

```text
01. Clone source reference
02. Create new Dawn & Dusk project
03. Establish version control
04. Add base app architecture
05. Add theme/navigation
06. Add local database
07. Add static creature catalog
08. Add authentication/session
09. Build Map screen
10. Add location permissions
11. Add player marker
12. Add GeoUtils
13. Add SpawnEngine
14. Add spawn markers
15. Add distance validation
16. Add Encounter screen
17. Add touch-only capture
18. Add CaptureEngine
19. Save Capture records
20. Add CameraX
21. Add gyroscope
22. Add sensor fallback
23. Add Profile
24. Add Codex
25. Add creature details
26. Add history map
27. Add audio
28. Add background refresh
29. Add analytics/crash logging if required
30. Add full testing
31. Security hardening
32. Performance pass
33. Release build
```

## 30.23 Definition of each integration checkpoint

### Checkpoint 1 — Application boot

Success:
```text
App installs → opens → splash → login
```

### Checkpoint 2 — Account

Success:
```text
register → session created → map
login → session restored → map
logout → login
```

### Checkpoint 3 — Location

Success:
```text
permission → current position → player marker
```

### Checkpoint 4 — Spawning

Success:
```text
player position → bounded region → generated spawns → map markers
```

### Checkpoint 5 — Encounter gate

Success:
```text
nearby marker → distance check → encounter
far marker → feedback → remain on map
```

### Checkpoint 6 — Capture

Success:
```text
encounter → throw → result → save capture
```

### Checkpoint 7 — Sensor/AR

Success:
```text
camera → creature overlay → gyroscope movement → touch throw
```

### Checkpoint 8 — Collection

Success:
```text
capture → codex updated → profile count updated → history marker saved
```

## 30.24 Testing matrix

| Feature | Unit | Integration | Device |
|---|---:|---:|---:|
| Login validation | ✓ | ✓ | ✓ |
| Registration | ✓ | ✓ | ✓ |
| Geo distance | ✓ | ✓ | ✓ |
| Spawn weights | ✓ | ✓ | ✓ |
| Spawn expiry | ✓ | ✓ | ✓ |
| Marker interaction |  | ✓ | ✓ |
| Throw physics | ✓ | ✓ | ✓ |
| Capture probability | ✓ | ✓ | ✓ |
| Camera |  | ✓ | ✓ |
| Gyroscope | ✓ | ✓ | ✓ |
| Room persistence | ✓ | ✓ | ✓ |
| Codex counting | ✓ | ✓ | ✓ |
| Lifecycle recovery |  | ✓ | ✓ |

## 30.25 Failure-mode matrix

```text
GPS unavailable
→ show map without live player movement

Location permission denied
→ explain limitation + retry option

Camera permission denied
→ touch-only encounter or controlled fallback

No gyroscope
→ touch-only encounter

Map SDK unavailable
→ show retry/error state

Database unavailable
→ do not silently discard capture

Spawn expired
→ remove marker + show expired message

Player moved too far
→ block encounter + show distance

App backgrounded during encounter
→ pause sensors/camera safely and restore state
```

## 30.26 Performance rules

```text
UI thread:
  rendering, navigation, user events

Background:
  database queries
  spawn calculations
  image processing
  sync operations

Sensor callback:
  lightweight math only

Location callback:
  update state, do not run expensive work inline
```

Use lifecycle-aware collectors and cancellation. Never keep an infinite worker alive after the owning screen has been destroyed.

## 30.27 Security rules

Mandatory for Dawn & Dusk:

```text
NO plaintext passwords
NO committed production API keys
NO client-trusted competitive rewards
NO secrets in logs
NO sensitive account data in analytics
```

The old manifest contains a Google Maps key; do not reuse it for the new game. Create a new restricted key and manage it outside source control.

## 30.28 Asset and branding rule

Treat the GitHub repository as a technical reference. For a distributable game, use appropriately licensed/original:

```text
creature art
icons
sounds
music
map styling
fonts
logos
UI assets
```

The product name for this project is **Pokémon Dawn & Dusk**. Before public commercial release, review branding/trademark and asset rights for any Pokémon-related names or content.

## 30.29 Git workflow

```bash
git clone https://github.com/lucasvegi/PokemonGoCloneOffline.git
cd PokemonGoCloneOffline

git remote -v
git branch -a
git log --oneline -5
```

Create the new project in its own repository:

```bash
mkdir pokemon-dawn-dusk
cd pokemon-dawn-dusk
git init

git add .
git commit -m "Initialize Pokemon Dawn & Dusk"
```

Recommended branches:

```text
main
  ├── develop
  ├── feature/auth
  ├── feature/map
  ├── feature/spawn-engine
  ├── feature/encounter
  ├── feature/capture
  ├── feature/codex
  └── feature/history-map
```

## 30.30 First development session

Do exactly this on Day 1:

```text
1. Clone/reference the old repository.
2. Open it and record its legacy build requirements.
3. Create a fresh Android project named Pokémon Dawn & Dusk.
4. Configure Kotlin + modern Gradle/Android tooling.
5. Create package layers: ui/domain/data/services/core.
6. Add navigation.
7. Add Room.
8. Add a static Creature catalog with a small test dataset.
9. Implement Login/Register using local mock data first.
10. Build the Map screen with a fake player coordinate.
11. Render one fake spawn marker.
12. Verify navigation from marker → encounter.
13. Commit the working slice.
```

## 30.31 First playable vertical slice

The first playable build should intentionally be small:

```text
Login
  ↓
Map
  ↓
One nearby creature
  ↓
Tap marker
  ↓
Distance validation
  ↓
Encounter
  ↓
Touch throw
  ↓
Capture success/failure
  ↓
Save capture
  ↓
Codex shows 1 captured
```

Only after this loop works end-to-end should the project expand to the full creature catalog, richer spawning, sensors, AR polish, history maps and online functionality.

## 30.32 Final architecture target

```text
                     ┌───────────────────┐
                     │     UI LAYER      │
                     │ Compose / Views   │
                     └─────────┬─────────┘
                               │
                         ViewModels
                               │
                     ┌─────────▼─────────┐
                     │   DOMAIN LAYER    │
                     │ Use Cases/Rules   │
                     └─────────┬─────────┘
                               │
               ┌───────────────┼────────────────┐
               │               │                │
        ┌──────▼─────┐  ┌─────▼──────┐  ┌─────▼─────┐
        │  LOCATION  │  │  CAPTURE   │  │   SPAWN   │
        │  SERVICE   │  │   ENGINE   │  │  ENGINE   │
        └──────┬─────┘  └─────┬──────┘  └─────┬─────┘
               │              │                │
               └──────────────┼────────────────┘
                              │
                     ┌────────▼────────┐
                     │ DATA / REPO     │
                     │ Room + optional │
                     │ remote sync     │
                     └─────────────────┘
```

## 30.33 Golden rule for the project

**Do not port the old repository line-by-line. Port its behavior and lessons.**

Preserve:

```text
location exploration
nearby creature spawning
marker interaction
AR-style encounter
sensor input
throw mechanics
capture history
codex
profile
```

Replace:

```text
legacy Android APIs
raw Activity-heavy architecture
plaintext credential patterns
embedded API keys
raw infinite threads
deprecated camera APIs
hard-coded balancing logic
```

That produces a maintainable **Pokémon Dawn & Dusk** foundation rather than a fragile copy of a 2017 classroom project.
