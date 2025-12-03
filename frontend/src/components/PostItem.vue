<script setup>
import { ref, computed, onMounted } from 'vue';
import { useAuthStore } from '../stores/auth.store';
import postsService from '../services/posts.service';
import commentsService from '../services/comments.service';
import usersService from '../services/users.service';
import tagsService from '../services/tags.service';
import ConfirmModal from './ConfirmModal.vue';

const props = defineProps({
  post: { type: Object, required: true }
});

const emit = defineEmits(['post-deleted']);
const authStore = useAuthStore();

// UI State
const comments = ref([]);
const showComments = ref(false);
const commentsLoading = ref(false);
const newCommentContent = ref('');
const isSubmittingComment = ref(false);
const authorPhotoUrl = ref(null);

// Menu & Edit Post State
const showMenu = ref(false);
const isEditingPost = ref(false);
const editPostForm = ref({ title: '', content: '', tags: [] });
const isDeleteModalOpen = ref(false);
const availableTags = ref([]);

// Comment Edit & Delete State
const replyingTo = ref(null);
const editContent = ref('');
const editingCommentId = ref(null);
const commentInputRef = ref(null);
const isDeleteCommentModalOpen = ref(false);
const commentToDeleteId = ref(null);

// Likes State (Post)
const localLikesCount = ref(props.post.likesCount);
const isLikedLocal = ref(props.post.isLiked || false); // Инициализираме от бекенда ако го има

const isAuthor = computed(() => {
  return authStore.user && authStore.user.id === props.post.author.id;
});

const isEditTitleValid = computed(() => editPostForm.value.title.length >= 16 && editPostForm.value.title.length <= 64);
const isEditContentValid = computed(() => editPostForm.value.content.length >= 32);

const getHintClass = (text, isValid) => {
  if (!text || text.length === 0) return '';
  return isValid ? 'valid-hint' : 'invalid-hint';
};

const formatDate = (dateString) => {
  const date = new Date(dateString);
  return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
};

onMounted(async () => {
  try {
    const userResponse = await usersService.getById(props.post.author.id);
    if (userResponse.data?.profilePhotoURL) authorPhotoUrl.value = userResponse.data.profilePhotoURL;
  } catch (err) {}
});

const structuredComments = computed(() => {
  const roots = [];
  const childrenMap = {};
  comments.value.forEach(c => { childrenMap[c.id] = []; });
  comments.value.forEach(c => {
    if (c.parentCommentId && childrenMap[c.parentCommentId]) childrenMap[c.parentCommentId].push(c);
    else roots.push(c);
  });
  roots.sort((a, b) => new Date(a.createdAt) - new Date(b.createdAt));
  return roots.map(root => ({ ...root, replies: childrenMap[root.id] || [] }));
});

// --- POST ACTIONS ---
const openDeleteModal = () => { showMenu.value = false; isDeleteModalOpen.value = true; };
const confirmDeletePost = async () => {
  try { await postsService.delete(props.post.id); isDeleteModalOpen.value = false; emit('post-deleted', props.post.id); }
  catch (err) { alert("Failed to delete post."); }
};

const startEditPost = async () => {
  showMenu.value = false;
  editPostForm.value = { title: props.post.title, content: props.post.content, tags: [...(props.post.tags || [])] };
  try { const res = await tagsService.getAll(); availableTags.value = res.data; } catch(e) {}
  isEditingPost.value = true;
};
const toggleEditTag = (t) => { if(editPostForm.value.tags.includes(t)) editPostForm.value.tags = editPostForm.value.tags.filter(x=>x!==t); else editPostForm.value.tags.push(t); };
const cancelEditPost = () => { isEditingPost.value = false; };
const saveEditPost = async () => {
  if (!isEditTitleValid.value || !isEditContentValid.value) return;
  try {
    const r = await postsService.update(props.post.id, editPostForm.value);
    props.post.title = r.data.title; props.post.content = r.data.content; props.post.tags = r.data.tags;
    isEditingPost.value = false;
  } catch (err) { alert("Update failed: " + err.message); }
};

// --- POST LIKE ---
const isRequestPending = ref(false);

