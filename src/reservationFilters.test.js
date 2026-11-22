import { describe, expect, it } from 'vitest'
import { filterReservations } from './reservationFilters.js'

function reservation(overrides = {}) {
  return {
    idReserva: 80,
    tituloEvento: 'Oficina de prototipagem',
    espaco: 'Sala de inovação',
    dataReserva: '2026-11-22',
    dataSolicitacao: '2026-11-21T23:30:00',
    statusReserva: 'Confirmada',
    ...overrides,
  }
}

describe('filterReservations', () => {
  it('searches titles without case or accent differences', () => {
    const item = reservation({ tituloEvento: 'Oficina de prototipagem Ágil' })

    expect(filterReservations([item], { texto: 'OFICINA DE PROTOTIPAGEM agil' })).toEqual([item])
  })

  it('searches the request number with or without a hash', () => {
    expect(filterReservations([reservation()], { texto: '#80' })).toHaveLength(1)
    expect(filterReservations([reservation()], { texto: '80' })).toHaveLength(1)
  })

  it('searches by the space name', () => {
    expect(filterReservations([reservation()], { texto: 'inovacao' })).toHaveLength(1)
  })

  it('filters by meeting date from 24 November through 24 November', () => {
    const item = reservation({ dataReserva: '2026-11-24' })

    expect(filterReservations([item], {
      campoData: 'dataReserva',
      de: '2026-11-24',
      ate: '2026-11-24',
    })).toEqual([item])
  })

  it('supports only a start date', () => {
    const items = [reservation({ dataReserva: '2026-11-21' }), reservation(), reservation({ dataReserva: '2026-11-23' })]

    expect(filterReservations(items, { de: '2026-11-22' })).toEqual([items[1], items[2]])
  })

  it('supports only an end date', () => {
    const items = [reservation({ dataReserva: '2026-11-21' }), reservation(), reservation({ dataReserva: '2026-11-23' })]

    expect(filterReservations(items, { ate: '2026-11-22' })).toEqual([items[0], items[1]])
  })

  it('includes the full day when start and end dates are equal', () => {
    const item = reservation()

    expect(filterReservations([item], { de: '2026-11-22', ate: '2026-11-22' })).toEqual([item])
  })

  it('does not move a request made at 23:30 on 22 November into 23 November', () => {
    const item = reservation({ dataSolicitacao: '2026-11-22T23:30:00' })

    expect(filterReservations([item], {
      campoData: 'solicitacao',
      de: '2026-11-23',
    })).toEqual([])
  })

  it('matches a 21:00 request and a meeting date on the same day', () => {
    const item = reservation({
      dataReserva: '2026-11-24',
      dataSolicitacao: '2026-11-24T21:00:00',
    })

    expect(filterReservations([item], {
      campoData: 'dataReserva',
      de: '2026-11-24',
      ate: '2026-11-24',
    })).toEqual([item])
    expect(filterReservations([item], {
      campoData: 'solicitacao',
      de: '2026-11-24',
      ate: '2026-11-24',
    })).toEqual([item])
  })

  it('combines text, status, and date filters', () => {
    const matching = reservation({ statusReserva: 'Pendente' })
    const items = [
      matching,
      reservation({ tituloEvento: 'Outra pesquisa', statusReserva: 'Pendente' }),
      reservation({ statusReserva: 'Confirmada' }),
    ]

    expect(filterReservations(items, {
      texto: 'OFICINA',
      status: 'Pendente',
      de: '2026-11-22',
      ate: '2026-11-22',
    })).toEqual([matching])
  })

  it('returns an empty list when there are no reservations', () => {
    expect(filterReservations([], { texto: 'oficina' })).toEqual([])
  })

  it('does not apply an inverted date range', () => {
    const item = reservation()

    expect(filterReservations([item], { de: '2026-11-23', ate: '2026-11-21' })).toEqual([item])
  })
})
