package com.dawnanddusk.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.dawnanddusk.app.navigation.AppNavGraph
import com.dawnanddusk.ui.theme.DeepNavy
import com.dawnanddusk.ui.theme.PokemonDawnAndDuskTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PokemonDawnAndDuskTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DeepNavy
                ) {
                    AppNavGraph()
                }
            }
        }
    }
}
