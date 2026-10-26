import type { Look } from './api'

/**
 * 인형 화풍. 큰 머리에 2등신인 작은 인형을 그린다.
 * 몸(남녀·은발), 머리, 수염, 옷, 모자·소품, 배경을 40×50 칸에 겹쳐 칠한 뒤 4배로 늘리면서
 * 계단진 모서리를 둥글게 다듬고(EPX), 인형 둘레에 얇은 테두리를 두른다. 눈·입·볼은 늘린 뒤에 매끈하게 그린다.
 * 브라우저에서는 PNG 한 장으로 구워 같은 차림이면 다시 쓰고, 캔버스가 없으면 칸 그대로 SVG로 낸다.
 */
// ---- 픽셀 칸 ----

const W = 40
const H = 50
const UNIT = 3 // 한 칸이 SVG에서 차지하는 크기(120×150)
const SCALE = 4 // 다듬을 때 늘리는 배수
const LINE = '#3a2a24'

/** 칸 한 겹. 가운데 줄은 x 19와 20 사이다. */
class Layer {
  readonly px: (string | undefined)[]

  constructor(
    readonly w = W,
    readonly h = H,
  ) {
    this.px = new Array(w * h)
  }

  dot(x: number, y: number, c: string) {
    if (x >= 0 && x < this.w && y >= 0 && y < this.h) this.px[y * this.w + x] = c
  }

  get(x: number, y: number) {
    return x >= 0 && x < this.w && y >= 0 && y < this.h ? this.px[y * this.w + x] : undefined
  }

  fill(x: number, y: number, w: number, h: number, c: string) {
    for (let j = 0; j < h; j++) for (let i = 0; i < w; i++) this.dot(x + i, y + j, c)
  }

  clear(x: number, y: number, w: number, h: number) {
    for (let j = 0; j < h; j++)
      for (let i = 0; i < w; i++) if (x + i >= 0 && x + i < this.w && y + j >= 0 && y + j < this.h) this.px[(y + j) * this.w + x + i] = undefined
  }

  ellipse(cx: number, cy: number, rx: number, ry: number, c: string) {
    for (let y = Math.floor(cy - ry); y <= cy + ry; y++)
      for (let x = Math.floor(cx - rx); x <= cx + rx; x++) if (((x + 0.5 - cx) / rx) ** 2 + ((y + 0.5 - cy) / ry) ** 2 <= 1) this.dot(x, y, c)
  }

  /** 가운데 줄을 기준으로 좌우가 같은 줄들. halves[i]는 y0+i 줄의 반 너비. */
  sym(y0: number, halves: number[], c: string) {
    halves.forEach((hw, i) => hw > 0 && this.fill(20 - hw, y0 + i, hw * 2, 1, c))
  }

  /** 가운데에서 dx만큼 떨어진 좌우 한 쌍. */
  pair(dx: number, y: number, c: string) {
    this.dot(19 - dx, y, c)
    this.dot(20 + dx, y, c)
  }

  circle(cx: number, cy: number, r: number, c: string) {
    for (let y = Math.floor(cy - r); y <= cy + r; y++)
      for (let x = Math.floor(cx - r); x <= cx + r; x++)
        if ((x - cx) ** 2 + (y - cy) ** 2 <= r * r + r * 0.6) this.dot(x, y, c)
  }

  line(x0: number, y0: number, x1: number, y1: number, c: string) {
    const n = Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0), 1)
    for (let i = 0; i <= n; i++) this.dot(Math.round(x0 + ((x1 - x0) * i) / n), Math.round(y0 + ((y1 - y0) * i) / n), c)
  }

  /** 이 겹에서 color로 칠한 칸 가운데 test를 만족하는 곳에 무늬를 찍는다. */
  pattern(on: Layer, color: string, test: (x: number, y: number) => boolean, c: string) {
    for (let y = 0; y < this.h; y++) for (let x = 0; x < this.w; x++) if (on.get(x, y) === color && test(x, y)) this.dot(x, y, c)
  }

  /** 칠한 칸 바깥 둘레에 테두리를 두른다. */
  outline(c: string) {
    const edge: number[] = []
    for (let y = 0; y < this.h; y++)
      for (let x = 0; x < this.w; x++)
        if (!this.get(x, y) && (this.get(x - 1, y) || this.get(x + 1, y) || this.get(x, y - 1) || this.get(x, y + 1))) edge.push(y * this.w + x)
    for (const i of edge) this.px[i] = c
  }

  /** 같은 색이 이어진 가로 줄을 사각형 하나로 묶는다. */
  svg(): string {
    const out: string[] = []
    for (let y = 0; y < this.h; y++) {
      let x = 0
      while (x < this.w) {
        const c = this.px[y * this.w + x]
        let e = x + 1
        while (e < this.w && this.px[y * this.w + e] === c) e++
        if (c) out.push(`<rect x="${x}" y="${y}" width="${e - x}" height="1" fill="${c}"/>`)
        x = e
      }
    }
    return out.join('')
  }

  /** 같은 색이 이어진 가로 줄을 캔버스에 칠한다. */
  paint(ctx: CanvasRenderingContext2D) {
    for (let y = 0; y < this.h; y++) {
      let x = 0
      while (x < this.w) {
        const c = this.px[y * this.w + x]
        let e = x + 1
        while (e < this.w && this.px[y * this.w + e] === c) e++
        if (c) {
          ctx.fillStyle = c
          ctx.fillRect(x, y, e - x, 1)
        }
        x = e
      }
    }
  }
}

