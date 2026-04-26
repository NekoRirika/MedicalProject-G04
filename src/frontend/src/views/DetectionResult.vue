<template>
  <Layout title="检测结果可视化" current-page="case">
    <div class="card result-card">
      <div class="card-header">
        <h2>检测信息</h2>
      </div>
      <div class="form-container">
        <div class="form-row">
          <div class="form-group">
            <label>病例编号</label>
            <input type="text" :value="caseDetail?.caseId || ''" disabled>
          </div>
          <div class="form-group">
            <label>患者姓名</label>
            <input type="text" :value="caseDetail?.patientName || ''" disabled>
          </div>
        </div>
        <div class="form-row">
          <div class="form-group">
            <label>检测时间</label>
            <input type="text" :value="formatDateTime(detection?.completedAt || detection?.createdAt)" disabled>
          </div>
          <div class="form-group">
            <label>使用模型</label>
            <input type="text" :value="detection?.modelName || ''" disabled>
          </div>
        </div>
      </div>
    </div>

    <div class="card">
      <div class="card-header">
        <h2>检测结果</h2>
        <div class="btn-group" style="margin-top: -10px;">
          <button class="btn btn-primary" @click="showExportOptions" :disabled="!detection || detection.status !== 'completed'">导出结果</button>
          <button class="btn btn-secondary" @click="scrollToFeedback" :disabled="!detection || detection.status !== 'completed'">人工反馈</button>
        </div>
      </div>
      <div style="text-align: center; padding: 20px;">
        <!-- 加载状态 -->
        <div v-if="!detection || !caseImages || caseImages.length === 0" style="padding: 40px;">
          <div style="color: var(--text-light);">加载中...</div>
        </div>
        
        <!-- 检测失败状态 -->
        <div v-else-if="detection.status === 'failed'" class="error-container">
          <div class="error-icon">❌</div>
          <div class="error-title">检测失败</div>
          <div class="error-details" v-if="detection.result">
            <div class="error-type" v-if="detection.result.errorType">
              <strong>错误类型：</strong>{{ getErrorTypeLabel(detection.result.errorType) }}
            </div>
            <div class="error-message" v-if="detection.result.errorMessage">
              <strong>错误信息：</strong>{{ detection.result.errorMessage }}
            </div>
            <div class="error-suggestion" v-if="detection.result.suggestion">
              <strong>建议操作：</strong>{{ detection.result.suggestion }}
            </div>
            <div class="error-cause" v-if="detection.result.cause">
              <strong>详细原因：</strong>{{ detection.result.cause }}
            </div>
          </div>
          <div class="error-fallback" v-else>
            {{ detection.errorMessage || '检测过程中发生错误，请稍后重试' }}
          </div>
          <div class="error-actions">
            <button class="btn btn-primary" @click="$router.push(`/case-management`)">返回病例管理</button>
            <button class="btn btn-secondary" @click="retryDetection" v-if="canRetry">重新检测</button>
          </div>
        </div>
        
        <!-- 检测中状态 -->
        <div v-else-if="detection.status === 'processing'" style="padding: 40px;">
          <div style="color: #007bff; font-size: 18px; margin-bottom: 10px;">
            ⏳ 检测中，请稍候...
          </div>
          <div style="color: var(--text-light);">
            AI 正在分析影像数据，这可能需要几分钟时间
          </div>
        </div>
        
        <!-- 检测完成状态 -->
        <div v-else-if="detection.status === 'completed'">
          <!-- 图片切换器：只在批量检测且有多张影像时显示 -->
          <div v-if="showImageSwitcher" style="margin-bottom: 15px;">
            <div style="display: flex; justify-content: center; gap: 10px; flex-wrap: wrap;">
              <button
                v-for="(img, idx) in caseImages"
                :key="img.imageId"
                class="btn"
                :class="currentImage?.imageId === img.imageId ? 'btn-primary' : 'btn-secondary'"
                @click="switchImage(idx)"
                style="padding: 6px 12px; font-size: 13px;"
              >
                影像 {{ idx + 1 }}: {{ img.fileName.substring(0, 20) }}{{ img.fileName.length > 20 ? '...' : '' }}
              </button>
            </div>
          </div>
          
          <div v-if="imageLoading" style="color: var(--text-light); padding: 40px;">
            图片加载中...
          </div>
          <div
            v-else-if="currentImage"
            class="detection-result-container"
            :class="{ 'detection-result-container--clickable': canPreviewCurrentImage() }"
            style="width: 100%; height: 600px; background: #000; border-radius: 4px;"
            @click="openImagePreview"
          >
            <!-- DICOM 文件使用 DicomViewer 组件 -->
            <DicomViewer
              v-if="isDicomFile(currentImage.filePath)"
              :file-path="currentImage.filePath"
              :case-id="caseId"
              :detections="detections"
            />
            <!-- 普通图片使用 img 标签 -->
            <div v-else class="detection-result" style="position: relative; display: inline-block; max-height: 600px;">
              <img
                ref="resultImg"
                :src="imageUrl"
                alt="胸部 X 光片"
                style="max-width: 100%; max-height: 600px; border-radius: 4px;"
                class="detection-result-image"
                @load="onImageLoad"
              >
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
            </div>
          </div>
          <div v-else style="color: var(--text-light); padding: 10px;">
            未找到可用于展示的影像
          </div>
          
          <!-- 无检测结果提示 -->
          <div v-if="(!detections || detections.length === 0)" style="margin-top: 20px; padding: 15px; background: #fff3cd; border: 1px solid #ffc107; border-radius: 4px; color: #856404;">
            ⚠️ 当前影像未检测到明显异常（可能是阴性结果）
          </div>
        </div>
        
        <!-- 未知状态 -->
        <div v-else style="padding: 40px;">
          <div style="color: var(--text-light);">
            未知检测状态：{{ detection.status }}
          </div>
        </div>
      </div>
      <div v-if="detection && detection.status === 'completed'" class="result-analysis">
          <h3>检测结果分析</h3>
          <div v-if="detection?.result?.analysis" style="margin-bottom: 10px;">
            <strong>总体分析：</strong>
            <p style="margin: 5px 0; line-height: 1.6;">{{ detection.result.analysis }}</p>
          </div>
          <div v-if="detection?.result?.totalDetections !== undefined" style="color: var(--text-light); font-size: 14px;">
            <span>共检测 {{ detection.result.totalImages }} 张影像</span>
            <span style="margin: 0 10px;">|</span>
            <span>发现 {{ detection.result.totalDetections }} 处病灶</span>
            <span v-if="detection.result.failedCount > 0" style="margin: 0 10px; color: #dc3545;">|</span>
            <span v-if="detection.result.failedCount > 0" style="color: #dc3545;">{{ detection.result.failedCount }} 张失败</span>
          </div>
          <div v-else style="color: var(--text-light);">
            {{ detection?.result?.analysis || '-' }}
          </div>
          
          <!-- 失败影像列表 -->
          <div v-if="detection?.result?.failedImages && detection.result.failedImages.length > 0" class="failed-images-section">
            <h4>⚠️ 失败的影像</h4>
            <ul>
              <li v-for="(errorMsg, idx) in detection.result.failedImages" :key="idx" class="failed-image-item">
                {{ errorMsg }}
              </li>
            </ul>
          </div>
        </div>
    </div>

    <div class="card feedback-card">
      <div class="card-header">
        <h2>人工反馈</h2>
      </div>
      <div class="form-container">
        <form ref="feedbackForm" class="feedback-form" @submit.prevent="submitFeedbackForm">
          <div class="form-group">
            <label>评价结果</label>
            <div
              class="feedback-choice-row"
              role="group"
              aria-label="评价结果"
            >
              <button
                v-for="opt in feedbackOptions"
                :key="opt.value"
                type="button"
                class="feedback-choice"
                :class="{ 'feedback-choice--active': feedback.result === opt.value }"
                @click="feedback.result = opt.value"
              >
                <span class="feedback-choice-emoji" aria-hidden="true">{{ opt.emoji }}</span>
                <span class="feedback-choice-label">{{ opt.label }}</span>
              </button>
            </div>
          </div>
          <div class="form-group">
            <label>反馈意见</label>
            <textarea v-model="feedback.comment" rows="3" placeholder="请输入您的反馈意见"></textarea>
          </div>
          <button type="submit" class="btn btn-primary">提交反馈</button>
        </form>
      </div>
    </div>

    <ImageLightbox
      v-model:visible="lightboxVisible"
      :kind="lightboxKind"
      :src="lightboxSrc"
      :case-id="caseId"
      :dicom-file-path="lightboxDicomPath"
      :detections="detections"
      title="检测结果影像预览"
    />

    <!-- 导出选项弹窗 -->
    <div v-if="showExportModal" class="modal-overlay" @click.self="closeExportModal">
      <div class="modal-content">
        <div class="modal-header">
          <h3>选择导出格式</h3>
          <button class="modal-close" @click="closeExportModal">&times;</button>
        </div>
        <div class="modal-body">
          <div class="export-options">
            <button class="export-option-btn" @click="exportResult('json')">
              <div class="export-option-icon">📄</div>
              <div class="export-option-title">JSON 格式</div>
              <div class="export-option-desc">仅导出检测结果数据</div>
            </button>
            <button class="export-option-btn" @click="exportResult('zip')">
              <div class="export-option-icon">📦</div>
              <div class="export-option-title">ZIP 格式（推荐）</div>
              <div class="export-option-desc">导出标注图 + JSON 结果文件</div>
            </button>
          </div>
        </div>
      </div>
    </div>
  </Layout>
