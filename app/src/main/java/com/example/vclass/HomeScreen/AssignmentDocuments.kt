package com.example.vclass.HomeScreen

import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore

internal fun isFirestoreDocumentId(value: String): Boolean =
    value.isNotBlank() && '/' !in value && value != "." && value != ".." &&
        !(value.startsWith("__") && value.endsWith("__")) && value.toByteArray(Charsets.UTF_8).size <= 1500

internal fun FirebaseFirestore.assignmentDocument(classCode: String, assignmentId: String): DocumentReference {
    require(isFirestoreDocumentId(classCode) && isFirestoreDocumentId(assignmentId))
    return collection("classes").document(classCode).collection("assignments").document(assignmentId)
}
