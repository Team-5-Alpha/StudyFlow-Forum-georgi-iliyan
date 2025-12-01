<script setup>
import { ref, computed, onMounted, nextTick } from 'vue';
import { useAuthStore } from '../stores/auth.store';
import postsService from '../services/posts.service';
import commentsService from '../services/comments.service';
import usersService from '../services/users.service';

const props = defineProps({
  post: {
    type: Object,
    required: true
  }
});

const authStore = useAuthStore();

const comments = ref([]);
const showComments = ref(false);
const commentsLoading = ref(false);
const newCommentContent = ref('');
const isSubmittingComment = ref(false);
const authorPhotoUrl = ref(null);

const replyingTo = ref(null);
const editingCommentId = ref(null);
const editContent = ref('');
const commentInputRef = ref(null);

const localLikesCount = ref(props.post.likesCount);
const isLikedLocal = ref(false);

const formatDate = (dateString) => {
  const date = new Date(dateString);
  return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
};

onMounted(async () => {
  try {
    const userResponse = await usersService.getById(props.post.author.id);
    if (userResponse.data && userResponse.data.profilePhotoURL) {
      authorPhotoUrl.value = userResponse.data.profilePhotoURL;
    }
  } catch (err) {}
});

const structuredComments = computed(() => {
  const roots = [];
  const childrenMap = {};
  comments.value.forEach(c => { childrenMap[c.id] = []; });
  comments.value.forEach(c => {
    if (c.parentCommentId && childrenMap[c.parentCommentId]) {
      childrenMap[c.parentCommentId].push(c);
    } else {
      roots.push(c);
    }
  });
  roots.sort((a, b) => new Date(a.createdAt) - new Date(b.createdAt));
  return roots.map(root => ({ ...root, replies: childrenMap[root.id] || [] }));
});


const toggleLike = async () => {
  if (!authStore.isAuthenticated) return;
  try {
    if (!isLikedLocal.value) {
      await postsService.like(props.post.id);
      localLikesCount.value++;
      isLikedLocal.value = true;
    } else {
      await postsService.unlike(props.post.id);
      localLikesCount.value--;
      isLikedLocal.value = false;
    }
  } catch (err) {
    if (!isLikedLocal.value) {
      await postsService.unlike(props.post.id);
      localLikesCount.value = Math.max(0, localLikesCount.value - 1);
    } else {
      await postsService.like(props.post.id);
      localLikesCount.value++;
    }
    isLikedLocal.value = !isLikedLocal.value;
  }
};

const toggleComments = async () => {
  showComments.value = !showComments.value;
  if (showComments.value && comments.value.length === 0) loadComments();
};

const loadComments = async () => {
  commentsLoading.value = true;
  try {
    const response = await postsService.getComments(props.post.id);
    comments.value = response.data;
  } catch (err) { console.error(err); }
  finally { commentsLoading.value = false; }
};

const prepareReply = (comment) => { cancelEdit(); replyingTo.value = comment; if(commentInputRef.value) commentInputRef.value.focus(); };
const cancelReply = () => { replyingTo.value = null; newCommentContent.value = ''; };

const submitComment = async () => {
  if (newCommentContent.value.length < 4) return;
  isSubmittingComment.value = true;
  try {
    let newComment;
    if (replyingTo.value) {
      const response = await commentsService.replyToComment(replyingTo.value.id, { content: newCommentContent.value });
      newComment = response.data; newComment.parentCommentId = replyingTo.value.id;
    } else {
      const response = await postsService.addComment(props.post.id, { content: newCommentContent.value });
      newComment = response.data;
    }
    comments.value.push(newComment); newCommentContent.value = ''; replyingTo.value = null;
  } catch (err) { alert("Failed: " + err.message); }
  finally { isSubmittingComment.value = false; }
};

