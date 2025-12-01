<script setup>
import { ref, onMounted, computed, watch } from 'vue';
import { useRoute } from 'vue-router';
import usersService from '../services/users.service';

const route = useRoute();

const users = ref([]);
const loading = ref(true);
const error = ref(null);

const listType = computed(() => {
  return route.path.includes('/followers') ? 'Followers' : 'Following';
});

const targetUserId = computed(() => route.params.id);

const fetchUsers = async () => {
  loading.value = true;
  error.value = null;
  users.value = [];

  try {
    let response;
    if (listType.value === 'Followers') {
      response = await usersService.getFollowers(targetUserId.value);
    } else {
      response = await usersService.getFollowing(targetUserId.value);
    }
    users.value = response.data;
  } catch (err) {
    console.error(err);
    error.value = "Failed to load list.";
  } finally {
    loading.value = false;
  }
};

watch(() => route.path, () => {
  fetchUsers();
});

onMounted(() => {
  fetchUsers();
});
</script>

<template>
  <div class="list-container">
    <div class="list-header">
      <router-link :to="`/profile/${targetUserId}`" class="back-btn">
        ← Back to Profile
      </router-link>
      <h2>{{ listType }}</h2>
    </div>

    <div v-if="loading" class="state-msg">Loading users... ⏳</div>
    <div v-if="error" class="state-msg error">{{ error }}</div>

    <div v-else class="users-list">
      <div v-if="users.length === 0" class="empty-state">
        No users found in this list.
      </div>

      <div v-for="user in users" :key="user.id" class="user-card">
        <router-link :to="`/profile/${user.id}`" class="user-link">
          <div class="user-avatar">
            <span class="avatar-letter">{{ user.username.charAt(0).toUpperCase() }}</span>
          </div>
          <div class="user-info">
            <span class="username">@{{ user.username }}</span>
          </div>
        </router-link>

        <router-link :to="`/profile/${user.id}`" class="btn-view">
          View
        </router-link>
      </div>
    </div>
  </div>
</template>

<style scoped>
.list-container {
  max-width: 600px;
  margin: 0 auto;
  padding: 20px;
}

.list-header {
  border-bottom: 1px solid #e2e8f0;
  padding-bottom: 15px;
  margin-bottom: 20px;
}

.list-header h2 {
  margin: 10px 0 0 0;
  color: var(--color-dark);
}

.back-btn {
  text-decoration: none;
  color: var(--color-text-muted);
  font-size: 14px;
  font-weight: 600;
  display: inline-block;
  margin-bottom: 5px;
}
.back-btn:hover { color: var(--color-accent); }

.users-list {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.user-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: var(--color-white);
  padding: 12px 20px;
  border-radius: 12px;
  box-shadow: var(--shadow-card);
  transition: transform 0.1s;
}

.user-card:hover {
  background-color: #f8fafc;
}

.user-link {
  display: flex;
  align-items: center;
  gap: 15px;
  text-decoration: none;
  color: inherit;
  flex: 1;
}

.user-avatar {
  width: 45px;
  height: 45px;
  background-color: var(--color-dark);
  color: var(--color-white);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  font-size: 18px;
}

.user-info {
  display: flex;
  flex-direction: column;
}

.username {
  font-weight: 700;
  color: var(--color-dark);
}

.btn-view {
  text-decoration: none;
  background-color: transparent;
  border: 1px solid #e2e8f0;
  color: var(--color-dark);
  padding: 6px 16px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 600;
  transition: all 0.2s;
}

.btn-view:hover {
  border-color: var(--color-accent);
  color: var(--color-accent);
}

.state-msg { text-align: center; padding: 40px; color: var(--color-text-muted); }
.empty-state { text-align: center; color: var(--color-text-muted); font-style: italic; margin-top: 20px; }
.error { color: #ef4444; }
</style>