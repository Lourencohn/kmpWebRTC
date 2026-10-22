export function formatBrl(cents: number): string {
  const whole = Math.floor(cents / 100)
  const fractional = String(cents % 100).padStart(2, '0')
  const wholeWithDots = whole.toString().replace(/\B(?=(\d{3})+(?!\d))/g, '.')
  return `R$ ${wholeWithDots},${fractional}`
}
