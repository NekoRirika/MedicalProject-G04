<template>
  <Layout title="首页" current-page="index">
    <div class="home-page">
      <div class="home-page__bg" :style="homeBackgroundStyle" aria-hidden="true"></div>
      <div class="home-page__veil" aria-hidden="true"></div>

      <div class="home-page__content">
        <div class="home-columns">
          <section class="home-column home-column--left">
            <section class="home-hero">
              <div class="home-hero__main">
                <div class="home-hero__badge">Medical Imaging Workspace</div>
                <h2 class="home-hero__title">欢迎回来，{{ welcomeName }}</h2>
                <p class="home-hero__subtitle">
                  今天是 {{ todayLabel }}。病例、影像、检测结果与反馈流转都已为你准备就绪。
                </p>
              </div>
            </section>

            <HomeDutyTable :schedule="dutySchedule" :date-label="todayLabel" />
          </section>

          <section class="home-column home-column--right">
            <HomeTodoPanel
              :todos="todoItems"
              @add="addTodo"
              @toggle="toggleTodo"
              @remove="removeTodo"
            />
            <HomeMeetingPanel :meetings="meetingItems" />
          </section>
        </div>
      </div>
    </div>
  </Layout>
</template>

<script>
import Layout from '@/components/common/Layout.vue'
import HomeDutyTable from '@/components/home/HomeDutyTable.vue'
import HomeTodoPanel from '@/components/home/HomeTodoPanel.vue'
import HomeMeetingPanel from '@/components/home/HomeMeetingPanel.vue'
import homePageImage from '@/assets/homePage.png'
import userStore from '@/store/userStore'

const TODO_STORAGE_KEY = 'home_todo_items_session_v1'

function formatDateLabel(date = new Date()) {
  const weekdays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  const year = date.getFullYear()
  const month = `${date.getMonth() + 1}`.padStart(2, '0')
  const day = `${date.getDate()}`.padStart(2, '0')
  return `${year}-${month}-${day} ${weekdays[date.getDay()]}`
}

function createDefaultTodos() {
  return [
    {
      id: `todo-${Date.now()}`,
      text: '复核今日新增病例资料是否完整，并关注待检测任务进度。',
      done: false,
      createdAtLabel: '默认待办'
    }
  ]
}

function loadTodos() {
  try {
    const raw = sessionStorage.getItem(TODO_STORAGE_KEY)
    if (!raw) return createDefaultTodos()
    const parsed = JSON.parse(raw)
    return Array.isArray(parsed) ? parsed : createDefaultTodos()
  } catch {
    return createDefaultTodos()
  }
}

export default {
  name: 'Index',
  components: {
    Layout,
    HomeDutyTable,
    HomeTodoPanel,
    HomeMeetingPanel
  },
  data() {
    return {
      userStore,
      dutySchedule: [
        { id: '1', time: '08:00 - 12:00', doctor: '李明', department: '呼吸科', room: '2F-201', note: '门诊接诊' },
        { id: '2', time: '09:00 - 13:00', doctor: '张宁', department: '心内科', room: '3F-308', note: '影像会诊' },
        { id: '3', time: '13:30 - 17:30', doctor: '王蕾', department: '胸外科', room: '4F-405', note: '术后复查' },
        { id: '4', time: '18:00 - 22:00', doctor: '周晨', department: '急诊内科', room: '急诊-2', note: '夜间值守' }
      ],
      todoItems: loadTodos(),
      meetingItems: [
        { id: 'm1', time: '09:30', location: '远程会议室 A', topic: '晨间影像质控会', desc: '复盘昨日异常样本与标注意见，同步今日重点病例。' },
        { id: 'm2', time: '14:00', location: '科研讨论室', topic: '模型评估例会', desc: '确认最新实验结果，对比人工反馈与模型输出差异。' },
        { id: 'm3', time: '16:30', location: '行政会议室', topic: '病例流程协调', desc: '同步病例上传、检测和审计日志的日常协作安排。' }
      ]
    }
  },
  computed: {
    homeBackgroundStyle() {
      return {
        backgroundImage: `url(${homePageImage})`
      }
    },
    welcomeName() {
      return this.userStore.displayName() || '同事'
    },
    todayLabel() {
      return formatDateLabel()
    }
  },
  methods: {
    persistTodos() {
      sessionStorage.setItem(TODO_STORAGE_KEY, JSON.stringify(this.todoItems))
    },
    addTodo(text) {
      this.todoItems.unshift({
        id: `todo-${Date.now()}-${Math.random().toString(16).slice(2, 8)}`,
        text,
        done: false,
        createdAtLabel: '刚刚添加'
      })
      this.persistTodos()
    },
    toggleTodo(id) {
      this.todoItems = this.todoItems.map((item) => {
        if (item.id !== id) return item
        return { ...item, done: !item.done }
      })
      this.persistTodos()
    },
    removeTodo(id) {
      this.todoItems = this.todoItems.filter((item) => item.id !== id)
      this.persistTodos()
    }
  }
}
</script>

