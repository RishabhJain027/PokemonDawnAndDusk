# Pokémon Dawn & Dusk — Master Engineering, Architecture & Game Design Document

```
========================================================================================
                          POKÉMON DAWN & DUSK (v1.0.0)
                Master Architecture, Gameplay Engineering & Design Document
========================================================================================
Project Creator & Lead Systems Architect : Rishabh Jain (@RishabhJain027)
Official Git Repository                  : https://github.com/RishabhJain027/PokemonDawnAndDusk
Live Web Showcase & Simulator            : https://rishabhjain027.github.io/PokemonDawnAndDusk/
Target Platform                          : Android 7.0+ (API Level 24 to 34) & Modern Web
Architecture Baseline                    : Clean Architecture + MVVM + Jetpack Compose + Room
========================================================================================
```

---

## 1. Executive Summary & Vision

**Pokémon Dawn & Dusk** is a location-based Augmented Reality (AR) mobile gaming platform engineered from scratch. The game merges real-world geolocation data with responsive physics-based capture mechanics, high-performance vector map rendering, gyroscope orientation tracking, and an offline-first relational database housing all 151 Generation 1 Pokémon.

The game is built to deliver a modern, resilient mobile experience using declarative UI, immutable unidirectional data flow (UDF), cryptographic credential protection, and lifecycle-safe reactive streams.

---

## 2. Master Game Loop & State Machine

The player's journey flows continuously through six decoupled lifecycle states:

```text
 ┌─────────────────────────────────────────────────────────────────────────┐
 │                               1. BOOT                                   │
 │  SplashScreen -> Check Local Session -> Authenticated?                  │
 └────────────────────┬───────────────────────────────┬────────────────────┘
                      │ No                            │ Yes
                      ▼                               ▼
 ┌─────────────────────────────────────────┐   ┌───────────────────────────┐
 │               2. AUTH                   │   │         3. MAP            │
 │ Login / Register -> Salted SHA-256 Auth ├──>│ GPS Coordinates Acquired  │
 └─────────────────────────────────────────┘   │ Dynamic Spawns Generated  │
                                               │ Radar Ping & Canvas Pins  │
                                               └─────────────┬─────────────┘
                                                             │ Marker Click
                                                             │ Distance < 60m
                                                             ▼
 ┌─────────────────────────────────────────┐   ┌───────────────────────────┐
 │               5. CODEX                  │   │       4. ENCOUNTER        │
 │ 151 Pokédex Grid -> Status & Attributes │<──┤ AR Camera / Dynamic Sky   │
 │ Historic GPS Map of All Catches         │   │ Gyroscope Tracking & Pan  │
 └─────────────────────────────────────────┘   │ Flick Projectile Physics  │
                                               │ Capture Probability Check │
                                               └─────────────┬─────────────┘
                                                             │
                                                             ▼
                                               ┌───────────────────────────┐
                                               │       6. PROFILE          │
                                               │ Level, XP, Total Catches  │
                                               └───────────────────────────┘
```

---

## 3. Full Technology Stack Specifications

| Architectural Tier | Selected Technology | Technical Rationale |
|---|---|---|
| **Language** | Kotlin 1.9.24 | 100% type-safe, null-safe, coroutine-native |
| **UI Framework** | Jetpack Compose + Material 3 | Declarative, hardware-accelerated, reactive state rendering |
| **Architecture** | Clean Architecture (5 Layers) | UI $\to$ Domain $\to$ Data $\to$ Services $\to$ Core separation |
| **Reactive State** | Kotlin Coroutines & `StateFlow` | Lifecycle-aware, cold & hot stream subscriptions |
| **Local Database** | Room Database 2.6.1 (SQLite ORM) | Structured relational schema, asynchronous DAOs, type converters |
| **Geolocation** | Android `LocationManager` + Fused GPS | High-accuracy real-world coordinates with simulated Joystick fallback |
| **Sensor Engine** | `SensorManager` (`TYPE_ROTATION_VECTOR`) | Low-pass exponential smoothing filter for jitter-free AR tracking |
| **Audio Engine** | Custom PCM `AudioTrack` Synthesizer | Direct synthesis of game tones without external heavy asset dependencies |
| **Security** | `SecureRandom` + `SHA-256` | Salted cryptographic password hashing |
| **Build System** | Gradle 8.7 + Kotlin DSL (`.kts`) | Fast, incremental, cacheable compilation |

