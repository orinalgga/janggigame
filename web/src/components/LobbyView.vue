<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import type { Formation, Level, Me, Side } from '../api'
import { auth } from '../api'
import { FORMATIONS, LEVELS, PERSONAS, type Mode, type StartOptions } from '../options'

const props = defineProps<{
  me: Me | null
  providers: string[]
  searching: boolean
  error: string
}>()
const emit = defineEmits<{
  (e: 'start', options: StartOptions): void
  (e: 'cancel'): void
}>()

const MODES: { value: Mode; title: string; desc: string; badge: string; tone: string }[] = [
  { value: 'AI', title: 'AI와 두기', desc: '세 가지 캐릭터와 이야기하며 한 판', badge: '漢', tone: 'han' },
  { value: 'ONLINE', title: '온라인 대전', desc: '비슷한 실력의 상대와 실시간 대국', badge: '楚', tone: 'cho' },
  { value: 'HUMAN', title: '같은 화면 2인', desc: '한 기기로 마주 앉아 두기', badge: '將', tone: 'wood' },
]

const SAVED_KEY = 'janggi.lobby'

function load(): Partial<StartOptions> {
  try {
    return JSON.parse(localStorage.getItem(SAVED_KEY) ?? '{}')
  } catch {
    return {}
  }
}

const saved = load()
const mode = ref<Mode>(saved.mode ?? 'AI')
const mySide = ref<Side>(saved.mySide ?? 'CHO')
const level = ref<Level>(saved.level ?? 'INTERMEDIATE')
const choFormation = ref<Formation>(saved.choFormation ?? 'MA_SANG_MA_SANG')
const hanFormation = ref<Formation>(saved.hanFormation ?? 'MA_SANG_MA_SANG')

const options = computed<StartOptions>(() => ({
  mode: mode.value,
  mySide: mySide.value,
  level: level.value,
  choFormation: choFormation.value,
  hanFormation: hanFormation.value,
}))

watch(options, (o) => {
  try {
    localStorage.setItem(SAVED_KEY, JSON.stringify(o))
  } catch {
    // 저장소를 못 쓰면 다음 방문 때 기본값으로 시작한다.
  }
})

/** AI 대국의 상차림은 '내 것'과 '상대 것'으로 보여 주고, 진영에 따라 초·한에 넣는다. */
const myFormation = computed({
  get: () => (mySide.value === 'CHO' ? choFormation.value : hanFormation.value),
  set: (f: Formation) => (mySide.value === 'CHO' ? (choFormation.value = f) : (hanFormation.value = f)),
})
const theirFormation = computed({
  get: () => (mySide.value === 'CHO' ? hanFormation.value : choFormation.value),
  set: (f: Formation) => (mySide.value === 'CHO' ? (hanFormation.value = f) : (choFormation.value = f)),
})

const canLogin = computed(() => props.providers.includes('google'))
const blocked = computed(() => mode.value === 'ONLINE' && !props.me)
const startLabel = computed(() =>
  mode.value === 'ONLINE' ? '상대 찾기' : mode.value === 'AI' ? `${PERSONAS[level.value].name}와 대국 시작` : '대국 시작',
)

function start() {
  if (blocked.value || props.searching) return
  // 온라인 대전은 '내 상차림' 하나만 쓴다. 진영은 서버가 정한다.
  emit('start', mode.value === 'ONLINE' ? { ...options.value, hanFormation: choFormation.value } : options.value)
}
</script>

