import { createRouter, createWebHistory } from 'vue-router';
import { useAuthStore } from './stores/auth.store';

// Pages
import Home from './pages/Home.vue';
import Login from './pages/Login.vue';
import Register from './pages/Register.vue';
import Profile from './pages/Profile.vue';
import FollowList from './pages/FollowList.vue';
import Notifications from './pages/Notifications.vue';
import AdminUsers from './pages/AdminUsers.vue'; // Import Admin Page

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
    {
        path: '/notifications',
        name: 'Notifications',
        component: Notifications,
        meta: { requiresAuth: true }
    },
    {
        path: '/admin/users',
        name: 'AdminUsers',
        component: AdminUsers,
        meta: { requiresAuth: true, adminOnly: true } // Added adminOnly meta
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
    
    const isAuthenticated = authStore.isAuthenticated;
    const isAdmin = authStore.isAdmin;
    
    // Use bracket notation to avoid "Unresolved variable" warnings
    const isGuestOnly = to.meta['guestOnly'];
    const isRequiresAuth = to.meta['requiresAuth'];
    const isAdminOnly = to.meta['adminOnly'];

    // 1. Check for guest-only routes (Redirect logged-in users to Home)
    if (isGuestOnly && isAuthenticated) {
        return next({ name: 'Home' });
    }

    // 2. Check for protected routes (Redirect guests to Login)
    if (isRequiresAuth && !isAuthenticated) {
        return next({ name: 'Login' });
    }

    // 3. Check for admin-only routes (Redirect non-admins to Home)
    if (isAdminOnly && !isAdmin) {
        return next({ name: 'Home' });
    }

    next(); // Allow navigation
});

export default router;