<script setup lang="ts">
import { computed } from 'vue'
import { avatarLook } from '../avatars'

const props = withDefaults(defineProps<{ id?: string | null; name?: string; size?: number }>(), {
  id: null,
  name: '',
  size: 28,
})

const look = computed(() => avatarLook(props.id))
const style = computed(() => ({
  width: `${props.size}px`,
  height: `${props.size}px`,
  fontSize: `${Math.round(props.size * (look.value.glyph ? 0.56 : 0.46))}px`,
  color: look.value.fg,
  background: `linear-gradient(160deg, ${look.value.from}, ${look.value.to})`,
  boxShadow: `inset 0 0 0 ${Math.max(1.5, props.size / 18)}px ${look.value.ring}`,
}))
</script>

<template>
  <span class="user-avatar" :class="{ glow: look.glow, glyph: !!look.glyph }" :style="style" aria-hidden="true">
    {{ look.glyph ?? name.slice(0, 1) }}
  </span>
</template>

<style scoped>
.user-avatar {
  flex: none;
  display: inline-grid;
  place-items: center;
  border-radius: 50%;
  font-weight: 700;
  line-height: 1;
  user-select: none;
}
.user-avatar.glyph {
  font-family: 'Noto Serif KR', serif;
  font-weight: 900;
}
.user-avatar.glow {
  filter: drop-shadow(0 0 4px rgba(228, 178, 60, 0.75));
}
</style>
