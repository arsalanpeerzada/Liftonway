package dev.liftonway.core.data

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.liftonway.core.model.Person
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import java.time.Clock
import javax.inject.Provider
import javax.inject.Qualifier
import javax.inject.Singleton

/** Work that outlives a screen, such as the offline demo's driver confirming a request a few seconds later. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AppScope

/** The signed-in user as a ride's driver, for the offline demo's published rides. */
fun interface CurrentPerson {
    suspend fun get(): Person
}

/**
 * Real API or offline demo, chosen at build time (`-Pliftonway.fake=true` for the demo).
 * Notifications and the wallet stay in memory either way: the API doesn't cover them yet.
 */
@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    @Provides
    @Singleton
    fun clock(): Clock = Clock.systemDefaultZone()

    @Provides
    @Singleton
    @AppScope
    fun appScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Provides
    fun currentPerson(session: SessionRepository) = CurrentPerson {
        val profile = session.session.first().profile
        Person("me", profile?.name ?: "You", rating = null, photoUri = profile?.photoUri)
    }

    @Provides
    @Singleton
    fun auth(fake: Provider<FakeAuthBackend>, remote: Provider<RemoteAuthBackend>): AuthBackend = if (BuildConfig.USE_FAKE_BACKEND) fake.get() else remote.get()

    @Provides
    @Singleton
    fun rides(fake: Provider<FakeRideRepository>, remote: Provider<RemoteRideRepository>): RideRepository =
        if (BuildConfig.USE_FAKE_BACKEND) fake.get() else remote.get()

    @Provides
    @Singleton
    fun vehicles(fake: Provider<FakeVehicleRepository>, remote: Provider<RemoteVehicleRepository>): VehicleRepository =
        if (BuildConfig.USE_FAKE_BACKEND) fake.get() else remote.get()
}
