package com.example.vclass


import android.app.Application
import io.realm.kotlin.Realm
import io.realm.kotlin.RealmConfiguration

class VClassApplication : Application() {

    companion object {
        lateinit var realm: Realm
    }

    override fun onCreate() {
        super.onCreate()

        val config = RealmConfiguration.Builder(
            schema = setOf(
                // Add all your Realm model classes here, e.g.
                LocalResource::class
            )
        )
            .schemaVersion(1)
            .build()

        realm = Realm.open(config)
    }
}
