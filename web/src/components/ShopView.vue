<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { api, auth, type CoinLedgerRow, type CoinPack, type Me, type ShopItem, type ShopOrder, type ShopSlot, type ShopState } from '../api'
import { BOARD_THEMES, PIECE_THEMES, equipped, setEquipped, startingPieces, type ThemeChoice } from '../themes'
import JanggiBoard from './JanggiBoard.vue'
import UserAvatar from './UserAvatar.vue'

const props = defineProps<{
  me: Me | null
  providers: string[]
}>()

const emit = defineEmits<{
  (e: 'close'): void
  /** 아바타를 바꿔 끼면 머리글에도 바로 보이게 알린다. */
  (e: 'avatar', id: string): void
}>()

const SECTIONS: { slot: ShopSlot; title: string }[] = [
  { slot: 'BOARD', title: '장기판' },
  { slot: 'PIECES', title: '장기알' },
  { slot: 'AVATAR', title: '아바타' },
]
const KIND: Record<CoinLedgerRow['kind'], string> = { CHARGE: '충전', SPEND: '구매', ADMIN: '운영자' }

const items = ref<ShopItem[]>([])
const packs = ref<CoinPack[]>([])
/** 엽전 잔액. 로그인하지 않았으면 null. */
const coins = ref<number | null>(null)
const freeCoins = ref(0)
const loading = ref(true)
const error = ref('')
const notice = ref('')
/** 사지 않아도 눌러서 미리 볼 수 있다. */
const preview = ref<ThemeChoice>({ board: equipped.board, pieces: equipped.pieces })
const pieces = startingPieces()

/** 엽전으로 살지 묻는 상품. */
const buying = ref<ShopItem | null>(null)
/** 충전 창: 고른 묶음과 결제 대기 중인 주문. */
const charging = ref(false)
const pack = ref<string>('')
const order = ref<ShopOrder | null>(null)
const paying = ref(false)
/** 충전이 끝나면 이어서 살 상품. */
const after = ref<ShopItem | null>(null)

const history = ref<CoinLedgerRow[] | null>(null)
const historyOpen = ref(false)

onMounted(load)

function apply(state: ShopState) {
  items.value = state.items
  packs.value = state.packs
  coins.value = state.coins
  freeCoins.value = state.freeCoins ?? 0
  if (historyOpen.value) void loadHistory()
}

async function load() {
  loading.value = true
  try {
    apply(await api.shop())
    error.value = ''
  } catch (e) {
    error.value = `상점을 불러오지 못했습니다: ${(e as Error).message}`
  } finally {
    loading.value = false
  }
}

const bySlot = (slot: ShopSlot) => items.value.filter((i) => i.slot === slot)
const previewed = (i: ShopItem) =>
  i.slot === 'AVATAR' ? previewAvatar.value === i.id : (i.slot === 'BOARD' ? preview.value.board : preview.value.pieces) === i.id
const isEquipped = (i: ShopItem) =>
  i.slot === 'AVATAR' ? i.equipped : (i.slot === 'BOARD' ? equipped.board : equipped.pieces) === i.id
/** 눌러 본 아바타. 판 미리보기와 따로 논다. */
const previewAvatar = ref<string | null>(null)
const won = (n: number) => `${n.toLocaleString('ko-KR')}원`
const nyang = (n: number) => `${n.toLocaleString('ko-KR')}닢`

const differs = computed(() => preview.value.board !== equipped.board || preview.value.pieces !== equipped.pieces)
/** 사려는 상품에 모자란 엽전. */
const short = computed(() => (buying.value && coins.value !== null ? Math.max(0, buying.value.price - coins.value) : 0))

