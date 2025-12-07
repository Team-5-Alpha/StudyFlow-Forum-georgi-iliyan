<script setup>
import { ref, reactive, onMounted, watch } from 'vue';
import { RouterLink, useRouter } from 'vue-router';
import adminService from '../services/admin.service';
import usersService from '../services/users.service';
import { useAuthStore } from '../stores/auth.store';
import ConfirmModal from '../components/modals/ConfirmModal.vue';
import AdminAlerts from '../components/admin/AdminAlerts.vue';
import { useAdminAlert } from '../composables/useAdminAlert';

const authStore = useAuthStore();
const router = useRouter();


const showConfirm = ref(false);
const confirmConfig = reactive({
  title: '',
  message: '',
  confirmText: 'Confirm',
  action: null,
  payload: null,
});

const { addAlert } = useAdminAlert();

const openConfirm = (title, message, action, payload = null, confirmText = 'Confirm') => {
  confirmConfig.title = title;
  confirmConfig.message = message;
  confirmConfig.confirmText = confirmText;
  confirmConfig.action = action;
  confirmConfig.payload = payload;
  showConfirm.value = true;
};

const handleConfirm = async () => {
  showConfirm.value = false;
  try {
    if (confirmConfig.action) await confirmConfig.action(confirmConfig.payload);
  } catch (error) {
    addAlert('Action failed', 'error');
  }
};
// State
const users = ref([]);
const loading = ref(false);
const page = ref(0);
const pageSize = ref(10);
const hasMore = ref(true);

// Filters
const filters = reactive({ username: '', email: '', isBlocked: '' });

// Suggestions State
const usernameSuggestions = ref([]);
const showUsernameSuggestions = ref(false);
let _debounceTimer = null;

// Debounce
const debounce = (fn, delay = 300) => {
  return (...args) => {
    clearTimeout(_debounceTimer);
    _debounceTimer = setTimeout(() => fn(...args), delay);
  };
};

const fetchUsernameSuggestions = async (query) => {
  if (!query || query.length < 2) {
    usernameSuggestions.value = [];
    showUsernameSuggestions.value = false;
    return;
  }
  try {
    const res = await usersService.search({ username: query, size: 6 });
    usernameSuggestions.value = Array.isArray(res.data) ? res.data.slice(0, 6) : [];
    showUsernameSuggestions.value = usernameSuggestions.value.length > 0;
  } catch (e) {
    usernameSuggestions.value = [];
  }
};

const debouncedFetchSuggestions = debounce(fetchUsernameSuggestions, 300);

watch(() => filters.username, (val) => {
  debouncedFetchSuggestions(val);
});

const selectUsernameSuggestion = (u) => {
  filters.username = u.username;
  showUsernameSuggestions.value = false;
  page.value = 0;
  fetchUsers();
};

// Fetch Users
const fetchUsers = async () => {
  loading.value = true;
  try {
    const params = { page: page.value, size: pageSize.value };
    if (filters.username) params.username = filters.username;
    if (filters.email) params.email = filters.email;
    if (filters.isBlocked !== '') params.isBlocked = filters.isBlocked;

    const response = await adminService.search(params);
    users.value = response.data;
    hasMore.value = users.value.length === pageSize.value;
  } catch (error) {
    console.error("Failed to fetch users", error);
  } finally {
    loading.value = false;
  }
};

// Search / Clear
const onSearch = () => { page.value = 0; fetchUsers(); };

const clearFilters = () => {
  filters.username = '';
  filters.email = '';
  filters.isBlocked = '';
  usernameSuggestions.value = [];
  showUsernameSuggestions.value = false;
  onSearch();
};

// Pagination
const nextPage = () => { if (hasMore.value) { page.value++; fetchUsers(); } };
const prevPage = () => { if (page.value > 0) { page.value--; fetchUsers(); } };

// Block / Unblock
const toggleBlock = (user) => {
  const actionName = user.blocked ? 'Unblock' : 'Block';
  openConfirm(
      `${actionName} user ${user.username}?`,
      'This action cannot be undone.',
      async () => {
        if (user.blocked) await adminService.unblockUser(user.id);
        else await adminService.blockUser(user.id);
        await fetchUsers();
        addAlert(`${actionName}ed ${user.username} successfully`);
      },
      user
  );
};

const promoteUser = (user) => {
  openConfirm(
      `Promote user ${user.username} to Admin?`,
      'This action cannot be undone.',
      async () => {
        await adminService.promoteUser(user.id);
        await fetchUsers();
        addAlert(`User ${user.username} promoted to Admin`);
      },
      user
  );
};