---

## 4. Deep-Dive Subsystem Architecture

```text
com.dawnanddusk/
│
├── app/
│   ├── DawnAndDuskApp.kt          # Global Application container & Service Locator
│   ├── MainActivity.kt            # Single Activity hosting Compose NavHost
│   └── navigation/
│       └── NavGraph.kt            # Type-safe Compose navigation routes & backstack
│
├── core/
│   ├── GeoUtils.kt                # Haversine distance, bounding box, radius validations
│   ├── SecurityUtils.kt           # Salted SHA-256 hashing, input sanitization
│   ├── AudioService.kt            # Synthesized audio sound effects & haptic feedback
│   └── Result.kt                  # Domain result wrapper (Success, Error, Loading)
│
├── domain/
│   ├── model/
│   │   ├── Creature.kt            # Pokémon species, types, base stats, catch/flee rates
│   │   ├── Enums.kt               # CreatureType and Rarity definitions
│   │   ├── Player.kt              # Trainer profile, level, XP, coordinates
│   │   ├── Spawn.kt               # Active wild spawn with TTL and coordinates
│   │   ├── Capture.kt             # Historic capture record with GPS tags
│   │   ├── Encounter.kt           # Real-time capture session state
│   │   └── ThrowResult.kt         # Projectile velocity, angle, and timing scores
│   ├── engine/
│   │   ├── SpawnEngine.kt         # Spatial bounding-box math, rarity distribution, proximity checks
│   │   └── CaptureEngine.kt       # Trajectory physics, accuracy bonus, catch probability
│   └── usecase/
│       ├── LoginUserUseCase.kt
│       ├── RegisterUserUseCase.kt
│       ├── GenerateSpawnsUseCase.kt
│       ├── ValidateEncounterUseCase.kt
│       ├── ResolveCaptureUseCase.kt
│       ├── GetPokedexUseCase.kt
│       ├── GetCaptureHistoryUseCase.kt
│       └── GetProfileStatsUseCase.kt
│
├── data/
│   ├── local/
│   │   ├── entity/
│   │   │   ├── PlayerEntity.kt
│   │   │   ├── CreatureEntity.kt
│   │   │   ├── SpawnEntity.kt
│   │   │   ├── CaptureEntity.kt
│   │   │   └── SessionEntity.kt
│   │   ├── dao/
│   │   │   ├── PlayerDao.kt
│   │   │   ├── CreatureDao.kt
│   │   │   ├── SpawnDao.kt
│   │   │   ├── CaptureDao.kt
│   │   │   └── SessionDao.kt
│   │   ├── database/
│   │   │   └── AppDatabase.kt     # Room database instance with pre-seeding callback
│   │   └── catalog/
│   │       └── StaticCreatureCatalog.kt # Complete Generation 1 (151 Pokémon) catalog
│   └── repository/
│       ├── AuthRepository.kt / AuthRepositoryImpl.kt
│       ├── CreatureRepository.kt / CreatureRepositoryImpl.kt
│       ├── SpawnRepository.kt / SpawnRepositoryImpl.kt
│       ├── CaptureRepository.kt / CaptureRepositoryImpl.kt
│       └── SessionRepository.kt / SessionRepositoryImpl.kt
│
├── services/
│   ├── LocationService.kt         # Dual-mode GPS tracking + Virtual joystick movement
│   └── SensorService.kt           # Gyroscope & rotation tracker with low-pass smoothing
│
└── ui/
    ├── theme/
    │   └── Theme.kt               # Custom Dawn Gold & Dusk Midnight palette
    ├── splash/
    │   └── SplashScreen.kt        # Animated emblem bootloader & session restore
    ├── auth/
    │   └── AuthScreens.kt         # Login, Registration & Trainer Avatar picker
    ├── map/
    │   └── MapScreen.kt           # Interactive Canvas radar map, player marker, spawn pins
    ├── capture/
    │   └── EncounterScreen.kt     # AR gyro aiming, contracting timing ring, flick physics
    ├── pokedex/
    │   └── PokedexScreens.kt      # 151 Pokédex grid, search, type filters, creature stat details
    ├── history/
    │   └── CaptureHistoryScreen.kt# GPS capture log with radar location mapping
    └── profile/
        └── ProfileScreen.kt       # Trainer ID card, level badge, capture stats, logout
```

