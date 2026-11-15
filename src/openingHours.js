const DAYS = [
  { day: 1, name: 'Segunda', closedName: 'segundas-feiras', prefix: 'às ' },
  { day: 2, name: 'Terça', closedName: 'terças-feiras', prefix: 'às ' },
  { day: 3, name: 'Quarta', closedName: 'quartas-feiras', prefix: 'às ' },
  { day: 4, name: 'Quinta', closedName: 'quintas-feiras', prefix: 'às ' },
  { day: 5, name: 'Sexta', closedName: 'sextas-feiras', prefix: 'às ' },
  { day: 6, name: 'Sábado', closedName: 'sábados', prefix: 'aos ' },
  { day: 7, name: 'Domingo', closedName: 'domingos', prefix: 'aos ' },
]

export function getIsoDayOfWeek(date) {
  if (!date) return null
  const [year, month, dayOfMonth] = date.split('-').map(Number)
  if (!year || !month || !dayOfMonth) return null
  const day = new Date(Date.UTC(year, month - 1, dayOfMonth)).getUTCDay()
  return day === 0 ? 7 : day
}

export function getHoursForDate(schedule, date) {
  const day = getIsoDayOfWeek(date)
  return schedule?.find(item => item.diaSemana === day) || null
}

export function getClosedDayMessage(date) {
  const day = DAYS.find(item => item.day === getIsoDayOfWeek(date))
  return day ? `O espaço não funciona ${day.prefix}${day.closedName}.` : ''
}

export function getApprovalMessage(schedule, date) {
  const hours = getHoursForDate(schedule, date)
  return hours?.exigeAprovacao ? 'Pedidos para domingo dependem de aprovação da equipe.' : ''
}

export function validateOperatingHours(schedule, date, start, end) {
  const hours = getHoursForDate(schedule, date)
  if (!hours) return 'Não foi possível validar o horário de funcionamento.'
  if (hours.fechado) return getClosedDayMessage(date)
  if (!start || !end) return ''
  if (end <= start) return 'O horário final deve ser posterior ao inicial.'
  const opening = String(hours.abertura).slice(0, 5)
  const closing = String(hours.fechamento).slice(0, 5)
  if (start < opening || end > closing) {
    return `O horário deve ficar entre ${opening} e ${closing}.`
  }
  return ''
}

export function getLatestStartTime(schedule, date, stepMinutes = 30) {
  const hours = getHoursForDate(schedule, date)
  if (!hours || hours.fechado || !hours.fechamento) return undefined
  const [hour, minute] = hours.fechamento.split(':').map(Number)
  const total = hour * 60 + minute - stepMinutes
  return `${String(Math.floor(total / 60)).padStart(2, '0')}:${String(total % 60).padStart(2, '0')}`
}

export function getTimeSlots(schedule, date, stepMinutes = 30) {
  const hours = getHoursForDate(schedule, date)
  if (!hours || hours.fechado || !hours.abertura || !hours.fechamento) return []
  const toMinutes = value => Number(value.slice(0, 2)) * 60 + Number(value.slice(3, 5))
  const opening = toMinutes(hours.abertura)
  const closing = toMinutes(hours.fechamento)
  const slots = []
  for (let minutes = opening; minutes + stepMinutes <= closing; minutes += stepMinutes) {
    slots.push(`${String(Math.floor(minutes / 60)).padStart(2, '0')}:${String(minutes % 60).padStart(2, '0')}`)
  }
  return slots
}

function displayTime(value) {
  return String(value).slice(0, 5)
}

export function formatWeeklyHours(schedule) {
  if (!schedule?.length) return ''
  const entries = DAYS.map(day => {
    const hours = schedule.find(item => item.diaSemana === day.day)
    if (!hours) return { ...day, value: 'Fechado' }
    return {
      ...day,
      value: hours.fechado
        ? 'Fechado'
        : hours.exigeAprovacao
          ? 'Somente com aprovação'
          : `${displayTime(hours.abertura)} às ${displayTime(hours.fechamento)}`,
    }
  })
  const groups = []
  for (const entry of entries) {
    const previous = groups[groups.length - 1]
    if (previous && previous.value === entry.value) previous.days.push(entry.name)
    else groups.push({ value: entry.value, days: [entry.name] })
  }
  return groups.map(group => {
    const days = group.days.length > 1
      ? `${group.days[0]} a ${group.days[group.days.length - 1].toLocaleLowerCase('pt-BR')}`
      : group.days[0]
    return `${days}: ${group.value}`
  }).join('. ') + '.'
}
