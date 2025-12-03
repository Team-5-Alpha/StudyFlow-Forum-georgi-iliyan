import apiClient from './axios-config';

const RESOURCE = '/admin/users';



export default {
    search(filterOptions) {
        return apiClient.get(RESOURCE, { params: filterOptions });
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