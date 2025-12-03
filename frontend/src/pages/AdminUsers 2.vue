<script setup>
import { ref, reactive, onMounted } from 'vue';
import { RouterLink } from 'vue-router'; // <--- Added import here
import adminService from '../services/admin.service';

// State
const users = ref([]);
const loading = ref(false);
const page = ref(0);
const pageSize = ref(10);
const hasMore = ref(true);

// Filters
const filters = reactive({
  username: '',
  email: '',
  isBlocked: '' // '' = All, 'true' = Blocked, 'false' = Active
});

// Fetch Data
const fetchUsers = async () => {
  loading.value = true;
  try {
    const params = {
      page: page.value,
      size: pageSize.value,
    };

    if (filters.username) params.username = filters.username;
    if (filters.email) params.email = filters.email;
    if (filters.isBlocked !== '') params.isBlocked = filters.isBlocked;

    const response = await adminService.search(params);
    users.value = response.data;

    // Simple pagination check
    hasMore.value = users.value.length === pageSize.value;
  } catch (error) {
    console.error("Failed to fetch users", error);
  } finally {
    loading.value = false;
  }
};

// Actions
const onSearch = () => {
  page.value = 0;
  fetchUsers();
};

const clearFilters = () => {
  filters.username = '';
  filters.email = '';
  filters.isBlocked = '';
  onSearch();
};

const nextPage = () => {
  if (hasMore.value) {
    page.value++;
    fetchUsers();
  }
};

const prevPage = () => {
  if (page.value > 0) {
    page.value--;
    fetchUsers();
  }
};

const toggleBlock = async (user) => {
  const action = user.blocked ? 'Unblock' : 'Block'; // Note: DTO might use 'blocked' or 'isBlocked'
  if (!confirm(`${action} user ${user.username}?`)) return;

  try {
    // Check if your DTO property is 'blocked' or 'isBlocked'
    // AdminUserResponseDTO usually has setBlocked/getBlocked
    if (user.blocked) {
      await adminService.unblockUser(user.id);
      user.blocked = false;
    } else {
      await adminService.blockUser(user.id);
      user.blocked = true;
    }
  } catch (error) {
    alert("Action failed.");
  }
};

onMounted(fetchUsers);
</script>

<template>
  <div class="admin-container">
    <div class="header">
      <h1>User Management</h1>
    </div>

    <!-- Search / Filter Bar -->
    <div class="filter-bar">
      <input v-model="filters.username" placeholder="Search Username..." class="form-input" @keyup.enter="onSearch"/>
      <input v-model="filters.email" placeholder="Search Email..." class="form-input" @keyup.enter="onSearch"/>
      
      <select v-model="filters.isBlocked" class="form-select" @change="onSearch">
        <option value="">All Statuses</option>
        <option value="false">Active</option>
        <option value="true">Blocked</option>
      </select>

      <button @click="onSearch" class="btn btn-primary">Search</button>
      <button @click="clearFilters" class="btn btn-outline">Clear</button>
    </div>

    <!-- Table -->
    <div class="table-wrapper">
      <div v-if="loading" class="loading-msg">Loading users...</div>
      
      <table v-else class="user-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>User</th>
            <th>Email</th>
            <th>Role</th>
            <th>Status</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="user in users" :key="user.id">
            <td>#{{ user.id }}</td>
            <td>
              <div class="user-cell">
                <div class="avatar-sm">{{ user.username.charAt(0).toUpperCase() }}</div>
                <span class="username-text">{{ user.username }}</span>
              </div>
            </td>
            <td>{{ user.email }}</td>
            <td>
              <span class="badge" :class="user.role === 'ADMIN' ? 'badge-admin' : 'badge-user'">
                {{ user.role }}
              </span>
            </td>
            <td>
              <!-- Check property name: blocked vs isBlocked -->
              <span class="badge" :class="user.blocked ? 'badge-blocked' : 'badge-active'">
                {{ user.blocked ? 'Blocked' : 'Active' }}
              </span>
            </td>
            <td>
              <div class="actions">
                <RouterLink :to="`/profile/${user.id}`" class="btn-link">View</RouterLink>
                <button 
                  @click="toggleBlock(user)" 
                  class="btn-sm"
                  :class="user.blocked ? 'btn-unblock' : 'btn-block'"
                >
                  {{ user.blocked ? 'Unblock' : 'Block' }}
                </button>
              </div>
            </td>
          </tr>
          <tr v-if="users.length === 0">
            <td colspan="6" class="empty-cell">No users found.</td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Pagination -->
    <div class="pagination">
      <button @click="prevPage" :disabled="page === 0" class="btn-page">Previous</button>
      <span class="page-info">Page {{ page + 1 }}</span>
      <button @click="nextPage" :disabled="!hasMore" class="btn-page">Next</button>
    </div>
  </div>
