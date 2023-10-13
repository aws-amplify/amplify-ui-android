# Authenticator Screenshots

Generates screenshots for the [Authenticator](../authenticator) project.

```shell
# Record screenshots
./gradlew authenticator-screenshots:recordPaparazziDebug

# Verify screenshots
./gradlew authenticator-screenshots:verifyPaparazziDebug
```

## Why a separate module?

`authenticator-screenshots` has a different rationale, but for this module the `paparazzi` plugin is incompatible with changing `externalNativeBuild` and `packagingOptions` in the `build.gradle.kts`.