<script setup>
import { ref, onMounted, computed, watch } from 'vue';
import { useRoute } from 'vue-router';
import { useAuthStore } from '../stores/auth.store';
import usersService from '../services/users.service';
import PostCard from '../components/PostCard.vue';
import PasswordConfirmModal from '../components/PasswordConfirmModal.vue';
import { getAuth, updateEmail, updatePassword, EmailAuthProvider, reauthenticateWithCredential } from 'firebase/auth';

const route = useRoute();
const authStore = useAuthStore();

// State
const profile = ref(null);
const posts = ref([]);
const followersList = ref([]);
const followingList = ref([]);
const loading = ref(true);
const error = ref(null);
const isFollowLoading = ref(false);
const isEditing = ref(false);
const editForm = ref({ firstName: '', lastName: '', email: '', password: '', profilePhotoURL: '' });

// Password Modal State
const isPasswordModalOpen = ref(false);
let passwordResolve = null;

// SUCCESS TOAST STATE
const successMessage = ref(null);

// Computed
const isCurrentUser = computed(() => authStore.user && profile.value && authStore.user.id === profile.value.id);
const isFollowing = computed(() => authStore.user && followersList.value && followersList.value.some(user => user.id === authStore.user.id));
const formatDate = (dateString) => new Date(dateString).toLocaleDateString('en-US', { year: 'numeric', month: 'long', day: 'numeric' });

// Fetch Profile
const fetchProfileData = async () => {
  loading.value = true;
  error.value = null;
  const targetId = route.params.id || authStore.user?.id;
  if (!targetId) { error.value = "User not found."; loading.value = false; return; }
  try {
    const [userRes, postsRes, followersRes, followingRes] = await Promise.all([
      usersService.getById(targetId),
      usersService.getPostsByUser(targetId),
      usersService.getFollowers(targetId),
      usersService.getFollowing(targetId)
    ]);
    profile.value = userRes.data;
    posts.value = postsRes.data;
    followersList.value = followersRes.data;
    followingList.value = followingRes.data;
    editForm.value = {
      firstName: profile.value.firstName,
      lastName: profile.value.lastName,
      email: profile.value.email,
      password: '',
      profilePhotoURL: profile.value.profilePhotoURL || ''
    };
  } catch (err) { error.value = "Failed to load profile."; } finally { loading.value = false; }
};

const toggleFollow = async () => {
  if (isFollowLoading.value || !profile.value) return;
  isFollowLoading.value = true;
  try {
    if (isFollowing.value) { await usersService.unfollow(profile.value.id); followersList.value = followersList.value.filter(u => u.id !== authStore.user.id); }
    else { await usersService.follow(profile.value.id); followersList.value.push(authStore.user); }
  } catch (err) { alert("Action failed: " + err.message); } finally { isFollowLoading.value = false; }
};

// Modal Logic
const askForPassword = () => { isPasswordModalOpen.value = true; return new Promise((resolve) => { passwordResolve = resolve; }); };
const onPasswordConfirm = (password) => { isPasswordModalOpen.value = false; if (passwordResolve) passwordResolve(password); };
const onPasswordCancel = () => { isPasswordModalOpen.value = false; if (passwordResolve) passwordResolve(null); };

// Helper за Success Toast
const showSuccess = (msg) => {
  successMessage.value = msg;
  setTimeout(() => { successMessage.value = null; }, 3000);
};

// SAVE PROFILE
const saveProfile = async () => {
  const auth = getAuth();
  const firebaseUser = auth.currentUser;
  const emailChanged = editForm.value.email !== profile.value.email;
  const passwordChanged = editForm.value.password && editForm.value.password.length >= 6;

  try {
    if (emailChanged || passwordChanged) {
      const currentPass = await askForPassword();
      if (!currentPass) throw new Error("Action cancelled.");
      const credential = EmailAuthProvider.credential(firebaseUser.email, currentPass);
      await reauthenticateWithCredential(firebaseUser, credential);
    }

    const updatePayload = {
      firstName: editForm.value.firstName,
      lastName: editForm.value.lastName,
      email: editForm.value.email,
      profilePhotoURL: editForm.value.profilePhotoURL || null
    };

    if (passwordChanged) {
      updatePayload.password = editForm.value.password;
    }

    const updatedUser = await usersService.update(profile.value.id, updatePayload);

    if (emailChanged) {
      await updateEmail(firebaseUser, editForm.value.email);
      await firebaseUser.getIdToken(true);
    }
    if (passwordChanged) {
      await updatePassword(firebaseUser, editForm.value.password);
    }

    profile.value = updatedUser.data;
    isEditing.value = false;

    if (isCurrentUser.value) {
      authStore.user = updatedUser.data;
      localStorage.setItem('user', JSON.stringify(updatedUser.data));
    }
    editForm.value.password = '';
    showSuccess("Profile updated successfully!");

  } catch (err) {
    console.error(err);
    let msg = err.message;
    if (err.response?.data?.message) msg = err.response.data.message;
    alert("Error: " + msg);
    if (msg.includes("401") || msg.includes("403")) {
      alert("Sync error. Please logout and login again.");
      authStore.logout();
      window.location.href = '/login';
    }
  }
};

