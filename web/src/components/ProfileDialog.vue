<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { api, type Profile } from '../api'
import { MARK, outcome, result } from '../records'
import CharacterAvatar from './CharacterAvatar.vue'

const props = defineProps<{ id: number }>()
const emit = defineEmits<{ (e: 'close'): void }>()

const profile = ref<Profile | null>(null)
const error = ref('')

const total = computed(() => (profile.value ? profile.value.wins + profile.value.losses + profile.value.draws : 0))
/** 승률은 비긴 판을 빼고 센다. */
const winRate = computed(() => {
  const p = profile.value
  if (!p || p.wins + p.losses === 0) return null
  return Math.round((p.wins / (p.wins + p.losses)) * 100)
})

function when(at: string) {
  return new Date(at).toLocaleDateString('ko-KR', { month: 'numeric', day: 'numeric' })
}

const onKey = (e: KeyboardEvent) => e.key === 'Escape' && emit('close')
onMounted(async () => {
  window.addEventListener('keydown', onKey)
  try {
    profile.value = await api.profile(props.id)
  } catch (e) {
    error.value = (e as Error).message
  }
})
onBeforeUnmount(() => window.removeEventListener('keydown', onKey))
</script>

<template>
  <div class="backdrop" @click.self="emit('close')">
    <div class="card" role="dialog" aria-modal="true" aria-labelledby="profile-title">
      <button type="button" class="x" aria-label="닫기" @click="emit('close')">×</button>
      <p v-if="error" class="muted">{{ error }}</p>
      <p v-else-if="!profile" class="muted">불러오는 중…</p>
      <template v-else>
        <div class="head">
          <CharacterAvatar :look="profile.look" class="figure" />
          <div>
            <h2 id="profile-title">{{ profile.nickname }}</h2>
            <p class="grade">{{ profile.grade }} · 레이팅 {{ profile.rating }}</p>
          </div>
        </div>

        <div class="stats">
          <div class="stat win"><b>{{ profile.wins }}</b><span>승</span></div>
          <div class="stat loss"><b>{{ profile.losses }}</b><span>패</span></div>
          <div class="stat draw"><b>{{ profile.draws }}</b><span>무</span></div>
          <div class="stat rate"><b>{{ winRate === null ? '-' : `${winRate}%` }}</b><span>승률</span></div>
        </div>
        <p class="muted small">온라인 대국 {{ total }}판 기준</p>

        <h3>최근 온라인 대국</h3>
        <p v-if="!profile.recent.length" class="muted">아직 둔 온라인 대국이 없어요.</p>
        <ul v-else>
          <li v-for="g in profile.recent" :key="g.id" :class="outcome(g)">
            <span class="mark">{{ outcome(g) ? MARK[outcome(g)!] : '…' }}</span>
            <span class="vs">vs <b>{{ g.opponentNickname ?? '?' }}</b></span>
            <span class="res">{{ result(g) }}</span>
            <span class="date">{{ when(g.startedAt) }}</span>
          </li>
        </ul>
      </template>
    </div>
  </div>
</template>

<style scoped>
.backdrop {
  position: fixed;
  inset: 0;
  z-index: 45;
  display: grid;
  place-items: center;
  padding: 16px;
  background: rgba(0, 0, 0, 0.45);
}
.card {
  position: relative;
  width: min(380px, 100%);
  max-height: calc(100vh - 32px);
  overflow-y: auto;
  padding: 20px 18px 16px;
  border-radius: 20px;
  color: var(--text);
  background: var(--surface-strong);
  box-shadow: 0 20px 50px rgba(0, 0, 0, 0.3);
}
.x {
  position: absolute;
  top: 8px;
  right: 10px;
  font-size: 1.4rem;
  line-height: 1;
  padding: 4px 8px;
  border: none;
  background: none;
  color: var(--muted);
  cursor: pointer;
}
.head {
  display: flex;
  align-items: center;
  gap: 14px;
}
.figure {
  width: 84px;
  border-radius: 14px;
  overflow: hidden;
  box-shadow: 0 0 0 1px var(--border);
}
h2 {
  margin: 0;
  font-size: 1.2rem;
}
.grade {
  margin: 4px 0 0;
  color: var(--muted);
  font-size: 0.88rem;
}
.stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 6px;
  margin-top: 16px;
}
.stat {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 8px 0;
  border-radius: 12px;
  background: var(--surface);
  border: 1px solid var(--border);
}
.stat b {
  font-size: 1.25rem;
}
.stat span {
  font-size: 0.75rem;
  color: var(--muted);
}
.stat.win b {
  color: #2f6fd6;
}
.stat.loss b {
  color: #d64545;
}
.stat.draw b {
  color: #8a8a8a;
}
h3 {
  margin: 16px 0 6px;
  font-size: 0.8rem;
  letter-spacing: 0.06em;
  color: var(--muted);
}
ul {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
li {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 8px;
  border-radius: 10px;
  background: var(--surface);
  font-size: 0.86rem;
}
.mark {
  flex: none;
  display: grid;
  place-items: center;
  width: 24px;
  height: 24px;
  border-radius: 7px;
  font-weight: 800;
  font-size: 0.8rem;
  color: #fff;
  background: var(--muted);
}
.win .mark {
  background: #2f6fd6;
}
.loss .mark {
  background: #d64545;
}
.draw .mark {
  background: #8a8a8a;
}
.vs {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}
.res {
  flex: none;
  font-weight: 700;
}
.win .res {
  color: #2f6fd6;
}
.loss .res {
  color: #d64545;
}
.date {
  flex: none;
  font-size: 0.75rem;
  color: var(--muted);
}
.muted {
  margin: 0;
  color: var(--muted);
  font-size: 0.88rem;
}
.small {
  margin-top: 6px;
  font-size: 0.75rem;
  text-align: right;
}
</style>
