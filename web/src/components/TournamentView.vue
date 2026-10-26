<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { api, auth, type Me, type TournamentDetail, type TournamentMatch, type TournamentPlayer } from '../api'
import { DIVISION, countdown, kindLabel, roundName, startLabel, timeControl } from '../tournaments'

const props = defineProps<{
  id: number
  me: Me | null
  providers: string[]
}>()

const emit = defineEmits<{
  (e: 'close'): void
  /** 내 대국이면 두러, 남의 대국이면 구경하러 간다. */
  (e: 'game', gameId: string): void
}>()

const detail = ref<TournamentDetail | null>(null)
const error = ref('')
const notice = ref('')
const busy = ref(false)
const confirm = ref<'leave' | 'cancel' | null>(null)
const now = ref(Date.now())
let tick: ReturnType<typeof setInterval> | undefined
let poll: ReturnType<typeof setInterval> | undefined

async function load() {
  try {
    detail.value = await api.tournament(props.id)
    error.value = ''
  } catch (e) {
    error.value = (e as Error).message
  }
}

onMounted(() => {
  void load()
  tick = setInterval(() => (now.value = Date.now()), 1000)
  // 진행 중에는 대진표가 자주 바뀌므로 자주, 아니면 가끔 새로 받는다.
  poll = setInterval(() => {
    const s = detail.value?.summary.status
    if (s === 'RUNNING' || s === 'SCHEDULED') void load()
  }, 3000)
})
onBeforeUnmount(() => {
  clearInterval(tick)
  clearInterval(poll)
})

async function act(work: () => Promise<TournamentDetail>, message: string) {
  busy.value = true
  error.value = ''
  notice.value = ''
  try {
    detail.value = await work()
    notice.value = message
    confirm.value = null
  } catch (e) {
    error.value = (e as Error).message
  } finally {
    busy.value = false
  }
}

const s = computed(() => detail.value?.summary)
const isHost = computed(() => !!props.me && s.value?.host?.id === props.me.id)
const full = computed(() => !!s.value && s.value.players >= s.value.maxPlayers)

const join = () => act(() => api.joinTournament(props.id), '참가 신청했어요. 시작 시각에 이 화면이나 로비에 있으면 대국이 자동으로 열려요.')
const leave = () =>
  act(
    () => api.leaveTournament(props.id),
    s.value?.status === 'SCHEDULED' ? '참가를 취소하고 참가비를 돌려받았어요.' : '대회를 기권했어요.',
  )
const cancelHosted = () => act(() => api.cancelHosted(props.id), '대회를 취소했어요. 참가비와 건 상금은 모두 돌려줬어요.')

/** 라운드별 대진. */
const rounds = computed(() => {
  const d = detail.value
  if (!d) return []
  const by = new Map<number, TournamentMatch[]>()
  for (const m of d.matches) by.set(m.round, [...(by.get(m.round) ?? []), m])
  return [...by.entries()].map(([round, matches]) => ({ round, name: roundName(round, d.summary.totalRounds), matches }))
})

function side(m: TournamentMatch, p: TournamentPlayer | null) {
  if (!p) return 'empty'
  if (m.winnerId === null) return ''
  return m.winnerId === p.id ? 'won' : 'lost'
}

const live = (m: TournamentMatch) => m.gameId !== null && m.winnerId === null
const mine = (m: TournamentMatch) => !!props.me && (m.cho?.id === props.me.id || m.han?.id === props.me.id)

const statusLine = computed(() => {
  const t = s.value
  const d = detail.value
  if (!t || !d) return ''
  switch (t.status) {
    case 'SCHEDULED':
      return `${countdown(t.startsAt, now.value)} 뒤 시작`
    case 'RUNNING':
      if (d.nextRoundAt) return `${roundName(t.currentRound + 1, t.totalRounds)}이 ${countdown(d.nextRoundAt, now.value)} 뒤 시작해요`
      return `${roundName(t.currentRound, t.totalRounds)} 진행 중`
    case 'FINISHED':
      return t.winner ? `${t.winner.nickname}님 우승!` : '끝난 대회'
    default:
      return '취소된 대회'
  }
})
</script>

