package com.kimmeldevs.margaritaone.ui.viewmodel

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import com.kimmeldevs.margaritaone.data.model.NewPost
import com.kimmeldevs.margaritaone.data.model.PostDto
import com.kimmeldevs.margaritaone.data.model.PostType
import com.kimmeldevs.margaritaone.data.repository.PostRepository
import com.kimmeldevs.margaritaone.navigation.Screen
import com.kimmeldevs.margaritaone.util.Dates
import com.kimmeldevs.margaritaone.util.ImageCompressor
import io.ktor.http.ContentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

const val MAX_POST_IMAGES = 10
private const val MAX_PDF_BYTES = 10 * 1024 * 1024

/** Everything on the post form. Lives in the ViewModel so editing can pre-fill it and it survives rotation. */
data class PostForm(
    val type: PostType = PostType.ANNOUNCEMENT,
    val title: String = "",
    val body: String = "",
    val pinned: Boolean = false,
    /** Photos already uploaded (when editing) that the admin kept. */
    val existingImagePaths: List<String> = emptyList(),
    /** Newly picked photos, uploaded on save. */
    val newImageUris: List<Uri> = emptyList(),
    val existingDocumentPath: String? = null,
    val newDocumentUri: Uri? = null,
    val newDocumentName: String? = null,
    /** Publish later (#18); null = publish now / keep the original time. */
    val scheduledAtMillis: Long? = null,
    /** Event details (#12), used when type is EVENT. */
    val eventStartMillis: Long? = null,
    val eventEndMillis: Long? = null,
    val eventLocation: String = ""
) {
    val photoCount: Int get() = existingImagePaths.size + newImageUris.size
    val isDirty: Boolean
        get() = title.isNotBlank() || body.isNotBlank() || newImageUris.isNotEmpty() || newDocumentUri != null
}

data class CreatePostState(
    val form: PostForm = PostForm(),
    /** Non-null when editing an existing post. */
    val editing: PostDto? = null,
    /** The form as loaded, to tell whether an edit changed anything. */
    val original: PostForm? = null,
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val error: String? = null,
    val posted: Boolean = false
)

/** Create or edit (#3) a bulletin post: photos, PDF, schedule (#18), and event details (#12). */
class CreatePostViewModel(
    savedStateHandle: SavedStateHandle,
    private val app: Application,
    private val postRepository: PostRepository
) : ViewModel() {

    private val editId: String? = savedStateHandle[Screen.CreatePost.ARG_POST_ID]

    private val _state = MutableStateFlow(CreatePostState(isLoading = editId != null))
    val state: StateFlow<CreatePostState> = _state.asStateFlow()

    init {
        if (editId != null) {
            viewModelScope.launch {
                when (val result = loadState { postRepository.getPost(editId) ?: error("Post not found") }) {
                    is UiState.Success -> {
                        val form = result.data.toForm()
                        _state.update { it.copy(isLoading = false, editing = result.data, form = form, original = form) }
                    }
                    is UiState.Error -> _state.update { it.copy(isLoading = false, error = result.message) }
                    UiState.Loading -> Unit
                }
            }
        }
    }

    fun update(change: (PostForm) -> PostForm) = _state.update { it.copy(form = change(it.form), error = null) }

    fun addImages(uris: List<Uri>) = _state.update {
        val room = MAX_POST_IMAGES - it.form.existingImagePaths.size
        val combined = (it.form.newImageUris + uris).distinct()
        it.copy(
            form = it.form.copy(newImageUris = combined.take(room)),
            error = if (combined.size > room) "You can add up to 10 photos." else null
        )
    }

    fun setDocument(uri: Uri?) = update {
        it.copy(newDocumentUri = uri, newDocumentName = uri?.let(::displayName))
    }

    fun removeExistingDocument() = update { it.copy(existingDocumentPath = null) }

    fun submit() {
        val current = _state.value
        val form = current.form
        if (current.isSubmitting) return
        if (form.title.trim().length < 3) return _state.update { it.copy(error = "Enter a title.") }
        if (form.type == PostType.EVENT && form.eventStartMillis != null && form.eventEndMillis != null &&
            form.eventEndMillis < form.eventStartMillis
        ) return _state.update { it.copy(error = "The event can't end before it starts.") }

        _state.update { it.copy(isSubmitting = true, error = null) }
        viewModelScope.launch {
            val error = runAction {
                val newImagePaths = form.newImageUris.map { uri ->
                    postRepository.uploadAttachment(ImageCompressor.compressToJpeg(app, uri), "jpg", ContentType.Image.JPEG)
                }
                val documentPath = form.newDocumentUri?.let { uploadPdf(it) } ?: form.existingDocumentPath
                val isEvent = form.type == PostType.EVENT
                val post = NewPost(
                    type = form.type,
                    title = form.title.trim(),
                    body = form.body.trim(),
                    attachmentPath = documentPath,
                    imagePaths = form.existingImagePaths + newImagePaths,
                    pinned = form.pinned,
                    // New posts need a time; edits keep the original unless rescheduled.
                    publishedAt = form.scheduledAtMillis?.let(Dates::isoFromMillis)
                        ?: current.editing?.publishedAt
                        ?: Dates.isoFromMillis(System.currentTimeMillis()),
                    eventStart = form.eventStartMillis?.takeIf { isEvent }?.let(Dates::isoFromMillis),
                    eventEnd = form.eventEndMillis?.takeIf { isEvent }?.let(Dates::isoFromMillis),
                    eventLocation = form.eventLocation.trim().takeIf { isEvent && it.isNotEmpty() }
                )
                val editing = current.editing
                if (editing == null) {
                    postRepository.createPost(post)
                } else {
                    postRepository.updatePost(editing.id, post)
                    // Files the admin removed while editing (#5).
                    val kept = post.imagePaths.toSet() + listOfNotNull(post.attachmentPath)
                    postRepository.deleteFiles(
                        (editing.allImagePaths + listOfNotNull(editing.documentPath)).filterNot { it in kept }
                    )
                }
            }
            _state.update { it.copy(isSubmitting = false, error = error, posted = error == null) }
        }
    }

    private suspend fun uploadPdf(uri: Uri): String {
        val bytes = withContext(Dispatchers.IO) {
            app.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        } ?: error("Couldn't read the selected file.")
        require(bytes.size <= MAX_PDF_BYTES) { "PDF is too large (max 10 MB)." }
        return postRepository.uploadAttachment(bytes, "pdf", ContentType.Application.Pdf)
    }

    private fun displayName(uri: Uri): String? =
        app.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }

    private fun PostDto.toForm() = PostForm(
        type = type,
        title = title,
        body = body,
        pinned = pinned,
        existingImagePaths = allImagePaths,
        existingDocumentPath = documentPath,
        scheduledAtMillis = Dates.toMillis(publishedAt)?.takeIf { it > System.currentTimeMillis() },
        eventStartMillis = eventStart?.let(Dates::toMillis),
        eventEndMillis = eventEnd?.let(Dates::toMillis),
        eventLocation = eventLocation.orEmpty()
    )

    fun imageUrl(path: String): String = postRepository.attachmentUrl(path)

    companion object {
        val Factory = appViewModelFactory {
            CreatePostViewModel(createSavedStateHandle(), this[APPLICATION_KEY] as Application, it.postRepository)
        }
    }
}

/** Leaving should ask first only if something would be lost. */
val CreatePostState.hasUnsavedChanges: Boolean
    get() = if (original != null) form != original else form.isDirty