const toggleLike = async () => {
  if (!authStore.isAuthenticated || isRequestPending.value) return;
  isRequestPending.value = true;

  // Optimistic update
  const optimisticLikeState = !isLikedLocal.value;
  const optimisticLikesCount =
      optimisticLikeState ? localLikesCount.value + 1 : localLikesCount.value - 1;

  const prevLikeState = isLikedLocal.value;
  const prevLikesCount = localLikesCount.value;

  isLikedLocal.value = optimisticLikeState;
  localLikesCount.value = Math.max(0, optimisticLikesCount);

  try {
    if (optimisticLikeState) {
      await postsService.like(props.post.id);
    } else {
      await postsService.unlike(props.post.id);
    }
  } catch (err) {
    console.error("Like action failed", err);

    // Rollback state if backend failed
    isLikedLocal.value = prevLikeState;
    localLikesCount.value = prevLikesCount;
  }

  isRequestPending.value = false;
};


// --- COMMENTS LOGIC ---
const toggleComments = async () => { showComments.value = !showComments.value; if (showComments.value && comments.value.length === 0) loadComments(); };
const loadComments = async () => { commentsLoading.value = true; try { const res = await postsService.getComments(props.post.id); comments.value = res.data; } catch (e) { console.error(e); } finally { commentsLoading.value = false; } };
const prepareReply = (c) => { cancelEdit(); replyingTo.value = c; if(commentInputRef.value) commentInputRef.value.focus(); };
const cancelReply = () => { replyingTo.value = null; newCommentContent.value = ''; };

const submitComment = async () => {
  if (newCommentContent.value.length < 4) return;
  isSubmittingComment.value = true;
  try {
    let nc;
    if (replyingTo.value) { const r = await commentsService.replyToComment(replyingTo.value.id, { content: newCommentContent.value }); nc = r.data; nc.parentCommentId = replyingTo.value.id; }
    else { const r = await postsService.addComment(props.post.id, { content: newCommentContent.value }); nc = r.data; }
    nc.likeCount = 0; nc.isLiked = false;
    comments.value.push(nc); newCommentContent.value = ''; replyingTo.value = null;
  } catch (e) { alert("Failed: " + e.message); } finally { isSubmittingComment.value = false; }
};

// --- COMMENT EDIT & DELETE ---
const startEdit = (c) => { cancelReply(); editingCommentId.value = c.id; editContent.value = c.content; };
const cancelEdit = () => { editingCommentId.value = null; editContent.value = ''; };

const submitEdit = async (cid) => {
  if (editContent.value.length < 4) return alert("Comment must be at least 4 chars");
  try {
    const r = await commentsService.update(cid, { content: editContent.value });
    const idx = comments.value.findIndex(c => c.id === cid);
    if (idx !== -1) { comments.value[idx].content = r.data.content; comments.value[idx].updatedAt = r.data.updatedAt; }
    cancelEdit();
  } catch (e) { alert("Update failed"); }
};

const promptDeleteComment = (id) => { commentToDeleteId.value = id; isDeleteCommentModalOpen.value = true; };
const confirmDeleteComment = async () => {
  try {
    await commentsService.delete(commentToDeleteId.value);
    comments.value = comments.value.filter(c => c.id !== commentToDeleteId.value);
    isDeleteCommentModalOpen.value = false;
  } catch (err) { alert("Failed to delete comment."); }
};

// --- COMMENT LIKE (FIXED) ---
const toggleCommentLike = async (commentObj) => {
  if (!authStore.isAuthenticated) return;

  // Намираме оригинала
  const realComment = comments.value.find(c => c.id === commentObj.id);
  if (!realComment) return;

  // Init
  if (realComment.isLiked === undefined) realComment.isLiked = false;
  if (realComment.likeCount === undefined) realComment.likeCount = 0;

  try {
    if (!realComment.isLiked) {
      // 1. Like Request
      await commentsService.likeComment(realComment.id);

      // 2. Update UI
      realComment.isLiked = true;
      realComment.likeCount++;
    } else {
      // 1. Unlike Request
      await commentsService.unlikeComment(realComment.id);

      // 2. Update UI
      realComment.isLiked = false;
      realComment.likeCount = Math.max(0, realComment.likeCount - 1);
    }
  } catch (e) {
    console.error("Comment like failed", e);
  }
};

const canEdit = (c) => authStore.user && authStore.user.id === c.author.id;
</script>

