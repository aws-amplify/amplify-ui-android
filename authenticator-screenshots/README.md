# Authenticator Screenshots

Generates screenshots for the [Authenticator](../authenticator) project.

```shell
# Record screenshots
./gradlew authenticator-screenshots:recordPaparazziDebug

# Verify screenshots
./gradlew authenticator-screenshots:verifyPaparazziDebug
```

## Why a separate module?

Paparazzi currently [has an issue](https://github.com/cashapp/paparazzi/issues/622) that prevents it from co-habitating with Robolectric tests.
