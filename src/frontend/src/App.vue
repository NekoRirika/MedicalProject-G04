<template>
  <div id="app">
    <router-view />
    <ToastHost />
  </div>
</template>

<script>
import ToastHost from '@/components/common/ToastHost.vue'

export default {
  name: 'App',
  components: { ToastHost }
}
</script>
