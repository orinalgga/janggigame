<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { api, type ChatMessage, type Side } from '../api'
import type { Live } from '../live'

const props = defineProps<{
  gameId: string
  /** ai면 AI 캐릭터와, online이면 상대 회원과 이야기한다. */
  kind: 'ai' | 'online'
  mySide: Side
  /** 머리에 보일 상대 이름. */
  title: string
  live: Live | null
}>()

const MAX = 200
const QUICK = {
  ai: ['안녕하세요!', '잘 부탁해요', '살살 봐주세요', '좋은 수네요'],
  online: ['잘 부탁드립니다', '좋은 수네요', '잠시만요', '수고하셨습니다'],
}

const messages = ref<ChatMessage[]>([])
const draft = ref('')
const sending = ref(false)
const error = ref('')
const list = ref<HTMLElement | null>(null)
/** 서버 답을 기다리는 동안 먼저 보여 주는 내 말. */
const pending = ref<string | null>(null)
let stop: (() => void) | null = null

// 좁은 화면에서는 판 아래로 밀려 안 보이지 않도록 화면 아래에 붙은 채팅 바가 되고, 누르면 펼쳐진다.
const narrowQuery = window.matchMedia?.('(max-width: 860px)')
const narrow = ref(narrowQuery?.matches ?? false)
const onNarrow = (e: MediaQueryListEvent) => (narrow.value = e.matches)
narrowQuery?.addEventListener('change', onNarrow)
const open = ref(false)
const unread = ref(0)
const bump = ref(0)
const latest = computed(() => messages.value[messages.value.length - 1])

function toggle() {
  if (!narrow.value) return
  open.value = !open.value
  if (open.value) {
    unread.value = 0
    void scrollDown()
  }
}

const length = computed(() => [...draft.value].length)

function add(m: ChatMessage) {
  if (messages.value.some((x) => x.id === m.id)) return
  messages.value = [...messages.value, m].sort((a, b) => a.id - b.id)
  const mine = m.side === props.mySide && !m.ai
  if (narrow.value && !open.value && !mine) {
    unread.value++
    bump.value++
  }
  void scrollDown()
}

async function scrollDown() {
  await nextTick()
  list.value?.scrollTo({ top: list.value.scrollHeight, behavior: 'smooth' })
}

onMounted(async () => {
  if (props.kind === 'online' && props.live) {
    stop = props.live.subscribe<ChatMessage>(`/topic/games/${props.gameId}/chat`, add)
  }
  const history = await api.chat(props.gameId).catch(() => [])
  history.forEach(add)
})
onBeforeUnmount(() => {
  stop?.()
  narrowQuery?.removeEventListener('change', onNarrow)
})

async function send(text = draft.value) {
  const t = text.trim()
  if (!t || sending.value || [...t].length > MAX) return
  sending.value = true
  pending.value = t
  draft.value = ''
  error.value = ''
  void scrollDown()
  try {
    ;(await api.say(props.gameId, t)).forEach(add)
  } catch (e) {
    error.value = (e as Error).message
    draft.value = t
  } finally {
    pending.value = null
    sending.value = false
  }
}

function time(at: number) {
  return new Date(at).toLocaleTimeString('ko-KR', { hour: 'numeric', minute: '2-digit' })
}

defineExpose({ push: add })
</script>

<template>
  <aside class="chat" :class="{ sheet: narrow, open: open || !narrow }" aria-label="채팅">
    <header
      :key="bump"
      :class="{ bump: bump > 0 && narrow && !open }"
      :role="narrow ? 'button' : undefined"
      :tabindex="narrow ? 0 : undefined"
      :aria-expanded="narrow ? open : undefined"
      @click="toggle"
      @keydown.enter="toggle"
    >
      <span class="dot" :class="kind" aria-hidden="true" />
      <b>{{ title }}</b>
      <span v-if="narrow && !open && latest" class="preview">{{ latest.text }}</span>
      <span v-else class="muted">{{ kind === 'ai' ? 'AI와 대화' : '상대와 대화' }}</span>
      <span v-if="unread" class="unread">{{ unread }}</span>
      <span v-if="narrow" class="chevron" aria-hidden="true">{{ open ? '▾' : '▴' }}</span>
    </header>

    <div ref="list" class="messages" aria-live="polite">
      <p v-if="!messages.length && !pending" class="empty">
        {{ kind === 'ai' ? '먼저 말을 걸어 보세요.' : '상대에게 인사를 건네 보세요.' }}
      </p>
      <div
        v-for="m in messages"
        :key="m.id"
        class="msg"
        :class="[m.side === mySide && !m.ai ? 'mine' : 'theirs', m.side.toLowerCase()]"
      >
        <span v-if="m.side !== mySide || m.ai" class="who">
          {{ m.nickname }}<span v-if="m.ai" class="ai-tag">AI</span>
        </span>
        <span class="bubble">{{ m.text }}</span>
        <span class="at">{{ time(m.at) }}</span>
      </div>
      <div v-if="pending" class="msg mine sending" :class="mySide.toLowerCase()">
        <span class="bubble">{{ pending }}</span>
      </div>
      <div v-if="sending && kind === 'ai'" class="msg theirs typing">
        <span class="bubble"><i /><i /><i /></span>
      </div>
    </div>

    <div class="quick">
      <button v-for="q in QUICK[kind]" :key="q" type="button" :disabled="sending" @click="send(q)">{{ q }}</button>
    </div>
    <form class="compose" @submit.prevent="send()">
      <input
        v-model="draft"
        :maxlength="MAX"
        :placeholder="kind === 'ai' ? `${title}에게 말 걸기` : '메시지 보내기'"
        aria-label="채팅 입력"
        autocomplete="off"
      />
      <button type="submit" :disabled="sending || !draft.trim()">보내기</button>
    </form>
    <p v-if="error" class="error">{{ error }}</p>
    <p v-else-if="length > MAX - 30" class="count">{{ length }} / {{ MAX }}</p>
  </aside>
