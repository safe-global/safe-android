package io.gnosis.safe

import android.content.Context
import androidx.multidex.MultiDexApplication
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.iid.FirebaseInstanceId
import io.gnosis.safe.di.ComponentProvider
import io.gnosis.safe.di.components.ApplicationComponent
import io.gnosis.safe.di.components.DaggerApplicationComponent
import io.gnosis.safe.di.modules.ApplicationModule
import org.bouncycastle.jce.provider.BouncyCastleProvider
import pm.gnosis.crypto.LinuxSecureRandom
import timber.log.Timber
import timber.log.Timber.DebugTree
import java.security.Security

class HeimdallApplication : MultiDexApplication(), ComponentProvider {

    private val component: ApplicationComponent =
        DaggerApplicationComponent.builder()
            .applicationModule(ApplicationModule(this))
            .build()

    override fun get(): ApplicationComponent = component

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(DebugTree())
        }
        component.appInitManager().init()

        try {
            LinuxSecureRandom()
        } catch (e: Exception) {
            Timber.e("Could not register LinuxSecureRandom. Using default SecureRandom.")
        }
        Security.insertProviderAt(BouncyCastleProvider(), 1)


        //TODO: remove
        FirebaseInstanceId.getInstance().instanceId
            .addOnCompleteListener(OnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Timber.e(task.exception)
                    return@OnCompleteListener
                }
                // Get new Instance ID token
                val token = task.result?.token
                Timber.d("Firebase token: $token")
            })

    }

    companion object Companion {
        operator fun get(context: Context): ApplicationComponent {
            return (context.applicationContext as ComponentProvider).get()
        }
    }
}
