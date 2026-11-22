import { describe, expect, it } from 'vitest'
import { formatWeeklyHours, getApprovalMessage, getClosedDayMessage, getHoursForDate, getIsoDayOfWeek, getTimeSlots, validateOperatingHours } from './openingHours.js'

const weeklySchedule = [
  ...[1, 2, 3, 4, 5].map(diaSemana => ({ diaSemana, fechado: false, exigeAprovacao: false, abertura: '07:00:00', fechamento: '21:00:00' })),
  { diaSemana: 6, fechado: false, exigeAprovacao: true, abertura: '07:00:00', fechamento: '21:00:00' },
  { diaSemana: 7, fechado: false, exigeAprovacao: true, abertura: '07:00:00', fechamento: '21:00:00' },
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
    expect(getApprovalMessage(weeklySchedule, '2026-11-21'))
      .toBe('Pedidos para sábado e domingo dependem de aprovação da equipe.')
    expect(getApprovalMessage(weeklySchedule, '2026-11-22'))
      .toBe('Pedidos para sábado e domingo dependem de aprovação da equipe.')
    expect(getApprovalMessage(weeklySchedule, '2026-11-20')).toBe('')
  })

  it('validates inclusive 07:00 and 21:00 bounds for every day', () => {
    expect(validateOperatingHours(weeklySchedule, '2026-11-16', '07:00', '21:00')).toBe('')
    expect(validateOperatingHours(weeklySchedule, '2026-11-21', '07:00', '21:00')).toBe('')
    expect(validateOperatingHours(weeklySchedule, '2026-11-22', '07:00', '21:00')).toBe('')
    expect(validateOperatingHours(weeklySchedule, '2026-11-16', '06:59', '07:30'))
      .toBe('O horário deve ficar entre 07:00 e 21:00.')
    expect(validateOperatingHours(weeklySchedule, '2026-11-16', '20:30', '21:01'))
      .toBe('O horário deve ficar entre 07:00 e 21:00.')
    expect(validateOperatingHours(weeklySchedule, '2026-11-22', '20:30', '21:01'))
      .toBe('O horário deve ficar entre 07:00 e 21:00.')
  })

  it('offers only slots whose complete half-hour interval fits the day', () => {
    const weekdaySlots = getTimeSlots(weeklySchedule, '2026-11-16')
    expect(weekdaySlots[0]).toBe('07:00')
    expect(weekdaySlots[weekdaySlots.length - 1]).toBe('20:30')
    expect(weekdaySlots).not.toContain('21:00')
    expect(getTimeSlots(weeklySchedule, '2026-11-22').slice(-1)[0]).toBe('20:30')
  })

  it('formats the displayed schedule using the approval flag from the API', () => {
    expect(formatWeeklyHours(weeklySchedule))
      .toBe('Segunda a sexta: 07:00 às 21:00. Sábado a domingo: Somente com aprovação.')
  })
})
