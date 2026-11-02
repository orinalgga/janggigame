/** 머리 상품: 앞머리(hair)와 머리 뒤로 넘어가는 것(back). 늙은 몸에는 은발(@elder)을 따로 만든다. */
import { capsule, circle, ellipse, mirror, shape, tone, write } from './lib'

const DARK = '#4a332a'
const SILVER = '#d8d2ca'

/** 정수리를 덮는 바가지. 앞머리는 저마다 덧붙인다. */
const CAP = 'M60 114 C56 66 84 44 120 44 C156 44 184 66 180 114'
const shine = (c: string) =>
  `<path d="M80 70 Q100 54 124 54" stroke="${tone(c, 0.45)}" stroke-width="6" fill="none" stroke-linecap="round" opacity="0.75"/>`

type Draw = (c: string) => string
interface Hair {
  front: Draw
  back?: Draw
}

const s = (d: string, c: string) => shape({ d, fill: c, shade: '' })
const curls = (pts: [number, number, number][], c: string) => pts.map(([x, y, r]) => s(circle(x, y, r), c)).join('')

const HAIRS: Record<string, Hair> = {
  'hair.short': {
    front: (c) =>
      s(`${CAP} L172 116 Q172 96 160 86 Q154 98 136 100 Q142 90 139 80 Q126 98 100 100 Q108 90 107 82 Q92 96 72 98 Q68 106 68 116 Z`, c) + shine(c),
  },
  'hair.bob': {
    back: (c) => s('M56 110 C54 62 84 42 120 42 C156 42 186 62 184 110 L186 160 Q172 168 156 162 L84 162 Q68 168 54 160 Z', tone(c, -0.12)),
    front: (c) =>
      s(`${CAP} L180 140 Q176 108 164 92 L162 100 Q142 104 120 100 Q98 104 78 100 L76 92 Q64 108 60 140 Z`, c) +
      `<path d="M100 100 L102 88 M140 100 L138 88 M120 100 L120 86" stroke="${tone(c, -0.25)}" stroke-width="2" stroke-linecap="round"/>` +
      shine(c),
  },
  'hair.perm': {
    back: (c) => curls([[64, 120, 16], [176, 120, 16], [62, 92, 17], [178, 92, 17], [70, 146, 13], [170, 146, 13]], tone(c, -0.1)),
    front: (c) =>
      curls(
        [
          [74, 74, 17], [96, 56, 18], [120, 50, 18], [144, 56, 18], [166, 74, 17],
          [70, 98, 14], [170, 98, 14], [88, 88, 13], [110, 84, 13], [132, 84, 13], [154, 88, 13],
        ],
        c,
      ) + `<path d="M98 50 q8 -6 16 -2 M128 46 q8 -4 14 2" stroke="${tone(c, 0.45)}" stroke-width="3" fill="none" stroke-linecap="round"/>`,
  },
  'hair.long': {
    back: (c) => s('M56 106 C54 60 86 42 120 42 C154 42 186 60 184 106 L192 224 Q174 236 156 228 L84 228 Q66 236 48 224 Z', tone(c, -0.12)),
    front: (c) =>
      s(`${CAP} L176 180 Q170 184 164 180 L164 110 Q156 92 140 84 Q128 98 98 102 Q86 104 76 110 L76 180 Q70 184 64 180 Z`, c) +
      `<path d="M140 86 Q130 98 114 100" stroke="${tone(c, -0.25)}" stroke-width="2" fill="none" stroke-linecap="round"/>` +
      shine(c),
  },
  'hair.sidepart': {
    front: (c) =>
      s(`${CAP} L174 116 Q172 92 156 82 Q132 94 98 98 Q80 100 70 110 L66 116 Z`, c) +
      `<path d="M98 50 Q94 66 98 80" stroke="${tone(c, -0.3)}" stroke-width="2.4" fill="none" stroke-linecap="round"/>` +
      `<path d="M104 62 Q130 64 158 76 M104 74 Q128 78 150 84" stroke="${tone(c, 0.3)}" stroke-width="2.4" fill="none" stroke-linecap="round"/>` +
      shine(c),
  },
  'hair.slick': {
    front: (c) =>
      s('M62 112 C56 64 86 44 120 44 C154 44 184 64 178 112 L172 112 Q170 84 150 74 Q120 64 90 74 Q70 84 68 112 Z', c) +
      `<path d="M88 72 Q104 56 124 54 M100 76 Q116 62 140 60 M118 70 Q136 62 156 70" stroke="${tone(c, 0.35)}" stroke-width="2.6" fill="none" stroke-linecap="round"/>`,
  },
  'hair.buzz': {
    front: (c) =>
      s('M64 108 C62 68 88 50 120 50 C152 50 178 68 176 108 L170 108 Q164 84 120 80 Q76 84 70 108 Z', tone(c, 0.12)) +
      [[90, 66], [104, 60], [120, 58], [136, 60], [150, 66], [96, 74], [112, 70], [128, 70], [144, 74], [80, 80], [160, 80]]
        .map(([x, y]) => `<circle cx="${x}" cy="${y}" r="1.4" fill="${tone(c, -0.25)}"/>`)
        .join(''),
  },
  'hair.ponytail': {
    back: (c) =>
      s('M146 62 Q196 70 196 136 Q196 176 176 196 Q182 160 174 128 Q168 100 146 86 Z', tone(c, -0.1)) +
      s('M56 112 C54 64 84 44 120 44 C156 44 186 64 184 112 L182 130 L58 130 Z', tone(c, -0.12)),
    front: (c) =>
      s(`${CAP} L178 128 Q174 100 160 88 Q140 100 112 98 Q122 92 124 84 Q100 98 78 98 Q66 108 62 128 Z`, c) +
      s(ellipse(160, 64, 8, 6), '#e2414a') +
      shine(c),
  },
  'hair.wave': {
    back: (c) =>
      s('M56 106 C54 60 86 42 120 42 C154 42 186 60 184 106 Q196 130 186 152 Q198 176 188 200 Q196 220 180 230 L60 230 Q44 220 52 200 Q42 176 54 152 Q44 130 56 106 Z', tone(c, -0.12)),
    front: (c) =>
      s(`${CAP} Q186 130 176 150 Q186 168 174 184 Q168 186 164 180 Q170 166 162 152 Q170 136 162 116 Q156 94 136 84 Q118 100 92 100 Q78 104 74 116 Q66 136 76 152 Q68 166 76 180 Q70 186 64 184 Q54 168 64 150 Q54 130 60 114 Z`, c) +
      shine(c),
  },
  'hair.bun': {
    back: (c) => s(circle(120, 38, 21), c) + `<path d="M106 32 Q118 22 132 30" stroke="${tone(c, 0.4)}" stroke-width="3" fill="none" stroke-linecap="round"/>`,
    front: (c) =>
      s(`${CAP} L176 124 Q174 100 162 90 Q146 96 124 94 Q104 98 86 94 Q70 104 64 124 Z`, c) +
      `<path d="M120 46 Q116 70 124 94" stroke="${tone(c, -0.25)}" stroke-width="2" fill="none"/>` +
      shine(c),
  },
  'hair.sangtu': {
    front: (c) =>
      s(capsule(120, 46, 120, 30, 10, 9), c) +
      s('M62 112 C56 64 86 46 120 46 C154 46 184 64 178 112 L172 112 Q168 88 150 80 Q120 72 90 80 Q72 88 68 112 Z', c) +
      shape({ d: 'M68 92 Q120 70 172 92 L170 102 Q120 82 70 102 Z', fill: '#1e1a1c', shade: '', sw: 1.8 }) +
      s(circle(72, 100, 4.6), '#e8b83f') +
      s(circle(168, 100, 4.6), '#e8b83f'),
  },
  'hair.jjok': {
    back: (c) =>
      s(ellipse(172, 150, 20, 15), c) +
      `<path d="M146 148 L204 140" stroke="#7ac0a0" stroke-width="5" stroke-linecap="round"/><circle cx="204" cy="140" r="4.5" fill="#9ad8bc" stroke="#3b2a24" stroke-width="1.6"/>`,
    front: (c) =>
      s(`${CAP} L180 132 Q176 104 162 92 Q140 82 120 80 Q100 82 78 92 Q64 104 60 132 Z`, c) +
      `<path d="M120 46 L120 80" stroke="${tone(c, -0.3)}" stroke-width="2.4"/>` +
      shine(c),
  },
  'hair.loose': {
    back: (c) => s('M150 92 Q186 100 188 140 Q190 170 176 186 Q178 150 166 126 Q156 110 146 104 Z', tone(c, -0.1)),
    front: (c) =>
      s('M62 112 C56 64 86 46 120 46 C154 46 184 64 178 112 L172 112 Q168 92 150 86 Q120 80 90 86 Q72 92 68 112 Z', c) +
      shape({ d: 'M66 90 Q120 70 174 90 L173 100 Q120 80 67 100 Z', fill: '#c8323a', shade: '', sw: 1.8 }) +
      shape({ d: 'M172 94 Q192 100 198 118 L190 120 Q186 106 170 102 Z', fill: '#c8323a', shade: '', sw: 1.8 }),
  },
  'hair.gache': {
    back: (c) => s('M44 92 C30 40 80 14 120 14 C160 14 210 40 196 92 Q180 74 120 70 Q60 74 44 92 Z', c),
    front: (c) =>
      s(`${CAP} L180 126 Q176 102 162 90 Q140 82 120 80 Q100 82 78 90 Q64 102 60 126 Z`, c) +
      `<path d="M120 46 L120 80" stroke="${tone(c, -0.3)}" stroke-width="2.4"/>` +
      `<path d="M58 54 Q120 30 182 54" stroke="${tone(c, -0.3)}" stroke-width="3" fill="none"/>` +
      [[84, 40], [120, 28], [156, 40]].map(([x, y]) => s(circle(x, y, 5.5), '#e8b83f')).join(''),
  },
}

export function hairs() {
  for (const [id, h] of Object.entries(HAIRS)) {
    write(id, 'hair', h.front(DARK))
    write(id, 'hair@elder', h.front(SILVER))
    if (h.back) {
      write(id, 'back', h.back(DARK))
      write(id, 'back@elder', h.back(SILVER))
    }
  }
}


