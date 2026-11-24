package com.example.vclass.data.firebase

import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

internal fun Query.observeSnapshots(metadata: MetadataChanges = MetadataChanges.EXCLUDE): Flow<QuerySnapshot> =
    callbackFlow {
        val registration = addSnapshotListener(metadata) { snapshot, error ->
            when {
                error != null -> close(error)
                snapshot != null -> trySend(snapshot)
            }
        }
        awaitClose { registration.remove() }
    }