</template>

<script>
import Layout from '@/components/common/Layout.vue'
import DicomViewer from '@/components/DicomViewer.vue'
import ImageLightbox from '@/components/case/ImageLightbox.vue'
import { getCaseDetail } from '@/api/cases'
import { getCaseImages } from '@/api/images'
import {
  getDetectionResult,
  exportDetectionResult,
  submitDetectionFeedback
} from '@/api/detection'
import { API_BASE_URL } from '@/api/config'
import { toast } from '@/utils/toast'

export default {
  name: 'DetectionResult',
  components: { Layout, DicomViewer, ImageLightbox },
  data() {
    return {
      caseId: Number(this.$route.params.caseId),
      detectionId: Number(this.$route.params.detectionId),
      sourceImageId: this.$route.query.imageId ? Number(this.$route.query.imageId) : null,  // 来源图片 ID（单张检测时传递）
      caseDetail: null,
      detection: null,
      caseImages: [],
      currentImage: null,
      imageUrl: '',
      imageLoading: false,
      detections: [],
      scaleX: 1,
      scaleY: 1,
      feedback: { result: '', comment: '' },
      pollingTimer: null,
      pollingInterval: 2000,
      pollingCount: 0,
      maxPollingCount: 20,
      showExportModal: false,
      lightboxVisible: false,
      lightboxKind: 'image',
      lightboxSrc: '',
      lightboxDicomPath: '',
      feedbackOptions: [
        { value: '准确', label: '结果准确', emoji: '✅' },
        { value: '漏检', label: '漏检', emoji: '🔍' },
        { value: '误检', label: '误检', emoji: '⚠️' }
      ]
    }
  },
  computed: {
    showImageSwitcher() {
      return !this.sourceImageId && this.caseImages.length > 1
    },
    canRetry() {
      return this.detection && this.detection.status === 'failed'
    }
  },
  mounted() {
    this.fetchAll()
  },
  beforeDestroy() {
    if (this.imageUrl && this.imageUrl.startsWith('blob:')) {
      URL.revokeObjectURL(this.imageUrl)
    }
    if (this.pollingTimer) {
      clearTimeout(this.pollingTimer)
      this.pollingTimer = null
    }
  },
  methods: {
    canPreviewCurrentImage() {
      return !!this.currentImage && (this.isDicomFile(this.currentImage.filePath) || !!this.imageUrl)
    },
    openImagePreview() {
      if (!this.currentImage) return
      if (this.isDicomFile(this.currentImage.filePath)) {
        this.lightboxKind = 'dicom'
        this.lightboxSrc = ''
        this.lightboxDicomPath = this.currentImage.filePath
        this.lightboxVisible = true
        return
      }
      if (!this.imageUrl) {
        toast.warning('图片尚未加载完成，请稍候再试')
        return
      }
      this.lightboxKind = 'image'
      this.lightboxDicomPath = ''
      this.lightboxSrc = this.imageUrl
      this.lightboxVisible = true
    },
    // 判断是否为 DICOM 文件
    isDicomFile(filePath) {
      if (!filePath) return false
      const lowerPath = filePath.toLowerCase()
      return lowerPath.endsWith('.dcm') || lowerPath.endsWith('.dicom')
    },
    // 使用带 token 认证的方式加载图片
    async loadImageWithToken(filePath) {
      if (!filePath) {
        this.imageUrl = ''
        return
      }

      this.imageLoading = true
      try {
        const token = sessionStorage.getItem('jwt_token')
        if (!token) {
          toast.error('未登录，请重新登录')
          this.imageUrl = ''
          return
        }

        const url = `${API_BASE_URL}/cases/${this.caseId}/images/${filePath}`
        console.log('[检测结果] 加载图片, url:', url)
        const res = await fetch(url, {
          method: 'GET',
          headers: {
            'Authorization': `Bearer ${token}`
          }
        })

        console.log('[检测结果] 图片响应状态:', res.status, res.ok)

        if (!res.ok) {
          throw new Error(`HTTP error! status: ${res.status}`)
        }

        const blob = await res.blob()
        console.log('[检测结果] 图片blob大小:', blob.size, 'type:', blob.type)
        if (this.imageUrl && this.imageUrl.startsWith('blob:')) {
          URL.revokeObjectURL(this.imageUrl)
        }
        this.imageUrl = URL.createObjectURL(blob)
        console.log('[检测结果] 图片URL已生成:', this.imageUrl)
      } catch (err) {
        console.error('[检测结果] 图片加载失败:', err)
        toast.error('图片加载失败')
        this.imageUrl = ''
      } finally {
        this.imageLoading = false
      }
    },
    async fetchAll() {
      console.log('[检测结果] ========== 开始加载检测结果页面 ==========')
      console.log('[检测结果] caseId:', this.caseId, ', detectionId:', this.detectionId, ', sourceImageId:', this.sourceImageId)
      try {
        const [caseData, imagesData, detectionData] = await Promise.all([
          getCaseDetail(this.caseId),
          getCaseImages(this.caseId),
          getDetectionResult(this.caseId, this.detectionId)
        ])

        this.caseDetail = caseData
        this.caseImages = imagesData || []
        this.detection = detectionData

        console.log('[检测结果] 病例详情:', caseData)
        console.log('[检测结果] 图像列表:', this.caseImages.length, '张')
        this.caseImages.forEach((img, i) => {
          console.log('[检测结果]   图像' + (i+1) + ':', img.imageId, img.fileName, img.filePath)
        })

        console.log('[检测结果] 检测状态:', detectionData?.status)
        console.log('[检测结果] 检测完整数据:', JSON.stringify(detectionData, null, 2))

        // 检查检测状态，如果是 pending 或 processing，启动轮询
        if (detectionData && (detectionData.status === 'pending' || detectionData.status === 'processing')) {
          console.log('[检测结果] 检测尚未完成，启动轮询... status=' + detectionData.status)
          this.startPolling()
          return  // 先不处理结果，等待轮询完成
        }

        // 检测已完成或失败，处理结果
        this.processDetectionResult()

        console.log('[检测结果] ========== 页面加载完成 ==========')
      } catch (err) {
        console.error('[检测结果] 加载失败:', err)
        toast.error(err.message || '加载检测结果失败')
      }
    },
    // 启动轮询
    startPolling() {
      this.pollingCount = 0
      this.pollingInterval = 2000
      console.log('[轮询] 启动指数退避轮询，初始间隔 2 秒')
      this.pollNext()
    },
    pollNext() {
      if (this.pollingCount >= this.maxPollingCount) {
        console.warn('[轮询] 达到最大轮询次数，停止轮询')
        toast.warning('检测时间过长，请刷新页面查看结果')
        return
      }
      
      this.pollingTimer = setTimeout(async () => {
        try {
          console.log(`[轮询] 第 ${this.pollingCount + 1} 次检查检测状态...`)
          const detectionData = await getDetectionResult(this.caseId, this.detectionId)
          this.detection = detectionData

          console.log('[轮询] 当前状态:', detectionData?.status)

          if (detectionData && (detectionData.status === 'completed' || detectionData.status === 'failed')) {
            console.log('[轮询] 检测完成或失败，停止轮询')
            this.processDetectionResult()
          } else {
            this.pollingCount++
            this.pollingInterval = Math.min(this.pollingInterval * 2, 16000)
            console.log(`[轮询] 下次轮询间隔: ${this.pollingInterval}ms`)
            this.pollNext()
          }
        } catch (err) {
          console.error('[轮询] 轮询失败:', err)
          this.pollingCount++
          this.pollNext()
        }
      }, this.pollingInterval)
    },
    /**
     * v1.1+：GET 检测结果可能在根级返回扁平 detections（与 imageResults[].detections 同形，且可带 imageId）。
     * 排除误把「检测任务对象」当作框列表（含 imageResults/status 等）。
     */
    getFlatRootDetectionBoxes() {
      const raw = this.detection?.detections
      if (!Array.isArray(raw) || raw.length === 0) return []
      const first = raw[0]
      if (!first || typeof first !== 'object') return []
      if ('imageResults' in first || ('status' in first && 'detectionId' in first && !('width' in first))) {
        return []
      }
      if (!('x' in first) && !('width' in first)) return []
      return raw
    },
    pickDetectionsForImage(imageId) {
      if (imageId == null) return []
      const imageResults = this.detection?.imageResults
      if (Array.isArray(imageResults) && imageResults.length > 0) {
        const r = imageResults.find((x) => x.imageId === imageId)
        const list = r?.detections
        if (Array.isArray(list) && list.length) return list
      }
      const flat = this.getFlatRootDetectionBoxes()
      if (!flat.length) return []
      if (flat[0]?.imageId != null) {
        return flat.filter((d) => d.imageId === imageId)
      }
      if (this.sourceImageId === imageId) return flat
      return []
    },
    // 处理检测结果
    processDetectionResult() {
      console.log('[检测结果] ========== 处理检测结果 ==========')
      console.log('[检测结果] 完整的 detection 对象:', JSON.stringify(this.detection, null, 2))
      const imageResults = this.detection?.imageResults
      console.log('[检测结果] imageResults:', imageResults)
      console.log('[检测结果] result 对象:', this.detection?.result)
      console.log('[检测结果] analysis:', this.detection?.result?.analysis)

      // 确定要显示的图片
      let targetImage = null
      let targetDetections = []

      if (this.sourceImageId) {
        console.log('[检测结果] 单张检测模式，查找 imageId:', this.sourceImageId)
        targetImage = this.caseImages.find((img) => img.imageId === this.sourceImageId)
        targetDetections = this.pickDetectionsForImage(this.sourceImageId)
        console.log('[检测结果] 单图检测框数量:', targetDetections.length)
      } else if (imageResults && imageResults.length > 0) {
        console.log('[检测结果] 批量检测模式，按 imageResults 首张对齐影像')
        const firstResult = imageResults[0]
        targetImage =
          this.caseImages.find((img) => img.imageId === firstResult.imageId) || this.caseImages[0]
        targetDetections = this.pickDetectionsForImage(firstResult.imageId)
        console.log('[检测结果] 首张图像检测结果:', targetDetections.length, '个检测项')
      } else if (this.caseImages.length > 0) {
        console.warn('[检测结果] imageResults 为空，尝试用首张影像 + 根级 detections 兜底')
        targetImage = this.caseImages[0]
        targetDetections = this.pickDetectionsForImage(targetImage.imageId)
      } else {
        console.warn('[检测结果] 无影像、无 imageResults，无法展示')
      }

      this.detections = targetDetections

      // 加载目标图片
      if (targetImage?.filePath) {
        console.log('[检测结果] 准备加载图片:', targetImage.filePath)
        this.currentImage = targetImage
        console.log('[检测结果] currentImage:', this.currentImage)
        // 如果是 DICOM 文件，不需要加载 imageUrl
        if (!this.isDicomFile(targetImage.filePath)) {
          this.loadImageWithToken(targetImage.filePath)
        }
      } else {
        console.warn('[检测结果] 没有可显示的图片')
      }

      console.log('[检测结果] ========== 结果处理完成 ==========')
    },
    onImageLoad() {
      const img = this.$refs.resultImg
      if (!img) return
      const naturalW = img.naturalWidth || 1
      const naturalH = img.naturalHeight || 1
      const renderedW = img.clientWidth || naturalW
      const renderedH = img.clientHeight || naturalH
      this.scaleX = renderedW / naturalW
      this.scaleY = renderedH / naturalH
      console.log('[检测结果] 图片加载完成，原始尺寸:', naturalW, 'x', naturalH, ', 渲染尺寸:', renderedW, 'x', renderedH, ', 缩放:', this.scaleX.toFixed(3), 'x', this.scaleY.toFixed(3))
    },
    switchImage(index) {
      console.log('[切换图片] 切换到第', index, '张图片')
      const img = this.caseImages[index]
      if (!img) return
      
      this.currentImage = img
      this.detections = this.pickDetectionsForImage(img.imageId)
      console.log('[切换图片] detections 数量:', this.detections.length)

      // 加载图片
      if (!this.isDicomFile(img.filePath)) {
        this.loadImageWithToken(img.filePath)
      }
    },
    bboxStyle(det) {
      const img = this.$refs.resultImg
      if (!img) {
        return { left: '0px', top: '0px', width: '0px', height: '0px' }
      }
      
      const naturalW = img.naturalWidth || 1
      const naturalH = img.naturalHeight || 1
      
      // 检查坐标是否归一化（0-1范围）
      const isNormalized = det.x <= 1 && det.y <= 1 && det.width <= 1 && det.height <= 1
      
      let x, y, width, height
      if (isNormalized) {
        // 归一化坐标：需要乘以原始图像尺寸
        x = det.x * naturalW * this.scaleX
        y = det.y * naturalH * this.scaleY
        width = det.width * naturalW * this.scaleX
        height = det.height * naturalH * this.scaleY
      } else {
        // 原始坐标：直接使用
        x = det.x * this.scaleX
        y = det.y * this.scaleY
        width = det.width * this.scaleX
        height = det.height * this.scaleY
      }
      
      return {
        left: `${x}px`,
        top: `${y}px`,
        width: `${width}px`,
        height: `${height}px`
      }
    },
    toPercent(conf) {
      const v = Number(conf)
      if (Number.isNaN(v)) return 0
      return Math.round(v * 100)
    },
    formatDateTime(date) {
      if (!date) return ''
      const s = String(date)
      if (s.includes('T')) return s.replace('T', ' ').slice(0, 19)
      return s
    },
    getErrorTypeLabel(errorType) {
      const typeMap = {
        'FILE_NOT_FOUND': '文件不存在',
        'AI_SERVICE_UNAVAILABLE': 'AI服务不可用',
        'MODEL_ERROR': '模型错误',
        'TASK_NOT_FOUND': '任务不存在',
        'NO_IMAGES': '无影像文件',
        'UNKNOWN': '未知错误'
      }
      return typeMap[errorType] || errorType
    },
    async retryDetection() {
      try {
        toast.info('正在重新发起检测...')
        this.$router.push(`/case-detail/${this.caseId}`)
      } catch (err) {
        toast.error(err.message || '重新检测失败')
      }
    },
    scrollToFeedback() {
      this.$refs.feedbackForm?.scrollIntoView({ behavior: 'smooth', block: 'start' })
    },
    async exportResult(format = 'json') {
      try {
        this.closeExportModal()
        toast.info('正在准备导出...')
        
        const blob = await exportDetectionResult(this.caseId, this.detectionId, format)
        const url = URL.createObjectURL(blob)
        const a = document.createElement('a')
        a.href = url
        
        const timestamp = new Date().getTime()
        if (format === 'zip') {
          a.download = `detection-${this.caseId}-${this.detectionId}-${timestamp}.zip`
        } else {
          a.download = `detection-${this.caseId}-${this.detectionId}-${timestamp}.json`
        }
        
        document.body.appendChild(a)
        a.click()
        document.body.removeChild(a)
        URL.revokeObjectURL(url)
        
        toast.success('导出成功')
      } catch (err) {
        toast.error(err.message || '导出失败')
      }
    },
    showExportOptions() {
      this.showExportModal = true
    },
    closeExportModal() {
      this.showExportModal = false
    },
    async submitFeedbackForm() {
      try {
        const { result, comment } = this.feedback
        if (!result || !comment) {
          toast.warning('请填写评价结果和反馈意见')
          return
        }

        await submitDetectionFeedback(this.caseId, this.detectionId, {
          evaluation: result,
          feedback: comment
        })
        toast.success('反馈提交成功')
        this.feedback = { result: '', comment: '' }
      } catch (err) {
        toast.error(err.message || '提交反馈失败')
      }
    }
  }
}
</script>