// --- НОВО: Изтриване на пост ---
const removePost = (postId) => {
  posts.value = posts.value.filter(p => p.id !== postId);
};

watch(() => route.params.id, () => { fetchProfileData(); });
onMounted(() => { fetchProfileData(); });
</script>

<template>
  <div class="profile-container">
    <div v-if="loading" class="state-msg">Loading profile... ⏳</div>
    <div v-if="error" class="state-msg error">{{ error }}</div>

    <div v-else-if="profile">
      <div class="profile-header-card">
        <div class="cover-photo"></div>
        <div class="profile-info-row">
          <div class="profile-avatar-large"><img v-if="profile.profilePhotoURL" :src="profile.profilePhotoURL" alt="Avatar" class="avatar-img" /><span v-else>{{ profile.username.charAt(0).toUpperCase() }}</span></div>
          <div class="profile-actions">
            <button v-if="isCurrentUser" @click="isEditing = !isEditing" class="btn-outline">{{ isEditing ? 'Cancel Edit' : 'Edit Profile' }}</button>
            <button v-else @click="toggleFollow" class="btn-primary" :class="{ 'btn-following': isFollowing }" :disabled="isFollowLoading">{{ isFollowing ? 'Unfollow' : 'Follow' }}</button>
          </div>
        </div>
        <div class="profile-details">
          <h2 class="profile-name">{{ profile.firstName }} {{ profile.lastName }}</h2>
          <div class="profile-handle">@{{ profile.username }}</div>
          <div class="profile-dates">📅 Joined {{ formatDate(profile.createdAt) }}</div>
          <div class="profile-stats">
            <span><strong>{{ posts.length }}</strong> Posts</span>
            <router-link :to="`/profile/${profile.id}/followers`" class="stat-link"><strong>{{ followersList.length }}</strong> Followers</router-link>
            <router-link :to="`/profile/${profile.id}/following`" class="stat-link"><strong>{{ followingList.length }}</strong> Following</router-link>
          </div>
        </div>
      </div>

      <div v-if="isEditing" class="edit-form-card">
        <h3>Edit Profile Details</h3>
        <form @submit.prevent="saveProfile">
          <div class="form-row">
            <div class="form-group"><label>First Name</label><input v-model="editForm.firstName" type="text" required minlength="4" maxlength="32" /></div>
            <div class="form-group"><label>Last Name</label><input v-model="editForm.lastName" type="text" required minlength="4" maxlength="32" /></div>
          </div>
          <div class="form-group"><label>Email Address</label><input v-model="editForm.email" type="email" required minlength="6" maxlength="128" /></div>
          <div class="form-group"><label>New Password</label><input v-model="editForm.password" type="password" placeholder="Min 6 characters" minlength="6" maxlength="128" autocomplete="new-password" /><small style="color: #888;">Only fill this if you want to change your password.</small></div>
          <div class="form-group"><label>Profile Photo URL</label><input v-model="editForm.profilePhotoURL" type="url" placeholder="https://..." maxlength="255" /></div>
          <button type="submit" class="btn-primary" style="margin-top: 10px;">Save Changes</button>
        </form>
      </div>

      <div v-else class="posts-section">
        <h3 class="section-title">Posts by @{{ profile.username }}</h3>
        <div v-if="posts.length === 0" class="no-posts">This user hasn't posted anything yet.</div>

        <PostCard
            v-for="post in posts"
            :key="post.id"
            :post="post"
            @post-deleted="removePost"
        />
      </div>
    </div>

    <PasswordConfirmModal :isOpen="isPasswordModalOpen" @confirm="onPasswordConfirm" @cancel="onPasswordCancel" />

    <transition name="fade-slide">
      <div v-if="successMessage" class="success-toast">
        <div class="check-icon">
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.5" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" d="M4.5 12.75l6 6 9-13.5" />
          </svg>
        </div>
        <span>{{ successMessage }}</span>
      </div>
    </transition>

  </div>