<template>
  <section class="lobby">
    <div class="hero">
      <div class="seal" aria-hidden="true">將<br />棋</div>
      <div>
        <h1>오늘은 어떤 장기를 둘까요?</h1>
        <p>AI 캐릭터와 이야기하며 두거나, 온라인에서 상대를 찾아 보세요.</p>
      </div>
    </div>

    <div class="modes" role="radiogroup" aria-label="대국 방식">
      <button
        v-for="m in MODES"
        :key="m.value"
        type="button"
        role="radio"
        class="mode"
        :class="{ active: mode === m.value }"
        :aria-checked="mode === m.value"
        :disabled="searching"
        @click="mode = m.value"
      >
        <span class="badge" :class="m.tone">{{ m.badge }}</span>
        <span class="mode-title">{{ m.title }}</span>
        <span class="mode-desc">{{ m.desc }}</span>
      </button>
    </div>

    <div class="setup">
      <template v-if="mode === 'AI'">
        <h3>상대</h3>
        <div class="personas" role="radiogroup" aria-label="AI 난이도">
          <button
            v-for="l in LEVELS"
            :key="l"
            type="button"
            role="radio"
            class="persona"
            :class="{ active: level === l }"
            :aria-checked="level === l"
            @click="level = l"
          >
            <span class="badge small han">{{ PERSONAS[l].badge }}</span>
            <span class="persona-text">
              <span class="persona-name">
                {{ PERSONAS[l].name }} <span class="level">{{ PERSONAS[l].level }}</span>
              </span>
              <span class="persona-desc">{{ PERSONAS[l].desc }}</span>
            </span>
          </button>
        </div>

        <h3>내 진영</h3>
        <div class="segmented" role="radiogroup" aria-label="내 진영">
          <button type="button" role="radio" :aria-checked="mySide === 'CHO'" :class="{ active: mySide === 'CHO' }" @click="mySide = 'CHO'">
            <b class="cho-text">초</b> 먼저 둡니다
          </button>
          <button type="button" role="radio" :aria-checked="mySide === 'HAN'" :class="{ active: mySide === 'HAN' }" @click="mySide = 'HAN'">
            <b class="han-text">한</b> 나중에 둡니다
          </button>
        </div>

        <h3>상차림</h3>
        <div class="formation-row">
          <span class="row-label">나</span>
          <div class="chips">
            <button
              v-for="f in FORMATIONS"
              :key="f.value"
              type="button"
              class="chip"
              :class="{ active: myFormation === f.value }"
              :aria-pressed="myFormation === f.value"
              @click="myFormation = f.value"
            >
              {{ f.label }}<small>{{ f.hanja }}</small>
            </button>
          </div>
        </div>
        <div class="formation-row">
          <span class="row-label">AI</span>
          <div class="chips">
            <button
              v-for="f in FORMATIONS"
              :key="f.value"
              type="button"
              class="chip"
              :class="{ active: theirFormation === f.value }"
              :aria-pressed="theirFormation === f.value"
              @click="theirFormation = f.value"
            >
              {{ f.label }}<small>{{ f.hanja }}</small>
            </button>
          </div>
        </div>
      </template>

      <template v-else-if="mode === 'ONLINE'">
        <div v-if="!me" class="login-box">
          <p>온라인 대전은 로그인한 회원끼리 둡니다. 레이팅이 기록됩니다.</p>
          <a v-if="canLogin" class="primary" :href="auth.loginUrl('google')" @click="auth.openLogin($event, 'google')">구글로 로그인</a>
          <p v-else class="muted">지금은 로그인을 쓸 수 없습니다.</p>
        </div>
        <template v-else>
          <div class="profile">
            <span class="badge small cho">楚</span>
            <span>
              <b>{{ me.nickname }}</b>
              <span class="muted"> · {{ me.grade }} · 레이팅 {{ me.rating }}</span>
            </span>
          </div>
          <p class="muted rule">기본 10분에 한 수마다 10초가 더해집니다. 진영은 무작위로 정해집니다.</p>
          <h3>내 상차림</h3>
          <div class="chips">
            <button
              v-for="f in FORMATIONS"
              :key="f.value"
              type="button"
              class="chip"
              :class="{ active: choFormation === f.value }"
              :aria-pressed="choFormation === f.value"
              :disabled="searching"
              @click="choFormation = f.value"
            >
              {{ f.label }}<small>{{ f.hanja }}</small>
            </button>
          </div>
        </template>
      </template>

      <template v-else>
        <div class="formation-row">
          <span class="row-label cho-text">초</span>
          <div class="chips">
            <button
              v-for="f in FORMATIONS"
              :key="f.value"
              type="button"
              class="chip"
              :class="{ active: choFormation === f.value }"
              :aria-pressed="choFormation === f.value"
              @click="choFormation = f.value"
            >
              {{ f.label }}<small>{{ f.hanja }}</small>
            </button>
          </div>
        </div>
        <div class="formation-row">
          <span class="row-label han-text">한</span>
          <div class="chips">
            <button
              v-for="f in FORMATIONS"
              :key="f.value"
              type="button"
              class="chip"
              :class="{ active: hanFormation === f.value }"
              :aria-pressed="hanFormation === f.value"
              @click="hanFormation = f.value"
            >
              {{ f.label }}<small>{{ f.hanja }}</small>
            </button>
          </div>
        </div>
      </template>

      <div v-if="searching" class="searching" role="status">
        <span class="spinner" aria-hidden="true" />
        <span>상대를 찾는 중…</span>
        <button type="button" class="ghost" @click="emit('cancel')">취소</button>
      </div>
      <button v-else-if="!blocked" type="button" class="primary start" @click="start">{{ startLabel }}</button>
      <p v-if="error" class="error">{{ error }}</p>
    </div>
  </section>
