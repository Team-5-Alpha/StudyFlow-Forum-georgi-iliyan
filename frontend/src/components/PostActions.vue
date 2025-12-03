<script setup>
const props = defineProps({
  isLiked: {
    type: Boolean,
    required: true
  },
  likesCount: {
    type: Number,
    required: true
  },
  showComments: {
    type: Boolean,
    default: false
  },
  disabled: {
    type: Boolean,
    default: false
  },
  isLikeLoading: {
    type: Boolean,
    default: false
  }
});

const emit = defineEmits(['toggle-like', 'toggle-comments']);
</script>

<template>
  <div class="post-actions">
    <!-- LIKE BUTTON -->
    <button
        @click="$emit('toggle-like')"
        class="action-btn like-btn"
        :class="{ 'liked': isLiked, 'loading': isLikeLoading }"
        :disabled="disabled || isLikeLoading"
        aria-label="Like post"
    >
      <!-- Filled Heart (Red) -->
      <svg v-if="isLiked" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" class="heart-icon filled">
        <path d="M11.645 20.91l-.007-.003-.022-.012a15.247 15.247 0 01-.383-.218 25.18 25.18 0 01-4.244-3.17C4.688 15.36 2.25 12.174 2.25 8.25 2.25 5.322 4.714 3 7.75 3c1.99 0 3.969 1.356 5.25 3.34C14.281 4.356 16.261 3 18.25 3c3.036 0 5.5 2.322 5.5 5.25 0 3.925-2.438 7.111-4.739 9.256a25.175 25.175 0 01-4.244 3.17 15.247 15.247 0 01-.383.219l-.022.012-.007.004-.003.001a.752.752 0 01-.704 0l-.003-.001z" />
      </svg>

      <!-- Outline Heart (Grey) -->
      <svg v-else xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="heart-icon outline">
        <path stroke-linecap="round" stroke-linejoin="round" d="M21 8.25c0-2.485-2.099-4.5-4.688-4.5-1.935 0-3.597 1.126-4.312 2.733-.715-1.607-2.377-2.733-4.313-2.733C5.1 3.75 3 5.765 3 8.25c0 7.22 9 12 9 12s9-4.78 9-12z" />
      </svg>

      <span class="count">{{ likesCount }}</span>
    </button>

    <!-- COMMENT BUTTON -->
    <button
        @click="$emit('toggle-comments')"
        class="action-btn comment-btn"
        :class="{ 'active': showComments }"
    >
      <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="bubble-icon">
        <path stroke-linecap="round" stroke-linejoin="round" d="M12 20.25c4.97 0 9-3.694 9-8.25s-4.03-8.25-9-8.25S3 7.444 3 12c0 2.104.859 4.023 2.273 5.48.432.447.74 1.04.586 1.641a4.483 4.483 0 01-.923 1.785A5.969 5.969 0 006 21c1.282 0 2.47-.402 3.445-1.087.81.22 1.668.337 2.555.337z" />
      </svg>
      <span class="count">{{ showComments ? 'Hide' : 'Comments' }}</span>
    </button>
  </div>
</template>

<style scoped>
.post-actions {
  display: flex;
  gap: 15px;
  padding-top: 12px;
  border-top: 1px solid #f1f1f1;
  margin-top: 10px;
}

.action-btn {
  background: none;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 6px;
  color: #1e293b;
  padding: 6px 10px;
  border-radius: 20px;
  transition: all 0.2s ease;
  font-size: 14px;
  font-weight: 600;
}

.action-btn:hover:not(:disabled) {
  background-color: #f1f5f9;
}

.action-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.action-btn.loading {
  opacity: 0.7;
}

.comment-btn.active {
  color: var(--color-accent);
  background-color: rgba(182, 157, 116, 0.1);
}

/* ICONS */
.heart-icon, .bubble-icon {
  width: 22px;
  height: 22px;
  transition: all 0.3s ease;
}

.heart-icon.outline {
  color: #1e293b;
  stroke-width: 1.8px;
}

.heart-icon.filled {
  color: #ef4444;
  animation: heart-pop 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275);
}

.bubble-icon {
  color: inherit;
  stroke-width: 1.8px;
  transform: scaleX(-1);
}

@keyframes heart-pop {
  0% {
    transform: scale(0.7);
    opacity: 0.5;
  }
  50% {
    transform: scale(1.3);
  }
  100% {
    transform: scale(1);
    opacity: 1;
  }
}

.count {
  font-size: 14px;
  font-weight: 600;
}
</style>