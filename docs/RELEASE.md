# Release Process

## QA
1. Decide what commit should be used for the release and create a draft release
  - This commit is normally a `master` branch commit, but there are exceptions (e.g. hotfixes)
  - The release description MUST contain a list of new features
2. Share release and a Crashlytics Beta link to that commit with the QA team
  - New features and core features (create Safe, send transactions, restore Safe) MUST be tested with the Mainnet version
  - Other features SHOULD be tested with the Rinkeby version
3. If required fix bugs and repeat process

## Build release version
1. Finalize the draft created in [QA step](#qa)
  - CI will automatically build a Mainnet and Rinkeby versions, which will be attached to the release
2. Download Mainnet apk file (prefixed with `-release`)
3. Create new version in PlayStore with downloaded Mainnet apk file
  - Check if version should be published as Alpha/Beta to signed up users first
  - Check with communications and product team for PlayStore assets
    - Version release notes
    - Updates to screenshots or store description
  - Check if there should be an announcement for the new release
4. Release new version on PlayStore
  - If enough users are using the app it is recommended to use a staged rollout over the duration of a couple days
  - If app was published as Alpha/Beta, a date MUST be set when the app should be propagated to the Release channel
  
## Monitor release
- Check Crashlytics for crashes
- Check Crashlytics for an increase in numbers of handled exceptions
- Check for unexpected backend errors triggered by the app