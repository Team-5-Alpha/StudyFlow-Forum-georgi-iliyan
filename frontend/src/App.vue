<script setup>
import { ref, onMounted, computed } from 'vue';
import { RouterView, RouterLink, useRouter } from 'vue-router';
import { useAuthStore } from './stores/auth.store';
import { useNotificationStore } from './stores/notifications.store';
import postsService from './services/posts.service';

const authStore = useAuthStore();
const notifStore = useNotificationStore();
const router = useRouter();

const topPosts = ref([]);
const isTrendingExpanded = ref(false);

// Admin check – от store или от localStorage (по-надеждно при refresh)
const isAdmin = computed(() => {
  if (authStore.user?.role === 'ADMIN') return true;
  try {
    const stored = JSON.parse(localStorage.getItem('user'));
    return stored?.role === 'ADMIN';
  } catch (e) {
    return false;
  }
});

const visibleTrendingPosts = computed(() => {
  return isTrendingExpanded.value ? topPosts.value : topPosts.value.slice(0, 3);
});

const handleLogout = async () => {
  notifStore.stopPolling();
  await authStore.logout();
  await router.push('/');
};

onMounted(async () => {
  if (authStore.user) {
    notifStore.startPolling();
  }
  try {
    const response = await postsService.getTopCommented(10);
    topPosts.value = response.data;
  } catch (err) {
    console.error('Failed to load trending posts', err);
  }
});
</script>

