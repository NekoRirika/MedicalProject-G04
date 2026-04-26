<template>
  <div class="home-panel home-panel--todo">
    <div class="home-panel__head">
      <div>
        <p class="home-panel__eyebrow">Today Tasks</p>
        <h3 class="home-panel__title">今日待办</h3>
      </div>
      <span class="home-panel__meta">{{ todos.length }} 项</span>
    </div>

    <div class="home-todo-create">
      <input
        v-model.trim="draft"
        type="text"
        class="home-todo-input"
        placeholder="添加新的待办事项"
        @keydown.enter.prevent="submit"
      >
      <button type="button" class="btn btn-primary home-todo-add" @click="submit">添加</button>
    </div>

    <div class="home-todo-scroll">
      <ul v-if="todos.length > 0" class="home-todo-list">
        <li v-for="item in todos" :key="item.id" class="home-todo-item">
          <button
            type="button"
            class="home-todo-check"
            :class="{ 'home-todo-check--done': item.done }"
            @click="$emit('toggle', item.id)"
          >
            {{ item.done ? '✓' : '' }}
          </button>
          <div class="home-todo-body">
            <p class="home-todo-text" :class="{ 'home-todo-text--done': item.done }">{{ item.text }}</p>
            <span class="home-todo-time">{{ item.createdAtLabel }}</span>
          </div>
          <button type="button" class="home-todo-remove" @click="$emit('remove', item.id)">删除</button>
        </li>
      </ul>
      <p v-else class="home-panel__empty">还没有待办事项，添加一个今日目标吧。</p>
    </div>
  </div>
</template>

<script>
export default {
  name: 'HomeTodoPanel',
  props: {
    todos: {
      type: Array,
      default: () => []
    }
  },
  emits: ['add', 'toggle', 'remove'],
  data() {
    return {
      draft: ''
    }
  },
  methods: {
    submit() {
      if (!this.draft) return
      this.$emit('add', this.draft)
      this.draft = ''
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

.home-panel--todo {
  display: grid;
  grid-template-rows: auto auto minmax(0, 1fr);
  align-content: start;
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

.home-todo-create {
  display: flex;
  gap: 10px;
  margin-bottom: 18px;
}

.home-todo-input {
  flex: 1;
  min-height: 44px;
  border-radius: 14px;
  border: 1px solid #d6e4f1;
  background: #fff;
  padding: 11px 14px;
  font-size: 14px;
  outline: none;
}

.home-todo-input:focus {
  border-color: #60a7f0;
  box-shadow: 0 0 0 4px rgba(30, 136, 229, 0.12);
}

.home-todo-add {
  flex-shrink: 0;
  min-width: 88px;
}

.home-todo-scroll {
  min-height: 0;
  overflow-y: auto;
  padding-right: 4px;
}

.home-todo-scroll::-webkit-scrollbar {
  width: 8px;
}

.home-todo-scroll::-webkit-scrollbar-thumb {
  background: rgba(120, 155, 191, 0.4);
  border-radius: 999px;
}

.home-todo-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: grid;
  gap: 12px;
}

.home-todo-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  border-radius: 16px;
  background: rgba(247, 251, 255, 0.92);
  border: 1px solid #e6eef7;
}

.home-todo-check {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  border: 1px solid #b7cadf;
  background: #fff;
  color: #fff;
  cursor: pointer;
  flex-shrink: 0;
}

.home-todo-check--done {
  background: #1e88e5;
  border-color: #1e88e5;
}

.home-todo-body {
  min-width: 0;
  flex: 1;
}

.home-todo-text {
  margin: 0 0 4px;
  color: #24384e;
  line-height: 1.5;
  word-break: break-word;
}

.home-todo-text--done {
  color: #7a8b9a;
  text-decoration: line-through;
}

.home-todo-time {
  font-size: 12px;
  color: #7b8b9c;
}

.home-todo-remove {
  border: none;
  background: transparent;
  color: #df5a5a;
  font-size: 13px;
  cursor: pointer;
}

.home-panel__empty {
  margin: 0;
  color: #718194;
  font-size: 14px;
}

@media (max-width: 640px) {
  .home-panel--todo {
    min-height: 320px;
    height: auto;
  }

  .home-todo-create {
    flex-direction: column;
  }
}
</style>
