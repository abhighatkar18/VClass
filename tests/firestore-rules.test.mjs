import { readFileSync } from "node:fs";
import { after, beforeEach, test } from "node:test";
import { assertFails, assertSucceeds, initializeTestEnvironment } from "@firebase/rules-unit-testing";
import {
  arrayRemove, arrayUnion, collection, deleteDoc, doc, getDoc, getDocs, runTransaction,
  serverTimestamp, setDoc, updateDoc, writeBatch
} from "firebase/firestore";

const projectId = "demo-vclass-phase2";
const rules = readFileSync(new URL("../firestore.rules", import.meta.url), "utf8");
const env = await initializeTestEnvironment({ projectId, firestore: { rules } });
const classCode = "ab1234";
const classroom = ["classes", classCode];
const assignment = id => [...classroom, "assignments", id];
const submission = (id, submissionId) => [...assignment(id), "submissions", submissionId];
const profile = uid => ["users", uid];
const roster = uid => [...classroom, "roster", uid];
const dbFor = uid => env.authenticatedContext(uid, { email: uid + "@example.test" }).firestore();
const work = { title: "Week One", dueDate: null, createdBy: "creator", createdAt: 1 };
const submissionData = (uid, suffix) => ({
  studentId: uid, studentName: "Student", submittedAt: 1, fileUrl: "https://example.test/" + suffix
});

after(async () => env.cleanup());
beforeEach(async () => {
  await env.clearFirestore();
  await env.withSecurityRulesDisabled(async context => {
    const db = context.firestore();
    const batch = writeBatch(db);
    batch.set(doc(db, ...classroom), { name: "Math", creatorId: "creator", teachers: ["teacher"], members: ["creator", "student"] });
    for (const uid of ["creator", "teacher", "student", "outsider"]) {
      batch.set(doc(db, ...profile(uid)), { name: uid, email: uid + "@example.test", classes: uid === "outsider" ? [] : [classCode] });
    }
    batch.update(doc(db, ...profile("student")), { password: "synthetic-legacy-field" });
    batch.set(doc(db, ...roster("student")), { displayName: "Student" });
    batch.set(doc(db, ...assignment("Week_One")), work);
    await batch.commit();
  });
});

test("profile writes are owner-only and cannot introduce password-derived fields", async () => {
  const newUser = dbFor("new-user");
  const ownProfile = doc(newUser, ...profile("new-user"));
  await assertFails(setDoc(ownProfile, { name: "New", email: "new-user@example.test", password: "test-value" }));
  await assertSucceeds(setDoc(ownProfile, { name: "New", email: "new-user@example.test" }));
  await assertFails(updateDoc(ownProfile, { passwordHash: "test-value" }));
  await assertSucceeds(updateDoc(doc(dbFor("student"), ...profile("student")), { fullName: "Updated student" }));
  for (const uid of ["creator", "teacher", "outsider"]) {
    await assertFails(getDoc(doc(dbFor(uid), ...profile("student"))));
  }
  await assertSucceeds(getDoc(doc(dbFor("student"), ...profile("student"))));
});

test("class roster is readable only by enrolled roles and contains only display names", async () => {
  for (const uid of ["creator", "teacher", "student"]) {
    await assertSucceeds(getDocs(collection(dbFor(uid), ...classroom, "roster")));
  }
  await assertFails(getDocs(collection(dbFor("outsider"), ...classroom, "roster")));
  await assertFails(getDoc(doc(env.unauthenticatedContext().firestore(), ...roster("student"))));
  const student = dbFor("student");
  await assertFails(setDoc(doc(student, ...roster("student")), { displayName: "Student", email: "student@example.test" }));
  await assertFails(setDoc(doc(student, ...roster("teacher")), { displayName: "Other teacher" }));
  await assertSucceeds(setDoc(doc(student, ...roster("student")), { displayName: "Updated name" }));
});

test("a creator can create a class with its index and roster in one transaction", async () => {
  const db = dbFor("creator");
  const newCode = "ab5678";
  const newClass = doc(db, "classes", newCode);
  await assertSucceeds(runTransaction(db, async transaction => {
    const existing = await transaction.get(newClass);
    if (existing.exists()) throw new Error("fixture code already exists");
    transaction.set(newClass, { name: "New class", creatorId: "creator", members: ["creator"] });
    transaction.update(doc(db, ...profile("creator")), { classes: arrayUnion(newCode) });
    transaction.set(doc(collection(newClass, "roster"), "creator"), { displayName: "Creator" });
  }));
});

