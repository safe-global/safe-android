# gnosis-authenticator-android

Under development: When building the application, target a test environment (eg.: TestRPC)

The QRCode scanning is done using the [ZXing Project](https://github.com/zxing/zxing).

It scans the transaction details written in the [ERC67](https://github.com/ethereum/EIPs/issues/67) format:
```
ethereum:<address>[?value=<value>][?gas=<suggestedGas>][?data=<bytecode>]
```
A default account is created per installation.

The Json RPC depends on [Infura](https://infura.io/). You need to get an API key and create a file named `project_keys` with the following contents:
```
INFURA_API_KEY = "<YOUR_API_KEY>"
```

Replace `<YOUR_API_KEY>` with the key that you get from Infura.