---

## 5. Mathematical Formulations & Core Algorithms

### 5.1 Geodesic Bounding Box & Haversine Distance (`GeoUtils`)

To generate random spawns evenly across the globe regardless of player latitude:

$$\Delta\text{Lat} = \left(\frac{R_\text{spawn}}{R_\text{Earth}}\right) \times \left(\frac{180}{\pi}\right)$$

$$\Delta\text{Lon} = \left(\frac{R_\text{spawn}}{R_\text{Earth} \times \cos(\text{radians}(\text{Lat}))}\right) \times \left(\frac{180}{\pi}\right)$$

Where $R_\text{Earth} = 6,371,000\text{ m}$ and $R_\text{spawn} = 300\text{ m}$.

The great-circle distance between trainer $(lat_1, lon_1)$ and spawn $(lat_2, lon_2)$ is computed via:

$$a = \sin^2\left(\frac{lat_2 - lat_1}{2}\right) + \cos(lat_1)\cos(lat_2)\sin^2\left(\frac{lon_2 - lon_1}{2}\right)$$

$$d = 2 \cdot R_\text{Earth} \cdot \text{atan2}\left(\sqrt{a}, \sqrt{1 - a}\right)$$

---

### 5.2 Calibrated Weighted Spawn Distribution (`SpawnEngine`)

Wild spawns are refreshed periodically within the 300m radius using a weighted probability table:

```text
┌──────────────┬─────────────┬─────────────────────────────────────────────────┐
│ Rarity Tier  │ Probability │ Representative Species                          │
├──────────────┼─────────────┼─────────────────────────────────────────────────┤
│ Common (C)   │    70%      │ Pidgey, Rattata, Bulbasaur, Charmander, Squirtle│
│ Uncommon (I) │    20%      │ Pikachu, Abra, Eevee, Haunter, Ivysaur          │
│ Rare (R)     │     9%      │ Gengar, Dragonite, Snorlax, Gyarados, Lapras    │
│ Legendary (L)│     1%      │ Articuno, Zapdos, Moltres, Mewtwo, Mew          │
└──────────────┴─────────────┴─────────────────────────────────────────────────┘
```

**Proximity Exclusion Rule:** Any generated spawn candidate within $25\text{ meters}$ of an existing active spawn is discarded and re-rolled to avoid overlapping markers.

---

### 5.3 Gyroscope Noise Smoothing (`SensorService`)

To convert raw hardware sensor samples into fluid AR viewport movement without jitter:

$$\text{Pitch}_t = (1 - \alpha)\text{Pitch}_{t-1} + \alpha \cdot \text{RawPitch}_t$$

$$\text{Roll}_t = (1 - \alpha)\text{Roll}_{t-1} + \alpha \cdot \text{RawRoll}_t$$

Where $\alpha = 0.20$ (exponential moving average) and angles are bounded within $[-80^\circ, +80^\circ]$.

---

### 5.4 Flick Trajectory Physics & Capture Probability (`CaptureEngine`)

1. **Throw Velocity:**
   $$V_x = \frac{x_\text{end} - x_\text{start}}{\Delta t}, \quad V_y = \frac{y_\text{end} - y_\text{start}}{\Delta t}$$

