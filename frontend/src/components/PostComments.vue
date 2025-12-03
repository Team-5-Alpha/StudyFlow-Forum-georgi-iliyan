<script setup>
import { ref, onMounted } from 'vue';
import { useAuthStore } from '../stores/auth.store';
import { useComments } from '../composables/useComments';
import CommentItem from './CommentItem.vue';

const props = defineProps({
  postId: {
    type: Number,
    required: true
  }
});

const authStore = useAuthStore();
const newCommentContent = ref('');
const isSubmitting = ref(false);

// Използваме composable логиката
const {
  structuredComments,
  loading,
  loadComments,
  addComment
} = useComments(props.postId);

// Зареждаме коментарите при монтиране
onMounted(() => {
  loadComments();
});

// Създаване на нов коментар (Top-level)
const handleSubmit = async () => {
  if (!newCommentContent.value.trim() || newCommentContent.value.length < 4) return;

  isSubmitting.value = true;
  const success = await addComment(newCommentContent.value);

  if (success) {
    newCommentContent.value = ''; // Изчистваме полето
  }
  isSubmitting.value = false;
};
</script>

<template>
  <div class="comments-section">

    <!-- Loading State -->
    <div v-if="loading" class="loading-indicator">
      Loading comments...
    </div>

    <!-- List of Comments -->
    <div v-else class="comments-list">
      <div v-if="structuredComments.length === 0" class="no-comments">
        No comments yet. Be the first to share your thoughts!
      </div>

      <!-- Рекурсивен рендеринг чрез CommentItem -->
      <CommentItem
          v-for="comment in structuredComments"
          :key="comment.id"
          :comment="comment"
          :post-id="postId"
      />
    </div>

    <!-- Create Comment Form -->
    <div v-if="authStore.isAuthenticated" class="comment-form-container">
      <div class="comment-form">
        <input
            v-model="newCommentContent"
            type="text"
            placeholder="Add a comment..."
            class="comment-input"
            @keyup.enter="handleSubmit"
        />
        <button
            @click="handleSubmit"
            class="btn-post"
            :disabled="newCommentContent.length < 4 || isSubmitting"
        >
          Post
        </button>
      </div>
    </div>

    <div v-else class="login-prompt">
      <router-link to="/login">Log in</router-link> to comment.
    </div>

  </div>
</template>

<style scoped>
.comments-section {
  margin-top: 15px;
  border-top: 1px solid #f1f1f1;
  padding-top: 15px;
  background-color: #fafafa; /* Леко разграничаване от поста */
  border-radius: 0 0 16px 16px; /* Заобляне само отдолу */
  padding: 15px 20px;
  margin-left: -20px; /* Компенсация за падинга на родителя */
  margin-right: -20px;
  margin-bottom: -20px;
}

.loading-indicator {
  text-align: center;
  color: #94a3b8;
  font-size: 13px;
  padding: 20px;
}

.comments-list {
  display: flex;
  flex-direction: column;
  gap: 15px;
  max-height: 500px;
  overflow-y: auto;
  margin-bottom: 15px;
}

.no-comments {
  text-align: center;
  color: #94a3b8;
  font-style: italic;
  font-size: 13px;
  padding: 10px 0;
}

/* FORM STYLES */
.comment-form-container {
  border-top: 1px solid #efefef;
  padding-top: 10px;
}

.comment-form {
  display: flex;
  align-items: center;
  gap: 10px;
  background: white;
  padding: 5px 5px 5px 15px;
  border-radius: 25px;
  border: 1px solid #e2e8f0;
  transition: border-color 0.2s;
}

.comment-form:focus-within {
  border-color: var(--color-accent);
}

.comment-input {
  flex: 1;
  border: none;
  font-size: 14px;
  background: transparent;
  outline: none;
}

.comment-input::placeholder {
  color: #94a3b8;
}

.btn-post {
  background: none;
  border: none;
  cursor: pointer;
  color: var(--color-accent);
  font-weight: 700;
  font-size: 14px;
  padding: 6px 12px;
  transition: opacity 0.2s;
}

.btn-post:disabled {
  opacity: 0.5;
  cursor: default;
}

.login-prompt {
  font-size: 13px;
  color: #64748b;
  text-align: center;
  margin: 10px 0;
}

.login-prompt a {
  color: var(--color-dark);
  font-weight: 700;
  text-decoration: none;
}
</style>