</template>

<style scoped>
.profile-container { padding-bottom: 40px; }
.profile-header-card { background-color: var(--color-white); border-bottom: 1px solid rgba(0,0,0,0.05); padding-bottom: 20px; margin-bottom: 20px; }
.cover-photo { height: 150px; background-color: var(--color-dark); background: linear-gradient(45deg, var(--color-dark), #2c3e50); }
.profile-info-row { display: flex; justify-content: space-between; align-items: flex-end; padding: 0 20px; margin-top: -50px; }
.profile-avatar-large { width: 100px; height: 100px; border-radius: 50%; border: 4px solid var(--color-white); background-color: var(--color-accent); color: var(--color-white); display: flex; align-items: center; justify-content: center; font-size: 40px; font-weight: 800; overflow: hidden; }
.avatar-img { width: 100%; height: 100%; object-fit: cover; }
.btn-outline { border: 1px solid var(--color-dark); background: transparent; padding: 8px 16px; border-radius: 20px; font-weight: 600; cursor: pointer; margin-bottom: 10px; transition: all 0.2s; }
.btn-outline:hover { background: rgba(0,0,0,0.05); }
.btn-primary { background: var(--color-dark); color: white; border: none; padding: 8px 20px; border-radius: 20px; font-weight: 600; cursor: pointer; margin-bottom: 10px; transition: all 0.2s; }
.btn-primary:hover { opacity: 0.9; }
.btn-following { background: transparent; border: 1px solid #ef4444; color: #ef4444; }
.btn-following:hover { background: #fee2e2; }
.profile-details { padding: 10px 20px; }
.profile-name { margin: 10px 0 0 0; font-size: 24px; color: var(--color-dark); }
.profile-handle { color: var(--color-text-muted); margin-bottom: 10px; font-weight: 500; }
.profile-dates { font-size: 14px; color: #64748b; margin-bottom: 15px; }
.profile-stats { display: flex; gap: 20px; font-size: 14px; }
.stat-link { color: inherit; text-decoration: none; cursor: pointer; transition: color 0.2s; }
.stat-link:hover { color: var(--color-accent); text-decoration: none; }
.edit-form-card { background: var(--color-white); margin: 20px; padding: 25px; border-radius: 16px; box-shadow: var(--shadow-card); }
.edit-form-card h3 { margin-top: 0; margin-bottom: 20px; border-bottom: 1px solid #eee; padding-bottom: 10px; }
.form-row { display: flex; gap: 15px; }
.form-group { margin-bottom: 15px; flex: 1; display: flex; flex-direction: column; gap: 5px; }
label { font-size: 14px; font-weight: 600; color: var(--color-dark); }
input { padding: 10px 12px; border: 1px solid #cbd5e1; border-radius: 8px; font-size: 15px; background: #f8fafc; }
input:focus { outline: none; border-color: var(--color-accent); background: #fff; }
.posts-section { padding: 0 20px; }
.section-title { border-bottom: 1px solid #ddd; padding-bottom: 10px; color: var(--color-dark); }
.state-msg { padding: 40px; text-align: center; color: #888; }
.error { color: #ef4444; }
.no-posts { color: #888; font-style: italic; margin-top: 20px; }

.success-toast { position: fixed; bottom: 30px; left: 50%; transform: translateX(-50%); background-color: var(--color-dark); color: white; padding: 12px 24px; border-radius: 50px; box-shadow: 0 10px 30px rgba(0,0,0,0.2); display: flex; align-items: center; gap: 12px; font-weight: 600; font-size: 15px; z-index: 2000; }
.check-icon { width: 24px; height: 24px; background-color: #22c55e; border-radius: 50%; display: flex; align-items: center; justify-content: center; }
.check-icon svg { width: 14px; height: 14px; color: white; }
.fade-slide-enter-active, .fade-slide-leave-active { transition: all 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275); }
.fade-slide-enter-from, .fade-slide-leave-to { opacity: 0; transform: translate(-50%, 20px); }
</style>