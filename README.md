# Gnosis Safe Android App [![Build Status](https://travis-ci.org/gnosis/safe-android.svg?branch=master)](https://travis-ci.org/gnosis/safe-android)

**WARNING: Under development. Don't use the application with real funds! Application right now targets the Rinkeby test network. Switching to mainnet (or any other ethereum network) can be done by the user but it's its responsibility in doing so.**

## Goal
The goal of this project is to provide a 2FA-enabled Ethereum Wallet. This can be achieved by using the new [Gnosis Safe](https://github.com/gnosis/gnosis-safe-contracts) (the new version of our [Multisig Wallet](https://github.com/gnosis/MultiSigWallet)). 

You can add multiple devices to the safe and secure your wallet by requiring a set of devices to confirm each transaction. Depending on you security setting and number of devices you can still access your funds even if you loose access to a device.

Deploying and interacting with your Gnosis Safe will also be much cheaper than with our Multisig Wallet.

## Roadmap
### Current Features
* Deploying and Restoring Safes
* Creating and restoring account access (via mnemonic phrase)
* Transfer funds (ether or tokens)
* Address Book (so you don't need to memorize all addresses)
* Manage Tokens - verify token addresses that you trust so you can access their information easily
* Mnemonic reveal for active account
* Fiat prices for transaction costs (USD)
* Fingerprint support
* Add/Remove owner from deployed safes

### To be added
* Trezor support
* Transaction scanning with QRCode
* Integration with external protocols/apps for signing


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


### CI setup

Travis is used for continuous integration. It requires the `google-services.json` and `debug-upload.jks`. These need to be encrypted into a single file using the travis cli.

```
tar cvf secrets.tar gnosis-upload.jks app/google-services.json app/src/dev/google-services.json
travis encrypt-file secrets.tar
```
