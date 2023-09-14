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

package com.amplifyframework.ui.authenticator

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.amplifyframework.auth.AuthChannelEventName
import com.amplifyframework.auth.AuthException
import com.amplifyframework.auth.AuthUserAttribute
import com.amplifyframework.auth.AuthUserAttributeKey
import com.amplifyframework.auth.cognito.exceptions.service.CodeDeliveryFailureException
import com.amplifyframework.auth.cognito.exceptions.service.CodeExpiredException
import com.amplifyframework.auth.cognito.exceptions.service.CodeMismatchException
import com.amplifyframework.auth.cognito.exceptions.service.CodeValidationException
import com.amplifyframework.auth.cognito.exceptions.service.InvalidParameterException
import com.amplifyframework.auth.cognito.exceptions.service.InvalidPasswordException
import com.amplifyframework.auth.cognito.exceptions.service.PasswordResetRequiredException
import com.amplifyframework.auth.cognito.exceptions.service.UserNotConfirmedException
import com.amplifyframework.auth.cognito.exceptions.service.UserNotFoundException
import com.amplifyframework.auth.cognito.exceptions.service.UsernameExistsException
import com.amplifyframework.auth.exceptions.NotAuthorizedException
import com.amplifyframework.auth.exceptions.UnknownException
import com.amplifyframework.auth.options.AuthSignUpOptions
import com.amplifyframework.auth.result.AuthResetPasswordResult
import com.amplifyframework.auth.result.AuthSignInResult
import com.amplifyframework.auth.result.AuthSignUpResult
import com.amplifyframework.auth.result.step.AuthResetPasswordStep
import com.amplifyframework.auth.result.step.AuthSignInStep
import com.amplifyframework.auth.result.step.AuthSignUpStep
import com.amplifyframework.core.Amplify
import com.amplifyframework.ui.authenticator.auth.AmplifyAuthConfiguration
import com.amplifyframework.ui.authenticator.auth.toAttributeKey
import com.amplifyframework.ui.authenticator.auth.toFieldKey
import com.amplifyframework.ui.authenticator.auth.toVerifiedAttributeKey
import com.amplifyframework.ui.authenticator.enums.AuthenticatorInitialStep
import com.amplifyframework.ui.authenticator.enums.AuthenticatorStep
import com.amplifyframework.ui.authenticator.forms.FieldError
import com.amplifyframework.ui.authenticator.forms.FieldError.ConfirmationCodeIncorrect
import com.amplifyframework.ui.authenticator.forms.FieldError.FieldValueExists
import com.amplifyframework.ui.authenticator.forms.FieldError.NotFound
import com.amplifyframework.ui.authenticator.forms.FieldKey.ConfirmationCode
import com.amplifyframework.ui.authenticator.forms.FieldKey.Password
import com.amplifyframework.ui.authenticator.forms.FormState
import com.amplifyframework.ui.authenticator.forms.buildForm
import com.amplifyframework.ui.authenticator.forms.setFieldError
import com.amplifyframework.ui.authenticator.states.BaseStateImpl
import com.amplifyframework.ui.authenticator.states.StepStateFactory
import com.amplifyframework.ui.authenticator.util.AmplifyResult
import com.amplifyframework.ui.authenticator.util.AuthProvider
import com.amplifyframework.ui.authenticator.util.AuthenticatorMessage
import com.amplifyframework.ui.authenticator.util.CannotSendCodeMessage
import com.amplifyframework.ui.authenticator.util.CodeSentMessage
import com.amplifyframework.ui.authenticator.util.ExpiredCodeMessage
import com.amplifyframework.ui.authenticator.util.InvalidLoginMessage
import com.amplifyframework.ui.authenticator.util.MissingConfigurationException
import com.amplifyframework.ui.authenticator.util.NetworkErrorMessage
import com.amplifyframework.ui.authenticator.util.PasswordResetMessage
import com.amplifyframework.ui.authenticator.util.RealAuthProvider
import com.amplifyframework.ui.authenticator.util.UnableToResetPasswordMessage
import com.amplifyframework.ui.authenticator.util.UnknownErrorMessage
import com.amplifyframework.ui.authenticator.util.toFieldError
import java.net.UnknownHostException
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.VisibleForTesting

