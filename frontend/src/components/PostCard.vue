<script setup>
import { ref, computed, onMounted } from 'vue';
import { RouterLink, useRouter } from 'vue-router';

import { useAuthStore } from '../stores/auth.store';
import usersService from '../services/users.service';

// Composables
import { usePostActions } from '../composables/usePostActions';
import { usePostEditing } from '../composables/usePostEditing';

// Components
import PostContent from './PostContent.vue';
import PostTags from './PostTags.vue';
import PostActions from './PostActions.vue';
import PostComments from './PostComments.vue';
import PostEditModal from './PostEditModal.vue';
import ConfirmModal from './ConfirmModal.vue';

const props = defineProps({
  post: { type: Object, required: true }
});

const emit = defineEmits(['post-deleted']);
const router = useRouter();
const authStore = useAuthStore();

// --- Post Actions ---
const {
  post: postData,
  isLiked,
  likesCount,
  isDeleting,
  isLikeLoading,
  toggleLike,
  deletePost
} = usePostActions(props.post, emit);

// --- Post Editing ---
const { isEditing, startEdit } = usePostEditing(postData);

// --- UI State ---
const authorPhotoUrl = ref(null);
const showMenu = ref(false);
const showComments = ref(false);
const isDeleteModalOpen = ref(false);

// --- Computed ---
const isAuthor = computed(() => authStore.user && authStore.user.id === postData.value.author.id);

const formatDate = (dateString) => {
  const date = new Date(dateString);
  return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
};

// --- Handlers ---
const handlePostUpdated = (updatedData) => {
  postData.value.title = updatedData.title;
  postData.value.content = updatedData.content;
  postData.value.tags = updatedData.tags;
};

const promptDelete = () => {
  showMenu.value = false;
  isDeleteModalOpen.value = true;
};

const confirmDelete = async () => {
  await deletePost();
  isDeleteModalOpen.value = false;
};

const handleLike = () => {
  if (!authStore.user) {
    router.push('/login');
    return;
  }
  toggleLike();
};

const handleDelete = async () => {
  if (confirm('Are you sure you want to delete this post?')) {
    await deletePost();
  }
};

// --- Load author avatar ---
onMounted(async () => {
  try {
    const res = await usersService.getById(postData.value.author.id);
    if (res.data?.profilePhotoURL) authorPhotoUrl.value = res.data.profilePhotoURL;
  } catch (e) {
    console.error('Failed to load author photo', e);
  }
});
</script>

<template>
  <article class="post-card">
    <!-- Sidebar Avatar -->
    <div class="post-sidebar">
      <router-link :to="`/profile/${postData.author.id}`" class="avatar-link">
        <div class="avatar-small">
          <img v-if="authorPhotoUrl" :src="authorPhotoUrl" alt="Avatar" class="avatar-img" />
          <span v-else>{{ postData.author.username.charAt(0).toUpperCase() }}</span>
        </div>
      </router-link>
    </div>

    <!-- Main Content -->
    <div class="post-main">
      <!-- Header & Menu -->
      <div class="post-header-row">
        <div class="post-meta">
          <router-link :to="`/profile/${postData.author.id}`" class="author-name-link">
            {{ postData.author.username }}
          </router-link>
          <span class="dot">·</span>
          <span class="post-date">{{ formatDate(postData.createdAt) }}</span>
        </div>

        <!-- Menu for Author -->
        <div v-if="isAuthor" class="menu-container">
          <button @click="showMenu = !showMenu" class="menu-btn">
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" d="M6.75 12a.75.75 0 11-1.5 0 .75.75 0 011.5 0zM12.75 12a.75.75 0 11-1.5 0 .75.75 0 011.5 0zM18.75 12a.75.75 0 11-1.5 0 .75.75 0 011.5 0z" />
            </svg>
          </button>

          <div v-if="showMenu" class="dropdown-menu">
            <button @click="startEdit" class="dropdown-item edit">Edit</button>
            <button @click="promptDelete" class="dropdown-item delete">Delete</button>
          </div>
          <div v-if="showMenu" class="menu-overlay" @click="showMenu = false"></div>
        </div>
      </div>

      <!-- Post Content & Tags -->
      <PostContent :title="postData.title" :content="postData.content" />
      <PostTags :tags="postData.tags" />

      <!-- Actions Bar -->
      <PostActions
          :is-liked="isLiked"
          :likes-count="likesCount"
          :show-comments="showComments"
          :disabled="!authStore.user"
          :is-like-loading="isLikeLoading"
          @toggle-like="handleLike"
          @toggle-comments="showComments = !showComments"
      />

      <!-- Comments Section -->
      <div v-if="showComments">
        <PostComments :post-id="postData.id" />
      </div>
    </div>

    <!-- Modals -->
    <PostEditModal
        :show="isEditing"
        :post="postData"
        @close="isEditing = false"
        @updated="handlePostUpdated"
    />

    <ConfirmModal
        :isOpen="isDeleteModalOpen"
        title="Delete Discussion?"
        message="This action cannot be undone."
        confirmText="Delete Post"
        @confirm="confirmDelete"
        @cancel="isDeleteModalOpen = false"
    />
  </article>
</template>

<style scoped>
.post-card {
  background-color: var(--color-white);
  padding: 20px;
  border-radius: 16px;
  box-shadow: var(--shadow-card);
  display: flex;
  gap: 15px;
  margin-bottom: 20px;
  position: relative;
}
.post-sidebar { flex-shrink: 0; }
.avatar-small {
  width: 40px; height: 40px;
  background-color: var(--color-dark);
  color: var(--color-white);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  overflow: hidden;
}
.avatar-img { width: 100%; height: 100%; object-fit: cover; }
.post-main { flex-grow: 1; }

.post-header-row { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 8px; }
.post-meta { font-size: 14px; color: var(--color-text-muted); }
.author-name-link { text-decoration: none; color: #262626; font-weight: 700; margin-right: 5px; }
.author-name-link:hover { text-decoration: underline; }
.avatar-link { display: block; }

/* MENU */
.menu-container { position: relative; }
.menu-btn { background: none; border: none; cursor: pointer; color: #94a3b8; padding: 5px; }
.menu-btn:hover { color: var(--color-dark); }
.menu-btn svg { width: 20px; height: 20px; }
.dropdown-menu { position: absolute; top: 100%; right: 0; width: 120px; background: white; border-radius: 8px; box-shadow: 0 5px 15px rgba(0,0,0,0.1); border: 1px solid #f1f1f1; z-index: 50; overflow: hidden; }
.dropdown-item { display: block; width: 100%; padding: 10px; border: none; background: none; text-align: left; font-size: 13px; font-weight: 600; cursor: pointer; color: var(--color-dark); }
.dropdown-item:hover { background-color: #f8fafc; }
.dropdown-item.delete { color: #ef4444; }
.dropdown-item.delete:hover { background-color: #fef2f2; }
.menu-overlay { position: fixed; top: 0; left: 0; width: 100%; height: 100%; z-index: 40; }

.post-card {
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  transition: all 0.2s ease;
}

.post-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  border-color: #cbd5e1;
}

.post-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
}

.post-menu {
  display: flex;
  gap: 8px;
}

.delete-btn {
  background: none;
  border: none;
  cursor: pointer;
  font-size: 18px;
  opacity: 0.5;
  transition: opacity 0.2s;
}

.delete-btn:hover:not(:disabled) {
  opacity: 1;
}

.delete-btn:disabled {
  cursor: not-allowed;
}

.tags-container {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 12px;
}

.tag {
  display: inline-block;
  background-color: #f1f5f9;
  color: #64748b;
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
}
</style>
