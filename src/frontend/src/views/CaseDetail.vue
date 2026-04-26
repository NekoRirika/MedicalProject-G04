<template>
  <Layout title="病例详情" current-page="case">
    <div class="card">
      <div class="card-header">
        <h2>病例信息</h2>
        <div v-if="userStore.isDoctor()" class="btn-group" style="margin-top: -10px;">
          <button class="btn btn-primary" @click="editCase">编辑病例</button>
          <button class="btn btn-danger" @click="confirmDelete">删除病例</button>
        </div>
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
            <label>患者性别</label>
            <input type="text" :value="caseDetail?.patientGender || ''" disabled>
          </div>
        </div>
        <div class="form-row">
          <div class="form-group">
            <label>患者身份证号</label>
            <input type="text" :value="caseDetail?.patientIdCard || ''" disabled>
          </div>
          <div class="form-group">
            <label>患者手机号</label>
            <input type="text" :value="caseDetail?.patientPhone || ''" disabled>
          </div>
          <div class="form-group">
            <label>患者出生日期</label>
            <input type="text" :value="caseDetail?.patientBirthday || ''" disabled>
          </div>
        </div>
        <div class="form-group">
          <label>病例描述</label>
          <textarea rows="3" disabled>{{ caseDetail?.caseDescription || '' }}</textarea>
        </div>
        <div class="form-row">
          <div class="form-group">
            <label>检查日期</label>
            <input type="text" :value="caseDetail?.checkDate || ''" disabled>
          </div>
          <div class="form-group">
            <label>创建时间</label>
            <input type="text" :value="formatDateTime(caseDetail?.createdAt)" disabled>
          </div>
        </div>
        <div class="form-group">
          <label>检查备注</label>
          <textarea rows="2" disabled>{{ caseDetail?.checkNote || '' }}</textarea>
        </div>
      </div>
    </div>

    <div class="card">
      <div class="card-header">
        <h2>影像管理</h2>
        <div v-if="userStore.isDoctor()" class="btn-group" style="margin-top: -10px;">
          <button class="btn btn-primary" @click="uploadFile">上传影像</button>
          <button class="btn btn-secondary" @click="batchUploadFile">批量上传</button>
        </div>
      </div>
      <input
        v-if="userStore.isDoctor()"
        ref="fileInput"
        type="file"
        accept="image/*,.dcm,application/dicom"
        style="display:none"
        @change="onFileSelected"
      >
      <input
        v-if="userStore.isDoctor()"
        ref="batchFileInput"
        type="file"
        accept="image/*,.dcm,application/dicom"
        multiple
        style="display:none"
        @change="onBatchFileSelected"
      >
      <div class="detection-region">
        <div class="image-preview">
          <div v-if="images.length === 0" style="color: var(--text-light); padding: 10px;">
            暂无影像
          </div>
          <div
            class="image-item"
            :class="{ 'image-item--previewable': canPreviewImage(img.filePath) }"
            v-for="img in images"
            :key="img.imageId"
            @click="handleImageItemClick(img)"
          >
            <img 
              v-if="isImageFile(img.filePath)"
              :src="imageUrls[img.imageId] || 'https://via.placeholder.com/200?text=加载中'" 
              alt="影像" 
              class="normal-image normal-image--clickable"
              draggable="false"
            >
            <div v-else class="dicom-container">
              <DicomViewer 
                :caseId="caseId" 
                :filePath="img.filePath" 
                :width="200" 
                :height="200" 
              />
            </div>
            <div v-if="userStore.isDoctor()" class="image-actions">
              <button
                class="btn btn-primary btn-sm"
                @click.stop="detectSingleImage(img)"
                title="检测此影像"
                :disabled="detectionLoading"
              >检测</button>
              <button
                class="btn btn-danger btn-sm"
                @click.stop="deleteImage(img)"
                :disabled="detectionLoading"
              >删除</button>
            </div>
          </div>
        </div>
        <div
          v-if="detectionLoading && detectionTarget === 'single'"
          class="detection-overlay"
          aria-live="polite"
        >
          <div class="detection-overlay-inner">
            <span class="inline-spinner" aria-hidden="true"></span>
            <p class="detection-overlay-text">单图检测中，请稍候…</p>
          </div>
        </div>
      </div>
    </div>

    <div v-if="userStore.isDoctor()" class="card">
      <div class="card-header">
        <h2>检测管理</h2>
      </div>
      <div class="detection-region detection-region--batch">
        <div class="batch-detection-inner">
          <div class="confidence-threshold-group">
            <label for="confidence-threshold">置信度阈值：</label>
            <input 
              type="number" 
              id="confidence-threshold" 
              v-model.number="confidenceThreshold" 
              min="0.1" 
              max="0.9" 
              step="0.05"
              class="confidence-input"
            >
            <span class="threshold-value">{{ confidenceThreshold.toFixed(2) }}</span>
          </div>
          <button 
            class="btn btn-primary btn-batch-detect" 
            @click="startDetection"
            :disabled="images.length === 0 || detectionLoading"
          >
            批量肺炎检测
          </button>
          <p v-if="images.length === 0" style="margin-top: 20px; color: #dc3545;">
            ⚠️ 当前病例暂无影像，请先上传影像后再进行检测
          </p>
          <p v-else style="margin-top: 20px; color: var(--text-light);">
            点击按钮开始 AI 辅助检测，将对所有影像进行检测，检测结果将在新页面展示
          </p>
        </div>
        <div
          v-if="detectionLoading && detectionTarget === 'batch'"
          class="detection-overlay"
          aria-live="polite"
        >
          <div class="detection-overlay-inner">
            <span class="inline-spinner" aria-hidden="true"></span>
            <p class="detection-overlay-text">批量检测中，请稍候…</p>
          </div>
        </div>
      </div>
    </div>

    <div class="card detection-history-card">
      <div class="card-header">
        <h2>检测历史记录</h2>
      </div>
      <p v-if="detectionHistoryRows.length === 0" class="detection-history-empty">
        暂无检测记录。医生发起批量或单图检测后，将在此显示该病例下的检测任务列表；科研人员可查看历史结果。
      </p>
      <div v-else class="table-container">
        <table class="table detection-history-table">
          <thead>
            <tr>
              <th>检测 ID</th>
              <th>模型</th>
              <th>状态</th>
              <th>发起时间</th>
              <th>完成时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in detectionHistoryRows" :key="row.detectionId ?? row.id">
              <td>{{ row.detectionId ?? row.id ?? '—' }}</td>
              <td>{{ row.modelName || '—' }}</td>
              <td>{{ detectionStatusLabel(row.status) }}</td>
              <td>{{ formatDateTime(row.createdAt) || '—' }}</td>
              <td>{{ formatDateTime(row.completedAt) || '—' }}</td>
              <td>
                <button
                  type="button"
                  class="btn btn-primary btn-sm"
                  :disabled="!canViewDetectionResult(row)"
                  :title="!canViewDetectionResult(row) ? '仅已完成或失败的检测可查看详情' : ''"
                  @click="goDetectionResult(row)"
                >
                  查看
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <EditCaseModal
      v-if="userStore.isDoctor()"
      v-model:visible="showEditCaseModal"
      :case-id="caseId"
      :case-detail="caseDetail"
      @saved="fetchCaseDetail"
    />

    <ImageLightbox
      v-model:visible="lightboxVisible"
      :kind="lightboxKind"
      :src="lightboxSrc"
      :case-id="caseId"
      :dicom-file-path="lightboxDicomPath"
      title="病例影像预览"
    />
  </Layout>
