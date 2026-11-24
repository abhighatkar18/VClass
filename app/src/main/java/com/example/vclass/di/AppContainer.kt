package com.example.vclass.di

import android.content.Context
import com.example.vclass.data.cloudinary.CloudinaryUploader
import com.example.vclass.data.firebase.AssignmentRepository
import com.example.vclass.data.firebase.AuthRepository
import com.example.vclass.data.firebase.CalendarRepository
import com.example.vclass.data.firebase.ClassroomRepository
import com.example.vclass.data.firebase.ProfileRepository
import com.example.vclass.data.gemini.GeminiChatRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import okhttp3.OkHttpClient

internal object AppContainer {
    private val firebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val uploadClient by lazy { OkHttpClient() }

    val auth by lazy { AuthRepository(firebaseAuth, firestore) }
    val classrooms by lazy { ClassroomRepository(firestore, firebaseAuth) }
    val assignments by lazy { AssignmentRepository(firestore, firebaseAuth) }

    fun calendar(context: Context) = CalendarRepository(firestore, context.applicationContext)
    fun profiles(context: Context) = ProfileRepository(firestore, firebaseAuth, context.applicationContext.contentResolver)
    fun uploader(context: Context) = CloudinaryUploader(context.applicationContext.contentResolver, uploadClient)
    fun newChat() = GeminiChatRepository()
}
