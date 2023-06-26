# Authenticator

Amplify Authenticator provides a complete drop-in implementation of an authentication flow for your [Material 3 Jetpack Compose](https://developer.android.com/jetpack/compose/designsystems/material3) application using [Amplify Authentication](https://docs.amplify.aws/lib/auth/getting-started/q/platform/android/).

## Usage

### Prerequisites

Configure an Authentication backend with AWS Cognito. See the [Amplify Authentication Getting Started Guide](https://docs.amplify.aws/lib/auth/getting-started/q/platform/android/#set-up-backend-resources) for details, or use the following steps with the [Amplify CLI](https://docs.amplify.aws/cli/start/install/).

First initialize your Amplify project from your project root directory:

```
amplify init
```

And enter the following when prompted:

```
? Enter a name for the project
    `MyAmplifyApp`
? Initialize the project with the above configuration?
    `No`
? Enter a name for the environment
    `dev`
? Choose your default editor:
    `Android Studio`
? Choose the type of app that you're building
    `android`
? Where is your Res directory:
    `app/src/main/res`
? Select the authentication method you want to use:
    `AWS profile`
? Please choose the profile you want to use
    `default`
```

Then add the auth category:

```
amplify add auth
```

And enter the following when prompted:

```
? Do you want to use the default authentication and security configuration?
    `Default configuration`
? How do you want users to be able to sign in?
    `Username`
? Do you want to configure advanced settings?
    `No, I am done.`
```

Finally, push your changes to deploy your Authentication backend:

```
amplify push
```

### Add Authenticator to your app

Add a dependency on Amplify Authenticator to your application's `dependencies` block:

```kotlin
dependencies {
    implementation("com.amplifyframework.ui:authenticator:1.0.0")
}
```

Ensure that the [Auth plugin](https://docs.amplify.aws/lib/auth/getting-started/q/platform/android/#initialize-amplify-auth) is configured for Amplify in your `Application` class:

```kotlin
override fun onCreate() {
    super.onCreate()
    Amplify.addPlugin(AWSCognitoAuthPlugin())
    Amplify.configure(applicationContext)
}
```

Add the `Authenticator` composable to wrap the content you want to display after the user has signed in:

```kotlin
@Composable
fun MyApp() {
    Authenticator {
        Text("You have signed in!")
    }
}
```