test("join and leave pair the private index with class membership and preserve roles", async () => {
  const db = dbFor("outsider");
  const classRef = doc(db, ...classroom);
  const index = doc(db, ...profile("outsider"));
  await assertFails(getDoc(classRef));
  await assertFails(updateDoc(classRef, { members: arrayUnion("outsider") }));
  const join = writeBatch(db);
  join.update(classRef, { members: arrayUnion("outsider") });
  join.update(index, { classes: arrayUnion(classCode) });
  join.set(doc(db, ...roster("outsider")), { displayName: "Joined" });
  await assertSucceeds(join.commit());
  await assertSucceeds(getDoc(classRef));
  await assertFails(updateDoc(classRef, { teachers: arrayUnion("outsider") }));
  const leave = writeBatch(db);
  leave.update(classRef, { members: arrayRemove("outsider") });
  leave.update(index, { classes: arrayRemove(classCode) });
  leave.delete(doc(db, ...roster("outsider")));
  await assertSucceeds(leave.commit());
  await assertFails(getDoc(doc(db, ...roster("student"))));
});

test("teachers can change existing roles, students cannot, and only creators archive", async () => {
  const student = doc(dbFor("student"), ...classroom);
  const teacher = doc(dbFor("teacher"), ...classroom);
  const creator = dbFor("creator");
  const changes = { teachers: arrayUnion("student"), members: arrayRemove("student") };
  await assertFails(updateDoc(student, changes));
  await assertSucceeds(updateDoc(teacher, changes));
  await assertFails(updateDoc(teacher, { teachers: arrayUnion("outsider") }));
  await assertFails(updateDoc(teacher, { archivedAt: serverTimestamp() }));
  await assertSucceeds(updateDoc(doc(creator, ...classroom), { archivedAt: serverTimestamp() }));
  await assertFails(getDoc(doc(creator, ...assignment("Week_One"))));
  await env.withSecurityRulesDisabled(async context => {
    const existing = await getDoc(doc(context.firestore(), ...assignment("Week_One")));
    if (!existing.exists()) throw new Error("Archiving removed a descendant");
  });
});

test("same-title and legacy assignments keep independent, immutable submissions", async () => {
  const creator = dbFor("creator");
  const student = dbFor("student");
  const ids = ["generated-a", "generated-b", "Week_One"];
  for (const id of ids.slice(0, 2)) {
    await assertSucceeds(setDoc(doc(creator, ...assignment(id)), work));
  }
  await assertFails(setDoc(doc(student, ...assignment("student-assignment")), { ...work, createdBy: "student" }));
  for (const id of ids) {
    const target = doc(student, ...submission(id, "fixed-draft"));
    await assertSucceeds(runTransaction(student, async transaction => {
      await transaction.get(doc(student, ...assignment(id)));
      await transaction.get(target);
      transaction.set(target, submissionData("student", id));
    }));
    await assertSucceeds(runTransaction(student, async transaction => {
      const existing = await transaction.get(target);
      if (existing.data()?.fileUrl !== submissionData("student", id).fileUrl) {
        throw new Error("Retry did not find the same submission");
      }
    }));
    await assertFails(updateDoc(target, { fileUrl: "https://example.test/replacement" }));
    await assertSucceeds(getDoc(target));
    await assertFails(getDocs(collection(student, ...assignment(id), "submissions")));
    const result = await assertSucceeds(getDocs(collection(creator, ...assignment(id), "submissions")));
    if (result.size !== 1 || result.docs[0].data().fileUrl !== "https://example.test/" + id) {
      throw new Error("Submission was associated with the wrong assignment");
    }
  }
});

test("missing assignment paths and submissions for another identity are denied", async () => {
  const student = dbFor("student");
  await assertFails(setDoc(doc(student, ...submission("missing-assignment", "attempt")), submissionData("student", "missing")));
  await assertFails(setDoc(doc(student, ...submission("Week_One", "attempt")), submissionData("teacher", "wrong-user")));
});

test("events are private and posting class content requires a manager", async () => {
  const student = dbFor("student");
  const teacher = dbFor("teacher");
  const eventPath = ["users", "student", "events", "event-a"];
  await assertSucceeds(setDoc(doc(student, ...eventPath), { title: "Lab", date: new Date("2027-01-08T09:00:00Z") }));
  await assertFails(getDoc(doc(teacher, ...eventPath)));
  await assertFails(deleteDoc(doc(teacher, ...eventPath)));
  await assertSucceeds(deleteDoc(doc(student, ...eventPath)));

  const announcementPath = [...classroom, "announcements", "lab"];
  const announcement = { title: "Lab", description: "Bring your notes.", timestamp: new Date() };
  await assertFails(setDoc(doc(student, ...announcementPath), { ...announcement, posterId: "student" }));
  await assertSucceeds(setDoc(doc(teacher, ...announcementPath), { ...announcement, posterId: "teacher" }));
  const resourcePath = [...classroom, "resources", "handout"];
  const resource = { name: "Lab.pdf", cloudinaryLink: "https://example.test/handout", uploadedBy: "Teacher", uploadedAt: 1 };
  await assertFails(setDoc(doc(student, ...resourcePath), resource));
  await assertSucceeds(setDoc(doc(teacher, ...resourcePath), resource));
  await assertSucceeds(setDoc(doc(teacher, ...resourcePath), resource));
  await assertFails(getDoc(doc(dbFor("outsider"), ...resourcePath)));
});
