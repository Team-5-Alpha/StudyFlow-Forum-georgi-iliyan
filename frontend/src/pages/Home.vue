<script setup>
import { ref, onMounted, computed, watch, nextTick } from 'vue';
import { useAuthStore } from '../stores/auth.store';
import { useRoute, useRouter } from 'vue-router';
import postsService from '../services/posts.service';
import usersService from '../services/users.service';
import tagsService from '../services/tags.service';
import CreatePost from '../components/CreatePost.vue';
import PostItem from '../components/PostItem.vue';

const authStore = useAuthStore();
const route = useRoute();
const router = useRouter();

const posts = ref([]);
const loading = ref(true);
const error = ref(null);
const tags = ref([]);
const selectedTag = ref(null);
const searchQuery = ref('');
const isSearchFocused = ref(false);
const isModalOpen = ref(false);
const activeTab = ref('forYou');

const suggestedTags = computed(() => {
  if (!searchQuery.value) return [];
  if (selectedTag.value === searchQuery.value) return [];
  const query = searchQuery.value.toLowerCase();
  return tags.value.filter(tag => tag.name.toLowerCase().includes(query));
});
const selectSearchTag = (tagName) => { selectedTag.value = tagName; searchQuery.value = tagName; isSearchFocused.value = false; if (activeTab.value === 'forYou') fetchForYou(); else fetchFollowing(); };
const clearSearch = () => { searchQuery.value = ''; selectedTag.value = null; if (activeTab.value === 'forYou') fetchForYou(); else fetchFollowing(); };
watch(searchQuery, (newVal) => { if (newVal === '' && selectedTag.value) clearSearch(); });

const fetchForYou = async () => {
  loading.value = true;
  error.value = null;
  try {
    const params = { sortBy: 'createdAt', sortOrder: 'desc' };
    if (selectedTag.value) params.tagName = selectedTag.value;
    const response = await postsService.getAll(params);
    posts.value = response.data;


    checkAndScrollToPost();
  } catch (err) {
    console.error(err);
    error.value = "Failed to load discussions.";
  } finally {
    loading.value = false;
  }
};


const fetchFollowing = async () => {
  if (!authStore.user) return;
  loading.value = true; posts.value = [];
  try {
    const followingRes = await usersService.getFollowing(authStore.user.id);
    const followingList = followingRes.data;
    if (followingList.length === 0) { posts.value = []; loading.value = false; return; }
    const promises = followingList.map(user => postsService.getAll({ authorId: user.id }));
    const results = await Promise.all(promises);
    const allPosts = results.map(res => res.data).flat();
    let finalPosts = allPosts;
    if (selectedTag.value) finalPosts = allPosts.filter(p => p.tags && p.tags.includes(selectedTag.value));
    finalPosts.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
    posts.value = finalPosts;
    checkAndScrollToPost();
  } catch (err) { error.value = "Failed"; } finally { loading.value = false; }
};

const switchTab = (tabName) => {
  if (tabName === 'following' && !authStore.user) return alert("Login required.");
  activeTab.value = tabName;
  if (tabName === 'forYou') fetchForYou();
  else fetchFollowing();
};

const onPostCreated = () => { isModalOpen.value = false; clearSearch(); };

const checkAndScrollToPost = async () => {
  const highlightId = route.query.highlight;
  if (!highlightId) return;

  await nextTick();


  const postExists = posts.value.find(p => p.id == highlightId);

  if (!postExists) {
    try {
      const res = await postsService.getById(highlightId);
      posts.value.unshift(res.data);
      await nextTick();
    } catch (e) {
      console.error("Post not found for highlight");
      return;
    }
  }

  const element = document.getElementById(`post-${highlightId}`);
  if (element) {
    element.scrollIntoView({ behavior: 'smooth', block: 'center' });


    element.classList.add('highlight-flash');

    setTimeout(() => {
      element.classList.remove('highlight-flash');
      router.replace({ query: null });
    }, 2000);
  }
};

watch(() => route.query.highlight, () => {
  checkAndScrollToPost();
});

onMounted(async () => {
  fetchForYou();
  try {
    const tagsRes = await tagsService.getAll();
    tags.value = tagsRes.data;
  } catch (err) {}
});
</script>

<template>
  <div class="feed-container">
    <header class="feed-header">
      <div class="header-top">
        <h2>Latest Discussions</h2>
        <div class="header-actions">
          <div class="tag-search-container">
            <div class="search-input-wrapper" :class="{ 'active': isSearchFocused }">
              <span class="search-icon"><svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" d="M21 21l-5.197-5.197m0 0A7.5 7.5 0 105.196 5.196a7.5 7.5 0 0010.607 10.607z" /></svg></span>
              <input type="text" v-model="searchQuery" placeholder="Search tags..." class="tag-search-input" @focus="isSearchFocused = true" @blur="setTimeout(() => isSearchFocused = false, 200)" />
              <button v-if="searchQuery" @click="clearSearch" class="clear-search-btn">✕</button>
            </div>
            <div v-if="isSearchFocused && suggestedTags.length > 0" class="search-suggestions">
              <div v-for="tag in suggestedTags" :key="tag.id" class="suggestion-item" @click="selectSearchTag(tag.name)">#{{ tag.name }}</div>
            </div>
          </div>
          <button v-if="authStore.user" @click="isModalOpen = true" class="new-post-btn"><span class="plus-icon">+</span> <span class="btn-text">New Post</span></button>
        </div>
      </div>
      <div class="tabs">
        <span class="tab" :class="{ active: activeTab === 'forYou' }" @click="switchTab('forYou')">For You</span>
        <span class="tab" :class="{ active: activeTab === 'following' }" @click="switchTab('following')">Following</span>
      </div>
    </header>

    <div v-if="!authStore.user && activeTab === 'forYou' && !selectedTag" class="guest-welcome-card">
      <h3>Welcome to StudyFlow! 🚀</h3>
      <p>Join the community of developers to discuss ideas.</p>
      <div class="guest-actions"><router-link to="/login" class="btn-primary">Log In</router-link><router-link to="/register" class="btn-outline">Sign Up</router-link></div>
    </div>

    <div v-if="loading" class="state-msg">Loading discussions... ⏳</div>
    <div v-if="error" class="state-msg error">{{ error }}</div>

    <div v-else class="posts-list">
      <div v-if="posts.length === 0" class="state-msg">
        <span v-if="selectedTag">No discussions found for <strong>#{{ selectedTag }}</strong>.</span>
        <span v-else>No posts found yet.</span>
      </div>

      <div
          v-for="post in posts"
          :key="post.id"
          :id="'post-' + post.id"
          class="post-wrapper"
      >
        <PostItem :post="post" />
      </div>
    </div>

    <CreatePost v-if="isModalOpen" @close="isModalOpen = false" @post-created="onPostCreated" />
  </div>
