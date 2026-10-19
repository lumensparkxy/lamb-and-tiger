# Upload signing

Use Play App Signing with a separate upload key per app. Google holds the distribution signing key; the developer signs uploaded AABs with the upload key. Per-app upload keys limit credential exposure across a portfolio. Maintain a secure vault inventory by package, alias, certificate fingerprint, owner, backup location and reset procedure. Never copy another app's private key into this repository.

The owner authorized a dedicated Lamb and Tiger upload key on 2026-10-19. It was generated locally as RSA 4096 / PKCS12, validity 10,000 days. Public upload certificate SHA-256:

`9D:67:4E:60:86:D7:49:F6:E5:F3:DE:5F:52:8D:04:FA:3E:0A:84:7A:15:1E:7C:BD:BC:A9:A4:96:27:1D:EB:FD`

The private key and credential record are outside Git in the owner's local application-support storage. They have restrictive local permissions. **A local copy is not an independent backup:** import the key and password into the owner's secure vault, retain a separate encrypted recovery copy and verify recovery before relying on it for ongoing releases. No backup service or CI access was provisioned by this work. Play accepted the current version 1 bundle signed with this key on 2026-10-19; Console reports releases signed by Google Play. The distribution certificate remains to be recorded separately. The owner explicitly authorized this Alpha before independent backup and will complete that backup afterward (D068).

## Custody naming convention

Use the stable Android application ID, key purpose and key creation date:

```text
AndroidSigning/
  com.maswadkar.lambandtiger/
    play-upload/
      2026-10-19/
        com.maswadkar.lambandtiger_play-upload_2026-10-19.p12
        com.maswadkar.lambandtiger_play-upload_2026-10-19.credentials.json
        com.maswadkar.lambandtiger_play-upload_2026-10-19.certificate.pem
        README.txt
```

The local root is `~/Library/Application Support/AndroidSigning/`. The earlier `LambAndTiger/signing/` files were relocated here and the credentials record's `LAMB_UPLOAD_KEYSTORE` path updated. Key bytes, credential values, existing alias and certificate were verified unchanged; no new key was generated. Other apps' custody was not changed.

Use `play-upload` for an upload key, `app-signing` only for a self-managed distribution key, and keep `debug` separate. Dates identify key creation, not release versions. If two keys share a creation date, append the first eight certificate SHA-256 hex characters to their folder and filename stems. Preserve rotated keys in their dated folders and record their status; do not overwrite them.

For Google Drive, use `Android Signing Backups/<application-id>/` containing an encrypted archive named `<application-id>_<purpose>_key-<creation-date>_backup-<backup-date>.7z`. Encrypt filenames too and keep the archive password separately. Raw credential files must stay private. This reorganization did not create or upload a backup; independent recovery remains unverified. On another machine, update the restored credentials record's absolute keystore path.

## Build

Provide these through the local process environment or your CI secret store, never command-line password arguments or committed properties:

- `LAMB_UPLOAD_KEYSTORE`: absolute existing path outside the checkout.
- `LAMB_UPLOAD_STORE_PASSWORD`
- `LAMB_UPLOAD_KEY_ALIAS`
- `LAMB_UPLOAD_KEY_PASSWORD`
- `LAMB_UPLOAD_CERT_SHA256`: expected public certificate fingerprint above.

With the JDK/SDK environment from README, run:

```sh
python3 scripts/prepare_play_release.py --live-ads --signed
```

Signing is opt-in. Ordinary preparation stays unsigned. The helper checks the AAB's signature and exact certificate fingerprint, rejects debug signing and records the public signer identity in the evidence report. It never uploads or accepts Play agreements. Play-delivered APKs will have Google's app-signing certificate, not necessarily this upload certificate; verify both identities separately.

[Official signing and reset guidance](https://developer.android.com/studio/publish/app-signing)
