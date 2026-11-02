<script setup lang="ts">
import { computed, onBeforeUnmount, ref } from 'vue'
import { auth, type Formation, type Invite, type Me } from '../api'
import { FORMATIONS } from '../options'

const props = defineProps<{
  invite: Invite
  me: Me | null
  providers: string[]
  busy: boolean
  error: string
}>()

const emit = defineEmits<{
  (e: 'accept', formation: Formation): void
  (e: 'decline'): void
}>()

function savedFormation(): Formation {
  try {
    return JSON.parse(localStorage.getItem('janggi.lobby') ?? '{}').choFormation ?? 'MA_SANG_MA_SANG'
  } catch {
    return 'MA_SANG_MA_SANG'
  }
}

const formation = ref<Formation>(savedFormation())
const now = ref(Date.now())
const timer = setInterval(() => (now.value = Date.now()), 1000)
onBeforeUnmount(() => clearInterval(timer))

const secondsLeft = computed(() => Math.max(0, Math.ceil((props.invite.expiresAt - now.value) / 1000)))
/** 링크 초대는 30분이라 남은 시간을 보여 주지 않는다. */
const direct = computed(() => props.invite.toId !== null)
const expired = computed(() => secondsLeft.value === 0)
const mine = computed(() => props.me?.id === props.invite.fromId)
</script>

<template>
  <div class="backdrop">
    <div class="dialog" role="dialog" aria-modal="true" aria-labelledby="invite-title">
      <span class="seal" aria-hidden="true">請</span>
      <h2 id="invite-title">대국 초대</h2>
      <p class="lead">
        <b>{{ invite.fromNickname }}</b> <span class="grade">{{ invite.fromGrade }}</span> 님이 한 판 두자고 해요.
      </p>
      <p class="rule">기본 10분, 한 수마다 10초. 진영은 무작위로 정해지고 레이팅이 기록돼요.</p>

      <template v-if="expired">
        <p class="note">시간이 지난 초대예요.</p>
        <div class="actions">
          <button type="button" class="primary" @click="emit('decline')">닫기</button>
        </div>
      </template>
      <template v-else-if="!me">
        <p class="note">로그인하면 바로 받을 수 있어요.</p>
        <div class="actions">
          <button type="button" class="ghost" @click="emit('decline')">닫기</button>
          <a v-if="providers.includes('google')" class="primary" :href="auth.loginUrl('google')" @click="auth.openLogin($event, 'google')">구글로 로그인</a>
        </div>
      </template>
      <template v-else-if="mine">
        <p class="note">내가 보낸 초대 링크예요. 상대가 열어서 받으면 대국이 시작돼요.</p>
        <div class="actions">
          <button type="button" class="primary" @click="emit('decline')">닫기</button>
        </div>
      </template>
      <template v-else>
        <h3>내 상차림</h3>
        <div class="chips">
          <button
            v-for="f in FORMATIONS"
            :key="f.value"
            type="button"
            class="chip"
            :class="{ active: formation === f.value }"
            :aria-pressed="formation === f.value"
            :disabled="busy"
            @click="formation = f.value"
          >
            {{ f.label }}<small>{{ f.hanja }}</small>
          </button>
        </div>
        <p v-if="error" class="error">{{ error }}</p>
        <div class="actions">
          <span v-if="direct" class="left" aria-live="off">{{ secondsLeft }}초</span>
          <button type="button" class="ghost" :disabled="busy" @click="emit('decline')">거절</button>
          <button type="button" class="primary" :disabled="busy" @click="emit('accept', formation)">
            {{ busy ? '대국 여는 중…' : '수락' }}
          </button>
        </div>
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
  background: rgba(30, 20, 10, 0.45);
}
.dialog {
  position: relative;
  box-sizing: border-box;
  width: min(100%, 400px);
  padding: 22px 20px 18px;
  border-radius: 16px;
  background: var(--surface-strong);
  box-shadow: 0 20px 50px rgba(0, 0, 0, 0.25);
  animation: pop 0.25s ease-out;
}
@keyframes pop {
  from {
    transform: scale(0.92);
    opacity: 0;
  }
}
.seal {
  position: absolute;
  top: 16px;
  right: 18px;
  display: grid;
  place-items: center;
  width: 38px;
  height: 38px;
  border-radius: 8px;
  font-family: 'Noto Serif KR', serif;
  font-weight: 900;
  font-size: 1.2rem;
  color: #fff;
  background: var(--han-color);
  transform: rotate(-8deg);
}
h2 {
  margin: 0 0 8px;
  font-size: 1.2rem;
}
h3 {
  margin: 14px 0 8px;
  font-size: 0.9rem;
}
.lead {
  margin: 0 0 6px;
  padding-right: 44px;
  line-height: 1.5;
}
.grade {
  font-size: 0.85rem;
  color: var(--muted);
}
.rule,
.note {
  margin: 0;
  font-size: 0.85rem;
  color: var(--muted);
}
.note {
  margin-top: 10px;
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
  font: inherit;
  font-size: 0.88rem;
  color: inherit;
  cursor: pointer;
  background: var(--surface);
  border: 1.5px solid var(--border);
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
.error {
  margin: 10px 0 0;
  font-size: 0.85rem;
  color: var(--han-color);
}
.actions {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 8px;
  margin-top: 16px;
}
.left {
  margin-right: auto;
  font-variant-numeric: tabular-nums;
  font-weight: 700;
  color: var(--muted);
}
.ghost,
.primary {
  font: inherit;
  font-weight: 700;
  padding: 9px 16px;
  border-radius: 10px;
  cursor: pointer;
  text-decoration: none;
}
.ghost {
  color: inherit;
  background: none;
  border: 1px solid var(--border);
}
.primary {
  color: #fff;
  background: var(--accent);
  border: none;
  box-shadow: 0 3px 0 var(--accent-deep);
}
button:disabled {
  opacity: 0.55;
  cursor: default;
}
</style>
