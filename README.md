# gnosis-android

The QRCode scanning is done using the [ZXing Project](https://github.com/zxing/zxing).

It scans the transaction details written in the [ERC67](https://github.com/ethereum/EIPs/issues/67) format:
```
ethereum:<address>[?value=<value>][?gas=<suggestedGas>][?data=<bytecode>]
```

A default account is created per installation.

As an example:

![alt text](https://i.imgur.com/D5sG8QH.png)

This QRCode represents the following transaction:
```
ethereum:0x89205A3A3b2A69De6Dbf7f01ED13B2108B2c43e7?gas=100000?data=0xa9059cbb00000000000000000000000000000000000000000000000000000000deadbeef0000000000000000000000000000000000000000000000000000000000000005
```

The Json RPC depends on [Infura](https://infura.io/). You need to get an API key and create a file named `project_keys` with the following contents:
```
INFURA_API_KEY = "<YOUR_API_KEY>"
```

Replace `<YOUR_API_KEY>` with the key that you get from Infura.
