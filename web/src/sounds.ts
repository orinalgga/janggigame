/**
 * 대국 효과음. 소리 파일 없이 Web Audio로 그 자리에서 만들어 낸다(저작권 걱정 없음).
 * 소리 켜기/끄기는 이 기기에만 저장한다.
 */
import { ref } from 'vue'

const KEY = 'janggi.sound'

function loadOn(): boolean {
  try {
    return localStorage.getItem(KEY) !== 'off'
  } catch {
    return true
  }
}

/** 소리가 켜져 있는지. 화면의 버튼이 바로 읽고 바꾼다. */
export const soundOn = ref(loadOn())

export function toggleSound() {
  soundOn.value = !soundOn.value
  try {
    localStorage.setItem(KEY, soundOn.value ? 'on' : 'off')
  } catch {
    /* 저장이 막힌 브라우저면 이번 화면에서만 기억한다. */
  }
  if (soundOn.value) play('move')
}

let ctx: AudioContext | null = null

function audio(): AudioContext | null {
  if (!ctx) {
    const AC = window.AudioContext ?? (window as unknown as { webkitAudioContext?: typeof AudioContext }).webkitAudioContext
    if (!AC) return null
    try {
      ctx = new AC()
    } catch {
      return null
    }
  }
  if (ctx.state === 'suspended') void ctx.resume().catch(() => undefined)
  return ctx
}

/** 나무 알이 판에 닿는 '탁' 소리: 짧은 잡음 + 낮은 울림. */
function knock(c: AudioContext, at: number, gain: number, pitch: number) {
  const len = Math.floor(c.sampleRate * 0.06)
  const buf = c.createBuffer(1, len, c.sampleRate)
  const data = buf.getChannelData(0)
  for (let i = 0; i < len; i++) data[i] = (Math.random() * 2 - 1) * Math.pow(1 - i / len, 4)
  const noise = c.createBufferSource()
  noise.buffer = buf
  const band = c.createBiquadFilter()
  band.type = 'bandpass'
  band.frequency.value = pitch * 9
  band.Q.value = 1.2
  const ng = c.createGain()
  ng.gain.value = gain
  noise.connect(band).connect(ng).connect(c.destination)
  noise.start(at)

  const osc = c.createOscillator()
  osc.type = 'sine'
  osc.frequency.setValueAtTime(pitch, at)
  osc.frequency.exponentialRampToValueAtTime(pitch * 0.6, at + 0.12)
  const og = c.createGain()
  og.gain.setValueAtTime(gain * 0.9, at)
  og.gain.exponentialRampToValueAtTime(0.001, at + 0.14)
  osc.connect(og).connect(c.destination)
  osc.start(at)
  osc.stop(at + 0.15)
}

/** 맑은 음 하나. */
function tone(c: AudioContext, at: number, freq: number, dur: number, gain: number, type: OscillatorType = 'triangle') {
  const osc = c.createOscillator()
  osc.type = type
  osc.frequency.value = freq
  const g = c.createGain()
  g.gain.setValueAtTime(0.0001, at)
  g.gain.exponentialRampToValueAtTime(gain, at + 0.015)
  g.gain.exponentialRampToValueAtTime(0.0001, at + dur)
  osc.connect(g).connect(c.destination)
  osc.start(at)
  osc.stop(at + dur + 0.02)
}

/** 장군 소리는 크게 내되 찢어지지 않도록 압축기를 거친다. */
let punch: DynamicsCompressorNode | null = null
function loud(c: AudioContext): AudioNode {
  if (!punch) {
    punch = c.createDynamicsCompressor()
    punch.threshold.value = -18
    punch.knee.value = 6
    punch.ratio.value = 8
    punch.attack.value = 0.002
    punch.release.value = 0.25
    const out = c.createGain()
    out.gain.value = 1.6
    punch.connect(out).connect(c.destination)
  }
  return punch
}

/** 큰북 한 번: 낮게 떨어지는 울림 + 가죽을 때리는 잡음. */
function drum(c: AudioContext, at: number, gain: number) {
  const out = loud(c)
  const osc = c.createOscillator()
  osc.type = 'sine'
  osc.frequency.setValueAtTime(120, at)
  osc.frequency.exponentialRampToValueAtTime(42, at + 0.35)
  const og = c.createGain()
  og.gain.setValueAtTime(0.0001, at)
  og.gain.exponentialRampToValueAtTime(gain, at + 0.006)
  og.gain.exponentialRampToValueAtTime(0.0001, at + 0.6)
  osc.connect(og).connect(out)
  osc.start(at)
  osc.stop(at + 0.62)

  const len = Math.floor(c.sampleRate * 0.12)
  const buf = c.createBuffer(1, len, c.sampleRate)
  const data = buf.getChannelData(0)
  for (let i = 0; i < len; i++) data[i] = (Math.random() * 2 - 1) * Math.pow(1 - i / len, 3)
  const noise = c.createBufferSource()
  noise.buffer = buf
  const low = c.createBiquadFilter()
  low.type = 'lowpass'
  low.frequency.value = 900
  const ng = c.createGain()
  ng.gain.value = gain * 0.7
  noise.connect(low).connect(ng).connect(out)
  noise.start(at)
}

/** 징: 서로 어긋난 배음 몇 개를 길게 울려 금속 떨림을 낸다. */
function gong(c: AudioContext, at: number) {
  const out = loud(c)
  const base = 98
  ;[1, 1.48, 2.02, 2.74, 3.76].forEach((ratio, i) => {
    const osc = c.createOscillator()
    osc.type = 'sine'
    osc.frequency.setValueAtTime(base * ratio * 1.02, at)
    osc.frequency.exponentialRampToValueAtTime(base * ratio, at + 0.4)
    const g = c.createGain()
    const peak = 0.32 / (i + 1)
    const dur = 1.6 - i * 0.2
    g.gain.setValueAtTime(0.0001, at)
    g.gain.exponentialRampToValueAtTime(peak, at + 0.02)
    g.gain.exponentialRampToValueAtTime(0.0001, at + dur)
    osc.connect(g).connect(out)
    osc.start(at)
    osc.stop(at + dur + 0.05)
  })
}

export type SoundName = 'move' | 'capture' | 'check' | 'win' | 'lose' | 'draw'

export function play(name: SoundName) {
  if (!soundOn.value) return
  const c = audio()
  if (!c) return
  const t = c.currentTime + 0.01
  switch (name) {
    case 'move':
      knock(c, t, 0.55, 190)
      break
    case 'capture':
      knock(c, t, 0.7, 160)
      knock(c, t + 0.07, 0.5, 230)
      break
    case 'check':
      // 큰북을 '둥-둥' 두 번 세게 치고 징을 길게 울린다.
      drum(c, t, 1)
      drum(c, t + 0.16, 1.15)
      gong(c, t + 0.16)
      break
    case 'win':
      ;[523, 659, 784, 1047].forEach((f, i) => tone(c, t + i * 0.12, f, 0.45, 0.22))
      break
    case 'lose':
      ;[440, 370, 311, 262].forEach((f, i) => tone(c, t + i * 0.16, f, 0.5, 0.2, 'sine'))
      break
    case 'draw':
      tone(c, t, 523, 0.4, 0.2)
      tone(c, t + 0.2, 523, 0.5, 0.2)
      break
  }
}
