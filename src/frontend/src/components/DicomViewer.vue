<template>
  <div
    class="dicom-viewer"
    :class="{ 'dicom-viewer--interactive': interactive, 'dicom-viewer--dragging': dragging }"
    @wheel="handleWheel"
    @mousedown="handleMouseDown"
  >
    <div ref="dicomElement" class="dicom-canvas"></div>
    <div
      v-for="(det, idx) in detections"
      :key="idx"
      class="bbox"
      :style="bboxStyle(det)"
    >
      <div class="bbox-label">
        {{ det.label }} ({{ toPercent(det.confidence) }}%)
      </div>
    </div>
    <div v-if="loading" class="loading-overlay">
      <span>加载中...</span>
    </div>
    <div v-if="error" class="error-overlay">
      <span>{{ error }}</span>
    </div>
  </div>
</template>

<script>
import cornerstone from 'cornerstone-core'
import cornerstoneWADOImageLoader from 'cornerstone-wado-image-loader'
import dicomParser from 'dicom-parser'
import { API_BASE_URL } from '@/api/config'

export default {
  name: 'DicomViewer',
  emits: ['viewport-change'],
  props: {
    caseId: {
      type: Number,
      required: true
    },
    filePath: {
      type: String,
      required: true
    },
    width: {
      type: Number,
      default: 200
    },
    height: {
      type: Number,
      default: 200
    },
    detections: {
      type: Array,
      default: () => []
    },
    interactive: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      loading: true,
      error: null,
      initialized: false,
      imageWidth: 0,
      imageHeight: 0,
      scaleX: 1,
      scaleY: 1,
      offsetX: 0,
      offsetY: 0,
      baseScale: 1,
      currentScale: 1,
      dragging: false,
      dragState: null
    }
  },
  computed: {
    dicomUrl() {
      return `${API_BASE_URL}/cases/${this.caseId}/images/${this.filePath}`
    }
  },
  mounted() {
    this.initCornerstone()
    window.addEventListener('resize', this.handleResize)
  },
  beforeUnmount() {
    this.stopDragging()
    if (this.$refs.dicomElement) {
      try {
        cornerstone.disable(this.$refs.dicomElement)
      } catch (e) {
        console.warn('清理 cornerstone 失败:', e)
      }
    }
    window.removeEventListener('resize', this.handleResize)
  },
  methods: {
    async initCornerstone() {
      if (this.initialized) {
        this.loadImage()
        return
      }

      try {
        cornerstoneWADOImageLoader.external.cornerstone = cornerstone
        cornerstoneWADOImageLoader.external.dicomParser = dicomParser

        cornerstoneWADOImageLoader.configure({
          beforeSend: (xhr) => {
            const token = sessionStorage.getItem('jwt_token')
            if (token) {
              xhr.setRequestHeader('Authorization', `Bearer ${token}`)
            }
          }
        })

        this.initialized = true
        await this.loadImage()
      } catch (err) {
        console.error('初始化 Cornerstone 失败:', err)
        this.error = '初始化失败'
        this.loading = false
      }
    },

    async loadImage() {
      const element = this.$refs.dicomElement
      if (!element) return

      this.loading = true
      this.error = null

      try {
        // 获取容器实际大小
        const parentContainer = element.parentElement
        const containerWidth = parentContainer.clientWidth || 800
        const containerHeight = parentContainer.clientHeight || 600
        
        element.style.width = `${containerWidth}px`
        element.style.height = `${containerHeight}px`

        try {
          cornerstone.getEnabledElement(element)
        } catch {
          cornerstone.enable(element)
        }

        const imageId = `wadouri:${this.dicomUrl}`
        const image = await cornerstone.loadImage(imageId)

        cornerstone.displayImage(element, image)

        this.imageWidth = image.width
        this.imageHeight = image.height

        const viewport = cornerstone.getViewport(element)
        viewport.scale = 1
        viewport.translation.x = 0
        viewport.translation.y = 0
        cornerstone.setViewport(element, viewport)

        cornerstone.fitToWindow(element)

        const finalViewport = cornerstone.getViewport(element)
        this.baseScale = finalViewport.scale || 1
        this.syncViewportMetrics(finalViewport, containerWidth, containerHeight)

        this.loading = false
      } catch (err) {
        console.error('加载 DICOM 图片失败:', err)
        this.error = '加载失败'
        this.loading = false
      }
    },
    syncViewportMetrics(viewport, containerWidth, containerHeight) {
      const element = this.$refs.dicomElement
      const host = element?.parentElement
      const width = containerWidth || host?.clientWidth || 0
      const height = containerHeight || host?.clientHeight || 0
      const scale = viewport?.scale || this.baseScale || 1
      const translationX = viewport?.translation?.x || 0
      const translationY = viewport?.translation?.y || 0

      this.currentScale = scale
      this.scaleX = scale
      this.scaleY = scale
      this.offsetX = (width - this.imageWidth * scale) / 2 + translationX
      this.offsetY = (height - this.imageHeight * scale) / 2 + translationY

      const percentBase = this.baseScale || 1
      this.$emit('viewport-change', Math.round((scale / percentBase) * 100))
    },
    getViewport() {
      const element = this.$refs.dicomElement
      if (!element) return null
      try {
        return cornerstone.getViewport(element)
      } catch {
        return null
      }
    },
    applyViewport(viewport) {
      const element = this.$refs.dicomElement
      if (!element || !viewport) return
      cornerstone.setViewport(element, viewport)
      this.syncViewportMetrics(viewport)
    },
    zoomIn() {
      this.zoomBy(1.18)
    },
    zoomOut() {
      this.zoomBy(1 / 1.18)
    },
    zoomBy(factor) {
      if (!this.interactive) return
      const viewport = this.getViewport()
      if (!viewport) return
      const minScale = Math.max(this.baseScale * 0.6, 0.1)
      const maxScale = Math.max(this.baseScale * 8, minScale + 0.1)
      viewport.scale = Math.min(maxScale, Math.max(minScale, viewport.scale * factor))
      this.applyViewport(viewport)
    },
    resetViewport() {
      const element = this.$refs.dicomElement
      if (!element) return
      cornerstone.fitToWindow(element)
      const viewport = cornerstone.getViewport(element)
      viewport.translation.x = 0
      viewport.translation.y = 0
      this.baseScale = viewport.scale || this.baseScale || 1
      this.applyViewport(viewport)
    },
    handleWheel(event) {
      if (!this.interactive) return
      event.preventDefault()
      const factor = event.deltaY < 0 ? 1.12 : 1 / 1.12
      this.zoomBy(factor)
    },
    handleMouseDown(event) {
      if (!this.interactive || event.button !== 0) return
      const viewport = this.getViewport()
      if (!viewport) return
      event.preventDefault()
      this.dragging = true
      this.dragState = {
        startX: event.clientX,
        startY: event.clientY,
        translationX: viewport.translation?.x || 0,
        translationY: viewport.translation?.y || 0
      }
      window.addEventListener('mousemove', this.handleMouseMove)
      window.addEventListener('mouseup', this.handleMouseUp)
      document.body.style.userSelect = 'none'
    },
    handleMouseMove(event) {
      if (!this.dragging || !this.dragState) return
      const viewport = this.getViewport()
      if (!viewport) return
      viewport.translation.x = this.dragState.translationX + (event.clientX - this.dragState.startX)
      viewport.translation.y = this.dragState.translationY + (event.clientY - this.dragState.startY)
      this.applyViewport(viewport)
    },
    handleMouseUp() {
      this.stopDragging()
    },
    stopDragging() {
      this.dragging = false
      this.dragState = null
      window.removeEventListener('mousemove', this.handleMouseMove)
      window.removeEventListener('mouseup', this.handleMouseUp)
      document.body.style.userSelect = ''
    },
    bboxStyle(det) {
      const isNormalized = det.x <= 1 && det.y <= 1 && det.width <= 1 && det.height <= 1
      
      let x, y, width, height
      if (isNormalized) {
        x = det.x * this.imageWidth * this.scaleX
        y = det.y * this.imageHeight * this.scaleY
        width = det.width * this.imageWidth * this.scaleX
        height = det.height * this.imageHeight * this.scaleY
      } else {
        x = det.x * this.scaleX
        y = det.y * this.scaleY
        width = det.width * this.scaleX
        height = det.height * this.scaleY
      }
      
      return {
        left: `${this.offsetX + x}px`,
        top: `${this.offsetY + y}px`,
        width: `${width}px`,
        height: `${height}px`
      }
    },
    toPercent(conf) {
      const v = Number(conf)
      if (Number.isNaN(v)) return 0
      return Math.round(v * 100)
    },
    handleResize() {
      if (this.initialized) {
        if (this.interactive) {
          this.resetViewport()
        } else {
          this.loadImage()
        }
      }
    }
  },
  watch: {
    filePath() {
      if (this.initialized) {
        this.loadImage()
      }
    },
    interactive() {
      this.$nextTick(() => {
        if (this.initialized) {
          this.resetViewport()
        }
      })
    }
  }
}
</script>

<style scoped>
.dicom-viewer {
  position: relative;
  width: 100%;
  height: 100%;
  background: #000;
  border-radius: 4px;
  overflow: hidden;
}

.dicom-viewer--interactive {
  cursor: grab;
}

.dicom-viewer--dragging {
  cursor: grabbing;
}

.dicom-canvas {
  width: 100%;
  height: 100%;
}

.loading-overlay,
.error-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.7);
  color: #fff;
  font-size: 14px;
}

.error-overlay {
  background: rgba(220, 53, 69, 0.8);
}

.bbox {
  position: absolute;
  border: 2px solid #00ff00;
  pointer-events: none;
  box-sizing: border-box;
}

.bbox-label {
  position: absolute;
  top: -20px;
  left: 0;
  background: rgba(0, 255, 0, 0.8);
  color: #000;
  font-size: 12px;
  padding: 2px 6px;
  border-radius: 2px;
  white-space: nowrap;
}
</style>