</template>

<script>
import Layout from '@/components/common/Layout.vue'
import DicomViewer from '@/components/DicomViewer.vue'
import EditCaseModal from '@/components/case/EditCaseModal.vue'
import ImageLightbox from '@/components/case/ImageLightbox.vue'
import { getCaseDetail, deleteCase } from '@/api/cases'
import { getCaseImages, uploadCaseImage, batchUploadCaseImages, deleteCaseImage } from '@/api/images'
import { startDetection as startDetectionApi } from '@/api/detection'
import { getModels } from '@/api/models'
import { API_BASE_URL } from '@/api/config'
import { toast } from '@/utils/toast'
import userStore from '@/store/userStore'

export default {
  name: 'CaseDetail',
  components: { Layout, DicomViewer, EditCaseModal, ImageLightbox },
  data() {
    return {
      userStore,
      caseId: Number(this.$route.params.id),
      caseDetail: null,
      images: [],
      imageUrls: {},
      detectionLoading: false,
      detectionTarget: null,
      showEditCaseModal: false,
      confidenceThreshold: 0.55,
      lightboxVisible: false,
      lightboxSrc: '',
      lightboxKind: 'image',
      lightboxDicomPath: ''
    }
  },
  mounted() {
    this.fetchCaseDetail()
    this.fetchImages()
  },
  beforeUnmount() {
    Object.values(this.imageUrls).forEach((url) => {
      if (url && String(url).startsWith('blob:')) URL.revokeObjectURL(url)
    })
  },
  watch: {
    // 监听路由的 fullPath 变化，确保每次导航都会触发
    '$route.fullPath': {
      handler() {
        console.log('[CaseDetail] 路由变化，重新获取病例详情')
        this.fetchCaseDetail()
      },
      immediate: false
    }
  },
  computed: {
    /** 病例详情接口返回的 detections：该病例下各次检测任务（与后端 CaseResponse 一致） */
    detectionHistoryRows() {
      const raw = this.caseDetail?.detections
      if (!Array.isArray(raw) || raw.length === 0) return []
      return [...raw].sort((a, b) => {
        const ta = Date.parse(a?.createdAt || a?.completedAt || 0) || 0
        const tb = Date.parse(b?.createdAt || b?.completedAt || 0) || 0
        return tb - ta
      })
    }
  },
  methods: {
    canPreviewImage(filePath) {
      return this.isImageFile(filePath) || this.isDicomFile(filePath)
    },
    handleImageItemClick(img) {
      if (this.isImageFile(img.filePath)) {
        this.openImageLightbox(img)
        return
      }
      if (this.isDicomFile(img.filePath)) {
        this.openDicomLightbox(img)
      }
    },
    openImageLightbox(img) {
      const src = this.imageUrls[img.imageId]
      if (!src || String(src).includes('placeholder')) {
        toast.warning('图片尚未加载完成，请稍候再试')
        return
      }
      this.lightboxKind = 'image'
      this.lightboxDicomPath = ''
      this.lightboxSrc = src
      this.lightboxVisible = true
    },
    openDicomLightbox(img) {
      if (!this.isDicomFile(img.filePath)) return
      this.lightboxKind = 'dicom'
      this.lightboxSrc = ''
      this.lightboxDicomPath = img.filePath
      this.lightboxVisible = true
    },
    closeImageLightbox() {
      this.lightboxVisible = false
      this.lightboxSrc = ''
      this.lightboxDicomPath = ''
    },
    isImageFile(filePath) {
      const imageExtensions = ['.jpg', '.jpeg', '.png', '.gif', '.bmp']
      const ext = filePath.substring(filePath.lastIndexOf('.')).toLowerCase()
      return imageExtensions.includes(ext)
    },
    isDicomFile(filePath) {
      const ext = filePath.substring(filePath.lastIndexOf('.')).toLowerCase()
      return ext === '.dcm' || ext === '.dicom'
    },
    async loadImageWithToken(imageId, imgPath) {
      try {
        // 使用与项目统一的 token 获取方式
        const token = sessionStorage.getItem('jwt_token')
        if (!token) {
          toast.error('未登录，请重新登录')
          return
        }

        // 使用正确的接口路径
        const url = `${API_BASE_URL}/cases/${this.caseId}/images/${imgPath}`
        const res = await fetch(url, {
          method: 'GET',
          headers: {
            Authorization: `Bearer ${token}`
          }
        })

        if (!res.ok) {
          throw new Error(`HTTP error! status: ${res.status}`)
        }

        const blob = await res.blob()
        this.imageUrls[imageId] = URL.createObjectURL(blob)
      } catch (err) {
        console.error('图片加载失败', err)
        this.imageUrls[imageId] = 'https://via.placeholder.com/200?text=无权限'
      }
    },
    formatDateTime(date) {
      if (!date) return ''
      const s = String(date)
      if (s.includes('T')) return s.replace('T', ' ').slice(0, 19)
      return s
    },
    detectionStatusLabel(status) {
      const s = String(status || '').toLowerCase()
      if (s === 'pending') return '待处理'
      if (s === 'processing') return '检测中'
      if (s === 'completed') return '已完成'
      if (s === 'failed') return '失败'
      return status || '—'
    },
    canViewDetectionResult(row) {
      const s = String(row?.status || '').toLowerCase()
      return s === 'completed' || s === 'failed'
    },
    goDetectionResult(row) {
      const id = row?.detectionId ?? row?.id
      if (id == null || !this.canViewDetectionResult(row)) return
      this.$router.push(`/detection-result/${this.caseId}/${id}`)
    },
    async fetchCaseDetail() {
      try {
        const data = await getCaseDetail(this.caseId)
        this.caseDetail = data
      } catch (err) {
        toast.error('获取病例详情失败')
      }
    },
    async fetchImages() {
      try {
        const data = await getCaseImages(this.caseId)
        this.images = data || []
        this.images.forEach(img => {
          if (this.isImageFile(img.filePath)) {
            this.loadImageWithToken(img.imageId, img.filePath)
          }
        })
      } catch (err) {
        toast.error('获取影像列表失败')
      }
    },
    uploadFile() {
      this.$refs.fileInput?.click()
    },
    batchUploadFile() {
      this.$refs.batchFileInput?.click()
    },
    async onFileSelected(e) {
      const file = e.target.files?.[0]
      if (!file) return
      try {
        await uploadCaseImage(this.caseId, file)
        toast.success('上传成功')
        await this.fetchImages()
        await this.fetchCaseDetail()
      } catch (err) {
        toast.error(err.message || '上传失败')
      } finally {
        this.$refs.fileInput.value = ''
      }
    },
    async onBatchFileSelected(e) {
      const files = e.target.files
      if (!files || files.length === 0) return
      try {
        const result = await batchUploadCaseImages(this.caseId, files)
        if (result.successCount > 0) {
          toast.success(`批量上传完成，成功${result.successCount}个，失败${result.failedCount}个`)
        } else {
          toast.error('批量上传失败')
        }
        if (result.failedFiles && result.failedFiles.length > 0) {
          console.warn('批量上传失败的文件:', result.failedFiles)
        }
        await this.fetchImages()
        await this.fetchCaseDetail()
      } catch (err) {
        toast.error(err.message || '批量上传失败')
      } finally {
        this.$refs.batchFileInput.value = ''
      }
    },
    async deleteImage(img) {
      if (!confirm('确定要删除该影像吗？')) return
      try {
        await deleteCaseImage(this.caseId, img.imageId)
        delete this.imageUrls[img.imageId]
        toast.success('删除成功')
        await this.fetchImages()
        // 删除影像后刷新病例详情，确保状态更新
        await this.fetchCaseDetail()
      } catch (err) {
        toast.error(err.message || '删除失败')
      }
    },
    async startDetection() {
      console.log('[发起检测] ========== 开始发起AI检测 ==========')
      
      if (this.confidenceThreshold < 0.1 || this.confidenceThreshold > 0.9) {
        toast.error('置信度阈值必须在 0.1 ~ 0.9 之间')
        return
      }
      
      this.detectionLoading = true
      this.detectionTarget = 'batch'
      try {
        const models = await getModels()
        console.log('[发起检测] 获取到的模型列表:', models)
        const active = (models || []).find(m => m.status === 'active')
        console.log('[发起检测] 当前激活模型:', active)
        const modelId = active?.id ?? 1
        console.log('[发起检测] 使用的modelId:', modelId)

        const payload = {
          modelId,
          confidenceThreshold: this.confidenceThreshold
        }
        console.log('[发起检测] 请求参数:', JSON.stringify(payload))
        console.log('[发起检测] 发送请求到后端...')
        const data = await startDetectionApi(this.caseId, payload)
        console.log('[发起检测] 后端返回数据:', JSON.stringify(data))
        const detectionId = data?.detectionId
        
        if (!detectionId) throw new Error('后端未返回检测ID')
        console.log('[发起检测] 检测任务已创建, detectionId:', detectionId)
        // 跳转到检测结果页面
        this.$router.push(`/detection-result/${this.caseId}/${detectionId}`)
      } finally {
        this.detectionLoading = false
        this.detectionTarget = null
        // 检测完成后刷新病例详情，确保状态更新
        this.fetchCaseDetail()
      }
    },
    async detectSingleImage(img) {
      console.log('[单张检测] ========== 开始检测单张影像 ==========')
      console.log('[单张检测] imageId:', img.imageId, ', fileName:', img.fileName)
      
      if (this.confidenceThreshold < 0.1 || this.confidenceThreshold > 0.9) {
        toast.error('置信度阈值必须在 0.1 ~ 0.9 之间')
        return
      }
      
      this.detectionLoading = true
      this.detectionTarget = 'single'
      try {
        const models = await getModels()
        const active = (models || []).find(m => m.status === 'active')
        const modelId = active?.id ?? 1

        const payload = {
          modelId,
          imageId: img.imageId,
          confidenceThreshold: this.confidenceThreshold
        }
        console.log('[单张检测] 请求参数:', JSON.stringify(payload))
        const data = await startDetectionApi(this.caseId, payload)
        const detectionId = data?.detectionId
        
        if (!detectionId) throw new Error('后端未返回检测 ID')
        console.log('[单张检测] 检测任务已创建，detectionId:', detectionId)
       // 传递 imageId 参数，让结果页面只显示这一张图片
        this.$router.push(`/detection-result/${this.caseId}/${detectionId}?imageId=${img.imageId}`)
      } catch (err) {
        console.error('[单张检测] 检测失败:', err)
        toast.error(err.message || '检测失败')
      } finally {
        this.detectionLoading = false
        this.detectionTarget = null
      }
    },
    editCase() {
      if (!this.caseDetail) return
      this.showEditCaseModal = true
    },
    async confirmDelete() {
      if (!confirm('确定要删除该病例吗？\n删除后无法恢复！')) return
      try {
        await deleteCase(this.caseId)
        toast.success('删除成功')
        this.$router.push('/case-management')
      } catch (err) {
        toast.error('删除失败')
      }
    }
  }
}
</script>

