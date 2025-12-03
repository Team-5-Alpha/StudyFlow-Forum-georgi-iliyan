import { ref } from 'vue';
import postsService from '../services/posts.service';

export function usePostActions(initialPost, emit) {
    const post = ref(initialPost);
    const isLiked = ref(initialPost.isLiked || false);
    const likesCount = ref(initialPost.likesCount || 0);
    const isDeleting = ref(false);
    const isLikeLoading = ref(false);

    const toggleLike = async () => {
        // Prevent duplicate requests
        if (isLikeLoading.value) return;

        // Store previous state for rollback
        const previousLiked = isLiked.value;
        const previousCount = likesCount.value;

        // Optimistic update
        isLiked.value = !isLiked.value;
        likesCount.value = isLiked.value ? likesCount.value + 1 : likesCount.value - 1;
        isLikeLoading.value = true;

        try {
            let response;
            if (isLiked.value) {
                response = await postsService.like(post.value.id);
            } else {
                response = await postsService.unlike(post.value.id);
            }

            // Update from backend response
            if (response.data) {
                post.value = response.data;
                isLiked.value = response.data.isLiked;
                likesCount.value = response.data.likesCount;
            }
        } catch (err) {
            console.error('Like action failed', err);
            // Rollback on error
            isLiked.value = previousLiked;
            likesCount.value = previousCount;
        } finally {
            isLikeLoading.value = false;
        }
    };

    const deletePost = async () => {
        isDeleting.value = true;
        try {
            await postsService.delete(post.value.id);
            emit('post-deleted', post.value.id);
        } catch (err) {
            console.error('Delete failed', err);
            alert('Failed to delete post.');
        } finally {
            isDeleting.value = false;
        }
    };

    return {
        post,
        isLiked,
        likesCount,
        isDeleting,
        isLikeLoading,
        toggleLike,
        deletePost
    };
}
