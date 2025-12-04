import { createRouter, createWebHistory } from 'vue-router';
import Home from './pages/Home.vue';
import Login from './pages/Login.vue';
import Register from './pages/Register.vue';
import Profile from './pages/Profile.vue';
import FollowList from './pages/FollowList.vue';
import AdminPanel from './pages/AdminPanel.vue';
import { useAuthStore } from './stores/auth.store';
import Notifications from './pages/Notifications.vue';

const routes = [
    { path: '/', name: 'Home', component: Home },
    { path: '/login', name: 'Login', component: Login, meta: { guestOnly: true } },
    { path: '/register', name: 'Register', component: Register, meta: { guestOnly: true } },
    { path: '/notifications', name: 'Notifications', component: Notifications, meta: { requiresAuth: true } },
    { path: '/admin', name: 'AdminPanel', component: AdminPanel, meta: { requiresAuth: true, adminOnly: true } },

    {
        path: '/profile',
        name: 'MyProfile',
        component: Profile,
        meta: { requiresAuth: true, transition: 'fade-slide'}
    },


    {
        path: '/profile/:id',
        name: 'UserProfile',
        component: Profile,
        meta: { transition: 'fade-slide' }
    },


    {
        path: '/profile/:id/followers',
        name: 'Followers',
        component: FollowList,
        meta: { transition: 'fade-slide' }
    },
    {
        path: '/profile/:id/following',
        name: 'Following',
        component: FollowList,
        meta: { transition: 'fade-slide' }
    },
];

const router = createRouter({
    history: createWebHistory(),
    routes,
});

router.beforeEach(async (to, from, next) => {
    const authStore = useAuthStore();

    if (authStore.loading) await authStore.initAuth();

    const isAuthenticated = authStore.isAuthenticated;
    const isAdmin = authStore.user?.role === 'ADMIN';

    if (to.meta.guestOnly && isAuthenticated) {
        next('/');
    } else if (to.meta.requiresAuth && !isAuthenticated) {
        next('/login');
    } else if (to.meta.adminOnly && !isAdmin) {
        next('/');
    } else {
        next();
    }
});

export default router;