/** EPX(Scale2x): 2배로 늘리면서 대각선 계단을 메워 모서리를 둥글게 한다. */
function epx(src: Layer): Layer {
  const out = new Layer(src.w * 2, src.h * 2)
  for (let y = 0; y < src.h; y++)
    for (let x = 0; x < src.w; x++) {
      const p = src.get(x, y)
      const a = src.get(x, y - 1)
      const b = src.get(x + 1, y)
      const c = src.get(x - 1, y)
      const d = src.get(x, y + 1)
      const set = (dx: number, dy: number, v: string | undefined) => {
        if (v) out.dot(x * 2 + dx, y * 2 + dy, v)
      }
      set(0, 0, c === a && c !== d && a !== b ? a : p)
      set(1, 0, a === b && a !== c && b !== d ? b : p)
      set(0, 1, d === c && d !== b && c !== a ? c : p)
      set(1, 1, b === d && b !== a && d !== c ? d : p)
    }
  return out
}

const smooth = (l: Layer) => epx(epx(l))

/** 그림 한 장: 배경(bg), 테두리를 두를 인형(doll), 그 위 잔무늬(ink), 눈·입 위에 얹는 안경(top). */
interface Paint {
  bg: Layer
  doll: Layer
  ink: Layer
  top: Layer
}

// ---- 몸과 얼굴 ----

interface Body {
  skin: string
  blush: string
  hair: string
  woman: boolean
  elder: boolean
}

const BODIES: Record<string, Body> = {
  'body.man': { skin: '#ffe0c2', blush: '#f7b1a3', hair: '#2b211d', woman: false, elder: false },
  'body.woman': { skin: '#ffe6d3', blush: '#f7a3a6', hair: '#3a2620', woman: true, elder: false },
  'body.elder': { skin: '#fbd9b9', blush: '#f2ae9c', hair: '#c9c4bc', woman: false, elder: true },
  'body.elder.woman': { skin: '#ffe2cc', blush: '#f2a8a2', hair: '#cdc6c2', woman: true, elder: true },
}

/** 머리는 y 9~25, 가장 넓은 곳이 x 10~29. */
const HEAD = [5, 7, 8, 9, 10, 10, 10, 10, 10, 10, 10, 10, 10, 9, 9, 8, 6]

function head(p: Paint, b: Body) {
  p.doll.fill(18, 26, 4, 1, b.skin) // 목
  p.doll.sym(9, HEAD, b.skin)
  p.doll.fill(9, 17, 1, 3, b.skin) // 귀
  p.doll.fill(30, 17, 1, 3, b.skin)
}

/** 눈·볼·입은 늘린 칸(hi)에 매끈하게 그린다. 좌표는 원래 칸 기준. */
function face(hi: Layer, b: Body) {
  const k = SCALE
  const eye = '#2a1d18'
  hi.ellipse(12.6 * k, 20.6 * k, 1.5 * k, 0.75 * k, b.blush)
  hi.ellipse(27.4 * k, 20.6 * k, 1.5 * k, 0.75 * k, b.blush)
  for (const cx of [15.5, 24.5]) {
    if (b.elder) {
      // 웃는 눈
      for (let t = -1; t <= 1; t += 0.05) hi.ellipse((cx + t * 1.3) * k, (18.4 - (1 - t * t) * 0.8) * k, 0.32 * k, 0.32 * k, eye)
    } else {
      hi.ellipse(cx * k, 18 * k, 0.62 * k, 0.95 * k, eye)
      hi.ellipse((cx - 0.2) * k, 17.6 * k, 0.2 * k, 0.2 * k, '#ffffff')
      if (b.woman) hi.ellipse((cx + (cx < 20 ? -0.75 : 0.75)) * k, 17.05 * k, 0.32 * k, 0.22 * k, eye)
    }
  }
  if (b.woman) hi.ellipse(20 * k, 21.6 * k, 0.8 * k, 0.45 * k, '#d9473f')
  else hi.fill(19.3 * k, 21.4 * k, 1.4 * k, 0.4 * k, '#c46a5a')
}

// ---- 옷 ----

type Cut = 'top' | 'pants' | 'dress' | 'robe'

interface Outfit {
  /** top: 남자는 바지, 여자는 치마(한복). pants: 누구나 바지. dress: 누구나 치마. robe: 발목까지 오는 겉옷. */
  cut: Cut
  coat: string
  sleeve?: string
  lower: string
  shoe?: string
  /** 목둘레: v는 한복 깃, round는 둥근 깃, crew는 티셔츠, shirt는 셔츠 깃. */
  neck: 'v' | 'round' | 'crew' | 'shirt'
  collar: string
  ribbon?: string
  belt?: string
  /** 여자 한복 저고리가 길면(당의) 치마가 늦게 시작한다. */
  longJacket?: boolean
  woman?: Partial<Omit<Outfit, 'woman'>>
  deco?: (p: Paint, woman: boolean) => void
  back?: (p: Paint) => void
}

const GOLD = '#e8b83f'
const SHOE = '#3b302c'

