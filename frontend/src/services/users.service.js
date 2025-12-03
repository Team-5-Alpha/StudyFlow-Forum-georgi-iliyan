import apiClient from './axios-config';

export default {
    search(filterOptions) {
        return apiClient.get('/users', { params: filterOptions });
    },

    getById(id) {
        return apiClient.get(`/users/${id}`);
    },

    getFollowers(id) {
        return apiClient.get(`/users/${id}/followers`);
    },

    getFollowing(id) {
        return apiClient.get(`/users/${id}/following`);
    },

    getPostsByUser(id) {
        return apiClient.get(`/users/${id}/posts`);
    },

    create(userCreateDTO) {
        return apiClient.post('/users', userCreateDTO);
    },

    update(id, userUpdateDTO) {
        return apiClient.put(`/users/${id}`, userUpdateDTO);
    },

    delete(id) {
        return apiClient.delete(`/users/${id}`);
    },

    follow(id) {
        return apiClient.post(`/users/${id}/follow`);
    },

    unfollow(id) {
        return apiClient.delete(`/users/${id}/follow`);
    }
};