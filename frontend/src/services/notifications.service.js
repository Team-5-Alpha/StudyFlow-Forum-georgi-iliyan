import apiClient from './axios-config';

export default {
    getAll(filterOptions) {
        return apiClient.get('/notifications', { params: filterOptions });
    },
    getUnreadCount() {
        // Тъй като нямаш специален endpoint за count, ще дръпнем последните
        // и ще филтрираме във фронтенда или ще ползваме isRead=false филтър
        return apiClient.get('/notifications', { params: { isRead: false, size: 100 } });
    },
    markAsRead(id) {
        return apiClient.put(`/notifications/${id}/read`);
    },
    markAllAsRead() {
        return apiClient.put('/notifications/read-all');
    },
    delete(id) {
        return apiClient.delete(`/notifications/${id}`);
    }
};