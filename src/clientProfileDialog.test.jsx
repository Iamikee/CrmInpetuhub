import { cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { ClientProfileDialog, ProfileDialog } from './App.jsx'

describe('client profile dialog', () => {
  afterEach(() => {
    cleanup()
    vi.restoreAllMocks()
  })

  it('keeps profile actions in the footer and deactivation separate', async () => {
    render(<ClientProfileDialog
      client={{
        idUsuario: 12,
        idPerfil: 4,
        nomeCompleto: 'Pessoa de teste',
        email: 'pessoa@example.invalid',
        telefone: '+5548991234567',
        organizacao: 'Organização teste',
        cargoVinculo: 'Responsável',
        ativo: true,
      }}
      reservations={[]}
      tickets={[]}
      auditItems={[]}
      close={vi.fn()}
      onSave={vi.fn()}
      onPersona={vi.fn()}
      onDeactivate={vi.fn()}
      canDeactivate
      canResetPassword
      onResetPassword={vi.fn()}
    />)

    const dialog = await screen.findByRole('dialog', { name: 'Pessoa de teste' })
    const fields = dialog.querySelector('.client-profile-grid')
    const footer = dialog.querySelector('.client-profile-edit-form .client-profile-actions')

    expect(fields.classList.contains('client-profile-grid')).toBe(true)
    expect(within(footer).getByRole('button', { name: 'Editar dados' })).toBeTruthy()
    expect(within(footer).getByRole('button', { name: 'Redefinir senha' })).toBeTruthy()
    expect(dialog.querySelector('.client-profile-deactivation > .client-profile-action-danger').textContent).toContain('Desativar cadastro')
  })

  it('keeps the normal footer after password reset and places the temporary password above it', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    const onResetPassword = vi.fn().mockResolvedValue({ senhaTemporaria: 'Teste#1234' })

    render(<ClientProfileDialog
      client={{
        idUsuario: 12,
        idPerfil: 4,
        nomeCompleto: 'Pessoa de teste',
        email: 'pessoa@example.invalid',
        telefone: '+5548991234567',
        organizacao: 'Organização teste',
        cargoVinculo: 'Responsável',
        ativo: true,
      }}
      reservations={[]}
      tickets={[]}
      auditItems={[]}
      close={vi.fn()}
      onSave={vi.fn()}
      onPersona={vi.fn()}
      onDeactivate={vi.fn()}
      canDeactivate
      canResetPassword
      onResetPassword={onResetPassword}
    />)

    const dialog = await screen.findByRole('dialog', { name: 'Pessoa de teste' })
    fireEvent.click(within(dialog).getByRole('button', { name: 'Redefinir senha' }))
    fireEvent.click(within(dialog).getByRole('button', { name: 'Confirmar redefinição' }))

    const result = await within(dialog).findByText('Senha temporária gerada')
    const footer = dialog.querySelector('.client-profile-edit-form .client-profile-actions')
    const editButton = within(footer).getByRole('button', { name: 'Editar dados' })
    const resetButton = within(footer).getByRole('button', { name: 'Redefinir senha' })
    const destructiveAction = dialog.querySelector('.client-profile-deactivation')

    expect(onResetPassword).toHaveBeenCalledOnce()
    expect(within(dialog).getByText('Teste#1234')).toBeTruthy()
    expect(result.compareDocumentPosition(footer) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy()
    expect(editButton).toBeTruthy()
    expect(resetButton).toBeTruthy()
    expect(destructiveAction.compareDocumentPosition(footer) & Node.DOCUMENT_POSITION_PRECEDING).toBeTruthy()
  })

  it('switches all four profile panels while keeping the selected tab focused', async () => {
    render(<ClientProfileDialog
      client={{
        idUsuario: 12,
        nomeCompleto: 'Pessoa de teste',
        email: 'pessoa@example.invalid',
        telefone: '+5548991234567',
        organizacao: 'Organização teste',
        cargoVinculo: 'Responsável',
        ativo: true,
      }}
      reservations={[{
        idReserva: 71,
        idSolicitante: 12,
        tituloEvento: 'Reserva de teste',
        dataReserva: '2026-11-20',
        horarioInicio: '10:00',
        horarioFim: '11:00',
        statusReserva: 'Pendente',
      }]}
      tickets={[]}
      auditItems={[{
        time: '20/10/2026, 10:00:00',
        user: 'Equipe de teste',
        action: 'Alteração de cadastro',
        target: 'Pessoa de teste · cadastro #12',
        detail: 'Registro de teste',
      }]}
      close={vi.fn()}
      onSave={vi.fn()}
      onPersona={vi.fn()}
      onDeactivate={vi.fn()}
      canDeactivate
      canResetPassword
      onResetPassword={vi.fn()}
    />)

    const dialog = await screen.findByRole('dialog', { name: 'Pessoa de teste' })
    const tabs = within(dialog).getAllByRole('tab')
    const panel = within(dialog).getByRole('tabpanel')

    expect(tabs).toHaveLength(4)
    expect(tabs.map(tab => tab.textContent.trim())).toEqual([
      'Dados gerais',
      'Reservas (1)',
      'Dúvidas (0)',
      'Auditoria',
    ])

    for (const index of [1, 2, 3, 0]) {
      const tab = tabs[index]
      const previousContent = panel.textContent

      tab.focus()
      fireEvent.click(tab)

      expect(tab.getAttribute('aria-selected')).toBe('true')
      expect(tab.getAttribute('aria-controls')).toBe(panel.id)
      expect(panel.getAttribute('aria-labelledby')).toBe(tab.id)
      expect(document.activeElement).toBe(tab)
      expect(panel.textContent).not.toBe(previousContent)
    }
  })

  it('allows a coordinator to change a user profile from the profile dialog', async () => {
    const client = {
      idUsuario: 12,
      idPerfil: 3,
      nomeCompleto: 'Pessoa de teste',
      email: 'pessoa@example.invalid',
      ativo: true,
    }
    const onRole = vi.fn().mockResolvedValue({ ...client, idPerfil: 4 })

    render(<ClientProfileDialog
      client={client}
      reservations={[]}
      tickets={[]}
      auditItems={[]}
      close={vi.fn()}
      onSave={vi.fn()}
      onPersona={vi.fn()}
      onRole={onRole}
      canManageRole
      onDeactivate={vi.fn()}
    />)

    const dialog = await screen.findByRole('dialog', { name: 'Pessoa de teste' })
    fireEvent.change(within(dialog).getByRole('combobox', { name: 'Perfil de acesso na ficha' }), { target: { value: '4' } })
    fireEvent.click(within(dialog).getByRole('button', { name: 'Salvar perfil' }))

    expect(onRole).toHaveBeenCalledWith(client, 4)
    expect(await within(dialog).findByText('Perfil de acesso atualizado. A pessoa deverá entrar novamente.')).toBeTruthy()
    expect(within(dialog).getByText('Solicitante')).toBeTruthy()
  })

  it('lets the team deactivate an active profile 3 account after confirming its identity', async () => {
    const client = {
      idUsuario: 12,
      idPerfil: 3,
      nomeCompleto: 'Pessoa de teste',
      email: 'pessoa@example.invalid',
      ativo: true,
    }
    const onStatus = vi.fn().mockResolvedValue({ ...client, ativo: false })
    vi.spyOn(window, 'confirm').mockReturnValue(true)

    render(<ClientProfileDialog
      client={client}
      reservations={[]}
      tickets={[]}
      auditItems={[]}
      close={vi.fn()}
      onSave={vi.fn()}
      onPersona={vi.fn()}
      canManageStatus
      currentUserId={1}
      onStatus={onStatus}
    />)

    const dialog = await screen.findByRole('dialog', { name: 'Pessoa de teste' })
    fireEvent.click(within(dialog).getByRole('button', { name: 'Inativar acesso' }))

    expect(window.confirm).toHaveBeenCalledWith(
      'Inativar o acesso de Pessoa de teste (pessoa@example.invalid)?',
    )
    expect(onStatus).toHaveBeenCalledWith(client, false)
    expect(await within(dialog).findByText('Cadastro inativo')).toBeTruthy()
    expect(within(dialog).getByRole('button', { name: 'Reativar acesso' })).toBeTruthy()
  })

  it('shows reactivation for an inactive profile 3 account', async () => {
    const client = {
      idUsuario: 12,
      idPerfil: 3,
      nomeCompleto: 'Pessoa de teste',
      email: 'pessoa@example.invalid',
      ativo: false,
    }
    const onStatus = vi.fn().mockResolvedValue({ ...client, ativo: true })

    render(<ClientProfileDialog
      client={client}
      reservations={[]}
      tickets={[]}
      auditItems={[]}
      close={vi.fn()}
      onSave={vi.fn()}
      onPersona={vi.fn()}
      canManageStatus
      currentUserId={1}
      onStatus={onStatus}
    />)

    const dialog = await screen.findByRole('dialog', { name: 'Pessoa de teste' })
    fireEvent.click(within(dialog).getByRole('button', { name: 'Reativar acesso' }))

    expect(onStatus).toHaveBeenCalledWith(client, true)
    expect(await within(dialog).findByText('Cadastro ativo')).toBeTruthy()
    expect(within(dialog).getByRole('button', { name: 'Inativar acesso' })).toBeTruthy()
  })

  it.each([
    ['the current account', { idUsuario: 1, idPerfil: 3 }, 1],
    ['a profile 4 account', { idUsuario: 12, idPerfil: 4 }, 1],
    ['a profile 5 account', { idUsuario: 12, idPerfil: 5 }, 1],
  ])('does not show access status controls for %s', async (_description, identity, currentUserId) => {
    render(<ClientProfileDialog
      client={{ ...identity, nomeCompleto: 'Pessoa de teste', email: 'pessoa@example.invalid', ativo: true }}
      reservations={[]}
      tickets={[]}
      auditItems={[]}
      close={vi.fn()}
      onSave={vi.fn()}
      onPersona={vi.fn()}
      canManageStatus
      currentUserId={currentUserId}
      onStatus={vi.fn()}
    />)

    const dialog = await screen.findByRole('dialog', { name: 'Pessoa de teste' })
    expect(within(dialog).queryByRole('button', { name: 'Inativar acesso' })).toBeNull()
    expect(within(dialog).queryByRole('button', { name: 'Reativar acesso' })).toBeNull()
  })

  it('blocks invalid client edits and normalizes name and email before saving', async () => {
    const client = {
      idUsuario: 12,
      nomeCompleto: 'Pessoa de teste',
      email: 'pessoa@example.invalid',
      telefone: '',
      organizacao: 'InPETU',
      cargoVinculo: 'Pesquisadora',
      ativo: true,
    }
    const onSave = vi.fn().mockResolvedValue({})

    render(<ClientProfileDialog
      client={client}
      reservations={[]}
      tickets={[]}
      auditItems={[]}
      close={vi.fn()}
      onSave={onSave}
      onPersona={vi.fn()}
      onDeactivate={vi.fn()}
    />)

    const dialog = await screen.findByRole('dialog', { name: 'Pessoa de teste' })
    fireEvent.click(within(dialog).getByRole('button', { name: 'Editar dados' }))
    const name = within(dialog).getByLabelText('Nome completo e sobrenome')
    const email = within(dialog).getByLabelText('E-mail')
    fireEvent.change(name, { target: { value: 'João 123 Silva' } })
    fireEvent.change(email, { target: { value: '765765' } })
    fireEvent.blur(email)
    expect(await within(dialog).findByText('E-mail inválido')).toBeTruthy()
    fireEvent.click(within(dialog).getByRole('button', { name: 'Salvar alterações' }))
    expect(onSave).not.toHaveBeenCalled()

    fireEvent.change(email, { target: { value: 'PESSOA@example.invalid' } })
    fireEvent.click(within(dialog).getByRole('button', { name: 'Salvar alterações' }))
    await waitFor(() => expect(onSave).toHaveBeenCalledOnce())
    expect(onSave.mock.calls[0][1]).toMatchObject({
      nomeCompleto: 'João Silva',
      email: 'pessoa@example.invalid',
    })
  })

  it('shows profile errors after blur and prevents the invalid profile request', async () => {
    const fetchMock = vi.fn(async () => ({
      ok: true,
      status: 200,
      text: async () => JSON.stringify({
        idUsuario: 22,
        nomeCompleto: 'Pessoa de teste',
        email: 'pessoa@example.invalid',
        telefone: '',
        organizacao: 'InPETU',
        cargoVinculo: 'Pesquisadora',
        perfil: 4,
      }),
    }))
    vi.stubGlobal('fetch', fetchMock)
    render(<ProfileDialog
      open
      close={vi.fn()}
      user={{ idUsuario: 22, nome: 'Pessoa de teste', perfil: 4 }}
      onUserUpdate={vi.fn()}
    />)

    const dialog = await screen.findByRole('dialog', { name: 'Meu perfil' })
    const name = await within(dialog).findByLabelText('Nome')
    fireEvent.change(name, { target: { value: 'Maria' } })
    expect(within(dialog).queryByText('Informe nome e sobrenome')).toBeNull()
    fireEvent.blur(name)
    expect(await within(dialog).findByText('Informe nome e sobrenome')).toBeTruthy()
    fireEvent.click(within(dialog).getByRole('button', { name: 'Salvar perfil' }))
    expect(fetchMock).toHaveBeenCalledOnce()
  })
})
