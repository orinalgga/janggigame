/**
 * 임시 아바타 그림을 만드는 도구. 모든 그림은 240×300 캔버스에 그리고, 선은 LINE 색 SW 굵기로 두른다.
 * 색칠은 바탕색 + 그늘(오른쪽 아래) + 빛(왼쪽 위)의 셀 셰이딩이다.
 */
import fs from 'node:fs'
import path from 'node:path'

export const W = 240
export const H = 300
export const LINE = '#3b2a24'
export const SW = 2.4

/** 색을 섞는다. k가 음수면 검정 쪽, 양수면 흰색 쪽으로. */
export function tone(hex: string, k: number): string {
  const n = parseInt(hex.slice(1), 16)
  const c = [(n >> 16) & 255, (n >> 8) & 255, n & 255].map((v) => Math.round(k < 0 ? v * (1 + k) : v + (255 - v) * k))
  return '#' + c.map((v) => v.toString(16).padStart(2, '0')).join('')
}

let clipId = 0

export interface Shape {
  d: string
  fill: string
  /** 그늘 모양(이 도형 안으로 잘린다). 없으면 오른쪽 아래를 자동으로 칠한다. 빈 문자열이면 그늘 없음. */
  shade?: string
  /** 빛 모양(이 도형 안으로 잘린다). */
  light?: string
  /** 선 굵기. 0이면 선 없음. */
  sw?: number
  /** 자동 그늘을 칠할 때 도형이 차지하는 대략의 범위 [x, y, w, h]. */
  box?: [number, number, number, number]
}

/** 도형 하나: 바탕 → 그늘 → 빛 → 테두리. */
export function shape(s: Shape): string {
  const id = `c${++clipId}`
  const shadeD = s.shade ?? (s.box ? autoShade(s.box) : '')
  const sw = s.sw ?? SW
  const inner =
    shadeD || s.light
      ? `<clipPath id="${id}"><path d="${s.d}"/></clipPath><g clip-path="url(#${id})">` +
        (shadeD ? `<path d="${shadeD}" fill="${tone(s.fill, -0.12)}"/>` : '') +
        (s.light ? `<path d="${s.light}" fill="${tone(s.fill, 0.35)}" opacity="0.8"/>` : '') +
        `</g>`
      : ''
  return (
    `<path d="${s.d}" fill="${s.fill}"/>` +
    inner +
    (sw ? `<path d="${s.d}" fill="none" stroke="${LINE}" stroke-width="${sw}" stroke-linejoin="round" stroke-linecap="round"/>` : '')
  )
}

/** 범위의 오른쪽 아래를 비스듬히 덮는 그늘. */
export function autoShade([x, y, w, h]: [number, number, number, number]): string {
  return `M${x + w * 0.72} ${y - 4} Q${x + w * 0.6} ${y + h * 0.6} ${x + w * 0.2} ${y + h + 6} L${x + w + 6} ${y + h + 6} L${x + w + 6} ${y - 4} Z`
}

/** 두 점을 잇는 둥근 막대(팔·다리·소매). */
export function capsule(x1: number, y1: number, x2: number, y2: number, r1: number, r2 = r1): string {
  const a = Math.atan2(y2 - y1, x2 - x1)
  const nx = Math.cos(a + Math.PI / 2)
  const ny = Math.sin(a + Math.PI / 2)
  const p = (x: number, y: number, r: number, s: number) => `${(x + nx * r * s).toFixed(1)} ${(y + ny * r * s).toFixed(1)}`
  return `M${p(x1, y1, r1, 1)} L${p(x2, y2, r2, 1)} A${r2} ${r2} 0 0 1 ${p(x2, y2, r2, -1)} L${p(x1, y1, r1, -1)} A${r1} ${r1} 0 0 1 ${p(x1, y1, r1, 1)} Z`
}

export const ellipse = (cx: number, cy: number, rx: number, ry: number) =>
  `M${cx - rx} ${cy} A${rx} ${ry} 0 1 0 ${cx + rx} ${cy} A${rx} ${ry} 0 1 0 ${cx - rx} ${cy} Z`

export const circle = (cx: number, cy: number, r: number) => ellipse(cx, cy, r, r)

/** 좌우를 뒤집은 경로(가운데 x = 120). 숫자 쌍의 x만 뒤집으므로 M/L/Q/C 같은 절대 좌표 명령에만 쓴다. */
export function mirror(d: string): string {
  let i = 0
  return d.replace(/-?\d+(\.\d+)?/g, (v) => (i++ % 2 === 0 ? String(+(240 - +v).toFixed(2)) : v))
}

export function svg(body: string): string {
  return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${W} ${H}" width="${W}" height="${H}">${body}</svg>\n`
}

/** web 폴더에서 돌린다(npm run avatar:parts). */
const ROOT = path.resolve('src/avatar/parts')

/** parts/<상품 id>/<층>.svg로 쓴다. 이미 작가 그림(png, webp)이 있으면 임시 그림을 쓰지 않는다. */
export function write(item: string, layer: string, body: string) {
  const dir = path.join(ROOT, item)
  fs.mkdirSync(dir, { recursive: true })
  if (['png', 'webp'].some((e) => fs.existsSync(path.join(dir, `${layer}.${e}`)))) return
  fs.writeFileSync(path.join(dir, `${layer}.svg`), svg(body))
}
