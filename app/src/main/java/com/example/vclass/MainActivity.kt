package com.example.vclass

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.vclass.navigation.AppNavigation

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppNavigation.openStart(this)
    }
}
