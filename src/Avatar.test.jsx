import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { Avatar, LoginInline, Modal, Title } from './App.jsx'
import CalendarPage from './CalendarPage.jsx'
import { AccessPage, PasswordChangePage, SignupPage } from './PublicPages.jsx'

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
  it('requires a surname before sending a signup request', async () => {
    render(<SignupPage onBack={vi.fn()} onCodeSent={vi.fn()} />)
    fireEvent.change(screen.getByLabelText(/Nome completo e sobrenome/), { target: { value: 'Ana' } })
    fireEvent.submit(screen.getByRole('button', { name: /Criar conta/ }).closest('form'))

    expect((await screen.findByRole('alert')).textContent).toContain('Informe nome e sobrenome.')
    expect(globalThis.fetch).not.toHaveBeenCalled()
  })

  it('shows and hides both signup passwords without changing their values', () => {
    render(<SignupPage onBack={vi.fn()} onCodeSent={vi.fn()} />)
    const password = screen.getByLabelText(/^Senha/)
    const confirmation = screen.getByLabelText(/Confirmar senha/)

    fireEvent.change(password, { target: { value: 'SenhaSegura123!' } })
    fireEvent.change(confirmation, { target: { value: 'SenhaSegura123!' } })
    fireEvent.click(screen.getByRole('button', { name: 'Mostrar senha' }))
    fireEvent.click(screen.getByRole('button', { name: 'Mostrar confirmação da senha' }))

    expect(password.type).toBe('text')
    expect(password.value).toBe('SenhaSegura123!')
    expect(confirmation.type).toBe('text')
    expect(confirmation.value).toBe('SenhaSegura123!')

    fireEvent.click(screen.getByRole('button', { name: 'Ocultar senha' }))
    fireEvent.click(screen.getByRole('button', { name: 'Ocultar confirmação da senha' }))
    expect(password.type).toBe('password')
    expect(password.value).toBe('SenhaSegura123!')
    expect(confirmation.type).toBe('password')
    expect(confirmation.value).toBe('SenhaSegura123!')
  })

  it('shows and hides each password in the temporary-password form', () => {
    render(<PasswordChangePage user={{ nome: 'Ana' }} onLogout={vi.fn()} onPasswordChanged={vi.fn()} />)
    const temporaryPassword = screen.getByLabelText('Senha temporária')
    const newPassword = screen.getByLabelText('Nova senha')
    const confirmation = screen.getByLabelText('Confirmar nova senha')

    fireEvent.click(screen.getByRole('button', { name: 'Mostrar senha temporária' }))
    fireEvent.click(screen.getByRole('button', { name: 'Mostrar nova senha' }))
    fireEvent.click(screen.getByRole('button', { name: 'Mostrar confirmação da nova senha' }))

    expect(temporaryPassword.type).toBe('text')
    expect(newPassword.type).toBe('text')
    expect(confirmation.type).toBe('text')
  })

  it('normalizes the phone value to E.164 for the API', async () => {
    globalThis.fetch.mockResolvedValue({
      ok: true,
      status: 202,
      text: async () => JSON.stringify({ mensagem: 'Código enviado.' }),
    })
    const onCodeSent = vi.fn()
    render(<SignupPage onBack={vi.fn()} onCodeSent={onCodeSent} />)
    fireEvent.change(screen.getByLabelText(/Nome completo/), { target: { value: 'Ana Souza' } })
    fireEvent.change(screen.getByLabelText(/E-mail/), { target: { value: 'ana@example.org' } })
    fireEvent.change(screen.getByLabelText('DDD'), { target: { value: '48' } })
    fireEvent.change(screen.getByLabelText('Número do telefone'), { target: { value: '999991234' } })
    fireEvent.change(screen.getByLabelText(/Organização/), { target: { value: 'UFSC' } })
    fireEvent.change(screen.getByLabelText(/Cargo ou vínculo/), { target: { value: 'Pesquisadora' } })
    fireEvent.change(screen.getByLabelText(/^Senha/), { target: { value: 'Senha123!' } })
    fireEvent.change(screen.getByLabelText(/Confirmar senha/), { target: { value: 'Senha123!' } })
    fireEvent.submit(screen.getByRole('button', { name: /Criar conta/ }).closest('form'))

    await waitFor(() => expect(onCodeSent).toHaveBeenCalledWith('ana@example.org'))
    expect(JSON.parse(globalThis.fetch.mock.calls[0][1].body).telefone).toBe('+5548999991234')
  })

  it('explains invalid phone format before making a request', async () => {
    render(<SignupPage onBack={vi.fn()} onCodeSent={vi.fn()} />)
    fireEvent.change(screen.getByLabelText(/Nome completo/), { target: { value: 'Ana Souza' } })
    fireEvent.change(screen.getByLabelText(/E-mail/), { target: { value: 'ana@example.org' } })
    fireEvent.change(screen.getByLabelText('DDD'), { target: { value: '48' } })
    fireEvent.change(screen.getByLabelText('Número do telefone'), { target: { value: '33334444' } })
    fireEvent.change(screen.getByLabelText(/Organização/), { target: { value: 'UFSC' } })
    fireEvent.change(screen.getByLabelText(/Cargo ou vínculo/), { target: { value: 'Pesquisadora' } })
    fireEvent.change(screen.getByLabelText(/^Senha/), { target: { value: 'Senha123!' } })
    fireEvent.change(screen.getByLabelText(/Confirmar senha/), { target: { value: 'Senha123!' } })
    fireEvent.submit(screen.getByRole('button', { name: /Criar conta/ }).closest('form'))

    expect((await screen.findByRole('alert')).textContent).toContain('Informe DDI, DDD e número válidos.')
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

  it('selects and scrolls to the requested space when opened from its availability link', async () => {
    render(<CalendarPage spaces={spaces} spacesLoaded initialDate="2099-10-15" initialSpaceId="42" onReserve={vi.fn()} />)

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

describe('Title', () => {
  it('renders an accessible heading without decorative emoji', () => {
    render(<Title as="h2">Minhas reservas</Title>)

    const heading = screen.getByRole('heading', { name: 'Minhas reservas', level: 2 })
    expect(heading.querySelector('.title-emoji')).toBeNull()
    expect(heading.textContent).toBe('Minhas reservas')
  })
})

describe('Modal', () => {
  it('renders the title without a decorative emoji', async () => {
    render(<Modal open close={() => {}} title="Meu perfil">Conteúdo</Modal>)

    const heading = await screen.findByRole('heading', { name: 'Meu perfil' })
    expect(heading.querySelector('.dialog-title-emoji')).toBeNull()
    expect(heading.textContent).toBe('Meu perfil')
  })
})
