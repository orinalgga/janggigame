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
      // 북을 두 번 치고 높은 음을 얹는다.
      knock(c, t, 0.8, 110)
      knock(c, t + 0.12, 0.8, 110)
      tone(c, t + 0.02, 660, 0.35, 0.18, 'square')
      tone(c, t + 0.14, 880, 0.4, 0.16, 'square')
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
