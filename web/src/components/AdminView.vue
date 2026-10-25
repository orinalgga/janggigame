<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { api, type AdminLog, type AdminOrder, type AdminStats, type AdminUser, type AdminUserPage, type Notice, type ShopItem } from '../api'

const emit = defineEmits<{
  (e: 'close'): void
}>()

type Tab = 'stats' | 'users' | 'notices' | 'orders' | 'logs'
const TABS: { value: Tab; label: string }[] = [
  { value: 'stats', label: '현황' },
  { value: 'users', label: '회원' },
  { value: 'notices', label: '공지' },
  { value: 'orders', label: '주문' },
  { value: 'logs', label: '기록' },
]
const ACTION: Record<AdminLog['action'], string> = {
  RENAME: '닉네임 변경',
  BAN: '이용 정지',
  UNBAN: '정지 해제',
  GRANT: '테마 지급',
  COINS: '엽전',
  NOTICE_CREATE: '공지 등록',
  NOTICE_UPDATE: '공지 수정',
  NOTICE_DELETE: '공지 삭제',
}
const STATUS: Record<AdminOrder['status'], string> = { PENDING: '결제 전', PAID: '결제 완료', FAILED: '실패' }

const tab = ref<Tab>('stats')
const error = ref('')
const notice = ref('')

const stats = ref<AdminStats | null>(null)
const query = ref('')
const page = ref<AdminUserPage | null>(null)
const orders = ref<AdminOrder[]>([])
const logs = ref<AdminLog[]>([])
const notices = ref<Notice[]>([])

/** 공지 쓰는 칸. editing이 null이면 새 공지. 시각은 datetime-local 값(내 시간대)이다. */
const editing = ref<number | null>(null)
const form = ref({ title: '', body: '', startsAt: '', endsAt: '', active: true })
const confirmDelete = ref<number | null>(null)
const catalog = ref<ShopItem[]>([])

/** 펼쳐서 손보는 중인 회원과 입력 칸. */
const open = ref<number | null>(null)
const newName = ref('')
const reason = ref('')
const grantItem = ref('')
const coinAmount = ref<number | null>(null)
const coinMemo = ref('')
const busy = ref(false)

