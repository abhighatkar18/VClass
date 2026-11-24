# Testing

## Run locally

Use the Android and JDK setup in [Development setup](setup.md). Android instrumentation requires a signed-out disposable emulator or test device. The synthetic Firebase client fixture and an empty Gemini key are sufficient. The Android suite does not need live backends.

```powershell
.\gradlew.bat --version
.\gradlew.bat :app:assembleDebug :app:lintDebug :app:testDebugUnitTest :app:assembleDebugAndroidTest
.\gradlew.bat :app:connectedDebugAndroidTest
```

Linux and macOS can use `bash ./gradlew` with the same tasks. Add `--offline` when all Android dependencies and the wrapper distribution are already cached. Report locations are under `app/build/reports/` and `app/build/outputs/androidTest-results/`.

For the **Firestore authorization suite**, install Node 24 and use JDK 21:

```powershell
npm.cmd ci --ignore-scripts --no-audit --no-fund
npm.cmd run test:firestore
```

Use `npm` on Linux or macOS. The script starts the Firestore emulator on loopback port 8088 for the synthetic project `demo-vclass-phase2`. It may download the emulator on first use. The suite checks the expected loopback endpoint before running fixtures. Expected permission-denied messages from negative tests appear in the log. Evaluate the test summary and exit status. This suite exercises the checked-in rule file. To verify an external deployment, have an authorized maintainer compare the deployed policy and perform the [staging checks](manual-verification.md). [Firebase emulator setup](https://firebase.google.com/docs/emulator-suite/install_and_configure)

## Test layers and priorities

| Priority | Scope | Evidence and boundaries |
|---|---|---|
| P0 | Auth, permissions, assignments, profile data | Auth ViewModel uses an in-memory fake. JVM tests verify profile fields and ID boundaries. The emulator verifies Firestore rules. Real auth sessions, rules deployed to a particular project, and Cloudinary delivery require staging. |
| P1 | Classroom fetches, Gemini, reminders | Android tests cover bounded classroom reads, retry, fake Gemini responses, and UI draft behavior. JVM tests cover reminder dates, identity, and delay. SDK-level Gemini retries and WorkManager delivery require further verification. |
| P2 | UI, navigation, feedback | Compose and device tests exercise auth input, signed-out navigation, calendar date semantics, and chat controls. Signed-in tabs, roster and assignment UI, profile editing, and TalkBack are in the manual checklist. |
| P3 | Scale and compatibility | Larger data sets, Release/R8, full device matrix, and memory and rendering profiles are future checks. |

## Regression matrix

| Phase | Prior problem | Test evidence and expected result | Limitation |
|---|---|---|---|
| 1 | New profile could hold password-derived data | `UserProfileFieldsTest` asserts exact profile fields. Firestore tests reject protected fields and unauthorized reads. | Existing historical records are separate. |
| 1–2 | Weak class or roster access | `ClassMemberPermissionsTest` checks client roles. Firestore tests exercise role, join, leave, archive, and roster authorization. | Server evidence refers to the emulator and checked-in rules. |
| 2 | Assignment titles could misroute submissions | `AssignmentDocumentsTest` checks ID boundaries. `AssignmentPathsTest` verifies the actual Android Firestore reference paths. Firestore fixtures verify same-title and legacy assignments, retried drafts, and assignment-author access. | Android file-picker and historical staging records still need verification. |
| 3 | State and session behavior needed regression coverage | `AuthViewModelTest`, `AuthFormTest`, `AppNavigationTest`, and `ClassroomViewModelTest` check validation, visible recovery, and signed-out routes. | Real Firebase Auth and signed-in reload require staging. |
| 4 | Serial classroom reads, repeated reminder work, and lost chat drafts | `ClassroomIndexTest` checks bounded concurrency and failure propagation. `ClassroomViewModelTest` checks cancellation on refresh and an empty ready state. `CalendarEventsTest` and `ReminderSchedulerTest` verify date and scheduling inputs. `ChatViewModelTest` and `ChatbotContentTest` check context, errors, and draft preservation. | No new performance benchmark or WorkManager delivery test in Phase 5. |
| 4 | Calendar cells could clip or hide events | `CalendarLayoutTest` exercises the last column at a narrow width in dark mode. | Verify TalkBack, larger fonts, and additional screen sizes manually. |

## GitHub Actions

The workflow runs for pull requests, pushes to `main` or `refactor/professional-vclass`, and manual dispatch. It runs the Android build, lint, JVM tests, and instrumentation compilation in one job, and Firestore rule tests in a separate job. Debug APKs stay on the runner. The workflow uploads only verification reports and signs or publishes no apps. It needs repository Actions enabled and hosted Linux runners. Once a branch or pull request contains the workflow, review the first run and select the job names as required checks if branch rules are used. GitHub manual dispatch requires the workflow on the default branch.

## Working with Git

Use a focused feature, fix, or refactor branch. Keep implementation, tests, and documentation reviewable with small commits such as `test: cover auth and assignment regressions` or `ci: verify Android and Firestore on pull requests`. Open a pull request with the test results and any staging gaps. Review the diff and require the two verification jobs before merging when branch rules are enabled. Create release tags after an approved and verified release.

## Phase 5 verification results

Verified on November 24, 2026 using Windows, Gradle 8.13, JDK 21, an offline API 36 Android emulator, and a separate local Firestore emulator.

| Check | Baseline | Phase 5 |
|---|---|---|
| Debug and instrumentation APK builds | Passed | Passed using the synthetic Firebase configuration |
| Debug lint | 0 errors, 93 warnings | 0 errors, 93 warnings |
| JUnit tests | 9 passed | 13 passed |
| Connected Android tests | 9 passed | 21 passed |
| Firestore rules tests | 8 passed | 10 passed |
| GitHub Actions | No workflow at baseline | Passed local actionlint 1.7.12 validation. Hosted execution pending |

The final automated runs recorded zero failed and zero skipped tests. Code coverage was not measured. SDK XML, native Realm packaging and 16 KB alignment, and transitive HTTP Client lint warnings still need review. The npm install also emitted deprecation notices for test-tool dependencies. The [manual checklist](manual-verification.md) tracks staging and device checks separately.
