import { afterEach, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { HelpPage, SignupPage } from './PublicPages.jsx'

describe('registration and contact form validation', () => {
  afterEach(() => {
    cleanup()
    vi.unstubAllGlobals()
  })

  it('filters name input and reports invalid values only after blur or submit', async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      status: 201,
      text: async () => '{}',
    })
    vi.stubGlobal('fetch', fetchMock)
    render(<SignupPage onBack={vi.fn()} onCodeSent={vi.fn()} />)

    const name = screen.getByLabelText(/Nome completo e sobrenome/)
    fireEvent.change(name, { target: { value: '76575675676576576' } })
    expect(name.value).toBe('')
    fireEvent.change(name, { target: { value: 'João 123 Silva' } })
    expect(name.value).toBe('João Silva')
    fireEvent.change(name, { target: { value: 'Maria' } })
    expect(screen.queryByText('Informe nome e sobrenome')).toBeNull()
    fireEvent.blur(name)
    expect(await screen.findByText('Informe nome e sobrenome')).toBeTruthy()
    expect(name.getAttribute('aria-describedby')).toBe('signup-name-error')

    const email = screen.getByLabelText(/E-mail/)
    fireEvent.change(email, { target: { value: '765765' } })
    fireEvent.blur(email)
    expect(await screen.findByText('E-mail inválido')).toBeTruthy()
    expect(fetchMock).not.toHaveBeenCalled()

    fireEvent.change(email, { target: { value: 'PESSOA@example.invalid' } })
    expect(email.value).toBe('pessoa@example.invalid')
    expect(screen.queryByText('E-mail inválido')).toBeNull()
    fireEvent.change(name, { target: { value: 'João 123 Silva' } })
    fireEvent.change(screen.getByLabelText(/Organização/), { target: { value: '3M' } })
    fireEvent.change(screen.getByLabelText(/Cargo ou vínculo/), { target: { value: 'QA' } })
    const passwords = name.closest('form').querySelectorAll('input[autocomplete="new-password"]')
    fireEvent.change(passwords[0], { target: { value: 'SenhaTeste123' } })
    fireEvent.change(passwords[1], { target: { value: 'SenhaTeste123' } })
    fireEvent.submit(name.closest('form'))

    await waitFor(() => expect(fetchMock).toHaveBeenCalledOnce())
    expect(JSON.parse(fetchMock.mock.calls[0][1].body)).toMatchObject({
      nomeCompleto: 'João Silva',
      email: 'pessoa@example.invalid',
      organizacao: '3M',
      cargoVinculo: 'QA',
    })
  })

  it('validates contact email on blur and sends only a valid lower-case address', async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      status: 201,
      text: async () => '{}',
    })
    vi.stubGlobal('fetch', fetchMock)
    render(<HelpPage />)

    const email = screen.getByLabelText('Seu e-mail')
    fireEvent.change(email, { target: { value: 'a@b' } })
    fireEvent.blur(email)
    expect(await screen.findByText('E-mail inválido')).toBeTruthy()
    fireEvent.change(email, { target: { value: 'CONTATO@example.invalid' } })
    fireEvent.change(screen.getByLabelText('Sua mensagem'), { target: { value: 'Mensagem fictícia para teste.' } })
    fireEvent.click(screen.getByRole('button', { name: /Enviar mensagem/ }))

    await waitFor(() => expect(fetchMock).toHaveBeenCalledOnce())
    expect(JSON.parse(fetchMock.mock.calls[0][1].body)).toMatchObject({
      emailContato: 'contato@example.invalid',
      mensagemDuvida: 'Mensagem fictícia para teste.',
    })
  })
})