const OUTFITS: Record<string, Outfit> = {
  'outfit.tee': {
    cut: 'pants', coat: '#fbfaf5', lower: '#4f73a8', neck: 'crew', collar: '#e9e6dc', shoe: '#f2f2ee',
    deco: (p) => {
      p.ink.fill(17, 31, 6, 1, '#e3e0d6')
      p.ink.pair(1, 38, '#3f5f8f')
    },
  },
  'outfit.hoodie': {
    cut: 'pants', coat: '#8fa3bd', lower: '#3c414d', neck: 'crew', collar: '#7d90a8', shoe: '#f2f2ee',
    back: (p) => p.doll.sym(23, [8, 8, 8, 7], '#8fa3bd'),
    deco: (p) => {
      p.ink.fill(18, 28, 1, 3, '#ffffff')
      p.ink.fill(21, 28, 1, 3, '#ffffff')
      p.ink.fill(16, 32, 8, 1, '#7d90a8')
    },
  },
  'outfit.knit': {
    cut: 'pants', coat: '#dcc59f', lower: '#5c5149', neck: 'shirt', collar: '#ffffff', shoe: '#5a3a2a',
    deco: (p) => {
      p.ink.pattern(p.doll, '#dcc59f', (x, y) => x % 2 === 0 && y > 28, '#ccb38a')
      p.ink.fill(14, 33, 12, 1, '#ccb38a')
    },
  },
  'outfit.suit': {
    cut: 'pants', coat: '#2e3648', lower: '#2e3648', neck: 'v', collar: '#1f2533', shoe: '#1e1a18',
    deco: (p) => {
      p.ink.fill(19, 28, 2, 4, '#b8323a')
      p.ink.fill(19, 28, 2, 1, '#8f2229')
      p.ink.dot(23, 30, '#ffffff')
    },
  },
  'outfit.dress': {
    cut: 'dress', coat: '#e2808f', lower: '#e2808f', neck: 'round', collar: '#ffffff', shoe: '#f2f2ee', belt: '#c25a6c',
    deco: (p) => p.ink.pattern(p.doll, '#e2808f', (x, y) => y > 34 && (x + y) % 4 === 0, '#f2b6c0'),
  },
  'outfit.golf': {
    cut: 'pants', coat: '#ffffff', lower: '#e6d8b8', neck: 'shirt', collar: '#2c4a7c', shoe: '#ffffff', belt: '#3a2f2a',
    deco: (p) => {
      p.ink.fill(14, 30, 12, 1, '#2c4a7c')
      p.ink.fill(14, 31, 12, 1, '#d9483b')
      p.ink.fill(26, 35, 2, 2, '#ffffff')
    },
  },
  'outfit.plain': {
    cut: 'top', coat: '#f6f0e2', lower: '#e8dcc6', neck: 'v', collar: '#d8ccb3', ribbon: '#d8ccb3', shoe: '#f2f2ee',
    woman: { coat: '#f6d55e', collar: '#d2403a', ribbon: '#d2403a', lower: '#d2403a' },
  },
  'outfit.durumagi': { cut: 'robe', coat: '#8b5a86', lower: '#8b5a86', neck: 'v', collar: '#f6f0e2', ribbon: '#6d4069' },
  'outfit.dopo': {
    cut: 'robe', coat: '#cfe4ea', lower: '#cfe4ea', neck: 'v', collar: '#2f3550', belt: '#3d5aa8',
    deco: (p) => p.ink.fill(23, 35, 1, 4, '#3d5aa8'),
  },
  'outfit.cheollik': {
    cut: 'robe', coat: '#2d3d63', lower: '#2d3d63', neck: 'v', collar: '#f6f0e2', belt: '#c0392b',
    deco: (p) => p.ink.pattern(p.doll, '#2d3d63', (x, y) => y > 35 && x % 2 === 1, '#22304f'),
  },
  'outfit.dangui': {
    cut: 'top', coat: '#3f8a57', lower: '#e8dcc6', neck: 'v', collar: '#7b2d6e', ribbon: '#7b2d6e', longJacket: true,
    woman: { lower: '#b8323a' },
    deco: (p) => p.ink.pattern(p.doll, '#3f8a57', (x, y) => (x + y * 2) % 4 === 0, GOLD),
  },
  'outfit.gwanbok': {
    cut: 'robe', coat: '#b8352f', lower: '#b8352f', neck: 'round', collar: '#7c1f1b', belt: GOLD,
    deco: (p) => {
      p.ink.fill(17, 30, 6, 4, '#283a6b')
      p.ink.fill(18, 31, 1, 1, '#ffffff')
      p.ink.fill(21, 31, 1, 1, '#ffffff')
      p.ink.fill(19, 32, 2, 1, '#ffffff')
    },
  },
  'outfit.armor': {
    cut: 'robe', coat: '#8e2b27', sleeve: '#3b3b42', lower: '#8e2b27', neck: 'v', collar: GOLD, belt: '#2b2b30',
    deco: (p) => {
      p.ink.pattern(p.doll, '#8e2b27', (x, y) => x % 2 === 0 && y % 2 === 1, GOLD)
      p.ink.pattern(p.doll, '#3b3b42', (x, y) => (x + y) % 2 === 0, GOLD)
      p.ink.fill(19, 31, 2, 2, '#dfe6ea')
    },
  },
  'outfit.wonsam': {
    cut: 'robe', coat: '#2f7a50', lower: '#2f7a50', neck: 'v', collar: '#c0392b', belt: '#c0392b',
    deco: (p) => {
      p.ink.pattern(p.doll, '#2f7a50', (x, y) => (x + y * 2) % 4 === 0, GOLD)
      for (const [y, c] of [[32, '#d2403a'], [33, '#f2c84b'], [34, '#3d6fb8']] as const) {
        p.ink.fill(12, y, 2, 1, c)
        p.ink.fill(26, y, 2, 1, c)
      }
    },
  },
  'outfit.gonryongpo': {
    cut: 'robe', coat: '#c0282d', lower: '#c0282d', neck: 'round', collar: '#7a1418', belt: '#d8e4cf',
    deco: (p) => {
      p.ink.sym(29, [1, 2, 2, 2, 1], GOLD)
      p.ink.fill(19, 31, 2, 1, '#c0282d')
      p.ink.dot(13, 28, GOLD)
      p.ink.dot(26, 28, GOLD)
    },
  },
}

