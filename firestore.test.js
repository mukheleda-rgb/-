const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function serverTimestampFromDb() {
  return new Date(Date.now() - 1000);
}

test("Unauthenticated user cannot read or list tracks", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("tracks").get()
  );
});

test("Authenticated owner can create and read UserSettings", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = serverTimestampFromDb(aliceDb);
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      languageCode: "ar",
      darkTheme: true,
      eyeComfortMode: true,
      autoIsolateQuran: true,
      eqPreset: "WARM_OUD",
      eqBands: [200, 100, 0, 150, 250],
      bassBoostStrength: 350,
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());
});

test("Shadow field injection on UserSettings fails", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = serverTimestampFromDb(aliceDb);
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      languageCode: "ar",
      darkTheme: true,
      eyeComfortMode: true,
      autoIsolateQuran: true,
      eqPreset: "WARM_OUD",
      eqBands: [200, 100, 0, 150, 250],
      bassBoostStrength: 350,
      isAdmin: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );
});

test("Authenticated owner can create and query own CloudTrack with aligned filter", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = serverTimestampFromDb(aliceDb);
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("tracks")
      .doc("track_1")
      .set({
        userId: ALICE_UID,
        title: "Layali Al Oud",
        artist: "Tariq Ensemble",
        album: "Midnight Sessions",
        genre: "Arabic Instrumental",
        category: "ARABIC_TARAB",
        quranAudio: false,
        durationMs: 185000,
        sourceType: "STUDIO_STARTER",
        sourceUri: "starter://oud_hijaz",
        lyrics: "[00:05.00] Instrumental Maqam Hijaz",
        favorite: true,
        playCount: 3,
        accentHex: "#E8A855",
        createdAt: ts,
        updatedAt: ts,
      })
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("tracks")
      .where("userId", "==", ALICE_UID)
      .get()
  );
});

test("Quran isolation rule rejects mismatched category and quranAudio flag", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = serverTimestampFromDb(aliceDb);
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("tracks")
      .doc("bad_quran_track")
      .set({
        userId: ALICE_UID,
        title: "Surah Ar-Rahman",
        artist: "Mishary Alafasy",
        album: "Holy Quran",
        genre: "Quran",
        category: "QURAN",
        quranAudio: false,
        durationMs: 600000,
        sourceType: "STREAM_URL",
        sourceUri: "https://server8.mp3quran.net/afs/055.mp3",
        lyrics: "",
        favorite: false,
        playCount: 0,
        accentHex: "#38B28B",
        createdAt: ts,
        updatedAt: ts,
      })
  );
});

test("Cross-user access to tracks and playlists is strictly denied", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context
      .firestore()
      .collection("users")
      .doc(BOB_UID)
      .collection("playlists")
      .doc("pl_bob")
      .set({
        userId: BOB_UID,
        name: "Bob Favorites",
        description: "Private playlist",
        trackIds: ["track_1"],
        sourceApp: "Nagham",
        quranPlaylist: false,
        coverHex: "#E8A855",
        createdAt: new Date(Date.now() - 1000),
        updatedAt: new Date(Date.now() - 1000),
      });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb
      .collection("users")
      .doc(BOB_UID)
      .collection("playlists")
      .doc("pl_bob")
      .get()
  );
});
