<script setup lang="ts">
import { computed } from 'vue'
import type { EndReason, GameDto, Side } from '../api'

const props = defineProps<{
  game: GameDto
  /** 내가 두던 진영. 같은 화면 2인이나 구경이면 null. */
  me: Side | null
  /** '한 판 더'·'새 상대 찾기'·'대회로 돌아가기' 같은 다음 버튼 글. 없으면 버튼을 숨긴다. */
  againLabel: string | null
}>()
const emit = defineEmits<{ (e: 'again'): void; (e: 'close'): void }>()

const REASON: Record<EndReason, string> = {
  CHECKMATE: '외통',
  BIKJANG: '빅장 점수 판정',
  RESIGN: '기권',
  TIMEOUT: '시간패',
  DISCONNECT: '접속 끊김',
}

const winner = computed<Side | null>(() =>
  props.game.result === 'CHO_WIN' ? 'CHO' : props.game.result === 'HAN_WIN' ? 'HAN' : null,
)

/** win·lose·draw, 내 진영이 없으면 초·한 중 이긴 쪽. */
const outcome = computed(() => {
  if (!winner.value) return { kind: 'draw', title: '무승부', mark: '和' }
  if (props.me) return winner.value === props.me ? { kind: 'win', title: '승리!', mark: '勝' } : { kind: 'lose', title: '패배', mark: '敗' }
  return { kind: winner.value === 'CHO' ? 'win' : 'lose', title: `${winner.value === 'CHO' ? '초' : '한'} 승리`, mark: '勝' }
})

const reason = computed(() => {
  const g = props.game
  const how = g.endReason ? REASON[g.endReason] : ''
  if (g.endReason === 'BIKJANG') return `${how} · 초 ${g.choScore}점 : 한 ${g.hanScore}점`
  return how
})

const rating = computed(() => {
  const c = props.game.ratingChange
  if (!c || !props.me) return null
  const d = props.me === 'CHO' ? c.cho : c.han
  return d >= 0 ? `레이팅 +${d}` : `레이팅 ${d}`
})
</script>

<template>
  <div class="result-backdrop" @click.self="emit('close')">
    <div class="result" :class="outcome.kind" role="dialog" aria-modal="true" aria-labelledby="result-title">
      <span class="mark" aria-hidden="true">{{ outcome.mark }}</span>
      <h2 id="result-title">{{ outcome.title }}</h2>
      <p v-if="reason" class="reason">{{ reason }}</p>
      <p v-if="rating" class="rating">{{ rating }}</p>
      <div class="buttons">
        <button v-if="againLabel" type="button" class="primary" @click="emit('again')">{{ againLabel }}</button>
        <button type="button" @click="emit('close')">판 보기</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.result-backdrop {
  position: fixed;
  inset: 0;
  z-index: 40;
  display: grid;
  place-items: center;
  padding: 16px;
  background: rgba(0, 0, 0, 0.45);
  animation: fade-in 0.25s ease-out both;
}
.result {
  width: min(320px, 100%);
  padding: 26px 22px 20px;
  border-radius: 20px;
  text-align: center;
  color: var(--text);
  background: var(--surface-strong);
  box-shadow: 0 20px 50px var(--shadow, rgba(0, 0, 0, 0.3));
  animation: pop 0.4s cubic-bezier(0.2, 1.5, 0.4, 1) both;
  --tone: var(--cho-color);
}
.result.lose {
  --tone: var(--han-color);
}
.result.draw {
  --tone: var(--muted);
}
.mark {
  display: inline-grid;
  place-items: center;
  width: 72px;
  height: 72px;
  border-radius: 50%;
  font-family: 'Noto Serif KR', serif;
  font-size: 2.2rem;
  font-weight: 900;
  color: #fff;
  background: var(--tone);
}
h2 {
  margin: 12px 0 4px;
  font-size: 1.7rem;
  color: var(--tone);
}
.reason {
  margin: 0;
  color: var(--muted);
}
.rating {
  margin: 6px 0 0;
  font-weight: 700;
}
.buttons {
  display: flex;
  gap: 8px;
  justify-content: center;
  margin-top: 18px;
}
.buttons button {
  flex: 1;
  padding: 10px 12px;
  border-radius: 10px;
  border: 1px solid rgba(0, 0, 0, 0.15);
  background: transparent;
  color: var(--text);
  font: inherit;
  cursor: pointer;
}
.buttons .primary {
  border-color: var(--tone);
  color: #fff;
  background: var(--tone);
  font-weight: 700;
}
@keyframes fade-in {
  from {
    opacity: 0;
  }
}
@keyframes pop {
  from {
    opacity: 0;
    transform: scale(0.8);
  }
}
</style>
