<script setup>
defineProps({
  title: {
    type: String,
    required: true
  },
  content: {
    type: String,
    required: true
  }
});
</script>

<template>
  <div class="post-content-wrapper">
    <h3 class="post-title">{{ title }}</h3>
    <p class="post-body">{{ content }}</p>
  </div>
</template>

<style scoped>
.post-content-wrapper {
  margin-bottom: 12px;
}

.post-title {
  margin: 0 0 8px 0;
  font-size: 18px;
  color: var(--color-dark);
  line-height: 1.3;
}

.post-body {
  color: #334155;
  line-height: 1.6;
  margin-bottom: 0;
  white-space: pre-wrap; /* Запазва новите редове */
  font-size: 15px;
}
</style>