<style scoped>
.result-card {
  max-width: 1100px;
  margin-left: auto;
  margin-right: auto;
}

.result-analysis {
  margin-top: 20px;
  padding: 14px 16px;
  background: #fafcff;
  border: 1px solid #e8eef8;
  border-radius: 8px;
}

.failed-images-section {
  margin-top: 15px;
  padding: 12px;
  background: #fff3cd;
  border: 1px solid #ffc107;
  border-radius: 6px;
}

.failed-images-section h4 {
  margin: 0 0 10px 0;
  color: #856404;
  font-size: 14px;
}

.failed-image-item {
  color: #856404;
  font-size: 13px;
  margin: 5px 0;
  line-height: 1.5;
}

.error-container {
  padding: 40px;
  text-align: center;
}

.error-icon {
  font-size: 48px;
  margin-bottom: 15px;
}

.error-title {
  color: #dc3545;
  font-size: 20px;
  font-weight: 600;
  margin-bottom: 20px;
}

.error-details {
  max-width: 600px;
  margin: 0 auto 20px;
  text-align: left;
  background: #f8d7da;
  border: 1px solid #f5c6cb;
  border-radius: 8px;
  padding: 20px;
}

.error-type {
  color: #721c24;
  margin-bottom: 10px;
  font-size: 14px;
}

