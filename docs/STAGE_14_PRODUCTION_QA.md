# Study Saathi — Stage 14 Production QA

This checklist protects the existing app while validating Stages 1–14. It does not change Room, Firebase, Family Sync, authentication or cloud record formats.

## Language regression

- English mode: UI/answer remains English unless the student explicitly requests a one-turn override.
- Hindi mode: explanation remains easy Hindi; formulas, symbols and proper nouns are preserved.
- Hinglish mode: natural Hindi + English mix; no fake Android Hinglish locale.
- School Medium remains independent from explanation language.

## Ask Study Saathi

- Text question works with existing offline/cache/Firebase routing.
- Voice result returns to the same Ask/Floating context.
- Photo can be captured or selected without forcing Dashboard navigation.
- Image questions use the existing bitmap loader and release image memory after completion/error.
- Exact page citation is accepted only when it matches parent-approved evidence.
- Unsupported/invented page citation is rejected by BookAnswerGroundingValidator.
- Weak/ambiguous book match falls back to normal tutoring instead of guessing a page.

## Floating Study Saathi

- Bubble remains draggable.
- Panel remains resizable and dismissible.
- Transparency preference persists.
- Chat history/new-chat behavior remains intact.
- Text, voice and photo share the same overlay answer flow.
- Camera/gallery bridge does not open the full app stack.

## Exact study context

- Active Student -> Board -> Class -> School Medium metadata is read-only.
- Subject -> Book -> Chapter -> Page comes only from approved book data.
- Text-only generic questions do not inherit stale photo evidence.
- Exact context never changes the saved explanation language.

## Smart explanation controls

Verify one-turn behavior for:

- Quick Answer
- Let's Understand
- Explain Simpler
- Show Example
- Diagram/Visual
- Exam Answer
- Test Me
- Explain in Hindi
- Explain in Hinglish
- Explain in English

Language override must not modify the student's saved preference.

## Learning memory

- Existing StudentKnowledgeGraphStore remains the source of answer/quiz/misconception signals.
- Repeat asks without assessment can become Needs Practice.
- Low quiz score or misconception can become Needs Revision.
- Strong repeated quiz evidence can remain Mastered.
- Weak-topic ordering prioritizes revision before practice before learning/mastered.

## Revision and practice intelligence

- Misconception/very low quiz -> easier re-teach.
- Needs Revision -> 2-minute revision.
- Existing pending revision -> complete pending revision.
- Needs Practice -> one-question practice check without revealing the answer first.
- Start/completion counts continue using RecommendedRevisionProgressStore.

## Parent trust and privacy

- Parent Dashboard still requires verified cloud account + secure device credential.
- Parent learning insight shows aggregate mastered/revision/practice counts and next action.
- Raw child chat transcripts/questions are not exposed by the new trust summary.
- Existing citation insights, goals, reminders, backup, cloud account and Family Workspace remain available.

## Protected connections / no schema change

- Room database version/schema unchanged.
- No Room migration added.
- Firebase Auth unchanged.
- Firebase App Check unchanged.
- Firestore/FamilyRealtimeSyncManager record format unchanged.
- Existing OCR libraries/dependencies unchanged.
- Existing Google Books key loading unchanged.

## Device matrix for final manual run

- Android 10/11 class device where available.
- Android 13/14 device.
- Android 15/16 device.
- Small phone portrait.
- Large phone portrait.
- Landscape for non-locked screens.
- Light and dark appearance.
- Overlay permission granted/revoked paths.
- Online and offline/cache paths.

## Build verification

Run from Android Studio or project terminal:

```text
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

Then perform one real-device smoke test covering Text -> Voice -> Photo -> Floating -> Book Page -> Revision -> Parent Dashboard.
