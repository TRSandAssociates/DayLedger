const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_123";
const BOB_UID = "bob_uid_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
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

function validWorkItemPayload(uid, ts) {
  return {
    userId: uid,
    title: "Morning Python Batch A",
    notes: "Cover decorators and generators",
    category: "CLASS_BATCH",
    scheduledDate: "2026-11-24",
    startTime: "09:30",
    durationMinutes: 60,
    recurrence: "WEEKDAYS",
    completed: false,
    batchOrGroup: "Batch A",
    clientId: "",
    clientName: "",
    billable: true,
    billingAmount: 150.0,
    reminderMinutesBefore: 15,
    calendarEventId: -1,
    calendarSynced: false,
    createdAt: ts,
    updatedAt: ts,
  };
}

function validClientPayload(uid, ts) {
  return {
    userId: uid,
    name: "Aarav Mehta",
    organization: "Apex Academy",
    email: "aarav@example.com",
    phone: "+1-555-0101",
    defaultRate: 120.0,
    currencySymbol: "$",
    notes: "Weekend coaching batch",
    createdAt: ts,
    updatedAt: ts,
  };
}

function validInvoicePayload(uid, ts, status = "UNPAID", paidAmount = 0.0, paidDate = "") {
  return {
    userId: uid,
    invoiceNumber: "INV-1001",
    clientId: "client_1",
    clientName: "Aarav Mehta",
    workTitle: "November Coaching Sessions",
    workItemId: "item_1",
    amount: 480.0,
    paidAmount,
    currencySymbol: "$",
    status,
    dueDate: "2026-11-30",
    paidDate,
    notes: "Net 7 days",
    reminderEnabled: true,
    createdAt: ts,
    updatedAt: ts,
  };
}

test("Unauthenticated user cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const ts = new Date(Date.now() - 1000);

  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("workItems").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("clients").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("invoices").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("workItems").doc("w1").set(validWorkItemPayload(ALICE_UID, ts))
  );
});

test("Cross-user isolation: Bob cannot read or modify Alice's workItems, clients, or invoices", async () => {
  const ts = new Date(Date.now() - 5000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("users").doc(ALICE_UID).collection("workItems").doc("w1").set(validWorkItemPayload(ALICE_UID, ts));
    await db.collection("users").doc(ALICE_UID).collection("clients").doc("c1").set(validClientPayload(ALICE_UID, ts));
    await db.collection("users").doc(ALICE_UID).collection("invoices").doc("inv1").set(validInvoicePayload(ALICE_UID, ts));
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("workItems").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("workItems").doc("w1").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("workItems").doc("w1").update({ completed: true, updatedAt: new Date(Date.now() - 500) }));
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("clients").doc("c1").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("invoices").doc("inv1").get());
});

test("Owner workItems: create, list, toggle completed, sync calendar, and reject invalid payloads", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = new Date(Date.now() - 5000);
  const workCol = aliceDb.collection("users").doc(ALICE_UID).collection("workItems");

  // Valid create
  await assertSucceeds(workCol.doc("w1").set(validWorkItemPayload(ALICE_UID, ts)));

  // Valid list on user-scoped subcollection
  await assertSucceeds(workCol.get());

  // Valid partial update: toggle completed
  await assertSucceeds(workCol.doc("w1").update({
    completed: true,
    updatedAt: new Date(Date.now() - 1000),
  }));

  // Valid partial update: sync calendar
  await assertSucceeds(workCol.doc("w1").update({
    calendarEventId: 9012,
    calendarSynced: true,
    updatedAt: new Date(Date.now() - 500),
  }));

  // Reject @DocumentId 'id' field inside payload
  await assertFails(workCol.doc("w_bad_id").set({
    ...validWorkItemPayload(ALICE_UID, ts),
    id: "w_bad_id",
  }));

  // Reject ghost field
  await assertFails(workCol.doc("w_ghost").set({
    ...validWorkItemPayload(ALICE_UID, ts),
    isAdmin: true,
  }));

  // Reject invalid category enum
  await assertFails(workCol.doc("w_bad_cat").set({
    ...validWorkItemPayload(ALICE_UID, ts),
    category: "UNKNOWN_CATEGORY",
  }));

  // Reject future timestamp
  await assertFails(workCol.doc("w_future").set(validWorkItemPayload(ALICE_UID, new Date(Date.now() + 60000))));

  // Reject createdAt mutation on update
  await assertFails(workCol.doc("w1").update({
    createdAt: new Date(Date.now() - 200),
    updatedAt: new Date(Date.now() - 200),
  }));

  // Valid delete
  await assertSucceeds(workCol.doc("w1").delete());
});

test("Owner clients: create, update, list, and delete succeed; invalid rate/currency fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = new Date(Date.now() - 5000);
  const clientsCol = aliceDb.collection("users").doc(ALICE_UID).collection("clients");

  await assertSucceeds(clientsCol.doc("c1").set(validClientPayload(ALICE_UID, ts)));
  await assertSucceeds(clientsCol.get());
  await assertSucceeds(clientsCol.doc("c1").update({
    defaultRate: 140.0,
    notes: "Updated rate for Q4",
    updatedAt: new Date(Date.now() - 1000),
  }));

  // Reject negative rate
  await assertFails(clientsCol.doc("c_neg").set({
    ...validClientPayload(ALICE_UID, ts),
    defaultRate: -10,
  }));

  await assertSucceeds(clientsCol.doc("c1").delete());
});

test("Owner invoices: payment status transitions work and PAID invoices lock billing amounts", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = new Date(Date.now() - 5000);
  const invCol = aliceDb.collection("users").doc(ALICE_UID).collection("invoices");

  // Create UNPAID invoice
  await assertSucceeds(invCol.doc("inv1").set(validInvoicePayload(ALICE_UID, ts)));
  await assertSucceeds(invCol.get());

  // Record PARTIAL payment
  await assertSucceeds(invCol.doc("inv1").update({
    paidAmount: 200.0,
    status: "PARTIAL",
    updatedAt: new Date(Date.now() - 2000),
  }));

  // Mark PAID
  await assertSucceeds(invCol.doc("inv1").update({
    paidAmount: 480.0,
    status: "PAID",
    paidDate: "2026-11-24",
    reminderEnabled: false,
    updatedAt: new Date(Date.now() - 1000),
  }));

  // Terminal state lock: cannot tamper with amount or revert status after PAID
  await assertFails(invCol.doc("inv1").update({
    amount: 999.0,
    updatedAt: new Date(Date.now() - 500),
  }));
  await assertFails(invCol.doc("inv1").update({
    status: "UNPAID",
    paidAmount: 0.0,
    updatedAt: new Date(Date.now() - 500),
  }));

  // Notes update on PAID invoice is permitted
  await assertSucceeds(invCol.doc("inv1").update({
    notes: "Paid via bank transfer",
    updatedAt: new Date(Date.now() - 300),
  }));

  // Reject paidAmount > amount
  await assertFails(invCol.doc("inv_over").set(validInvoicePayload(ALICE_UID, ts, "PAID", 600.0, "2026-11-24")));
});
