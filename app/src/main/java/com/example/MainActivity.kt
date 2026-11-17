package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.SampleRepository
import com.example.data.local.SampleDatabase
import com.example.data.location.FieldLocationProvider
import com.example.data.network.OpenMeteoApi
import com.example.ui.MainAppScaffold
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.authStateFlow
import com.example.ui.theme.RiceLoggerTheme
import com.example.ui.viewmodel.RiceLoggerViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RiceLoggerTheme {
                val auth = Firebase.auth
                var isDirectlySignedIn by remember { mutableStateOf(false) }
                var customEmailUser by remember { mutableStateOf<String?>(null) }
                val currentUser by auth.authStateFlow()
                    .collectAsStateWithLifecycle(initialValue = auth.currentUser)

                val user = currentUser
                val activeEmail = user?.email ?: customEmailUser ?: if (isDirectlySignedIn) "Field Researcher" else null

                if (user == null && customEmailUser == null && !isDirectlySignedIn) {
                    AuthScreen(
                        onAuthSuccess = {
                            isDirectlySignedIn = true
                        },
                        onGuestSignIn = {
                            isDirectlySignedIn = true
                        },
                        onEmailSignIn = { email ->
                            customEmailUser = email
                            isDirectlySignedIn = true
                        }
                    )
                } else {
                    val activeUid = user?.uid ?: customEmailUser ?: "researcher_account"
                    key(activeUid) {
                        val loggerViewModel: RiceLoggerViewModel = viewModel(
                            key = activeUid,
                            factory = viewModelFactory {
                                initializer {
                                    val app = checkNotNull(this[APPLICATION_KEY]) {
                                        "APPLICATION_KEY missing from CreationExtras"
                                    }
                                    val db = SampleDatabase.getInstance(app)
                                    val weatherApi = OpenMeteoApi.create()
                                    val repo = SampleRepository(db.sampleDao(), weatherApi)
                                    val locationProvider = FieldLocationProvider(app)
                                    RiceLoggerViewModel(
                                        repository = repo,
                                        locationProvider = locationProvider,
                                        researcherUid = activeUid
                                    )
                                }
                            }
                        )
                        MainAppScaffold(
                            user = user,
                            customEmail = activeEmail,
                            viewModel = loggerViewModel,
                            onGuestSignOut = {
                                isDirectlySignedIn = false
                                customEmailUser = null
                            }
                        )
                    }
                }
            }
        }
    }
}
