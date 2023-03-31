/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.amplifyframework.ui.sample.liveness

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.amplifyframework.ui.sample.liveness.ui.HomeScreen
import com.amplifyframework.ui.sample.liveness.ui.LivenessScreen
import com.amplifyframework.ui.sample.liveness.ui.ResultScreen
import com.amplifyframework.ui.sample.liveness.ui.theme.MyApplicationTheme
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {

    companion object {
        const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MyApplicationTheme {
                Navigation()
            }
        }
    }

    @Composable
    private fun Navigation(viewModel: MainViewModel = viewModel()) {
        val navController: NavHostController = rememberNavController()
        NavHost(navController = navController, startDestination = "home") {
            composable("home") {
                HomeScreen(
                    viewModel,
                    onStartChallenge = { navController.navigate("challenge") }
                )
            }

            composable(route = "challenge") {
                LivenessScreen(
                    viewModel,
                    onChallengeComplete = {
                        navController.navigate("results") {
                            popUpTo("challenge") {
                                inclusive = true
                            }
                        }

                    },
                    onBack = {
                        viewModel.clearSession()
                        navController.popBackStack()
                    }
                )
            }

            composable(route = "results") {
                ResultScreen(
                    viewModel,
                    onBack = {
                        viewModel.clearSession()
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}

