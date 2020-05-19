# Build `internal` app

The `internal` app uses the Rinkeby test network. It uses the staging environment for relay and notification services. It is meant to test new features as soon as they are merged to master.

## Local Build Setup Requirements
### Firebase / Goggle Services Plugin

To build the `internal` buildType you need to have a properly configured `google-services.json` file. We need it for App distribution via Firebase and Crash reporting.
Whe don't want to have this file in the git repository because it contains an api_key which might cause builds by external developers to report accidentally to our Crashlytics database
As an Gnosis developer you can get the latest version here: <https://console.firebase.google.com/u/0/project/safe-firebase-staging/settings/general/android:io.gnosis.safe.internal>
As an external developer please have a look at [README](../README.md#Firebase) on instructions how to get it.

### INFURA API Key
  - Why do we need it?
    - The Json RPC depends on [Infura](https://infura.io/).
  - How/where to get it
    - Create a free (of charge) account at <https://infura.io/>
    - Create a project
    - Find project id in the project settings -> Keys

You need to get an API key and create a file named ``project_keys` in the project folder with the following contents:
INFURA_API_KEY=<YOUR_PROJECT_ID>
Replace <YOUR_PROJECT_ID> with the project id that you get from Infura.


## Gradle tasks for local builds

`./gradlew assembleInternal`

## Buildkite (Continuous Integration) Setup

We use a self hosted Buildkite agent. Given you have the appropriate credentials you can see its Android projects here: https://buildkite.com/gnosis
The Buildkite instance is also responsible for app signing and distribution. The `internal` app is distributed to Firebase.

## Secrets injected on the ci server

- SIGNING_STORE_PASSWORD
- SIGNING_KEY_PASSWORD
- FIREBASE_TOKEN -> Necessary to push updates to Firebase App Distribution
- INFURA_API_KEY -> See above
- GITHUB_API_KEY ->
- SLACK_WEBHOOK -> Used to notify certain slack channels
- CODECOV

##  Access to the `internal` app

Access to `internal` builds is defined in the `internal` app in the `safe-firebase-staging` project on Firebase. See `Users and permissions` tab in `Settings`

## Build trigger

Whenever a PR is merged to master and `internal` build is triggered.