function dress(p: Paint, b: Body, base: Outfit) {
  const o = b.woman && base.woman ? { ...base, ...base.woman } : base
  const d = p.doll
  const shoe = o.shoe ?? SHOE
  const cut: Cut = o.cut === 'top' ? (b.woman ? 'dress' : 'pants') : o.cut
  let ribbonY = 31
  if (cut === 'robe') {
    d.fill(15, 42, 4, 2, shoe)
    d.fill(21, 42, 4, 2, shoe)
    d.sym(27, [5, 6, 6, 6, 6, 6, 6, 7, 7, 7, 7, 7, 7, 8, 8], o.coat)
    p.ink.fill(20, 32, 1, 10, '#00000022')
  } else if (cut === 'dress') {
    const jacket = o.cut === 'top' ? (o.longJacket ? 6 : 4) : 6
    d.fill(16, 43, 3, 1, shoe)
    d.fill(21, 43, 3, 1, shoe)
    const skirt = [6, 7, 7, 7, 8, 8, 8, 9, 9, 9, 10, 10, 10]
    d.sym(27 + jacket, skirt.slice(0, 16 - jacket), o.lower)
    d.sym(27, [5, 6, 6, 6, 6, 6].slice(0, jacket), o.coat)
    ribbonY = 27 + jacket - 2
  } else {
    d.fill(14, 41, 5, 2, shoe)
    d.fill(21, 41, 5, 2, shoe)
    d.sym(34, [6, 6, 6, 6, 6, 6, 6], o.lower)
    d.clear(19, 37, 2, 4) // 가랑이
    d.sym(27, [5, 6, 6, 6, 6, 6, 6], o.coat)
  }
  // 소매와 손
  d.fill(12, 28, 2, 7, o.sleeve ?? o.coat)
  d.fill(26, 28, 2, 7, o.sleeve ?? o.coat)
  d.fill(12, 35, 2, 2, b.skin)
  d.fill(26, 35, 2, 2, b.skin)

  // 목둘레
  const ink = p.ink
  if (o.neck === 'v') {
    ink.fill(17, 27, 6, 1, '#fbf7ee')
    ink.fill(18, 28, 4, 1, '#fbf7ee')
    ink.fill(19, 29, 2, 1, '#fbf7ee')
    for (let i = 0; i < 4; i++) ink.pair(3 - i, 27 + i, o.collar)
  } else if (o.neck === 'round') {
    ink.fill(17, 27, 6, 1, '#fbf7ee')
    ink.pair(3, 27, o.collar)
    ink.pair(2, 28, o.collar)
    ink.fill(18, 29, 4, 1, o.collar)
    ink.fill(18, 28, 4, 1, '#fbf7ee')
  } else if (o.neck === 'crew') {
    ink.fill(18, 27, 4, 1, b.skin)
    ink.fill(17, 27, 1, 1, o.collar)
    ink.fill(22, 27, 1, 1, o.collar)
    ink.fill(18, 28, 4, 1, o.collar)
  } else {
    ink.fill(18, 27, 4, 1, b.skin)
    ink.pair(2, 27, o.collar)
    ink.pair(3, 27, o.collar)
    ink.pair(1, 28, o.collar)
    ink.pair(2, 28, o.collar)
  }
  if (o.ribbon) {
    ink.fill(21, ribbonY, 2, 1, o.ribbon)
    ink.fill(21, ribbonY + 1, 1, 3, o.ribbon)
    ink.fill(23, ribbonY + 1, 1, 2, o.ribbon)
  }
  if (o.belt) {
    if (cut === 'robe') ink.fill(13, 34, 14, 1, o.belt)
    else if (cut === 'dress') ink.fill(14, 32, 12, 1, o.belt)
    else ink.fill(14, 34, 12, 1, o.belt)
  }
  o.deco?.(p, b.woman)
}

// ---- 머리 ----

interface Hair {
  back?: (p: Paint, c: string) => void
  front: (p: Paint, c: string) => void
}

/** 정수리를 덮는 머리. n줄(y 8부터). */
const cap = (p: Paint, c: string, n = 6) => p.doll.sym(8, [4, 7, 8, 9, 10, 10].slice(0, n), c)

const HAIRS: Record<string, Hair> = {
  'hair.short': {
    front: (p, c) => {
      cap(p, c, 5)
      p.doll.fill(10, 13, 6, 1, c)
      p.doll.fill(24, 13, 6, 1, c)
      p.doll.fill(10, 14, 1, 3, c)
      p.doll.fill(29, 14, 1, 3, c)
    },
  },
  'hair.bob': {
    back: (p, c) => p.doll.sym(14, [11, 11, 11, 11, 11, 11, 11, 11, 11, 10], c),
    front: (p, c) => {
      cap(p, c)
      p.doll.fill(11, 14, 18, 1, c)
      p.doll.fill(9, 14, 2, 9, c)
      p.doll.fill(29, 14, 2, 9, c)
    },
  },
  'hair.perm': {
    // 뽀글뽀글한 파마: 2칸짜리 몽우리를 둘레에 붙인다.
    back: (p, c) => {
      p.doll.sym(7, [6, 9, 10, 11, 11, 11, 11, 11, 11, 11, 11, 11, 10, 9], c)
      for (const y of [9, 13, 17]) {
        p.doll.fill(7, y, 2, 2, c)
        p.doll.fill(31, y, 2, 2, c)
      }
      for (const x of [13, 18, 23]) p.doll.fill(x, 5, 3, 2, c)
    },
    front: (p, c) => {
      cap(p, c)
      for (const x of [11, 16, 21, 26]) p.doll.fill(x, 14, 3, 1, c)
      p.ink.pattern(p.doll, c, (x, y) => y < 14 && (x * 2 + y * 3) % 7 === 0, '#00000030')
    },
  },
  'hair.long': {
    back: (p, c) => p.doll.sym(12, [11, 11, 11, 11, 11, 11, 11, 11, 11, 11, 11, 11, 11, 11, 11, 11, 11, 11, 11, 11, 11, 10, 9], c),
    front: (p, c) => {
      cap(p, c)
      p.doll.fill(10, 14, 7, 1, c)
      p.doll.fill(10, 15, 4, 1, c)
      p.doll.fill(9, 14, 2, 14, c)
      p.doll.fill(29, 14, 2, 14, c)
    },
  },
  'hair.sangtu': {
    front: (p, c) => {
      p.doll.sym(4, [1, 2, 2, 1], c)
      cap(p, c, 4)
      p.doll.sym(12, [10], '#16110f')
      p.doll.fill(10, 13, 1, 4, c)
      p.doll.fill(29, 13, 1, 4, c)
      p.ink.pair(9, 12, GOLD)
      p.ink.fill(17, 6, 6, 1, GOLD)
    },
  },
  'hair.jjok': {
    back: (p, c) => {
      p.doll.fill(30, 18, 3, 1, c)
      p.doll.fill(29, 19, 5, 4, c)
      p.doll.fill(30, 23, 3, 1, c)
    },
    front: (p, c) => {
      cap(p, c)
      p.doll.fill(10, 14, 2, 7, c)
      p.doll.fill(28, 14, 2, 7, c)
      p.doll.fill(12, 14, 3, 1, c)
      p.doll.fill(25, 14, 3, 1, c)
      p.ink.fill(19, 9, 1, 4, '#ffe6d3')
    },
  },
  'hair.loose': {
    back: (p, c) => p.doll.sym(12, [11, 11, 11, 11, 11, 11, 11, 11, 11, 11, 11, 11, 11, 11, 11, 11, 10, 9], c),
    front: (p, c) => {
      cap(p, c)
      for (let x = 11; x < 29; x++) if (x % 3 !== 0) p.doll.dot(x, 14, c)
      p.doll.fill(9, 14, 2, 10, c)
      p.doll.fill(29, 14, 2, 10, c)
      p.ink.sym(11, [9], '#c0392b')
      p.ink.fill(29, 11, 3, 1, '#c0392b')
      p.ink.fill(31, 12, 1, 3, '#c0392b')
    },
  },
  'hair.gache': {
    back: (p, c) => {
      p.doll.sym(1, [5, 8, 10, 11, 12, 12, 12, 11, 10], c)
      p.ink.pattern(p.doll, c, (x, y) => y < 9 && (x + y) % 2 === 0, '#00000038')
    },
    front: (p, c) => {
      cap(p, c)
      p.doll.fill(10, 14, 2, 6, c)
      p.doll.fill(28, 14, 2, 6, c)
      p.ink.fill(19, 9, 1, 4, '#ffe6d3')
    },
  },
}

