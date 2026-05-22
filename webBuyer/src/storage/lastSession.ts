const STORAGE_KEY = 'trovatacast:last-session'

export type LastSessionState = {
  token: string
  openedAtMs: number
  joined: boolean
}

export type Storage = Pick<globalThis.Storage, 'getItem' | 'setItem' | 'removeItem'>

function defaultStorage(): Storage | null {
  if (typeof window === 'undefined') return null
  try {
    return window.localStorage
  } catch {
    return null
  }
}

export function saveLastSession(state: LastSessionState, storage: Storage | null = defaultStorage()): void {
  if (!storage) return
  try {
    storage.setItem(STORAGE_KEY, JSON.stringify(state))
  } catch {
    /* quota / disabled — silenciar */
  }
}

export function loadLastSession(storage: Storage | null = defaultStorage()): LastSessionState | null {
  if (!storage) return null
  const raw = storage.getItem(STORAGE_KEY)
  if (!raw) return null
  try {
    const parsed = JSON.parse(raw) as Partial<LastSessionState>
    if (typeof parsed.token !== 'string' || typeof parsed.openedAtMs !== 'number') {
      return null
    }
    return {
      token: parsed.token,
      openedAtMs: parsed.openedAtMs,
      joined: Boolean(parsed.joined),
    }
  } catch {
    return null
  }
}

export function clearLastSession(storage: Storage | null = defaultStorage()): void {
  if (!storage) return
  try {
    storage.removeItem(STORAGE_KEY)
  } catch {
    /* ignore */
  }
}
