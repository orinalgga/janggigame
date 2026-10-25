<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { api, type Notice } from '../api'

defineProps<{
  /** 대국 중에는 띄우지 않고 기다린다. */
  paused: boolean
}>()

const HIDDEN_KEY = 'janggi.notices.hidden'

const notices = ref<Notice[]>([])
/** 이번 방문에서 닫은 공지. */
const closed = ref(new Set<string>())

// 공지를 고치면 updatedAt이 바뀌므로 '오늘 하루 보지 않기'를 했어도 다시 뜬다.
const keyOf = (n: Notice) => `${n.id}:${n.updatedAt}`

/** '오늘 하루 보지 않기'한 공지와 그 효력이 끝나는 때(ms). */
function hidden(): Record<string, number> {
  try {
    const all = JSON.parse(localStorage.getItem(HIDDEN_KEY) ?? '{}') as Record<string, number>
    return Object.fromEntries(Object.entries(all).filter(([, until]) => until > Date.now()))
  } catch {
    return {}
  }
}

onMounted(async () => {
  const list = await api.notices().catch(() => [])
  const h = hidden()
  notices.value = list.filter((n) => !h[keyOf(n)])
})

const current = computed(() => notices.value.find((n) => !closed.value.has(keyOf(n))) ?? null)
const left = computed(() => notices.value.filter((n) => !closed.value.has(keyOf(n))).length)

function close() {
  if (current.value) closed.value = new Set(closed.value).add(keyOf(current.value))
}

function hideToday() {
  const n = current.value
  if (!n) return
  const midnight = new Date()
  midnight.setHours(24, 0, 0, 0)
  try {
    localStorage.setItem(HIDDEN_KEY, JSON.stringify({ ...hidden(), [keyOf(n)]: midnight.getTime() }))
  } catch {
    // 저장소를 못 쓰면 이번 방문에서만 닫힌다.
  }
  close()
}

const date = (s: string) => new Date(s).toLocaleDateString('ko-KR', { month: 'long', day: 'numeric' })
</script>

<template>
  <div v-if="current && !paused" class="notice-backdrop" @click.self="close">
    <article class="notice-dialog" role="dialog" aria-modal="true" aria-labelledby="notice-title">
      <header>
        <span class="tag">공지</span>
        <span v-if="left > 1" class="more">{{ left }}개 중 1</span>
        <time :datetime="current.startsAt">{{ date(current.startsAt) }}</time>
      </header>
      <h2 id="notice-title">{{ current.title }}</h2>
      <p class="body">{{ current.body }}</p>
      <div class="actions">
        <button type="button" class="ghost" @click="hideToday">오늘 하루 보지 않기</button>
        <button type="button" class="primary" @click="close">{{ left > 1 ? '다음' : '닫기' }}</button>
      </div>
    </article>
  </div>
</template>

<style scoped>
.notice-backdrop {
  position: fixed;
  inset: 0;
  z-index: 45;
  display: grid;
  place-items: center;
  padding: 16px;
  background: rgba(30, 20, 10, 0.45);
}
.notice-dialog {
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  width: min(100%, 420px);
  max-height: min(80vh, 560px);
  padding: 20px 20px 16px;
  border-radius: 16px;
  background: var(--surface-strong);
  box-shadow: 0 20px 50px rgba(0, 0, 0, 0.25);
}
header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 0.8rem;
  color: var(--muted);
}
.tag {
  padding: 2px 9px;
  border-radius: 999px;
  font-weight: 700;
  color: #fff;
  background: var(--accent);
}
time {
  margin-left: auto;
}
h2 {
  margin: 12px 0 8px;
  font-size: 1.2rem;
  line-height: 1.35;
  overflow-wrap: anywhere;
}
.body {
  flex: 1;
  min-height: 0;
  margin: 0;
  overflow-y: auto;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
  line-height: 1.6;
  font-size: 0.95rem;
}
.actions {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  margin-top: 16px;
}
.ghost,
.primary {
  font: inherit;
  font-size: 0.88rem;
  font-weight: 700;
  padding: 9px 14px;
  border-radius: 10px;
  cursor: pointer;
}
.ghost {
  color: var(--muted);
  background: none;
  border: 1px solid var(--border);
}
.primary {
  min-width: 80px;
  color: #fff;
  background: var(--accent);
  border: none;
  box-shadow: 0 3px 0 var(--accent-deep);
}
</style>
