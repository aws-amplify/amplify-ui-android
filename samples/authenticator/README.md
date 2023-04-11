# Amplify Authenticator for Android

![](images/signin.png)
![](images/signup.png)
![](images/resetpassword.png)

## Overview

Authenticator is a _connected UI component_ in Amplify UI - that is, it is a UI implementation that is built on
functionality from the Amplify library. Authenticator uses the Amplify Auth category to implement a UI flow for
application sign up, sign in, and reset password.

## Running the Sample App

To run the sample application you will need:

- [Android Studio](https://developer.android.com/studio) Electric Eel or higher
- [Amplify CLI](https://docs.amplify.aws/cli/)

### Setup your backend

1. Follow the [project setup](https://docs.amplify.aws/lib/project-setup/prereq/q/platform/android/) steps on the
   Amplify Android documentation if you have not previously set up an AWS profile.
2. Create an Amplify project using
   the [amplify init](https://docs.amplify.aws/lib/project-setup/create-application/q/platform/android/#3-provision-the-backend-with-amplify-cli)
   command.
3. Configure your authentication backend using
   the [amplify add auth](https://docs.amplify.aws/lib/auth/getting-started/q/platform/android/#configure-auth-category)
   command. You do not need to follow the subsequent sections starting with `Install Amplify Libraries` as this is
   already done in the sample application. Remember to do an `amplify push` to deploy your backend.

### Run the application

1. Open the root folder in Android Studio to load the project into the IDE.
2. If you do not have an existing emulator setup, create
   an [Android Virtual Device](https://developer.android.com/studio/run/managing-avds) that will run the app.
3. Click the run button to build and run the application.

![](images/run.png)

### Modify your backend

You can try using different auth configurations to see how Authenticator responds. Some configurations need to be set
when the cognito user pool is created, while others can be modified in an existing pool.

#### Modify an existing user pool

You can modify the existing backend using the `amplify update auth` CLI command (recommended), or you can use
the [AWS Cognito Console](https://docs.aws.amazon.com/cognito/latest/developerguide/cognito-console.html)
to modify the user pool settings, followed by running `amplify pull` to update the local configuration.

#### Create a new user pool

You can run `amplify remove auth` followed by `amplify add auth` to create and start using a new user pool. Note this
will delete any existing users you have created, but it will allow changing some configurations such as the username
attribute. Remember to do an `amplify push` to deploy your changes.
