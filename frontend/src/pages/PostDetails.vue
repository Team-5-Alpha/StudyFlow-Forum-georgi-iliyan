<script setup>
import { ref, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import postsService from '../services/posts.service';
import PostCard from '../components/PostCard.vue';

const route = useRoute();
const post = ref(null);
const loading = ref(true);
const error = ref(null);

onMounted(async () => {
  try {
    const postId = route.params.id;
    const response = await postsService.getById(postId);
    post.value = response.data;
  } catch (err) {
    console.error(err);
    error.value = "Post not found or deleted.";
  } finally {
    loading.value = false;
  }
});
</script>

<template>
  <div class="single-post-container">

    <div class="header">
      <router-link to="/" class="back-btn">← Back to Feed</router-link>
      <h2>Post Details</h2>
    </div>

    <div v-if="loading" class="state-msg">Loading post... ⏳</div>
    <div v-if="error" class="state-msg error">{{ error }}</div>

    <div v-else-if="post">
      <PostCard :post="post" />
    </div>

  </div>
</template>

<style scoped>
.single-post-container {
  max-width: 700px;
  margin: 0 auto;
  padding: 20px;
}

.header {
  margin-bottom: 20px;
  border-bottom: 1px solid #e2e8f0;
  padding-bottom: 15px;
}

.header h2 {
  margin: 10px 0 0 0;
  color: var(--color-dark);
}

.back-btn {
  text-decoration: none;
  color: var(--color-text-muted);
  font-weight: 600;
  font-size: 14px;
  transition: color 0.2s;
}

.back-btn:hover {
  color: var(--color-accent);
}

.state-msg {
  text-align: center;
  padding: 40px;
  color: var(--color-text-muted);
}

.error {
  color: #ef4444;
}
</style>