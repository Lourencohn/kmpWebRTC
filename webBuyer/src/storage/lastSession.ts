const STORAGE_KEY = 'trovatacast:last-session'

type LastSessionState = {
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

function ignoringStorageErrors(operation: () => void): void {
  try {
    operation()
  } catch {}
}

export function saveLastSession(state: LastSessionState, storage: Storage | null = defaultStorage()): void {
  if (!storage) return
  ignoringStorageErrors(() => storage.setItem(STORAGE_KEY, JSON.stringify(state)))
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
  ignoringStorageErrors(() => storage.removeItem(STORAGE_KEY))
}
