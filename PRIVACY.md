# Privacy Policy — Lamb and Tiger

Last updated: **19 October 2026**

This policy describes the current data practices of **Lamb and Tiger**, the Android board game with application ID `com.maswadkar.lambandtiger`. For privacy questions, contact **[maswadkar@gmail.com](mailto:maswadkar@gmail.com)**.

## Saved games and preferences

The app stores saved games and preferences on your device so that you can continue playing and retain your settings. Nearby play also stores a locally generated player identifier and connection and match information needed to reconnect with the same opponent. You do not need to create an app account.

The computer opponent calculates its moves on your device. Your Solo game is not sent to a cloud AI service to calculate those moves. The app does not include custom game analytics. Versions with advertising include Google Mobile Ads and Google's User Messaging Platform (UMP), as described below.

## Playing with a nearby friend

When you choose **Host Game** or **Join Game**, the app uses Android Bluetooth. The joining phone uses Android’s **Companion Device Manager** device picker to select the hosting phone. The app connects only to that selected companion, using a secure Bluetooth connection. Both players must compare and confirm the displayed code before game information is exchanged.

The phones exchange locally generated player and match identifiers, game actions and saved-state information needed to keep the match consistent. Android processes Bluetooth device names and addresses for device selection, pairing and connection. The app does not send these addresses or game messages to an app-operated server or to AdMob for targeting.

The host asks Android to make the phone discoverable for up to two minutes. Bluetooth connection and, for hosting, discoverability permissions are requested during setup on Android versions that require them. The app does not request location, Bluetooth scanning or nearby Wi-Fi permissions. Android’s system device picker may require Location Services to be enabled; the game does not request geographic coordinates. Solo play needs no Bluetooth connection.

The app removes its CDM associations when you leave Play Friend, and clears leftover associations when beginning a new joining request. Android Bluetooth bonds can remain; remove them through Android’s Bluetooth settings if you no longer want the phones paired. The app does not rename your phone. Saved opponent identity and game state remain available for an explicitly confirmed reconnect.

This implementation replaces the earlier Google Nearby Connections SDK. The current version does not include that SDK or its usage-diagnostics integration. Android Bluetooth and system services remain governed by the device’s settings and policies.

## Player age group

Before the game opens, the app asks for the current player’s age group without a preselected answer. It stores only the selected group (under 13, 13–15, 16–17, or 18 and over) on this device. It does not collect a date of birth or send this answer to an app-operated server or advertising service. Choose **Change player** in Settings to remove the answer and select again. Clearing app storage or uninstalling also removes it.

No game features or advertising benefits are offered for choosing an older age. Every age group receives the same restricted advertising settings. Unknown or unanswered age groups cannot trigger the app’s ad initialization or ad requests. This screen is not identity verification or parental consent.

## Advertising

Ad-enabled versions use **Google AdMob (Google Mobile Ads SDK)** to display a banner on Home and an occasional full-screen ad after a completed game when you choose Play Again or Switch Sides. Ads are not displayed over an ongoing game. If an ad is unavailable, you can continue playing. Earlier versions without advertising do not display these ads.

We configure all ad requests for child-directed treatment, limit requested ad content to Google's **G** category and request non-personalized ads. We ask only for the local age group described above; we do not collect a date of birth. The Android advertising-ID and Android AdServices identifier, attribution and topics permissions are removed from the app. These choices limit advertising behavior; they do not mean that no information is processed.

Google documents that its Mobile Ads SDK can automatically collect and share information including IP addresses (which can indicate approximate location), app/ad interactions, diagnostics and device or other identifiers, depending on configuration. Google uses SDK information for advertising, measurement, fraud prevention and security. We do not send your saved board positions or Nearby game messages to AdMob as ad-targeting data. Google's services handle advertising information under their own policies; the app developer does not set Google's retention periods.

The app uses Google's **User Messaging Platform (UMP)** to check applicable privacy requirements. Ad requests begin only after a successful privacy-status update allows them. Child/under-age signals are set separately for UMP and the advertising SDK; a privacy form is not proof of age or parental consent. Where UMP requires privacy choices, you can access **Ad privacy choices** in Settings. If the privacy check fails, ads remain unavailable and gameplay remains usable. Settings always includes this privacy-policy link.

The app stores the time of the last displayed full-screen ad on your device to limit frequency. This setting is removed when you clear app storage or uninstall the app.

For more information:

- [Google Mobile Ads SDK data disclosures](https://developers.google.com/admob/android/privacy/play-data-disclosure)
- [How Google uses information from apps that use its services](https://policies.google.com/technologies/partner-sites)
- [Google Privacy Policy](https://policies.google.com/privacy)

## Retention and removal of local information

Game data and preferences remain on your device until removed or replaced through the app's supported operations or Android's app-storage controls. The app disables Android backup for its application data.

You can remove the app's local saved games, preferences and local Nearby identity through **Android Settings** by clearing the app's storage or uninstalling it. This removes your ability to resume those local saves.

Removing information from your phone does not remove a friend's independent saved match or information already handled by Google under its own services and policies. Google's policies govern its diagnostic-data retention and controls.

## Children and families

Lamb and Tiger is intended for children and adults. The current app does not offer an app account, freeform chat or photo sharing. Nearby mode exchanges the information required for a game with the other player, as described above.

Parents and guardians can contact **[maswadkar@gmail.com](mailto:maswadkar@gmail.com)** with privacy questions, including concerns about their child's use of the app.

## Contacting us

If you email us, your message and the contact information you provide are used to respond to your request. Please avoid including sensitive personal information or information about children that is not necessary for your question.

Email: **[maswadkar@gmail.com](mailto:maswadkar@gmail.com)**

## Changes to this policy

Changes to the app's data practices will be reflected in an updated policy and applicable app and store disclosures. The date at the top of this page identifies the latest policy update.
