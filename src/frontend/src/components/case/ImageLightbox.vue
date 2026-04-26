<template>
  <div
    v-if="visible"
    class="image-lightbox"
    role="dialog"
    aria-modal="true"
    aria-label="影像放大预览"
  >
    <div class="image-lightbox__backdrop" @click="close"></div>

    <div class="image-lightbox__layer">
      <div class="image-lightbox__toolbar">
        <div class="image-lightbox__status">{{ scaleText }}</div>
        <div class="image-lightbox__actions">
          <button type="button" class="image-lightbox__btn" @click="handleZoomOut">-</button>
          <button type="button" class="image-lightbox__btn" @click="handleZoomIn">+</button>
          <button type="button" class="image-lightbox__btn image-lightbox__btn--wide" @click="handleReset">重置</button>
          <button type="button" class="image-lightbox__btn image-lightbox__btn--close" aria-label="关闭" @click="close">×</button>
        </div>
      </div>

      <div
        ref="viewport"
        class="image-lightbox__viewport"
        :class="{
          'image-lightbox__viewport--dragging': dragging,
          'image-lightbox__viewport--dicom': isDicom
        }"
        @wheel.prevent="handleViewportWheel"
        @mousedown="handleViewportMouseDown"
      >
        <img
          v-if="!isDicom && src"
          :src="src"
          :alt="title || '病例影像预览'"
          class="image-lightbox__image"
          :style="imageTransformStyle"
          draggable="false"
        >
        <div v-else-if="isDicom" class="image-lightbox__dicom-shell">
          <DicomViewer
            ref="dicomViewer"
            :case-id="caseId"
            :file-path="dicomFilePath"
            :detections="detections"
            :interactive="true"
            @viewport-change="handleDicomViewportChange"
          />
        </div>
      </div>

      <p class="image-lightbox__hint">
        {{ isDicom ? '滚轮或按钮缩放 DCM，按住鼠标左键拖动查看，Esc 关闭' : '滚轮或按钮缩放，按住鼠标左键拖动查看，Esc 关闭' }}
      </p>
    </div>
  </div>
</template>

<script>
import DicomViewer from '@/components/DicomViewer.vue'

const MIN_SCALE = 0.4
const MAX_SCALE = 6
const STEP = 0.2

