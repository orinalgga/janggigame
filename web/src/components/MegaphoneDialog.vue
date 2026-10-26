<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { api, type MegaphoneState, type Me } from '../api'
import CharacterAvatar from './CharacterAvatar.vue'

/** 확성기 창: 가진 개수, 사기, 외치기, 지난 하루 동안의 확성기. 운영자는 여기서 내릴 수 있다. */
const props = defineProps<{ me: Me | null }>()
const emit = defineEmits<{ (e: 'close'): void }>()

const state = ref<MegaphoneState | null>(null)
const text = ref('')
const busy = ref(false)
const error = ref('')
const done = ref('')
watch(text, () => (error.value = ''))

onMounted(load)

async function load() {
  try {
    state.value = await api.megaphone()
  } catch (e) {
    error.value = (e as Error).message
  }
}

const max = computed(() => state.value?.maxLength ?? 60)
const length = computed(() => [...text.value.trim()].length)
const count = computed(() => state.value?.count ?? 0)
const canShout = computed(() => !busy.value && length.value > 0 && length.value <= max.value && count.value > 0)

async function run(action: () => Promise<void>) {
  busy.value = true
  error.value = ''
  done.value = ''
  try {
    await action()
  } catch (e) {
    error.value = (e as Error).message
  } finally {
    busy.value = false
  }
}

const buy = (packId: string) =>
  run(async () => {
    state.value = await api.buyMegaphone(packId)
    done.value = '확성기를 샀어요'
  })

const shout = () =>
  canShout.value &&
  run(async () => {
    await api.shout(text.value.trim())
    text.value = ''
    done.value = '모두에게 외쳤어요'
    state.value = await api.megaphone()
  })

const hide = (id: number) =>
  run(async () => {
    await api.admin.hideMegaphone(id)
    state.value = await api.megaphone()
  })

function ago(at: string) {
  const min = Math.floor((Date.now() - new Date(at).getTime()) / 60000)
  return min < 1 ? '방금' : min < 60 ? `${min}분 전` : `${Math.floor(min / 60)}시간 전`
}
</script>

<template>
  <div class="backdrop" @click.self="emit('close')">
    <section class="dialog" role="dialog" aria-modal="true" aria-labelledby="mega-title">
      <header class="head">
        <h2 id="mega-title">📢 확성기</h2>
        <button type="button" class="x" aria-label="닫기" @click="emit('close')">×</button>
      </header>
      <p class="lead">한 개를 쓰면 지금 접속한 모든 사람 화면 위로 내 캐릭터와 한 줄이 지나가요.</p>

      <template v-if="props.me && state">
        <div class="mine">
          <span>내 확성기 <b>{{ count }}개</b></span>
          <span class="packs">
            <button v-for="p in state.packs" :key="p.id" type="button" class="pack" :disabled="busy" @click="buy(p.id)">
              {{ p.count }}개 <b>{{ p.price }}닢</b>
            </button>
          </span>
        </div>
        <form class="write" @submit.prevent="shout">
          <input
            v-model="text"
            :maxlength="max + 10"
            :placeholder="count > 0 ? '모두에게 외칠 말 (예: 한 판 두실 분!)' : '확성기를 사면 외칠 수 있어요'"
            aria-label="외칠 말"
            autocomplete="off"
            :disabled="count === 0"
          />
          <button type="submit" class="primary" :disabled="!canShout">외치기</button>
        </form>
        <p class="help" :class="{ bad: error || length > max }">
          <span>{{ error || done || '욕설·광고는 쓸 수 없고, 30초에 한 번 외칠 수 있어요' }}</span>
          <span class="count">{{ length }} / {{ max }}</span>
        </p>
      </template>
      <p v-else-if="!props.me" class="help">로그인하면 확성기를 사서 쓸 수 있어요.</p>

      <h3>지난 하루</h3>
      <ul v-if="state?.recent.length" class="recent">
        <li v-for="p in state.recent" :key="p.id">
          <CharacterAvatar :look="p.look" head class="face" />
          <div class="line">
            <span class="meta"><b>{{ p.nickname }}</b> · {{ ago(p.at) }}</span>
            <span class="said">{{ p.text }}</span>
          </div>
          <button v-if="props.me?.admin" type="button" class="hide" :disabled="busy" @click="hide(p.id)">내리기</button>
        </li>
      </ul>
      <p v-else class="empty">아직 아무도 외치지 않았어요. 첫 번째로 외쳐 보세요!</p>
    </section>
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
  width: min(100%, 440px);
  max-height: calc(100dvh - 32px);
  overflow-y: auto;
  padding: 18px 18px 16px;
  border-radius: 16px;
  background: var(--surface-strong);
  box-shadow: 0 20px 50px rgba(0, 0, 0, 0.25);
}
.head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
h2 {
  margin: 0;
  font-size: 1.2rem;
}
h3 {
  margin: 18px 0 8px;
  font-size: 0.95rem;
}
.x {
  border: none;
  background: none;
  font-size: 1.5rem;
  color: var(--muted);
  cursor: pointer;
}
.lead {
  margin: 4px 0 14px;
  font-size: 0.88rem;
  color: var(--muted);
  line-height: 1.5;
}
.mine {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 10px 12px;
  border-radius: 12px;
  background: #fff6d6;
  border: 1px solid #ecd28a;
}
.packs {
  display: flex;
  gap: 6px;
}
.pack {
  padding: 6px 10px;
  border-radius: 999px;
  border: 1px solid var(--accent-soft);
  background: var(--surface-strong);
  color: var(--text);
  cursor: pointer;
}
.pack b {
  color: var(--accent);
}
.write {
  display: flex;
  gap: 8px;
  margin-top: 12px;
}
input {
  flex: 1;
  min-width: 0;
  box-sizing: border-box;
  font: inherit;
  padding: 10px 12px;
  border-radius: 10px;
  border: 1px solid var(--border);
  background: var(--surface);
}
input:focus {
  outline: 2px solid var(--accent-soft);
  border-color: var(--accent);
}
.primary {
  flex: none;
  font: inherit;
  font-weight: 700;
  padding: 9px 16px;
  border-radius: 10px;
  color: #fff;
  background: var(--accent);
  border: none;
  box-shadow: 0 3px 0 var(--accent-deep);
  cursor: pointer;
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
}
.recent {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  gap: 8px;
}
.recent li {
  display: flex;
  align-items: center;
  gap: 10px;
}
.face {
  width: 34px;
}
.line {
  flex: 1;
  min-width: 0;
  display: grid;
  gap: 2px;
}
.meta {
  font-size: 0.78rem;
  color: var(--muted);
}
.meta b {
  color: var(--text);
}
.said {
  overflow-wrap: anywhere;
}
.hide {
  flex: none;
  font-size: 0.8rem;
  padding: 4px 8px;
  border-radius: 8px;
  border: 1px solid var(--han-color);
  color: var(--han-color);
  background: none;
  cursor: pointer;
}
.empty {
  margin: 0;
  font-size: 0.88rem;
  color: var(--muted);
}
button:disabled {
  opacity: 0.55;
  cursor: default;
}
</style>
