import { ref, computed } from 'vue';
import postsService from '../services/posts.service';
import commentsService from '../services/comments.service';

export function useComments(postId) {
    const comments = ref([]);
    const loading = ref(false);
    const error = ref(null);

    // --- O(n) STRUCTURED COMMENTS ---
    // Превръща плосък списък в дърво само с едно минаване през масива
    const structuredComments = computed(() => {
        if (!comments.value.length) return [];

        const map = new Map();
        const roots = [];

        // 1. Инициализираме мапа и добавяме поле 'replies' към всеки коментар
        // Важно: Правим shallow copy, за да не счупим реактивността, но да имаме структура
        comments.value.forEach(c => {
            // Уверяваме се, че полетата за лайк съществуват
            if (c.isLiked === undefined) c.isLiked = false;
            if (c.likeCount === undefined) c.likeCount = 0;

            map.set(c.id, { ...c, replies: [] });
        });

        // 2. Свързваме децата с родителите
        comments.value.forEach(c => {
            const node = map.get(c.id);
            if (c.parentCommentId && map.has(c.parentCommentId)) {
                const parent = map.get(c.parentCommentId);
                parent.replies.push(node);
            } else {
                roots.push(node);
            }
        });

        // 3. Сортираме корените (най-старите първи или както предпочиташ)
        return roots.sort((a, b) => new Date(a.createdAt) - new Date(b.createdAt));
    });

    // --- LOAD ---
    const loadComments = async () => {
        loading.value = true;
        try {
            const res = await postsService.getComments(postId);
            comments.value = res.data;
        } catch (err) {
            console.error(err);
            error.value = "Failed to load comments";
        } finally {
            loading.value = false;
        }
    };

    // --- ADD (Reply or New) ---
    const addComment = async (content, parentId = null) => {
        try {
            let res;
            if (parentId) {
                res = await commentsService.replyToComment(parentId, { content });
            } else {
                res = await postsService.addComment(postId, { content });
            }

            const newComment = res.data;
            // Важно: сетваме parentId ръчно, ако бекендът не го върне веднага в респонса
            if (parentId) newComment.parentCommentId = parentId;

            comments.value.push(newComment);
            return true; // Success
        } catch (err) {
            alert("Failed to add comment: " + err.message);
            return false;
        }
    };

    // --- EDIT ---
    const editComment = async (id, newContent) => {
        try {
            const res = await commentsService.update(id, { content: newContent });
            // Намираме и обновяваме в локалния масив
            const index = comments.value.findIndex(c => c.id === id);
            if (index !== -1) {
                comments.value[index].content = res.data.content;
                comments.value[index].updatedAt = res.data.updatedAt;
            }
            return true;
        } catch (err) {
            alert("Update failed");
            return false;
        }
    };

    // --- DELETE ---
    const deleteComment = async (id) => {
        try {
            await commentsService.delete(id);
            comments.value = comments.value.filter(c => c.id !== id);
        } catch (err) {
            alert("Failed to delete comment");
        }
    };

    // --- OPTIMISTIC COMMENT LIKE ---
    const toggleCommentLike = async (commentId) => {
        const comment = comments.value.find(c => c.id === commentId);
        if (!comment) return;

        const wasLiked = comment.isLiked;

        // Optimistic Update
        comment.isLiked = !wasLiked;
        comment.likeCount += wasLiked ? -1 : 1;

        try {
            if (!wasLiked) {
                await commentsService.likeComment(commentId);
            } else {
                await commentsService.unlikeComment(commentId);
            }
        } catch (err) {
            // Revert
            comment.isLiked = wasLiked;
            comment.likeCount += wasLiked ? 1 : -1;
        }
    };

    return {
        comments,
        structuredComments,
        loading,
        loadComments,
        addComment,
        editComment,
        deleteComment,
        toggleCommentLike
    };
}