onMounted(() => {
  void loadStats()
  void api.shop().then((s) => (catalog.value = s.items.filter((i) => i.price > 0))).catch(() => undefined)
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

async function loadLogs() {
  logs.value = (await run(api.admin.logs)) ?? logs.value
}

async function loadNotices() {
  notices.value = (await run(api.admin.notices)) ?? notices.value
}

/** ISO 시각을 datetime-local 칸 값(내 시간대, 분까지)으로. */
function toLocal(iso: string | null) {
  if (!iso) return ''
  const d = new Date(iso)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`
}
const fromLocal = (v: string) => (v ? new Date(v).toISOString() : null)

function newNotice() {
  editing.value = null
  form.value = { title: '', body: '', startsAt: '', endsAt: '', active: true }
  confirmDelete.value = null
}

function editNotice(n: Notice) {
  editing.value = n.id
  form.value = { title: n.title, body: n.body, startsAt: toLocal(n.startsAt), endsAt: toLocal(n.endsAt), active: n.active }
  confirmDelete.value = null
  notice.value = ''
}

async function saveNotice() {
  const f = form.value
  const input = { title: f.title.trim(), body: f.body.trim(), startsAt: fromLocal(f.startsAt), endsAt: fromLocal(f.endsAt), active: f.active }
  const id = editing.value
  const saved = await run(() => (id === null ? api.admin.createNotice(input) : api.admin.updateNotice(id, input)))
  if (!saved) return
  notice.value = id === null ? '공지를 올렸어요.' : '공지를 고쳤어요. 이미 닫은 사람에게도 다시 떠요.'
  newNotice()
  await loadNotices()
}

async function deleteNotice(n: Notice) {
  if (await run(() => api.admin.deleteNotice(n.id))) {
    notice.value = `'${n.title}' 공지를 지웠어요.`
    if (editing.value === n.id) newNotice()
    confirmDelete.value = null
    await loadNotices()
  }
}

/** 공지가 지금 사용자에게 보이는지. */
function noticeState(n: Notice): { label: string; cls: string } {
  const now = Date.now()
  if (!n.active) return { label: '꺼짐', cls: '' }
  if (new Date(n.startsAt).getTime() > now) return { label: '예약', cls: 'pending' }
  if (n.endsAt && new Date(n.endsAt).getTime() <= now) return { label: '끝남', cls: '' }
  return { label: '보이는 중', cls: 'paid' }
}

async function loadOrders() {
  orders.value = (await run(api.admin.orders)) ?? orders.value
}

function show(t: Tab) {
  tab.value = t
  notice.value = ''
  if (t === 'stats') void loadStats()
  if (t === 'users' && !page.value) void search()
  if (t === 'notices') void loadNotices()
  if (t === 'orders') void loadOrders()
  if (t === 'logs') void loadLogs()
}

function toggle(u: AdminUser) {
  open.value = open.value === u.id ? null : u.id
  newName.value = u.nickname
  reason.value = ''
  grantItem.value = ''
  coinAmount.value = null
  coinMemo.value = ''
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

const giveCoins = (u: AdminUser) => {
  const n = coinAmount.value ?? 0
  return run(() => api.admin.coins(u.id, n, coinMemo.value.trim())).then((r) => {
    replace(r, `엽전 ${Math.abs(n)}닢을 ${n > 0 ? '넣었' : '뺐'}어요. 잔액 ${r?.coins}닢(운영자 지급 ${r?.freeCoins}닢).`)
    if (r) coinAmount.value = null
  })
}

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
              가입 {{ date(u.createdAt) }} · 엽전 {{ u.coins.toLocaleString('ko-KR') }}닢(운영자 지급 {{ u.freeCoins.toLocaleString('ko-KR') }}닢) · 산 테마 {{ u.items.length ? u.items.map(title).join(', ') : '없음' }}
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
              <input v-model.number="coinAmount" type="number" step="1" placeholder="엽전(빼려면 -)" aria-label="넣거나 뺄 엽전" :title="`운영자가 준 엽전만 뺄 수 있어요. 지금 ${u.freeCoins}닢`" />
              <input v-model="coinMemo" maxlength="200" placeholder="사유(회원 내역에 보여요)" aria-label="엽전 사유" />
              <button type="button" class="ghost" :disabled="busy || !coinAmount" @click="giveCoins(u)">엽전 넣기·빼기</button>
            </div>
            <p class="hint">넣은 엽전은 운영자 지급분이 되고, 빼기는 운영자 지급분({{ u.freeCoins }}닢)에서만 돼요. 회원이 충전한 엽전은 뺄 수 없어요.</p>
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

    <div v-else-if="tab === 'notices'" class="notices">
      <form class="notice-form" @submit.prevent="saveNotice">
        <h2>{{ editing === null ? '새 공지' : '공지 고치기' }}</h2>
        <input v-model="form.title" maxlength="60" placeholder="제목" aria-label="공지 제목" required />
        <textarea v-model="form.body" maxlength="2000" rows="5" placeholder="내용(줄바꿈 그대로 보여요)" aria-label="공지 내용" required />
        <div class="when-fields">
          <label>
            <span>보이기 시작</span>
            <input v-model="form.startsAt" type="datetime-local" aria-label="보이기 시작" />
          </label>
          <label>
            <span>끝</span>
            <input v-model="form.endsAt" type="datetime-local" aria-label="끝" />
          </label>
        </div>
        <p class="count">시작을 비우면 지금부터, 끝을 비우면 끌 때까지 보여요. 사용자가 로비에 들어올 때 팝업으로 떠요.</p>
        <label class="check">
          <input v-model="form.active" type="checkbox" />
          켜기
        </label>
        <div class="form-actions">
          <button v-if="editing !== null" type="button" class="ghost" :disabled="busy" @click="newNotice">취소</button>
          <button type="submit" class="primary" :disabled="busy || !form.title.trim() || !form.body.trim()">
            {{ editing === null ? '올리기' : '고치기' }}
          </button>
        </div>
      </form>

      <p v-if="!notices.length && !busy" class="count">공지가 아직 없어요.</p>
      <ul v-else class="notice-list">
        <li v-for="n in notices" :key="n.id" :class="{ open: editing === n.id }">
          <div class="notice-main">
            <span class="status" :class="noticeState(n).cls">{{ noticeState(n).label }}</span>
            <b>{{ n.title }}</b>
            <small>{{ date(n.startsAt) }} ~ {{ n.endsAt ? date(n.endsAt) : '끌 때까지' }}</small>
          </div>
          <div class="notice-buttons">
            <button type="button" class="ghost" :disabled="busy" @click="editNotice(n)">고치기</button>
            <button v-if="confirmDelete !== n.id" type="button" class="ghost" :disabled="busy" @click="confirmDelete = n.id">지우기</button>
            <button v-else type="button" class="danger" :disabled="busy" @click="deleteNotice(n)">정말 지우기</button>
          </div>
        </li>
      </ul>
    </div>

    <div v-else-if="tab === 'logs'" class="orders">
      <p v-if="!logs.length && !busy" class="count">아직 기록이 없어요.</p>
      <table v-else>
        <thead>
          <tr>
            <th>때</th>
            <th>관리자</th>
            <th>한 일</th>
            <th>대상</th>
            <th>내용</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(l, i) in logs" :key="i">
            <td>{{ date(l.at) }}</td>
            <td>{{ l.admin }}</td>
            <td><span class="status" :class="{ failed: l.action === 'BAN' }">{{ ACTION[l.action] }}</span></td>
            <td>{{ l.target ?? '-' }} <small v-if="l.targetId">#{{ l.targetId }}</small></td>
            <td>{{ l.detail ?? '' }}</td>
          </tr>
        </tbody>
      </table>
      <p class="note">최근 100건이에요. 기록은 지울 수 없어요.</p>
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
  max-width: 100%;
  box-sizing: border-box;
}
.tabs button {
  white-space: nowrap;
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
.hint {
  margin: -2px 0 0;
  font-size: 0.78rem;
  color: var(--muted);
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
.status.pending {
  color: var(--accent-deep);
  background: var(--accent-bg);
}
.notice-form {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 14px;
  margin-bottom: 14px;
  border-radius: 14px;
  background: var(--surface-strong);
  border: 1px solid var(--border);
}
.notice-form h2 {
  margin: 0 0 2px;
  font-size: 1rem;
}
.notice-form input:not([type='checkbox']),
.notice-form textarea {
  box-sizing: border-box;
  width: 100%;
  min-width: 0;
  font: inherit;
  padding: 9px 12px;
  border-radius: 10px;
  border: 1px solid var(--border);
  background: var(--surface);
}
.notice-form textarea {
  resize: vertical;
  line-height: 1.5;
}
.notice-form .count {
  margin: 0;
}
.when-fields {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}
.when-fields label {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
  font-size: 0.8rem;
  color: var(--muted);
}
.check {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 600;
}
.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
.notice-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.notice-list li {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 12px;
  background: var(--surface-strong);
  border: 1px solid var(--border);
}
.notice-list li.open {
  border-color: var(--accent);
}
.notice-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 2px 8px;
}
.notice-main b {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.notice-main small {
  flex-basis: 100%;
}
.notice-buttons {
  flex: none;
  display: flex;
  gap: 6px;
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
  .field,
  .notice-list li {
    flex-wrap: wrap;
  }
  .when-fields {
    grid-template-columns: 1fr;
  }
  .notice-main {
    flex-basis: 100%;
  }
  .notice-buttons {
    margin-left: auto;
  }
  .tabs button {
    padding: 7px 12px;
  }
  .field input,
  .field select {
    flex-basis: 100%;
  }
}
</style>
