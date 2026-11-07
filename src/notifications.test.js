import { describe, expect, it } from 'vitest'
import { formatRelativeNotificationDate, groupNotificationsByDay, normalizeNotificationMessage, notificationDestination } from './notifications.js'

describe('notification presentation', () => {
  const now = Date.parse('2025-03-01T12:00:00Z')

  it('formats past and future notification times in Portuguese', () => {
    expect(formatRelativeNotificationDate('2025-03-01T11:59:00Z', now)).toBe('há 1 minuto')
    expect(formatRelativeNotificationDate('2025-03-01T11:59:30Z', now)).toBe('há 1 minuto')
    expect(formatRelativeNotificationDate('2025-03-03T12:00:00Z', now)).toBe('depois de amanhã')
  })

  it('marks invalid dates explicitly', () => {
    expect(formatRelativeNotificationDate('invalid', now)).toBe('Data indisponível')
  })

  it('groups notifications by their America/Sao_Paulo calendar date', () => {
    const groups = groupNotificationsByDay([
      { idNotificacao: 1, dataHora: '2025-03-01T10:00:00' },
      { idNotificacao: 2, dataHora: '2025-02-28T23:59:00' },
      { idNotificacao: 3, dataHora: '2025-02-27T23:59:00' },
      { idNotificacao: 4, dataHora: '2025-03-01T01:00:00Z' },
    ], now)

    expect(groups.map(group => [group.label, group.items.map(item => item.idNotificacao)])).toEqual([
      ['Hoje', [1]],
      ['Ontem', [2, 4]],
      ['Anteriores', [3]],
    ])
  })

  it('routes reservation and ticket notifications to their matching sections', () => {
    expect(notificationDestination('RESERVA_CRIADA')).toBe('reservation')
    expect(notificationDestination('TICKET_RESPONDIDO')).toBe('support')
  })

  it('normalizes legacy dash-separated messages without losing their content', () => {
    expect(normalizeNotificationMessage('Reserva aprovada — dia 10 – sala principal'))
      .toBe('Reserva aprovada, dia 10, sala principal')
    expect(normalizeNotificationMessage('  ')).toBe('Sem mensagem disponível.')
  })
})