// ---- 수염 ----

const BEARDS: Record<string, (p: Paint, c: string) => void> = {
  'beard.none': () => {},
  'beard.mustache': (p, c) => {
    p.ink.fill(16, 20, 3, 1, c)
    p.ink.fill(21, 20, 3, 1, c)
    p.ink.pair(4, 21, c)
  },
  'beard.scholar': (p, c) => {
    BEARDS['beard.mustache'](p, c)
    p.ink.fill(19, 23, 2, 4, c)
  },
  'beard.full': (p, c) => {
    p.ink.fill(15, 20, 4, 1, c)
    p.ink.fill(21, 20, 4, 1, c)
    p.ink.fill(10, 19, 2, 3, c)
    p.ink.fill(28, 19, 2, 3, c)
    p.ink.sym(22, [9, 9, 8, 6, 3], c)
  },
  'beard.sage': (p, c) => {
    p.ink.fill(15, 20, 4, 1, c)
    p.ink.fill(21, 20, 4, 1, c)
    p.ink.pair(5, 21, c)
    p.ink.pair(6, 22, c)
    p.ink.sym(22, [2, 2, 2, 1, 1, 1, 1, 1], c)
  },
}

// ---- 모자·소품 ----

const HATS: Record<string, (p: Paint) => void> = {
  'hat.none': () => {},
  'hat.cap': (p) => {
    p.doll.sym(5, [5, 7, 8, 9, 10, 10, 10], '#2f4f8f')
    p.doll.fill(7, 12, 18, 1, '#233c6e')
    p.ink.fill(19, 8, 2, 2, '#ffffff')
  },
  'hat.beanie': (p) => {
    p.doll.sym(2, [2, 2], '#ffffff')
    p.doll.sym(4, [4, 6, 8, 9, 10, 10, 10, 11, 11], '#c7473f')
    p.ink.pattern(p.doll, '#c7473f', (x, y) => y >= 11 && x % 2 === 0, '#a8382f')
  },
  'hat.glasses': (p) => {
    const f = '#2a2220'
    for (const x of [13, 22]) {
      p.top.fill(x, 16, 5, 1, f)
      p.top.fill(x, 19, 5, 1, f)
      p.top.fill(x, 16, 1, 4, f)
      p.top.fill(x + 4, 16, 1, 4, f)
    }
    p.top.fill(18, 17, 4, 1, f)
  },
  'hat.sunglasses': (p) => {
    p.top.fill(13, 17, 5, 2, '#1c1a1e')
    p.top.fill(22, 17, 5, 2, '#1c1a1e')
    p.top.fill(18, 17, 4, 1, '#1c1a1e')
    p.top.dot(14, 17, '#7d8aa0')
    p.top.dot(23, 17, '#7d8aa0')
  },
  'hat.gat': (p) => {
    p.doll.sym(2, [3, 4, 4, 4, 4, 4], '#2a2a30')
    p.doll.sym(8, [13], '#2a2a30')
    const bead = '#d39a3a'
    for (const y of [16, 18, 20]) p.ink.pair(10, y, bead)
    p.ink.pair(9, 22, bead)
    p.ink.pair(7, 24, bead)
    p.ink.pair(5, 26, bead)
  },
  'hat.binyeo': (p) => {
    p.doll.fill(26, 20, 9, 1, GOLD)
    p.doll.fill(34, 19, 2, 3, '#6fbf9f')
  },
  'hat.jeongjagwan': (p) => {
    const c = '#1e1a1e'
    p.doll.sym(1, [4, 5, 5, 6, 6, 7, 8, 9, 10], c)
    p.ink.line(15, 4, 19, 9, '#4a4248')
    p.ink.line(24, 4, 20, 9, '#4a4248')
  },
  'hat.jokduri': (p) => {
    p.doll.sym(4, [2, 3, 4, 4], '#1e1a1e')
    p.ink.fill(19, 4, 2, 1, '#d2403a')
    p.ink.pair(2, 5, '#3d6fb8')
    p.ink.fill(18, 6, 4, 1, GOLD)
    p.doll.fill(15, 8, 1, 3, '#d2403a')
    p.doll.fill(24, 8, 1, 3, '#d2403a')
  },
  'hat.samo': (p) => {
    const c = '#1e1a1e'
    p.doll.sym(4, [5, 7, 8, 9, 9, 10, 10, 10], c)
    p.doll.fill(3, 8, 8, 2, c)
    p.doll.fill(29, 8, 8, 2, c)
    p.doll.fill(4, 7, 4, 1, c)
    p.doll.fill(32, 7, 4, 1, c)
  },
  'hat.jeonrip': (p) => {
    p.doll.sym(0, [1, 2, 2], '#d23a2f')
    p.doll.sym(3, [3, 4, 5, 5, 5], '#2b2420')
    p.doll.sym(8, [12], '#2b2420')
    p.ink.line(25, 4, 31, 1, '#3f9a7a')
    p.ink.dot(32, 1, GOLD)
  },
  'hat.tteoljam': (p) => {
    for (const [x, y, c] of [[13, 4, '#d2403a'], [20, 2, '#3d6fb8'], [26, 4, '#3f9a7a']] as const) {
      p.doll.fill(x - 1, y, 3, 1, c)
      p.doll.fill(x, y - 1, 1, 3, c)
      p.doll.dot(x, y, GOLD)
    }
  },
  'hat.ikseon': (p) => {
    const c = '#1e1a1e'
    p.doll.sym(1, [3, 4, 4], c)
    p.doll.sym(4, [6, 7, 8, 9, 9, 10, 10], c)
    p.ink.sym(3, [4], GOLD)
    p.ink.sym(11, [10], GOLD)
  },
}

