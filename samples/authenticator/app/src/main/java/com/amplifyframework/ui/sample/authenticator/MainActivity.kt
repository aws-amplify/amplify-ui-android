/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 * http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *  express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.amplifyframework.ui.sample.authenticator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.amplifyframework.ui.authenticator.AuthenticatorState
import com.amplifyframework.ui.authenticator.SignedInState
import com.amplifyframework.ui.authenticator.rememberAuthenticatorState
import com.amplifyframework.ui.authenticator.ui.Authenticator
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val inDarkMode = isSystemInDarkTheme()
            var currentTheme by remember { mutableStateOf(SupportedTheme.Default) }
            var darkMode by remember { mutableStateOf(inDarkMode) }
            val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

            val authenticatorState = rememberAuthenticatorState()

            ApplyTheme(theme = currentTheme, darkMode = darkMode) {
                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        Surface(modifier = Modifier.fillMaxHeight()) {
                            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                ThemeSelector(
                                    modifier = Modifier.padding(16.dp),
                                    currentTheme = currentTheme,
                                    darkMode = darkMode,
                                    onChangeCurrentTheme = { currentTheme = it },
                                    onChangeDarkMode = { darkMode = it }
                                )
                            }
                        }
                    }
                ) {
                    SampleAppContent(drawerState, authenticatorState)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SampleAppContent(
    drawerState: DrawerState,
    authenticatorState: AuthenticatorState
) {
    val scope = rememberCoroutineScope()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Authenticator Sample App") },
                navigationIcon = {
                    IconButton(onClick = {
                        scope.launch { drawerState.open() }
                    }) {
                        Icon(imageVector = Icons.Default.Menu, contentDescription = "Open Menu")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.Center
        ) {
            Authenticator(
                modifier = Modifier.fillMaxWidth(),
                state = authenticatorState
            ) { state ->
                SignedInContent(state)
            }
        }
    }
}

@Suppress("DeferredResultUnused")
@Composable
fun SignedInContent(state: SignedInState) {
    val scope = rememberCoroutineScope()
    Column {
        Text("You've signed in as ${state.user.username}!")
        Button(onClick = { scope.launch { state.signOut() } }) {
            Text("Sign Out")
        }
    }
}
