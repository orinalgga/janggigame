<script setup lang="ts">
import { computed, ref } from 'vue'
import { api, type Me } from '../api'
import { emptyInput, fromLocal, localIn } from '../tournaments'

defineProps<{ me: Me }>()
const emit = defineEmits<{ (e: 'created', id: number): void; (e: 'close'): void }>()

const PRESETS = [
  { label: '1분+10초', base: 60, inc: 10 },
  { label: '3분+5초', base: 180, inc: 5 },
  { label: '5분+5초', base: 300, inc: 5 },
]

const form = ref({ ...emptyInput(), maxPlayers: 8, startsAt: localIn(30) })
const preset = ref(0)
const saving = ref(false)
const error = ref('')

const canSave = computed(() => !saving.value && form.value.title.trim() !== '' && form.value.startsAt !== '')

async function save() {
  if (!canSave.value) return
  saving.value = true
  error.value = ''
  const p = PRESETS[preset.value]
  try {
    const d = await api.hostTournament({
      ...form.value,
      title: form.value.title.trim(),
      description: form.value.description?.trim() || null,
      startsAt: fromLocal(form.value.startsAt),
      baseSeconds: p.base,
      incrementSeconds: p.inc,
    })
    emit('created', d.summary.id)
  } catch (e) {
    error.value = (e as Error).message
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div class="host-backdrop" @click.self="emit('close')">
    <form class="host-dialog" role="dialog" aria-modal="true" aria-labelledby="host-title" @submit.prevent="save">
      <h2 id="host-title">대회 열기</h2>
      <p class="lead">내가 여는 특별 리그예요. 정해진 시각에 토너먼트로 시작하고, 우승자는 내가 건 상금을 받아요. 참가비는 참가 비용으로 쓰여 없어져요.</p>
      <label>
        <span>대회 이름</span>
        <input v-model="form.title" maxlength="60" placeholder="예: 동네 고수 대결" required />
      </label>
      <label>
        <span>소개 (선택)</span>
        <textarea v-model="form.description" maxlength="1000" rows="2" placeholder="참가자에게 하고 싶은 말" />
      </label>
      <div class="row">
        <label>
          <span>시작</span>
          <input v-model="form.startsAt" type="datetime-local" required />
        </label>
        <label>
          <span>시간</span>
          <select v-model.number="preset">
            <option v-for="(p, i) in PRESETS" :key="p.label" :value="i">{{ p.label }}</option>
          </select>
        </label>
      </div>
      <div class="row three">
        <label>
          <span>정원</span>
          <select v-model.number="form.maxPlayers">
            <option :value="4">4명</option>
            <option :value="8">8명</option>
            <option :value="16">16명</option>
          </select>
        </label>
        <label>
          <span>참가비(닢)</span>
          <input v-model.number="form.entryFee" type="number" min="0" max="10" />
        </label>
        <label>
          <span>건 상금(닢)</span>
          <input v-model.number="form.bonusPrize" type="number" min="0" max="1000" />
        </label>
      </div>
      <p class="help">건 상금은 대회를 열 때 내 엽전에서 빠지고, 대회가 취소되면 돌려받아요. 7일 안에 시작하는 대회만 열 수 있고, 한 번에 하나만 열 수 있어요.</p>
      <p v-if="error" class="error" role="alert">{{ error }}</p>
      <div class="actions">
        <button type="button" class="ghost" :disabled="saving" @click="emit('close')">취소</button>
        <button type="submit" class="primary" :disabled="!canSave">{{ saving ? '여는 중…' : '대회 열기' }}</button>
      </div>
    </form>
  </div>
</template>

<style scoped>
.host-backdrop {
  position: fixed;
  inset: 0;
  z-index: 40;
  display: grid;
  place-items: center;
  padding: 16px;
  background: rgba(30, 20, 10, 0.45);
  overflow-y: auto;
}
.host-dialog {
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  gap: 10px;
  width: min(100%, 440px);
  padding: 20px;
  border-radius: 16px;
  background: var(--surface-strong);
  box-shadow: 0 20px 50px rgba(0, 0, 0, 0.25);
}
h2 {
  margin: 0;
  font-size: 1.2rem;
}
.lead,
.help {
  margin: 0;
  font-size: 0.84rem;
  color: var(--muted);
  line-height: 1.5;
}
label {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
  font-size: 0.8rem;
  color: var(--muted);
}
input,
select,
textarea {
  box-sizing: border-box;
  width: 100%;
  min-width: 0;
  font: inherit;
  font-size: 0.95rem;
  color: var(--text);
  padding: 9px 11px;
  border-radius: 10px;
  border: 1px solid var(--border);
  background: var(--surface);
}
textarea {
  resize: vertical;
}
.row {
  display: grid;
  grid-template-columns: 1.4fr 1fr;
  gap: 8px;
}
.row.three {
  grid-template-columns: repeat(3, 1fr);
}
.error {
  margin: 0;
  color: var(--han-color);
}
.actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
.ghost,
.primary {
  font: inherit;
  font-weight: 700;
  padding: 9px 16px;
  border-radius: 10px;
  cursor: pointer;
}
.ghost {
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
@media (max-width: 420px) {
  .row {
    grid-template-columns: 1fr;
  }
}
</style>