// ---- 배경 ----

/** 바닥이 있는 방. */
function room(p: Paint, wall: string, floor: string, plank: string) {
  const b = p.bg
  b.fill(0, 0, W, 38, wall)
  b.fill(0, 38, W, 12, floor)
  b.fill(0, 38, W, 1, plank)
  for (const y of [42, 46]) b.fill(0, y, W, 1, plank)
  for (let x = 4; x < W; x += 9) b.fill(x, 39, 1, 3, plank)
}

const sparkle = (b: Layer, x: number, y: number, c: string) => {
  b.dot(x, y, c)
  b.dot(x - 1, y, c)
  b.dot(x + 1, y, c)
  b.dot(x, y - 1, c)
  b.dot(x, y + 1, c)
}

const peak = (b: Layer, cx: number, top: number, bottom: number, c: string) => {
  for (let y = top; y <= bottom; y++) b.fill(cx - (y - top), y, (y - top) * 2 + 1, 1, c)
}

const pine = (b: Layer, x: number) => {
  b.fill(x + 3, 12, 2, 28, '#7a4f34')
  for (const [dy, w] of [[8, 6], [10, 9], [15, 11], [20, 9], [25, 11]]) b.fill(x + 4 - Math.floor(w / 2), dy, w, 3, '#2f6b45')
}

const crane = (b: Layer, x: number, y: number) => {
  b.fill(x, y + 1, 7, 2, '#ffffff')
  b.fill(x + 2, y, 3, 1, '#ffffff')
  b.fill(x - 2, y + 1, 2, 1, '#2a2a2a')
  b.fill(x + 7, y, 2, 1, '#ffffff')
  b.dot(x + 8, y - 1, '#d2403a')
}