.error-message {
  color: #721c24;
  margin-bottom: 10px;
  font-size: 14px;
  line-height: 1.5;
}

.error-suggestion {
  color: #0c5460;
  background: #d1ecf1;
  border: 1px solid #bee5eb;
  padding: 10px;
  border-radius: 4px;
  margin-top: 10px;
  font-size: 14px;
}

.error-cause {
  color: #721c24;
  font-size: 13px;
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px solid #f5c6cb;
}

.error-fallback {
  color: var(--text-light);
  margin-bottom: 20px;
}

.error-actions {
  display: flex;
  gap: 10px;
  justify-content: center;
  margin-top: 20px;
}

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background: #fff;
  border-radius: 8px;
  width: 90%;
  max-width: 500px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #e0e0e0;
}

.modal-header h3 {
  margin: 0;
  font-size: 18px;
  color: #333;
}

.modal-close {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: #666;
  padding: 0;
  width: 30px;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.modal-close:hover {
  color: #333;
}

.modal-body {
  padding: 20px;
}

.export-options {
  display: flex;
  gap: 15px;
}

.export-option-btn {
  flex: 1;
  padding: 20px;
  border: 2px solid #e0e0e0;
  border-radius: 8px;
  background: #fff;
  cursor: pointer;
  text-align: center;
  transition: all 0.2s ease;
}

.export-option-btn:hover {
  border-color: #007bff;
  background: #f8f9fa;
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(0, 123, 255, 0.1);
}

.export-option-icon {
  font-size: 40px;
  margin-bottom: 10px;
}

.export-option-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin-bottom: 5px;
}

.export-option-desc {
  font-size: 13px;
  color: #666;
}

.feedback-card {
  max-width: 1100px;
  margin-left: auto;
  margin-right: auto;
}

.feedback-form {
  padding-bottom: 4px;
}

.feedback-choice-row {
  display: flex;
  flex-direction: row;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 10px;
}

.feedback-choice {
  flex: 1 1 160px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 16px 12px;
  border: 2px solid #e0e4ec;
  border-radius: 10px;
  background: #fff;
  cursor: pointer;
  font: inherit;
  color: inherit;
  transition: border-color 0.15s ease, background 0.15s ease, box-shadow 0.15s ease;
}

.feedback-choice:hover {
  border-color: #b8c5d9;
  background: #f8fafc;
}

.feedback-choice--active {
  border-color: var(--primary-color, #007bff);
  background: #e8f4ff;
  box-shadow: 0 0 0 2px rgba(0, 123, 255, 0.22);
}

.feedback-choice-emoji {
  font-size: 2.75rem;
  line-height: 1;
}

.feedback-choice-label {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-color, #333);
}

.detection-result-container--clickable {
  cursor: zoom-in;
}

.detection-result-image {
  cursor: inherit;
}
</style>
