import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { Avatar, BookingModal } from './App.jsx'

const originalCreateObjectURL = URL.createObjectURL
const originalRevokeObjectURL = URL.revokeObjectURL
const originalFetch = globalThis.fetch

beforeEach(() => {
  globalThis.fetch = vi.fn()
  URL.createObjectURL = vi.fn(() => 'blob:avatar-test')
  URL.revokeObjectURL = vi.fn()
})

afterEach(() => {
  cleanup()
  vi.restoreAllMocks()
  globalThis.fetch = originalFetch
  URL.createObjectURL = originalCreateObjectURL
  URL.revokeObjectURL = originalRevokeObjectURL
})

describe('Avatar', () => {
  it('loads an authenticated user photo as a blob', async () => {
    globalThis.fetch.mockResolvedValue({
      ok: true,
      status: 200,
      headers: { get: () => '"avatar-etag"' },
      blob: async () => new Blob(['image-data'], { type: 'image/jpeg' }),
    })

    const { container, unmount } = render(<Avatar user={{ idUsuario: 501, nomeCompleto: 'Ana Souza', temFoto: true, fotoVersao: 1 }} />)

    await screen.findByRole('img', { name: 'Avatar de Ana Souza' })
    await waitFor(() => expect(container.querySelector('img')?.getAttribute('src')).toBe('blob:avatar-test'))
    await waitFor(() => expect(globalThis.fetch).toHaveBeenCalledWith(
      expect.stringContaining('/usuarios/501/foto'),
      expect.objectContaining({ credentials: 'include' }),
    ))
    expect(URL.createObjectURL).toHaveBeenCalledOnce()
    unmount()
    expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:avatar-test')
  })

  it('reuses the cached blob with an If-None-Match request', async () => {
    globalThis.fetch
      .mockResolvedValueOnce({
        ok: true,
        status: 200,
        headers: { get: () => '"avatar-etag"' },
        blob: async () => new Blob(['image-data'], { type: 'image/jpeg' }),
      })
      .mockResolvedValueOnce({ ok: false, status: 304 })
    const { rerender, container } = render(
      <Avatar user={{ idUsuario: 504, nomeCompleto: 'Ana Souza', temFoto: true, fotoVersao: 1 }} />,
    )
    await waitFor(() => expect(container.querySelector('img')).toBeTruthy())

    rerender(<Avatar user={{ idUsuario: 504, nomeCompleto: 'Ana Souza', temFoto: true, fotoVersao: 2 }} />)

    await waitFor(() => expect(globalThis.fetch).toHaveBeenCalledTimes(2))
    expect(globalThis.fetch.mock.calls[1][1].headers).toEqual({ 'If-None-Match': '"avatar-etag"' })
    await waitFor(() => expect(container.querySelector('img')).toBeTruthy())
  })

  it('shows initials when no photo is registered', () => {
    render(<Avatar user={{ idUsuario: 502, nomeCompleto: 'Ana Souza', temFoto: false }} />)

    expect(screen.getByRole('img', { name: 'Avatar de Ana Souza' }).textContent).toBe('AS')
    expect(globalThis.fetch).not.toHaveBeenCalled()
  })

  it('falls back to initials when the photo endpoint returns 404', async () => {
    globalThis.fetch.mockResolvedValue({ ok: false, status: 404 })

    render(<Avatar user={{ idUsuario: 503, nomeCompleto: 'Ana Souza', temFoto: true, fotoVersao: 1 }} />)

    await waitFor(() => expect(screen.getByRole('img', { name: 'Avatar de Ana Souza' }).textContent).toBe('AS'))
  })
})

describe('BookingModal', () => {
  it('offers login or signup after an unauthenticated reservation and keeps the draft', async () => {
    const onRequireAuthentication = vi.fn()
    const close = vi.fn()
    const form = {
      dataReserva: '2026-11-01',
      horarioInicio: '09:30',
      horarioFim: '10:30',
      tituloEvento: 'Oficina de teste',
      descricaoFinalidade: 'Continuar após autenticar',
      quantidadeParticipantes: '',
    }
    globalThis.fetch.mockResolvedValue({
      ok: false,
      status: 403,
      text: async () => JSON.stringify({ error: 'Forbidden' }),
    })

    render(
      <BookingModal
        space={{ idEspaco: 12, nomeEspaco: 'Coworking', tipoEspaco: 'Coworking', capacidadePessoas: 20 }}
        close={close}
        onSaved={vi.fn()}
        onRequireAuthentication={onRequireAuthentication}
        initialDate={form.dataReserva}
        initialTime={form.horarioInicio}
        initialEndTime={form.horarioFim}
        initialForm={form}
      />,
    )

    fireEvent.submit(screen.getByRole('button', { name: /Continuar solicitação/ }).closest('form'))

    expect(await screen.findByText(/A solicitação não foi enviada/)).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: 'Criar conta' }))

    expect(onRequireAuthentication).toHaveBeenCalledWith('signup', { spaceId: 12, form })
    expect(close).not.toHaveBeenCalled()
  })
})
