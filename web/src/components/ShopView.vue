<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { api, auth, type Me, type ShopItem, type ShopOrder, type ShopSlot } from '../api'
import { BOARD_THEMES, PIECE_THEMES, equipped, setEquipped, startingPieces, type ThemeChoice } from '../themes'
import JanggiBoard from './JanggiBoard.vue'

const props = defineProps<{
  me: Me | null
  providers: string[]
}>()

const emit = defineEmits<{
  (e: 'close'): void
}>()

const SECTIONS: { slot: ShopSlot; title: string }[] = [
  { slot: 'BOARD', title: '장기판' },
  { slot: 'PIECES', title: '장기알' },
]

const items = ref<ShopItem[]>([])
const loading = ref(true)
const error = ref('')
const notice = ref('')
/** 사지 않아도 눌러서 미리 볼 수 있다. */
const preview = ref<ThemeChoice>({ board: equipped.board, pieces: equipped.pieces })
const order = ref<ShopOrder | null>(null)
const paying = ref(false)
const pieces = startingPieces()

onMounted(load)

async function load() {
  loading.value = true
  try {
    items.value = await api.shop()
    error.value = ''
  } catch (e) {
    error.value = `상점을 불러오지 못했습니다: ${(e as Error).message}`
  } finally {
    loading.value = false
  }
}

const bySlot = (slot: ShopSlot) => items.value.filter((i) => i.slot === slot)
const previewed = (i: ShopItem) => (i.slot === 'BOARD' ? preview.value.board : preview.value.pieces) === i.id
const isEquipped = (i: ShopItem) => (i.slot === 'BOARD' ? equipped.board : equipped.pieces) === i.id
const won = (n: number) => `${n.toLocaleString('ko-KR')}원`

const differs = computed(() => preview.value.board !== equipped.board || preview.value.pieces !== equipped.pieces)

function pick(i: ShopItem) {
  notice.value = ''
  preview.value = i.slot === 'BOARD' ? { ...preview.value, board: i.id } : { ...preview.value, pieces: i.id }
}

/** 카드 안의 작은 견본: 판 색 위에 알 하나. */
function swatch(i: ShopItem) {
  const board = BOARD_THEMES[i.slot === 'BOARD' ? i.id : preview.value.board] ?? {}
  const piece = PIECE_THEMES[i.slot === 'PIECES' ? i.id : preview.value.pieces] ?? {}
  return { ...board, ...piece }
}

async function equip(i: ShopItem) {
  error.value = ''
  if (!props.me) {
    // 로그인하지 않았으면 무료 상품만 이번 방문 동안 바꿔 낀다.
    setEquipped(i.slot === 'BOARD' ? i.id : equipped.board, i.slot === 'PIECES' ? i.id : equipped.pieces)
    items.value = items.value.map((x) => (x.slot === i.slot ? { ...x, equipped: x.id === i.id } : x))
    return
  }
  try {
    items.value = await api.equip(i.id)
    setEquipped(
      items.value.find((x) => x.slot === 'BOARD' && x.equipped)?.id,
      items.value.find((x) => x.slot === 'PIECES' && x.equipped)?.id,
    )
    notice.value = `${i.title}을(를) 장착했어요.`
  } catch (e) {
    error.value = (e as Error).message
  }
}

async function buy(i: ShopItem) {
  error.value = ''
  notice.value = ''
  pick(i)
  try {
    order.value = await api.order(i.id)
  } catch (e) {
    error.value = (e as Error).message
  }
}

async function pay() {
  const o = order.value
  if (!o) return
  paying.value = true
  error.value = ''
  try {
    // 테스트 결제는 결제창 없이 바로 승인 키를 만든다. 실제 결제사는 결제창이 이 키를 돌려준다.
    items.value = await api.confirmOrder(o.orderId, `test-${Date.now()}`)
    order.value = null
    const bought = items.value.find((x) => x.id === o.itemId)
    if (bought) await equip(bought)
    notice.value = `${o.title}을(를) 샀어요. 바로 장착했어요.`
  } catch (e) {
    error.value = (e as Error).message
  } finally {
    paying.value = false
  }
}
</script>

