<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { api, type Me } from '../api'

const props = defineProps<{
  me: Me
}>()

const emit = defineEmits<{
  (e: 'saved', me: Me): void
  (e: 'close'): void
}>()

const MIN = 2
const MAX = 12
const ALLOWED = /^[가-힣a-zA-Z0-9_]+$/

// 아직 임시 닉네임(장기꾼+번호)이면 빈 칸에서 시작한다.
const name = ref(props.me.nicknameChosen ? props.me.nickname : '')
const saving = ref(false)
const error = ref('')
const input = ref<HTMLInputElement | null>(null)
// 고쳐 쓰기 시작하면 서버가 알려 준 오류는 지운다.
watch(name, () => (error.value = ''))

onMounted(async () => {
  await nextTick()
  input.value?.focus()
})

const length = computed(() => [...name.value.trim()].length)
/** 서버에 보내기 전에 바로 알려 줄 수 있는 것만 본다. 중복은 서버가 알려 준다. */
const hint = computed(() => {
  const n = name.value.trim()
  if (!n) return ''
  if (length.value < MIN || length.value > MAX) return `${MIN}~${MAX}자로 정해 주세요`
  if (!ALLOWED.test(n)) return '한글, 영문, 숫자, 밑줄(_)만 쓸 수 있어요'
  return ''
})
const canSave = computed(() => !saving.value && name.value.trim() !== '' && !hint.value)

async function save() {
  if (!canSave.value) return
  saving.value = true
  error.value = ''
  try {
    emit('saved', await api.setNickname(name.value.trim()))
  } catch (e) {
    error.value = (e as Error).message
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div class="backdrop" @click.self="emit('close')">
    <form class="dialog" role="dialog" aria-modal="true" aria-labelledby="nick-title" @submit.prevent="save">
      <h2 id="nick-title">{{ me.nicknameChosen ? '닉네임 바꾸기' : '닉네임을 정해 주세요' }}</h2>
      <p class="lead">
        대국 상대에게는 구글 이름 대신 이 닉네임이 보여요.
        <template v-if="!me.nicknameChosen">지금은 임시로 <b>{{ me.nickname }}</b>이에요.</template>
      </p>
      <input
        ref="input"
        v-model="name"
        :maxlength="MAX + 4"
        placeholder="예: 포장마차고수"
        aria-label="닉네임"
        autocomplete="off"
        :aria-invalid="!!(hint || error)"
      />
      <p class="help" :class="{ bad: hint || error }">
        {{ error || hint || `${MIN}~${MAX}자, 한글·영문·숫자·밑줄` }}
        <span class="count">{{ length }} / {{ MAX }}</span>
      </p>
      <div class="actions">
        <button type="button" class="ghost" :disabled="saving" @click="emit('close')">
          {{ me.nicknameChosen ? '취소' : '나중에' }}
        </button>
        <button type="submit" class="primary" :disabled="!canSave">{{ saving ? '저장하는 중…' : '저장' }}</button>
      </div>
    </form>
  </div>
</template>

<style scoped>
.backdrop {
  position: fixed;
  inset: 0;
  z-index: 40;
  display: grid;
  place-items: center;
  padding: 16px;
  background: rgba(30, 20, 10, 0.45);
}
.dialog {
  box-sizing: border-box;
  width: min(100%, 380px);
  padding: 22px 20px 18px;
  border-radius: 16px;
  background: var(--surface-strong);
  box-shadow: 0 20px 50px rgba(0, 0, 0, 0.25);
}
h2 {
  margin: 0 0 6px;
  font-size: 1.2rem;
}
.lead {
  margin: 0 0 14px;
  font-size: 0.9rem;
  color: var(--muted);
  line-height: 1.5;
}
.lead b {
  color: var(--text);
}
input {
  box-sizing: border-box;
  width: 100%;
  font: inherit;
  font-size: 1.05rem;
  padding: 11px 12px;
  border-radius: 10px;
  border: 1px solid var(--border);
  background: var(--surface);
}
input:focus {
  outline: 2px solid var(--accent-soft);
  border-color: var(--accent);
}
.help {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  margin: 6px 2px 0;
  font-size: 0.8rem;
  color: var(--muted);
}
.help.bad {
  color: var(--han-color);
}
.count {
  flex: none;
  color: var(--muted);
}
.actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 16px;
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
</style>
