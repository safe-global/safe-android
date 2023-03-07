---
name: Release New Version
about: Release a new version
title: Release x.y.z
labels: infrastructure
assignees: ''
---

- [ ] Create a release task in GitHub using the “New Release” template.
- [ ] Create and push the release branch
```
git checkout master -b release
git push -u origin release
```
- [ ] APP_VERSION_NAME is updated (x.y.z)
- [ ] Create PR `release` -> `master` (add `do-not-merge` label)

- [ ] Notify QA
- [ ] QA approved release candidate build
- [ ] Product Owner approved submission

**AFTER PRODUCT OWNER APPROVAL**

- [ ] Draft new release
  - [ ] Tag release using `vX.Y.Z` format
  - [ ] Add title using `Version X.Y.Z` format
  - [ ] Add release notes from Product Owner to description
  - [ ] Publish release
- [ ] Create a new release in the Google Playstore at: https://play.google.com/console -> Release -> Production -> Create new release
  - [ ] Download mainnet apk from release page (https://github.com/gnosis/safe-android/releases/tag/vX.Y.Z) and upload it to playstore
- [ ] Notify the team that release was submitted using the template below:
```
@here Hi everyone! We have submitted a new Android app vX.Y.Z to the Google Playstore and it will be available for download soon.
```

- [ ]  Merge the release branch to master branch via new pull-request
