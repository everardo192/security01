package mx.edu.utng.arg.security01

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import mx.edu.utng.arg.security01.navigation.NavigationGraph
import mx.edu.utng.arg.security01.navigation.Screen
import mx.edu.utng.arg.security01.network.RetrofitClient
import mx.edu.utng.arg.security01.ui.theme.Security01Theme
import mx.edu.utng.arg.security01.viewmodel.AuthViewModel

class MainActivity : ComponentActivity() {

    // CORRECCIÓN: Descomentar y usar viewModels()
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // CORRECCIÓN: Eliminar installSplashScreen() si no está disponible
        // installSplashScreen() - Solo disponible en versiones recientes

        super.onCreate(savedInstanceState)

        // Inicializar Retrofit
        RetrofitClient.initialize(this)

        setContent {
            Security01Theme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SecurityApp()
                }
            }
        }
    }

    @Composable
    fun SecurityApp() {
        val navController = rememberNavController()

        // CORRECCIÓN: Usar authViewModel que ahora está declarado
        LaunchedEffect(authViewModel.isLoggedIn()) {
            if (authViewModel.isLoggedIn()) {
                navController.navigate(Screen.Home.route) {
                    popUpTo(0) { inclusive = true }
                }
            }
        }

        NavigationGraph(
            navController = navController,
            startDestination = Screen.Splash.route
        )
    }
}