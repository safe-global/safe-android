# Heimdall Android

**WARNING: Under development: When building the application, target a test environment (eg.: TestRPC). Don't use the application with real funds!**

### Goal
The goal of this project is to provide a 2FA mechanism for your ethereum accounts. This can be achieved by using the new [Gnosis Safe](https://github.com/gnosis/gnosis-safe) the new version of our [Multisig Wallet](https://github.com/gnosis/MultiSigWallet). 

You can add multiple devices to the safe and secure your wallet by requiring a set of devices to confirm each transaction. Depending on you security setting and number of devices you can still access your funds even if you loose access to a device.

Deploying and interacting with your Gnosis Safe will also be much cheaper than with our Multisig Wallet.

### Main Features
* Deploying and Restoring Safes
* Creating and restoring account access (via mnemonic phrase)
* Transfer funds (ether or tokens)
* Management of your safe (number of required confirmations, owners,...)
* Address Book (so you don't need to memorize all addresses)
* Manage Tokens - verify token addresses that you trust so you can access their information easily
* Transaction scanning with QRCode

### Setup
The Json RPC depends on [Infura](https://infura.io/). You need to get an API key and create a file named `project_keys` with the following contents:
```
INFURA_API_KEY=<YOUR_API_KEY>
```

Replace `<YOUR_API_KEY>` with the key that you get from Infura.

### Contribute
You can contribute to this repo by creating a Pull Request or an issue. Please follow the default template set for the Pull Requests.
