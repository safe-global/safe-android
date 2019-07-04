# Gnosis Safe Android App [![Build Status](https://travis-ci.org/gnosis/safe-android.svg?branch=master)](https://travis-ci.org/gnosis/safe-android)

## Goal
The Gnosis Safe aims to provide all users with a convenient, yet secure way to manage their funds and interact with decentralized applications on Ethereum. 
Transactions are secured by on-chain multi-factor-authentication. This is achieved by using the [Gnosis Safe smart contracts](https://github.com/gnosis/safe-contracts). They are the successor of the broadly trusted [Gnosis Multisig Wallet smart contracts](https://github.com/gnosis/MultiSigWallet)).

## Setup

### Infura
The Json RPC depends on [Infura](https://infura.io/). You need to get an API key and create a file named `project_keys` with the following contents:
```
INFURA_API_KEY=<YOUR_API_KEY>
```

Replace `<YOUR_API_KEY>` with the key that you get from Infura.

### Fabric
This project is integrated with Fabric by default so you need to create a file named `fabric.properties` inside the `app` module with the following contents:
```
apiSecret=<YOUR_FABRIC_API_SECRET>
apiKey=<YOUR_FABRIC_API_KEY>
```
Replace each field with the respective information (found in Fabric).

**If you don't want to setup Fabric for this project you can follow the steps present in this [page](https://docs.fabric.io/android/crashlytics/build-tools.html) to disable the integration. We will improve this integration in the future so it can be easily enabled/disabled**

### Firebase
The Gnosis Safe Android App uses Firebase and your build will fail if you don't have the `google-services.json` file.

This file can be found in the Settings page of the Firebase project.

After getting access to the file, move it to the `app` module.

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