<template>
  <section class="tournament">
    <div class="head">
      <button type="button" class="back-button" @click="emit('close')">‹ 로비</button>
      <div v-if="s" class="title-box">
        <span class="badges">
          <span class="badge" :class="{ regular: s.kind === 'REGULAR' && !s.host, hosted: !!s.host }">{{ kindLabel(s) }}</span>
          <span v-if="s.division !== 'ALL'" class="badge div">{{ DIVISION[s.division] }} 리그</span>
        </span>
        <h1>{{ s.title }}</h1>
      </div>
    </div>

    <p v-if="error" class="error" role="alert">{{ error }}</p>
    <p v-if="notice" class="notice" role="status">{{ notice }}</p>

    <template v-if="s && detail">
      <div class="info">
        <div class="status" :class="s.status.toLowerCase()">{{ statusLine }}</div>
        <dl>
          <div><dt>시작</dt><dd>{{ startLabel(s.startsAt) }}</dd></div>
          <div><dt>시간</dt><dd>{{ timeControl(s) }} 속기</dd></div>
          <div><dt>참가</dt><dd>{{ s.players }}/{{ s.maxPlayers }}명</dd></div>
          <div><dt>참가비</dt><dd>{{ s.entryFee }}닢</dd></div>
          <div class="prize">
            <dt>우승 상금</dt>
            <dd>{{ s.prizePool ? `${s.prizePool.toLocaleString('ko-KR')}닢` : '없음' }}</dd>
          </div>
          <div v-if="s.host"><dt>연 사람</dt><dd>{{ s.host.nickname }}</dd></div>
        </dl>
        <p v-if="detail.description" class="desc">{{ detail.description }}</p>

        <div class="actions">
          <button v-if="detail.myGameId" type="button" class="primary pulse" @click="emit('game', detail.myGameId)">내 대국 두러 가기</button>
          <template v-if="s.status === 'SCHEDULED'">
            <template v-if="me">
              <button v-if="!s.joined" type="button" class="primary" :disabled="busy || full" @click="join">
                {{ full ? '정원이 찼어요' : `참가하기 (${s.entryFee}닢)` }}
              </button>
              <template v-else>
                <span class="joined">참가 신청함</span>
                <button v-if="confirm !== 'leave'" type="button" class="ghost" :disabled="busy" @click="confirm = 'leave'">참가 취소</button>
                <button v-else type="button" class="danger" :disabled="busy" @click="leave">취소하고 환불받기</button>
              </template>
              <template v-if="isHost">
                <button v-if="confirm !== 'cancel'" type="button" class="ghost" :disabled="busy" @click="confirm = 'cancel'">대회 취소</button>
                <button v-else type="button" class="danger" :disabled="busy" @click="cancelHosted">정말 대회 취소</button>
              </template>
            </template>
            <a v-else-if="providers.includes('google')" class="login" :href="auth.loginUrl('google')" @click="auth.openLogin($event, 'google')">로그인하고 참가하기</a>
          </template>
          <template v-else-if="s.status === 'RUNNING' && s.joined && !detail.eliminated">
            <button v-if="confirm !== 'leave'" type="button" class="ghost" :disabled="busy" @click="confirm = 'leave'">기권</button>
            <button v-else type="button" class="danger" :disabled="busy" @click="leave">정말 기권 (참가비는 돌려받지 못해요)</button>
          </template>
          <span v-else-if="s.status === 'RUNNING' && detail.eliminated" class="out">탈락했어요. 다른 대국을 구경해 보세요.</span>
        </div>
        <p class="rules">
          토너먼트로 지면 탈락해요. 짝이 안 맞으면 레이팅이 높은 사람이 부전승으로 올라가요. 대국이 열리고 1분 안에 첫 수를 두지 않으면 기권으로 처리돼요.
          무승부는 바로 남은 기물 점수로 가려요(한에 덤 1.5점).
        </p>
      </div>

      <div v-if="rounds.length" class="bracket-wrap">
        <h2>대진표</h2>
        <div class="bracket">
          <div v-for="r in rounds" :key="r.round" class="round">
            <h3>{{ r.name }}</h3>
            <div v-for="m in r.matches" :key="m.slot" class="match" :class="{ live: live(m), mine: mine(m) }">
              <div v-for="(p, i) in [m.cho, m.han]" :key="i" class="player" :class="side(m, p)">
                <template v-if="p">
                  <span class="seed">{{ p.seed }}</span>
                  <span class="name">{{ p.nickname }}</span>
                  <span class="grade">{{ p.grade }}</span>
                </template>
                <span v-else class="name bye">{{ m.cho || m.han ? '부전승' : '-' }}</span>
              </div>
              <button v-if="live(m)" type="button" class="watch" @click="emit('game', m.gameId!)">
                {{ mine(m) ? '내 대국' : '구경하기' }}
              </button>
              <span v-else-if="m.gameId && m.winnerId" class="replay-hint">끝남</span>
            </div>
          </div>
        </div>
      </div>

      <div class="entries">
        <h2>참가자 {{ detail.entries.length }}명</h2>
        <p v-if="!detail.entries.length" class="muted">아직 참가자가 없어요.</p>
        <ol v-else>
          <li v-for="e in detail.entries" :key="e.player.id" :class="{ me: me?.id === e.player.id, out: e.out !== null }">
            <span class="rank">{{ e.rank ?? e.player.seed ?? '·' }}</span>
            <span class="who"><b>{{ e.player.nickname }}</b><small>{{ e.player.grade }} · {{ e.player.rating }}</small></span>
            <span v-if="e.rank === 1" class="medal gold">우승</span>
            <span v-else-if="e.rank === 2" class="medal silver">준우승</span>
            <span v-else-if="e.out !== null" class="muted small">{{ roundName(e.out, s.totalRounds) }} 탈락</span>
          </li>
        </ol>
      </div>
    </template>
  </section>
