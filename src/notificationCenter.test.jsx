import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { NotificationCenter } from './App.jsx'

function todayAtSaoPauloNoon() {
  const parts = Object.fromEntries(new Intl.DateTimeFormat('en-CA', {
    timeZone: 'America/Sao_Paulo',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).formatToParts(new Date()).map(part => [part.type, part.value]))
  return `${parts.year}-${parts.month}-${parts.day}T12:00:00`
}

describe('notification center drawer', () => {
  afterEach(() => {
    cleanup()
    vi.unstubAllGlobals()
  })

  it('shows a 300-character legacy message and updates the unread badge after marking all read', async () => {
    const message = 'x'.repeat(300)
    const notifications = [{
      idNotificacao: 12,
      tipo: 'RESERVA_CRIADA',
      titulo: 'Solicitação de reserva recebida',
      mensagem: message,
      lida: false,
      dataHora: todayAtSaoPauloNoon(),
      statusEmail: 'DESATIVADO',
    }]
    const fetchMock = vi.fn(async (url, options = {}) => {
      if (String(url).endsWith('/notificacoes/minhas')) {
        return { status: 200, ok: true, text: async () => JSON.stringify(notifications) }
      }
      if (String(url).endsWith('/notificacoes/lidas') && options.method === 'PATCH') {
        return { status: 204, ok: true, text: async () => '' }
      }
      throw new Error(`Unexpected request: ${String(url)}`)
    })
    vi.stubGlobal('fetch', fetchMock)

    render(<NotificationCenter user={{ idUsuario: 1 }} />)
    const trigger = await screen.findByRole('button', { name: 'Notificações, 1 não lidas' })

    fireEvent.click(trigger)
    const drawer = await screen.findByRole('dialog', { name: 'Notificações' })
    expect(drawer).toBeTruthy()
    expect(screen.getByText(message)).toBeTruthy()
    expect(screen.queryByText(/E-mail desativado/)).toBeNull()
    expect(screen.getByRole('heading', { name: 'Hoje' })).toBeTruthy()

    fireEvent.click(screen.getByRole('button', { name: 'Marcar todas como lidas' }))

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Notificações' })).toBeTruthy()
    })
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining('/notificacoes/lidas'),
      expect.objectContaining({ method: 'PATCH' }),
    )
    expect(drawer.querySelector('.notification-unread')).toBeNull()
  })

  it('closes from the backdrop or Escape and restores focus to the bell', async () => {
    vi.stubGlobal('fetch', vi.fn(async url => {
      if (String(url).endsWith('/notificacoes/minhas')) {
        return { status: 200, ok: true, text: async () => '[]' }
      }
      throw new Error(`Unexpected request: ${String(url)}`)
    }))

    const { rerender } = render(<NotificationCenter user={{ idUsuario: 1 }} routeKey="overview" />)
    const trigger = await screen.findByRole('button', { name: 'Notificações' })

    fireEvent.click(trigger)
    await screen.findByRole('dialog', { name: 'Notificações' })
    fireEvent.click(document.querySelector('.notification-backdrop'))
    await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull())
    await waitFor(() => expect(document.activeElement).toBe(trigger))

    fireEvent.click(trigger)
    await screen.findByRole('dialog', { name: 'Notificações' })
    fireEvent.keyDown(document, { key: 'Escape' })
    await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull())
    await waitFor(() => expect(document.activeElement).toBe(trigger))

    fireEvent.click(trigger)
    await screen.findByRole('dialog', { name: 'Notificações' })
    rerender(<NotificationCenter user={{ idUsuario: 1 }} routeKey="reservas" />)
    await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull())
    await waitFor(() => expect(document.activeElement).toBe(trigger))
  })
})
