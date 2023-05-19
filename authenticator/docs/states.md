# Authenticator States

This document covers the state transitions for the Authenticator component as a [mermaid state diagram](https://mermaid.js.org/syntax/stateDiagram.html).

Some transitions are omitted for clarity:
- Most states (except `Loading`, `Error`, and `SignedIn`) can move to the initial states (`SignIn`, `SignUp`, `ResetPassword`) based on user action (e.g. clicking a "Back to Sign In" button).
- `SignIn` can have a next step that leads to `SignUpConfirm` (if SignUpConfirm was previously abandoned) or `ResetPassword` (if admin marks the account as needing password reset).

```mermaid
stateDiagram-v2
    state ChooseInitial <<choice>>
    state CheckAttributes <<choice>>
    state AutoSignIn <<choice>>
    state SignIn
    state SignInConfirmMfa
    state SignInConfirmNewPassword
    state SignInConfirmCustom
    state SignInNextStep <<choice>> 
    state SignUp
    state SignUpNextStep <<choice>>
    state SignUpConfirm
    state PasswordReset
    state PasswordResetConfirm
    state VerifyUser
    state VerifyUserConfirm

    [*] --> Loading

    Loading : Loading
    Loading : readConfiguration()
    Loading : fetchSession()

    
    Loading --> Error : [Error]

    ChooseInitial --> SignUp : initialStep[SignUp]
    ChooseInitial --> SignIn : initialStep[SignIn]
    ChooseInitial --> PasswordReset : initialStep[PasswordReset]

    SignIn : SignIn
    SignIn : signIn(username, password)
    SignIn --> SignInNextStep : Success
    
    SignInNextStep --> SignInConfirmMfa : [nextStep == MFA]
    SignInNextStep --> SignInConfirmCustom : [nextStep == Custom]
    SignInNextStep --> SignInConfirmNewPassword : [nextStep == NewPassword]
    SignInNextStep --> CheckAttributes : [nextStep == Done]

    SignInConfirmMfa : SignInConfirmMfa
    SignInConfirmMfa : confirmSignIn(confirmationCode)
    SignInConfirmMfa --> SignInNextStep : Success

    SignInConfirmNewPassword : SignInConfirmNewPassword
    SignInConfirmNewPassword : confirmSignIn(newPassword)
    SignInConfirmNewPassword --> SignInNextStep : Success

    SignInConfirmCustom : SignInConfirmCustom
    SignInConfirmCustom : confirmSignIn(challengeResponse)
    SignInConfirmCustom --> SignInNextStep : Success

    SignUp : SignUp
    SignUp : signUp(username, password, attributes)
    SignUp --> SignUpNextStep : Success

    SignUpNextStep --> AutoSignIn : [nextStep == DONE]
    SignUpNextStep --> SignUpConfirm : [nextStep == CONFIRM_SIGN_UP_STEP]
    
    SignUpConfirm : SignUpConfirm
    SignUpConfirm : confirmSignUp(username, confirmationCode)
    SignUpConfirm --> SignUpNextStep : Success

    PasswordReset : PasswordReset
    PasswordReset : resetPassword(username)
    PasswordReset --> PasswordResetConfirm : Success

    PasswordResetConfirm : PasswordResetConfirm
    PasswordResetConfirm : confirmResetPassword(username, newPassword, confirmationCode)
    PasswordResetConfirm --> SignIn : Success

    VerifyUser : VerifyUser
    VerifyUser : resendUserAttributeConfirmationCode(attributeKey)
    VerifyUser --> VerifyUserConfirm : Success
    VerifyUser --> SignedIn : Skip

    VerifyUserConfirm : VerifyUserConfirm
    VerifyUserConfirm : confirmUserAttribute(attributeKey, confirmationCode)
    VerifyUserConfirm --> SignedIn : Skip
    VerifyUserConfirm --> SignedIn : Success
    
    AutoSignIn --> SignIn : [signIn=error]
    AutoSignIn --> CheckAttributes : [signIn=success]

    CheckAttributes --> SignedIn : [numVerifiedAttributes >= 1]
    CheckAttributes --> VerifyUser : [numVerifiedAttributes == 0]

    Loading --> CheckAttributes : [signedIn == true]
    Loading --> ChooseInitial : [signedIn == false]

    Error --> [*]
    SignedIn --> [*]
   
```
