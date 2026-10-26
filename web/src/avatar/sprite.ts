import type { Look } from '../api'

/**
 * 부품 그림을 겹쳐 그리는 2D 캐릭터. 그림은 상품마다 avatar/parts/<상품 id>/ 폴더의 파일이고,
 * 모두 같은 240×300 캔버스(발 끝 y 270, 얼굴 가운데 (120, 112))에 맞춰 그린다. 규격은 docs/avatar-art.md.
 *
 * 파일 이름은 <층>.svg(또는 .png, .webp)이고, 몸에 따라 다른 그림이 필요하면 <층>@elder, @woman, @animal을 따로 둔다.
 * 같은 층에 그림 파일(png, webp)과 svg가 함께 있으면 그림 파일을 쓴다(작가 그림이 임시 그림을 이긴다).
 */
const FILES = import.meta.glob('./parts/*/*.{svg,png,webp}', { eager: true, query: '?url', import: 'default' }) as Record<
  string,
  string
>

/** 아래에서 위로 그리는 층. [상품 칸, 층 이름]. */
const ORDER: [Slot, string][] = [
  ['background', 'bg'],
  ['body', 'back'],
  ['hair', 'back'],
  ['outfit', 'back'],
  ['hat', 'back'],
  ['body', 'body'],
  ['outfit', 'outfit'],
  ['body', 'head'],
  ['body', 'face'],
  ['beard', 'beard'],
  ['hair', 'hair'],
  ['hat', 'hat'],
]

type Slot = 'background' | 'body' | 'hair' | 'beard' | 'outfit' | 'hat'

/** 몸이 가진 꼴. 그림 파일을 고를 때 앞에 있는 것부터 찾는다. */
const TAGS: Record<string, string[]> = {
  'body.man': [],
  'body.woman': ['woman'],
  'body.elder': ['elder'],
  'body.elder.woman': ['elder', 'woman'],
}
const tagsOf = (body: string) => TAGS[body] ?? ['animal']

/** "상품 id/층@꼴" → 주소. */
const MANIFEST = (() => {
  const rank = (ext: string) => (ext === 'svg' ? 0 : 1)
  const out = new Map<string, { url: string; rank: number }>()
  for (const [path, url] of Object.entries(FILES)) {
    const m = path.match(/^\.\/parts\/([^/]+)\/([a-z]+(?:@[a-z]+)?)\.(svg|png|webp)$/)
    if (!m) continue
    const key = `${m[1]}/${m[2]}`
    const r = rank(m[3])
    if (!out.has(key) || out.get(key)!.rank < r) out.set(key, { url, rank: r })
  }
  return new Map([...out].map(([k, v]) => [k, v.url]))
})()

function file(item: string, layer: string, tags: string[]): string | undefined {
  for (const t of tags) {
    const hit = MANIFEST.get(`${item}/${layer}@${t}`)
    if (hit) return hit
  }
  return MANIFEST.get(`${item}/${layer}`)
}

/** 그림이 있는 상품인지. 없는 상품은 기본 부품으로 그린다. */
const known = (item: string) => [...MANIFEST.keys()].some((k) => k.startsWith(item + '/'))

const DEFAULTS: Record<Slot, string> = {
  background: 'avatar.basic',
  body: 'body.man',
  hair: 'hair.short',
  beard: 'beard.none',
  outfit: 'outfit.tee',
  hat: 'hat.none',
}

/** 이 차림을 그리는 그림 파일 주소들, 아래부터. */
export function spriteLayers(l: Look): string[] {
  const pick = (slot: Slot) => (known(l[slot]) ? l[slot] : DEFAULTS[slot])
  const body = pick('body')
  const tags = tagsOf(body)
  const animal = tags.includes('animal')
  const skip = (slot: Slot) => (animal && (slot === 'hair' || slot === 'beard')) || (slot === 'beard' && tags.includes('woman'))
  const urls: string[] = []
  for (const [slot, layer] of ORDER) {
    if (skip(slot)) continue
    const url = file(slot === 'body' ? body : pick(slot), layer, tags)
    if (url) urls.push(url)
  }
  return urls
}

let uid = 0

/** 캐릭터 한 장. head면 얼굴만 둥글게 잘라 작은 자리(머리글 등)에 쓴다. */
export function spriteSvg(l: Look, opts: { head?: boolean } = {}): string {
  const id = `sp${++uid}`
  const view = opts.head ? '50 40 140 140' : '0 0 240 300'
  const clip = opts.head ? '<circle cx="120" cy="110" r="70"/>' : '<rect width="240" height="300" rx="28"/>'
  const frame = opts.head
    ? '<circle cx="120" cy="110" r="69" fill="none" stroke="#00000022" stroke-width="2"/>'
    : '<rect x="1" y="1" width="238" height="298" rx="27" fill="none" stroke="#00000026" stroke-width="2"/>'
  const images = spriteLayers(l)
    .map((u) => `<image href="${u}" width="240" height="300"/>`)
    .join('')
  return (
    `<svg xmlns="http://www.w3.org/2000/svg" viewBox="${view}" role="img">` +
    `<defs><clipPath id="${id}c">${clip}</clipPath></defs>` +
    `<g clip-path="url(#${id}c)">${images}</g>${frame}</svg>`
  )
}
