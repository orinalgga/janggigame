/** 모든 부품이 맞춰 그리는 몸의 뼈대. 좌표는 240×300 캔버스. */
import { capsule, circle, ellipse } from './lib'

/** 얼굴: 가운데 (120, 112), 너비 112, 턱 끝 y 166. */
export const FACE =
  'M64 110 C64 74 88 56 120 56 C152 56 176 74 176 110 C176 142 154 166 120 166 C86 166 64 142 64 110 Z'
export const EARS = [ellipse(63, 118, 7.5, 10.5), ellipse(177, 118, 7.5, 10.5)]
/** 얼굴 그늘: 오른쪽 볼과 턱 밑. */
export const FACE_SHADE = 'M168 84 Q172 140 128 166 L190 190 L190 70 Z M70 138 Q100 170 160 150 L170 200 L60 200 Z'
export const FACE_LIGHT = 'M78 82 Q92 64 112 62 Q92 72 84 92 Z'

export const NECK = 'M110 156 L130 156 L131 178 L109 178 Z'
/** 맨몸 몸통(옷 아래). 어깨 y 174, 허리 y 224. */
export const TORSO = 'M92 176 Q120 168 148 176 Q153 180 152 190 L149 226 L91 226 L88 190 Q87 180 92 176 Z'
export const ARMS = [capsule(91, 180, 79, 215, 7.5, 6.5), capsule(149, 180, 161, 215, 7.5, 6.5)]
export const HANDS = [circle(77, 221, 7.5), circle(163, 221, 7.5)]
export const LEGS = [capsule(110, 222, 109, 258, 8.5, 7.5), capsule(130, 222, 131, 258, 8.5, 7.5)]
export const FEET = [ellipse(107, 264, 11, 7), ellipse(133, 264, 11, 7)]

/** 눈 가운데. */
export const EYE_L = [100, 122] as const
export const EYE_R = [140, 122] as const
