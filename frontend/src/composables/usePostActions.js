import { ref, watch } from 'vue';
import postsService from '../services/posts.service';

export function usePostActions(initialPost, emit) {
    const post = ref(initialPost);
    const isLiked = ref(initialPost.likedByCurrentUser || false);
    const likesCount = ref(initialPost.likesCount || 0);
    const isDeleting = ref(false);
    const isLikeLoading = ref(false);

    watch(() => initialPost, (newPost) => {
        post.value = newPost;
        isLiked.value = newPost.likedByCurrentUser || false;
        likesCount.value = newPost.likesCount || 0;
    }, { deep: true });

    const toggleLike = async () => {
        if (isLikeLoading.value) return;

        const previousLiked = isLiked.value;
        const previousCount = likesCount.value;

        isLiked.value = !isLiked.value;
        likesCount.value = isLiked.value ? likesCount.value + 1 : likesCount.value - 1;
        isLikeLoading.value = true;

        try {
            const response = await postsService.toggleLike(post.value.id);

            if (response.data) {
                isLiked.value = response.data.likedByCurrentUser;
                likesCount.value = response.data.likesCount;
                post.value.likedByCurrentUser = response.data.likedByCurrentUser;
                post.value.likesCount = response.data.likesCount;
            }
        } catch (err) {
            console.error('Like toggle failed:', err);
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
            console.error('Delete failed:', err);
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
