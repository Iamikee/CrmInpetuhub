import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { ReservationsPage } from './PublicPages.jsx'

function dateOffset(offset) {
  const date = new Date()
  date.setHours(12, 0, 0, 0)
  date.setDate(date.getDate() + offset)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

function nextSunday() {
  const date = new Date()
  const daysUntilSunday = (7 - date.getDay()) % 7 || 7
  return dateOffset(daysUntilSunday)
}

function reservation(overrides = {}) {
  return {
    idReserva: 12,
    tituloEvento: 'Encontro Érico Ávila com título de teste',
    espaco: 'Sala de demonstração',
    dataReserva: dateOffset(20),
    horarioInicio: '10:00:00',
    horarioFim: '11:30:00',
    statusReserva: 'Confirmada',
    ...overrides,
  }
}

function renderPage({ items = [], reservationsLoaded = true, onRefresh = vi.fn(async () => true), onBack = vi.fn() } = {}) {
  return {
    ...render(<ReservationsPage items={items} reservationsLoaded={reservationsLoaded} onRefresh={onRefresh} onBack={onBack} onCancel={vi.fn()} />),
    onBack,
    onRefresh,
  }
}

describe('reservations page', () => {
  afterEach(() => {
    cleanup()
    vi.restoreAllMocks()
  })

  it('shows a styled empty state without table headings when there are no reservations', () => {
    renderPage()

    expect(screen.getByText('Nenhuma reserva encontrada com esses filtros')).toBeTruthy()
    expect(document.querySelector('table')).toBeNull()
    expect(document.querySelector('.reservations-empty svg')).toBeTruthy()
  })

  it('filters by title after typing and clears active filters', async () => {
    renderPage({ items: [reservation({ tituloEvento: 'Oficina de prototipagem' }), reservation({ idReserva: 13, tituloEvento: 'Encontro de pesquisa' })] })
    const search = screen.getByRole('searchbox', { name: 'Buscar por nome, espaço ou solicitação' })

    fireEvent.change(search, { target: { value: 'oficina' } })

    expect(await screen.findByText('Mostrando 1 de 2 reservas')).toBeTruthy()
    expect(screen.getByText('Oficina de prototipagem')).toBeTruthy()
    expect(screen.queryByText('Encontro de pesquisa')).toBeNull()
    fireEvent.click(screen.getByRole('button', { name: 'Limpar filtros' }))
    expect(screen.getByText('Mostrando 2 de 2 reservas')).toBeTruthy()
  })

  it('searches by the space name from the loaded space list', async () => {
    renderPage({
      items: [reservation({ espaco: undefined, idEspaco: 6 })],
      spaces: [{ idEspaco: 6, nomeEspaco: 'Sala de inovação' }],
    })
    fireEvent.change(screen.getByRole('searchbox', { name: 'Buscar por nome, espaço ou solicitação' }), { target: { value: 'inovacao' } })

    expect(await screen.findByText('Mostrando 1 de 1 reservas')).toBeTruthy()
  })

  it('announces an inverted date range after the date field loses focus without applying it', () => {
    renderPage({ items: [reservation(), reservation({ idReserva: 13, dataReserva: dateOffset(21) })] })
    const from = screen.getByLabelText('De')
    const to = screen.getByLabelText('Até')

    fireEvent.change(from, { target: { value: '2026-11-23' } })
    fireEvent.change(to, { target: { value: '2026-11-22' } })
    expect(screen.queryByText('A data Até deve ser igual ou posterior à data De.')).toBeNull()

    fireEvent.blur(to)
    expect(screen.getByText('A data Até deve ser igual ou posterior à data De.')).toBeTruthy()
    expect(screen.getByText('Mostrando 2 de 2 reservas')).toBeTruthy()
  })

  it('shows the initial loading state', () => {
    renderPage({ reservationsLoaded: false })

    expect(screen.getByRole('status').textContent).toBe('Carregando suas reservas...')
    expect(screen.getByRole('button', { name: 'Atualizar' }).disabled).toBe(true)
  })

  it('keeps already-loaded reservations visible when a manual refresh fails', async () => {
    renderPage({ items: [reservation()], onRefresh: vi.fn(async () => false) })

    fireEvent.click(screen.getByRole('button', { name: 'Atualizar' }))

    expect((await screen.findByRole('alert')).textContent).toBe('Não foi possível atualizar suas reservas. Tente novamente.')
    expect(screen.getByText('Encontro Érico Ávila com título de teste')).toBeTruthy()
  })

  it('disables refresh and announces the loading label while the request is pending', async () => {
    let finishRefresh
    const onRefresh = vi.fn(() => new Promise(resolve => { finishRefresh = resolve }))
    renderPage({ items: [reservation()], onRefresh })

    fireEvent.click(screen.getByRole('button', { name: 'Atualizar' }))

    const button = screen.getByRole('button', { name: 'Atualizando...' })
    expect(button.disabled).toBe(true)
    finishRefresh(true)
    await waitFor(() => expect(screen.getByRole('button', { name: 'Atualizar' }).disabled).toBe(false))
  })

  it('shows an accented title and a separate request number, with Portuguese date and minute-only time', () => {
    renderPage({ items: [reservation({ tituloEvento: 'Á É Í Ó Ú Ç Ã Õ' })] })

    expect(screen.getByText('Á É Í Ó Ú Ç Ã Õ').className).toContain('reservation-title')
    expect(screen.getByText('Solicitação #12')).toBeTruthy()
    expect(document.querySelector('time').textContent.replace(/\s+/g, ' ')).toMatch(/10:00 às 11:30$/)
  })

  it('shows the Sunday approval note in a separate block for a pending reservation', () => {
    renderPage({ items: [reservation({ dataReserva: nextSunday(), statusReserva: 'Pendente' })] })

    expect(document.querySelector('.reservation-status').textContent.trim()).toBe('Pendente')
    expect(screen.getByText('Pedido de domingo aguardando aprovação da equipe.').className).toContain('reservation-sunday-note')
  })

  it('does not show a cancel action or short-notice warning for a cancelled reservation', () => {
    renderPage({ items: [reservation({ statusReserva: 'Cancelada', dataReserva: dateOffset(3) })] })

    expect(screen.queryByRole('button', { name: 'Cancelar' })).toBeNull()
    expect(screen.queryByText('Menos de 15 dias: solicite exceção à equipe.')).toBeNull()
  })

  it('does not show a short-notice warning for a past reservation', () => {
    renderPage({ items: [reservation({ dataReserva: dateOffset(-1) })] })

    expect(screen.queryByText('Menos de 15 dias: solicite exceção à equipe.')).toBeNull()
    expect(screen.queryByRole('button', { name: 'Cancelar' })).toBeNull()
  })

  it('shows the existing short-notice message for an upcoming reservation within 15 days', () => {
    renderPage({ items: [reservation({ dataReserva: dateOffset(7) })] })

    expect(screen.getByText('Menos de 15 dias: solicite exceção à equipe.')).toBeTruthy()
    expect(screen.queryByRole('button', { name: 'Cancelar' })).toBeNull()
  })

  it('offers cancellation starting 15 days before the reservation', () => {
    renderPage({ items: [reservation({ dataReserva: dateOffset(15) })] })

    expect(screen.getByRole('button', { name: 'Cancelar' })).toBeTruthy()
    expect(screen.queryByText('Menos de 15 dias: solicite exceção à equipe.')).toBeNull()
  })

  it('navigates back to the requester portal with an accessible button', () => {
    const { onBack } = renderPage()
    const button = screen.getByRole('button', { name: 'Voltar ao portal' })

    button.focus()
    expect(document.activeElement).toBe(button)
    expect(button.type).toBe('button')
    fireEvent.click(button)
    expect(onBack).toHaveBeenCalledOnce()
  })
})
