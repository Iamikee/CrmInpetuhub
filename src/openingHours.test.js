import { describe, expect, it } from 'vitest'
import { formatWeeklyHours, getApprovalMessage, getClosedDayMessage, getHoursForDate, getIsoDayOfWeek, getTimeSlots, validateOperatingHours } from './openingHours.js'

const weeklySchedule = [
  ...[1, 2, 3, 4, 5, 6].map(diaSemana => ({ diaSemana, fechado: false, exigeAprovacao: false, abertura: '08:00:00', fechamento: '22:00:00' })),
  { diaSemana: 7, fechado: false, exigeAprovacao: true, abertura: '08:00:00', fechamento: '22:00:00' },
]

describe('weekly operating hours', () => {
  it('resolves the ISO weekday independently of the browser timezone', () => {
    expect(getIsoDayOfWeek('2026-11-16')).toBe(1)
    expect(getIsoDayOfWeek('2026-11-22')).toBe(7)
  })

  it('resolves open Saturday and Sunday schedule values from the API', () => {
    expect(getHoursForDate(weeklySchedule, '2026-11-16')).toEqual(weeklySchedule[0])
    expect(getHoursForDate(weeklySchedule, '2026-11-21')).toEqual(weeklySchedule[5])
    expect(getHoursForDate(weeklySchedule, '2026-11-21').fechado).toBe(false)
    expect(getHoursForDate(weeklySchedule, '2026-11-22')).toEqual(weeklySchedule[6])
    expect(getHoursForDate(weeklySchedule, '2026-11-22').fechado).toBe(false)
    expect(getApprovalMessage(weeklySchedule, '2026-11-22'))
      .toBe('Pedidos para domingo dependem de aprovação da equipe.')
    expect(getApprovalMessage(weeklySchedule, '2026-11-21')).toBe('')
  })

  it('validates inclusive 08:00 and 22:00 bounds for every day', () => {
    expect(validateOperatingHours(weeklySchedule, '2026-11-16', '08:00', '22:00')).toBe('')
    expect(validateOperatingHours(weeklySchedule, '2026-11-21', '08:00', '22:00')).toBe('')
    expect(validateOperatingHours(weeklySchedule, '2026-11-22', '08:00', '22:00')).toBe('')
    expect(validateOperatingHours(weeklySchedule, '2026-11-16', '07:59', '08:30'))
      .toBe('O horário deve ficar entre 08:00 e 22:00.')
    expect(validateOperatingHours(weeklySchedule, '2026-11-16', '21:30', '22:01'))
      .toBe('O horário deve ficar entre 08:00 e 22:00.')
    expect(validateOperatingHours(weeklySchedule, '2026-11-22', '21:30', '22:01'))
      .toBe('O horário deve ficar entre 08:00 e 22:00.')
  })

  it('offers only slots whose complete half-hour interval fits the day', () => {
    const weekdaySlots = getTimeSlots(weeklySchedule, '2026-11-16')
    expect(weekdaySlots[0]).toBe('08:00')
    expect(weekdaySlots[weekdaySlots.length - 1]).toBe('21:30')
    expect(weekdaySlots).not.toContain('22:00')
    expect(getTimeSlots(weeklySchedule, '2026-11-22').slice(-1)[0]).toBe('21:30')
  })

  it('formats the displayed schedule using the approval flag from the API', () => {
    expect(formatWeeklyHours(weeklySchedule))
      .toBe('Segunda a sábado: 08:00 às 22:00. Domingo: Somente com aprovação.')
  })
})
