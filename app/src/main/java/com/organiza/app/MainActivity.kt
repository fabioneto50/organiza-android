package com.organiza.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.organiza.app.ui.OrganizaApp
import com.organiza.app.ui.theme.OrganizaTheme

class MainActivity : ComponentActivity() {
    private val viewModel: OrganizaViewModel by viewModels {
        OrganizaViewModel.factory(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OrganizaTheme {
                OrganizaApp(viewModel)
            }
        }
    }
}
