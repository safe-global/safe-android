# Safe Android App 
[![codecov](https://codecov.io/gh/safe-global/safe-android/branch/master/graph/badge.svg)](https://codecov.io/gh/safe-global/safe-android)

## Goal
The Safe aims to provide all users with a convenient, yet secure way to manage their funds and interact with decentralized applications on Ethereum. 
Transactions are secured by on-chain multi-factor-authentication. This is achieved by using the [Safe smart contracts](https://github.com/safe-global/safe-contracts). They are the successor of the broadly trusted [Gnosis Multisig Wallet smart contracts](https://github.com/gnosis/MultiSigWallet)).

## Setup

### Infura
The Json RPC depends on [Infura](https://infura.io/). You need to get an API key and create a file named `project_keys` with the following contents:
```
INFURA_API_KEY=<YOUR_PROJECT_ID>
```

Replace `<YOUR_PROJECT_ID>` with the `project id` that you get from Infura. You can find this `project id` at `Projects -> Settings -> Keys -> PROJECT ID` on the Infura Website.

### Firebase
The Safe Android App uses Firebase and your build will fail if you don't have the `google-services.json` file.
To get this file, you need to create a Firebase project at <https://console.firebase.google.com/> and add at least one Android application.
If you didn't change the applicationId in `app/build.gradle` you need to create an app with the package name `io.gnosis.safe.debug` to be able to build a debug app. You can find the latest `google-services.json` file in the `Project Settings` -> `General`

After downloading the file, copy it to the `app` module folder.

### Contribute
You can contribute to this repo by creating a Pull Request or an issue. Please follow the default template set for the Pull Requests.

#### Code Style
Make sure that you apply the [Kotlin coding conventions](https://kotlinlang.org/docs/reference/coding-conventions.html) and use the [style schema](heimdall-style.xml) in this repo with Android Studio.

Also always add newlines at the end of files. This can be enforced by Android Studio. For this enable `Editor -> General -> Ensure line feed … (Under the “Others” section)`.

### CI Setup

Travis is used for continuous integration. It requires the `google-services.json` and `debug-upload.jks`. These need to be encrypted into a single file using the travis cli.

```
tar cvf secrets.tar gnosis-upload.jks app/google-services.json app/src/rinkeby/google-services.json app/src/release/google-services.json
travis encrypt-file secrets.tar
```

### Release Process

See our [Release steps](docs/RELEASE.md) on how to prepare a release.
