import apiClient from './axios-config';
import { useAuthStore } from '../stores/auth.store';

const RESOURCE = '/admin/users';

export default {
    search(filterOptions) {
        return apiClient.get(RESOURCE, { params: filterOptions })
            .catch(error => {
                if (error.response?.status === 403) {
                    const authStore = useAuthStore();
                    authStore.logout();
                    window.location.href = '/login';
                }
                throw error;
            });
    },

    update(id, adminUpdateDTO) {
        return apiClient.put(`${RESOURCE}/${id}`, adminUpdateDTO);
    },

    blockUser(id) {
        return apiClient.put(`${RESOURCE}/${id}/block`);
    },

    unblockUser(id) {
        return apiClient.put(`${RESOURCE}/${id}/unblock`);
    },

    promoteUser(id) {
        return apiClient.put(`${RESOURCE}/${id}/promote`);
    }
};