import { defineStore } from 'pinia';
import axios from 'axios';
import { auth } from '../firebaseConfig';
import { createUserWithEmailAndPassword, signInWithEmailAndPassword, signOut } from 'firebase/auth';
import usersService from '../services/users.service'; // Needed for backend registration

// Removed unused API_URL constant to fix warning

export const useAuthStore = defineStore('auth', {
    state: () => ({
        user: JSON.parse(localStorage.getItem('user')) || null,
        firebaseUser: null,
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
            // Simplified logic: check role OR fallback username
            return u?.role === 'ADMIN' || u?.username === 'admin';
        }
    },
    actions: {
        async login(email, password) {
            try {
                // 1. Firebase Login
                const userCredential = await signInWithEmailAndPassword(auth, email, password);
                this.firebaseUser = userCredential.user;

                // 2. Backend Login (Fetch User Details)
                // Using the general user search or get endpoint
                const response = await axios.get(`http://localhost:8080/api/users`, { 
                    params: { email: email } 
                });
                
                const userData = response.data[0]; // Assuming search returns a list

                if (!userData) {
                    throw new Error("User not found in backend");
                }

                // Manual Role Patch if missing
                if (userData.username === 'admin' || userData.role === 'ADMIN') {
                    userData.role = 'ADMIN';
                } else {
                    userData.role = 'USER';
                }

                this.user = userData;
                localStorage.setItem('user', JSON.stringify(userData));

            } catch (error) {
                console.error("Login failed:", error);
                throw error;
            }
        },

        async register(userData) {
            try {
                // 1. Create user in Firebase
                const userCredential = await createUserWithEmailAndPassword(auth, userData.email, userData.password);
                this.firebaseUser = userCredential.user;

                // 2. Create user in Backend
                const response = await usersService.create(userData);
                
                const newUser = response.data;
                // Ensure role is set for the new user (default is USER)
                newUser.role = 'USER';

                this.user = newUser;
                localStorage.setItem('user', JSON.stringify(newUser));

            } catch (error) {
                console.error("Registration failed:", error);
                // If backend creation fails, we might want to cleanup firebase user, 
                // but for now let's just throw the error.
                throw error; 
            }
        },

        async logout() {
            try {
                await signOut(auth);
            } catch (err) {
                console.error("Logout error", err);
            } finally {
                this.user = null;
                this.firebaseUser = null;
                localStorage.removeItem('user');
            }
        }
    }
});