function pick(i: ShopItem) {
  notice.value = ''
  if (i.slot === 'AVATAR') {
    previewAvatar.value = i.id
    return
  }
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
  if (i.slot === 'AVATAR') {
    if (!props.me) return
    try {
      apply(await api.equip(i.id))
      emit('avatar', i.id)
      notice.value = `${i.title}을(를) 장착했어요.`
    } catch (e) {
      error.value = (e as Error).message
    }
    return
  }
  if (!props.me) {
    // 로그인하지 않았으면 무료 상품만 이번 방문 동안 바꿔 낀다.
    setEquipped(i.slot === 'BOARD' ? i.id : equipped.board, i.slot === 'PIECES' ? i.id : equipped.pieces)
    items.value = items.value.map((x) => (x.slot === i.slot ? { ...x, equipped: x.id === i.id } : x))
    return
  }
  try {
    apply(await api.equip(i.id))
    setEquipped(
      items.value.find((x) => x.slot === 'BOARD' && x.equipped)?.id,
      items.value.find((x) => x.slot === 'PIECES' && x.equipped)?.id,
    )
    notice.value = `${i.title}을(를) 장착했어요.`
  } catch (e) {
    error.value = (e as Error).message
  }
}

function askBuy(i: ShopItem) {
  error.value = ''
  notice.value = ''
  pick(i)
  buying.value = i
}

async function buy() {
  const i = buying.value
  if (!i) return
  paying.value = true
  error.value = ''
  try {
    apply(await api.buy(i.id))
    buying.value = null
    const bought = items.value.find((x) => x.id === i.id)
    if (bought) await equip(bought)
    notice.value = `${i.title}을(를) 샀어요. 바로 장착했어요.`
  } catch (e) {
    error.value = (e as Error).message
  } finally {
    paying.value = false
  }
}

/** 충전 창을 연다. 모자라서 연 것이면 모자란 만큼 채우는 가장 작은 묶음을 골라 둔다. */
function openCharge(need = 0) {
  error.value = ''
  after.value = need > 0 ? buying.value : null
  buying.value = null
  order.value = null
  pack.value = (packs.value.find((p) => p.coins >= need) ?? packs.value[packs.value.length - 1])?.id ?? ''
  charging.value = true
}

async function startOrder() {
  error.value = ''
  try {
    order.value = await api.order(pack.value)
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
    apply(await api.confirmOrder(o.orderId, `test-${Date.now()}`))
    order.value = null
    charging.value = false
    notice.value = `${o.title}을(를) 충전했어요.`
    // 사려다 모자라서 충전했으면 그 상품을 다시 묻는다.
    if (after.value) buying.value = items.value.find((x) => x.id === after.value!.id) ?? null
    after.value = null
  } catch (e) {
    error.value = (e as Error).message
  } finally {
    paying.value = false
  }
}

async function loadHistory() {
  history.value = await api.coinHistory().catch(() => history.value ?? [])
}

function toggleHistory() {
  historyOpen.value = !historyOpen.value
  if (historyOpen.value) void loadHistory()
}

const date = (s: string) =>
  new Date(s).toLocaleString('ko-KR', { month: 'numeric', day: 'numeric', hour: 'numeric', minute: '2-digit' })
</script>

