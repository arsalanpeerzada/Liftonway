# Lift On Way (Android)

The Lift On Way ride-sharing app, built from the design screenshots in the parent folder. It talks to the Spring Boot backend in `../../LiftOnWay-Backend`; a build flag swaps in an offline demo with in-memory data.

## Run it

Open this `android` folder in Android Studio and run the `app` configuration. From a terminal:

```bash
./gradlew installDebug
```

For a true sense of smoothness, try a release build (`./gradlew assembleRelease`). Debug Compose builds are much slower, especially the first launch.

## Backend

By default debug builds talk to the Lift On Way API in `../../LiftOnWay-Backend`. Start it with `./gradlew bootRun` there (no Docker needed). The emulator reaches it at `http://10.0.2.2:8080/api/v1/`. Debug builds allow plain HTTP to that address only.

- **Other server (debug):** `-Pliftonway.apiUrl=https://...` points the app elsewhere.
- **Release builds:** `-Pliftonway.releaseApiUrl=https://.../api/v1/` is the deployed API, and it must be HTTPS. Until a server exists the release default is a reserved, unreachable name, so a release build can never talk to a developer's machine.
- **Offline demo:** `-Pliftonway.fake=true` builds the original demo with in-memory data and no backend.

**What still uses in-memory data:** the wallet and payments. The profile photo also stays on the device.

**Push notifications:**
- **Setup:** register the Android app in the Firebase project with package name `io.github.arsalanpeerzada.liftonway`, download `google-services.json` into `app/` (Git ignores it) and rebuild. The build reads the four values it needs from that file and starts Firebase itself, so the google-services Gradle plugin isn't used. Without the file, push is simply off.
- **Behaviour:**
  - After sign-in the app registers the phone with the API.
  - It asks once for the notification permission on Android 13 and later.
  - Pushes arrive as data messages, so the app builds each notification itself. Tapping one opens the Notifications tab.
  - On a locked screen only "New activity on your rides" shows, because the full text names people and places.
  - Signing out stops pushes to that phone.

**Sessions:**
- The app holds a 15-minute access token and a refresh token, both encrypted with an AES key that never leaves the Android Keystore.
- When the access token runs out, the API client renews it once, one renewal at a time, and repeats the call.
- If the server has ended the session, the app returns to sign-in.
- Sign-out also ends the session on the server.
- Backups are off, and a session that can't be decrypted on a new device simply signs in again.

**Prototype shortcuts:**
- The local backend accepts the code `1234`, and so does the offline demo (it accepts any 4 digits).
- Cards must pass a Luhn check, so use a published test card number.

## Stack

Kotlin, Jetpack Compose (Material 3), Navigation 3, Hilt, Coil 3, DataStore and Lottie. The setup mirrors `../Pokedex Compose`:

- AGP 9 with built-in Kotlin
- `build-logic` convention plugins
- the same version catalogue

| Module | What's in it |
| --- | --- |
| `core:model` | Data classes, PKR and date formatting, `Policy` (4-hour cancellation cut-off, seat limit) |
| `core:data` | Fake repositories: session, rides, vehicle, notifications, wallet |
| `core:designsystem` | Theme, Plus Jakarta Sans, icons, motion helpers, Lottie player, shared components |
| `feature:*` | onboarding, auth, home, findride, publish, trips, notifications, profile, wallet |
| `app` | Navigation 3 shell, transitions, bottom tabs |
| `baselineprofile` | Journeys that generate the baseline profile for release builds |

## Tests

```bash
./gradlew :core:model:testDebugUnitTest :core:data:testDebugUnitTest :feature:trips:testDebugUnitTest
```

- `core:model`: the 4-hour cancellation rule at its boundaries, and PKR and date formatting.
- `core:data`: search window, stops staying in the right cities, recommendations, a request being confirmed, cancelling, top-up, paying with too little credit, and saved cards keeping only the last four digits. These use a fixed clock and virtual time.
- `core:data` (`RemoteApiTest`): the app's side of the API contract, using Ktor's mock engine. It covers:
  - JSON to models in Pakistan time
  - problem messages and the cancel window
  - notifications and the driver's rides
  - token renewal (once, even when calls fail together), and signing out when the session has ended but not when the server is merely down
  - the offline message
- `feature:trips`: Compose UI tests, run on the JVM with Robolectric, for ride details: the 4-hour rule, the number plate appearing once confirmed, and rating a completed ride.

## Baseline profile

The profile makes release builds start and scroll with pre-compiled code. With an emulator or device on API 33 or later connected:

```bash
./gradlew :app:generateBaselineProfile
```

This writes `app/src/release/generated/baselineProfiles/baseline-prof.txt`, which you should commit. The journeys sign up and search, and release-type builds point at the deployed API, so add `-Pliftonway.fake=true` unless a deployed server is set with `-Pliftonway.releaseApiUrl`.

**Two traps on Windows:**
- **Path length:** the profile is pulled back into a long path under `baselineprofile/build`. If it passes 260 characters, the copy fails without failing the build, and no file appears. Turning on Windows long-path support, or keeping the project in a shorter folder, avoids this.
- **Emulator storage:** each run reinstalls and recompiles the app several times. Give the emulator at least 1 GB free, or installs fail part-way through.

**Release signing:**
- Copy `keystore.properties.example` to `keystore.properties` (ignored by Git) and point it at your upload keystore. A build server can use the `LIFTONWAY_KEYSTORE_*` and `LIFTONWAY_KEY_*` environment variables instead.
- Without a key, release builds fall back to the debug key and the build warns. Those builds install for testing, but Play rejects them.
- Back up the upload keystore: Play App Signing can reset a lost upload key, but only through Google's support process.

## Animations

The Lottie files in `core/designsystem/src/main/res/raw` are generated by `tools/lottie/generate.py`. They are original vector work, so there's nothing to license.

To change one, edit the script and run:

```bash
python tools/lottie/generate.py
```

The script also writes `tools/lottie/preview.html` for a quick look in a browser.

## Decisions and known gaps

**Decisions:**
- **Font:** the designs use Gilroy, which is a paid font. Plus Jakarta Sans (SIL OFL, licence in `core/designsystem/FONT-OFL.txt`) stands in for it.
- **Currency:** PKR throughout, with sample Pakistani places. All names in the sample data are made up.

**Gaps against the designs:**
- **Price:** the Publish form has no price field, so published rides use a flat PKR 500 per seat. This needs a design decision.
- **Missing screens:** Chat, Help, Terms, Travel preferences, Verify ID and Licence confirmation aren't in the screenshots. They open placeholder pages.
- **Partly designed:**
  - The ride filter icon opens a simple sort sheet.
  - Top-up and add-card use simple sheets, since the wallet screenshots don't show those flows.
- **Payments:** card entry is a prototype only. A real build must hand card fields to the payment provider's SDK and keep only its token.
