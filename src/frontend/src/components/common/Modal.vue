<template>
  <div v-if="visible" class="modal-overlay" @click.self="close">
    <div class="modal" :class="modalWidthClass">
      <div class="modal-header">
        <h3>{{ title }}</h3>
        <button class="close-btn" @click="close">×</button>
      </div>
      <div class="modal-body" :class="{ 'modal-body--compact': compactBody }">
        <slot></slot>
      </div>
      <div class="modal-footer">
        <slot name="footer"></slot>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'Modal',
  props: {
    visible: Boolean,
    title: String,
    /**
     * narrow：窄表单；medium：常规；wide：宽表单；
     * xlarge：病例编辑等大段内容（PC 端偏横向比例）
     */
    widthPreset: {
      type: String,
      default: 'medium',
      validator: (v) => ['narrow', 'medium', 'wide', 'xlarge'].includes(v)
    },
    /** 收窄正文留白（如短确认框） */
    compactBody: {
      type: Boolean,
      default: false
    }
  },
  computed: {
    modalWidthClass() {
      return `modal--${this.widthPreset}`
    }
  },
  methods: {
    close() {
      this.$emit('update:visible', false)
    }
  }
}
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: flex-start;
  padding: 24px 16px;
  box-sizing: border-box;
  overflow-y: auto;
  z-index: 1000;
}

.modal {
  /* 修复：全局样式可能把 `.modal` 设为 display:none */
  background: white;
  border-radius: 8px;
  position: relative; /* 避免全局 `.modal` 的 fixed 布局干扰 */
  margin: 0;
  width: min(94vw, 100%);
  max-height: min(88vh, 920px);
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  display: flex;
  flex-direction: column;
  align-self: center;
}

.modal--narrow {
  max-width: min(520px, 94vw);
}

.modal--medium {
  max-width: min(720px, 94vw);
}

.modal--wide {
  max-width: min(960px, 94vw);
}

.modal--xlarge {
  max-width: min(1100px, 96vw);
}

.modal-header {
  padding: 14px 18px;
  border-bottom: 1px solid #eee;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-shrink: 0;
}

.modal-header h3 {
  margin: 0;
}

.close-btn {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: #666;
}

.modal-body {
  padding: 16px 18px;
  overflow-y: auto;
  flex: 0 1 auto;
  min-height: 0;
}

.modal-body--compact {
  padding: 10px 16px;
}

.modal-footer {
  padding: 12px 18px;
  border-top: 1px solid #eee;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  flex-shrink: 0;
}
</style>