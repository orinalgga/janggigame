<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { api, type AdminOrder, type AdminStats, type AdminUser, type AdminUserPage, type ShopItem } from '../api'

const emit = defineEmits<{
  (e: 'close'): void
}>()

type Tab = 'stats' | 'users' | 'orders'
const TABS: { value: Tab; label: string }[] = [
  { value: 'stats', label: '현황' },
  { value: 'users', label: '회원' },
  { value: 'orders', label: '주문' },
]
const STATUS: Record<AdminOrder['status'], string> = { PENDING: '결제 전', PAID: '결제 완료', FAILED: '실패' }

const tab = ref<Tab>('stats')
const error = ref('')
const notice = ref('')

const stats = ref<AdminStats | null>(null)
const query = ref('')
const page = ref<AdminUserPage | null>(null)
const orders = ref<AdminOrder[]>([])
const catalog = ref<ShopItem[]>([])

/** 펼쳐서 손보는 중인 회원과 입력 칸. */
const open = ref<number | null>(null)
const newName = ref('')
const reason = ref('')
const grantItem = ref('')
const busy = ref(false)

onMounted(() => {
  void loadStats()
  void api.shop().then((items) => (catalog.value = items.filter((i) => i.price > 0))).catch(() => undefined)
})

async function run<T>(work: () => Promise<T>): Promise<T | undefined> {
  busy.value = true
  error.value = ''
  try {
    return await work()
  } catch (e) {
    error.value = (e as Error).message
    return undefined
  } finally {
    busy.value = false
  }
}

async function loadStats() {
  stats.value = (await run(api.admin.stats)) ?? stats.value
}

async function search(p = 0) {
  open.value = null
  page.value = (await run(() => api.admin.users(query.value, p))) ?? page.value
}

async function loadOrders() {
  orders.value = (await run(api.admin.orders)) ?? orders.value
}

function show(t: Tab) {
  tab.value = t
  notice.value = ''
  if (t === 'stats') void loadStats()
  if (t === 'users' && !page.value) void search()
  if (t === 'orders') void loadOrders()
}

function toggle(u: AdminUser) {
  open.value = open.value === u.id ? null : u.id
  newName.value = u.nickname
  reason.value = ''
  grantItem.value = ''
  notice.value = ''
}

/** 바뀐 회원 한 줄을 목록에 다시 넣는다. */
function replace(u: AdminUser | undefined, message: string) {
  if (!u || !page.value) return
  page.value = { ...page.value, users: page.value.users.map((x) => (x.id === u.id ? u : x)) }
  notice.value = message
}

const rename = (u: AdminUser) =>
  run(() => api.admin.rename(u.id, newName.value.trim())).then((r) => replace(r, `닉네임을 ${r?.nickname}(으)로 바꿨어요.`))
const ban = (u: AdminUser) =>
  run(() => api.admin.ban(u.id, reason.value.trim())).then((r) => replace(r, `${u.nickname}님을 정지했어요.`))
const unban = (u: AdminUser) =>
  run(() => api.admin.unban(u.id)).then((r) => replace(r, `${u.nickname}님의 정지를 풀었어요.`))
const grant = (u: AdminUser) =>
  run(() => api.admin.grant(u.id, grantItem.value)).then((r) => replace(r, `${title(grantItem.value)}을(를) 넣어 줬어요.`))

const title = (id: string) => catalog.value.find((i) => i.id === id)?.title ?? id
const won = (n: number) => `${n.toLocaleString('ko-KR')}원`
const date = (s: string) =>
  new Date(s).toLocaleString('ko-KR', { month: 'numeric', day: 'numeric', hour: 'numeric', minute: '2-digit' })

const cards = computed(() => {
  const s = stats.value
  if (!s) return []
  return [
    { label: '전체 회원', value: s.users.toLocaleString('ko-KR'), sub: `오늘 +${s.usersToday}` },
    { label: '오늘 대국', value: s.gamesToday.toLocaleString('ko-KR'), sub: 'AI·2인·온라인 모두' },
    { label: '온라인 대국 중', value: s.onlinePlaying.toLocaleString('ko-KR'), sub: `상대 찾는 중 ${s.waiting}명` },
    { label: '매출', value: won(s.revenueTotal), sub: `오늘 ${won(s.revenueToday)} · ${s.paidOrders}건` },
  ]
})
</script>

