<template>
  <div class="home-panel home-panel--meeting">
    <div class="home-panel__head">
      <div>
        <p class="home-panel__eyebrow">Meetings</p>
        <h3 class="home-panel__title">会议安排</h3>
      </div>
      <span class="home-panel__meta">固定日程</span>
    </div>

    <div class="home-meeting-scroll">
      <ul class="home-meeting-list">
        <li v-for="item in meetings" :key="item.id" class="home-meeting-item">
          <div class="home-meeting-time">
            <strong>{{ item.time }}</strong>
            <span>{{ item.location }}</span>
          </div>
          <div class="home-meeting-body">
            <h4>{{ item.topic }}</h4>
            <p>{{ item.desc }}</p>
          </div>
        </li>
      </ul>
    </div>
  </div>
</template>

<script>
export default {
  name: 'HomeMeetingPanel',
  props: {
    meetings: {
      type: Array,
      default: () => []
    }
  }
}
</script>

<style scoped>
.home-panel {
  background: rgba(255, 255, 255, 0.8);
  border: 1px solid rgba(214, 228, 243, 0.9);
  border-radius: 22px;
  box-shadow: 0 20px 46px rgba(88, 122, 161, 0.1);
  backdrop-filter: blur(10px);
  padding: 22px;
}

.home-panel--meeting {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  min-height: 0;
  height: 100%;
}

.home-panel__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 18px;
}

.home-panel__eyebrow {
  margin: 0 0 6px;
  font-size: 12px;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  color: #6f88a5;
}

.home-panel__title {
  margin: 0;
  font-size: 22px;
  color: #1f3653;
}

.home-panel__meta {
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(78, 153, 232, 0.12);
  color: #3f75a8;
  font-size: 12px;
  font-weight: 700;
}

.home-meeting-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: grid;
  gap: 12px;
}

.home-meeting-scroll {
  min-height: 0;
  overflow-y: auto;
  padding-right: 4px;
}

.home-meeting-scroll::-webkit-scrollbar {
  width: 8px;
}

.home-meeting-scroll::-webkit-scrollbar-thumb {
  background: rgba(120, 155, 191, 0.34);
  border-radius: 999px;
}

.home-meeting-item {
  display: grid;
  grid-template-columns: minmax(84px, 24%) minmax(0, 76%);
  gap: 14px;
  padding: 14px 16px;
  border-radius: 16px;
  background: rgba(247, 251, 255, 0.92);
  border: 1px solid #e6eef7;
  min-width: 0;
}

.home-meeting-time {
  display: grid;
  gap: 4px;
  align-content: start;
  min-width: 0;
}

.home-meeting-time strong {
  font-size: 16px;
  color: #234264;
  word-break: break-word;
}

.home-meeting-time span {
  font-size: 12px;
  color: #6f8398;
  word-break: break-word;
}

.home-meeting-body {
  min-width: 0;
}

.home-meeting-body h4 {
  margin: 0 0 6px;
  font-size: clamp(14px, 1vw, 16px);
  color: #1f3653;
  word-break: break-word;
}

.home-meeting-body p {
  margin: 0;
  color: #67798d;
  line-height: 1.6;
  font-size: clamp(13px, 0.9vw, 14px);
  word-break: break-word;
}

@media (max-width: 900px) {
  .home-meeting-item {
    grid-template-columns: 1fr;
  }
}
</style>