<template>
  <div class="app-layout">
    <!-- SIDEBAR -->
    <aside class="sidebar">
      <div class="logo-container">
        <h1 class="logo">
          StudyFlow <span class="dot">.</span>
        </h1>
      </div>

      <!-- USER WELCOME / ADMIN BADGE -->
      <div v-if="authStore.user" class="user-welcome">
        <div class="welcome-text">
          Hello, {{ authStore.user.firstName }}
          <span v-if="isAdmin" class="admin-badge-sidebar">Admin 👑</span>
        </div>
      </div>

      <!-- NAV MENU -->
      <nav class="nav-menu">
        <RouterLink to="/" class="nav-item">
          <svg
              xmlns="http://www.w3.org/2000/svg"
              fill="none"
              viewBox="0 0 24 24"
              stroke-width="1.8"
              stroke="currentColor"
              class="nav-icon"
          >
            <path
                stroke-linecap="round"
                stroke-linejoin="round"
                d="M2.25 12l8.954-8.955c.44-.439 1.152-.439 1.591 0L21.75 12M4.5 9.75v10.125c0 .621.504 1.125 1.125 1.125H9.75v-4.875c0-.621.504-1.125 1.125-1.125h2.25c.621 0 1.125.504 1.125 1.125V21h4.125c.621 0 1.125-.504 1.125-1.125V9.75M8.25 21h8.25"
            />
          </svg>
          Home
        </RouterLink>

        <!-- LOGGED IN MENU -->
        <template v-if="authStore.user">
          <RouterLink to="/notifications" class="nav-item">
            <div class="icon-wrapper">
              <svg
                  xmlns="http://www.w3.org/2000/svg"
                  fill="none"
                  viewBox="0 0 24 24"
                  stroke-width="1.8"
                  stroke="currentColor"
                  class="nav-icon"
              >
                <path
                    stroke-linecap="round"
                    stroke-linejoin="round"
                    d="M14.857 17.082a23.848 23.848 0 005.454-1.31A8.967 8.967 0 0118 9.75v-.7V9A6 6 0 006 9v.75a8.967 8.967 0 01-2.312 6.022c1.733.64 3.56 1.085 5.455 1.31m5.714 0a24.255 24.255 0 01-5.714 0m5.714 0a3 3 0 11-5.714 0"
                />
              </svg>
              <span v-if="notifStore.unreadCount > 0" class="notif-badge">
                {{ notifStore.unreadCount > 9 ? '9+' : notifStore.unreadCount }}
              </span>
            </div>
            Notifications
          </RouterLink>

          <RouterLink to="/profile" class="nav-item">
            <svg
                xmlns="http://www.w3.org/2000/svg"
                fill="none"
                viewBox="0 0 24 24"
                stroke-width="1.8"
                stroke="currentColor"
                class="nav-icon"
            >
              <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  d="M15.75 6a3.75 3.75 0 11-7.5 0 3.75 3.75 0 017.5 0zM4.501 20.118a7.5 7.5 0 0114.998 0A17.933 17.933 0 0112 21.75c-2.676 0-5.216-.584-7.499-1.632z"
              />
            </svg>
            Profile
          </RouterLink>

          <!-- ADMIN DASHBOARD LINK -->
          <RouterLink
              v-if="isAdmin"
              to="/admin/users"
              class="nav-item admin-link"
          >
            <svg
                xmlns="http://www.w3.org/2000/svg"
                fill="none"
                viewBox="0 0 24 24"
                stroke-width="1.8"
                stroke="currentColor"
                class="nav-icon"
            >
              <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  d="M9 12.75L11.25 15 15 9.75M21 12c0 1.268-.63 2.39-1.593 3.068a3.745 3.745 0 01-1.043 3.296 3.745 3.745 0 01-3.296 1.043A3.745 3.745 0 0112 21c-1.268 0-2.39-.63-3.068-1.593a3.746 3.746 0 01-3.296-1.043 3.745 3.745 0 01-1.043-3.296A3.745 3.745 0 013 12c0-1.268.63-2.39 1.593-3.068a3.745 3.745 0 011.043-3.296 3.746 3.746 0 013.296-1.043A3.746 3.746 0 0112 3c1.268 0 2.39.63 3.068 1.593a3.746 3.746 0 013.296 1.043 3.746 3.746 0 011.043 3.296A3.745 3.745 0 0121 12z"
              />
            </svg>
            User Management
          </RouterLink>

          <button @click="handleLogout" class="nav-item logout-btn">
            <svg
                xmlns="http://www.w3.org/2000/svg"
                fill="none"
                viewBox="0 0 24 24"
                stroke-width="1.8"
                stroke="currentColor"
                class="nav-icon"
            >
              <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  d="M15.75 9V5.25A2.25 2.25 0 0013.5 3h-6a2.25 2.25 0 00-2.25 2.25v13.5A2.25 2.25 0 007.5 21h6a2.25 2.25 0 002.25-2.25V15m3 0l3-3m0 0l-3-3m3 3H9"
              />
            </svg>
            Logout
          </button>
        </template>

        <!-- LOGGED OUT MENU -->
        <template v-else>
          <RouterLink to="/login" class="nav-item">
            <svg
                xmlns="http://www.w3.org/2000/svg"
                fill="none"
                viewBox="0 0 24 24"
                stroke-width="1.8"
                stroke="currentColor"
                class="nav-icon"
            >
              <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  d="M15.75 9V5.25A2.25 2.25 0 0013.5 3h-6a2.25 2.25 0 00-2.25 2.25v13.5A2.25 2.25 0 007.5 21h6a2.25 2.25 0 002.25-2.25V15M12 9l-3 3m0 0l3 3m-3-3h12.75"
              />
            </svg>
            Login
          </RouterLink>

          <RouterLink to="/register" class="nav-item">
            <svg
                xmlns="http://www.w3.org/2000/svg"
                fill="none"
                viewBox="0 0 24 24"
                stroke-width="1.8"
                stroke="currentColor"
                class="nav-icon"
            >
              <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  d="M19 7.5v3m0 0v3m0-3h3m-3 0h-3m-2.25-4.125a3.375 3.375 0 11-6.75 0 3.375 3.375 0 016.75 0zM4 19.235v-.11a6.375 6.375 0 0112.75 0v.109A12.318 12.318 0 0110.374 21c-2.331 0-4.512-.645-6.374-1.766z"
              />
            </svg>
            Register
          </RouterLink>
        </template>
      </nav>

      <!-- MINI PROFILE -->
      <RouterLink
          v-if="authStore.user"
          to="/profile"
          class="user-mini-profile-link"
      >
        <div class="user-mini-profile">
          <div class="avatar">
            <img
                v-if="authStore.user.profilePhotoURL"
                :src="authStore.user.profilePhotoURL"
                alt="Me"
                class="avatar-img"
            />
            <span v-else>{{ authStore.user.username.charAt(0).toUpperCase() }}</span>
          </div>
          <div class="user-info">
            <span class="username">@{{ authStore.user.username }}</span>
            <span class="profile-label">View Profile</span>
          </div>
        </div>
      </RouterLink>
    </aside>

    <!-- MAIN CONTENT + PAGE TRANSITION -->
    <main class="content">
      <router-view v-slot="{ Component }">
        <transition name="fade-slide" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </main>

    <!-- TRENDING SIDEBAR -->
    <aside class="trending">
      <div class="trending-card">
        <h3 class="trending-header">
          <svg
              xmlns="http://www.w3.org/2000/svg"
              viewBox="0 0 24 24"
              fill="currentColor"
              class="trending-icon"
          >
            <path
                fill-rule="evenodd"
                d="M12.963 2.286a.75.75 0 0 0-1.071-.136 9.742 9.742 0 0 0-3.539 6.176 7.547 7.547 0 0 1-1.705-1.715.75.75 0 0 0-1.152.082A9 9 0 1 0 15.68 4.534a7.46 7.46 0 0 1-2.717-2.248ZM15.75 14.25a3.75 3.75 0 1 1-7.5 0 3.75 3.75 0 0 1 7.5 0Z"
                clip-rule="evenodd"
            />
          </svg>
          Top Discussions
        </h3>

        <ul class="trend-list">
          <li v-if="topPosts.length === 0" class="empty-trend">
            No active discussions yet.
          </li>
          <li
              v-for="post in visibleTrendingPosts"
              :key="post.id"
              class="trend-item"
          >
            <div class="trend-meta">
              <router-link
                  :to="`/profile/${post.author.id}`"
                  class="trend-author"
              >
                @{{ post.author.username }}
              </router-link>
            </div>
            <p class="trend-title" :title="post.title">
              {{ post.title }}
            </p>
            <span class="trend-status">Hot Topic</span>
          </li>
        </ul>

        <button
            v-if="topPosts.length > 3"
            @click="isTrendingExpanded = !isTrendingExpanded"
            class="show-more-btn"
        >
          {{ isTrendingExpanded ? 'Show Less' : 'Show More' }}
        </button>
      </div>
    </aside>

    <!-- TOAST NOTIFICATION -->
    <transition name="toast">
      <div
          v-if="notifStore.popupVisible"
          class="toast-notification"
          @click="router.push('/notifications')"
      >
        <div class="toast-icon-box" :class="notifStore.popupData.type">
          <svg
              v-if="notifStore.popupData.type && notifStore.popupData.type.includes('LIKE')"
              xmlns="http://www.w3.org/2000/svg"
              viewBox="0 0 24 24"
              fill="currentColor"
          >
            <path
                d="M11.645 20.91l-.007-.003-.022-.012a15.247 15.247 0 01-.383-.218 25.18 25.18 0 01-4.244-3.17C4.688 15.36 2.25 12.174 2.25 8.25 2.25 5.322 4.714 3 7.75 3c1.99 0 3.969 1.356 5.25 3.34C14.281 4.356 16.261 3 18.25 3c3.036 0 5.5 2.322 5.5 5.25 0 3.925-2.438 7.111-4.739 9.256a25.175 25.175 0 01-4.244 3.17 15.247 15.247 0 01-.383.219l-.022.012-.007.004-.003.001a.752.752 0 01-.704 0l-.003-.001z"
            />
          </svg>
          <svg
              v-else-if="notifStore.popupData.type && notifStore.popupData.type.includes('FOLLOW')"
              xmlns="http://www.w3.org/2000/svg"
              viewBox="0 0 24 24"
              fill="currentColor"
          >
            <path
                fill-rule="evenodd"
                d="M7.5 6a4.5 4.5 0 119 0 4.5 4.5 0 01-9 0zM3.751 20.105a8.25 8.25 0 0116.498 0 .75.75 0 01-.437.695A18.683 18.683 0 0112 22.5c-2.786 0-5.433-.608-7.812-1.7a.75.75 0 01-.437-.695z"
                clip-rule="evenodd"
            />
          </svg>
          <svg
              v-else
              xmlns="http://www.w3.org/2000/svg"
              viewBox="0 0 24 24"
              fill="currentColor"
          >
            <path
                fill-rule="evenodd"
                d="M4.804 21.644A6.707 6.707 0 006 21.75a6.721 6.721 0 003.583-1.029c.774.182 1.584.279 2.417.279 5.322 0 9.75-3.97 9.75-9 0-5.03-4.428-9-9.75-9s-9.75 3.97-9.75 9c0 2.409 1.025 4.587 2.674 6.192.232.226.277.428.254.543a3.73 3.73 0 01-.814 1.686.75.75 0 00.44 1.223zM8.25 10.875a1.125 1.125 0 100 2.25 1.125 1.125 0 000-2.25zM10.875 12a1.125 1.125 0 112.25 0 1.125 1.125 0 01-2.25 0zm4.875-1.125a1.125 1.125 0 100 2.25 1.125 1.125 0 000-2.25z"
                clip-rule="evenodd"
            />
          </svg>
        </div>

        <div class="toast-content">
          <span class="toast-title">New Activity</span>
          <span class="toast-message">
            {{ notifStore.popupData.message }}
          </span>
        </div>
      </div>
    </transition>
  </div>
