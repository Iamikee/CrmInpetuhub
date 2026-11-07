const relativeTimeFormatter = new Intl.RelativeTimeFormat('pt-BR', { numeric: 'auto' })
const saoPauloDateFormatter = new Intl.DateTimeFormat('en-CA', {
  timeZone: 'America/Sao_Paulo',
  year: 'numeric',
  month: '2-digit',
  day: '2-digit',
})

function formatSaoPauloDate(timestamp) {
  const parts = Object.fromEntries(saoPauloDateFormatter.formatToParts(timestamp)
    .map(part => [part.type, part.value]))
  return `${parts.year}-${parts.month}-${parts.day}`
}

function saoPauloDateKey(value) {
  const localDate = String(value ?? '').match(/^(\d{4}-\d{2}-\d{2})(?:T\d{2}:\d{2}(?::\d{2}(?:\.\d+)?)?)?$/)
  if (localDate) return localDate[1]

  const timestamp = new Date(value)
  if (!Number.isFinite(timestamp.getTime())) return ''
  return formatSaoPauloDate(timestamp)
}

export function formatRelativeNotificationDate(value, now = Date.now()) {
  const timestamp = new Date(value).getTime()
  if (!Number.isFinite(timestamp)) return 'Data indisponível'

  const difference = timestamp - now
  if (Math.abs(difference) < 30_000) return 'agora'

  const units = [
    ['minute', 60_000, 3_600_000],
    ['hour', 3_600_000, 86_400_000],
    ['day', 86_400_000, 604_800_000],
    ['week', 604_800_000, 2_629_746_000],
    ['month', 2_629_746_000, 31_556_952_000],
    ['year', 31_556_952_000, Number.POSITIVE_INFINITY],
  ]
  const [unit, milliseconds] = units.find(([, , upperBound]) => Math.abs(difference) < upperBound)
  const roundedDifference = Math.round(difference / milliseconds)
  return relativeTimeFormatter.format(roundedDifference || Math.sign(difference), unit)
}

export function groupNotificationsByDay(items, now = Date.now()) {
  const today = formatSaoPauloDate(new Date(now))
  const yesterdayDate = new Date(`${today}T12:00:00Z`)
  yesterdayDate.setUTCDate(yesterdayDate.getUTCDate() - 1)
  const yesterday = yesterdayDate.toISOString().slice(0, 10)
  const groups = [
    { label: 'Hoje', date: today, items: [] },
    { label: 'Ontem', date: yesterday, items: [] },
    { label: 'Anteriores', date: null, items: [] },
  ]

  for (const item of items) {
    const date = saoPauloDateKey(item.dataHora)
    const group = groups.find(candidate => candidate.date === date) || groups[2]
    group.items.push(item)
  }

  return groups.filter(group => group.items.length > 0)
}

export function notificationDestination(type) {
  return /ticket|atendimento|d[uú]vida/i.test(String(type ?? '')) ? 'support' : 'reservation'
}

export function normalizeNotificationMessage(value) {
  const message = String(value ?? '')
    .replace(/[\u2012\u2013\u2014\u2015]/g, ',')
    .replace(/\s*,\s*/g, ', ')
    .replace(/(?:,\s*){2,}/g, ', ')
    .replace(/\s+/g, ' ')
    .replace(/^[,\s]+|[,\s]+$/g, '')

  return message || 'Sem mensagem disponível.'
}