<template>
  <section class="admin">
    <div class="head">
      <button type="button" class="back-button" @click="emit('close')">‹ 로비</button>
      <h1>관리자</h1>
    </div>

    <div class="tabs" role="tablist">
      <button
        v-for="t in TABS"
        :key="t.value"
        type="button"
        role="tab"
        :aria-selected="tab === t.value"
        :class="{ active: tab === t.value }"
        @click="show(t.value)"
      >
        {{ t.label }}
      </button>
    </div>

    <p v-if="error" class="error" role="alert">{{ error }}</p>
    <p v-if="notice" class="notice" role="status">{{ notice }}</p>

    <div v-if="tab === 'stats'" class="cards">
      <div v-for="c in cards" :key="c.label" class="card">
        <span class="label">{{ c.label }}</span>
        <b class="value">{{ c.value }}</b>
        <span class="sub">{{ c.sub }}</span>
      </div>
      <p v-if="stats" class="note">결제는 아직 테스트 결제라 매출은 실제 돈이 아니에요. '오늘'은 한국 시간 자정부터예요.</p>
    </div>

    <div v-else-if="tab === 'users'">
      <form class="search" @submit.prevent="search(0)">
        <input v-model="query" placeholder="닉네임, 메일, 회원 번호" aria-label="회원 검색" />
        <button type="submit" class="primary" :disabled="busy">검색</button>
      </form>
      <p v-if="page" class="count">{{ page.total.toLocaleString('ko-KR') }}명</p>
      <ul v-if="page" class="users">
        <li v-for="u in page.users" :key="u.id" :class="{ banned: u.banned, open: open === u.id }">
          <button type="button" class="row" :aria-expanded="open === u.id" @click="toggle(u)">
            <span class="id">#{{ u.id }}</span>
            <span class="who">
              <b>{{ u.nickname }}</b>
              <span v-if="u.admin" class="tag">관리자</span>
              <span v-if="u.banned" class="tag bad">정지</span>
              <span v-if="!u.nicknameChosen" class="tag soft">임시 닉네임</span>
              <small>{{ u.emails.join(', ') || '메일 없음' }}</small>
            </span>
            <span class="rating">{{ u.grade }} <small>{{ u.rating }}</small></span>
          </button>
          <div v-if="open === u.id" class="detail">
            <p class="meta">
              가입 {{ date(u.createdAt) }} · 산 테마 {{ u.items.length ? u.items.map(title).join(', ') : '없음' }}
              <template v-if="u.banned"><br />정지 사유: {{ u.banReason }}</template>
            </p>
            <div class="field">
              <input v-model="newName" maxlength="16" aria-label="새 닉네임" />
              <button type="button" class="ghost" :disabled="busy" @click="rename(u)">닉네임 바꾸기</button>
            </div>
            <div class="field">
              <select v-model="grantItem" aria-label="지급할 테마">
                <option value="" disabled>테마 고르기</option>
                <option v-for="i in catalog.filter((c) => !u.items.includes(c.id))" :key="i.id" :value="i.id">
                  {{ i.title }}
                </option>
              </select>
              <button type="button" class="ghost" :disabled="busy || !grantItem" @click="grant(u)">테마 지급</button>
            </div>
            <div class="field">
              <template v-if="!u.banned">
                <input v-model="reason" maxlength="200" placeholder="정지 사유(비우면 '운영 정책 위반')" aria-label="정지 사유" />
                <button type="button" class="danger" :disabled="busy || u.admin" @click="ban(u)">이용 정지</button>
              </template>
              <button v-else type="button" class="ghost" :disabled="busy" @click="unban(u)">정지 풀기</button>
            </div>
          </div>
        </li>
      </ul>
      <div v-if="page && page.totalPages > 1" class="pager">
        <button type="button" class="ghost" :disabled="busy || page.page === 0" @click="search(page.page - 1)">이전</button>
        <span>{{ page.page + 1 }} / {{ page.totalPages }}</span>
        <button type="button" class="ghost" :disabled="busy || page.page + 1 >= page.totalPages" @click="search(page.page + 1)">
          다음
        </button>
      </div>
    </div>

    <div v-else class="orders">
      <p v-if="!orders.length && !busy" class="count">주문이 아직 없어요.</p>
      <table v-else>
        <thead>
          <tr>
            <th>때</th>
            <th>회원</th>
            <th>상품</th>
            <th class="num">금액</th>
            <th>상태</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="o in orders" :key="o.orderId">
            <td>{{ date(o.createdAt) }}</td>
            <td>{{ o.nickname }} <small>#{{ o.userId }}</small></td>
            <td>{{ o.itemTitle }}</td>
            <td class="num">{{ won(o.amount) }}</td>
            <td><span class="status" :class="o.status.toLowerCase()">{{ STATUS[o.status] }}</span></td>
          </tr>
        </tbody>
      </table>
      <p class="note">최근 100건이에요. 결제사가 test면 실제 돈이 오가지 않은 주문이에요.</p>
    </div>
  </section>
