import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { AccessPage, Avatar, BookingModal, CalendarPage, LoginInline, SignupPage } from './App.jsx'

const originalCreateObjectURL = URL.createObjectURL
const originalRevokeObjectURL = URL.revokeObjectURL
const originalFetch = globalThis.fetch
const originalScrollIntoView = Object.getOwnPropertyDescriptor(HTMLElement.prototype, 'scrollIntoView')

beforeEach(() => {
  globalThis.fetch = vi.fn()
  URL.createObjectURL = vi.fn(() => 'blob:avatar-test')
  URL.revokeObjectURL = vi.fn()
})

afterEach(() => {
  cleanup()
  vi.restoreAllMocks()
  globalThis.fetch = originalFetch
  localStorage.removeItem('inpetu-user')
  URL.createObjectURL = originalCreateObjectURL
  URL.revokeObjectURL = originalRevokeObjectURL
  if (originalScrollIntoView) Object.defineProperty(HTMLElement.prototype, 'scrollIntoView', originalScrollIntoView)
  else delete HTMLElement.prototype.scrollIntoView
})

describe('Login password visibility', () => {
  it.each([
    ['public login', AccessPage, { onLogin: vi.fn(), onSignup: vi.fn() }],
    ['team login', LoginInline, { onLogin: vi.fn() }],
  ])('shows and hides the password without changing its value in the %s', (_, Login, props) => {
    render(<Login {...props} />)
    const input = screen.getByPlaceholderText('Sua senha')

    fireEvent.change(input, { target: { value: 'SenhaSegura123!' } })
    expect(input.type).toBe('password')
    fireEvent.click(screen.getByRole('button', { name: 'Mostrar senha' }))
    expect(input.type).toBe('text')
    expect(input.value).toBe('SenhaSegura123!')
    fireEvent.click(screen.getByRole('button', { name: 'Ocultar senha' }))
    expect(input.type).toBe('password')
    expect(input.value).toBe('SenhaSegura123!')
  })

  it('logs in with cookie credentials when the API accepts the session', async () => {
    globalThis.fetch.mockResolvedValue({
      ok: true,
      status: 200,
      text: async () => JSON.stringify({ idUsuario: 7, nome: 'Ana Souza', perfil: 4 }),
    })
    const onLogin = vi.fn()
    render(<AccessPage onLogin={onLogin} onSignup={vi.fn()} />)
    fireEvent.change(screen.getByLabelText(/E-mail/), { target: { value: 'ana@example.org' } })
    fireEvent.change(screen.getByPlaceholderText('Sua senha'), { target: { value: 'Senha123!' } })
    fireEvent.submit(screen.getByRole('button', { name: /Entrar com e-mail e senha/ }).closest('form'))

    await waitFor(() => expect(onLogin).toHaveBeenCalledWith(
      expect.objectContaining({ idUsuario: 7, perfil: 4 }),
    ))
    expect(globalThis.fetch).toHaveBeenCalledWith(
      expect.stringContaining('/auth/login'),
      expect.objectContaining({ credentials: 'include' }),
    )
  })

  it('shows a clear message for invalid login credentials', async () => {
    globalThis.fetch.mockResolvedValue({
      ok: false,
      status: 401,
      text: async () => JSON.stringify({ error: 'Unauthorized' }),
    })
    render(<AccessPage onLogin={vi.fn()} onSignup={vi.fn()} />)
    fireEvent.change(screen.getByLabelText(/E-mail/), { target: { value: 'ana@example.org' } })
    fireEvent.change(screen.getByPlaceholderText('Sua senha'), { target: { value: 'SenhaErrada!' } })
    fireEvent.submit(screen.getByRole('button', { name: /Entrar com e-mail e senha/ }).closest('form'))

    expect((await screen.findByRole('alert')).textContent).toContain('E-mail ou senha incorretos.')
  })
})

