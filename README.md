# gnosis-android

The QRCode scanning is done using the [ZXing Project](https://github.com/zxing/zxing).

The application scans the transaction details written in JSON format and encoded in Base64 (does not represent final format).

The application creates a default account (per installation).

As an example:

![alt text](https://i.imgur.com/cHuN6Zm.png)

This QRCode represents the following Json:
```json
{
    "data": "",
    "gasLimit": "0x5208",
    "gasPrice": "0x01",
    "nonce": "0x00",
    "r": "0x98ff921201554726367d2be8c804a7ff89ccf285ebc57dff8ae4c44b9c19ac4a",
    "s": "0x8887321be575c8095f789dd4c743dfe42c1820f9231f98a962b210e3ac2452a3",
    "to": "0x000000000000000000000000000b9331677e6ebf",
    "v": "0x1c",
    "value": "0x0a"
}
```

which is represented in Base64:
```
ewogICAgImRhdGEiOiAiIiwKICAgICJnYXNMaW1pdCI6ICIweDUyMDgiLAogICAgImdhc1ByaWNlIjogIjB4MDEiLAogICAgIm5vbmNlIjogIjB4MDAiLAogICAgInIiOiAiMHg5OGZmOTIxMjAxNTU0NzI2MzY3ZDJiZThjODA0YTdmZjg5Y2NmMjg1ZWJjNTdkZmY4YWU0YzQ0YjljMTlhYzRhIiwKICAgICJzIjogIjB4ODg4NzMyMWJlNTc1YzgwOTVmNzg5ZGQ0Yzc0M2RmZTQyYzE4MjBmOTIzMWY5OGE5NjJiMjEwZTNhYzI0NTJhMyIsCiAgICAidG8iOiAiMHgwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDBiOTMzMTY3N2U2ZWJmIiwKICAgICJ2IjogIjB4MWMiLAogICAgInZhbHVlIjogIjB4MGEiCn0=
```
