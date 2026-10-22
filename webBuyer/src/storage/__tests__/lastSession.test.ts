import { beforeEach, describe, expect, it } from 'vitest'
import { clearLastSession, loadLastSession, saveLastSession, type Storage } from '../lastSession'

function memoryStorage(initial: Record<string, string> = {}): Storage {
  const map = new Map<string, string>(Object.entries(initial))
  return {
    getItem: (key) => map.get(key) ?? null,
    setItem: (key, value) => {
      map.set(key, value)
    },
    removeItem: (key) => {
      map.delete(key)
    },
  }
}

describe('lastSession', () => {
  let storage: Storage

  beforeEach(() => {
    storage = memoryStorage()
  })

  it('save + load round-trip', () => {
    saveLastSession({ token: 'abc', openedAtMs: 1234, joined: true }, storage)
    expect(loadLastSession(storage)).toEqual({ token: 'abc', openedAtMs: 1234, joined: true })
  })

  it('load returns null when empty', () => {
    expect(loadLastSession(storage)).toBeNull()
  })

  it('load returns null on corrupted JSON', () => {
    storage.setItem('trovatacast:last-session', '{broken')
    expect(loadLastSession(storage)).toBeNull()
  })

  it('load returns null on invalid schema', () => {
    storage.setItem('trovatacast:last-session', JSON.stringify({ token: 1 }))
    expect(loadLastSession(storage)).toBeNull()
  })

  it('clear removes the entry', () => {
    saveLastSession({ token: 'abc', openedAtMs: 1, joined: false }, storage)
    clearLastSession(storage)
    expect(loadLastSession(storage)).toBeNull()
  })

  it('accepts a null storage without throwing', () => {
    expect(() => saveLastSession({ token: 'x', openedAtMs: 0, joined: false }, null)).not.toThrow()
    expect(loadLastSession(null)).toBeNull()
    expect(() => clearLastSession(null)).not.toThrow()
  })
})
