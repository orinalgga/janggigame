<script setup lang="ts">
import { computed } from 'vue'

/**
 * 날짜·시각 칸. 값은 datetime-local과 같은 'YYYY-MM-DDTHH:mm'(내 시간대), 비우면 ''.
 * 한 칸짜리 datetime-local은 브라우저에 따라 달력에서 날짜를 눌러도 창 바깥을 한 번 더
 * 눌러야 값이 들어가서, 날짜(달력)와 시·분(고르기)을 따로 두고 고르는 즉시 반영한다.
 */
const props = withDefaults(defineProps<{ modelValue: string; label?: string; required?: boolean }>(), {
  label: '',
  required: false,
})
const emit = defineEmits<{ (e: 'update:modelValue', v: string): void }>()

const pad = (n: number) => String(n).padStart(2, '0')
const datePart = computed(() => props.modelValue.slice(0, 10))
const hour = computed(() => Number(props.modelValue.slice(11, 13)) || 0)
const minute = computed(() => Number(props.modelValue.slice(14, 16)) || 0)

const hours = Array.from({ length: 24 }, (_, h) => h)
/** 5분 단위. 지금 값이 5분 단위가 아니면(예: 13:47) 그 값도 넣어 둔다. */
const minutes = computed(() => {
  const list = Array.from({ length: 12 }, (_, i) => i * 5)
  if (!list.includes(minute.value)) list.push(minute.value)
  return list.sort((a, b) => a - b)
})

function today() {
  const d = new Date()
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

function set(date: string, h: number, m: number) {
  emit('update:modelValue', date ? `${date}T${pad(h)}:${pad(m)}` : '')
}
function onDate(e: Event) {
  set((e.target as HTMLInputElement).value, hour.value, minute.value)
}
function onHour(e: Event) {
  set(datePart.value || today(), Number((e.target as HTMLSelectElement).value), minute.value)
}
function onMinute(e: Event) {
  set(datePart.value || today(), hour.value, Number((e.target as HTMLSelectElement).value))
}
</script>

<template>
  <span class="dt">
    <input
      type="date"
      class="date"
      :value="datePart"
      :required="required"
      :aria-label="label ? `${label} 날짜` : '날짜'"
      @input="onDate"
      @change="onDate"
    />
    <select :value="hour" :aria-label="label ? `${label} 시` : '시'" @change="onHour">
      <option v-for="h in hours" :key="h" :value="h">{{ pad(h) }}시</option>
    </select>
    <select :value="minute" :aria-label="label ? `${label} 분` : '분'" @change="onMinute">
      <option v-for="m in minutes" :key="m" :value="m">{{ pad(m) }}분</option>
    </select>
  </span>
</template>

<style scoped>
/* 날짜는 한 줄 전체, 시·분은 그 아래 나란히. 좁은 칸(반 칸, 휴대폰)에서도 잘리지 않게. */
.dt {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  min-width: 0;
}
.dt input,
.dt select {
  box-sizing: border-box;
  min-width: 0;
  font: inherit;
  font-size: 0.95rem;
  color: var(--text);
  padding: 9px 11px;
  border-radius: 10px;
  border: 1px solid var(--border);
  background: var(--surface);
}
.dt .date {
  flex: 1 1 100%;
}
.dt select {
  flex: 1 1 0;
}
</style>