<template>
  <section class="shop">
    <div class="head">
      <button type="button" class="back" @click="emit('close')">‹ 로비</button>
      <div>
        <h1>상점</h1>
        <p>장기판·장기알·아바타를 골라 나만의 판을 꾸며 보세요. 눌러 보면 바로 미리 볼 수 있어요.</p>
      </div>
    </div>

    <div v-if="me && coins !== null" class="wallet">
      <span class="coin" aria-hidden="true">錢</span>
      <span class="balance">
        <small>내 엽전</small>
        <b>{{ nyang(coins) }}</b>
        <small v-if="freeCoins">운영자 지급 {{ nyang(freeCoins) }} 포함</small>
      </span>
      <button type="button" class="text-link" :aria-expanded="historyOpen" @click="toggleHistory">내역</button>
      <button type="button" class="buy" @click="openCharge()">충전</button>
    </div>
    <div v-if="historyOpen" class="history">
      <p v-if="history && !history.length" class="muted">아직 엽전 내역이 없어요.</p>
      <ul v-else-if="history">
        <li v-for="h in history" :key="h.id">
          <span class="h-kind">{{ KIND[h.kind] }}</span>
          <span class="h-memo">
            {{ h.memo ?? '' }}
            <small>
              {{ date(h.at) }}
              <template v-if="h.kind === 'SPEND' && h.freeDelta"> · 운영자 지급분 {{ -h.freeDelta }}닢 사용</template>
            </small>
          </span>
          <span class="h-delta" :class="{ plus: h.delta > 0 }">{{ h.delta > 0 ? '+' : '' }}{{ h.delta }}</span>
          <span class="h-balance">{{ nyang(h.balance) }}</span>
        </li>
      </ul>
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
              <UserAvatar v-if="s.slot === 'AVATAR'" :id="i.id" :name="me?.nickname ?? '장'" :size="52" />
              <span v-else class="swatch" :style="swatch(i)" aria-hidden="true">
                <span class="mini-piece">{{ s.slot === 'BOARD' ? '車' : '漢' }}</span>
              </span>
              <span class="item-text">
                <b>{{ i.title }}</b>
                <span class="desc">{{ i.description }}</span>
              </span>
              <span class="item-action" @click.stop>
                <span v-if="isEquipped(i)" class="tag">장착 중</span>
                <button v-else-if="i.owned && (me || s.slot !== 'AVATAR')" type="button" class="ghost" @click="equip(i)">장착</button>
                <span v-else-if="i.reward" class="tag reward">대회 우승 보상</span>
                <button v-else-if="me" type="button" class="buy" @click="askBuy(i)">
                  <span class="coin small" aria-hidden="true">錢</span>{{ nyang(i.price) }}
                </button>
                <span v-else class="price">{{ nyang(i.price) }}</span>
              </span>
            </div>
          </div>
        </template>

        <p v-if="!me && !loading" class="login-hint">
          유료 테마는 로그인한 뒤 엽전(1닢 = 100원)으로 살 수 있어요.
          <a v-if="providers.includes('google')" :href="auth.loginUrl('google')" @click="auth.openLogin($event, 'google')">구글로 로그인</a>
        </p>
      </div>
    </div>

    <div v-if="buying" class="dialog-back" @click.self="buying = null">
      <div class="dialog" role="dialog" aria-modal="true" aria-labelledby="buy-title">
        <h2 id="buy-title">엽전으로 사기</h2>
        <div class="receipt">
          <span>{{ buying.title }}</span>
          <b>{{ nyang(buying.price) }}</b>
        </div>
        <p class="sub-line">
          가진 엽전 {{ nyang(coins ?? 0) }}<template v-if="freeCoins">(운영자 지급분부터 써요)</template>
          <template v-if="!short"> · 사고 나면 {{ nyang((coins ?? 0) - buying.price) }}</template>
          <b v-else class="short"> · {{ nyang(short) }} 모자라요</b>
        </p>
        <p v-if="error" class="error">{{ error }}</p>
        <div class="dialog-actions">
          <button type="button" class="ghost" :disabled="paying" @click="buying = null">취소</button>
          <button v-if="short" type="button" class="primary" @click="openCharge(short)">충전하러 가기</button>
          <button v-else type="button" class="primary" :disabled="paying" @click="buy">
            {{ paying ? '사는 중…' : `${nyang(buying.price)}으로 사기` }}
          </button>
        </div>
      </div>
    </div>

    <div v-if="charging" class="dialog-back" @click.self="!paying && (charging = false)">
      <div class="dialog" role="dialog" aria-modal="true" aria-labelledby="charge-title">
        <h2 id="charge-title">엽전 충전</h2>
        <template v-if="!order">
          <p class="sub-line">1닢은 100원이에요.{{ after ? ` ${after.title}을(를) 사려면 엽전이 더 필요해요.` : '' }}</p>
          <div class="packs" role="radiogroup" aria-label="충전할 엽전">
            <label v-for="p in packs" :key="p.id" class="pack" :class="{ active: pack === p.id }">
              <input v-model="pack" type="radio" name="pack" :value="p.id" />
              <span class="coin small" aria-hidden="true">錢</span>
              <b>{{ nyang(p.coins) }}</b>
              <span class="pack-price">{{ won(p.price) }}</span>
            </label>
          </div>
          <p v-if="error" class="error">{{ error }}</p>
          <div class="dialog-actions">
            <button type="button" class="ghost" @click="charging = false">취소</button>
            <button type="button" class="primary" :disabled="!pack" @click="startOrder">다음</button>
          </div>
        </template>
        <template v-else>
          <div class="receipt">
            <span>{{ order.title }}</span>
            <b>{{ won(order.amount) }}</b>
          </div>
          <p v-if="order.provider === 'test'" class="test-note">
            지금은 테스트 결제라서 실제로 돈이 나가지 않아요.
          </p>
          <p v-if="error" class="error">{{ error }}</p>
          <div class="dialog-actions">
            <button type="button" class="ghost" :disabled="paying" @click="order = null">뒤로</button>
            <button type="button" class="primary" :disabled="paying" @click="pay">
              {{ paying ? '결제하는 중…' : `${won(order.amount)} 결제하기` }}
            </button>
          </div>
        </template>
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
.wallet {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  margin-bottom: 8px;
  border-radius: 14px;
  background: var(--surface-strong);
  border: 1px solid var(--border);
}
.coin {
  flex: none;
  display: inline-grid;
  place-items: center;
  width: 34px;
  height: 34px;
  border-radius: 50%;
  font-family: 'Noto Serif KR', serif;
  font-weight: 900;
  font-size: 0.95rem;
  color: #6b4a12;
  background: radial-gradient(circle at 40% 35%, #f3d77a, #c9a227);
  box-shadow: inset 0 0 0 2px #a8821c;
}
.coin.small {
  width: 18px;
  height: 18px;
  margin-right: 4px;
  font-size: 0.6rem;
  vertical-align: -3px;
  box-shadow: inset 0 0 0 1.5px #a8821c;
}
.balance {
  display: flex;
  flex-direction: column;
  margin-right: auto;
  line-height: 1.2;
}
.balance small {
  font-size: 0.75rem;
  color: var(--muted);
}
.balance b {
  font-size: 1.15rem;
  font-variant-numeric: tabular-nums;
}
.text-link {
  font: inherit;
  font-size: 0.85rem;
  color: var(--muted);
  background: none;
  border: none;
  text-decoration: underline;
  cursor: pointer;
}
.history {
  margin-bottom: 14px;
  border-radius: 14px;
  background: var(--surface-strong);
  border: 1px solid var(--border);
  max-height: 260px;
  overflow-y: auto;
}
.history .muted {
  margin: 0;
  padding: 12px 14px;
}
.history ul {
  list-style: none;
  margin: 0;
  padding: 0;
}
.history li {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 14px;
  border-top: 1px solid var(--border);
  font-size: 0.85rem;
}
.history li:first-child {
  border-top: none;
}
.h-kind {
  flex: none;
  font-size: 0.72rem;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: 999px;
  color: var(--accent);
  background: var(--accent-bg);
}
.h-memo {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}
.h-memo small {
  font-size: 0.72rem;
  color: var(--muted);
}
.h-delta {
  flex: none;
  font-weight: 800;
  color: var(--han-color);
  font-variant-numeric: tabular-nums;
}
.h-delta.plus {
  color: var(--cho-color);
}
.h-balance {
  flex: none;
  width: 4.5em;
  text-align: right;
  color: var(--muted);
  font-variant-numeric: tabular-nums;
}
.sub-line {
  font-size: 0.88rem;
  color: var(--muted);
}
.short {
  color: var(--han-color);
}
.packs {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}
.pack {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 2px 4px;
  padding: 10px 12px;
  border-radius: 12px;
  border: 1px solid var(--border);
  background: var(--surface);
  cursor: pointer;
}
.pack.active {
  border-color: var(--accent);
  box-shadow: 0 0 0 2px var(--accent-soft);
}
.pack input {
  position: absolute;
  opacity: 0;
  pointer-events: none;
}
.pack-price {
  flex-basis: 100%;
  font-size: 0.82rem;
  color: var(--muted);
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
.tag.reward {
  color: #7a4a00;
  background: #fbe8b0;
  white-space: nowrap;
}
.price {
  font-weight: 700;
  font-size: 0.9rem;
  color: var(--muted);
}
.buy {
  display: inline-flex;
  align-items: center;
}
.buy .coin.small {
  vertical-align: initial;
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
