package com.example.vclass.feature.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vclass.data.firebase.Profile
import com.example.vclass.data.firebase.ProfileDetails
import com.example.vclass.data.firebase.ProfileRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class ProfileState(
    val profile: Profile? = null,
    val loading: Boolean = true,
    val saving: Boolean = false,
    val saveError: String? = null,
    val editSaved: Boolean = false
)

internal class ProfileViewModel(private val uid: String, private val repository: ProfileRepository) : ViewModel() {
    private val mutableState = MutableStateFlow(ProfileState())
    val state = mutableState.asStateFlow()
    private var loadJob: Job? = null

    fun load() {
        if (loadJob?.isActive == true || mutableState.value.saving) return
        mutableState.update { it.copy(loading = true) }
        loadJob = viewModelScope.launch {
            val profile = try {
                repository.load(uid)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                null
            }
            mutableState.update { it.copy(profile = profile, loading = false) }
        }
    }

    fun saveDetails(details: ProfileDetails) {
        val edited = details.copy(name = details.name.trim())
        if (edited.name.length !in 1..120) {
            mutableState.update { it.copy(saveError = "Enter a name between 1 and 120 characters.") }
            return
        }
        save("Could not save your profile. Please try again.") { profile ->
            repository.update(uid, edited)
            mutableState.update { it.copy(profile = profile.copy(details = edited), editSaved = true) }
        }
    }

    fun savePhoto(uri: Uri) {
        save("Could not save the photo. Try a different image or retry online.") { profile ->
            val bitmap = repository.savePhoto(uid, uri)
            mutableState.update { it.copy(profile = profile.copy(photo = bitmap)) }
        }
    }

    fun consumeEditSaved() = mutableState.update { it.copy(editSaved = false) }
    fun clearSaveError() = mutableState.update { it.copy(saveError = null) }

    private fun save(failureMessage: String, operation: suspend (Profile) -> Unit) {
        val current = mutableState.value
        if (current.saving || current.loading) return
        val profile = current.profile ?: return
        mutableState.update { it.copy(saving = true, saveError = null) }
        viewModelScope.launch {
            try {
                operation(profile)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                mutableState.update { it.copy(saveError = failureMessage) }
            } finally {
                mutableState.update { it.copy(saving = false) }
            }
        }
    }
}
