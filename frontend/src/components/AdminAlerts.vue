<template>
  <div class="alerts-container">
    <transition-group name="toast" tag="div">
      <div
          v-for="(alert, index) in alerts"
          :key="alert.id"
          :class="['toast', alert.type]"
      >
        {{ alert.message }}
        <button class="close-btn" @click="removeAlert(alert.id)">×</button>
      </div>
    </transition-group>
  </div>
</template>

<script>
import { reactive } from 'vue';

const alerts = reactive([]);

export function addAlert(message, type = 'success', duration = 3000) {
  const id = Date.now() + Math.random();
  alerts.push({ id, message, type });
  setTimeout(() => removeAlert(id), duration);
}

export function removeAlert(id) {
  const index = alerts.findIndex(a => a.id === id);
  if (index !== -1) alerts.splice(index, 1);
}

export default {
  name: 'AdminAlerts',
  setup() {
    return { alerts, removeAlert };
  }
};
</script>

<style scoped>
.alerts-container {
  position: fixed;
  top: 20px;
  right: 20px;
  z-index: 9999;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.toast {
  padding: 12px 18px;
  border-radius: 12px;
  color: white;
  font-weight: 600;
  min-width: 200px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.toast.success { background-color: #16a34a; }
.toast.error { background-color: #ef4444; }
.toast.promote { background-color: #16a34a; }


.close-btn {
  background: transparent;
  border: none;
  color: white;
  font-size: 16px;
  cursor: pointer;
}

.toast-enter-active, .toast-leave-active {
  transition: all 0.3s;
}
.toast-enter-from { opacity: 0; transform: translateX(50px); }
.toast-leave-to { opacity: 0; transform: translateX(50px); }
</style>
