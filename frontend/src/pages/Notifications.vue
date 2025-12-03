<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useNotificationStore } from '../stores/notifications.store';
import ConfirmModal from '../components/ConfirmModal.vue'; // <--- НОВ ИМПОРТ

const notifStore = useNotificationStore();
const router = useRouter();

// State за модала
const isClearModalOpen = ref(false);

onMounted(() => {
  notifStore.fetchNotifications();
});

const formatDate = (dateString) => {
  const date = new Date(dateString);
  const now = new Date();
  if (date.toDateString() === now.toDateString()) {
    return date.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' });
  }
  return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
};

const getNotificationText = (type) => {
  if (!type) return "interacted with you.";
  const t = type.toUpperCase();
  if (t.includes('LIKE')) return "liked your post";
  if (t.includes('COMMENT')) return "commented on your discussion";
  if (t.includes('FOLLOW')) return "started following you";
  if (t.includes('CREATE') || t.includes('NEW_POST')) return "published a new post";
  return "interacted with you";
};

const handleNotificationClick = async (notif) => {
  if (!notif.isRead) await notifStore.markRead(notif.id);
  const type = notif.actionType.toUpperCase();
  if (type.includes('FOLLOW')) {
    router.push(`/profile/${notif.actor.id}`);
  } else {
    router.push({ path: '/', query: { highlight: notif.entityId } });
  }
};

const handleDelete = (id) => {
  notifStore.remove(id);
};

// --- LOGIC ЗА МОДАЛА ---
const openClearModal = () => {
  isClearModalOpen.value = true;
};

const confirmClear = () => {
  notifStore.clearAll();
  isClearModalOpen.value = false;
};
</script>

<template>
  <div class="notif-container">

    <div class="header">
      <h2>Notifications</h2>

      <button
          v-if="notifStore.notifications.length > 0"
          @click="openClearModal"
          class="clear-all-btn"
      >
        <span class="btn-icon">
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.8" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" d="M14.74 9l-.346 9m-4.788 0L9.26 9m9.968-3.21c.342.052.682.107 1.022.166m-1.022-.165L18.16 19.673a2.25 2.25 0 01-2.244 2.077H8.084a2.25 2.25 0 01-2.244-2.077L4.772 5.79m14.456 0a48.108 48.108 0 00-3.478-.397m-12 .562c.34-.059.68-.114 1.022-.165m0 0a48.11 48.11 0 013.478-.397m7.5 0v-.916c0-1.18-.91-2.164-2.09-2.201a51.964 51.964 0 00-3.32 0c-1.18.037-2.09 1.022-2.09 2.201v.916m7.5 0a48.667 48.667 0 00-7.5 0" />
          </svg>
        </span>
        Clear All
      </button>
    </div>

    <div v-if="notifStore.notifications.length === 0" class="empty">
      <div class="empty-icon">
        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" d="M9.143 17.082a24.255 24.255 0 01-5.714 0m5.714 0a3 3 0 11-5.714 0M3.75 21h16.5M4.5 3l15 15" />
        </svg>
      </div>
      <p>No new notifications</p>
    </div>

    <div class="notif-list">
      <div
          v-for="notif in notifStore.notifications"
          :key="notif.id"
          class="notif-item"
          :class="{ 'unread': !notif.isRead }"
          @click="handleNotificationClick(notif)"
      >
        <div class="icon-box" :class="notif.actionType">
          <svg v-if="notif.actionType.includes('FOLLOW')" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor"><path d="M5.25 6.375a4.125 4.125 0 118.25 0 4.125 4.125 0 01-8.25 0zM2.25 19.125a7.125 7.125 0 0114.25 0v.003l-.001.119a.75.75 0 01-.363.63 13.067 13.067 0 01-6.761 1.873c-2.472 0-4.786-.684-6.76-1.873a.75.75 0 01-.364-.63l-.001-.122zM18.75 7.5a.75.75 0 00-1.5 0v2.25H15a.75.75 0 000 1.5h2.25v2.25a.75.75 0 001.5 0v-2.25H21a.75.75 0 000-1.5h-2.25V7.5z" /></svg>
          <svg v-else-if="notif.actionType.includes('LIKE')" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor"><path d="M11.645 20.91l-.007-.003-.022-.012a15.247 15.247 0 01-.383-.218 25.18 25.18 0 01-4.244-3.17C4.688 15.36 2.25 12.174 2.25 8.25 2.25 5.322 4.714 3 7.75 3c1.99 0 3.969 1.356 5.25 3.34C14.281 4.356 16.261 3 18.25 3c3.036 0 5.5 2.322 5.5 5.25 0 3.925-2.438 7.111-4.739 9.256a25.175 25.175 0 01-4.244 3.17 15.247 15.247 0 01-.383.219l-.022.012-.007.004-.003.001a.752.752 0 01-.704 0l-.003-.001z" /></svg>
          <svg v-else-if="notif.actionType.includes('COMMENT')" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor"><path fill-rule="evenodd" d="M4.804 21.644A6.707 6.707 0 006 21.75a6.721 6.721 0 003.583-1.029c.774.182 1.584.279 2.417.279 5.322 0 9.75-3.97 9.75-9 0-5.03-4.428-9-9.75-9s-9.75 3.97-9.75 9c0 2.409 1.025 4.587 2.674 6.192.232.226.277.428.254.543a3.73 3.73 0 01-.814 1.686.75.75 0 00.44 1.223zM8.25 10.875a1.125 1.125 0 100 2.25 1.125 1.125 0 000-2.25zM10.875 12a1.125 1.125 0 112.25 0 1.125 1.125 0 01-2.25 0zm4.875-1.125a1.125 1.125 0 100 2.25 1.125 1.125 0 000-2.25z" clip-rule="evenodd" /></svg>
          <svg v-else xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor"><path fill-rule="evenodd" d="M5.25 9a6.75 6.75 0 0113.5 0v.75c0 2.123.8 4.057 2.118 5.52a.75.75 0 01-.297 1.206c-1.544.57-3.16.99-4.831 1.243a3.75 3.75 0 11-7.48 0 24.585 24.585 0 01-4.831-1.244.75.75 0 01-.298-1.205A8.217 8.217 0 005.25 9.75V9zm4.502 8.9a2.25 2.25 0 104.496 0 25.057 25.057 0 01-4.496 0z" clip-rule="evenodd" /></svg>
        </div>

        <div class="content">
          <p class="text">
            <span class="actor">@{{ notif.actor.username }}</span>
            <span class="action-text">{{ getNotificationText(notif.actionType) }}</span>
          </p>
          <span class="date">{{ formatDate(notif.createdAt) }}</span>
        </div>

        <button @click.stop="handleDelete(notif.id)" class="delete-btn" title="Remove notification">
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.8" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" d="M14.74 9l-.346 9m-4.788 0L9.26 9m9.968-3.21c.342.052.682.107 1.022.166m-1.022-.165L18.16 19.673a2.25 2.25 0 01-2.244 2.077H8.084a2.25 2.25 0 01-2.244-2.077L4.772 5.79m14.456 0a48.108 48.108 0 00-3.478-.397m-12 .562c.34-.059.68-.114 1.022-.165m0 0a48.11 48.11 0 013.478-.397m7.5 0v-.916c0-1.18-.91-2.164-2.09-2.201a51.964 51.964 0 00-3.32 0c-1.18.037-2.09 1.022-2.09 2.201v.916m7.5 0a48.667 48.667 0 00-7.5 0" /></svg>
        </button>
      </div>
    </div>

    <ConfirmModal
        :isOpen="isClearModalOpen"
        title="Clear Notifications?"
        message="This will permanently remove all your notifications. This action cannot be undone."
        confirmText="Clear All"
        @confirm="confirmClear"
        @cancel="isClearModalOpen = false"
    />

  </div>
