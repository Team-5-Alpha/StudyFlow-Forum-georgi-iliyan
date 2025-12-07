<script setup>
import { ref, onMounted, computed, watch, nextTick } from 'vue';
import { useAuthStore } from '../stores/auth.store';
import { useRoute, useRouter } from 'vue-router';
import postsService from '../services/posts.service';
import usersService from '../services/users.service';
import tagsService from '../services/tags.service';
import UserSearch from '../components/UserSearch.vue';
import CreatePost from '../components/CreatePost.vue';
import PostCard from '../components/PostCard.vue';

const authStore = useAuthStore();
const route = useRoute();
const router = useRouter();

const posts = ref([]);
const loading = ref(true);
const error = ref(null);

// Pagination
const page = ref(0);
const pageSize = ref(10);
const hasMore = ref(true);

// Tags Data
const tags = ref([]);
const selectedTag = ref(null);
const tagDropdownOpen = ref(false);

// Search Logic
const searchQuery = ref('');
const isSearchFocused = ref(false);

// Users (for user search results)
const users = ref([]);
const allUsersCache = ref([]);
let searchDebounce = null;

// suggestedUsers: show up to 8 matching users for the dropdown
const suggestedUsers = computed(() => {
  const q = searchQuery.value.trim().toLowerCase();
  if (!q) return [];
  return users.value.filter(u => {
    const username = (u.username || '').toLowerCase();
    const name = ((u.firstName || '') + ' ' + (u.lastName || '')).toLowerCase();
    return username.includes(q) || name.includes(q);
  }).slice(0, 8);
});

// Selecting a tag still filters posts but does NOT overwrite the user search input
const selectSearchTag = (tagName) => {
  selectedTag.value = tagName;
  tagDropdownOpen.value = false;
  if (activeTab.value === 'forYou') fetchForYou();
  else fetchFollowing();
};

// Navigate to profile on selection
const selectUser = (user) => {
  // navigate to profile by id
  router.push(`/profile/${user.id}`);
  // clear search UI
  searchQuery.value = '';
  users.value = [];
  isSearchFocused.value = false;
};

// Clear only the user search input (keeps tag filter)
const clearUserSearch = () => {
  searchQuery.value = '';
  users.value = [];
  isSearchFocused.value = false;
};

// legacy clear (clears tags + search) remains for other actions
const clearSearch = () => {
  searchQuery.value = '';
  selectedTag.value = null;
  tagDropdownOpen.value = false;
  if (activeTab.value === 'forYou') fetchForYou();
  else fetchFollowing();
};

// Debounced watch: prefer usersService.search, fallback to getAll + local filter
watch(searchQuery, (newVal) => {
  if (searchDebounce) clearTimeout(searchDebounce);

  if (!newVal || !newVal.trim()) {
    users.value = [];
    return;
  }

  searchDebounce = setTimeout(async () => {
    const q = newVal.trim();
    try {
      if (typeof usersService.search === 'function') {
        const res = await usersService.search(q);
        users.value = res.data || [];
      } else {
        if (allUsersCache.value.length === 0) {
          const resAll = await usersService.getAll();
          allUsersCache.value = resAll.data || [];
        }
        const lower = q.toLowerCase();
        users.value = allUsersCache.value.filter(u => {
          const username = (u.username || '').toLowerCase();
          const name = ((u.firstName || '') + ' ' + (u.lastName || '')).toLowerCase();
          return username.includes(lower) || name.includes(lower);
        });
      }
    } catch (e) {
      console.error('User search failed', e);
      users.value = [];
    }
  }, 250);
});

// Watch selectedTag to auto-refresh when changed from dropdown/pills
watch(selectedTag, (val) => {
  if (val === null) return;
  page.value = 0;
  if (activeTab.value === 'forYou') fetchForYou();
  else fetchFollowing();
});

const isModalOpen = ref(false);
const activeTab = ref('forYou');

