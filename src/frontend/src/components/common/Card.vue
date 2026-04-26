<template>
  <div class="card">
    <div class="card-header" v-if="title || $slots['header-actions']">
      <h2 v-if="title">{{ title }}</h2>
      <slot name="header-actions"></slot>
    </div>
    <slot></slot>
  </div>
</template>

<script>
export default {
  name: 'Card',
  props: {
    title: { type: String, default: '' }
  }
}
</script>