const startEdit = (comment) => { cancelReply(); editingCommentId.value = comment.id; editContent.value = comment.content; };
const cancelEdit = () => { editingCommentId.value = null; editContent.value = ''; };
const submitEdit = async (commentId) => {
  if (editContent.value.length < 4) return alert("Min 4 chars");
  try {
    const response = await commentsService.update(commentId, { content: editContent.value });
    const index = comments.value.findIndex(c => c.id === commentId);
    if (index !== -1) {
      comments.value[index].content = response.data.content;
      comments.value[index].updatedAt = response.data.updatedAt;
    }
    cancelEdit();
  } catch (err) { alert("Update failed"); }
};
const canEdit = (comment) => authStore.user && authStore.user.id === comment.author.id;
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
      <div class="post-meta">
        <router-link :to="`/profile/${post.author.id}`" class="author-name-link">
          {{ post.author.username }}
        </router-link>
        <span class="dot">·</span>
        <span class="post-date">{{ formatDate(post.createdAt) }}</span>
      </div>

      <h3 class="post-title">{{ post.title }}</h3>
      <p class="post-content">{{ post.content }}</p>

      <div class="tags-container" v-if="post.tags && post.tags.length">
        <span v-for="tag in post.tags" :key="tag" class="tag">#{{ tag }}</span>
      </div>

      <div class="post-actions">
        <button @click="toggleLike" class="action-btn like-btn">
          <svg v-if="isLikedLocal" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" class="heart-icon filled">
            <path d="M11.645 20.91l-.007-.003-.022-.012a15.247 15.247 0 01-.383-.218 25.18 25.18 0 01-4.244-3.17C4.688 15.36 2.25 12.174 2.25 8.25 2.25 5.322 4.714 3 7.75 3c1.99 0 3.969 1.356 5.25 3.34C14.281 4.356 16.261 3 18.25 3c3.036 0 5.5 2.322 5.5 5.25 0 3.925-2.438 7.111-4.739 9.256a25.175 25.175 0 01-4.244 3.17 15.247 15.247 0 01-.383.219l-.022.012-.007.004-.003.001a.752.752 0 01-.704 0l-.003-.001z" />
          </svg>
          <svg v-else xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="heart-icon outline">
            <path stroke-linecap="round" stroke-linejoin="round" d="M21 8.25c0-2.485-2.099-4.5-4.688-4.5-1.935 0-3.597 1.126-4.312 2.733-.715-1.607-2.377-2.733-4.313-2.733C5.1 3.75 3 5.765 3 8.25c0 7.22 9 12 9 12s9-4.78 9-12z" />
          </svg>
          <span class="count">{{ localLikesCount }}</span>
        </button>

        <button @click="toggleComments" class="action-btn comment-btn">
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="bubble-icon">
            <path stroke-linecap="round" stroke-linejoin="round" d="M12 20.25c4.97 0 9-3.694 9-8.25s-4.03-8.25-9-8.25S3 7.444 3 12c0 2.104.859 4.023 2.273 5.48.432.447.74 1.04.586 1.641a4.483 4.483 0 01-.923 1.785A5.969 5.969 0 006 21c1.282 0 2.47-.402 3.445-1.087.81.22 1.668.337 2.555.337z" />
          </svg>
          <span class="count">{{ showComments ? 'Hide' : 'Comments' }}</span>
        </button>
      </div>

      <div v-if="showComments" class="comments-section">
        <div v-if="commentsLoading" class="loading-small">Loading comments...</div>
        <div v-else class="comments-list">
          <div v-if="comments.length === 0" class="no-comments">No comments yet.</div>

          <div v-for="root in structuredComments" :key="root.id" class="comment-block">
            <div class="comment-item">
              <div v-if="editingCommentId !== root.id">
                <router-link :to="`/profile/${root.author.id}`" class="comment-author-link">
                  {{ root.author.username }}
                </router-link>
                <span class="comment-text">{{ root.content }}</span>
                <div class="comment-meta">
                  <button @click="prepareReply(root)" class="meta-btn">Reply</button>
                  <button v-if="canEdit(root)" @click="startEdit(root)" class="meta-btn edit-btn">Edit</button>
                </div>
              </div>
              <div v-else class="edit-mode">
                <input v-model="editContent" class="edit-input" @keyup.enter="submitEdit(root.id)" />
                <div class="edit-actions"><button @click="submitEdit(root.id)" class="save-btn">Save</button><button @click="cancelEdit" class="cancel-btn">✕</button></div>
              </div>
            </div>

            <div v-if="root.replies.length > 0" class="replies-list">
              <div v-for="reply in root.replies" :key="reply.id" class="comment-item reply-item">
                <div v-if="editingCommentId !== reply.id">
                  <router-link :to="`/profile/${reply.author.id}`" class="comment-author-link">
                    {{ reply.author.username }}
                  </router-link>
                  <span class="comment-text">{{ reply.content }}</span>
                  <div class="comment-meta"><button v-if="canEdit(reply)" @click="startEdit(reply)" class="meta-btn edit-btn">Edit</button></div>
                </div>
                <div v-else class="edit-mode">
                  <input v-model="editContent" class="edit-input" @keyup.enter="submitEdit(reply.id)" />
                  <div class="edit-actions"><button @click="submitEdit(reply.id)" class="save-btn">Save</button><button @click="cancelEdit" class="cancel-btn">✕</button></div>
                </div>
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
  </article>