<template>
  <section class="shop">
    <div class="head">
      <button type="button" class="back" @click="emit('close')">‹ 로비</button>
      <div>
        <h1>상점</h1>
        <p>장기판과 장기알을 골라 나만의 판을 꾸며 보세요. 눌러 보면 바로 미리 볼 수 있어요.</p>
      </div>
    </div>

    <div class="layout">
      <div class="preview">
        <JanggiBoard
          :pieces="pieces"
          :legal-moves="[]"
          :selected="null"
          :last-move="null"
          :flipped="false"
          :theme="preview"
        />
        <p class="preview-note">
          <template v-if="differs">미리보기 중이에요. 사거나 장착해야 대국에 적용돼요.</template>
          <template v-else>지금 대국에 쓰는 판이에요.</template>
        </p>
      </div>

      <div class="catalog">
        <p v-if="loading" class="muted">불러오는 중…</p>
        <p v-if="error" class="error" role="alert">{{ error }}</p>
        <p v-if="notice" class="notice" role="status">{{ notice }}</p>

        <template v-for="s in SECTIONS" :key="s.slot">
          <h3>{{ s.title }}</h3>
          <div class="items">
            <div
              v-for="i in bySlot(s.slot)"
              :key="i.id"
              class="item"
              :class="{ active: previewed(i) }"
              role="button"
              tabindex="0"
              :aria-pressed="previewed(i)"
              @click="pick(i)"
              @keydown.enter="pick(i)"
            >
              <span class="swatch" :style="swatch(i)" aria-hidden="true">
                <span class="mini-piece">{{ s.slot === 'BOARD' ? '車' : '漢' }}</span>
              </span>
              <span class="item-text">
                <b>{{ i.title }}</b>
                <span class="desc">{{ i.description }}</span>
              </span>
              <span class="item-action" @click.stop>
                <span v-if="isEquipped(i)" class="tag">장착 중</span>
                <button v-else-if="i.owned" type="button" class="ghost" @click="equip(i)">장착</button>
                <button v-else-if="me" type="button" class="buy" @click="buy(i)">{{ won(i.price) }}</button>
                <span v-else class="price">{{ won(i.price) }}</span>
              </span>
            </div>
          </div>
        </template>

        <p v-if="!me && !loading" class="login-hint">
          유료 테마는 로그인하면 살 수 있어요.
          <a v-if="providers.includes('google')" :href="auth.loginUrl('google')" @click="auth.openLogin($event, 'google')">구글로 로그인</a>
        </p>
      </div>
    </div>

    <div v-if="order" class="dialog-back" @click.self="order = null">
      <div class="dialog" role="dialog" aria-modal="true" aria-labelledby="pay-title">
        <h2 id="pay-title">결제</h2>
        <div class="receipt">
          <span>{{ order.title }}</span>
          <b>{{ won(order.amount) }}</b>
        </div>
        <p v-if="order.provider === 'test'" class="test-note">
          지금은 테스트 결제라서 실제로 돈이 나가지 않아요.
        </p>
        <p v-if="error" class="error">{{ error }}</p>
        <div class="dialog-actions">
          <button type="button" class="ghost" :disabled="paying" @click="order = null">취소</button>
          <button type="button" class="primary" :disabled="paying" @click="pay">
            {{ paying ? '결제하는 중…' : `${won(order.amount)} 결제하기` }}
          </button>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
