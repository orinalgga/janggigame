<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { api, type OnlineUser } from '../api'
import CharacterAvatar from './CharacterAvatar.vue'
import { openProfile } from '../profile'

const props = defineProps<{
  myId: number
  /** 상대를 찾거나 초대 답을 기다리는 중이면 부르기를 막는다. */
  busy: boolean
}>()
const emit = defineEmits<{ (e: 'invite', nickname: string): void }>()

/** 접속한 사람은 자주 바뀌니 화면이 보이는 동안 이 간격으로 다시 받는다. */
const REFRESH_MS = 15_000
const FOLDED = 6

const users = ref<OnlineUser[]>([])
const loaded = ref(false)
const failed = ref(false)
const expanded = ref(false)
let timer: ReturnType<typeof setInterval> | undefined

const others = computed(() => users.value.filter((u) => u.id !== props.myId))
const shown = computed(() => (expanded.value ? others.value : others.value.slice(0, FOLDED)))

async function load() {
  if (document.hidden) return
  try {
    users.value = (await api.online()).users
    failed.value = false
  } catch {
    failed.value = true
  } finally {
    loaded.value = true
  }
}

const onVisible = () => !document.hidden && void load()
onMounted(() => {
  void load()
  timer = setInterval(load, REFRESH_MS)
  document.addEventListener('visibilitychange', onVisible)
})
onBeforeUnmount(() => {
  clearInterval(timer)
  document.removeEventListener('visibilitychange', onVisible)
})
</script>

<template>
  <section class="online" aria-label="지금 접속 중인 사람">
    <h3>
      지금 접속 중 <span class="count">{{ others.length }}명</span>
      <button type="button" class="refresh" aria-label="새로고침" @click="load">↻</button>
    </h3>
    <p v-if="!loaded" class="muted">불러오는 중…</p>
    <p v-else-if="failed && !others.length" class="muted">목록을 불러오지 못했어요.</p>
    <p v-else-if="!others.length" class="muted">지금은 나 말고 접속한 사람이 없어요. 링크로 친구를 불러 보세요.</p>
    <ul v-else>
      <li v-for="u in shown" :key="u.id">
        <button type="button" class="open" :aria-label="`${u.nickname} 전적 보기`" @click="openProfile(u.id)">
          <CharacterAvatar :look="u.look" head class="face" />
          <span class="who">
            <b>{{ u.nickname }}</b>
            <small>{{ u.grade }} · {{ u.rating }}</small>
          </span>
        </button>
        <span v-if="u.playing" class="tag">대국 중</span>
        <button v-else type="button" class="call" :disabled="busy" @click="emit('invite', u.nickname)">부르기</button>
      </li>
    </ul>
    <button v-if="others.length > FOLDED" type="button" class="more" @click="expanded = !expanded">
      {{ expanded ? '접기' : `${others.length - FOLDED}명 더 보기` }}
    </button>
  </section>
</template>

<style scoped>
.online {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
h3 {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 6px 0 0;
  font-size: 0.8rem;
  font-weight: 700;
  letter-spacing: 0.06em;
  color: var(--muted);
}
.count {
  color: var(--cho-color);
}
.count::before {
  content: '';
  display: inline-block;
  width: 7px;
  height: 7px;
  margin-right: 4px;
  border-radius: 50%;
  background: #3cb371;
  vertical-align: 1px;
}
.refresh {
  margin-left: auto;
  font: inherit;
  padding: 0 6px;
  border: none;
  background: none;
  color: var(--muted);
  cursor: pointer;
}
ul {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 6px;
}
li {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
  padding: 6px 10px 6px 6px;
  border-radius: 12px;
  border: 1px solid var(--border);
  background: var(--surface);
}
.open {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0;
  font: inherit;
  text-align: left;
  color: inherit;
  border: none;
  background: none;
  cursor: pointer;
}
.open:hover b {
  text-decoration: underline;
}
.face {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  overflow: hidden;
  box-shadow: 0 0 0 1px var(--border);
}
.who {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  line-height: 1.25;
}
.who b {
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
  font-size: 0.9rem;
}
.who small {
  font-size: 0.74rem;
  color: var(--muted);
}
.tag {
  flex: none;
  font-size: 0.74rem;
  padding: 3px 8px;
  border-radius: 999px;
  color: var(--muted);
  background: var(--surface-strong);
  border: 1px solid var(--border);
}
.call {
  flex: none;
  font: inherit;
  font-size: 0.8rem;
  font-weight: 700;
  padding: 5px 10px;
  border-radius: 999px;
  cursor: pointer;
  color: var(--accent-deep);
  background: var(--accent-bg);
  border: 1.5px solid var(--accent-soft);
}
.call:disabled {
  opacity: 0.55;
  cursor: default;
}
.more {
  align-self: flex-start;
  font: inherit;
  font-size: 0.8rem;
  padding: 2px 4px;
  border: none;
  background: none;
  color: var(--accent);
  cursor: pointer;
}
.muted {
  margin: 0;
  font-size: 0.85rem;
  color: var(--muted);
}
</style>