// 1. Fetch For You
const fetchForYou = async () => {
  loading.value = true;
  error.value = null;
  try {
    const params = {
      sortBy: 'createdAt',
      sortOrder: 'desc',
      page: page.value,
      size: pageSize.value
    };
    if (selectedTag.value) params.tagName = selectedTag.value;

    const response = await postsService.getAll(params);
    posts.value = response.data;

    // If we're on a page > 0 and got no results, go back to previous page
    if (posts.value.length === 0 && page.value > 0) {
      page.value--;
      await fetchForYou();
      return;
    }

    // Only enable "Next" if we got a full page of results
    hasMore.value = posts.value.length === pageSize.value;

    checkAndScrollToPost();
  } catch (err) {
    console.error(err);
    error.value = "Failed to load discussions.";
  } finally {
    loading.value = false;
  }
};

// 2. Fetch Following
const fetchFollowing = async () => {
  if (!authStore.user) return;
  loading.value = true;
  error.value = null;
  posts.value = [];

  try {
    const followingRes = await usersService.getFollowing(authStore.user.id);
    const followingList = followingRes.data;

    if (followingList.length === 0) {
      posts.value = [];
      hasMore.value = false;
      loading.value = false;
      return;
    }

    const promises = followingList.map(user => postsService.getAll({ authorId: user.id }));
    const results = await Promise.all(promises);
    const allPosts = results.map(res => res.data).flat();

    let finalPosts = allPosts;
    if (selectedTag.value) {
      finalPosts = allPosts.filter(p => p.tags && p.tags.includes(selectedTag.value));
    }

    finalPosts.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));

    // Client-side pagination for following feed
    const startIdx = page.value * pageSize.value;
    const endIdx = startIdx + pageSize.value;
    posts.value = finalPosts.slice(startIdx, endIdx);
    hasMore.value = endIdx < finalPosts.length;

    // If we're on a page > 0 and got no results, go back to previous page
    if (posts.value.length === 0 && page.value > 0) {
      page.value--;
      await fetchFollowing();
      return;
    }

    checkAndScrollToPost();
  } catch (err) {
    console.error(err);
    error.value = "Failed to load following feed.";
  } finally {
    loading.value = false;
  }
};

const switchTab = (tabName) => {
  if (tabName === 'following' && !authStore.user) return alert("Login required.");
  activeTab.value = tabName;
  page.value = 0;
  if (tabName === 'forYou') fetchForYou();
  else fetchFollowing();
};

const clearTagFilter = () => {
  selectedTag.value = null;
  page.value = 0;
  if (activeTab.value === 'forYou') fetchForYou();
  else fetchFollowing();
};

const onPostCreated = () => {
  isModalOpen.value = false;
  page.value = 0;
  if (activeTab.value === 'forYou') fetchForYou();
  else fetchFollowing();
};

// Pagination Navigation
const nextPage = () => {
  if (hasMore.value && !loading.value) {
    page.value++;
    if (activeTab.value === 'forYou') fetchForYou();
    else fetchFollowing();
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }
};

const prevPage = () => {
  if (page.value > 0 && !loading.value) {
    page.value--;
    if (activeTab.value === 'forYou') fetchForYou();
    else fetchFollowing();
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }
};

// Remove post from list after deletion
const removePost = (postId) => {
  posts.value = posts.value.filter(p => p.id !== postId);
};

