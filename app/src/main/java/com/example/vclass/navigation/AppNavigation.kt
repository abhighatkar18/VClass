package com.example.vclass.navigation

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.example.vclass.di.AppContainer
import com.example.vclass.feature.auth.LoginActivity
import com.example.vclass.feature.auth.SignupActivity
import com.example.vclass.feature.dashboard.DashboardActivity

internal object AppNavigation {
    fun openStart(activity: Activity) {
        val destination = if (AppContainer.auth.currentUserId == null) SignupActivity::class.java
            else DashboardActivity::class.java
        activity.startActivity(Intent(activity, destination))
        activity.finish()
    }

    fun openDashboard(activity: Activity) = replaceTask(activity, DashboardActivity::class.java)

    fun openSignedOut(activity: Activity) = replaceTask(activity, SignupActivity::class.java)

    fun openLogin(context: Context) = context.startActivity(Intent(context, LoginActivity::class.java))

    fun openSignup(context: Context) = context.startActivity(Intent(context, SignupActivity::class.java))

    private fun replaceTask(activity: Activity, destination: Class<out Activity>) {
        activity.startActivity(Intent(activity, destination).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        activity.finish()
    }
}
