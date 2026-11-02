import { ref } from 'vue'

/** 지금 열려 있는 프로필 창의 회원 id. 어느 화면에서든 openProfile로 연다. */
export const profileId = ref<number | null>(null)

export function openProfile(id: number | null | undefined) {
  if (id) profileId.value = id
}
