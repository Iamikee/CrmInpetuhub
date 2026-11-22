import { describe, expect, it } from 'vitest'
import { formatWeeklyHours, getApprovalMessage, getClosedDayMessage, getDefaultEndTime, getEndTimeDisplayOptions, getHoursForDate, getIsoDayOfWeek, getPossibleEndTimes, getTimeSlots, validateOperatingHours } from './openingHours.js'

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

  it('limits possible ends to the next occupied reservation', () => {
    expect(getPossibleEndTimes(weeklySchedule, '2026-11-16', '09:00', [
      { inicio: '10:00:00', fim: '11:00:00', statusReserva: 'Confirmada' },
    ])).toEqual(['09:30', '10:00'])
  })

  it('allows the end at closing time but never after 21:00', () => {
    expect(getPossibleEndTimes(weeklySchedule, '2026-11-16', '20:30')).toEqual(['21:00'])
  })

  it('chooses a one-hour default when possible, otherwise the longest option', () => {
    expect(getDefaultEndTime(['09:30', '10:00', '10:30'], '09:00')).toBe('10:00')
    expect(getDefaultEndTime(['09:30'], '09:00')).toBe('09:30')
  })

  it('does not offer an expand action when all end choices fit in the initial list', () => {
    const options = getEndTimeDisplayOptions(['09:30', '10:00', '10:30'], '10:00')
    expect(options).toEqual({
      visibleEndTimes: ['09:30', '10:00', '10:30'],
      hasMore: false,
      expanded: false,
    })
  })

  it('limits long end lists to eight choices and keeps the one-hour default among them', () => {
    const endTimes = Array.from({ length: 12 }, (_, index) => {
      const minutes = 8 * 60 + (index + 1) * 30
      return `${String(Math.floor(minutes / 60)).padStart(2, '0')}:${String(minutes % 60).padStart(2, '0')}`
    })
    const options = getEndTimeDisplayOptions(endTimes, '09:00')
    expect(options.visibleEndTimes).toHaveLength(8)
    expect(options.visibleEndTimes).toContain('09:00')
    expect(options.hasMore).toBe(true)
    expect(options.expanded).toBe(false)
  })

  it('starts expanded for a selected end beyond the initial limit and retains it when collapsed', () => {
    const endTimes = Array.from({ length: 12 }, (_, index) => {
      const minutes = 8 * 60 + (index + 1) * 30
      return `${String(Math.floor(minutes / 60)).padStart(2, '0')}:${String(minutes % 60).padStart(2, '0')}`
    })
    const selectedEnd = '14:00'
    const initial = getEndTimeDisplayOptions(endTimes, selectedEnd)
    expect(initial.expanded).toBe(true)
    expect(initial.visibleEndTimes).toEqual(endTimes)
    const collapsed = getEndTimeDisplayOptions(endTimes, selectedEnd, false)
    expect(collapsed.expanded).toBe(false)
    expect(collapsed.visibleEndTimes).toContain(selectedEnd)
  })

  it('excludes past starts today while retaining future starts', () => {
    const now = { date: '2026-11-16', time: '10:15' }
    const shortDay = weeklySchedule.map(hours => hours.diaSemana === 1 ? { ...hours, fechamento: '11:30:00' } : hours)
    expect(getPossibleEndTimes(shortDay, '2026-11-16', '10:00', [], now)).toEqual([])
    expect(getPossibleEndTimes(shortDay, '2026-11-16', '10:30', [], now)).toEqual(['11:00', '11:30'])
  })

  it('formats the displayed schedule using the approval flag from the API', () => {
    expect(formatWeeklyHours(weeklySchedule))
      .toBe('Segunda a sexta: 07:00 às 21:00. Sábado a domingo: Somente com aprovação.')
  })
})