onMounted(async () => {
  if (authStore.user?.role !== 'ADMIN') {
    router.push('/');
    return;
  }
  await fetchUsers();
});
</script>

<template>
  <div class="admin-container">

    <!-- HEADER -->
    <div class="page-header">
      <div class="header-content">
        <h1>Admin Panel</h1>
        <p class="subtitle">Manage users, roles, and permissions</p>
      </div>
      <div class="header-icon">
        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" d="M9 12.75L11.25 15 15 9.75m-3-7.036A11.959 11.959 0 013.598 6 11.99 11.99 0 003 9.749c0 5.592 3.824 10.29 9 11.623 5.176-1.332 9-6.03 9-11.622 0-1.31-.21-2.571-.598-3.751h-.152c-3.196 0-6.1-1.248-8.25-3.285z" />
        </svg>
      </div>
    </div>

    <!-- FILTERS -->
    <div class="filter-section">
      <div class="filter-row">

        <!-- Username Search -->
        <div class="search-wrapper">
          <span class="search-icon">
             <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" d="M21 21l-5.197-5.197m0 0A7.5 7.5 0 105.196 5.196a7.5 7.5 0 0010.607 10.607z" /></svg>
          </span>
          <input
              v-model="filters.username"
              placeholder="Search username..."
              class="search-input"
              @keyup.enter="onSearch"
              @focus="showUsernameSuggestions = usernameSuggestions.length > 0"
          />

          <!-- Suggestions -->
          <ul v-if="showUsernameSuggestions" class="suggestions">
            <li
                v-for="s in usernameSuggestions"
                :key="s.id"
                @click="selectUsernameSuggestion(s)"
                class="suggestion-item"
            >
              <div class="s-meta">
                <div class="s-avatar">{{ s.username.charAt(0).toUpperCase() }}</div>
                <div class="s-info">
                  <span class="s-username">{{ s.username }}</span>
                  <span class="s-email">{{ s.email }}</span>
                </div>
              </div>
            </li>
          </ul>

        </div>

        <!-- Email Search -->
        <div class="search-wrapper">
          <span class="search-icon">
             <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" d="M21.75 6.75v10.5a2.25 2.25 0 01-2.25 2.25h-15a2.25 2.25 0 01-2.25-2.25V6.75m19.5 0A2.25 2.25 0 0019.5 4.5h-15a2.25 2.25 0 00-2.25 2.25m19.5 0v.243a2.25 2.25 0 01-1.07 1.916l-7.5 4.615a2.25 2.25 0 01-2.36 0L3.32 8.91a2.25 2.25 0 01-1.07-1.916V6.75" /></svg>
          </span>
          <input v-model="filters.email" placeholder="Search email..." class="search-input" @keyup.enter="onSearch"/>
        </div>

        <!-- Status Select -->
        <div class="select-wrapper">
          <select v-model="filters.isBlocked" class="custom-select" @change="onSearch">
            <option value="">All Statuses</option>
            <option value="false">Active Only</option>
            <option value="true">Blocked Only</option>
          </select>
          <span class="select-arrow">▼</span>
        </div>

        <div class="filter-buttons">
          <button @click="onSearch" class="btn-search">Search</button>
          <button @click="clearFilters" class="btn-clear">Clear</button>
        </div>

      </div>
    </div>

    <!-- TABLE -->
    <div class="table-container">
      <div v-if="loading" class="loading-state">Loading users...</div>

      <table v-else class="users-table">
        <thead>
        <tr>
          <th style="width: 25%">User</th>
          <th style="width: 25%">Email</th>
          <th style="width: 10%">Role</th>
          <th style="width: 10%">Status</th>
          <th style="width: 30%">Actions</th>
        </tr>
        </thead>
        <tbody>

        <tr v-for="user in users" :key="user.id">
          <td>
            <div class="user-cell">
              <div class="avatar-sm">
                <img v-if="user.profilePhotoURL" :src="user.profilePhotoURL" class="avatar-img" />
                <span v-else>{{ user.username.charAt(0).toUpperCase() }}</span>
              </div>
              <div class="user-info">
                <span class="u-name">{{ user.username }}</span>
                <span class="u-id">#{{ user.id }}</span>
              </div>
            </div>
          </td>

          <td class="email-cell">{{ user.email }}</td>

          <td>
               <span class="badge" :class="user.role === 'ADMIN' ? 'badge-admin' : 'badge-user'">
                 {{ user.role }}
               </span>
          </td>

          <td>
               <span class="badge" :class="user.blocked ? 'badge-blocked' : 'badge-active'">
                 {{ user.blocked ? 'Blocked' : 'Active' }}
               </span>
          </td>

          <td>
            <div class="actions-grid">

              <!-- VIEW -->
              <RouterLink :to="`/profile/${user.id}`" class="action-icon-btn view" title="View Profile">
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.8" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" d="M2.036 12.322a1.012 1.012 0 010-.639C3.423 7.51 7.36 4.5 12 4.5c4.638 0 8.573 3.007 9.963 7.178.07.207.07.431 0 .639C20.577 16.49 16.64 19.5 12 19.5c-4.638 0-8.573-3.007-9.963-7.178z" /><path stroke-linecap="round" stroke-linejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" /></svg>
              </RouterLink>

              <!-- BLOCK / UNBLOCK -->
              <button
                  @click="toggleBlock(user)"
                  class="action-icon-btn"
                  :class="user.blocked ? 'unblock' : 'block'"
                  :title="user.blocked ? 'Unblock' : 'Block'"
              >
                <svg v-if="!user.blocked" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.8" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" d="M18.364 18.364A9 9 0 005.636 5.636m12.728 12.728A9 9 0 015.636 5.636m12.728 12.728L5.636 5.636" /></svg>

                <svg v-else xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.8" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" d="M9 12.75L11.25 15 15 9.75M21 12a9 9 0 11-18 0 9 9 0 0118 0z" /></svg>
              </button>

              <!-- PROMOTE -->
              <div class="promote-slot">
                <button
                    v-if="user.role !== 'ADMIN'"
                    @click="promoteUser(user)"
                    class="action-icon-btn promote"
                    title="Promote to Admin"
                >
                  <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.8" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M4.5 12.75l7.5-7.5 7.5 7.5m-15 6l7.5-7.5 7.5 7.5" />
                  </svg>
                </button>
              </div>

            </div>
          </td>
        </tr>

        <tr v-if="users.length === 0">
          <td colspan="5" class="empty-cell">No users found matching criteria.</td>
        </tr>

        </tbody>
      </table>
    </div>

    <!-- PAGINATION -->
    <div class="pagination">
      <button @click="prevPage" :disabled="page === 0" class="page-btn">Previous</button>
      <span class="page-count">Page {{ page + 1 }}</span>
      <button @click="nextPage" :disabled="!hasMore" class="page-btn">Next</button>
    </div>

    <ConfirmModal
        :isOpen="showConfirm"
        :title="confirmConfig.title"
        :message="confirmConfig.message"
        :confirmText="confirmConfig.confirmText"
        :showIcon="false"
        @confirm="handleConfirm"
        @cancel="showConfirm = false"
    />

    <AdminAlerts />


  </div>