</template>

<!-- GLOBAL (NON-SCOPED) STYLES -->
<style>
.router-link-active {
  color: var(--color-accent);
  font-weight: 700;
}
.router-link-active .nav-icon {
  stroke-width: 2.2;
}

/* Toast transition */
.toast-enter-active,
.toast-leave-active {
  transition: all 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275);
}
.toast-enter-from,
.toast-leave-to {
  transform: translateX(50px);
  opacity: 0;
}

/* Page transition */
.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: opacity 0.2s ease;
}
.fade-slide-enter-from,
.fade-slide-leave-to {
  opacity: 0;
}
</style>

<!-- SCOPED LAYOUT / COMPONENT STYLES -->
<style scoped>
* {
  box-sizing: border-box;
}

.app-layout {
  display: grid;
  grid-template-columns: 280px 1fr 350px;
  min-height: 100vh;
  width: 100%;
  margin: 0;
  max-width: none;
}

.sidebar {
  background-color: var(--color-dark);
  color: var(--color-white);
  padding: 30px 15px;
  position: sticky;
  top: 0;
  height: 100vh;
  display: flex;
  flex-direction: column;
  box-shadow: 2px 0 10px rgba(0, 0, 0, 0.1);
  overflow-y: auto;
}
.sidebar::-webkit-scrollbar {
  display: none;
}

