# Dev release keystore

`mags-dev.jks` signs the **release** APK so we can install a minified build on a device.

It is a local development key, **not** for Play Console or any production signing. Store password, key password and alias are all `mags-dev`. Replace this file before a real store upload.

CI must use this exact file (never generate a new JKS). Certificate SHA-256:

`04a999b18d0d8ecba01f398ea73d2e3ddcf5bcbcf2f3823f536c25d0446dad64`
