# Build `internal` app

The `internal` build type is meant to test new features as soon as they are merged to master. It uses the Rinkeby test network and the staging environment for relay and notification services.

## Local Build Setup Requirements
### Firebase / Google Services Plugin

To build the `internal` build type you need to have a properly configured `google-services.json` file. We need it for app distribution and crash reporting via Firebase. We don't want to have this file in the git repository because it contains an api_key which might cause custom builds to report accidentally to our Crashlytics database. For a Gnosis build you can get the latest version here: <https://console.firebase.google.com/u/0/project/safe-firebase-staging/settings/general/android:io.gnosis.safe.internal>.

For a custom build, please have a look at the [README.md#Firebase](../README.md#Firebase) for instructions on how to get it.

### Infura API Key

We need an Infura API key for the JSON RPC calls. Please create a free (of charge) account with Infura at: <https://infura.io/>. Then create a project. Find the `project id` in the Project settings -> Keys.
You need to get an API key and create a file named `project_keys` in the project root folder with the following content:

```
INFURA_API_KEY=<YOUR_PROJECT_ID>
```
Replace `<YOUR_PROJECT_ID>` with the `project id` that you got from Infura.

### Gradle task for local builds

`./gradlew assembleInternal`

Configure the gradle's JVM version according to the target SDK version, as provided in the [reference](https://developer.android.com/build/jdks#compileSdk).

## Buildkite (Continuous Integration) Setup

We use a self hosted Buildkite agent. Given that you have the appropriate credentials you can see the Android projects here: https://buildkite.com/gnosis
The Buildkite instance is also responsible for app signing and distribution. The `internal` app is distributed to Firebase.

## buildkite configuration within the project

The directory `.buildkite` contains two yml files recognized by the ci server:
- `deployment.yml`
  - Contains a label to upload to Firebase App distribution via `ci/upload_app_distribution.sh` on merges to master
- `pipeline.yml`
  - Contains a label to run certain scripts in the `ci` folder for unit and UI tests

The directory `ci` contains scripts run on the CI server during a build

- `prepare_env_buildkite.sh`
  - sets versionCode and versionName
- `run_unit_tests.sh`
  - Runs `testDebugUnitTest` with the `google-services.json.sample` from the `ci` folder
- `run_ui_tests.sh`
  - Starts integration tests on an emulator and triggers code coverage report generation

## Buildkite scripts (on build server)

We use the following hook scripts (located in `/etc/buildkite-agent/hooks` on the build server):

- `environment`
  - Setup environment variables regarding the Android SDK and the CODECOV_TOKEN
- `pre-command`
  - Copy `google-services.json` to the respective build type folders (prod version for `rinkeby` and `release` build type and staging version for `internal` builds)
  - Copy `gnosis-upload.jks` keystore used for release and rinkeby version (NOT `internal`)
  - Prepare build environment with secrets (see below)

## Secrets injected on the ci server

The `internal` build uses the `debug.keystore` that has been committed to the git repository. The CI server adds the following secrets to the build process.

- FIREBASE_TOKEN
  - For pushing updates to Firebase App Distribution
- INFURA_API_KEY
  - Used for Infura API calls
- GITHUB_API_KEY
  - Deploy to github (used in `ci/upload_to_github.sh`)
- SLACK_WEBHOOK
  - Used to notify certain slack channels
- CODECOV_TOKEN
  - To provide test coverage analysis

##  Access to the `internal` app

Access to `internal` builds is defined in the `internal` app in the `safe-firebase-staging` project on Firebase. See `Users and permissions` tab in `Settings`

## Build trigger

Whenever a PR is merged to master an `internal` build and its distribution to Firebase is triggered automatically. Firebase sends an email to all registered email addresses to notify them, that there is a new version. The email contains a link to download the new APK.

