# Weekly Sync-up

## Week 1 (27/03 - 31/03)

### Done

- Added an entrance for Adding Keystone Owner Key
- Resolved the issue with animate code scanning
- Currently working on parsing QR codes into Key

### To-do for next week

- Encapsulating an existing Rust library

## Week 2 (03/04 - 07/04)

### Done

- Added methods for parsing HD Key in library [keystone-sdk-android](https://github.com/KeystoneHQ/keystone-sdk-android)
- Introduced `keystone-sdk-android` in `safe-android` and parsed QR code to HD Key

### To-do for next week

- Derive public keys from HD Key
- Render keys in selection key list

## Week 3 (10/04 - 14/04)

### Done

- Import owner key for `standard` and `ledger.legacy` type

### To-do for next week

- Support scan dynamic QR code
- Import owner key for `ledger.live` type
- Add API in [keystone-sdk-android](https://github.com/KeystoneHQ/keystone-sdk-android) library to generate unsigned UR code for transaction

## Week 4 (17/04 - 21/04)

### Done

- Finish importing owner key completely

### To-do for next week

- Continue on [keystone-sdk-android](https://github.com/KeystoneHQ/keystone-sdk-android) library to generate unsigned UR code for transaction
- Show unsigned UR code when request signature