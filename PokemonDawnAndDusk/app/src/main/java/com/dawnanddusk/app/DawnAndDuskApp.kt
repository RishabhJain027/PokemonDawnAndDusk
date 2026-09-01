package com.dawnanddusk.app

import android.app.Application
import com.dawnanddusk.core.AudioService
import com.dawnanddusk.data.local.database.AppDatabase
import com.dawnanddusk.data.repository.*
import com.dawnanddusk.domain.engine.CaptureEngine
import com.dawnanddusk.domain.engine.SpawnEngine
import com.dawnanddusk.domain.usecase.*
import com.dawnanddusk.services.LocationService
import com.dawnanddusk.services.SensorService

class DawnAndDuskApp : Application() {

    lateinit var database: AppDatabase private set

    // Repositories
    lateinit var authRepository: AuthRepository private set
    lateinit var creatureRepository: CreatureRepository private set
    lateinit var spawnRepository: SpawnRepository private set
    lateinit var captureRepository: CaptureRepository private set
    lateinit var sessionRepository: SessionRepository private set

    // Engines
    val spawnEngine = SpawnEngine()
    val captureEngine = CaptureEngine()

    // Services
    lateinit var locationService: LocationService private set
    lateinit var sensorService: SensorService private set
    lateinit var audioService: AudioService private set

    // Use cases
    lateinit var loginUserUseCase: LoginUserUseCase private set
    lateinit var registerUserUseCase: RegisterUserUseCase private set
    lateinit var getSessionUseCase: GetSessionUseCase private set
    lateinit var logoutUserUseCase: LogoutUserUseCase private set
    lateinit var generateSpawnsUseCase: GenerateSpawnsUseCase private set
    lateinit var validateEncounterUseCase: ValidateEncounterUseCase private set
    lateinit var resolveCaptureUseCase: ResolveCaptureUseCase private set
    lateinit var getPokedexUseCase: GetPokedexUseCase private set
    lateinit var getCaptureHistoryUseCase: GetCaptureHistoryUseCase private set
    lateinit var getProfileStatsUseCase: GetProfileStatsUseCase private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = AppDatabase.getDatabase(this)

        sessionRepository = SessionRepositoryImpl(database.sessionDao())
        authRepository = AuthRepositoryImpl(database.playerDao(), database.sessionDao())
        creatureRepository = CreatureRepositoryImpl(database.creatureDao())
        spawnRepository = SpawnRepositoryImpl(database.spawnDao(), creatureRepository)
        captureRepository = CaptureRepositoryImpl(database.captureDao(), database.playerDao(), creatureRepository)

        locationService = LocationService(this)
        sensorService = SensorService(this)
        audioService = AudioService(this)

        loginUserUseCase = LoginUserUseCase(authRepository)
        registerUserUseCase = RegisterUserUseCase(authRepository)
        getSessionUseCase = GetSessionUseCase(sessionRepository, authRepository)
        logoutUserUseCase = LogoutUserUseCase(sessionRepository)
        generateSpawnsUseCase = GenerateSpawnsUseCase(spawnRepository, spawnEngine)
        validateEncounterUseCase = ValidateEncounterUseCase(spawnEngine, spawnRepository)
        resolveCaptureUseCase = ResolveCaptureUseCase(captureEngine, captureRepository, spawnRepository)
        getPokedexUseCase = GetPokedexUseCase(creatureRepository, captureRepository)
        getCaptureHistoryUseCase = GetCaptureHistoryUseCase(captureRepository)
        getProfileStatsUseCase = GetProfileStatsUseCase(authRepository, captureRepository)
    }

    companion object {
        lateinit var instance: DawnAndDuskApp
            private set
    }
}