.logo {
  font-size: 28px;
  font-weight: 800;
  margin: 0 0 40px 15px;
  color: var(--color-white);
  letter-spacing: -0.5px;
}
.dot {
  color: var(--color-accent);
  font-size: 32px;
}

.nav-menu {
  flex-grow: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 16px;
  font-size: 19px;
  font-weight: 500;
  text-decoration: none;
  color: #aeb5bc;
  padding: 14px 20px;
  border-radius: 30px;
  transition: all 0.2s ease;
  border: none;
  background: none;
  width: 100%;
  text-align: left;
  cursor: pointer;
  position: relative;
}
.nav-item:hover {
  background-color: rgba(255, 255, 255, 0.1);
  color: var(--color-white);
}

.nav-icon {
  width: 26px;
  height: 26px;
  color: inherit;
}

.icon-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}
.notif-badge {
  position: absolute;
  top: -5px;
  right: -5px;
  background-color: #ef4444;
  color: white;
  font-size: 10px;
  font-weight: bold;
  padding: 2px 5px;
  border-radius: 10px;
  border: 2px solid var(--color-dark);
}

.logout-btn {
  color: #ef4444;
  margin-top: auto;
}
.logout-btn:hover {
  background-color: rgba(239, 68, 68, 0.1);
  color: #ef4444;
}

.user-mini-profile-link {
  text-decoration: none;
  margin-top: 20px;
  display: block;
}
.user-mini-profile {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 20px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 50px;
  transition: background 0.2s;
  cursor: pointer;
}
.user-mini-profile:hover {
  background: rgba(255, 255, 255, 0.15);
}

