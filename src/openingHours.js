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
  return hours?.exigeAprovacao ? 'Pedidos para sábado e domingo dependem de aprovação da equipe.' : ''
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

export function getPossibleEndTimes(schedule, date, start, reservations = [], now = null, stepMinutes = 30) {
  const hours = getHoursForDate(schedule, date)
  if (!hours || hours.fechado || !hours.abertura || !hours.fechamento || !start) return []
  if (now && (date < now.date || (date === now.date && start < now.time))) return []

  const toMinutes = value => Number(String(value).slice(0, 2)) * 60 + Number(String(value).slice(3, 5))
  const toTime = minutes => `${String(Math.floor(minutes / 60)).padStart(2, '0')}:${String(minutes % 60).padStart(2, '0')}`
  const startMinutes = toMinutes(start)
  const opening = toMinutes(hours.abertura)
  const closing = toMinutes(hours.fechamento)
  if (startMinutes < opening || startMinutes >= closing) return []

  const possibleEnds = []
  for (let endMinutes = startMinutes + stepMinutes; endMinutes <= closing; endMinutes += stepMinutes) {
    const end = toTime(endMinutes)
    const conflicts = reservations.some(reservation => {
      const reservationStart = String(reservation.inicio || reservation.horarioInicio || '').slice(0, 5)
      const reservationEnd = String(reservation.fim || reservation.horarioFim || '').slice(0, 5)
      return reservationStart && reservationEnd && start < reservationEnd && end > reservationStart
    })
    if (conflicts) break
    possibleEnds.push(end)
  }
  return possibleEnds
}

export function getDefaultEndTime(possibleEnds, start) {
  if (!possibleEnds.length || !start) return ''
  const oneHourMinutes = Number(start.slice(0, 2)) * 60 + Number(start.slice(3, 5)) + 60
  const oneHourEnd = `${String(Math.floor(oneHourMinutes / 60)).padStart(2, '0')}:${String(oneHourMinutes % 60).padStart(2, '0')}`
  return possibleEnds.includes(oneHourEnd) ? oneHourEnd : possibleEnds[possibleEnds.length - 1]
}

export function getEndTimeDisplayOptions(possibleEnds, selectedEnd, expanded = null) {
  const initialOptions = possibleEnds.slice(0, 8)
  const selectedIsBeyondLimit = selectedEnd && possibleEnds.indexOf(selectedEnd) >= initialOptions.length
  const isExpanded = expanded === null ? Boolean(selectedIsBeyondLimit) : expanded
  const visibleEndTimes = isExpanded ? [...possibleEnds] : [...initialOptions]
  if (!isExpanded && selectedIsBeyondLimit) visibleEndTimes.push(selectedEnd)
  return {
    visibleEndTimes,
    hasMore: possibleEnds.length > initialOptions.length,
    expanded: isExpanded,
  }
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
