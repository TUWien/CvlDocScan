# DocScan
App for document scanning.

<a href='https://play.google.com/store/apps/details?id=at.ac.tuwien.caa.docscan&pcampaignid=MKT-Other-global-all-co-prtnr-py-PartBadge-Mar2515-1'><img alt='Get it on Google Play' src='https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png' width="250px"/></a>

## Authors
Fabian Hollaus,
Florian Kleber,
Markus Diem

### Build Instructions
DocScan makes use of native (C++) OpenCV and OpenCV Java API. The Java part is automatically downloaded after syncing,
the native part is imported as a git submodule from `https://github.com/hollaus/opencv_libs.git` which also automatically
downloads the resources.

### Sync and build the project

## API keys
The app makes use of two APIs that require keys which are not published in the repository and should never be provided to the public. Instead not working dummy keys are provided in the following files:
- `gradle.properties`: contains the key for Dropbox integration
- `google-services.json`: contains the key for Firebase integration (needed for OCR)

You can get the API key if you send a mail to docscan@cvl.tuwien.ac.at. Before you replace the dummy keys, assure that you do not commit the keys with the following commands:
- `git update-index --assume-unchanged google-services.json`
- `git update-index --assume-unchanged gradle.properties`

## Cheatsheet
In case you need a more detailed error message for build errors, try: ``.\gradlew clean build``

## Visual Studio Project (C++ Library)
- Optional for testing the C++ module
- C++ lib for page segmentation and focus measure
- use CMake to create a Visual Studio Project
  - source code path: DocScan/app/src/main
  - binaries path e.g.: DocScan/build2015-x64
  - Specify OpenCV_DIR
