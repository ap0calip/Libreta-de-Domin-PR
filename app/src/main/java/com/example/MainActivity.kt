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

  @Suppress("DEPRECATION")
  override fun onCreate(savedInstanceState: Bundle?) {
    // Read and apply saved language on startup
    val sharedPrefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
    val lang = sharedPrefs.getString("selected_language", null)
    if (lang != null) {
      val locale = java.util.Locale(lang)
      java.util.Locale.setDefault(locale)
      val config = resources.configuration
      config.setLocale(locale)
      resources.updateConfiguration(config, resources.displayMetrics)
      
      val appConfig = applicationContext.resources.configuration
      appConfig.setLocale(locale)
      applicationContext.resources.updateConfiguration(appConfig, applicationContext.resources.displayMetrics)
    }

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

  @Suppress("DEPRECATION")
  fun updateLanguage(langCode: String) {
    val sharedPrefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
    sharedPrefs.edit().putString("selected_language", langCode).apply()
    
    val locale = java.util.Locale(langCode)
    java.util.Locale.setDefault(locale)
    val config = resources.configuration
    config.setLocale(locale)
    resources.updateConfiguration(config, resources.displayMetrics)
    
    val appConfig = applicationContext.resources.configuration
    appConfig.setLocale(locale)
    applicationContext.resources.updateConfiguration(appConfig, applicationContext.resources.displayMetrics)
    
    recreate()
  }
}
