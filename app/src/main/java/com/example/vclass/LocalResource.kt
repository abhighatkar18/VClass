package com.example.vclass

import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey

class LocalResource : RealmObject {
    @PrimaryKey
    var id: String = ""
    var name: String = ""
    var path: String = ""
    var uploadedBy: String = ""
    var classCode: String = ""
    var timestamp: Long = 0L
}
