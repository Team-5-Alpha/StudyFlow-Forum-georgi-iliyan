<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import usersService from '../../services/users.service';

const router = useRouter();
const searchQuery = ref('');
const searchResults = ref([]);
const isFocused = ref(false);
const loading = ref(false);

let timeout = null;

const handleInput = () => {
  if (timeout) clearTimeout(timeout);

  if (searchQuery.value.length < 2) {
    searchResults.value = [];
    return;
  }

  loading.value = true;
  timeout = setTimeout(async () => {
    try {
      const response = await usersService.search({
        username: searchQuery.value,
        size: 5
      });
      searchResults.value = response.data || [];
    } catch (error) {
      console.error("Search failed", error);
      searchResults.value = [];
    } finally {
      loading.value = false;
    }
  }, 300);
};

const goToProfile = (userId) => {
  searchQuery.value = '';
  searchResults.value = [];
  isFocused.value = false;
  router.push(`/profile/${userId}`);
};

const clearSearch = () => {
  searchQuery.value = '';
  searchResults.value = [];
};
</script>

<template>
  <div class="user-search-container">
    <div class="search-wrapper" :class="{ active: isFocused }">
      <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.8" stroke="currentColor" class="search-icon">
        <path stroke-linecap="round" stroke-linejoin="round" d="M21 21l-5.197-5.197m0 0A7.5 7.5 0 105.196 5.196a7.5 7.5 0 0010.607 10.607z" />
      </svg>

      <input
          type="text"
          v-model="searchQuery"
          placeholder="Search users..."
          class="search-input"
          @focus="isFocused = true"
          @blur="setTimeout(() => isFocused = false, 200)"
          @input="handleInput"
      />

      <button v-if="searchQuery" @click="clearSearch" class="clear-btn" aria-label="Clear search">✕</button>
    </div>

    <!-- DROPDOWN RESULTS -->
    <div v-if="isFocused && searchQuery.length >= 2" class="search-dropdown" role="listbox">
      <div v-if="loading" class="dropdown-info">Searching...</div>

      <div v-else-if="searchResults.length === 0" class="dropdown-info no-results">
        No users found.
      </div>

      <div v-else class="results-list">
        <div
            v-for="user in searchResults"
            :key="user.id"
            class="result-item"
            role="option"
            tabindex="0"
            @click="goToProfile(user.id)"
            @keyup.enter="goToProfile(user.id)"
        >
          <div class="result-avatar">
            <img v-if="user.profilePhotoURL" :src="user.profilePhotoURL" alt="Avatar" />
            <span v-else>{{ user.username.charAt(0).toUpperCase() }}</span>
          </div>
          <div class="result-info">
            <span class="username">@{{ user.username }}</span>
            <span class="fullname">{{ user.firstName }} {{ user.lastName }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.user-search-container {
  position: relative;
  width: 240px;
  transition: width 0.2s;
}
.user-search-container:focus-within { width: 280px; }

.search-wrapper {
  display: flex; align-items: center;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 50px;
  padding: 8px 16px;
  transition: all 0.2s;
}
.search-wrapper.active {
  border-color: var(--color-accent);
  box-shadow: 0 0 0 3px rgba(182, 157, 116, 0.15);
}

.search-icon { width: 18px; height: 18px; color: #94a3b8; margin-right: 8px; }
.search-wrapper.active .search-icon { color: var(--color-dark); }

.search-input {
  border: none; background: transparent; outline: none;
  font-size: 14px; width: 100%; color: var(--color-dark);
}
.search-input::placeholder { color: #94a3b8; }

.clear-btn {
  background: none; border: none; color: #94a3b8; cursor: pointer; padding: 0 0 0 5px;
}
.clear-btn:hover { color: var(--color-dark); }

/* DROPDOWN */
.search-dropdown {
  position: absolute; top: 115%; left: 0; width: 100%;
  background: white; border: 1px solid #e2e8f0; border-radius: 16px;
  box-shadow: 0 10px 25px rgba(0,0,0,0.1); z-index: 100;
  overflow: hidden;
}

.dropdown-info { padding: 15px; text-align: center; color: #94a3b8; font-size: 13px; }
.dropdown-info.no-results { font-weight: 500; }

.result-item {
  display: flex; align-items: center; gap: 10px;
  padding: 10px 15px; cursor: pointer; border-bottom: 1px solid #f8fafc;
  transition: background 0.2s;
}
.result-item:last-child { border-bottom: none; }
.result-item:hover { background-color: #f1f5f9; }
.result-item:focus { outline: 2px solid var(--color-accent); outline-offset: -2px; }

.result-avatar {
  width: 32px; height: 32px; border-radius: 50%;
  background: var(--color-dark); color: white;
  display: flex; align-items: center; justify-content: center;
  font-weight: bold; font-size: 12px; overflow: hidden; flex-shrink: 0;
}
.result-avatar img { width: 100%; height: 100%; object-fit: cover; }

.result-info { display: flex; flex-direction: column; }
.username { font-weight: 700; font-size: 13px; color: var(--color-dark); }
.fullname { font-size: 11px; color: #64748b; }
</style>