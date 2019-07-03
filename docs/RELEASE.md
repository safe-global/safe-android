# Release Process

## QA

1. Decide what commit should be used for the release and create a draft release
  - The description should contain a list of new features
1. Share release and a Crashlytics Beta link to that commit with the QA team
  - New features and core features (create Safe, send transactions, restore Safe) MUST be tested with the Mainnet version
  - Other features SHOULD be tested with the Rinkeby version
1. If required fix bugs and repeat process

## Build release version
1. Finalize the draft created in [QA step](#qa)
  - CI will automatically build a Mainnet and Rinkeby versions, which will be attached to the release
1. Download Mainnet apk file (prefixed with `-release`)
1. Create new version in PlayStore with downloaded Mainnet apk file