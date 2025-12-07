<script setup>
import { ref, computed, onMounted } from 'vue';
import { RouterLink } from 'vue-router';
import { useAuthStore } from '../stores/auth.store';
import commentsService from '../services/comments.service';
import usersService from '../services/users.service';
import ConfirmModal from './ConfirmModal.vue';

const props = defineProps({
  comment: {
    type: Object,
    required: true
  },
  postId: {
    type: Number,
    required: true
  }
});

const authStore = useAuthStore();

// --- STATE ---
const authorPhotoUrl = ref(null);
const isReplying = ref(false);
const isEditing = ref(false);
const isDeleted = ref(false);
const replyContent = ref('');
const editContent = ref('');
const isDeleteModalOpen = ref(false);

// Like State
const isLiked = ref(props.comment.isLiked || false);
const likeCount = ref(props.comment.likeCount || 0);

// --- COMPUTED ---
const isAuthor = computed(() => authStore.user && authStore.user.id === props.comment.author.id);
const canEdit = computed(() => authStore.user && (authStore.user.id === props.comment.author.id || authStore.isAdmin));

const formatDate = (dateString) => {
  const date = new Date(dateString);
  const now = new Date();
  const diffMs = now - date;
  const diffHrs = diffMs / (1000 * 60 * 60);

  if (diffHrs < 24) {
    if (diffHrs < 1) return Math.max(1, Math.floor(diffMs / (1000 * 60))) + 'm';
    return Math.floor(diffHrs) + 'h';
  }
  return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
};

// --- ACTIONS ---

const toggleLike = async () => {
  if (!authStore.isAuthenticated) return;

  const wasLiked = isLiked.value;

  isLiked.value = !isLiked.value;
  likeCount.value += isLiked.value ? 1 : -1;

  try {
    if (!wasLiked) {
      await commentsService.likeComment(props.comment.id);
    } else {
      await commentsService.unlikeComment(props.comment.id);
    }
  } catch (err) {
    isLiked.value = wasLiked;
    likeCount.value += wasLiked ? 1 : -1;
    console.error("Like failed", err);
  }
};

const confirmDelete = async () => {
  try {
    await commentsService.delete(props.comment.id);
    isDeleteModalOpen.value = false;
    isDeleted.value = true; // Скриваме го от UI
  } catch (err) {
    alert("Failed to delete comment.");
  }
};

const startEdit = () => {
  editContent.value = props.comment.content;
  isEditing.value = true;
  isReplying.value = false;
};

const saveEdit = async () => {
  if (editContent.value.length < 4) return alert("Min 4 chars");
  try {
    const res = await commentsService.update(props.comment.id, { content: editContent.value });
    props.comment.content = res.data.content; // Обновяваме prop обекта (или локален state ако ползваме такъв)
    isEditing.value = false;
  } catch (err) {
    alert("Update failed");
  }
};

const startReply = () => {
  replyContent.value = '';
  isReplying.value = true;
  isEditing.value = false;
};

const submitReply = async () => {
  if (replyContent.value.length < 2) return;
  try {
    const res = await commentsService.replyToComment(props.comment.id, { content: replyContent.value });


    if (!props.comment.replies) props.comment.replies = [];
    props.comment.replies.push({
      ...res.data,
      replies: [],
      isLiked: false,
      likeCount: 0
    });

    isReplying.value = false;
    replyContent.value = '';
  } catch (err) {
    alert("Failed to reply: " + err.message);
  }
};

onMounted(async () => {
  try {
    const res = await usersService.getById(props.comment.author.id);
    if (res.data?.profilePhotoURL) authorPhotoUrl.value = res.data.profilePhotoURL;
  } catch (e) {}
});
</script>

