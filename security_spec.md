# Security Specification — Nagham Player Cloud Sync

## 1. Data Invariants
1. **Strict Multi-Tenant Ownership**: Every document under `/users/{userId}`, `/users/{userId}/tracks/{trackId}`, and `/users/{userId}/playlists/{playlistId}` must belong exclusively to `request.auth.uid == userId` and carry `userId == request.auth.uid`.
2. **Quran Audio Isolation Invariant**: In `CloudTrack`, `(category == 'QURAN') == quranAudio` must always hold true on create and update, ensuring Quranic recitations can never be misclassified into music genres or vice versa.
3. **Immutable Metadata**: `userId` and `createdAt` are strictly immutable after creation across all entities.
4. **Bounded Payloads**: Every string, array, and document ID is bounded to prevent Denial-of-Wallet and resource poisoning attacks.

## 2. The "Dirty Dozen" Payloads
1. Unauthenticated read on `/users/alice_123/tracks` -> REJECTED (`isSignedIn()`).
2. Cross-user read by Alice on `/users/bob_456/tracks/track_1` -> REJECTED (`request.auth.uid == userId`).
3. Cross-user list query without `where('userId', '==', uid)` -> REJECTED (`existing().userId == request.auth.uid`).
4. Identity spoofing on create (`userId: 'bob_456'` inside `/users/alice_123/tracks/t1`) -> REJECTED (`data.userId == request.auth.uid`).
5. Shadow field injection (`isAdmin: true` on `UserSettings` create/update) -> REJECTED (`keys().hasOnly(...)` / `affectedKeys().hasOnly(...)`).
6. Immutable `createdAt` mutation on update -> REJECTED (`incoming().createdAt == existing().createdAt`).
7. Immutable `userId` reassignment on update -> REJECTED (`incoming().userId == existing().userId`).
8. Quran isolation mismatch (`category: 'QURAN', quranAudio: false`) -> REJECTED (`(data.category == 'QURAN') == data.quranAudio`).
9. Oversized lyrics payload (> 15,000 chars) -> REJECTED (`data.lyrics.size() <= 15000`).
10. Invalid Equalizer band count (`eqBands: [0, 0]`, size != 5) -> REJECTED (`data.eqBands.size() == 5`).
11. Unbounded playlist `trackIds` (> 200 items) -> REJECTED (`data.trackIds.size() <= 200`).
12. Poisoned document ID containing special characters/spaces (`track id!@#`) -> REJECTED (`isValidId()`).

## 3. Red Team Audit Summary
- **Shadow Update Test**: Passed (`hasOnly` enforced on all creates and `affectedKeys().hasOnly` on all updates).
- **PII Blanket Test**: Passed (no email/phone stored; `/users/{userId}` restricts `get` to `request.auth.uid == userId` and blocks `list`).
- **Query Trust Test**: Passed (`allow list` checks `request.auth.uid == userId && existing().userId == request.auth.uid`).
- **Value Poisoning Test**: Passed (`isValidUserSettings`, `isValidCloudTrack`, and `isValidCloudPlaylist` wrap all create and update blocks).
