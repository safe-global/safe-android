# Release Process

## Release candidate
1. Create a release task in GitHub using the "New Release" template
1. Create and push the `release` branch
1. Update APP_VERSION_NAME (x.y.z)
1. Create PR `release` -> `master` (don't forget to add `do-not-merge` label)
1. Build pipeline will produce and upload builds with `rc` prefix (x.y.z-buildNumber`rc`-buildVariant)
1. Notify QA about release candidate
1. Create pull-request for release branch in case additional work/fixes are required for release candidate and go to step 4.
1. Normal development continues targeting `master` branch.

## Publishing release
1. QA approved release candidate
1. Get release notes from Product Owner
1. Draft new Release in GithHub
   - Select release branch as target
   - Tag release using `vX.Y.Z` format
   - Add title using `Version X.Y.Z` format
   - Add release notes from Product Owner to description
1. Team will be notified in slack about new release
   > Version X.Y.Z for gnosis/safe-android is out!

1. Download mainnet apk (prefixed with `-release`) from release page (https://github.com/safe-global/safe-android/releases/tag/vX.Y.Z) and upload it to playstore
   - Check if version should be published as Alpha/Beta to signed up users first
   - Add external testers in firebase console for mainnet and rinkeby if any
   - Check with communications and product team for PlayStore assets
      - Version release notes
      - Updates to screenshots or store description
1. Publish release
   - If enough users are using the app it is recommended to use a staged rollout over the duration of a couple days
   - If app was published as Alpha/Beta, a date MUST be set when the app should be propagated to the Release channel
1. Notify the team that release was submitted using the template below:
   > @here Hi everyone! We have submitted new android app vX.Y.Z to google playstore and it will be soon available for download.

1. Merge the release branch to master branch via new pull-request

## Monitor release
- Check Crashlytics for crashes
- Check Crashlytics for an increase in numbers of handled exceptions
- Check for unexpected backend errors triggered by the app
