import { createRouter, createWebHistory } from 'vue-router';
import { useAuthStore } from './stores/auth.store';

// Pages
import Home from './pages/Home.vue';
import Login from './pages/Login.vue';
import Register from './pages/Register.vue';
import Profile from './pages/Profile.vue';
import FollowList from './pages/FollowList.vue';
import Notifications from './pages/Notifications.vue';
import AdminUsers from './pages/AdminUsers.vue'; // Admin Page

const routes = [
    {
        path: '/',
        name: 'Home',
        component: Home
    },
    {
        path: '/login',
        name: 'Login',
        component: Login,
        meta: { guestOnly: true }
    },
    {
        path: '/register',
        name: 'Register',
        component: Register,
        meta: { guestOnly: true }
    },
    {
        path: '/notifications',
        name: 'Notifications',
        component: Notifications,
        meta: { requiresAuth: true }
    },
    {
        path: '/profile',
        name: 'MyProfile',
        component: Profile,
        meta: { requiresAuth: true, transition: 'fade-slide' }
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
    {
        path: '/admin/users',
        name: 'AdminUsers',
        component: AdminUsers,
        meta: { requiresAuth: true, adminOnly: true } // само за админ
    }
];

const router = createRouter({
    history: createWebHistory(),
    routes
});

// Global Navigation Guard
/**
 * @param {import('vue-router').RouteLocationNormalized} to
 * @param {import('vue-router').RouteLocationNormalized} from
 * @param {import('vue-router').NavigationGuardNext} next
 */
router.beforeEach(async (to, from, next) => {
    const authStore = useAuthStore();

    // Уверяваме се, че auth е инициализиран преди да проверяваме права
    if (authStore.loading) {
        await authStore.initAuth();
    }

    const isAuthenticated = authStore.isAuthenticated;
    const isAdmin = authStore.isAdmin;

    const isGuestOnly = to.meta['guestOnly'];
    const isRequiresAuth = to.meta['requiresAuth'];
    const isAdminOnly = to.meta['adminOnly'];

    // 1) Гост-only (login/register) – ако сме логнати, към Home
    if (isGuestOnly && isAuthenticated) {
        return next({ name: 'Home' });
    }

    // 2) Requires auth – ако не сме логнати, към Login
    if (isRequiresAuth && !isAuthenticated) {
        return next({ name: 'Login' });
    }

    // 3) Admin only – ако не сме admin, към Home
    if (isAdminOnly && !isAdmin) {
        return next({ name: 'Home' });
    }

    // 4) Всичко е ок
    next();
});

export default router;