.head {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  margin-bottom: 16px;
}
.head h1 {
  margin: 0 0 4px;
  font-size: 1.45rem;
  letter-spacing: -0.02em;
}
.head p {
  margin: 0;
  color: var(--muted);
}
.back {
  flex: none;
  padding: 7px 12px;
  border-radius: 10px;
  border: 1px solid var(--border);
  background: var(--surface-strong);
  cursor: pointer;
  font-weight: 600;
}
.layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 20px;
  align-items: start;
}
.preview {
  position: sticky;
  top: 12px;
}
.preview-note {
  margin: 8px 0 0;
  text-align: center;
  font-size: 0.85rem;
  color: var(--muted);
}
h3 {
  margin: 4px 0 8px;
  font-size: 0.95rem;
}
.items {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 18px;
}
.item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 14px;
  background: var(--surface-strong);
  border: 1px solid var(--border);
  cursor: pointer;
  text-align: left;
  transition: border-color 0.15s, box-shadow 0.15s;
}
.item.active {
  border-color: var(--accent);
  box-shadow: 0 0 0 2px var(--accent-soft);
}
.swatch {
  flex: none;
  display: grid;
  place-items: center;
  width: 52px;
  height: 52px;
  border-radius: 10px;
  background: linear-gradient(135deg, var(--board-light), var(--board-dark));
  box-shadow: inset 0 0 0 2px var(--board-line);
}
.mini-piece {
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  font-family: 'Noto Serif KR', serif;
  font-weight: 900;
  font-size: 1rem;
  color: var(--han-color);
  background: radial-gradient(circle at 40% 35%, var(--piece-light), var(--piece-dark));
  clip-path: polygon(30% 0, 70% 0, 100% 30%, 100% 70%, 70% 100%, 30% 100%, 0 70%, 0 30%);
  box-shadow: inset 0 0 0 2.5px var(--piece-edge);
}
.item-text {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.desc {
  font-size: 0.82rem;
  color: var(--muted);
}
.item-action {
  flex: none;
}
.tag {
  font-size: 0.78rem;
  font-weight: 700;
  padding: 4px 10px;
  border-radius: 999px;
  color: var(--accent);
  background: var(--accent-bg);
}
.price {
  font-weight: 700;
  font-size: 0.9rem;
  color: var(--muted);
}
.ghost,
.buy {
  font: inherit;
  font-size: 0.88rem;
  font-weight: 700;
  padding: 6px 12px;
  border-radius: 10px;
  cursor: pointer;
}
.ghost {
  background: none;
  border: 1px solid var(--border);
}
.buy {
  color: #fff;
  background: var(--accent);
  border: none;
  box-shadow: 0 2px 0 var(--accent-deep);
}
.primary {
  font: inherit;
  font-weight: 700;
  color: #fff;
  background: var(--accent);
  border: none;
  border-radius: 12px;
  padding: 10px 16px;
  box-shadow: 0 3px 0 var(--accent-deep);
  cursor: pointer;
}
button:disabled {
  opacity: 0.6;
  cursor: default;
}
.muted {
  color: var(--muted);
}
.error {
  color: var(--han-color);
}
.notice {
  padding: 8px 12px;
  border-radius: 10px;
  background: var(--accent-bg);
  color: var(--accent-deep);
  font-weight: 600;
}
.login-hint {
  font-size: 0.9rem;
  color: var(--muted);
}
.login-hint a {
  margin-left: 6px;
  font-weight: 700;
  color: var(--accent);
}
.dialog-back {
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
  width: min(100%, 360px);
  padding: 20px;
  border-radius: 16px;
  background: var(--surface-strong);
  box-shadow: 0 20px 50px rgba(0, 0, 0, 0.25);
}
.dialog h2 {
  margin: 0 0 12px;
  font-size: 1.15rem;
}
.receipt {
  display: flex;
  justify-content: space-between;
  padding: 12px 14px;
  border-radius: 12px;
  background: var(--surface);
  border: 1px solid var(--border);
}
.test-note {
  font-size: 0.85rem;
  color: var(--muted);
}
.dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 14px;
}
@media (max-width: 720px) {
  .layout {
    grid-template-columns: minmax(0, 1fr);
  }
  .preview {
    position: static;
  }
}
</style>
