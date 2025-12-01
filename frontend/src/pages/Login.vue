<script setup>
import { ref } from 'vue';
import { useAuthStore } from '../stores/auth.store';
import { useRouter } from 'vue-router';

const authStore = useAuthStore();
const router = useRouter();

const email = ref('');
const password = ref('');
const loading = ref(false);
const error = ref(null);

const handleLogin = async () => {
  loading.value = true;
  error.value = null;

  try {
    await authStore.login(email.value, password.value);
    router.push('/');
  } catch (err) {
    error.value = "Invalid email or password.";
    console.error(err);
  } finally {
    loading.value = false;
  }
};
</script>

<template>
  <div class="auth-container">
    <div class="auth-card">
      <div class="auth-header">
        <h2>Welcome Back</h2>
        <p>Login to continue to StudyFlow.</p>
      </div>

      <form @submit.prevent="handleLogin" class="auth-form">
        <div v-if="error" class="error-alert">
          {{ error }}
        </div>

        <div class="form-group">
          <label>Email</label>
          <input v-model="email" type="email" required placeholder="you@example.com" />
        </div>

        <div class="form-group">
          <label>Password</label>
          <input v-model="password" type="password" required placeholder="••••••••" />
        </div>

        <button type="submit" class="btn-submit" :disabled="loading">
          {{ loading ? 'Logging in...' : 'Log In' }}
        </button>

        <p class="auth-footer">
          New to StudyFlow?
          <router-link to="/register" class="link">Create an account</router-link>
        </p>
      </form>
    </div>
  </div>
</template>

<style scoped>

.auth-container {
  display: flex;
  justify-content: center;
  padding-top: 60px;
}

.auth-card {
  background: var(--color-white);
  width: 100%;
  max-width: 400px;
  padding: 40px;
  border-radius: 16px;
  box-shadow: var(--shadow-card);
}

.auth-header h2 {
  margin: 0;
  font-size: 26px;
  color: var(--color-dark);
}

.auth-header p {
  color: var(--color-text-muted);
  margin-top: 5px;
}

.auth-form {
  margin-top: 30px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

label {
  font-weight: 600;
  font-size: 14px;
  color: var(--color-dark);
}

input {
  padding: 12px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  font-size: 16px;
  transition: border-color 0.2s;
  background-color: #f8fafc;
}

input:focus {
  outline: none;
  border-color: var(--color-accent);
  background-color: #fff;
}

.btn-submit {
  background-color: var(--color-dark);
  color: var(--color-white);
  padding: 14px;
  border: none;
  border-radius: 30px;
  font-size: 16px;
  font-weight: 700;
  margin-top: 10px;
  transition: all 0.2s;
}

.btn-submit:hover:not(:disabled) {
  background-color: var(--color-accent);
  transform: translateY(-1px);
}

.btn-submit:disabled {
  opacity: 0.7;
}

.error-alert {
  background-color: #fee2e2;
  color: #ef4444;
  padding: 12px;
  border-radius: 8px;
  font-size: 14px;
  text-align: center;
}

.auth-footer {
  text-align: center;
  font-size: 14px;
  color: var(--color-text-muted);
  margin-top: 10px;
}

.link {
  color: var(--color-accent);
  font-weight: 700;
  text-decoration: none;
}
</style>