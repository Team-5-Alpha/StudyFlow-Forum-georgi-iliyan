import { defineStore } from 'pinia';
import { auth } from '../firebaseConfig';
import {
    createUserWithEmailAndPassword,
    signInWithEmailAndPassword,
    signOut,
    onAuthStateChanged
} from 'firebase/auth';
import usersService from '../services/users.service';

export const useAuthStore = defineStore('auth', {
    state: () => ({
        user: JSON.parse(localStorage.getItem('user')) || null,
        firebaseUser: null,
        loading: false,
        error: null
    }),

    getters: {
        isAuthenticated: (state) => !!state.user,
        currentUserId: (state) => state.user?.id,
        isAdmin: (state) => state.user?.role === 'admin',
    },

    actions: {
        initAuth() {
            return new Promise((resolve) => {
                onAuthStateChanged(auth, (currentUser) => {
                    this.firebaseUser = currentUser;
                    if (!currentUser) {
                        this.user = null;
                        localStorage.removeItem('user');
                    }
                    resolve(currentUser);
                });
            });
        },

        async register(registerData) {
            this.loading = true;
            this.error = null;
            try {
                const userCredential = await createUserWithEmailAndPassword(
                    auth,
                    registerData.email,
                    registerData.password
                );
                this.firebaseUser = userCredential.user;

                const backendResponse = await usersService.create({
                    username: registerData.username,
                    firstName: registerData.firstName,
                    lastName: registerData.lastName,
                    email: registerData.email,
                    password: registerData.password
                });

                this.user = backendResponse.data;
                localStorage.setItem('user', JSON.stringify(this.user));

                return true;
            } catch (err) {
                console.error("Registration error:", err);
                this.error = err.response?.data?.message || err.message;
                throw err;
            } finally {
                this.loading = false;
            }
        },


        async login(email, password) {
            this.loading = true;
            this.error = null;
            try {
                const userCredential = await signInWithEmailAndPassword(auth, email, password);
                this.firebaseUser = userCredential.user;


                const response = await usersService.search({ email: email });

                if (response.data && response.data.length > 0) {
                    this.user = response.data[0]; // Взимаме първия намерен
                    localStorage.setItem('user', JSON.stringify(this.user));
                } else {
                    throw new Error("Потребителят е аутентикиран във Firebase, но не е намерен в базата данни на StudyFlow.");
                }

                return true;
            } catch (err) {
                console.error("Login error:", err);
                this.error = err.response?.data?.message || err.message;
                throw err;
            } finally {
                this.loading = false;
            }
        },

        async logout() {
            try {
                await signOut(auth);
                this.user = null;
                this.firebaseUser = null;
                localStorage.removeItem('user');
            } catch (err) {
                console.error("Logout error", err);
            }
        }
    }
});