<style scoped>
.detection-region {
  position: relative;
}

.detection-region--batch {
  min-height: 140px;
}

.batch-detection-inner {
  text-align: center;
  padding: 40px;
}

.btn-batch-detect {
  font-size: 18px;
  padding: 15px 30px;
}

.detection-overlay {
  position: absolute;
  inset: 0;
  z-index: 20;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.82);
  border-radius: inherit;
}

.detection-overlay-inner {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.detection-overlay-text {
  margin: 0;
  font-size: 15px;
  color: var(--text-color, #333);
}

.inline-spinner {
  display: inline-block;
  width: 40px;
  height: 40px;
  border: 3px solid #e0e0e0;
  border-top-color: var(--primary-color, #007bff);
  border-radius: 50%;
  animation: spin 0.85s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.image-preview .btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.batch-detection-inner .btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.image-preview {
  display: flex;
  flex-wrap: wrap;
  gap: 15px;
  padding: 15px;
}

.image-item {
  position: relative;
  width: 200px;
  height: 200px;
  border: 1px solid #ddd;
  border-radius: 4px;
  overflow: hidden;
}

.image-item--previewable {
  cursor: zoom-in;
}

.image-actions {
  position: absolute;
  right: 8px;
  top: 8px;
  display: flex;
  gap: 4px;
}

.btn-sm {
  padding: 4px 8px;
  font-size: 12px;
}

.normal-image {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.normal-image--clickable {
  cursor: inherit;
}

.dicom-container {
  width: 100%;
  height: 100%;
}

.detection-history-card {
  margin-bottom: 8px;
}

.detection-history-empty {
  margin: 0;
  padding: 16px 18px 20px;
  color: var(--text-light, #666);
  font-size: 14px;
  line-height: 1.5;
}

.detection-history-table {
  font-size: 14px;
}

.confidence-threshold-group {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-bottom: 16px;
  padding: 12px;
  background: #f8f9fa;
  border-radius: 6px;
}

.confidence-threshold-group label {
  font-size: 14px;
  color: #333;
  font-weight: 500;
}

.confidence-input {
  width: 80px;
  padding: 6px 10px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
  text-align: center;
}

.threshold-value {
  font-size: 16px;
  font-weight: 600;
  color: #007bff;
  min-width: 40px;
}
</style>