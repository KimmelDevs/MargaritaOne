package com.kimmeldevs.margaritaone.data.repository

import com.kimmeldevs.margaritaone.data.model.NewComment
import com.kimmeldevs.margaritaone.data.model.NewPost
import com.kimmeldevs.margaritaone.data.model.PostComment
import com.kimmeldevs.margaritaone.data.model.NewReaction
import com.kimmeldevs.margaritaone.data.model.PostReaction
import com.kimmeldevs.margaritaone.data.model.ReactionType
import com.kimmeldevs.margaritaone.data.model.PostDto
import com.kimmeldevs.margaritaone.data.model.PostType
import com.kimmeldevs.margaritaone.data.remote.Buckets
import com.kimmeldevs.margaritaone.data.remote.Tables
import com.kimmeldevs.margaritaone.util.Dates
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.storage
import io.ktor.http.ContentType
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.UUID

class PostRepository(private val client: () -> SupabaseClient) {

    suspend fun getPosts(): List<PostDto> =
        client().from(Tables.POSTS).select {
            order("pinned", Order.DESCENDING)
            order("published_at", Order.DESCENDING)
        }.decodeList()

    /** Most recent emergency post from the last [withinHours] hours, if any. */
    suspend fun latestEmergency(withinHours: Long = 72): PostDto? {
        val since = Dates.isoHoursAgo(withinHours)
        return client().from(Tables.POSTS).select {
            filter {
                eq("type", PostType.EMERGENCY)
                gte("published_at", since)
                // Admins can see scheduled posts; don't alert anyone before they go live.
                lte("published_at", Dates.isoHoursAgo(0))
            }
            order("published_at", Order.DESCENDING)
            limit(1)
        }.decodeList<PostDto>().firstOrNull()
    }

    /** Admin only (enforced by RLS). */
    suspend fun createPost(post: NewPost): PostDto =
        client().from(Tables.POSTS)
            .insert(post) { select() }
            .decodeSingle()

    suspend fun getPost(id: String): PostDto? =
        client().from(Tables.POSTS).select { filter { eq("id", id) } }.decodeSingleOrNull()

    /** Admin only (enforced by RLS). */
    suspend fun updatePost(id: String, post: NewPost) {
        client().from(Tables.POSTS).update(post) { filter { eq("id", id) } }
    }

    /** Admin only. Also removes the post's photos/PDF from storage so they don't pile up. */
    suspend fun deletePost(post: PostDto) {
        client().from(Tables.POSTS).delete { filter { eq("id", post.id) } }
        deleteFiles(post.allImagePaths + listOfNotNull(post.documentPath))
    }

    /** Admin only (storage policy). Best-effort: a leftover file is harmless. */
    suspend fun deleteFiles(paths: List<String>) {
        if (paths.isEmpty()) return
        runCatching { client().storage.from(Buckets.POST_ATTACHMENTS).delete(paths) }
    }

    suspend fun getComments(postId: String): List<PostComment> =
        client().from(Tables.POST_COMMENTS).select {
            filter { eq("post_id", postId) }
            order("created_at", Order.ASCENDING)
        }.decodeList()

    /** Number of comments per post, for the "12 comments" label. */
    suspend fun commentCounts(postIds: List<String>): Map<String, Int> {
        if (postIds.isEmpty()) return emptyMap()
        return client().from(Tables.POST_COMMENTS).select(Columns.list("post_id")) {
            filter { isIn("post_id", postIds) }
        }.decodeList<PostIdOnly>().groupingBy { it.postId }.eachCount()
    }

    suspend fun addComment(postId: String, body: String) {
        client().from(Tables.POST_COMMENTS).insert(NewComment(postId, body))
    }

    /** Own comment, or any comment for admins (RLS). */
    suspend fun deleteComment(id: String) {
        client().from(Tables.POST_COMMENTS).delete { filter { eq("id", id) } }
    }

    /** Admin only (storage policy). Returns the object path to store in posts.attachment_path. */
    suspend fun uploadAttachment(bytes: ByteArray, extension: String, contentType: ContentType): String {
        val path = "posts/${UUID.randomUUID()}.$extension"
        client().storage.from(Buckets.POST_ATTACHMENTS).upload(path, bytes) {
            this.contentType = contentType
        }
        return path
    }

    /** Posts the signed-in user has reacted to, newest first. */
    suspend fun getReactedPosts(): List<PostDto> {
        val userId = requireNotNull(client().auth.currentUserOrNull()?.id) { "Not signed in" }
        val postIds = client().from(Tables.POST_REACTIONS).select {
            filter { eq("user_id", userId) }
        }.decodeList<PostReaction>().map { it.postId }
        if (postIds.isEmpty()) return emptyList()
        return client().from(Tables.POSTS).select {
            filter { isIn("id", postIds) }
            order("published_at", Order.DESCENDING)
        }.decodeList()
    }

    /** Reactions for the given posts (everyone's, so counts can be shown). */
    suspend fun getReactions(postIds: List<String>): List<PostReaction> {
        if (postIds.isEmpty()) return emptyList()
        return client().from(Tables.POST_REACTIONS).select {
            filter { isIn("post_id", postIds) }
        }.decodeList()
    }

    /** Sets, changes, or removes ([reaction] = null) the signed-in user's reaction. */
    suspend fun setReaction(postId: String, current: ReactionType?, reaction: ReactionType?) {
        val table = client().from(Tables.POST_REACTIONS)
        val userId = requireNotNull(client().auth.currentUserOrNull()?.id) { "Not signed in" }
        when {
            reaction == null -> table.delete {
                filter { eq("post_id", postId); eq("user_id", userId) }
            }
            current == null -> table.insert(NewReaction(postId, reaction))
            else -> table.update(buildJsonObject { put("reaction", reaction.name.lowercase()) }) {
                filter { eq("post_id", postId); eq("user_id", userId) }
            }
        }
    }

    fun attachmentUrl(path: String): String =
        client().storage.from(Buckets.POST_ATTACHMENTS).publicUrl(path)
}

@Serializable
private data class PostIdOnly(@SerialName("post_id") val postId: String)