export default {
  name: 'ImageLightbox',
  components: { DicomViewer },
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    src: {
      type: String,
      default: ''
    },
    kind: {
      type: String,
      default: 'image'
    },
    caseId: {
      type: Number,
      default: null
    },
    dicomFilePath: {
      type: String,
      default: ''
    },
    detections: {
      type: Array,
      default: () => []
    },
    title: {
      type: String,
      default: ''
    }
  },
  emits: ['update:visible'],
  data() {
    return {
      scale: 1,
      panX: 0,
      panY: 0,
      dragging: false,
      dragState: null,
      onKeydownHandler: null,
      dicomScalePercent: 100
    }
  },
  computed: {
    isDicom() {
      return this.kind === 'dicom'
    },
    imageTransformStyle() {
      return {
        transform: `translate(${this.panX}px, ${this.panY}px) scale(${this.scale})`
      }
    },
    scaleText() {
      if (this.isDicom) {
        return `${Math.round(this.dicomScalePercent || 100)}%`
      }
      return `${Math.round(this.scale * 100)}%`
    }
  },
  watch: {
    visible(value) {
      if (value) {
        this.open()
        return
      }
      this.cleanup()
    },
    kind() {
      if (this.visible) {
        this.handleReset()
      }
    },
    src() {
      if (this.visible) {
        this.resetView()
      }
    },
    dicomFilePath() {
      if (this.visible && this.isDicom) {
        this.handleReset()
      }
    }
  },
  mounted() {
    if (this.visible) {
      this.open()
    }
  },
  beforeUnmount() {
    this.cleanup()
  },
  methods: {
    open() {
      this.handleReset()
      this.bindKeyboard()
      document.body.style.overflow = 'hidden'
    },
    close() {
      this.$emit('update:visible', false)
    },
    cleanup() {
      this.dragging = false
      this.dragState = null
      window.removeEventListener('mousemove', this.onMouseMove)
      window.removeEventListener('mouseup', this.onMouseUp)
      document.removeEventListener('keydown', this.onKeydownHandler)
      this.onKeydownHandler = null
      document.body.style.overflow = ''
      document.body.style.userSelect = ''
      this.dicomScalePercent = 100
    },
    bindKeyboard() {
      if (this.onKeydownHandler) {
        document.removeEventListener('keydown', this.onKeydownHandler)
      }
      this.onKeydownHandler = (event) => {
        if (event.key === 'Escape') this.close()
      }
      document.addEventListener('keydown', this.onKeydownHandler)
    },
    clampScale(next) {
      return Math.min(MAX_SCALE, Math.max(MIN_SCALE, next))
    },
    setScale(next) {
      this.scale = this.clampScale(next)
    },
    zoomIn() {
      this.setScale(this.scale + STEP)
    },
    zoomOut() {
      this.setScale(this.scale - STEP)
    },
    resetView() {
      this.scale = 1
      this.panX = 0
      this.panY = 0
    },
    handleDicomViewportChange(percent) {
      this.dicomScalePercent = percent || 100
    },
    handleZoomIn() {
      if (this.isDicom) {
        this.$refs.dicomViewer?.zoomIn?.()
        return
      }
      this.zoomIn()
    },
    handleZoomOut() {
      if (this.isDicom) {
        this.$refs.dicomViewer?.zoomOut?.()
        return
      }
      this.zoomOut()
    },
    handleReset() {
      if (this.isDicom) {
        this.dicomScalePercent = 100
        this.$nextTick(() => {
          this.$refs.dicomViewer?.resetViewport?.()
        })
        return
      }
      this.resetView()
    },
    handleViewportWheel(event) {
      if (this.isDicom) return
      this.onWheel(event)
    },
    handleViewportMouseDown(event) {
      if (this.isDicom) return
      this.onMouseDown(event)
    },
    onWheel(event) {
      const delta = event.deltaY > 0 ? -STEP : STEP
      this.setScale(this.scale + delta)
    },
    onMouseDown(event) {
      if (event.button !== 0) return
      event.preventDefault()
      this.dragging = true
      this.dragState = {
        startX: event.clientX,
        startY: event.clientY,
        panX: this.panX,
        panY: this.panY
      }
      window.addEventListener('mousemove', this.onMouseMove)
      window.addEventListener('mouseup', this.onMouseUp)
      document.body.style.userSelect = 'none'
    },
    onMouseMove(event) {
      if (!this.dragging || !this.dragState) return
      this.panX = this.dragState.panX + (event.clientX - this.dragState.startX)
      this.panY = this.dragState.panY + (event.clientY - this.dragState.startY)
    },
    onMouseUp() {
      this.dragging = false
      this.dragState = null
      window.removeEventListener('mousemove', this.onMouseMove)
      window.removeEventListener('mouseup', this.onMouseUp)
      document.body.style.userSelect = ''
    }
  }
}
</script>

<style scoped>
.image-lightbox {
  position: fixed;
  inset: 0;
  z-index: 4000;
}

.image-lightbox__backdrop {
  position: absolute;
  inset: 0;
  background: rgba(6, 14, 24, 0.82);
  backdrop-filter: blur(4px);
}

.image-lightbox__layer {
  position: relative;
  z-index: 1;
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: 18px 20px 24px;
}

.image-lightbox__toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 14px;
}

.image-lightbox__status {
  min-width: 72px;
  padding: 8px 14px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.12);
  border: 1px solid rgba(255, 255, 255, 0.18);
  color: #fff;
  font-weight: 700;
  text-align: center;
}

.image-lightbox__actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.image-lightbox__btn {
  min-width: 44px;
  height: 44px;
  border: 1px solid rgba(255, 255, 255, 0.16);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.1);
  color: #fff;
  font-size: 18px;
  cursor: pointer;
}

.image-lightbox__btn--wide {
  min-width: 68px;
  font-size: 14px;
}

.image-lightbox__btn--close {
  font-size: 24px;
  line-height: 1;
}

.image-lightbox__btn:hover {
  background: rgba(255, 255, 255, 0.18);
}

.image-lightbox__viewport {
  flex: 1 1 auto;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border-radius: 22px;
  background: rgba(11, 21, 34, 0.5);
  border: 1px solid rgba(255, 255, 255, 0.08);
  cursor: grab;
}

.image-lightbox__viewport--dicom {
  cursor: default;
}

.image-lightbox__viewport--dragging {
  cursor: grabbing;
}

.image-lightbox__dicom-shell {
  width: 100%;
  height: 100%;
}

.image-lightbox__image {
  max-width: min(88vw, 1400px);
  max-height: calc(100vh - 180px);
  user-select: none;
  -webkit-user-drag: none;
  will-change: transform;
  transform-origin: center center;
}

.image-lightbox__hint {
  margin: 12px 0 0;
  text-align: center;
  color: rgba(255, 255, 255, 0.74);
  font-size: 13px;
}

@media (max-width: 640px) {
  .image-lightbox__toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .image-lightbox__actions {
    justify-content: flex-end;
    flex-wrap: wrap;
  }
}
</style>
