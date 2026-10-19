# Upload signing

Use Play App Signing with a separate upload key per app. Google holds the distribution signing key; the developer signs uploaded AABs with the upload key. Per-app upload keys limit credential exposure across a portfolio. Maintain a secure vault inventory by package, alias, certificate fingerprint, owner, backup location and reset procedure. Never copy another app's private key into this repository.

The owner authorized a dedicated Lamb and Tiger upload key on 2026-10-19. It was generated locally as RSA 4096 / PKCS12, validity 10,000 days. Public upload certificate SHA-256:

`9D:67:4E:60:86:D7:49:F6:E5:F3:DE:5F:52:8D:04:FA:3E:0A:84:7A:15:1E:7C:BD:BC:A9:A4:96:27:1D:EB:FD`

The private key and credential record are outside Git in the owner's local application-support storage. They have restrictive local permissions. **A local copy is not an independent backup:** import the key and password into the owner's secure vault, retain a separate encrypted recovery copy and verify recovery before relying on it for ongoing releases. No backup service or CI access was provisioned by this work. This key has not yet been registered in Play Console.

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
