package com.example.passwordmanager.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.passwordmanager.data.Account
import com.example.passwordmanager.data.EncryptionHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    accounts: MutableList<Account>
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf(TextFieldValue("")) }
    var deletingAccount by remember { mutableStateOf<Account?>(null) }
    var navigationInProgress by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    fun handleNavigateToCreate() {
        if (navigationInProgress) return

        navigationInProgress = true
        coroutineScope.launch {
            try {
                navController.navigate("createAccount") {
                    launchSingleTop = true
                    popUpTo(navController.graph.startDestinationId)
                    restoreState = true
                }
            } finally {
                delay(0)
                navigationInProgress = false
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Search...") },
                        singleLine = true,
                        shape = RoundedCornerShape(50.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent
                        ),
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    navController.navigate("settings")
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Settings"
                                )
                            }
                        }
                    )
                }
            )
        },
        floatingActionButton = {
            FloatingAddButton(
                onClick = { handleNavigateToCreate() },
                enabled = !navigationInProgress
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            // Filtered Account List
            val filteredAccounts = accounts.filter {
                it.getName().contains(searchQuery.text, ignoreCase = true)
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                items(filteredAccounts) { account ->
                    AccountItem(
                        account = account,
                        context = context,
                        onEdit = { accountToEdit ->
                            navController.navigate("createAccount/${accountToEdit.getName()}")
                        },
                        onDelete = { deletingAccount = it }
                    )
                }
            }

            // Delete Dialog
            deletingAccount?.let { account ->
                DeleteAccountDialog(
                    account = account,
                    onDismiss = { deletingAccount = null },
                    onDelete = {
                        accounts.remove(account)
                        deletingAccount = null
                        EncryptionHelper.saveAccounts(context, accounts)
                    }
                )
            }
        }
    }
}

@Composable
fun DeleteAccountDialog(
    account: Account,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Delete Account",
                color = MaterialTheme.colorScheme.error
            )
        },
        text = {
            Column {
                Text("Are you sure you want to delete this account?")
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = account.getName(),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                Text(
                    text = "This action cannot be undone",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDelete,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}