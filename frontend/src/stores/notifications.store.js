import { defineStore } from 'pinia';
import notificationsService from '../services/notifications.service';
import { useAuthStore } from './auth.store';

export const useNotificationStore = defineStore('notifications', {
    state: () => ({
        notifications: [],
        unreadCount: 0,
        popupVisible: false,
        popupData: { message: '', type: '' },
        pollingInterval: null
    }),

    actions: {
        async fetchNotifications() {
            const authStore = useAuthStore();
            const myId = authStore.user?.id;
            if (!myId) return;

            try {
                const response = await notificationsService.getAll({
                    page: 0, size: 20, sortBy: 'createdAt', sortOrder: 'desc'
                });

                const allNotifs = response.data;
                const filteredNotifs = allNotifs.filter(n => n.actor.id !== myId);


                if (this.notifications.length > 0 && filteredNotifs.length > 0) {
                    const latestOld = this.notifications[0];
                    const latestNew = filteredNotifs[0];
                    if (latestNew.id > latestOld.id) {
                        this.triggerPopup(latestNew);
                    }
                }

                this.notifications = filteredNotifs;
                this.unreadCount = this.notifications.filter(n => !n.isRead).length;

            } catch (err) {
                console.error("Failed to fetch notifications", err);
            }
        },

        async remove(id) {
            try {
                await notificationsService.delete(id);

                const notif = this.notifications.find(n => n.id === id);
                if (notif && !notif.isRead) {
                    this.unreadCount = Math.max(0, this.unreadCount - 1);
                }
                this.notifications = this.notifications.filter(n => n.id !== id);

            } catch (err) {
                console.error("Failed to delete notification", err);
            }
        },


        async clearAll() {
            try {
                const deletePromises = this.notifications.map(n => notificationsService.delete(n.id));
                await Promise.all(deletePromises);

                this.notifications = [];
                this.unreadCount = 0;
            } catch (err) {
                console.error("Failed to clear all", err);
            }
        },

        async markRead(id) {
            await notificationsService.markAsRead(id);
            const notif = this.notifications.find(n => n.id === id);
            if (notif && !notif.isRead) {
                notif.isRead = true;
                this.unreadCount = Math.max(0, this.unreadCount - 1);
            }
        },

        startPolling() {
            if (this.pollingInterval) return;
            this.fetchNotifications();
            this.pollingInterval = setInterval(() => { this.fetchNotifications(); }, 10000);
        },

        stopPolling() {
            if (this.pollingInterval) {
                clearInterval(this.pollingInterval);
                this.pollingInterval = null;
            }
            this.notifications = [];
            this.unreadCount = 0;
        },

        triggerPopup(notif) {
            let msg = '';
            const actor = notif.actor.username;
            if (notif.actionType.includes('LIKE')) msg = `@${actor} liked your post.`;
            else if (notif.actionType.includes('COMMENT')) msg = `@${actor} commented on your post.`;
            else if (notif.actionType.includes('FOLLOW')) msg = `@${actor} followed you.`;
            else msg = `@${actor} interacted with you.`;

            this.popupData = { message: msg, type: notif.actionType };
            this.popupVisible = true;
            setTimeout(() => { this.popupVisible = false; }, 4000);
        }
    }
});