// --- SCROLL LOGIC ---
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
      return;
    }
  }

  const element = document.getElementById(`post-${highlightId}`);
  if (element) {
    element.scrollIntoView({ behavior: 'smooth', block: 'center' });
    element.classList.add('highlight-flash');
    setTimeout(() => {
      element.classList.remove('highlight-flash');
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
    <!-- HEADER -->
    <header class="feed-header">
      <div class="header-top">
        <h2>Latest Discussions</h2>

        <div class="header-actions">
          <!-- USER SEARCH ONLY -->
          <UserSearch />

          <!-- TAG DROPDOWN BUTTON -->
          <div class="tag-filter-wrapper">
            <button @click="tagDropdownOpen = !tagDropdownOpen" class="tag-filter-btn">Tags ▾</button>

            <div v-if="tagDropdownOpen" class="tag-dropdown">
              <div class="tag-dropdown-header">
                <strong>Filter by tag</strong>
                <button v-if="selectedTag" @click="clearTagFilter" class="clear-tag-btn">Clear</button>
              </div>
              <div class="tags-list">
                <button v-for="t in tags" :key="t.id" class="tag-item" :class="{ active: selectedTag === t.name }" @click="selectSearchTag(t.name)">
                  #{{ t.name }}
                </button>
              </div>
            </div>
          </div>

          <button v-if="authStore.user" @click="isModalOpen = true" class="new-post-btn">
            <span class="plus-icon">+</span> <span class="btn-text">New Post</span>
          </button>
        </div>
      </div>

      <div class="tabs">
        <span class="tab" :class="{ active: activeTab === 'forYou' }" @click="switchTab('forYou')">For You</span>
        <span class="tab" :class="{ active: activeTab === 'following' }" @click="switchTab('following')">Following</span>

        <!-- Selected tag pill -->
        <div v-if="selectedTag" class="selected-tag-pill">
          #{{ selectedTag }} <button class="pill-close" @click="clearTagFilter">✕</button>
        </div>
      </div>
    </header>

    <div v-if="!authStore.user && activeTab === 'forYou' && !selectedTag" class="guest-welcome-card">
      <div class="welcome-icon-wrapper">
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" class="welcome-icon">
          <path fill-rule="evenodd" d="M8.25 6.75a3.75 3.75 0 117.5 0 3.75 3.75 0 01-7.5 0zM15.75 9.75a3 3 0 116 0 3 3 0 01-6 0zM2.25 9.75a3 3 0 116 0 3 3 0 01-6 0zM6.31 15.117A6.745 6.745 0 0112 12a6.745 6.745 0 016.709 7.498.75.75 0 01-.372.568A12.696 12.696 0 0112 21.75c-2.305 0-4.47-.612-6.337-1.684a.75.75 0 01-.372-.568 6.787 6.787 0 011.019-4.38z" clip-rule="evenodd" />
          <path d="M5.082 14.254a6.741 6.741 0 00-2.17-3.298.75.75 0 01.437-1.28 9.637 9.637 0 007.915-2.635.75.75 0 01.674 0 9.637 9.637 0 007.915 2.635.75.75 0 01.437 1.28 6.741 6.741 0 00-2.17 3.298 9.673 9.673 0 01-6.564 2.498c-2.427 0-4.66-.816-6.474-2.2z" opacity="0.5"/>
        </svg>
      </div>
      <h3>Welcome to StudyFlow!</h3>
      <p>Join the community of developers to discuss ideas, solve problems, and share knowledge.</p>
      <div class="guest-actions">
        <router-link to="/login" class="btn-primary">Log In</router-link>
        <router-link to="/register" class="btn-outline">Sign Up</router-link>
      </div>
    </div>

    <!-- CONTENT -->
    <div v-if="loading" class="state-msg">Loading discussions... ⏳</div>
    <div v-else-if="error" class="state-msg error">{{ error }}</div>

    <div v-else-if="posts.length === 0" class="state-msg">
      No posts found.
    </div>

    <div v-else class="posts-list">
      <div
          v-for="post in posts"
          :key="post.id"
          :id="'post-' + post.id"
          class="post-wrapper"
      >
        <PostCard
            :post="post"
            @post-deleted="removePost"
        />
      </div>
    </div>

    <!-- PAGINATION -->
    <div v-if="!loading && !error" class="pagination">
      <button @click="prevPage" :disabled="page === 0" class="page-btn">Previous</button>
      <span class="page-count">Page {{ page + 1 }}</span>
      <button @click="nextPage" :disabled="!hasMore || posts.length === 0" class="page-btn">Next</button>
    </div>

    <CreatePost v-if="isModalOpen" @close="isModalOpen = false" @post-created="onPostCreated" />
  </div>
</template>

<style scoped>
.post-wrapper { transition: all 0.5s ease; border-radius: 16px; }
.highlight-flash { box-shadow: 0 0 0 4px rgba(182, 157, 116, 0.5); transform: scale(1.02); z-index: 10; position: relative; }

.feed-header { position: sticky; top: 0; background-color: rgba(245, 245, 239, 0.95); backdrop-filter: blur(10px); border-bottom: 1px solid rgba(0,0,0,0.05); z-index: 10; padding: 15px 0 0 0; }
.header-top { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; padding: 0 20px; }
.header-top h2 { margin: 0; font-size: 20px; flex-shrink: 0; }
.header-actions { display: flex; align-items: center; gap: 12px; flex: 1; justify-content: flex-end; }

.new-post-btn { background-color: var(--color-dark); color: white; border: none; padding: 10px 20px; border-radius: 50px; font-weight: 600; font-size: 14px; cursor: pointer; display: flex; align-items: center; gap: 6px; transition: transform 0.2s; white-space: nowrap; }
.new-post-btn:hover { background-color: var(--color-accent); transform: translateY(-1px); }
@media (max-width: 700px) { .btn-text { display: none; } }

.tabs { display: flex; gap: 20px; padding: 0 20px; }
.tab { padding-bottom: 12px; font-weight: 600; color: var(--color-text-muted); cursor: pointer; position: relative; }
.tab.active { color: var(--color-dark); }
.tab.active::after { content: ''; position: absolute; bottom: 0; left: 0; width: 100%; height: 4px; background-color: var(--color-accent); border-radius: 2px 2px 0 0; }

.guest-welcome-card { background-color: var(--color-white); margin: 20px 20px 0 20px; padding: 30px; border-radius: 16px; text-align: center; box-shadow: var(--shadow-card); border: 1px solid rgba(0,0,0,0.05); }
.guest-welcome-card h3 { margin-top: 10px; color: var(--color-dark); font-size: 22px; }
.guest-welcome-card p { color: var(--color-text-muted); margin-bottom: 20px; font-size: 15px; }
.guest-actions { display: flex; justify-content: center; gap: 15px; }

.welcome-icon-wrapper { display: flex; justify-content: center; margin-bottom: 15px; }
.welcome-icon { width: 50px; height: 50px; color: var(--color-accent); }

.btn-primary { background-color: var(--color-dark); color: white; padding: 10px 24px; border-radius: 25px; text-decoration: none; font-weight: 600; transition: all 0.2s; }
.btn-primary:hover { transform: translateY(-1px); background-color: var(--color-accent); }
.btn-outline { border: 2px solid var(--color-dark); color: var(--color-dark); padding: 8px 22px; border-radius: 25px; text-decoration: none; font-weight: 600; transition: all 0.2s; }
.btn-outline:hover { background-color: #f8fafc; }

.posts-list { padding: 0 20px 20px 20px; display: flex; flex-direction: column; gap: 15px; margin-top: 15px; }
.state-msg { text-align: center; padding: 40px; color: var(--color-text-muted); }
.error { color: #ef4444; }

.tag-filter-wrapper { position: relative; display: inline-block; }
.tag-filter-btn { background: white; border: 1px solid #e2e8f0; padding: 8px 12px; border-radius: 12px; cursor: pointer; font-weight: 600; }
.tag-dropdown { position: absolute; right: 0; top: 110%; background: white; border: 1px solid #e1e8ed; border-radius: 12px; box-shadow: 0 8px 20px rgba(0,0,0,0.08); padding: 10px; width: 260px; z-index: 200; }
.tag-dropdown-header { display: flex; justify-content: space-between; align-items: center; padding-bottom: 8px; border-bottom: 1px solid #f1f5f9; margin-bottom: 8px; }
.clear-tag-btn { background: none; border: none; color: #ef4444; cursor: pointer; font-weight: 700; }
.tags-list { display: flex; flex-wrap: wrap; gap: 8px; max-height: 240px; overflow-y: auto; }
.tag-item { background: #fff; border: 1px solid #e2e8f0; padding: 6px 10px; border-radius: 16px; cursor: pointer; font-size: 13px; }
.tag-item.active { background: var(--color-accent); color: white; border-color: var(--color-accent); }

.selected-tag-pill { margin-left: 12px; display: flex; align-items: center; gap: 8px; background: #fff6ec; border: 1px solid rgba(182,157,116,0.2); padding: 6px 10px; border-radius: 999px; font-weight: 700; color: var(--color-dark); }
.pill-close { background: none; border: none; cursor: pointer; font-size: 12px; margin-left: 6px; }

/* Pagination */
.pagination { display: flex; justify-content: center; align-items: center; gap: 15px; margin: 25px 20px; }
.page-btn { background: white; border: 1px solid #e2e8f0; padding: 8px 20px; border-radius: 50px; cursor: pointer; color: var(--color-dark); font-weight: 600; font-size: 14px; transition: all 0.2s; }
.page-btn:hover:not(:disabled) { border-color: var(--color-dark); background: #f8fafc; }
.page-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.page-count { font-size: 14px; color: #94a3b8; font-weight: 600; }
</style>