import { createRouter, createWebHistory } from 'vue-router';
import Home from './pages/Home.vue';
import Login from './pages/Login.vue';
import Register from './pages/Register.vue';
import Profile from './pages/Profile.vue';
import FollowList from './pages/FollowList.vue';
import { useAuthStore } from './stores/auth.store';
import Notifications from './pages/Notifications.vue';

const routes = [
    { path: '/', name: 'Home', component: Home },
    { path: '/login', name: 'Login', component: Login, meta: { guestOnly: true } },
    { path: '/register', name: 'Register', component: Register, meta: { guestOnly: true } },
    { path: '/notifications', name: 'Notifications', component: Notifications, meta: { requiresAuth: true } },

    {
        path: '/profile',
        name: 'MyProfile',
        component: Profile,
        meta: { requiresAuth: true }
    },


    {
        path: '/profile/:id',
        name: 'UserProfile',
        component: Profile
    },


    {
        path: '/profile/:id/followers',
        name: 'Followers',
        component: FollowList
    },
    {
        path: '/profile/:id/following',
        name: 'Following',
        component: FollowList
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

    if (to.meta.guestOnly && isAuthenticated) {
        next('/');
    } else if (to.meta.requiresAuth && !isAuthenticated) {
        next('/login');
    } else {
        next();
    }
});

export default router;