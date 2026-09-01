# Pokémon Dawn & Dusk — Master Architecture & Game Engineering Blueprint

**Project Name:** Pokémon Dawn & Dusk  
**Author & Lead Architect:** Rishabh Jain ([@RishabhJain027](https://github.com/RishabhJain027))  
**Repository:** [https://github.com/RishabhJain027/PokemonDawnAndDusk](https://github.com/RishabhJain027/PokemonDawnAndDusk)  
**Live Showcase Portal:** [https://rishabhjain027.github.io/PokemonDawnAndDusk/](https://rishabhjain027.github.io/PokemonDawnAndDusk/)  
**Version:** 1.0.0 (Production Architecture)

---

## 1. Project Vision & Overview

**Pokémon Dawn & Dusk** is a location-based Augmented Reality (AR) mobile adventure built entirely from the ground up on a modern Android reactive architecture. The game immerses the player into the real world, detecting device coordinates, dynamically generating wild Pokémon encounters across a 300-meter geographic radius, allowing interactive gesture-based Poké Ball throws with sensor-driven AR orientation tracking, maintaining persistent collection progress in a 151-species Pokédex, and visualizing historical exploration history on an interactive GPS map.

### 1.1 Core Gameplay Loop

```text
       ┌────────────────────────┐
       │   Launch Application   │
       └───────────┬────────────┘
                   │
                   ▼
       ┌────────────────────────┐
       │ Authenticate / Session │
       └───────────┬────────────┘
                   │
                   ▼
       ┌────────────────────────┐
       │  Acquire GPS Location  │
       └───────────┬────────────┘
                   │
                   ▼
       ┌────────────────────────┐
       │ Generate Nearby Spawns │
       │ (Weighted Rarity Pool) │
       └───────────┬────────────┘
                   │
                   ▼
       ┌────────────────────────┐
       │  Explore World & Radar │
       └───────────┬────────────┘
                   │
       Marker Tap  ▼
       ┌────────────────────────┐
       │ Distance Check (<60m)  │
       └───────────┬────────────┘
       Passed Check│
                   ▼
       ┌────────────────────────┐
       │  AR Encounter Scene    │
       │ (Gyro + Flick Physics) │
       └───────────┬────────────┘
                   │
        Catch / Flee
                   ▼
       ┌────────────────────────┐
       │ Persist Capture Record │
       │ (Coordinates + Time)   │
       └───────────┬────────────┘
                   │
                   ▼
       ┌────────────────────────┐
       │ Update Pokédex & Stats │
       └────────────────────────┘
```

---

## 2. Technical Stack & Engineering Philosophy

Designed with zero legacy dependencies, maximum testability, and high-performance mobile execution:

- **Language:** Kotlin 1.9.24+ (100% Type-safe & Coroutine-native)
- **UI Framework:** Jetpack Compose + Material 3 with customized Dawn & Dusk visual palette
- **Architecture Pattern:** Clean Architecture (UI / Domain / Data / Services / Core) + MVVM with reactive `StateFlow`
- **Local Persistence:** Room Database 2.6.1 (SQLite ORM) with asynchronous DAOs and Flow emissions
- **Concurrency:** Kotlin Coroutines + Structured Concurrency (`Dispatchers.IO`, `Dispatchers.Default`, `Dispatchers.Main`)
- **Hardware Integrations:**
  - **Geolocation:** Fused Location Provider + Android `LocationManager` with simulated Joystick walk mode for flexible indoor/outdoor testing
  - **Sensors:** `SensorManager` with `TYPE_ROTATION_VECTOR` / `TYPE_GYROSCOPE` and low-pass exponential smoothing
  - **Audio/Haptics:** Programmatic PCM tone synthesis + `VibrationEffect` haptic pulses
  - **Security:** Salted `SHA-256` password hashing with cryptographic `SecureRandom`

---

## 3. System Architecture Breakdown

```text
com.dawnanddusk/
├── app/
│   ├── DawnAndDuskApp.kt          # Global Application container & Service Locator
│   ├── MainActivity.kt            # Single Activity hosting Compose NavHost
│   └── navigation/
│       └── NavGraph.kt            # Type-safe Compose navigation routes & backstack
│
├── core/
│   ├── GeoUtils.kt                # Haversine distance, bounding box, radius validations
│   ├── SecurityUtils.kt           # Salted SHA-256 hashing, input validators
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

## 4. Mathematical Formulations & Algorithms

### 4.1 Geodesic Calculations & Bounding Box (`GeoUtils`)

To determine the geographic coordinate boundaries around a trainer for spawning without distortion:

1. **Latitude Delta:**
   $$\Delta\text{Lat} = \left(\frac{\text{radius}}{\text{EarthRadius}}\right) \times \left(\frac{180}{\pi}\right)$$
   *(where $\text{EarthRadius} = 6,371,000\text{ meters}$)*

2. **Longitude Delta (Corrected for Latitude):**
   $$\Delta\text{Lon} = \left(\frac{\text{radius}}{\text{EarthRadius} \times \cos(\text{rad}(\text{Lat}))}\right) \times \left(\frac{180}{\pi}\right)$$

3. **Haversine Distance Formula:**
   $$a = \sin^2\left(\frac{\Delta\text{Lat}}{2}\right) + \cos(\text{Lat}_1)\cos(\text{Lat}_2)\sin^2\left(\frac{\Delta\text{Lon}}{2}\right)$$
   $$d = 2 \cdot \text{EarthRadius} \cdot \text{atan2}\left(\sqrt{a}, \sqrt{1 - a}\right)$$

---

### 4.2 Weighted Spawn Generation (`SpawnEngine`)

Wild Pokémon spawn within a 300-meter radius of the trainer based on calibrated rarity pools:

- **Common (70% probability):** Pidgey, Rattata, Caterpie, Zubat, Magikarp, Bulbasaur, Charmander, Squirtle, Oddish, Poliwag, etc.
- **Uncommon (20% probability):** Pikachu, Abra, Eevee, Machoke, Haunter, Ivysaur, Charmeleon, Wartortle, etc.
- **Rare (9% probability):** Gengar, Dragonite, Snorlax, Gyarados, Lapras, Alakazam, Machamp, etc.
- **Legendary (1% probability):** Articuno, Zapdos, Moltres, Mewtwo, Mew.

#### Duplicate & Proximity Filtering:
Every candidate coordinate is tested against existing active spawns. If the distance is less than $25\text{ meters}$, the candidate point is rejected to ensure uniform world distribution.

---

### 4.3 Gyroscope Noise Filtering (`SensorService`)

To eliminate high-frequency sensor jitter while tracking device orientation:

$$\text{Orientation}_t = (1 - \alpha) \cdot \text{Orientation}_{t-1} + \alpha \cdot \text{RawSample}_t$$

*(Smoothing factor $\alpha = 0.20$ with angular clamping $[-80^\circ, +80^\circ]$)*

---

### 4.4 Projectile Physics & Capture Chance (`CaptureEngine`)

When a player drags and flicks a Poké Ball:

1. **Velocity Calculation:**
   $$V_x = \frac{\Delta X}{\Delta t}, \quad V_y = \frac{\Delta Y}{\Delta t}$$

2. **Hit Accuracy Score ($0.0 \to 1.0$):**
   $$\text{Accuracy} = \max\left(0, 1 - \frac{\text{Distance(Landing, Target)}}{\text{TargetRadius} \times 1.8}\right)$$

3. **Timing Bonus ($0.0 \to 1.0$):**
   $$\text{TimingScore} = 1 - \text{RingScale} \quad (\text{Contracting ring } 1.0 \to 0.2)$$

4. **Composite Catch Probability:**
   $$P(\text{Catch}) = \text{clamp}\left((\text{BaseRate} \times 0.65) + (\text{Accuracy} \times 0.25) + (\text{TimingScore} \times 0.15), 0.05, 0.95\right)$$

---

## 5. Security & Authentication Model

1. **Password Hashing:** Passwords are never stored in plaintext. Each account generates a unique 16-character cryptographic salt via `SecureRandom` combined with `SHA-256`:
   $$\text{Hash} = \text{SHA-256}(\text{Salt} + ":" + \text{Password})$$
2. **Session Persistence:** Active sessions are managed in Room SQLite (`sessions` table) and restored automatically on cold startup.
3. **Validation Guards:** Strict alphanumeric validation for usernames and minimum 6-character constraints on passwords.

---

## 6. Complete Generation 1 Catalog Specification

The database is pre-seeded with all **151 Pokémon** from the Kanto region, containing:
- Official National Pokédex ID (#001 Bulbasaur through #151 Mew)
- Primary and Secondary Element Types
- Height (meters) & Weight (kilograms)
- Base Combat Attributes (HP, Attack, Defense, Speed)
- Calibrated Base Capture Rate and Flee Rate
- Official Sprite Assets and Pokédex descriptions

---

## 7. Deliverables & Deployment

1. **Android APK:** Ready-to-install debug binary located at `PokemonDawnAndDusk-v1.0.apk`.
2. **GitHub Source Code:** Maintained at `https://github.com/RishabhJain027/PokemonDawnAndDusk`.
3. **Live Web Portal & Encounter Simulator:** Hosted on GitHub Pages at `https://rishabhjain027.github.io/PokemonDawnAndDusk/`.

---

© 2026 Rishabh Jain. All rights reserved. Built for Pokémon Dawn & Dusk.