</template>

<style scoped>
.head {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 14px;
}
h1 {
  margin: 0;
  font-size: 1.45rem;
}
.back-button {
  padding: 7px 12px;
  border-radius: 10px;
  border: 1px solid var(--border);
  background: var(--surface-strong);
  cursor: pointer;
  font-weight: 600;
}
.tabs {
  display: flex;
  gap: 4px;
  padding: 4px;
  margin-bottom: 14px;
  border-radius: 12px;
  background: var(--accent-bg);
  width: fit-content;
}
.tabs button {
  font: inherit;
  font-weight: 700;
  padding: 7px 18px;
  border: none;
  border-radius: 9px;
  background: none;
  color: var(--muted);
  cursor: pointer;
}
.tabs button.active {
  background: var(--surface-strong);
  color: var(--text);
  box-shadow: 0 1px 3px var(--shadow);
}
.cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 10px;
}
.card {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 14px 16px;
  border-radius: 14px;
  background: var(--surface-strong);
  border: 1px solid var(--border);
}
.label,
.sub,
.count,
.note,
small {
  font-size: 0.82rem;
  color: var(--muted);
}
.value {
  font-size: 1.6rem;
  font-variant-numeric: tabular-nums;
}
.note {
  grid-column: 1 / -1;
}
.search,
.field {
  display: flex;
  gap: 8px;
}
.search input,
.field input,
.field select {
  flex: 1;
  min-width: 0;
  font: inherit;
  padding: 9px 12px;
  border-radius: 10px;
  border: 1px solid var(--border);
  background: var(--surface);
}
.users {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.users li {
  border-radius: 12px;
  background: var(--surface-strong);
  border: 1px solid var(--border);
}
.users li.open {
  border-color: var(--accent);
}
.users li.banned {
  background: color-mix(in srgb, var(--han-color) 6%, var(--surface-strong));
}
.row {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 10px 12px;
  font: inherit;
  text-align: left;
  background: none;
  border: none;
  cursor: pointer;
}
.id {
  flex: none;
  width: 3.2em;
  font-size: 0.8rem;
  color: var(--muted);
  font-variant-numeric: tabular-nums;
}
.who {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 2px 6px;
}
.who small {
  flex-basis: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.rating {
  flex: none;
  font-weight: 700;
  color: var(--accent);
  font-variant-numeric: tabular-nums;
}
.tag {
  font-size: 0.7rem;
  font-weight: 700;
  padding: 1px 7px;
  border-radius: 999px;
  color: var(--accent);
  background: var(--accent-bg);
}
.tag.bad {
  color: #fff;
  background: var(--han-color);
}
.tag.soft {
  color: var(--muted);
  background: var(--surface);
}
.detail {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 0 12px 12px;
}
.meta {
  margin: 0;
  font-size: 0.85rem;
  color: var(--muted);
}
.pager {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 12px;
  margin-top: 12px;
}
table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.88rem;
  background: var(--surface-strong);
  border-radius: 12px;
  overflow: hidden;
}
th,
td {
  padding: 9px 10px;
  text-align: left;
  border-bottom: 1px solid var(--border);
}
th {
  font-size: 0.78rem;
  color: var(--muted);
  font-weight: 600;
}
.num {
  text-align: right;
  font-variant-numeric: tabular-nums;
}
.status {
  font-size: 0.75rem;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: 999px;
  white-space: nowrap;
  background: var(--surface);
  color: var(--muted);
}
.status.paid {
  color: var(--cho-color);
  background: color-mix(in srgb, var(--cho-color) 12%, transparent);
}
.status.failed {
  color: var(--han-color);
  background: color-mix(in srgb, var(--han-color) 12%, transparent);
}
.orders {
  overflow-x: auto;
}
.primary,
.ghost,
.danger {
  flex: none;
  font: inherit;
  font-size: 0.88rem;
  font-weight: 700;
  padding: 8px 14px;
  border-radius: 10px;
  cursor: pointer;
  white-space: nowrap;
}
.primary {
  color: #fff;
  background: var(--accent);
  border: none;
}
.ghost {
  background: var(--surface-strong);
  border: 1px solid var(--border);
}
.danger {
  color: #fff;
  background: var(--han-color);
  border: none;
}
button:disabled {
  opacity: 0.5;
  cursor: default;
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
@media (max-width: 560px) {
  .field {
    flex-wrap: wrap;
  }
  .field input,
  .field select {
    flex-basis: 100%;
  }
}
</style>