</template>

<style scoped>
.head {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  margin-bottom: 14px;
}
.back-button {
  flex: none;
  padding: 7px 12px;
  border-radius: 10px;
  border: 1px solid var(--border);
  background: var(--surface-strong);
  cursor: pointer;
  font-weight: 600;
}
.title-box {
  min-width: 0;
}
.badges {
  display: flex;
  gap: 6px;
}
h1 {
  margin: 4px 0 0;
  font-size: 1.35rem;
  overflow-wrap: anywhere;
}
h2 {
  margin: 22px 0 8px;
  font-size: 0.8rem;
  font-weight: 700;
  letter-spacing: 0.06em;
  color: var(--muted);
}
.badge {
  font-size: 0.7rem;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: 999px;
  color: var(--accent);
  background: var(--accent-bg);
}
.badge.regular {
  color: #fff;
  background: var(--accent);
}
.badge.hosted {
  color: var(--cho-color);
  background: color-mix(in srgb, var(--cho-color) 12%, transparent);
}
.badge.div {
  color: var(--text);
  background: var(--surface);
  border: 1px solid var(--border);
}
.info {
  padding: 14px;
  border-radius: 14px;
  background: var(--surface-strong);
  border: 1px solid var(--border);
}
.status {
  font-size: 1.05rem;
  font-weight: 800;
  color: var(--accent-deep);
  font-variant-numeric: tabular-nums;
}
.status.running {
  color: var(--han-color);
}
dl {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(130px, 1fr));
  gap: 8px 12px;
  margin: 12px 0 0;
}
dl div {
  display: flex;
  flex-direction: column;
}
dt {
  font-size: 0.72rem;
  color: var(--muted);
}
dd {
  margin: 0;
  font-weight: 700;
  font-size: 0.92rem;
}
.prize dd {
  color: #9a7412;
}
.prize small {
  font-weight: 500;
  color: var(--muted);
}
.desc {
  margin: 12px 0 0;
  white-space: pre-wrap;
  font-size: 0.9rem;
  line-height: 1.5;
}
.actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-top: 14px;
}
.rules {
  margin: 12px 0 0;
  font-size: 0.76rem;
  color: var(--muted);
  line-height: 1.5;
}
.joined {
  font-weight: 700;
  color: var(--cho-color);
}
.out {
  font-size: 0.88rem;
  color: var(--muted);
}
.login {
  font-weight: 700;
  color: var(--accent);
}
.primary,
.ghost,
.danger {
  font: inherit;
  font-size: 0.9rem;
  font-weight: 700;
  padding: 9px 14px;
  border-radius: 10px;
  cursor: pointer;
}
.primary {
  color: #fff;
  background: var(--accent);
  border: none;
  box-shadow: 0 3px 0 var(--accent-deep);
}
.primary.pulse {
  background: var(--han-color);
  box-shadow: 0 3px 0 color-mix(in srgb, var(--han-color) 60%, black);
}
.ghost {
  background: none;
  border: 1px solid var(--border);
}
.danger {
  color: #fff;
  background: var(--han-color);
  border: none;
}
button:disabled {
  opacity: 0.55;
  cursor: default;
}
.bracket-wrap {
  overflow: hidden;
}
.bracket {
  display: flex;
  gap: 12px;
  overflow-x: auto;
  padding-bottom: 6px;
}
.round {
  flex: 0 0 170px;
  display: flex;
  flex-direction: column;
  justify-content: space-around;
  gap: 8px;
}
h3 {
  margin: 0;
  font-size: 0.78rem;
  text-align: center;
  color: var(--muted);
}
.match {
  display: flex;
  flex-direction: column;
  border-radius: 10px;
  border: 1px solid var(--border);
  background: var(--surface-strong);
  overflow: hidden;
}
.match.live {
  border-color: var(--han-color);
}
.match.mine {
  box-shadow: 0 0 0 2px var(--accent-soft);
}
.player {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 5px 8px;
  font-size: 0.82rem;
  border-bottom: 1px solid var(--border);
}
.player.won {
  font-weight: 800;
  background: color-mix(in srgb, var(--cho-color) 10%, transparent);
}
.player.lost {
  opacity: 0.45;
  text-decoration: line-through;
}
.seed {
  flex: none;
  width: 1.4em;
  font-size: 0.7rem;
  color: var(--muted);
  font-variant-numeric: tabular-nums;
}
.name {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.bye {
  color: var(--muted);
  font-style: italic;
}
.grade {
  flex: none;
  font-size: 0.7rem;
  color: var(--muted);
}
.watch {
  font: inherit;
  font-size: 0.76rem;
  font-weight: 700;
  padding: 5px;
  border: none;
  color: #fff;
  background: var(--han-color);
  cursor: pointer;
}
.replay-hint {
  padding: 3px;
  text-align: center;
  font-size: 0.7rem;
  color: var(--muted);
}
.entries ol {
  list-style: none;
  margin: 0;
  padding: 0;
  border-radius: 14px;
  border: 1px solid var(--border);
  background: var(--surface-strong);
  overflow: hidden;
}
.entries li {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 14px;
  border-top: 1px solid var(--border);
}
.entries li:first-child {
  border-top: none;
}
.entries li.me {
  background: var(--accent-bg);
}
.entries li.out .who {
  opacity: 0.6;
}
.rank {
  flex: none;
  width: 1.6em;
  font-weight: 800;
  color: var(--muted);
  font-variant-numeric: tabular-nums;
}
.who {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}
.who small {
  font-size: 0.74rem;
  color: var(--muted);
}
.medal {
  font-size: 0.74rem;
  font-weight: 800;
  padding: 2px 9px;
  border-radius: 999px;
  color: #fff;
}
.medal.gold {
  background: #c9a227;
}
.medal.silver {
  background: #9aa3ad;
}
.muted {
  color: var(--muted);
}
.small {
  font-size: 0.78rem;
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
</style>
