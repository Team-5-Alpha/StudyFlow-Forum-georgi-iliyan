<script setup>
import {onMounted} from 'vue';
import {useRouter} from 'vue-router';
import {useNotificationStore} from '../stores/notifications.store';

const notifStore = useNotificationStore();
const router = useRouter();

onMounted(() => {
  notifStore.fetchNotifications();
});

const formatDate = (dateString) => {
  const date = new Date(dateString);
  const now = new Date();
  if (date.toDateString() === now.toDateString()) {
    return date.toLocaleTimeString('en-US', {hour: '2-digit', minute: '2-digit'});
  }
  return date.toLocaleDateString('en-US', {month: 'short', day: 'numeric'});
};

const getNotificationText = (type) => {
  if (!type) return "interacted with you.";
  const t = type.toUpperCase();
  if (t.includes('LIKE')) return "liked your post.";
  if (t.includes('COMMENT')) return "commented on your post.";
  if (t.includes('FOLLOW')) return "started following you.";
  if (t.includes('CREATE') || t.includes('NEW_POST')) return "posted a new discussion.";
  return "interacted with you.";
};


const handleNotificationClick = async (notif) => {
  if (!notif.isRead) await notifStore.markRead(notif.id);

  const type = notif.actionType.toUpperCase();
  if (type.includes('FOLLOW')) {
    router.push(`/profile/${notif.actor.id}`);
  } else {
    router.push({path: '/', query: {highlight: notif.entityId}});
  }
};


const handleDelete = (id) => {
  if (confirm("Remove this notification?")) {
    notifStore.remove(id);
  }
};

const handleClearAll = () => {
  if (confirm("Are you sure you want to delete ALL notifications?")) {
    notifStore.clearAll();
  }
};
</script>

<template>
  <div class="notif-container">
    <div class="header">
      <h2>Notifications</h2>
      <button
          v-if="notifStore.notifications.length > 0"
          @click="handleClearAll"
          class="clear-all-btn"
      >
        Clear All
      </button>
    </div>

    <div v-if="notifStore.notifications.length === 0" class="empty">
      <div class="empty-icon">🔕</div>
      No notifications yet.
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
          <svg v-if="notif.actionType.includes('FOLLOW')" xmlns="http://www.w3.org/2000/svg" fill="none"
               viewBox="0 0 24 24" stroke-width="2" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round"
                  d="M19 7.5v3m0 0v3m0-3h3m-3 0h-3m-2.25-4.125a3.375 3.375 0 11-6.75 0 3.375 3.375 0 016.75 0zM4 19.235v-.11a6.375 6.375 0 0112.75 0v.109A12.318 12.318 0 0110.374 21c-2.331 0-4.512-.645-6.374-1.766z"/>
          </svg>
          <svg v-else-if="notif.actionType.includes('LIKE')" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"
               fill="currentColor">
            <path
                d="M11.645 20.91l-.007-.003-.022-.012a15.247 15.247 0 01-.383-.218 25.18 25.18 0 01-4.244-3.17C4.688 15.36 2.25 12.174 2.25 8.25 2.25 5.322 4.714 3 7.75 3c1.99 0 3.969 1.356 5.25 3.34C14.281 4.356 16.261 3 18.25 3c3.036 0 5.5 2.322 5.5 5.25 0 3.925-2.438 7.111-4.739 9.256a25.175 25.175 0 01-4.244 3.17 15.247 15.247 0 01-.383.219l-.022.012-.007.004-.003.001a.752.752 0 01-.704 0l-.003-.001z"/>
          </svg>
          <svg v-else-if="notif.actionType.includes('COMMENT')" xmlns="http://www.w3.org/2000/svg" fill="none"
               viewBox="0 0 24 24" stroke-width="2" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round"
                  d="M7.5 8.25h9m-9 3H12m-9.75 1.51c0 1.6 1.123 2.994 2.707 3.227 1.129.166 2.27.293 3.423.379.35.026.67.21.865.501L12 21l2.755-4.133a1.14 1.14 0 01.865-.501 48.172 48.172 0 003.423-.379c1.584-.233 2.707-1.626 2.707-3.228V6.741c0-1.602-1.123-2.995-2.707-3.228A48.394 48.394 0 0012 3c-2.392 0-4.744.175-7.043.513C3.373 3.746 2.25 5.14 2.25 6.741v6.018z"/>
          </svg>
          <svg v-else xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2"
               stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round"
                  d="M14.857 17.082a23.848 23.848 0 005.454-1.31A8.967 8.967 0 0118 9.75v-.7V9A6 6 0 006 9v.75a8.967 8.967 0 01-2.312 6.022c1.733.64 3.56 1.085 5.455 1.31m5.714 0a24.255 24.255 0 01-5.714 0m5.714 0a3 3 0 11-5.714 0"/>
          </svg>
        </div>

        <div class="content">
          <p class="text">
            <span class="actor">@{{ notif.actor.username }}</span>
            {{ getNotificationText(notif.actionType) }}
          </p>
          <span class="date">{{ formatDate(notif.createdAt) }}</span>
        </div>

        <button @click.stop="handleDelete(notif.id)" class="delete-btn">
          ✕
        </button>

      </div>
    </div>
  </div>
</template>

<style scoped>
.notif-container {
  padding: 20px;
  max-width: 700px;
  margin: 0 auto;
}

.header {
  border-bottom: 1px solid #e2e8f0;
  padding-bottom: 15px;
  margin-bottom: 15px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header h2 {
  margin: 0;
  color: var(--color-dark);
  font-size: 24px;
}

.clear-all-btn {
  background: none;
  border: none;
  color: var(--color-accent);
  font-weight: 700;
  cursor: pointer;
  font-size: 14px;
}

.clear-all-btn:hover {
  text-decoration: underline;
  color: #ef4444;
}

.notif-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.notif-item {
  display: flex;
  align-items: center;
  gap: 15px;
  padding: 15px;
  border-radius: 12px;
  background: var(--color-white);
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  border: 1px solid transparent;
  position: relative;
}

.notif-item:hover {
  background-color: #f8fafc;
  border-color: #e2e8f0;
}

.notif-item.unread {
  background-color: #f0f9ff;
  border-left: 4px solid var(--color-accent);
}

.icon-box {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.icon-box svg {
  width: 22px;
  height: 22px;
}

.icon-box[class*="FOLLOW"] {
  background-color: #e0f2fe;
  color: #0284c7;
}

.icon-box[class*="LIKE"] {
  background-color: #fee2e2;
  color: #ef4444;
}

.icon-box[class*="COMMENT"] {
  background-color: #dcfce7;
  color: #16a34a;
}

.icon-box {
  background-color: #f1f5f9;
  color: #64748b;
}

.text {
  margin: 0 0 4px 0;
  font-size: 15px;
  color: #334155;
  line-height: 1.4;
}

.actor {
  font-weight: 700;
  color: var(--color-dark);
}

.date {
  font-size: 12px;
  color: #94a3b8;
}

.delete-btn {
  background: transparent;
  border: none;
  color: #cbd5e1;
  font-size: 16px;
  font-weight: 700;
  cursor: pointer;
  margin-left: auto;
  padding: 5px 10px;
  border-radius: 50%;
  transition: all 0.2s;
}

.delete-btn:hover {
  background-color: #fee2e2;
  color: #ef4444;
}

.empty {
  text-align: center;
  color: #94a3b8;
  padding: 60px;
}

.empty-icon {
  font-size: 40px;
  margin-bottom: 10px;
  filter: grayscale(1);
  opacity: 0.5;
}
</style>