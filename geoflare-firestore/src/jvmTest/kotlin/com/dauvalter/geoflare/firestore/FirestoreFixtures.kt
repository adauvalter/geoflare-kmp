package com.dauvalter.geoflare.firestore

import dev.gitlive.firebase.firestore.DocumentSnapshot
import dev.gitlive.firebase.firestore.QuerySnapshot
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import com.google.firebase.firestore.DocumentReference as NativeDocumentReference
import com.google.firebase.firestore.DocumentSnapshot as NativeDocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot as NativeQuerySnapshot

/** Mock only the native transport layer; exercise real GitLive snapshot wrappers. */
internal fun nativeDocument(path: String): NativeDocumentSnapshot {
    val reference = mock(NativeDocumentReference::class.java)
    `when`(reference.path).thenReturn(path)
    val document = mock(NativeDocumentSnapshot::class.java)
    `when`(document.id).thenReturn(path.substringAfterLast('/'))
    `when`(document.reference).thenReturn(reference)
    return document
}

internal fun querySnapshot(vararg documents: NativeDocumentSnapshot): QuerySnapshot {
    val snapshot = mock(NativeQuerySnapshot::class.java)
    `when`(snapshot.documents).thenReturn(documents.toList())
    return QuerySnapshot(snapshot)
}

internal fun documentSnapshot(path: String): DocumentSnapshot = querySnapshot(nativeDocument(path)).documents.single()
