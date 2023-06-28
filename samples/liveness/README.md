# Liveness Sample

This folder contains a sample application to demonstrate usage of the FaceLivenessDetector to conduct a face liveness check.

## Run the App Locally

### Set Up the Sample App

1. Install [Android Studio](https://developer.android.com/studio#downloads) version 4.0 or higher
2. Open Terminal and clone the Amplify UI Android GitHub repository under your preferred directory:
```
git clone https://github.com/aws-amplify/amplify-ui-android.git
```
3. Change directory to `samples/liveness`
```
cd amplify-ui-android/samples/liveness
```
4. Import the sample app as a project in Android Studio (select `import` instead of `open`)

### Provision AWS Backend Resources
1. Follow the [instructions](https://docs.amplify.aws/start/getting-started/installation/q/integration/android/) to sign up for an AWS account and set up the Amplify CLI.
2. Initialize Amplify in the project by running the following command from the project directory:
```
amplify init
```
Provide the responses shown after each of the following prompts.
```
? Enter a name for the environment
    `dev`
? Choose your default editor:
    `Android Studio`
? Where is your Res directory:
    `app/src/main/res`
? Select the authentication method you want to use:
    `AWS profile`
? Please choose the profile you want to use
    `default`
```
Wait until provisioning is finished. Upon successfully running `amplify init`, you will see a configuration file created in `./app/src/main/res/raw/` called `amplifyconfiguration.json`. This file will be bundled into your application so that the Amplify libraries know how to reach your provisioned backend resources at runtime.

3. Configure Auth Category

The Amplify Auth category provides an interface for authenticating a user and also provides the necessary authorization to other Amplify categories. It comes with default, built-in support for Amazon Cognito User Pools and Identity Pools. From your project directory, run the following command to add the Amplify Auth category:
```
amplify add auth
```
Provide the responses shown after each of the following prompts.
```
? Do you want to use the default authentication and security configuration? 
    `Default configuration with Social Provider (Federation)`
? How do you want users to be able to sign in? 
    `Username`
? Do you want to configure advanced settings? 
    `No, I am done.`
? What domain name prefix you want us to create for you? 
    `(default)`
? Enter your redirect signin URI: 
    `myapp://callback/`
? Do you want to add another redirect signin URI 
    `No`
? Enter your redirect signout URI: 
    `myapp://signout/`
? Do you want to add another redirect signout URI 
    `No`
? Select the social providers you want to configure for your user pool: 
    `<hit enter>`
```
4. Once finished, run `amplify push` to publish your changes.
   Upon completion, `amplifyconfiguration.json` should be updated to reference these provisioned backend resources.
5. Follow the steps below to create an inline policy to enable authenticated app users to access Rekognition, which powers the FaceLivenessDetector.
   1. Go to AWS IAM console, then Roles
   2. Select the newly created `authRole` for the project (`amplify-<project_name>-<env_name>-<id>-authRole`).
   3. Choose **Add Permissions**, then select **Create Inline Policy**, then choose **JSON** and paste the following:

    ```
    {
        "Version": "2012-10-17",
        "Statement": [
            {
                "Effect": "Allow",
                "Action": "rekognition:StartFaceLivenessSession",
                "Resource": "*"
            }
        ]
    }
    ```

   4. Choose **Review Policy**
   5. Name the policy
   6. Choose **Create Policy**

### Provision Backend API
Set up a backend API using [Amplify API category](https://docs.amplify.aws/lib/restapi/getting-started/q/platform/android/) to create the liveness session and retrieve the liveness session results. The liveness sample app is set up to use API Gateway endpoints for creating and retrieving the liveness session. 

1. Run the following command to create a new REST API:
```
amplify add api
```
Provide the responses shown after each of the following prompts.
```
? Please select from one of the below mentioned services
    `REST`
? Would you like to add a new path to an existing REST API:
    `N`
? Provide a friendly name for your resource to be used as a label for this category in the project: 
    `livenessBackendAPI`
? Provide a path (e.g., /book/{isbn}): 
    `/liveness/create`
? Choose a Lambda source
    `Create a new Lambda function`
?  Provide an AWS Lambda function name:
    `createSession`
? Choose the runtime that you want to use: 
    `NodeJS`
? Choose the function template that you want to use: 
    `Serverless ExpressJS function (Integration with API Gateway)`
? Do you want to configure advanced settings? 
    `N`
? Do you want to edit the local lambda function now? 
    `Y`
? Restrict API access?
    `Y`
? Who should have access? 
    `N`
? Do you want to configure advanced settings? 
    `Authenticated users only`
? What permissions do you want to grant to Authenticated users? 
    `create,read,update`
? Do you want to add another path?  
    `Y`
? Provide a path (e.g., /book/{isbn}): 
    `/liveness/{sessionId}`
? Choose a Lambda source
    `Create a new Lambda function`
?  Provide an AWS Lambda function name:
    `getResults`
? Choose the runtime that you want to use: 
    `NodeJS`
? Choose the function template that you want to use: 
    `Serverless ExpressJS function (Integration with API Gateway)`
? Do you want to configure advanced settings? 
    `N`
? Do you want to edit the local lambda function now? 
    `Y`
? Restrict API access?
    `Y`
? Who should have access? 
    `Authenticated users only`
? What permissions do you want to grant to Authenticated users? 
    `create,read,update`
? Do you want to add another path?  
    `N`
```
2. Copy the code for from amplify-ui-android/samples/backend-lambda-functions to the path provided
3. Once finished, run `amplify push` to publish your changes. 
4. Follow the steps below to create an inline policy to enable the **createSession** lambda function to access Rekognition.
   1. Go to AWS Lambda console -> **CreateSession**  -> Configuration -> Permissions
   2. Click the role name under 'Execution role'
   3. Choose **Add Permissions**, then select **Create Inline Policy**, then choose **JSON** and paste the following:

    ```
    {
        "Version": "2012-10-17",
        "Statement": [
            {
                "Effect": "Allow",
                "Action": "rekognition:CreateFaceLivenessSession",
                "Resource": "*"
            }
        ]
    }
    ```
   4. Choose **Review Policy**
   5. Name the policy
   6. Choose **Create Policy**
5. Follow the steps below to create an inline policy to enable the **getResults** lambda function to access Rekognition.
   1. Go to AWS Lambda console -> **getResults**  -> Configuration -> Permissions
   2. Click the role name under 'Execution role'
   3. Choose **Add Permissions**, then select **Create Inline Policy**, then choose **JSON** and paste the following:

    ```
    {
        "Version": "2012-10-17",
        "Statement": [
            {
                "Effect": "Allow",
                "Action": "rekognition:GetFaceLivenessSessionResults",
                "Resource": "*"
            }
        ]
    }   
    ```
   4. Choose **Review Policy**
   5. Name the policy
   6. Choose **Create Policy**
### Run the App

Delete the generated API files app/src/main/java/[YOUR_API_NAME]

You may need to go to File -> Sync Project with Gradle Files if you get an error "SDK location not found".

Build and run the project on an Android device in Android Studio. 


The project requires Android SDK API level 24 (Android 7.0) or higher.
