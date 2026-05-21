const root = document.getElementById('app')
if (!root) {
  throw new Error('app root not found')
}

if (import.meta.env.DEV) {
  console.info('[trovatacast/web] booted')
}