<style scoped>
.home-page {
  position: relative;
  height: calc(100vh - 182px);
  min-height: 0;
  padding: 22px;
  border-radius: 28px;
  overflow: hidden;
  isolation: isolate;
}

.home-page__bg,
.home-page__veil {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.home-page__bg {
  z-index: 0;
  background-position: center center;
  background-repeat: no-repeat;
  background-size: cover;
  transform: scale(1.04);
  opacity: 0.42;
}

.home-page__veil {
  z-index: 1;
  background:
    linear-gradient(180deg, rgba(246, 250, 255, 0.88) 0%, rgba(246, 250, 255, 0.76) 35%, rgba(246, 250, 255, 0.9) 100%),
    radial-gradient(circle at top right, rgba(101, 178, 255, 0.18), transparent 28%);
}

.home-page__content {
  position: relative;
  z-index: 2;
  height: 100%;
  min-height: 0;
}

.home-columns {
  display: grid;
  grid-template-columns: minmax(0, 56%) minmax(320px, 44%);
  gap: 16px;
  height: 100%;
  min-height: 0;
}

.home-column {
  min-height: 0;
}

.home-column--left {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  gap: 16px;
}

.home-column--right {
  display: grid;
  grid-template-rows: minmax(0, 56%) minmax(0, 44%);
  gap: 16px;
  align-content: start;
}

.home-hero {
  min-width: 0;
}

.home-hero__main {
  min-width: 0;
  border-radius: 24px;
  padding: 20px 22px;
  background: rgba(255, 255, 255, 0.78);
  border: 1px solid rgba(214, 228, 243, 0.92);
  box-shadow: 0 22px 44px rgba(87, 123, 163, 0.1);
  backdrop-filter: blur(10px);
}

.home-hero__badge {
  display: inline-flex;
  align-items: center;
  padding: 7px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.78);
  border: 1px solid rgba(171, 205, 238, 0.7);
  color: #4b7196;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.04em;
  text-transform: uppercase;
}

.home-hero__title {
  margin: 12px 0 8px;
  font-size: clamp(26px, 2.7vw, 36px);
  line-height: 1.18;
  color: #1e3a5f;
}

.home-hero__subtitle {
  margin: 0;
  max-width: 720px;
  color: #56708b;
  font-size: 15px;
  line-height: 1.75;
}

@media (max-width: 1100px) {
  .home-page {
    height: auto;
    min-height: calc(100vh - 182px);
  }

  .home-columns {
    grid-template-columns: 1fr;
    height: auto;
  }

  .home-column--right {
    grid-template-rows: auto auto;
  }
}

@media (max-width: 768px) {
  .home-page {
    padding: 18px;
    border-radius: 20px;
  }

  .home-page__bg {
    background-position: 62% center;
    opacity: 0.36;
  }

  .home-page__content {
    height: auto;
  }

  .home-hero__main {
    padding: 20px;
    border-radius: 20px;
  }
}
</style>