<template>
  <article class="post-card">
    <div class="post-sidebar">
      <router-link :to="`/profile/${post.author.id}`" class="avatar-link">
        <div class="avatar-small">
          <img v-if="authorPhotoUrl" :src="authorPhotoUrl" alt="User Avatar" class="avatar-img" />
          <span v-else>{{ post.author.username.charAt(0).toUpperCase() }}</span>
        </div>
      </router-link>
    </div>

    <div class="post-main">
      <div class="post-header-row">
        <div class="post-meta">
          <router-link :to="`/profile/${post.author.id}`" class="author-name-link">{{ post.author.username }}</router-link>
          <span class="dot">·</span>
          <span class="post-date">{{ formatDate(post.createdAt) }}</span>
        </div>

        <div v-if="isAuthor" class="menu-container">
          <button @click="showMenu = !showMenu" class="menu-btn">
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" d="M6.75 12a.75.75 0 11-1.5 0 .75.75 0 011.5 0zM12.75 12a.75.75 0 11-1.5 0 .75.75 0 011.5 0zM18.75 12a.75.75 0 11-1.5 0 .75.75 0 011.5 0z" /></svg>
          </button>
          <div v-if="showMenu" class="dropdown-menu">
            <button @click="startEditPost" class="dropdown-item edit">
              <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" d="M16.862 4.487l1.687-1.688a1.875 1.875 0 112.652 2.652L10.582 16.07a4.5 4.5 0 01-1.897 1.13L6 18l.8-2.685a4.5 4.5 0 011.13-1.897l8.932-8.931zm0 0L19.5 7.125M18 14v4.75A2.25 2.25 0 0115.75 21H5.25A2.25 2.25 0 013 18.75V8.25A2.25 2.25 0 015.25 6H10" /></svg>
              Edit
            </button>
            <button @click="openDeleteModal" class="dropdown-item delete">
              <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" d="M14.74 9l-.346 9m-4.788 0L9.26 9m9.968-3.21c.342.052.682.107 1.022.166m-1.022-.165L18.16 19.673a2.25 2.25 0 01-2.244 2.077H8.084a2.25 2.25 0 01-2.244-2.077L4.772 5.79m14.456 0a48.108 48.108 0 00-3.478-.397m-12 .562c.34-.059.68-.114 1.022-.165m0 0a48.11 48.11 0 013.478-.397m7.5 0v-.916c0-1.18-.91-2.164-2.09-2.201a51.964 51.964 0 00-3.32 0c-1.18.037-2.09 1.022-2.09 2.201v.916m7.5 0a48.667 48.667 0 00-7.5 0" /></svg>
              Delete
            </button>
          </div>
          <div v-if="showMenu" class="menu-overlay" @click="showMenu = false"></div>
        </div>
      </div>

      <div v-if="!isEditingPost">
        <h3 class="post-title">{{ post.title }}</h3>
        <p class="post-content">{{ post.content }}</p>
        <div class="tags-container" v-if="post.tags && post.tags.length">
          <span v-for="tag in post.tags" :key="tag" class="tag">#{{ tag }}</span>
        </div>
      </div>

      <div v-else class="edit-post-form">
        <div class="edit-group">
          <label class="edit-label">Title</label>
          <input v-model="editPostForm.title" type="text" class="edit-input title-edit" />
          <p class="input-hint" :class="getHintClass(editPostForm.title, isEditTitleValid)">Must be 16-64 chars.</p>
        </div>
        <div class="edit-group">
          <label class="edit-label">Content</label>
          <textarea v-model="editPostForm.content" rows="4" class="edit-input content-edit"></textarea>
          <p class="input-hint" :class="getHintClass(editPostForm.content, isEditContentValid)">Must be at least 32 chars.</p>
        </div>
        <div class="edit-group">
          <label class="edit-label">Tags</label>
          <div class="tags-wrapper">
            <button type="button" v-for="tag in availableTags" :key="tag.id" @click="toggleEditTag(tag.name)" class="tag-chip" :class="{ 'selected': editPostForm.tags.includes(tag.name) }">{{ tag.name }}</button>
          </div>
        </div>
        <div class="edit-actions">
          <button @click="saveEditPost" class="btn-save" :disabled="!isEditTitleValid || !isEditContentValid">Save Changes</button>
          <button @click="cancelEditPost" class="btn-cancel">Cancel</button>
        </div>
      </div>

      <div class="post-actions">
        <button @click="toggleLike" class="action-btn like-btn">
          <svg v-if="isLikedLocal" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" class="heart-icon filled"><path d="M11.645 20.91l-.007-.003-.022-.012a15.247 15.247 0 01-.383-.218 25.18 25.18 0 01-4.244-3.17C4.688 15.36 2.25 12.174 2.25 8.25 2.25 5.322 4.714 3 7.75 3c1.99 0 3.969 1.356 5.25 3.34C14.281 4.356 16.261 3 18.25 3c3.036 0 5.5 2.322 5.5 5.25 0 3.925-2.438 7.111-4.739 9.256a25.175 25.175 0 01-4.244 3.17 15.247 15.247 0 01-.383.219l-.022.012-.007.004-.003.001a.752.752 0 01-.704 0l-.003-.001z" /></svg>
          <svg v-else xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="heart-icon outline"><path stroke-linecap="round" stroke-linejoin="round" d="M21 8.25c0-2.485-2.099-4.5-4.688-4.5-1.935 0-3.597 1.126-4.312 2.733-.715-1.607-2.377-2.733-4.313-2.733C5.1 3.75 3 5.765 3 8.25c0 7.22 9 12 9 12s9-4.78 9-12z" /></svg>
          <span class="count">{{ localLikesCount }}</span>
        </button>
        <button @click="toggleComments" class="action-btn comment-btn">
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="bubble-icon"><path stroke-linecap="round" stroke-linejoin="round" d="M12 20.25c4.97 0 9-3.694 9-8.25s-4.03-8.25-9-8.25S3 7.444 3 12c0 2.104.859 4.023 2.273 5.48.432.447.74 1.04.586 1.641a4.483 4.483 0 01-.923 1.785A5.969 5.969 0 006 21c1.282 0 2.47-.402 3.445-1.087.81.22 1.668.337 2.555.337z" /></svg>
          <span class="count">{{ showComments ? 'Hide' : 'Comments' }}</span>
        </button>
      </div>

      <div v-if="showComments" class="comments-section">
        <div v-if="commentsLoading" class="loading-small">Loading comments...</div>
        <div v-else class="comments-list">
          <div v-if="comments.length === 0" class="no-comments">No comments yet.</div>
          <div v-for="root in structuredComments" :key="root.id" class="comment-block">
            <div class="comment-item">
              <div v-if="editingCommentId !== root.id" class="comment-view">
                <router-link :to="`/profile/${root.author.id}`" class="comment-author-link">{{ root.author.username }}</router-link>
                <span class="comment-text">{{ root.content }}</span>

                <div class="comment-meta">
                  <button @click="toggleCommentLike(root)" class="meta-btn like-comment-btn">
                    <svg v-if="root.isLiked" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" class="mini-heart filled-heart"><path d="M11.645 20.91l-.007-.003-.022-.012a15.247 15.247 0 01-.383-.218 25.18 25.18 0 01-4.244-3.17C4.688 15.36 2.25 12.174 2.25 8.25 2.25 5.322 4.714 3 7.75 3c1.99 0 3.969 1.356 5.25 3.34C14.281 4.356 16.261 3 18.25 3c3.036 0 5.5 2.322 5.5 5.25 0 3.925-2.438 7.111-4.739 9.256a25.175 25.175 0 01-4.244 3.17 15.247 15.247 0 01-.383.219l-.022.012-.007.004-.003.001a.752.752 0 01-.704 0l-.003-.001z" /></svg>
                    <svg v-else xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="mini-heart outline-heart"><path stroke-linecap="round" stroke-linejoin="round" d="M21 8.25c0-2.485-2.099-4.5-4.688-4.5-1.935 0-3.597 1.126-4.312 2.733-.715-1.607-2.377-2.733-4.313-2.733C5.1 3.75 3 5.765 3 8.25c0 7.22 9 12 9 12s9-4.78 9-12z" /></svg>
                    <span v-if="root.likeCount > 0">{{ root.likeCount }}</span>
                  </button>

                  <button @click="prepareReply(root)" class="meta-btn">Reply</button>
                  <template v-if="canEdit(root)">
                    <span class="dot">·</span>
                    <button @click="startEdit(root)" class="meta-btn edit-text-btn">Edit</button>
                    <span class="dot">·</span>
                    <button @click="promptDeleteComment(root.id)" class="meta-btn delete-text-btn">Delete</button>
                  </template>
                </div>
              </div>
              <div v-else class="edit-mode"><input v-model="editContent" class="edit-input" @keyup.enter="submitEdit(root.id)" placeholder="Edit comment..." /><div class="edit-actions-small"><button @click="submitEdit(root.id)" class="save-small-btn">Save</button><button @click="cancelEdit" class="cancel-small-btn">Cancel</button></div></div>
            </div>

            <div v-if="root.replies.length > 0" class="replies-list">
              <div v-for="reply in root.replies" :key="reply.id" class="comment-item reply-item">
                <div v-if="editingCommentId !== reply.id" class="comment-view">
                  <router-link :to="`/profile/${reply.author.id}`" class="comment-author-link">{{ reply.author.username }}</router-link>
                  <span class="comment-text">{{ reply.content }}</span>

                  <div class="comment-meta">
                    <button @click="toggleCommentLike(reply)" class="meta-btn like-comment-btn">
                      <svg v-if="reply.isLiked" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" class="mini-heart filled-heart"><path d="M11.645 20.91l-.007-.003-.022-.012a15.247 15.247 0 01-.383-.218 25.18 25.18 0 01-4.244-3.17C4.688 15.36 2.25 12.174 2.25 8.25 2.25 5.322 4.714 3 7.75 3c1.99 0 3.969 1.356 5.25 3.34C14.281 4.356 16.261 3 18.25 3c3.036 0 5.5 2.322 5.5 5.25 0 3.925-2.438 7.111-4.739 9.256a25.175 25.175 0 01-4.244 3.17 15.247 15.247 0 01-.383.219l-.022.012-.007.004-.003.001a.752.752 0 01-.704 0l-.003-.001z" /></svg>
                      <svg v-else xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="mini-heart outline-heart"><path stroke-linecap="round" stroke-linejoin="round" d="M21 8.25c0-2.485-2.099-4.5-4.688-4.5-1.935 0-3.597 1.126-4.312 2.733-.715-1.607-2.377-2.733-4.313-2.733C5.1 3.75 3 5.765 3 8.25c0 7.22 9 12 9 12s9-4.78 9-12z" /></svg>
                      <span v-if="reply.likeCount > 0">{{ reply.likeCount }}</span>
                    </button>

                    <template v-if="canEdit(reply)">
                      <button @click="startEdit(reply)" class="meta-btn edit-text-btn">Edit</button>
                      <span class="dot">·</span>
                      <button @click="promptDeleteComment(reply.id)" class="meta-btn delete-text-btn">Delete</button>
                    </template>
                  </div>
                </div>
                <div v-else class="edit-mode"><input v-model="editContent" class="edit-input" @keyup.enter="submitEdit(reply.id)" placeholder="Edit reply..." /><div class="edit-actions-small"><button @click="submitEdit(reply.id)" class="save-small-btn">Save</button><button @click="cancelEdit" class="cancel-small-btn">Cancel</button></div></div>
              </div>
            </div>
          </div>
        </div>
        <div v-if="authStore.isAuthenticated" class="comment-form-container">
          <div v-if="replyingTo" class="replying-bar"><span>Replying to <strong>{{ replyingTo.author.username }}</strong></span><button @click="cancelReply" class="close-reply">✕</button></div>
          <div class="comment-form"><input ref="commentInputRef" v-model="newCommentContent" type="text" :placeholder="replyingTo ? 'Add a reply...' : 'Add a comment...'" class="comment-input" @keyup.enter="submitComment" /><button @click="submitComment" class="btn-post-text" :disabled="newCommentContent.length < 4 || isSubmittingComment">Post</button></div>
        </div>
        <div v-else class="login-prompt"><router-link to="/login">Log in</router-link> to comment.</div>
      </div>
    </div>

    <ConfirmModal :isOpen="isDeleteModalOpen" title="Delete Post?" message="Are you sure you want to delete this discussion? This cannot be undone." confirmText="Delete Post" @confirm="confirmDeletePost" @cancel="isDeleteModalOpen = false" />
    <ConfirmModal :isOpen="isDeleteCommentModalOpen" title="Delete Comment?" message="Are you sure you want to delete this comment?" confirmText="Delete" @confirm="confirmDeleteComment" @cancel="isDeleteCommentModalOpen = false" />
  </article>