.avatar {
  width: 40px;
  height: 40px;
  background-color: var(--color-accent);
  color: var(--color-white);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  flex-shrink: 0;
  overflow: hidden;
}
.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.user-info {
  display: flex;
  flex-direction: column;
}
.username {
  font-size: 14px;
  font-weight: 700;
  color: var(--color-white);
}
.profile-label {
  font-size: 12px;
  color: #aeb5bc;
}

/* ADMIN STYLE */
.admin-link {
  color: #22c55e !important;
}
.admin-link:hover {
  background-color: rgba(34, 197, 94, 0.1) !important;
}
.admin-badge-sidebar {
  background-color: #22c55e;
  color: white;
  font-size: 10px;
  padding: 2px 6px;
  border-radius: 4px;
  font-weight: bold;
  margin-left: 8px;
  vertical-align: middle;
}
.user-welcome {
  padding: 0 15px 20px 15px;
}
.welcome-text {
  color: #aeb5bc;
  font-size: 14px;
  font-weight: 600;
}

/* TOAST LAYOUT */
.toast-notification {
  position: fixed;
  bottom: 30px;
  right: 30px;
  background-color: white;
  color: var(--color-dark);
  padding: 15px 20px;
  border-radius: 16px;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.15);
  border: 1px solid #f1f1f1;
  z-index: 2000;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 15px;
  max-width: 350px;
}
.toast-icon-box {
  width: 35px;
  height: 35px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.toast-icon-box svg {
  width: 20px;
  height: 20px;
}
.toast-icon-box[class*='LIKE'] {
  background-color: #fee2e2;
  color: #ef4444;
}
.toast-icon-box[class*='FOLLOW'] {
  background-color: #e0f2fe;
  color: #0284c7;
}
.toast-icon-box {
  background-color: #f1f5f9;
  color: #64748b;
}
.toast-content {
  display: flex;
  flex-direction: column;
}
.toast-title {
  font-size: 12px;
  font-weight: 700;
  color: #94a3b8;
  text-transform: uppercase;
}
.toast-message {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-dark);
}

/* MAIN CONTENT */
.content {
  position: relative; /* важно за transition-а */
  padding: 0;
  border-right: 1px solid rgba(0, 0, 0, 0.05);
  border-left: 1px solid rgba(0, 0, 0, 0.05);
  width: 100%;
}

/* TRENDING SIDEBAR */
.trending {
  padding: 30px 40px 30px 20px;
}
.trending-card {
  background-color: var(--color-white);
  border-radius: 16px;
  padding: 20px;
  box-shadow: var(--shadow-card);
  border: 1px solid rgba(0, 0, 0, 0.05);
}
.trending-header {
  margin-top: 0;
  font-size: 18px;
  border-bottom: 2px solid var(--color-bg);
  padding-bottom: 10px;
  margin-bottom: 15px;
  color: var(--color-dark);
  display: flex;
  align-items: center;
  gap: 8px;
}
.trending-icon {
  width: 24px;
  height: 24px;
  color: var(--color-accent);
}
.trend-list {
  list-style: none;
  padding: 0;
  margin: 0;
}
.trend-item {
  padding: 12px 0;
  border-bottom: 1px solid #f1f1f1;
  transition: background 0.2s;
}
.trend-item:last-child {
  border-bottom: none;
}
.trend-meta {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: var(--color-text-muted);
  margin-bottom: 4px;
}
.trend-author {
  font-weight: 600;
  color: var(--color-text-muted);
  text-decoration: none;
}
.trend-author:hover {
  text-decoration: underline;
  color: var(--color-dark);
}
.trend-title {
  margin: 0;
  font-size: 14px;
  font-weight: 700;
  color: var(--color-dark);
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.trend-status {
  font-size: 11px;
  color: var(--color-accent);
  font-weight: 600;
  margin-top: 4px;
  display: inline-block;
}
.empty-trend {
  color: var(--color-text-muted);
  font-size: 14px;
  text-align: center;
  padding: 20px 0;
}
.show-more-btn {
  background: none;
  border: none;
  color: var(--color-accent);
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  padding: 15px 0 0 0;
  width: 100%;
  text-align: left;
  transition: color 0.2s;
  display: block;
}
.show-more-btn:hover {
  color: var(--color-dark);
  text-decoration: underline;
}
</style>