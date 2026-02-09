package com.amplifyframework.ui.sample.liveness

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amplifyframework.auth.AuthException
import com.amplifyframework.auth.AuthSession
import com.amplifyframework.auth.cognito.exceptions.invalidstate.SignedInException
import com.amplifyframework.auth.exceptions.SessionExpiredException
import com.amplifyframework.kotlin.core.Amplify
import com.amplifyframework.ui.liveness.model.FaceLivenessDetectionException
import kotlin.math.sign
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Fetching)
    val authState = _authState.asStateFlow()

    private val _fetchingSession = MutableStateFlow(false)
    val fetchingSession = _fetchingSession.asStateFlow()

    private val _sessionId = MutableStateFlow<String?>(null)
    val sessionId = _sessionId.asStateFlow()

    private val _fetchingResult = MutableStateFlow(false)
    val fetchingResult = _fetchingResult.asStateFlow()

    private val _resultData = MutableStateFlow<ResultData?>(null)
    val resultData = _resultData.asStateFlow()

    init {
        viewModelScope.launch {
            fetchAuthState()
        }
    }

    private suspend fun fetchAuthState() {
        _authState.value = AuthState.Fetching
        _authState.value = try {
            val result = Amplify.Auth.fetchAuthSession()
            if (result.isSignedIn) {
                AuthState.SignedIn
            } else {
                AuthState.SignedOut
            }
        } catch (error: AuthException) {
            Log.e(MainActivity.TAG, "fetchAuthState failed", error)
            AuthState.SignedOut
        }
    }

    fun launchSignIn(activity: Activity) {
        _authState.value = AuthState.SigningIn
        viewModelScope.launch {
            _authState.value = try {
                Amplify.Auth.signInWithWebUI(activity)
                AuthState.SignedIn
            } catch (e: Exception) {
                if (e is SignedInException) {
                    AuthState.SignedIn
                } else {
                    Log.e(MainActivity.TAG, "Failed to sign in", e)
                    AuthState.SignedOut
                }
            }
        }
    }

    suspend fun createLivenessSession(): String? {
        _fetchingSession.value = true
        return viewModelScope.async {
            try {
                val sessionId = LivenessSampleBackend.createSession()
                _sessionId.value = sessionId
                sessionId
            } catch (e: Exception) {
                _fetchingSession.value = false
                Log.e(MainActivity.TAG, "Failed to create Liveness session ID.", e)
                _sessionId.value = null

                if (e.isSessionExpired()) {
                    signOut()
                }

                null
            }
        }.await()
    }

    fun fetchSessionResult(sessionId: String) {
        if (_resultData.value != null) return // we already have result, likely timeout

        _fetchingResult.value = true
        viewModelScope.launch {
            try {
                val result = LivenessSampleBackend.getLivenessSessionResults(sessionId)

                val imageBytes = Base64.decode(result.auditImageBytes, Base64.DEFAULT)
                val auditImage = BitmapFactory.decodeByteArray(
                    imageBytes,
                    0,
                    imageBytes.size
                )

                val resultData = ResultData(
                    sessionId,
                    isLive = result.isLive,
                    confidenceScore = result.confidenceScore,
                    referenceImage = auditImage
                )

                _resultData.value = resultData
            } catch (e: Exception) {
                val results = ResultData(
                    sessionId,
                    error = FaceLivenessDetectionException(
                        e.message ?: "Error retrieving liveness results",
                        throwable = e
                    )
                )
                _resultData.value = results
            }
            _fetchingResult.value = false
        }
    }

    fun reportErrorResult(exception: FaceLivenessDetectionException) {
        sessionId.value?.let {
            _resultData.value = ResultData(it, error = exception)
            _fetchingResult.value = false
        }
    }

    fun clearSession() {
        _sessionId.value = null
        _resultData.value = null
        _fetchingResult.value = false
        _fetchingSession.value = false
    }

    private suspend fun signOut() {
        Amplify.Auth.signOut()
        _authState.value = AuthState.SignedOut
    }

    // Session refresh tokens require the user to sign in again
    private tailrec fun Throwable.isSessionExpired(): Boolean {
        val cause = this.cause
        return this is SessionExpiredException || (cause != null && cause != this && cause.isSessionExpired())
    }
}

sealed class AuthState {
    object Fetching : AuthState()
    object SignedIn : AuthState()
    object SigningIn : AuthState()
    object SignedOut : AuthState()
}

data class ResultData(
    val sessionId: String,
    val isLive: Boolean = false,
    val confidenceScore: Float = 0f,
    val referenceImage: Bitmap? = null,
    val error: FaceLivenessDetectionException? = null
)
