# Build internal/debug app

## Requirements
### Firebase / Goggle Services Plugin
- google-services.json
  - Why do we need it?
    - Google Services Plugin depends on it for:
      - App distribution
      - Crash reporting
  - Why don't we provide one
    -  It contains an api_key which might cause builds by external developers to report accidentally to our Crashlytics database
  - How where to get it
    - As an Gnosis developer you can get it here: <https://console.firebase.google.com/u/0/project/safe-firebase-staging/settings/general/android:io.gnosis.safe.internal>
    - As an external developer you need to do the following
      - Create a project here: <https://console.firebase.google.com/>
      - In that project create at least one app with the applicationId `io.gnosis.safe.debug`
      - Download the file google-services.json and place it inside the projects app folder
      - If you use a different applicationId then adjust the applicationId in: `app/build.gradle`
  - Gnosis employees / Outside contributors

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


## Gradle tasks
- ./gradlew assembleDebug
- 