internal class AuthenticatorViewModel(
    application: Application,
    private val authProvider: AuthProvider
) : AndroidViewModel(application) {

    // Constructor for compose viewModels provider
    constructor(application: Application) : this(application, RealAuthProvider())

    private val logger = Amplify.Logging.forNamespace("Authenticator")

    private lateinit var authConfiguration: AmplifyAuthConfiguration
    private lateinit var stateFactory: StepStateFactory
    lateinit var configuration: AuthenticatorConfiguration
        private set

    private val _stepState = MutableStateFlow<AuthenticatorStepState>(LoadingState)
    val stepState = _stepState.asStateFlow()

    private val currentState: AuthenticatorStepState
        get() = stepState.value

    // Gets the current state or null if the current state is not the parameter type
    private inline fun <reified T> currentStateAs(): T? = currentState as? T

    private val _events = MutableSharedFlow<AuthenticatorMessage>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events = _events.asSharedFlow()

    fun start(configuration: AuthenticatorConfiguration) {
        if (::configuration.isInitialized) {
            return
        }

        this.configuration = configuration

        viewModelScope.launch {
            val authConfig = authProvider.getConfiguration()

            if (authConfig == null) {
                handleGeneralFailure(MissingConfigurationException())
                return@launch
            }

            authConfiguration = authConfig

            stateFactory = StepStateFactory(
                authConfiguration,
                buildForm(configuration.signUpForm),
                ::moveTo
            )

            // Fetch the current session to determine if the user is already authenticated
            val result = authProvider.fetchAuthSession()
            when {
                result is AmplifyResult.Error -> handleGeneralFailure(result.error)
                result is AmplifyResult.Success && result.data.isSignedIn -> handleSignedIn()
                else -> moveTo(configuration.initialStep)
            }
        }

        // Respond to any events from Amplify Auth
        viewModelScope.launch {
            authProvider.authStatusEvents().collect {
                when (it.name) {
                    AuthChannelEventName.SIGNED_IN.name -> handleSignedInEvent()
                    AuthChannelEventName.SIGNED_OUT.name -> handleSignedOut()
                }
            }
        }
    }

    fun moveTo(initialStep: AuthenticatorInitialStep) {
        logger.debug("Moving to initial step: $initialStep")
        val state = when (initialStep) {
            AuthenticatorStep.SignUp -> stateFactory.newSignUpState(this::signUp)
            AuthenticatorStep.PasswordReset -> stateFactory.newResetPasswordState(this::resetPassword)
            else -> stateFactory.newSignInState(this::signIn)
        }
        moveTo(state)
    }

    private fun moveTo(state: AuthenticatorStepState) {
        logger.debug("Moving to step: ${state.step}")
        _stepState.value = state
    }

    //region SignUp

    private suspend fun signUp(username: String, password: String, attributes: List<AuthUserAttribute>) {
        viewModelScope.launch {
            val options = AuthSignUpOptions.builder().userAttributes(attributes).build()

            when (val result = authProvider.signUp(username, password, options)) {
                is AmplifyResult.Error -> handleSignUpFailure(result.error)
                is AmplifyResult.Success -> handleSignUpSuccess(username, password, result.data)
            }
        }.join()
    }

    private suspend fun confirmSignUp(username: String, password: String, code: String) {
        viewModelScope.launch {
            when (val result = authProvider.confirmSignUp(username, code)) {
                is AmplifyResult.Error -> handleSignUpConfirmFailure(result.error)
                is AmplifyResult.Success -> handleSignUpSuccess(username, password, result.data)
            }
        }.join()
    }

    private suspend fun resendSignUpCode(username: String) {
        viewModelScope.launch {
            logger.debug("Resending the SignUp code")
            when (val result = authProvider.resendSignUpCode(username)) {
                is AmplifyResult.Error -> handleSignUpFailure(result.error)
                is AmplifyResult.Success -> sendMessage(CodeSentMessage)
            }
        }.join()
    }

    private suspend fun handleSignUpFailure(error: AuthException) = handleAuthException(error)
    private suspend fun handleSignUpConfirmFailure(error: AuthException) = handleAuthException(error)

    private suspend fun handleSignUpSuccess(username: String, password: String, result: AuthSignUpResult) {
        when (result.nextStep.signUpStep) {
            AuthSignUpStep.CONFIRM_SIGN_UP_STEP -> {
                val newState = stateFactory.newSignUpConfirmState(
                    result.nextStep.codeDeliveryDetails,
                    onResendCode = { resendSignUpCode(username) },
                    onSubmit = { confirmationCode -> confirmSignUp(username, password, confirmationCode) }
                )
                moveTo(newState)
            }
            AuthSignUpStep.DONE -> handleSignedUp(username, password)
        }
    }

    private suspend fun handleSignedUp(username: String, password: String) {
        when (val result = authProvider.signIn(username, password)) {
            is AmplifyResult.Error -> {
                moveTo(AuthenticatorStep.SignIn)
                handleSignInFailure(username, password, result.error)
            }
            is AmplifyResult.Success -> handleSignInSuccess(username, password, result.data)
        }
    }

    //endregion
    //region SignIn

    @VisibleForTesting
    suspend fun signIn(username: String, password: String) {
        viewModelScope.launch {
            when (val result = authProvider.signIn(username, password)) {
                is AmplifyResult.Error -> handleSignInFailure(username, password, result.error)
                is AmplifyResult.Success -> handleSignInSuccess(username, password, result.data)
            }
        }.join()
    }

    private suspend fun confirmSignIn(username: String, password: String, challengeResponse: String) {
        viewModelScope.launch {
            when (val result = authProvider.confirmSignIn(challengeResponse)) {
                is AmplifyResult.Error -> handleSignInFailure(username, password, result.error)
                is AmplifyResult.Success -> handleSignInSuccess(username, password, result.data)
            }
        }.join()
    }

    private suspend fun setNewSignInPassword(username: String, password: String) {
        viewModelScope.launch {
            when (val result = authProvider.confirmSignIn(password)) {
                // an error here is more similar to a sign up error
                is AmplifyResult.Error -> handleSignUpFailure(result.error)
                is AmplifyResult.Success -> handleSignInSuccess(username, password, result.data)
            }
        }.join()
    }

    private suspend fun handleSignInFailure(username: String, password: String, error: AuthException) {
        // UserNotConfirmed and PasswordResetRequired are special cases where we need
        // to enter different flows
        when (error) {
            is UserNotConfirmedException -> handleUnconfirmedSignIn(username, password)
            is PasswordResetRequiredException -> handleResetRequiredSignIn(username)
            is NotAuthorizedException -> sendMessage(InvalidLoginMessage(error))
            else -> handleAuthException(error)
        }
    }

    private suspend fun handleUnconfirmedSignIn(username: String, password: String) {
        when (val result = authProvider.resendSignUpCode(username)) {
            is AmplifyResult.Error -> handleAuthException(result.error)
            is AmplifyResult.Success -> {
                val details = result.data
                val newState = stateFactory.newSignUpConfirmState(
                    details,
                    onResendCode = { resendSignUpCode(username) },
                    onSubmit = { confirmationCode -> confirmSignUp(username, password, confirmationCode) }
                )
                moveTo(newState)
            }
        }
    }

    private suspend fun handleResetRequiredSignIn(username: String) {
        when (val result = authProvider.resetPassword(username)) {
            is AmplifyResult.Error -> moveTo(AuthenticatorStep.PasswordReset)
            is AmplifyResult.Success -> handleResetPasswordSuccess(username, result.data)
        }
    }

    private suspend fun handleSignInSuccess(username: String, password: String, result: AuthSignInResult) {
        when (val nextStep = result.nextStep.signInStep) {
            AuthSignInStep.DONE -> checkVerificationMechanisms()
            AuthSignInStep.CONFIRM_SIGN_IN_WITH_SMS_MFA_CODE -> moveTo(
                stateFactory.newSignInMfaState(
                    result.nextStep.codeDeliveryDetails
                ) { confirmationCode -> confirmSignIn(username, password, confirmationCode) }
            )
            AuthSignInStep.CONFIRM_SIGN_IN_WITH_CUSTOM_CHALLENGE -> moveTo(
                stateFactory.newSignInConfirmCustomState(
                    result.nextStep.codeDeliveryDetails,
                    result.nextStep.additionalInfo ?: emptyMap()
                ) { confirmationCode -> confirmSignIn(username, password, confirmationCode) }
            )
            AuthSignInStep.CONFIRM_SIGN_IN_WITH_NEW_PASSWORD -> moveTo(
                stateFactory.newSignInConfirmNewPasswordState { newPassword ->
                    setNewSignInPassword(username, newPassword)
                }
            )
            // This step isn't actually returned, it comes back as a PasswordResetRequiredException.
            // Handling here for future correctness
            AuthSignInStep.RESET_PASSWORD -> handleResetRequiredSignIn(username)
            // This step isn't actually returned, it comes back as a UserNotConfirmedException.
            // Handling here for future correctness
            AuthSignInStep.CONFIRM_SIGN_UP -> handleUnconfirmedSignIn(username, password)
            // Show an error for TOTP next step
            AuthSignInStep.CONTINUE_SIGN_IN_WITH_TOTP_SETUP,
            AuthSignInStep.CONTINUE_SIGN_IN_WITH_MFA_SELECTION,
            AuthSignInStep.CONFIRM_SIGN_IN_WITH_TOTP_CODE -> {
                val exception = AuthException(
                    "Authenticator does not yet support TOTP workflows.",
                    "Disable TOTP to use Authenticator."
                )
                logger.error("Unsupported next step $nextStep", exception)
                sendMessage(UnknownErrorMessage(exception))
            }
            else -> {
                // Generic error for any other next steps that may be added in the future
                val exception = AuthException(
                    "Unsupported next step $nextStep.",
                    "Authenticator does not support this Authentication flow, disable it to use Authenticator."
                )
                logger.error("Unsupported next step $nextStep", exception)
                sendMessage(UnknownErrorMessage(exception))
            }
        }
    }

    private suspend fun checkVerificationMechanisms() {
        val mechanisms = authConfiguration.verificationMechanisms
        if (mechanisms.isEmpty()) {
            handleSignedIn()
        } else {
            when (val result = authProvider.fetchUserAttributes()) {
                is AmplifyResult.Error -> {
                    // We can't verify their attributes, simply skip it and continue
                    // the sign in
                    handleSignedIn()
                }
                is AmplifyResult.Success -> {
                    val hasVerified = mechanisms.any { mechanism ->
                        val key = mechanism.toVerifiedAttributeKey()
                        result.data.any { it.key == key && it.value.toBoolean() }
                    }
                    val verificationAttributeKeys = mechanisms.map { it.toAttributeKey() }
                    val verificationAttributes = result.data.filter { verificationAttributeKeys.contains(it.key) }
                    if (hasVerified) {
                        handleSignedIn()
                    } else {
                        val newState = stateFactory.newVerifyUserState(
                            attributes = verificationAttributes,
                            onSubmit = this::verifyUserAttribute,
                            onSkip = this::skipUserVerification
                        )
                        moveTo(newState)
                    }
                }
            }
        }
    }

    //endregion
    //region Password Reset

    private suspend fun resetPassword(username: String) {
        viewModelScope.launch {
            logger.debug("Initiating reset password")
            when (val result = authProvider.resetPassword(username)) {
                is AmplifyResult.Error -> handleResetPasswordError(result.error)
                is AmplifyResult.Success -> handleResetPasswordSuccess(username, result.data)
            }
        }.join()
    }

    private suspend fun confirmResetPassword(username: String, password: String, code: String) {
        viewModelScope.launch {
            logger.debug("Confirming password reset")
            when (val result = authProvider.confirmResetPassword(username, password, code)) {
                is AmplifyResult.Error -> handleResetPasswordError(result.error)
                is AmplifyResult.Success -> handlePasswordResetComplete()
            }
        }.join()
    }

    private suspend fun handleResetPasswordSuccess(
        username: String,
        result: AuthResetPasswordResult
    ) {
        when (result.nextStep.resetPasswordStep) {
            AuthResetPasswordStep.DONE -> handlePasswordResetComplete()
            AuthResetPasswordStep.CONFIRM_RESET_PASSWORD_WITH_CODE -> {
                logger.debug("Password reset confirmation required")
                val state = stateFactory.newResetPasswordConfirmState(
                    result.nextStep.codeDeliveryDetails
                ) { newPassword, confirmationCode ->
                    confirmResetPassword(
                        username = username,
                        password = newPassword,
                        code = confirmationCode
                    )
                }
                moveTo(state)
            }
        }
    }

    private suspend fun handlePasswordResetComplete() {
        logger.debug("Password reset complete")
        sendMessage(PasswordResetMessage)
        moveTo(
            stateFactory.newSignInState(this::signIn)
        )
    }

    private suspend fun handleResetPasswordError(error: AuthException) = handleAuthException(error)

//endregion
//region Sign Out

    private suspend fun signOut() = withContext(viewModelScope.coroutineContext) {
        logger.debug("Signing out the user")
        authProvider.signOut()
    }

//endregion
// region Verify User

    private suspend fun verifyUserAttribute(key: AuthUserAttributeKey) {
        viewModelScope.launch {
            when (val result = authProvider.resendUserAttributeConfirmationCode(key)) {
                is AmplifyResult.Error -> handleAuthException(result.error)
                is AmplifyResult.Success -> {
                    val newState = stateFactory.newVerifyUserConfirmState(
                        result.data,
                        onSubmit = { confirmUserAttribute(key, it) },
                        onResendCode = { resendUserAttributeCode(key) },
                        onSkip = { skipUserVerification() }
                    )
                    moveTo(newState)
                }
            }
        }.join()
    }

    private fun skipUserVerification() {
        viewModelScope.launch {
            handleSignedIn()
        }
    }

    private suspend fun confirmUserAttribute(key: AuthUserAttributeKey, confirmationCode: String) {
        viewModelScope.launch {
            when (val result = authProvider.confirmUserAttribute(key, confirmationCode)) {
                is AmplifyResult.Error -> handleAuthException(result.error)
                is AmplifyResult.Success -> handleSignedIn()
            }
        }.join()
    }

    private suspend fun resendUserAttributeCode(key: AuthUserAttributeKey) {
        viewModelScope.launch {
            when (val result = authProvider.resendUserAttributeConfirmationCode(key)) {
                is AmplifyResult.Error -> handleAuthException(result.error)
                is AmplifyResult.Success -> sendMessage(CodeSentMessage)
            }
        }.join()
    }

//endregion

    private suspend fun handleAuthException(error: AuthException) {
        logger.warn("Encountered AuthException: $error")
        val state = currentStateAs<BaseStateImpl>() ?: return
        when (error) {
            is InvalidParameterException -> {
                // TODO : This happens if a field is invalid format e.g. phone number
                // TODO : User's email is not verified...  how to handle?
                // This can also occur if no email/phone is set on a created user during
                // sign in with a temp password, i.e. FORCE_CHANGE_PASSWORD state
                sendMessage(UnknownErrorMessage(error))
            }
            is UserNotFoundException -> state.form.setSignInMethodError(NotFound)
            is UsernameExistsException -> state.form.setSignInMethodError(FieldValueExists)
            is InvalidPasswordException -> state.form.setFieldError(Password, error.toFieldError())
            is NotAuthorizedException -> {
                // FORCE_CHANGE_PASSWORD is not allowed to reset password
                sendMessage(UnableToResetPasswordMessage(error))
            }
            is CodeMismatchException -> state.form.setFieldError(ConfirmationCode, ConfirmationCodeIncorrect)
            is CodeDeliveryFailureException -> sendMessage(CannotSendCodeMessage(error))
            is CodeExpiredException -> sendMessage(ExpiredCodeMessage(error))
            is CodeValidationException -> sendMessage(UnknownErrorMessage(error))
            is UnknownException -> {
                if (error.cause is UnknownHostException) {
                    sendMessage(NetworkErrorMessage(error))
                } else {
                    sendMessage(UnknownErrorMessage(error))
                }
            }
            else -> sendMessage(UnknownErrorMessage(error))
        }
    }

    private suspend fun handleSignedIn() {
        logger.debug("Log in successful, getting current user")
        when (val result = authProvider.getCurrentUser()) {
            is AmplifyResult.Error -> handleGeneralFailure(result.error)
            is AmplifyResult.Success -> moveTo(stateFactory.newSignedInState(result.data, this::signOut))
        }
    }

    // Amplify has told us the user signed in.
    private suspend fun handleSignedInEvent() {
        // TODO : move the user to signedInState *if* we are not in the process of signing in or verifying the user
    }

    private fun handleSignedOut() {
        logger.debug("User has been signed out")
        moveTo(AuthenticatorStep.SignIn)
    }

    private fun handleGeneralFailure(error: AuthException) {
        logger.error(error.toString())
        moveTo(ErrorState(error))
    }

    private suspend fun sendMessage(event: AuthenticatorMessage) {
        logger.debug("Sending message: $event")
        _events.emit(event)
    }

    private fun FormState.setSignInMethodError(error: FieldError) {
        setFieldError(authConfiguration.signInMethod.toFieldKey(), error)
    }
}