</template>

<style scoped>
/* GENERAL */
.avatar-link, .author-name-link, .comment-author-link { text-decoration: none; color: inherit; cursor: pointer; }
.author-name-link:hover, .comment-author-link:hover { text-decoration: underline; }
.avatar-link { display: block; }
.comment-author-link { text-decoration: none; color: #262626; font-weight: 700; cursor: pointer; margin-right: 8px; }
.comment-author-link:hover { text-decoration: underline; }

.post-card { background-color: var(--color-white); padding: 20px; border-radius: 16px; box-shadow: var(--shadow-card); display: flex; gap: 15px; margin-bottom: 20px; position: relative; }
.post-sidebar { flex-shrink: 0; }
.avatar-small { width: 40px; height: 40px; background-color: var(--color-dark); color: var(--color-white); border-radius: 50%; display: flex; align-items: center; justify-content: center; font-weight: bold; overflow: hidden; }
.avatar-img { width: 100%; height: 100%; object-fit: cover; }
.post-main { flex-grow: 1; }
.post-header-row { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 8px; }
.post-meta { font-size: 14px; color: var(--color-text-muted); }
.author-name { font-weight: 700; color: var(--color-dark); margin-right: 5px;}

/* MENU STYLES */
.menu-container { position: relative; }
.menu-btn { background: none; border: none; cursor: pointer; color: #94a3b8; padding: 5px; transition: color 0.2s; }
.menu-btn:hover { color: var(--color-dark); }
.menu-btn svg { width: 20px; height: 20px; }
.dropdown-menu { position: absolute; top: 100%; right: 0; width: 140px; background: white; border-radius: 12px; box-shadow: 0 5px 20px rgba(0,0,0,0.15); border: 1px solid #f1f1f1; z-index: 50; overflow: hidden; animation: fadeIn 0.2s ease; }
@keyframes fadeIn { from { opacity: 0; transform: translateY(-5px); } to { opacity: 1; transform: translateY(0); } }
.dropdown-item { display: flex; align-items: center; gap: 10px; width: 100%; padding: 12px 15px; border: none; background: none; text-align: left; font-size: 14px; font-weight: 600; cursor: pointer; color: var(--color-dark); transition: background 0.2s; }
.dropdown-item:hover { background-color: #f8fafc; }
.dropdown-item svg { width: 16px; height: 16px; }
.dropdown-item.delete { color: #ef4444; }
.dropdown-item.delete:hover { background-color: #fef2f2; }
.menu-overlay { position: fixed; top: 0; left: 0; width: 100%; height: 100%; z-index: 40; cursor: default; }

/* EDIT POST STYLES */
.edit-post-form { display: flex; flex-direction: column; gap: 10px; margin-bottom: 15px; background: #fcfcfc; padding: 15px; border-radius: 12px; border: 1px solid #f1f1f1; }
.edit-group { margin-bottom: 5px; }
.edit-label { font-size: 12px; font-weight: 700; color: #94a3b8; text-transform: uppercase; display: block; margin-bottom: 4px; }
.edit-input { padding: 10px; border: 1px solid #e2e8f0; border-radius: 8px; font-family: inherit; width: 100%; box-sizing: border-box; background: white; }
.edit-input:focus { outline: none; border-color: var(--color-accent); }
.title-edit { font-weight: 700; font-size: 16px; }
.content-edit { font-size: 15px; resize: vertical; }
.input-hint { font-size: 11px; margin-top: 4px; color: #94a3b8; font-weight: 500; }
.valid-hint { color: #22c55e; }
.invalid-hint { color: #ef4444; }
.edit-actions { display: flex; gap: 10px; margin-top: 10px; }
.btn-save { background-color: var(--color-dark); color: white; border: none; padding: 8px 24px; border-radius: 20px; font-weight: 600; cursor: pointer; font-size: 14px; transition: all 0.2s; }
.btn-save:hover:not(:disabled) { background-color: var(--color-accent); transform: translateY(-1px); }
.btn-save:disabled { opacity: 0.5; cursor: not-allowed; }
.btn-cancel { background-color: transparent; border: 1px solid #e2e8f0; color: var(--color-text-muted); padding: 8px 24px; border-radius: 20px; font-weight: 600; cursor: pointer; font-size: 14px; transition: all 0.2s; }
.btn-cancel:hover { background-color: #f8fafc; color: var(--color-dark); }
.tags-wrapper { display: flex; flex-wrap: wrap; gap: 8px; }
.tag-chip { background-color: #fff; border: 1px solid #cbd5e1; color: #64748b; padding: 5px 10px; border-radius: 15px; font-size: 12px; cursor: pointer; transition: all 0.2s; }
.tag-chip:hover { border-color: var(--color-dark); color: var(--color-dark); }
.tag-chip.selected { background-color: var(--color-accent); color: white; border-color: var(--color-accent); font-weight: 600; }

.post-title { margin: 0 0 8px 0; font-size: 18px; color: var(--color-dark); }
.post-content { color: #334155; line-height: 1.5; margin-bottom: 12px; white-space: pre-wrap; }
.tags-container { margin-bottom: 15px; }
.tag { color: var(--color-accent); font-weight: 600; margin-right: 8px; font-size: 14px; }
.post-actions { display: flex; gap: 15px; padding-top: 8px; }
.action-btn { background: none; border: none; cursor: pointer; display: flex; align-items: center; gap: 6px; color: #1e293b; padding: 5px; transition: opacity 0.2s; }
.action-btn:hover { opacity: 0.6; }
.heart-icon, .bubble-icon { width: 24px; height: 24px; }
.heart-icon.outline { color: #1e293b; stroke-width: 1.8px; }
.bubble-icon { color: #1e293b; stroke-width: 1.8px; transform: scaleX(-1); }
.heart-icon.filled { color: #ed4956; animation: heart-pop 0.2s ease; } /* Faster animation */
@keyframes heart-pop { 0% { transform: scale(1); } 50% { transform: scale(1.3); } 100% { transform: scale(1); } }
.count { font-size: 14px; font-weight: 600; color: #1e293b; }
.comments-section { margin-top: 15px; border-top: 1px solid #f1f1f1; padding-top: 15px; }
.comments-list { display: flex; flex-direction: column; gap: 12px; max-height: 400px; overflow-y: auto; margin-bottom: 15px; }
.comment-item { font-size: 14px; line-height: 1.4; }
.comment-text { color: #262626; }
.comment-meta { margin-top: 4px; display: flex; gap: 10px; align-items: center; }
.meta-btn { background: none; border: none; padding: 0; color: #8e8e8e; font-size: 12px; font-weight: 600; cursor: pointer; }
.meta-btn:hover { color: #555; }
.edit-text-btn { color: #a1a1aa; }
.edit-text-btn:hover { color: var(--color-accent); }
.delete-text-btn { color: #ef4444; }
.delete-text-btn:hover { text-decoration: underline; }
.edit-mode { display: flex; gap: 5px; align-items: center; width: 100%; margin-top: 5px; }
.edit-input { flex: 1; padding: 6px 10px; border: 1px solid #ddd; border-radius: 12px; font-size: 14px; background: white; }
.edit-input:focus { outline: none; border-color: var(--color-accent); }
.edit-actions-small { display: flex; gap: 5px; }
.save-small-btn { background: var(--color-dark); color: white; border: none; padding: 5px 10px; border-radius: 12px; cursor: pointer; font-size: 11px; font-weight: 600; }
.cancel-small-btn { background: #f1f5f9; border: none; color: #64748b; padding: 5px 10px; border-radius: 12px; cursor: pointer; font-size: 11px; font-weight: 600; }

/* COMMENT LIKE STYLES */
.like-comment-btn { display: flex; align-items: center; gap: 3px; padding: 0; margin-right: 5px; }
.like-comment-btn:hover { opacity: 0.8; }
.mini-heart { width: 14px; height: 14px; }
.mini-heart.filled-heart { color: #ef4444; animation: heart-pop 0.2s ease; }
.mini-heart.outline-heart { color: #8e8e8e; }

.replies-list { margin-left: 20px; margin-top: 8px; padding-left: 10px; border-left: 2px solid #f0f0f0; display: flex; flex-direction: column; gap: 8px; }
.comment-form-container { border-top: 1px solid #efefef; padding-top: 10px; }
.replying-bar { display: flex; justify-content: space-between; align-items: center; background-color: #f0f9ff; padding: 5px 10px; border-radius: 4px; margin-bottom: 8px; font-size: 12px; color: #666; }
.close-reply { background: none; border: none; cursor: pointer; font-weight: bold; }
.comment-form { display: flex; align-items: center; gap: 10px; }
.comment-input { flex: 1; border: none; font-size: 14px; padding: 5px; background: transparent; }
.comment-input:focus { outline: none; }
.comment-input::placeholder { color: #8e8e8e; }
.btn-post-text { background: none; border: none; cursor: pointer; color: #0095f6; font-weight: 600; font-size: 14px; }
.btn-post-text:disabled { opacity: 0.3; cursor: default; }
.loading-small, .no-comments, .login-prompt { font-size: 13px; color: #8e8e8e; text-align: center; margin: 10px 0; }
</style>