</template>

<style scoped>

.avatar-link, .author-name-link { text-decoration: none; color: inherit; cursor: pointer; }
.author-name-link:hover { text-decoration: underline; }
.avatar-link { display: block; }


.comment-author-link {
  text-decoration: none;
  color: #262626;
  font-weight: 700;
  cursor: pointer;
  margin-right: 8px;
}
.comment-author-link:hover { text-decoration: underline; }

.post-card { background-color: var(--color-white); padding: 20px; border-radius: 16px; box-shadow: var(--shadow-card); display: flex; gap: 15px; margin-bottom: 20px; }
.post-sidebar { flex-shrink: 0; }
.avatar-small { width: 40px; height: 40px; background-color: var(--color-dark); color: var(--color-white); border-radius: 50%; display: flex; align-items: center; justify-content: center; font-weight: bold; overflow: hidden; }
.avatar-img { width: 100%; height: 100%; object-fit: cover; }
.post-main { flex-grow: 1; }
.post-meta { font-size: 14px; color: var(--color-text-muted); margin-bottom: 8px; }
.author-name { font-weight: 700; color: var(--color-dark); margin-right: 5px;}
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
.heart-icon.filled { color: #ed4956; animation: heart-pop 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275); }
@keyframes heart-pop { 0% { transform: scale(1); } 50% { transform: scale(1.3); } 100% { transform: scale(1); } }
.count { font-size: 14px; font-weight: 600; color: #1e293b; }
.comments-section { margin-top: 15px; border-top: 1px solid #f1f1f1; padding-top: 15px; }
.comments-list { display: flex; flex-direction: column; gap: 12px; max-height: 400px; overflow-y: auto; margin-bottom: 15px; }
.comment-item { font-size: 14px; line-height: 1.4; }
.comment-text { color: #262626; }
.comment-meta { margin-top: 4px; display: flex; gap: 10px; }
.meta-btn { background: none; border: none; padding: 0; color: #8e8e8e; font-size: 12px; font-weight: 600; cursor: pointer; }
.meta-btn:hover { color: #555; }
.edit-btn { color: #a1a1aa; }
.edit-mode { display: flex; gap: 5px; align-items: center; width: 100%; }
.edit-input { flex: 1; padding: 5px; border: 1px solid #ddd; border-radius: 4px; font-size: 14px; }
.save-btn { background: #0095f6; color: white; border: none; padding: 4px 10px; border-radius: 4px; cursor: pointer; font-size: 12px; }
.cancel-btn { background: none; border: none; color: #888; cursor: pointer; font-size: 14px; }
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