</template>

<style scoped>
/* Container */
.admin-container { max-width: 1200px; margin: 0 auto; padding: 30px 20px; }

.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 30px; }
.header-content h1 { margin: 0; font-size: 28px; color: var(--color-dark); }
.subtitle { margin: 5px 0 0 0; color: #64748b; font-size: 14px; }
.header-icon { width: 48px; height: 48px; color: var(--color-accent); background: #fdf6e7; border-radius: 12px; display: flex; align-items: center; justify-content: center; }
.header-icon svg { width: 28px; height: 28px; }

/* Filters */
.filter-section { margin-bottom: 25px; }
.filter-row { display: flex; gap: 15px; flex-wrap: wrap; align-items: center; }

.search-wrapper { position: relative; display: flex; align-items: center; background: white; border: 1px solid #e2e8f0; border-radius: 50px; padding: 8px 16px; min-width: 250px; transition: all 0.2s; }
.search-wrapper:focus-within { border-color: var(--color-accent); box-shadow: 0 0 0 3px rgba(182, 157, 116, 0.1); }
.search-icon { width: 18px; height: 18px; color: #94a3b8; margin-right: 8px; }
.search-input { border: none; outline: none; font-size: 14px; width: 100%; color: var(--color-dark); }

/* Select */
.select-wrapper { position: relative; min-width: 180px; }
.custom-select { width: 100%; appearance: none; background: white; border: 1px solid #e2e8f0; border-radius: 50px; padding: 10px 35px 10px 16px; font-size: 14px; color: var(--color-dark); outline: none; cursor: pointer; }
.custom-select:focus { border-color: var(--color-accent); }
.select-arrow { position: absolute; right: 15px; top: 50%; transform: translateY(-50%); font-size: 10px; color: #94a3b8; pointer-events: none; }

.filter-buttons { display: flex; gap: 10px; margin-left: auto; }
.btn-search { background: var(--color-dark); color: white; border: none; padding: 10px 24px; border-radius: 50px; font-weight: 600; font-size: 14px; cursor: pointer; transition: all 0.2s; }
.btn-search:hover { background: var(--color-accent); transform: translateY(-1px); }
.btn-clear { background: white; border: 1px solid #e2e8f0; color: #64748b; padding: 10px 24px; border-radius: 50px; font-weight: 600; font-size: 14px; cursor: pointer; transition: all 0.2s; }
.btn-clear:hover { border-color: var(--color-dark); color: var(--color-dark); }

/* Suggestions */
.suggestions {
  position: absolute;
  top: 115%;
  left: 0;
  width: 100%;
  background: white;
  border-radius: 12px;
  box-shadow: 0 10px 25px rgba(0,0,0,0.1);
  z-index: 50;
  border: 1px solid #e2e8f0;
  overflow: hidden;
  padding: 6px 0;
}

.suggestion-item { padding: 10px 14px; cursor: pointer; transition: background 0.2s; }
.suggestion-item:hover { background: #f8fafc; }

.s-meta { display: flex; align-items: center; gap: 12px; }
.s-avatar { width: 34px; height: 34px; background: var(--color-accent); color: white; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-weight: bold; font-size: 14px; }
.s-info { display: flex; flex-direction: column; justify-content: center; line-height: 1.2; }
.s-username { font-weight: 700; color: var(--color-dark); font-size: 14px; }
.s-email { font-size: 12px; color: #94a3b8; }

/* Table */
.table-container { background: white; border-radius: 16px; box-shadow: 0 4px 20px rgba(0,0,0,0.05); overflow: hidden; border: 1px solid #f1f1f1; }
.users-table { width: 100%; border-collapse: collapse; }
.users-table th { background: #fcfcfc; color: #64748b; font-weight: 700; font-size: 13px; text-transform: uppercase; padding: 15px 20px; text-align: left; border-bottom: 1px solid #e2e8f0; }
.users-table td { padding: 15px 20px; border-bottom: 1px solid #f8fafc; vertical-align: middle; color: #334155; font-size: 14px; }
.users-table tr:hover { background: #fbfbfb; }

.user-cell { display: flex; align-items: center; gap: 12px; }
.avatar-sm { width: 36px; height: 36px; background: var(--color-dark); color: white; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-weight: bold; font-size: 14px; overflow: hidden; }
.avatar-img { width: 100%; height: 100%; object-fit: cover; }
.u-name { font-weight: 700; color: var(--color-dark); display: block; }
.u-id { font-size: 11px; color: #94a3b8; }
.email-cell { color: #64748b; }

/* Badges */
.badge { padding: 4px 10px; border-radius: 20px; font-size: 11px; font-weight: 700; text-transform: uppercase; }
.badge-admin { background: #e0f2fe; color: #0284c7; }
.badge-user { background: #f1f5f9; color: #64748b; }
.badge-active { background: #dcfce7; color: #16a34a; }
.badge-blocked { background: #fee2e2; color: #ef4444; }

/* Actions Grid */
.actions-grid {
  display: grid;
  grid-template-columns: repeat(3, 32px);
  gap: 8px;
  align-items: center;
}
.promote-slot { width: 32px; height: 32px; display: flex; align-items: center; justify-content: center; }

.action-icon-btn {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  border: 1px solid transparent;
  background: transparent;
  color: #64748b;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
}
.action-icon-btn svg { width: 18px; height: 18px; }
.action-icon-btn:hover { background: #f1f5f9; color: var(--color-dark); }

.action-icon-btn.view:hover { color: var(--color-accent); background: #fff9ed; }
.action-icon-btn.block:hover { color: #ef4444; background: #fee2e2; }
.action-icon-btn.unblock { color: #d97706; background: #fef3c7; }
.action-icon-btn.promote { color: #16a34a; }
.action-icon-btn.promote:hover { background: #dcfce7; }


/* Pagination */
.pagination { display: flex; justify-content: center; align-items: center; gap: 15px; margin-top: 25px; }
.page-btn { background: white; border: 1px solid #e2e8f0; padding: 8px 20px; border-radius: 50px; cursor: pointer; color: var(--color-dark); font-weight: 600; font-size: 14px; transition: all 0.2s; }
.page-btn:hover:not(:disabled) { border-color: var(--color-dark); background: #f8fafc; }
.page-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.page-count { font-size: 14px; color: #94a3b8; font-weight: 600; }

/* Misc */
.loading-state { text-align: center; padding: 50px; color: #94a3b8; }
.empty-cell { text-align: center; padding: 30px; color: #94a3b8; font-style: italic; }
</style>