2. **Hit Accuracy ($0.0 \le \text{Accuracy} \le 1.0$):**
   $$\text{Accuracy} = \max\left(0, 1 - \frac{\text{dist}(\text{LandingPoint}, \text{TargetCenter})}{R_\text{target} \times 1.8}\right)$$

3. **Timing Bonus ($0.0 \le \text{Timing} \le 1.0$):**
   $$\text{TimingBonus} = 1 - \text{RingScale} \quad (\text{Contracting ring: } 1.0 \to 0.2)$$

4. **Composite Catch Formula:**
   $$P(\text{Catch}) = \text{clamp}\left((\text{BaseRate} \times 0.65) + (\text{Accuracy} \times 0.25) + (\text{TimingBonus} \times 0.15), 0.05, 0.95\right)$$

---

## 6. Complete Generation 1 (151 Pokémon) Pokédex Catalog

The embedded catalog in [`StaticCreatureCatalog.kt`](file:///c:/Users/Asus/OneDrive/Desktop/Pok%C3%A9mon%20Dawn%20&%20Dusk/PokemonDawnAndDusk/app/src/main/java/com/dawnanddusk/data/local/catalog/StaticCreatureCatalog.kt) includes all 151 Generation 1 Pokémon with official attributes:

- **#001 Bulbasaur** to **#009 Blastoise** (Kanto Starters & Evolutions)
- **#010 Caterpie** to **#024 Arbok** (Early Forest & Route Species)
- **#025 Pikachu** & **#026 Raichu** (Electric Mouse Iconics)
- **#027 Sandshrew** to **#038 Ninetales** (Ground, Poison, Fairy, Fire Lines)
- **#039 Jigglypuff** to **#094 Gengar** (Normal, Grass, Psychic, Ghost Lines)
- **#095 Onix** to **#143 Snorlax** (Rock, Fighting, Water, Fossil Lines)
- **#144 Articuno, #145 Zapdos, #146 Moltres** (Legendary Bird Trio)
- **#147 Dratini, #148 Dragonair, #149 Dragonite** (Pseudo-Legendary Dragon Family)
- **#150 Mewtwo & #151 Mew** (Mythical & Genetic Legendaries)

---

## 7. Quality Assurance, Testing & Build Verification

The codebase includes an automated unit test suite in [`GameUnitTests.kt`](file:///c:/Users/Asus/OneDrive/Desktop/Pok%C3%A9mon%20Dawn%20&%20Dusk/PokemonDawnAndDusk/app/src/test/java/com/dawnanddusk/GameUnitTests.kt):

- `testGeoDistanceAndBoundingBox`: Validates geodesic math and bounding-box precision.
- `testSecurityPasswordHashingAndValidation`: Tests cryptographic salt generation, SHA-256 hashing, and username constraints.
- `testStaticCatalog151Creatures`: Verifies all 151 Pokémon entities, valid types, rarity classifications, and attributes.
- `testSpawnEngineGeneratesValidSpawns`: Verifies spawn boundary adherence and TTL expiration.
- `testCaptureEngineOutcomes`: Tests throw accuracy and outcome state machines.

---

## 8. Artifacts & Deliverables

1. **Android Application Package (APK):**
   - File: `PokemonDawnAndDusk-v1.0.apk`
   - Location: Project root & `web/PokemonDawnAndDusk.apk`
2. **Git Repository:**
   - Hosted at: [https://github.com/RishabhJain027/PokemonDawnAndDusk](https://github.com/RishabhJain027/PokemonDawnAndDusk)
3. **Web Showcase & Live Encounter Simulator:**
   - Deployed at: [https://rishabhjain027.github.io/PokemonDawnAndDusk/](https://rishabhjain027.github.io/PokemonDawnAndDusk/)

---

```
========================================================================================
             Designed, Engineered & Architected by Rishabh Jain (@RishabhJain027)
                         © 2026 Pokémon Dawn & Dusk. All rights reserved.
========================================================================================
```
