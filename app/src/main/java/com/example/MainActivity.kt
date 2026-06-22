package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.data.AppDatabase
import com.example.data.DominoRepository
import com.example.ui.DominoApp
import com.example.ui.DominoViewModel
import com.example.ui.DominoViewModelFactory
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    // Initialize Database & Repository
    val database = AppDatabase.getDatabase(applicationContext)
    val repository = DominoRepository(database.dominoDao())
    
    // Instantiate ViewModel with factory
    val viewModelCount: DominoViewModel by viewModels {
      DominoViewModelFactory(repository)
    }

    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        Surface(
          modifier = Modifier.fillMaxSize()
        ) {
          DominoApp(viewModel = viewModelCount)
        }
      }
    }
  }
}
