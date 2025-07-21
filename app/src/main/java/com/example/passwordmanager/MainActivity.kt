package com.example.passwordmanager

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.passwordmanager.data.Account
import com.example.passwordmanager.data.EncryptionHelper
import com.example.passwordmanager.ui.CreateAccountScreen
import com.example.passwordmanager.ui.HomeScreen
import com.example.passwordmanager.ui.LockScreen
import com.example.passwordmanager.ui.SettingsScreen
import com.example.passwordmanager.ui.security.SecurityManager
import com.example.passwordmanager.ui.theme.PasswordManagerTheme

class MainActivity : FragmentActivity() {
    private lateinit var securityManager: SecurityManager

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        securityManager = SecurityManager(this)
        enableEdgeToEdge()

        setContent {
            PasswordManagerTheme {
                val navController = rememberNavController()
                val accounts = remember { mutableStateListOf<Account>() }
                var isUnlocked by remember { mutableStateOf(!securityManager.isSecurityEnabled()) }

                // Load accounts when activity starts
                LaunchedEffect(Unit) {
                    val loadedAccounts = EncryptionHelper.loadAccounts(this@MainActivity)
                    accounts.addAll(loadedAccounts)
                }

                // Save accounts immediately when modified
                LaunchedEffect(accounts) {
                    if (accounts.isNotEmpty()) { // Only save if accounts exist
                        EncryptionHelper.saveAccounts(this@MainActivity, accounts)
                    }
                }

                if (!isUnlocked && securityManager.isSecurityEnabled()) {
                    LockScreen(
                        onUnlockRequested = {
                            securityManager.authenticate(
                                this@MainActivity,
                                onSuccess = { isUnlocked = true },
                                onError = { /* Handle error if needed */ }
                            )
                        }
                    )
                } else {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        contentWindowInsets = WindowInsets(0, 0, 0, 0)
                    ) {
                        NavHost(navController, startDestination = "home") {
                            composable("home") {
                                HomeScreen(
                                    navController = navController,
                                    accounts = accounts
                                )
                            }
                            composable("createAccount") {
                                CreateAccountScreen(navController, Modifier, accounts, context = this@MainActivity)
                            }
                            composable("createAccount/{accountName}") { backStackEntry ->
                                val accountName = backStackEntry.arguments?.getString("accountName")
                                val account = accounts.find { it.getName() == accountName }
                                CreateAccountScreen(
                                    navController = navController,
                                    modifier = Modifier,
                                    accounts = accounts,
                                    existingAccount = account,
                                    context = this@MainActivity
                                )
                            }
                            composable("settings") {
                                SettingsScreen(
                                    navController = navController,
                                    securityManager = securityManager,
                                    onSecurityChanged = { enabled ->
                                        isUnlocked = !enabled
                                    },
                                    onImport = { importedAccounts ->
                                        accounts.clear()
                                        accounts.addAll(importedAccounts)
                                        EncryptionHelper.saveAccounts(this@MainActivity, accounts)
                                    },
                                    onExport = { accounts.toList() },
                                    onDeleteAll = {
                                        accounts.clear()
                                        EncryptionHelper.saveAccounts(this@MainActivity, accounts)

                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}