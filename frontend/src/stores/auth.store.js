import { defineStore } from 'pinia';
import apiClient from '../services/axios-config'; // shared axios instance with interceptors
import { auth } from '../firebaseConfig';
import {
    createUserWithEmailAndPassword,
    signInWithEmailAndPassword,
    signOut
} from 'firebase/auth';
import usersService from '../services/users.service'; // Backend user API

// Removed unused API_URL constant to fix warning

export const useAuthStore = defineStore('auth', {
    state: () => ({
        user: JSON.parse(localStorage.getItem('user')) || null,
        loading: false,
    }),
    getters: {
        isAuthenticated: (state) => !!state.user,
        /**
         * Checks if the current user is an admin.
         * @param {object} state
         * @returns {boolean}
         */
        isAdmin: (state) => {
            const u = state.user;
            // Check role OR fallback username
            return u?.role === 'ADMIN' || u?.username === 'admin';
        }
    },
    actions: {
        /**
         * Called from router.beforeEach to ensure auth store is in sync
         * with localStorage and Firebase currentUser.
         */
        async initAuth() {
            if (this.loading) {
                return;
            }
            this.loading = true;
            try {
                const userStr = localStorage.getItem('user');
                if (userStr) {
                    try {
                        this.user = JSON.parse(userStr);
                    } catch (e) {
                        console.error('Error parsing user from localStorage', e);
                        this.user = null;
                        localStorage.removeItem('user');
                    }
                } else {
                    this.user = null;
                }

                // Optional: you could check auth.currentUser here if needed.
                // For now we rely on firebase/auth + interceptors in axios-config.
            } finally {
                this.loading = false;
            }
        },

        /**
         * Login flow:
         * 1) Firebase email/password auth
         * 2) Backend user lookup by email
         * 3) Normalize role and store user in state + localStorage
         */
        async login(email, password) {
            try {
                // 1. Firebase Login
                const userCredential = await signInWithEmailAndPassword(auth, email, password);
                const firebaseUser = userCredential.user;

                // 2. Backend Login (Fetch User Details)
                const response = await apiClient.get('/users', {
                    params: { email: email }
                });

                const userData = response.data[0]; // assuming list

                if (!userData) {
                    // noinspection ExceptionCaughtLocallyJS
                    throw new Error('User not found in backend');
                }

                // Normalize role
                if (userData.username === 'admin' || userData.role === 'ADMIN') {
                    userData.role = 'ADMIN';
                } else {
                    userData.role = 'USER';
                }

                this.user = userData;
                localStorage.setItem('user', JSON.stringify(userData));

                // Force token to be fresh (optional but useful)
                await firebaseUser.getIdToken(true);
            } catch (error) {
                console.error('Login failed:', error);
                throw error;
            }
        },

        /**
         * Registration:
         * 1) Create user in Firebase
         * 2) Create user in backend
         * 3) Store backend user in state + localStorage
         */
        async register(userData) {
            try {
                // 1. Create user in Firebase
                const userCredential = await createUserWithEmailAndPassword(
                    auth,
                    userData.email,
                    userData.password
                );
                const firebaseUser = userCredential.user;

                // 2. Create user in Backend
                const response = await usersService.create(userData);
                const newUser = response.data;

                // Default role
                newUser.role = 'USER';

                this.user = newUser;
                localStorage.setItem('user', JSON.stringify(newUser));

                // Ensure token is ready (optional)
                await firebaseUser.getIdToken(true);
            } catch (error) {
                console.error('Registration failed:', error);
                throw error;
            }
        },

        /**
         * Logout from Firebase and clear local state.
         */
        async logout() {
            try {
                await signOut(auth);
            } catch (err) {
                console.error('Logout error', err);
            } finally {
                this.user = null;
                localStorage.removeItem('user');
            }
        }
    }
});