</template>

<style scoped>
.chat {
  display: flex;
  flex-direction: column;
  min-height: 0;
  border-radius: 16px;
  background: var(--surface-strong);
  border: 1px solid var(--border);
  box-shadow: 0 10px 30px var(--shadow);
  overflow: hidden;
}
header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 14px;
  border-bottom: 1px solid var(--border);
}
header .muted {
  margin-left: auto;
  font-size: 0.78rem;
}
.preview {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
  font-size: 0.85rem;
  color: var(--muted);
}
.unread {
  flex: none;
  min-width: 20px;
  padding: 1px 6px;
  border-radius: 999px;
  text-align: center;
  font-size: 0.75rem;
  font-weight: 700;
  color: #fff;
  background: var(--han-color);
}
.chevron {
  flex: none;
  color: var(--muted);
}
.sheet {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 20;
  max-height: 72vh;
  border-radius: 16px 16px 0 0;
  padding-bottom: env(safe-area-inset-bottom);
  box-shadow: 0 -8px 30px var(--shadow);
}
.sheet header {
  cursor: pointer;
  padding: 14px 16px;
}
.sheet:not(.open) header {
  border-bottom: none;
}
.sheet:not(.open) .messages,
.sheet:not(.open) .quick,
.sheet:not(.open) .compose,
.sheet:not(.open) .error,
.sheet:not(.open) .count {
  display: none;
}
.sheet .messages {
  max-height: none;
  min-height: 160px;
}
.bump {
  animation: bump 0.45s ease-out;
}
@keyframes bump {
  30% {
    background: var(--accent-bg);
    transform: translateY(-4px);
  }
}
.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--cho-color);
}
.dot.ai {
  background: var(--han-color);
}
.messages {
  flex: 1;
  min-height: 220px;
  max-height: 420px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 14px;
  background: var(--surface);
}
.empty {
  margin: auto;
  color: var(--muted);
  font-size: 0.88rem;
}
.msg {
  display: flex;
  flex-direction: column;
  max-width: 82%;
  gap: 3px;
  animation: rise 0.2s ease-out both;
}
.msg.mine {
  align-self: flex-end;
  align-items: flex-end;
}
.who {
  font-size: 0.74rem;
  font-weight: 600;
  color: var(--muted);
}
.ai-tag {
  margin-left: 4px;
  padding: 0 5px;
  border-radius: 4px;
  font-size: 0.65rem;
  background: var(--accent-bg);
  color: var(--accent);
}
.bubble {
  padding: 8px 12px;
  border-radius: 14px;
  line-height: 1.4;
  font-size: 0.92rem;
  white-space: pre-wrap;
  word-break: break-word;
  background: var(--surface-strong);
  border: 1px solid var(--border);
}
.theirs .bubble {
  border-top-left-radius: 4px;
}
.mine .bubble {
  border: none;
  border-top-right-radius: 4px;
  color: #fff;
  background: var(--cho-color);
}
.mine.han .bubble {
  background: var(--han-color);
}
.sending .bubble {
  opacity: 0.6;
}
.at {
  font-size: 0.68rem;
  color: var(--muted);
}
.typing .bubble {
  display: inline-flex;
  gap: 4px;
  padding: 11px 12px;
}
.typing i {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--muted);
  animation: blink 1s infinite;
}
.typing i:nth-child(2) {
  animation-delay: 0.15s;
}
.typing i:nth-child(3) {
  animation-delay: 0.3s;
}
.quick {
  display: flex;
  gap: 6px;
  padding: 10px 12px 0;
  overflow-x: auto;
}
.quick button {
  flex: none;
  font: inherit;
  font-size: 0.8rem;
  padding: 5px 10px;
  border-radius: 999px;
  border: 1px solid var(--border);
  background: var(--surface);
  cursor: pointer;
}
.compose {
  display: flex;
  gap: 8px;
  padding: 10px 12px 12px;
}
.compose input {
  flex: 1;
  min-width: 0;
  font: inherit;
  padding: 9px 12px;
  border-radius: 10px;
  border: 1px solid var(--border);
  background: var(--surface);
}
.compose input:focus {
  outline: 2px solid var(--accent-soft);
  border-color: var(--accent);
}
.compose button {
  font: inherit;
  font-weight: 700;
  padding: 0 14px;
  border: none;
  border-radius: 10px;
  color: #fff;
  background: var(--accent);
  cursor: pointer;
}
.compose button:disabled,
.quick button:disabled {
  opacity: 0.5;
  cursor: default;
}
.error,
.count {
  margin: 0;
  padding: 0 14px 10px;
  font-size: 0.8rem;
}
.error {
  color: var(--han-color);
}
.count {
  color: var(--muted);
  text-align: right;
}
.muted {
  color: var(--muted);
}
@keyframes rise {
  from {
    opacity: 0;
    transform: translateY(6px);
  }
}
@keyframes blink {
  50% {
    opacity: 0.25;
  }
}
@media (prefers-reduced-motion: reduce) {
  .bump,
  .msg,
  .typing i {
    animation: none;
  }
}
</style>