describe('Public signup phone format', () => {
  it('sends the formatted phone value accepted by the API', async () => {
    globalThis.fetch.mockResolvedValue({
      ok: true,
      status: 202,
      text: async () => JSON.stringify({ mensagem: 'Código enviado.' }),
    })
    const onCodeSent = vi.fn()
    render(<SignupPage onBack={vi.fn()} onCodeSent={onCodeSent} />)
    fireEvent.change(screen.getByLabelText(/Nome completo/), { target: { value: 'Ana Souza' } })
    fireEvent.change(screen.getByLabelText(/E-mail/), { target: { value: 'ana@example.org' } })
    fireEvent.change(screen.getByPlaceholderText('(48) 99999-9999'), { target: { value: '48999991234' } })
    fireEvent.change(screen.getByLabelText(/Organização/), { target: { value: 'UFSC' } })
    fireEvent.change(screen.getByLabelText(/Cargo ou vínculo/), { target: { value: 'Pesquisadora' } })
    fireEvent.change(screen.getByLabelText(/^Senha/), { target: { value: 'Senha123!' } })
    fireEvent.change(screen.getByLabelText(/Confirmar senha/), { target: { value: 'Senha123!' } })
    fireEvent.submit(screen.getByRole('button', { name: /Criar conta/ }).closest('form'))

    await waitFor(() => expect(onCodeSent).toHaveBeenCalledWith('ana@example.org'))
    expect(JSON.parse(globalThis.fetch.mock.calls[0][1].body).telefone).toBe('(48) 99999-1234')
  })

  it('explains invalid phone format before making a request', async () => {
    render(<SignupPage onBack={vi.fn()} onCodeSent={vi.fn()} />)
    fireEvent.change(screen.getByLabelText(/Nome completo/), { target: { value: 'Ana Souza' } })
    fireEvent.change(screen.getByLabelText(/E-mail/), { target: { value: 'ana@example.org' } })
    fireEvent.change(screen.getByPlaceholderText('(48) 99999-9999'), { target: { value: '4833334444' } })
    fireEvent.change(screen.getByLabelText(/Organização/), { target: { value: 'UFSC' } })
    fireEvent.change(screen.getByLabelText(/Cargo ou vínculo/), { target: { value: 'Pesquisadora' } })
    fireEvent.change(screen.getByLabelText(/^Senha/), { target: { value: 'Senha123!' } })
    fireEvent.change(screen.getByLabelText(/Confirmar senha/), { target: { value: 'Senha123!' } })
    fireEvent.submit(screen.getByRole('button', { name: /Criar conta/ }).closest('form'))

    expect((await screen.findByRole('alert')).textContent).toContain('Informe um celular no formato (48) 99999-9999.')
    expect(globalThis.fetch).not.toHaveBeenCalled()
  })
})

describe('Calendar navigation', () => {
  const spaces = [{ idEspaco: 42, nomeEspaco: 'Coworking', tipoEspaco: 'Coworking', ativo: true, capacidadePessoas: 35 }]

  beforeEach(() => {
    globalThis.fetch.mockResolvedValue({
      ok: true,
      status: 200,
      text: async () => '[]',
    })
    Object.defineProperty(HTMLElement.prototype, 'scrollIntoView', { configurable: true, value: vi.fn() })
  })

  it('scrolls to date and time after selecting a space', async () => {
    render(<CalendarPage spaces={spaces} spacesLoaded initialDate="2026-10-15" onReserve={vi.fn()} />)

    fireEvent.click(screen.getByRole('button', { name: /Coworking, até 35 pessoas/ }))

    await waitFor(() => expect(HTMLElement.prototype.scrollIntoView).toHaveBeenCalledWith({
      behavior: 'smooth',
      block: 'start',
    }))
  })

  it('selects and scrolls to the requested space when opened from its availability link', async () => {
    render(<CalendarPage spaces={spaces} spacesLoaded initialDate="2026-10-15" initialSpaceId="42" onReserve={vi.fn()} />)

    expect(screen.getByRole('button', { name: /Coworking, até 35 pessoas/ }).getAttribute('aria-pressed')).toBe('true')
    await waitFor(() => expect(HTMLElement.prototype.scrollIntoView).toHaveBeenCalled())
  })
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
  it('submits a booking with the authenticated cookie session', async () => {
    globalThis.fetch.mockResolvedValue({
      ok: true,
      status: 201,
      text: async () => JSON.stringify({ idReserva: 31, statusReserva: 'Pendente' }),
    })
    const onSaved = vi.fn()
    const close = vi.fn()
    render(
      <BookingModal
        space={{ idEspaco: 12, nomeEspaco: 'Coworking', tipoEspaco: 'Coworking', capacidadePessoas: 20 }}
        close={close}
        onSaved={onSaved}
        onRequireAuthentication={vi.fn()}
        initialDate="2026-11-01"
        initialTime="09:30"
        initialEndTime="10:30"
        initialForm={{
          dataReserva: '2026-11-01',
          horarioInicio: '09:30',
          horarioFim: '10:30',
          tituloEvento: 'Oficina de teste',
          descricaoFinalidade: 'Teste autenticado',
          quantidadeParticipantes: '',
        }}
      />,
    )

    fireEvent.submit(screen.getByRole('button', { name: /Continuar solicitação/ }).closest('form'))

    await waitFor(() => expect(onSaved).toHaveBeenCalledWith({ idReserva: 31, statusReserva: 'Pendente' }))
    expect(globalThis.fetch).toHaveBeenCalledWith(
      expect.stringContaining('/reservas'),
      expect.objectContaining({ credentials: 'include' }),
    )
    expect(close).toHaveBeenCalledOnce()
  })

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