<template>
  <div class="comment-container" v-if="!isDeleted">
    <!-- MAIN ROW -->
    <div class="comment-row">
      <!-- Avatar -->
      <router-link :to="`/profile/${comment.author.id}`" class="avatar-link">
        <div class="avatar-mini">
          <img v-if="authorPhotoUrl" :src="authorPhotoUrl" alt="Avatar" class="avatar-img" />
          <span v-else>{{ comment.author.username.charAt(0).toUpperCase() }}</span>
        </div>
      </router-link>

      <div class="comment-body">
        <!-- CONTENT (Read Mode) -->
        <div v-if="!isEditing">
          <span class="author-name">
            <router-link :to="`/profile/${comment.author.id}`">{{ comment.author.username }}</router-link>
          </span>
          <span class="comment-text">{{ comment.content }}</span>
        </div>

        <!-- EDIT FORM -->
        <div v-else class="edit-form">
          <input v-model="editContent" class="edit-input" @keyup.enter="saveEdit" />
          <div class="edit-actions">
            <button @click="saveEdit" class="action-link save">Save</button>
            <button @click="isEditing = false" class="action-link cancel">Cancel</button>
          </div>
        </div>

        <!-- META (Like, Reply, Date) -->
        <div class="comment-meta">
          <span class="date">{{ formatDate(comment.createdAt) }}</span>

          <span v-if="likeCount > 0" class="likes-label">{{ likeCount }} likes</span>

          <button @click="startReply" class="meta-btn">Reply</button>

          <template v-if="canEdit">
            <button @click="showMenu = !showMenu" class="meta-btn options-btn">•••</button>
            <!-- Simple options dropdown/inline -->
            <div class="inline-options">
              <button @click="startEdit" class="meta-btn">Edit</button>
              <button @click="isDeleteModalOpen = true" class="meta-btn delete">Delete</button>
            </div>
          </template>
        </div>
      </div>

      <!-- LIKE BUTTON -->
      <button @click="toggleLike" class="like-btn">
        <svg v-if="isLiked" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" class="heart-icon filled"><path d="M11.645 20.91l-.007-.003-.022-.012a15.247 15.247 0 01-.383-.218 25.18 25.18 0 01-4.244-3.17C4.688 15.36 2.25 12.174 2.25 8.25 2.25 5.322 4.714 3 7.75 3c1.99 0 3.969 1.356 5.25 3.34C14.281 4.356 16.261 3 18.25 3c3.036 0 5.5 2.322 5.5 5.25 0 3.925-2.438 7.111-4.739 9.256a25.175 25.175 0 01-4.244 3.17 15.247 15.247 0 01-.383.219l-.022.012-.007.004-.003.001a.752.752 0 01-.704 0l-.003-.001z" /></svg>
        <svg v-else xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="heart-icon outline"><path stroke-linecap="round" stroke-linejoin="round" d="M21 8.25c0-2.485-2.099-4.5-4.688-4.5-1.935 0-3.597 1.126-4.312 2.733-.715-1.607-2.377-2.733-4.313-2.733C5.1 3.75 3 5.765 3 8.25c0 7.22 9 12 9 12s9-4.78 9-12z" /></svg>
      </button>
    </div>

    <!-- REPLY INPUT -->
    <div v-if="isReplying" class="reply-form">
      <input v-model="replyContent" class="reply-input" placeholder="Add a reply..." @keyup.enter="submitReply" autoFocus />
      <button @click="submitReply" :disabled="!replyContent" class="post-reply-btn">Post</button>
    </div>

    <!-- NESTED REPLIES -->
    <div class="nested-replies" v-if="comment.replies && comment.replies.length > 0">
      <CommentItem
          v-for="reply in comment.replies"
          :key="reply.id"
          :comment="reply"
          :post-id="postId"
      />
    </div>

    <!-- DELETE MODAL -->
    <ConfirmModal
        :isOpen="isDeleteModalOpen"
        title="Delete Comment?"
        message="Are you sure?"
        confirmText="Delete"
        @confirm="confirmDelete"
        @cancel="isDeleteModalOpen = false"
    />
  </div>
</template>

<style scoped>
.comment-container {
  margin-bottom: 16px;
}

.comment-row {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  position: relative;
}

/* AVATAR */
.avatar-mini {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background-color: var(--color-dark);
  color: white;
  display: flex; align-items: center; justify-content: center;
  font-weight: bold; font-size: 12px; overflow: hidden;
}
.avatar-img { width: 100%; height: 100%; object-fit: cover; }

/* BODY */
.comment-body {
  flex: 1;
  font-size: 14px;
  line-height: 1.4;
}

.author-name a {
  font-weight: 700;
  color: var(--color-dark);
  text-decoration: none;
  margin-right: 6px;
}
.author-name a:hover { text-decoration: underline; }

.comment-text {
  color: #262626;
}

/* META */
.comment-meta {
  display: flex;
  gap: 12px;
  margin-top: 4px;
  align-items: center;
  font-size: 12px;
  color: #8e8e8e;
}

.meta-btn {
  background: none; border: none; padding: 0;
  color: #8e8e8e; font-size: 12px; font-weight: 600; cursor: pointer;
}
.meta-btn:hover { color: #555; }
.meta-btn.delete { color: #ef4444; margin-left: 5px; }

.inline-options { display: inline-flex; gap: 8px; margin-left: 5px; }

/* HEART ICON */
.like-btn {
  background: none; border: none; cursor: pointer; padding: 0;
  margin-top: 2px;
}
.heart-icon { width: 12px; height: 12px; }
.heart-icon.outline { color: #8e8e8e; }
.heart-icon.filled { color: #ef4444; animation: heart-pop 0.2s ease; }
@keyframes heart-pop { 0% { transform: scale(1); } 50% { transform: scale(1.3); } 100% { transform: scale(1); } }

/* REPLY FORM */
.reply-form {
  margin-left: 44px;
  margin-top: 8px;
  display: flex; align-items: center; gap: 10px;
}
.reply-input {
  flex: 1; border: none; border-bottom: 1px solid #ddd; padding: 5px; font-size: 13px; background: transparent;
}
.reply-input:focus { outline: none; border-bottom-color: var(--color-accent); }
.post-reply-btn {
  background: none; border: none; color: var(--color-accent); font-weight: 700; cursor: pointer; font-size: 13px;
}
.post-reply-btn:disabled { opacity: 0.5; }

/* NESTED COMMENTS */
.nested-replies {
  margin-left: 44px;
  margin-top: 12px;
}

/* EDIT MODE */
.edit-form { display: flex; gap: 5px; align-items: center; width: 100%; }
.edit-input { flex: 1; padding: 4px 8px; border: 1px solid #ddd; border-radius: 4px; font-size: 13px; }
.action-link { background: none; border: none; font-size: 11px; font-weight: 700; cursor: pointer; }
.save { color: var(--color-accent); }
.cancel { color: #8e8e8e; }
</style>