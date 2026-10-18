package com.maswadkar.lambandtiger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider

class MainActivity : ComponentActivity() {
    lateinit var model: GameViewModel
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        model = ViewModelProvider(this)[GameViewModel::class.java]
        if (savedInstanceState?.getBoolean("gameScreen") == true) model.restoreScreen()
        setContent { LambAndTiger(model) }
    }

    private val friend by lazy { ViewModelProvider(this)[com.maswadkar.lambandtiger.friend.FriendViewModel::class.java] }

    override fun onStart() {
        super.onStart()
        model.foreground(true)
        friend.foreground(true)
    }

    override fun onStop() {
        if (!isChangingConfigurations) { model.foreground(false); friend.foreground(false) }
        super.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("gameScreen", model.ui.value.screen == Screen.GAME)
        super.onSaveInstanceState(outState)
    }
}