</template>

<style scoped>
.notif-container { padding: 20px; max-width: 700px; margin: 0 auto; }

.header {
  border-bottom: 1px solid #e2e8f0;
  padding-bottom: 15px;
  margin-bottom: 15px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.header h2 { margin: 0; color: var(--color-dark); font-size: 24px; }

.clear-all-btn {
  background: transparent;
  border: 1px solid #e2e8f0;
  border-radius: 20px;
  color: var(--color-text-muted);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  padding: 6px 14px;
  display: flex; align-items: center; gap: 6px;
  transition: all 0.2s;
}
.clear-all-btn:hover {
  background-color: #fee2e2;
  color: #ef4444;
  border-color: #ef4444;
}
.btn-icon svg { width: 14px; height: 14px; }

.notif-list { display: flex; flex-direction: column; gap: 10px; }

.notif-item {
  display: flex; align-items: center; gap: 15px;
  padding: 15px; border-radius: 12px; background: var(--color-white);
  cursor: pointer; transition: all 0.2s;
  box-shadow: 0 1px 3px rgba(0,0,0,0.05); border: 1px solid transparent;
  position: relative;
}
.notif-item:hover {
  background-color: #f8fafc;
  transform: translateY(-1px);
  box-shadow: 0 4px 6px rgba(0,0,0,0.05);
}

.notif-item.unread {
  background-color: #f0f9ff;
  border-left: 4px solid var(--color-accent);
}

.icon-box {
  width: 44px; height: 44px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
.icon-box svg { width: 22px; height: 22px; }

.icon-box[class*="FOLLOW"] { background-color: #e0f2fe; color: #0284c7; }
.icon-box[class*="LIKE"] { background-color: #fee2e2; color: #ef4444; }
.icon-box[class*="COMMENT"] { background-color: #dcfce7; color: #16a34a; }
.icon-box { background-color: #f1f5f9; color: #64748b; }

.text { margin: 0 0 4px 0; font-size: 15px; color: #334155; line-height: 1.4; }
.actor { font-weight: 700; color: var(--color-dark); margin-right: 4px; }
.action-text { color: #475569; }
.date { font-size: 12px; color: #94a3b8; font-weight: 500; }

.delete-btn {
  background: transparent;
  border: none;
  color: #cbd5e1;
  cursor: pointer;
  padding: 8px;
  border-radius: 50%;
  transition: all 0.2s;
  display: flex; align-items: center; justify-content: center;
  margin-left: auto;
}
.delete-btn svg { width: 18px; height: 18px; }
.notif-item:hover .delete-btn { color: #94a3b8; }
.delete-btn:hover { background-color: #fee2e2; color: #ef4444; }

.empty { text-align: center; color: #94a3b8; padding: 60px; }
.empty-icon { width: 48px; height: 48px; margin: 0 auto 15px auto; color: #cbd5e1; }
.empty-icon svg { width: 100%; height: 100%; }
</style>