</template>

<style scoped>
.lobby {
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.hero {
  display: flex;
  align-items: center;
  gap: 16px;
}
.hero h1 {
  font-size: 1.45rem;
  margin: 0 0 4px;
  letter-spacing: -0.02em;
}
.hero p {
  margin: 0;
  color: var(--muted);
}
.seal {
  flex: none;
  display: grid;
  place-items: center;
  width: 64px;
  height: 64px;
  border-radius: 10px;
  background: var(--han-color);
  color: #fff6e8;
  font-family: 'Noto Serif KR', serif;
  font-weight: 900;
  font-size: 1.25rem;
  line-height: 1.05;
  text-align: center;
  box-shadow: 0 3px 0 rgba(0, 0, 0, 0.18);
  transform: rotate(-4deg);
}
.modes {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
}
.mode,
.persona,
.chip,
.segmented button {
  font: inherit;
  color: inherit;
  cursor: pointer;
  background: var(--surface);
  border: 1.5px solid var(--border);
  transition:
    border-color 0.15s,
    box-shadow 0.15s,
    transform 0.15s,
    background 0.15s;
}
.mode {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 6px;
  padding: 14px;
  border-radius: 14px;
  text-align: left;
}
.mode:hover:not(:disabled),
.persona:hover,
.chip:hover:not(:disabled) {
  border-color: var(--accent-soft);
  transform: translateY(-1px);
}
.mode.active,
.persona.active {
  border-color: var(--accent);
  background: var(--surface-strong);
  box-shadow: 0 6px 18px var(--shadow);
}
.mode:disabled {
  cursor: default;
  opacity: 0.6;
}
.mode-title {
  font-weight: 700;
}
.mode-desc {
  font-size: 0.82rem;
  color: var(--muted);
  line-height: 1.35;
}
.badge {
  display: grid;
  place-items: center;
  width: 40px;
  height: 40px;
  font-family: 'Noto Serif KR', serif;
  font-weight: 900;
  font-size: 1.15rem;
  background: var(--piece-light);
  clip-path: polygon(30% 0, 70% 0, 100% 30%, 100% 70%, 70% 100%, 30% 100%, 0 70%, 0 30%);
  box-shadow: inset 0 0 0 3px var(--piece-edge);
}
.badge.small {
  width: 34px;
  height: 34px;
  font-size: 1rem;
  flex: none;
}
.badge.han {
  color: var(--han-color);
}
.badge.cho {
  color: var(--cho-color);
}
.badge.wood {
  color: var(--board-line);
  background: var(--board-light);
}
.setup {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 18px;
  border-radius: 16px;
  background: var(--surface-strong);
  border: 1px solid var(--border);
  box-shadow: 0 10px 30px var(--shadow);
}
h3 {
  margin: 6px 0 0;
  font-size: 0.8rem;
  font-weight: 700;
  letter-spacing: 0.06em;
  color: var(--muted);
}
.personas {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
}
.persona {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px;
  border-radius: 12px;
  text-align: left;
}
.persona-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}
.persona-name {
  font-weight: 700;
  font-size: 0.92rem;
}
.level {
  margin-left: 2px;
  padding: 1px 6px;
  border-radius: 999px;
  font-size: 0.7rem;
  font-weight: 600;
  background: var(--accent-bg);
  color: var(--accent);
  white-space: nowrap;
}
.persona-desc {
  font-size: 0.78rem;
  color: var(--muted);
}
.segmented {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 6px;
}
.segmented button {
  padding: 10px;
  border-radius: 10px;
}
.segmented button.active {
  border-color: var(--accent);
  background: var(--accent-bg);
  font-weight: 600;
}
.formation-row {
  display: flex;
  align-items: center;
  gap: 10px;
}
.row-label {
  flex: none;
  width: 24px;
  font-weight: 700;
  font-size: 0.85rem;
  color: var(--muted);
}
.chips {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.chip {
  display: inline-flex;
  align-items: baseline;
  gap: 5px;
  padding: 6px 11px;
  border-radius: 999px;
  font-size: 0.88rem;
}
.chip small {
  font-family: 'Noto Serif KR', serif;
  font-weight: 900;
  font-size: 0.72rem;
  color: var(--muted);
}
.chip.active {
  border-color: var(--accent);
  background: var(--accent);
  color: #fff;
}
.chip.active small {
  color: rgba(255, 255, 255, 0.8);
}
.cho-text {
  color: var(--cho-color);
}
.han-text {
  color: var(--han-color);
}
.primary {
  display: inline-block;
  font: inherit;
  font-weight: 700;
  text-align: center;
  text-decoration: none;
  cursor: pointer;
  color: #fff;
  background: var(--accent);
  border: none;
  border-radius: 12px;
  padding: 12px 18px;
  box-shadow: 0 3px 0 var(--accent-deep);
  transition:
    transform 0.1s,
    box-shadow 0.1s;
}
.primary:active {
  transform: translateY(2px);
  box-shadow: 0 1px 0 var(--accent-deep);
}
.start {
  margin-top: 8px;
  font-size: 1.05rem;
}
.ghost {
  font: inherit;
  background: none;
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 6px 12px;
  cursor: pointer;
}
.login-box {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 10px;
}
.login-box p {
  margin: 0;
}
.profile {
  display: flex;
  align-items: center;
  gap: 10px;
}
.rule {
  margin: 0;
  font-size: 0.85rem;
}
.muted {
  color: var(--muted);
}
.searching {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 8px;
  padding: 12px 14px;
  border-radius: 12px;
  background: var(--accent-bg);
  font-weight: 600;
}
.searching .ghost {
  margin-left: auto;
}
.spinner {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  border: 3px solid var(--accent-soft);
  border-top-color: var(--accent);
  animation: spin 0.8s linear infinite;
}
@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
.error {
  margin: 0;
  color: var(--han-color);
}
@media (max-width: 600px) {
  .modes,
  .personas {
    grid-template-columns: 1fr;
  }
  .mode {
    flex-direction: row;
    align-items: center;
    flex-wrap: wrap;
  }
  .mode-desc {
    flex-basis: 100%;
  }
  .formation-row {
    align-items: flex-start;
  }
  .row-label {
    padding-top: 7px;
  }
}
</style>