const BACKDROPS: Record<string, (p: Paint) => void> = {
  'avatar.basic': (p) => {
    room(p, '#fbf3e4', '#dcb98c', '#c9a57a')
    p.bg.pattern(p.bg, '#fbf3e4', (x, y) => x % 6 === 0 && y % 6 === 3, '#efe2c8')
    p.bg.fill(27, 6, 10, 10, '#ffffff')
    p.bg.fill(28, 7, 8, 8, '#bfe3f5')
    p.bg.fill(31, 7, 1, 8, '#ffffff')
    p.bg.fill(28, 10, 8, 1, '#ffffff')
    p.bg.fill(3, 30, 5, 8, '#c77b4c')
    p.bg.circle(5, 27, 3, '#5e9c5a')
  },
  'avatar.cafe': (p) => {
    room(p, '#ead7c0', '#7a5640', '#684732')
    p.bg.pattern(p.bg, '#ead7c0', (x, y) => y % 3 === 0 || (x + (Math.floor(y / 3) % 2) * 3) % 6 === 0, '#dcc3a6')
    for (const x of [8, 31]) {
      p.bg.fill(x, 0, 1, 6, '#3a2a24')
      p.bg.fill(x - 2, 6, 5, 2, '#3a2a24')
      p.bg.fill(x - 1, 8, 3, 1, '#ffe08a')
    }
    p.bg.fill(30, 33, 9, 1, '#5a3a2a')
    p.bg.fill(34, 34, 1, 4, '#5a3a2a')
    p.bg.fill(32, 30, 3, 3, '#ffffff')
    p.bg.dot(35, 31, '#ffffff')
  },
  'avatar.beach': (p) => {
    const b = p.bg
    b.fill(0, 0, W, 26, '#8fd3f0')
    b.fill(0, 26, W, 6, '#2f8fcf')
    b.pattern(b, '#2f8fcf', (x, y) => (x + y * 3) % 7 === 0, '#bfe9ff')
    b.fill(0, 32, W, 18, '#f3dca8')
    b.circle(32, 7, 3, '#ffe066')
    b.fill(4, 12, 2, 24, '#8a5a3a')
    for (const [x, y, w] of [[0, 10, 6], [5, 9, 6], [1, 12, 3], [7, 11, 3]]) b.fill(x, y, w, 2, '#3f9a4f')
    b.fill(11, 6, 6, 1, '#ffffff')
    b.fill(13, 5, 3, 1, '#ffffff')
  },
  'avatar.city': (p) => {
    const b = p.bg
    b.fill(0, 0, W, 50, '#2b2450')
    b.fill(0, 0, W, 10, '#1d1838')
    b.circle(31, 6, 2, '#fff3b0')
    for (const [x, w, top] of [[0, 7, 18], [7, 6, 12], [13, 8, 22], [21, 5, 9], [26, 7, 16], [33, 7, 20]]) {
      b.fill(x, top, w, 40 - top, '#191530')
      for (let y = top + 2; y < 38; y += 3) for (let i = x + 1; i < x + w - 1; i += 2) if ((i * 7 + y) % 5 < 3) b.dot(i, y, '#ffd86b')
    }
    b.fill(0, 40, W, 10, '#3a3460')
    for (const [x, y] of [[4, 4], [15, 3], [22, 6], [37, 3]]) b.dot(x, y, '#ffffff')
  },
  'avatar.golf': (p) => {
    const b = p.bg
    b.fill(0, 0, W, 24, '#bfe4f5')
    b.fill(0, 24, W, 26, '#79b85a')
    for (let x = 0; x < W; x += 5) b.circle(x + 2, 23, 3, '#3f8a4a')
    b.sym(32, [14, 16, 17, 18], '#9bd27a')
    b.fill(33, 12, 1, 24, '#ffffff')
    b.fill(34, 12, 4, 3, '#d9483b')
    b.fill(32, 36, 3, 1, '#2a3a2a')
    b.fill(6, 5, 7, 2, '#ffffff')
    b.fill(8, 4, 3, 1, '#ffffff')
  },
  'avatar.plum': (p) => {
    room(p, '#fbeaea', '#e8c9b2', '#d8b49a')
    const b = p.bg
    b.line(0, 4, 14, 12, '#6b4a3a')
    b.line(0, 5, 14, 13, '#6b4a3a')
    b.line(7, 8, 10, 2, '#6b4a3a')
    b.line(39, 16, 30, 22, '#6b4a3a')
    for (const [x, y] of [[4, 5], [10, 3], [13, 11], [8, 9], [34, 18], [31, 21], [37, 15]]) {
      b.fill(x - 1, y, 3, 1, '#f29bb2')
      b.fill(x, y - 1, 1, 3, '#f29bb2')
      b.dot(x, y, '#f6d55c')
    }
  },
  'avatar.moon': (p) => {
    const b = p.bg
    b.fill(0, 0, W, 40, '#1f2c55')
    b.fill(0, 40, W, 10, '#2e3a52')
    peak(b, 8, 28, 40, '#2a3550')
    peak(b, 30, 24, 40, '#26304a')
    b.circle(31, 8, 4, '#ffe9a3')
    for (const [x, y] of [[4, 4], [12, 8], [18, 3], [23, 11], [38, 15], [8, 16]]) b.dot(x, y, '#fff8d0')
  },
  'avatar.turtle': (p) => {
    const b = p.bg
    b.fill(0, 0, W, 50, '#eef0ea')
    peak(b, 8, 18, 40, '#c9cdc4')
    peak(b, 31, 14, 40, '#b5bab0')
    peak(b, 20, 26, 40, '#8d938a')
    b.fill(0, 40, W, 10, '#d7dbd0')
    b.fill(2, 30, 12, 1, '#ffffff')
    b.fill(26, 26, 10, 1, '#ffffff')
  },
  'avatar.pine': (p) => {
    const b = p.bg
    b.fill(0, 0, W, 40, '#d4ebf2')
    b.fill(0, 40, W, 10, '#a9cf86')
    pine(b, 0)
    crane(b, 28, 7)
  },
  'avatar.crane': (p) => {
    const b = p.bg
    b.fill(0, 0, W, 40, '#d8efe6')
    b.circle(33, 6, 3, '#e0483a')
    b.fill(2, 8, 9, 2, '#ffffff')
    b.fill(4, 7, 4, 1, '#ffffff')
    peak(b, 6, 26, 40, '#6fa36b')
    peak(b, 34, 22, 40, '#5f955c')
    b.fill(0, 40, W, 10, '#8fbf72')
    crane(b, 22, 12)
  },
  'avatar.tiger': (p) => {
    const b = p.bg
    b.fill(0, 0, W, 40, '#eef3dd')
    b.fill(0, 40, W, 10, '#b8cf8c')
    pine(b, 31)
    b.fill(1, 32, 8, 7, '#e8892e')
    b.fill(1, 31, 2, 1, '#e8892e')
    b.fill(7, 31, 2, 1, '#e8892e')
    b.fill(3, 33, 1, 1, '#2a1d18')
    b.fill(6, 33, 1, 1, '#2a1d18')
    b.fill(3, 36, 4, 2, '#fff3e0')
    b.fill(4, 32, 2, 1, '#2a1d18')
    b.fill(1, 35, 1, 2, '#2a1d18')
    b.fill(8, 35, 1, 2, '#2a1d18')
    b.fill(33, 5, 4, 2, '#1e1e24')
    b.fill(34, 6, 2, 1, '#ffffff')
    b.fill(37, 5, 2, 1, '#1e1e24')
  },
  'avatar.dragon': (p) => {
    const b = p.bg
    b.fill(0, 0, W, 50, '#1d2b4b')
    for (let x = 0; x < W; x++) {
      const y = Math.round(9 + 3 * Math.sin(x / 4))
      b.fill(x, y, 1, 2, '#3fa37a')
      if (x % 3 === 0) b.dot(x, y, GOLD)
    }
    b.fill(36, 6, 4, 4, '#3fa37a')
    b.dot(37, 7, '#d2403a')
    for (const [x, y, w] of [[2, 20, 8], [26, 24, 10], [12, 30, 7]]) {
      b.fill(x, y, w, 2, '#cfd8e8')
      b.fill(x + 2, y - 1, w - 4, 1, '#cfd8e8')
    }
    b.fill(0, 42, W, 8, '#24365a')
  },
  'avatar.phoenix': (p) => {
    const b = p.bg
    b.fill(0, 0, W, 16, '#f9e4cc')
    b.fill(0, 16, W, 24, '#f3cfa9')
    b.fill(0, 40, W, 10, '#e9bf93')
    b.circle(6, 7, 3, '#e0483a')
    const tail = ['#d2403a', GOLD, '#3f9a7a', '#3d6fb8']
    tail.forEach((c, i) => b.line(34, 6, 22 + i * 2, 18 + i * 2, c))
    b.fill(33, 3, 4, 4, '#d2403a')
    b.dot(36, 4, GOLD)
  },
  'avatar.irworobong': (p) => {
    const b = p.bg
    b.fill(0, 0, W, 50, '#1e3e72')
    b.circle(7, 7, 3, '#e0483a')
    b.circle(32, 7, 3, '#f2f0e6')
    for (const [cx, top] of [[4, 22], [12, 16], [20, 12], [28, 16], [36, 22]]) peak(b, cx, top, 42, '#3f8f5a')
    b.fill(0, 42, W, 8, '#3b6fb0')
    b.pattern(b, '#3b6fb0', (x, y) => (x + y * 2) % 5 === 0, '#ffffff')
  },
  'avatar.crown': (p) => {
    const b = p.bg
    b.fill(0, 0, W, 50, '#f3d27a')
    b.pattern(b, '#f3d27a', (x, y) => (x + y) % 8 < 2, '#f8e3a3')
    for (const [x, y] of [[5, 5], [33, 8], [8, 30], [34, 34]]) sparkle(b, x, y, '#ffffff')
  },
  'avatar.king': (p) => {
    const b = p.bg
    b.fill(0, 0, W, 50, '#3d2257')
    for (const [x, y] of [[5, 6], [33, 5], [6, 32], [34, 30]]) sparkle(b, x, y, GOLD)
    for (const [x, y] of [[14, 3], [27, 12], [3, 20], [37, 22]]) b.dot(x, y, GOLD)
  },
  'avatar.master': (p) => {
    const b = p.bg
    b.fill(0, 0, W, 50, '#24211e')
    b.line(0, 40, 39, 30, '#3a3631')
    b.line(0, 41, 39, 31, '#3a3631')
    for (const [x, y] of [[6, 6], [32, 9], [34, 36]]) sparkle(b, x, y, GOLD)
  },
}

