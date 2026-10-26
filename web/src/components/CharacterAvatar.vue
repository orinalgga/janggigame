<script setup lang="ts">
import { computed } from 'vue'
import type { Look } from '../api'
import { characterSvg } from '../character'

/** head면 얼굴만 동그랗게, 아니면 배경 패 위의 상반신. 크기는 감싸는 쪽이 width로 정한다. */
const props = defineProps<{ look?: Partial<Look> | null; head?: boolean }>()

const svg = computed(() => characterSvg(props.look, { head: props.head }))
</script>

<template>
  <!-- 부품 id는 정해진 목록에서만 그림으로 바뀌고, 사용자 글은 들어가지 않는다. -->
  <span class="character" :class="{ head }" aria-hidden="true" v-html="svg" />
</template>

<style scoped>
.character {
  display: inline-block;
  flex: none;
  line-height: 0;
}
.character :deep(svg) {
  width: 100%;
  height: auto;
  display: block;
}
.character.head :deep(svg) {
  border-radius: 50%;
}
</style>
