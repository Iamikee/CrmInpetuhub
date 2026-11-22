import { cleanup, fireEvent, render, screen, within } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { BookingModal } from './App.jsx'

const operatingHours = Array.from({ length: 7 }, (_, index) => ({
  diaSemana: index + 1,
  fechado: false,
  exigeAprovacao: index > 4,
  abertura: '08:00:00',
  fechamento: '21:00:00',
}))

function renderBookingModal({ initialTime = '08:00', initialEndTime = '09:00' } = {}) {
  return render(<BookingModal
    space={{ idEspaco: 42, nomeEspaco: 'Coworking', tipoEspaco: 'Coworking', capacidadePessoas: 20, ativo: true }}
    close={vi.fn()}
    onSaved={vi.fn()}
    openLogin={vi.fn()}
    initialDate="2099-11-16"
    initialTime={initialTime}
    initialEndTime={initialEndTime}
  />)
}

describe('booking modal time summary and editor', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn(async url => ({
      ok: true,
      status: 200,
      text: async () => JSON.stringify(String(url).includes('/agenda?') ? [] : operatingHours),
    })))
  })

  afterEach(() => {
    cleanup()
    vi.unstubAllGlobals()
  })

  it('starts with the compact summary, edits the end, and keeps it after concluding', async () => {
    renderBookingModal()
    const dialog = await screen.findByRole('dialog', { name: 'Solicitar este espaço' })

    expect(await within(dialog).findByText('16/11/2099')).toBeTruthy()
    expect(dialog.querySelector('.booking-modal-time-summary').textContent).toContain('08:00 às 09:00 (1 h)')
    expect(within(dialog).queryByRole('button', { name: /09:30/ })).toBeNull()

    const changeTimeButton = within(dialog).getByRole('button', { name: 'Alterar horário' })
    expect(changeTimeButton.getAttribute('aria-expanded')).toBe('false')
    fireEvent.click(changeTimeButton)

    const endChoice = await within(dialog).findByRole('button', { name: '09:30, 1 h 30 min' })
    expect(within(dialog).getByText('Passo 1: Início')).toBeTruthy()
    expect(within(dialog).getByText('Passo 2: Agora escolha o fim')).toBeTruthy()
    fireEvent.click(endChoice)

    expect(endChoice.getAttribute('aria-pressed')).toBe('true')
    expect(dialog.querySelector('.booking-time-summary').textContent).toBe('08:00 às 09:30 (1 h 30 min)')

    const concludeButton = within(dialog).getByRole('button', { name: 'Concluir' })
    expect(concludeButton.getAttribute('aria-expanded')).toBe('true')
    fireEvent.click(concludeButton)

    expect(dialog.querySelector('.booking-modal-time-summary').textContent).toContain('08:00 às 09:30 (1 h 30 min)')
    expect(within(dialog).getByRole('button', { name: 'Alterar horário' }).getAttribute('aria-expanded')).toBe('false')
    expect(within(dialog).queryByRole('button', { name: /09:30/ })).toBeNull()
  })

  it('opens the selector when the modal has no chosen time', async () => {
    renderBookingModal({ initialTime: '', initialEndTime: '' })
    const dialog = await screen.findByRole('dialog', { name: 'Solicitar este espaço' })

    expect(await within(dialog).findByText('Passo 1: Início')).toBeTruthy()
    expect(dialog.querySelector('.booking-modal-time-editor .booking-modal-time-toggle').getAttribute('aria-expanded')).toBe('true')
    expect(within(dialog).queryByText('Horário não selecionado')).toBeNull()
  })
})
