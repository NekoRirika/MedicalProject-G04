<template>
  <div class="home-panel">
    <div class="home-panel__head">
      <div>
        <p class="home-panel__eyebrow">Today On Duty</p>
        <h3 class="home-panel__title">今日值班表</h3>
      </div>
      <span class="home-panel__meta">{{ dateLabel }}</span>
    </div>

    <div class="home-duty-table-wrap">
      <table class="home-duty-table">
        <thead>
          <tr>
            <th>时段</th>
            <th>值班医生</th>
            <th>科室</th>
            <th>诊室</th>
            <th>备注</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in schedule" :key="item.id">
            <td>{{ item.time }}</td>
            <td>{{ item.doctor }}</td>
            <td>{{ item.department }}</td>
            <td>{{ item.room }}</td>
            <td>{{ item.note }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script>
export default {
  name: 'HomeDutyTable',
  props: {
    schedule: {
      type: Array,
      default: () => []
    },
    dateLabel: {
      type: String,
      default: ''
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

.home-duty-table-wrap {
  overflow-x: auto;
}

.home-duty-table {
  width: 100%;
  border-collapse: collapse;
  min-width: 620px;
}

.home-duty-table th,
.home-duty-table td {
  padding: 12px 14px;
  border-bottom: 1px solid #e8eef5;
  text-align: left;
  font-size: 14px;
}

.home-duty-table th {
  color: #68809b;
  font-weight: 700;
  background: rgba(245, 249, 253, 0.9);
}

.home-duty-table td {
  color: #27415d;
}

.home-duty-table tbody tr:last-child td {
  border-bottom: none;
}
</style>
