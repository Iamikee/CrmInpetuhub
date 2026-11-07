import { cleanup, fireEvent, render, screen, within } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { ClientProfileDialog } from './App.jsx'

describe('client profile dialog', () => {
  afterEach(() => {
    cleanup()
    vi.restoreAllMocks()
  })

  it('keeps profile actions in the footer and deactivation separate', async () => {
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
})