</template>

<style scoped>

.post-wrapper {
  transition: all 0.5s ease;
  border-radius: 16px;
}

.highlight-flash {
  box-shadow: 0 0 0 4px rgba(182, 157, 116, 0.5);
  transform: scale(1.02);
  z-index: 10;
  position: relative;
}


.feed-header { position: sticky; top: 0; background-color: rgba(245, 245, 239, 0.95); backdrop-filter: blur(10px); border-bottom: 1px solid rgba(0,0,0,0.05); z-index: 10; padding: 15px 0 0 0; }
.header-top { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; padding: 0 20px; }
.header-top h2 { margin: 0; font-size: 20px; flex-shrink: 0; }
.header-actions { display: flex; align-items: center; gap: 12px; flex: 1; justify-content: flex-end; }
.tag-search-container { position: relative; width: 220px; transition: width 0.2s; }
.tag-search-container:focus-within { width: 260px; }
.search-input-wrapper { display: flex; align-items: center; background-color: #ffffff; border: 1px solid #e2e8f0; border-radius: 50px; padding: 8px 16px; transition: all 0.2s ease; }
.search-input-wrapper.active { border-color: var(--color-accent); box-shadow: 0 0 0 3px rgba(182, 157, 116, 0.15); }
.search-icon svg { width: 18px; height: 18px; margin-right: 8px; color: #94a3b8; }
.search-input-wrapper.active .search-icon svg { color: var(--color-dark); }
.tag-search-input { border: none; background: transparent; outline: none; font-size: 14px; width: 100%; color: var(--color-dark); }
.tag-search-input::placeholder { color: #94a3b8; }
.clear-search-btn { background: none; border: none; color: #94a3b8; cursor: pointer; font-size: 14px; padding: 0; margin-left: 5px; }
.clear-search-btn:hover { color: var(--color-dark); }
.search-suggestions { position: absolute; top: 115%; left: 0; width: 100%; background-color: white; border: 1px solid #e1e8ed; border-radius: 16px; box-shadow: 0 4px 15px rgba(0,0,0,0.1); z-index: 100; max-height: 200px; overflow-y: auto; padding: 5px 0; }
.suggestion-item { padding: 10px 15px; font-size: 14px; color: var(--color-dark); cursor: pointer; }
.suggestion-item:hover { background-color: #f8fafc; color: var(--color-accent); }
.new-post-btn { background-color: var(--color-dark); color: white; border: none; padding: 10px 20px; border-radius: 50px; font-weight: 600; font-size: 14px; cursor: pointer; display: flex; align-items: center; gap: 6px; transition: transform 0.2s; white-space: nowrap; }
.new-post-btn:hover { background-color: var(--color-accent); transform: translateY(-1px); }
@media (max-width: 700px) { .btn-text { display: none; } .tag-search-container { width: 140px; } }
.tabs { display: flex; gap: 20px; padding: 0 20px; }
.tab { padding-bottom: 12px; font-weight: 600; color: var(--color-text-muted); cursor: pointer; position: relative; }
.tab.active { color: var(--color-dark); }
.tab.active::after { content: ''; position: absolute; bottom: 0; left: 0; width: 100%; height: 4px; background-color: var(--color-accent); border-radius: 2px 2px 0 0; }
.guest-welcome-card { background-color: var(--color-white); margin: 20px 20px 0 20px; padding: 30px; border-radius: 16px; text-align: center; box-shadow: var(--shadow-card); border: 1px solid rgba(0,0,0,0.05); }
.guest-welcome-card h3 { margin-top: 0; color: var(--color-dark); font-size: 22px; }
.guest-welcome-card p { color: var(--color-text-muted); margin-bottom: 20px; font-size: 15px; }
.guest-actions { display: flex; justify-content: center; gap: 15px; }
.btn-primary { background-color: var(--color-dark); color: white; padding: 10px 24px; border-radius: 25px; text-decoration: none; font-weight: 600; }
.btn-outline { border: 2px solid var(--color-dark); color: var(--color-dark); padding: 8px 22px; border-radius: 25px; text-decoration: none; font-weight: 600; }
.posts-list { padding: 0 20px 20px 20px; display: flex; flex-direction: column; gap: 15px; margin-top: 15px; }
.state-msg { text-align: center; padding: 40px; color: var(--color-text-muted); }
.error { color: #ef4444; }
</style>