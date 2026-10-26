<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import type { MegaphoneEvent, MegaphonePost } from '../api'
import { Live } from '../live'
import CharacterAvatar from './CharacterAvatar.vue'

/** 확성기 띠. 누가 확성기를 쓰면 화면 맨 위로 한 줄씩 차례로 지나간다. 누르면 확성기 창을 연다. */
const emit = defineEmits<{ (e: 'open'): void }>()

const SHOW_MS = 6500
/** 줄이 밀리면 한 줄을 이만큼만 보여 준다. */
const BUSY_MS = 3500

const current = ref<MegaphonePost | null>(null)
const queue: MegaphonePost[] = []
let timer: ReturnType<typeof setTimeout> | undefined
let live: Live | null = null
let stop: (() => void) | null = null

function next() {
  clearTimeout(timer)
  current.value = queue.shift() ?? null
  if (current.value) timer = setTimeout(next, queue.length > 2 ? BUSY_MS : SHOW_MS)
}

function onEvent(e: MegaphoneEvent) {
  if (e.post) {
    queue.push(e.post)
    if (!current.value) next()
  }
  if (e.hidden != null) {
    const i = queue.findIndex((p) => p.id === e.hidden)
    if (i >= 0) queue.splice(i, 1)
    if (current.value?.id === e.hidden) next()
  }
}

onMounted(() => {
  live = new Live()
  stop = live.subscribe<MegaphoneEvent>('/topic/megaphone', onEvent)
})

onBeforeUnmount(() => {
  clearTimeout(timer)
  stop?.()
  live?.close()
})
</script>

<template>
  <div class="bar-slot" aria-live="polite">
    <Transition name="shout">
      <div v-if="current" :key="current.id" class="bar">
        <button type="button" class="body" title="확성기 열기" @click="emit('open')">
          <span class="horn" aria-hidden="true">📢</span>
          <CharacterAvatar :look="current.look" head class="face" />
          <b class="who">{{ current.nickname }}</b>
          <span class="text">{{ current.text }}</span>
        </button>
        <button type="button" class="close" aria-label="이 확성기 닫기" @click="next">×</button>
      </div>
    </Transition>
  </div>
</template>

<style scoped>
.bar-slot {
  position: fixed;
  top: calc(env(safe-area-inset-top) + 8px);
  left: 0;
  right: 0;
  z-index: 35;
  display: flex;
  justify-content: center;
  padding: 0 12px;
  pointer-events: none;
}
.bar {
  box-sizing: border-box;
  display: flex;
  align-items: center;
  width: min(100%, 680px);
  border-radius: 18px;
  background: linear-gradient(90deg, #fff3c4, #ffe7a3);
  border: 1.5px solid #e2b23c;
  box-shadow: 0 6px 18px rgba(120, 80, 10, 0.25);
  pointer-events: auto;
}
.body {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 4px 6px 12px;
  border: none;
  background: none;
  color: var(--text);
  text-align: left;
  cursor: pointer;
}
.horn {
  flex: none;
  font-size: 1.1rem;
}
.face {
  width: 28px;
}
.who {
  flex: none;
  max-width: 30%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--accent-deep);
}
.text {
  min-width: 0;
  overflow: hidden;
  overflow-wrap: anywhere;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  line-height: 1.35;
}
.close {
  flex: none;
  width: 32px;
  height: 32px;
  margin-right: 4px;
  border: none;
  border-radius: 50%;
  background: none;
  color: var(--muted);
  font-size: 1.2rem;
  cursor: pointer;
}
.shout-enter-active,
.shout-leave-active {
  transition:
    transform 0.35s ease,
    opacity 0.35s ease;
}
.shout-enter-from {
  transform: translateY(-120%);
  opacity: 0;
}
.shout-leave-to {
  transform: translateY(-60%);
  opacity: 0;
}
.shout-leave-active {
  position: absolute;
}
@media (prefers-reduced-motion: reduce) {
  .shout-enter-active,
  .shout-leave-active {
    transition: opacity 0.2s;
  }
  .shout-enter-from,
  .shout-leave-to {
    transform: none;
  }
}
</style>