</template>

<style scoped>
.admin-container { max-width: 1000px; margin: 0 auto; padding: 20px; }
.header h1 { color: var(--color-dark); margin-bottom: 20px; }

/* Filters */
.filter-bar { 
  display: flex; gap: 10px; margin-bottom: 20px; flex-wrap: wrap; 
  background: white; padding: 15px; border-radius: 12px; box-shadow: var(--shadow-card);
}
.form-input, .form-select { 
  padding: 8px 12px; border: 1px solid #e2e8f0; border-radius: 8px; 
  font-size: 14px; outline: none; 
}
.form-input:focus, .form-select:focus { border-color: var(--color-accent); }

/* Table */
.table-wrapper { background: white; border-radius: 12px; overflow: hidden; box-shadow: var(--shadow-card); }
.user-table { width: 100%; border-collapse: collapse; font-size: 14px; }
.user-table th, .user-table td { padding: 12px 15px; text-align: left; border-bottom: 1px solid #f1f1f1; }
.user-table th { background-color: #f8fafc; font-weight: 600; color: #64748b; }
.user-cell { display: flex; align-items: center; gap: 10px; }
.avatar-sm { 
  width: 30px; height: 30px; background: var(--color-dark); color: white; 
  border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 12px; font-weight: bold; 
}
.username-text { font-weight: 600; color: var(--color-dark); }

/* Badges */
.badge { padding: 4px 8px; border-radius: 4px; font-size: 11px; font-weight: bold; text-transform: uppercase; }
.badge-admin { background: #e0f2fe; color: #0284c7; }
.badge-user { background: #f1f5f9; color: #64748b; }
.badge-active { background: #dcfce7; color: #16a34a; }
.badge-blocked { background: #fee2e2; color: #ef4444; }

/* Actions */
.actions { display: flex; gap: 10px; align-items: center; }
.btn-link { color: var(--color-accent); text-decoration: none; font-weight: 600; font-size: 13px; }
.btn-link:hover { text-decoration: underline; }

/* Buttons */
.btn { padding: 8px 16px; border-radius: 8px; border: none; cursor: pointer; font-weight: 600; font-size: 14px; }
.btn-primary { background: var(--color-dark); color: white; }
.btn-outline { background: transparent; border: 1px solid #ccc; color: #666; }
.btn-sm { padding: 4px 10px; border-radius: 4px; font-size: 12px; border: none; cursor: pointer; font-weight: 600; }
.btn-block { background: #fee2e2; color: #ef4444; }
.btn-block:hover { background: #fecaca; }
.btn-unblock { background: #fef3c7; color: #d97706; }
.btn-unblock:hover { background: #fde68a; }

/* Pagination */
.pagination { display: flex; justify-content: center; align-items: center; gap: 15px; margin-top: 20px; }
.btn-page { padding: 8px 16px; background: white; border: 1px solid #e2e8f0; border-radius: 8px; cursor: pointer; }
.btn-page:disabled { opacity: 0.5; cursor: not-allowed; }
.loading-msg, .empty-cell { padding: 20px; text-align: center; color: #888; }
</style>