let uid = 0
const baked = new Map<string, string>()

/** 차림을 PNG로 굽는다. 캔버스가 없으면(서버·테스트) null. */
function bake(l: Look): string | null {
  if (typeof document === 'undefined') return null
  const key = JSON.stringify(l)
  const hit = baked.get(key)
  if (hit) return hit
  const p = draw(l)
  const layers = [smooth(p.bg), smooth(p.doll), smooth(p.ink)]
  layers[1].outline(LINE)
  layers[1].outline(LINE)
  const hi = new Layer(W * SCALE, H * SCALE)
  face(hi, BODIES[l.body] ?? BODIES['body.man'])
  layers.push(hi, smooth(p.top))
  const canvas = document.createElement('canvas')
  canvas.width = W * SCALE
  canvas.height = H * SCALE
  const ctx = canvas.getContext('2d')
  if (!ctx) return null
  for (const layer of layers) layer.paint(ctx)
  const url = canvas.toDataURL('image/png')
  baked.set(key, url)
  return url
}

function draw(l: Look): Paint {
  const body = BODIES[l.body] ?? BODIES['body.man']
  const outfit = OUTFITS[l.outfit] ?? OUTFITS['outfit.tee']
  const hair = HAIRS[l.hair] ?? HAIRS['hair.short']
  const hat = HATS[l.hat] ?? HATS['hat.none']
  const backdrop = BACKDROPS[l.background] ?? BACKDROPS['avatar.basic']
  const p: Paint = { bg: new Layer(), doll: new Layer(), ink: new Layer(), top: new Layer() }

  backdrop(p)
  p.bg.sym(44, [8], '#00000022') // 발밑 그림자
  hair.back?.(p, body.hair)
  outfit.back?.(p)
  dress(p, body, outfit)
  head(p, body)
  hair.front(p, body.hair)
  hat(p)
  if (!body.woman) (BEARDS[l.beard] ?? BEARDS['beard.none'])(p, body.elder ? '#eeeae2' : body.hair)
  return p
}

/** 인형 한 장. head면 얼굴만 둥글게 잘라 작은 자리(머리글 등)에 쓴다. */
export function dollSvg(l: Look, opts: { head?: boolean } = {}): string {
  const id = `ch${++uid}`
  const clip = opts.head ? '<circle cx="60" cy="51" r="40"/>' : `<rect width="120" height="150" rx="14"/>`
  const view = opts.head ? '20 11 80 80' : '0 0 120 150'
  const frame = opts.head
    ? `<circle cx="60" cy="51" r="39.4" fill="none" stroke="#00000022" stroke-width="1.2"/>`
    : `<rect x="0.6" y="0.6" width="118.8" height="148.8" rx="13.6" fill="none" stroke="#00000026" stroke-width="1.2"/>`
  const url = bake(l)
  let art: string
  if (url) art = `<image href="${url}" width="120" height="150"/>`
  else {
    const p = draw(l)
    p.doll.outline(LINE)
    art = `<g transform="scale(${UNIT})" shape-rendering="crispEdges">${p.bg.svg()}${p.doll.svg()}${p.ink.svg()}${p.top.svg()}</g>`
  }
  return (
    `<svg xmlns="http://www.w3.org/2000/svg" viewBox="${view}" role="img">` +
    `<defs><clipPath id="${id}c">${clip}</clipPath></defs>` +
    `<g clip-path="url(#${id}c)">${art}</g>${frame}</svg>`
  )
}
