import { cleanup, fireEvent, render, screen, within } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { useState } from 'react'
import Crm from './CrmPage.jsx'

const users = Array.from({ length: 8 }, (_, index) => ({
  idUsuario: index + 1,
  nomeCompleto: index === 0
    ? 'Ana Responsável com um nome excepcionalmente longo para testar o cartão'
    : `Responsável de teste ${index + 1}`,
  email: `responsavel${index + 1}@example.invalid`,
  telefone: '+5548991234567',
  idPerfil: 4,
  idOrganizacao: index + 1,
  idPersona: index % 2 ? 2 : null,
  ativo: true,
  dataCadastro: '2026-10-25T10:00:00',
}))

const organizations = users.map((person, index) => ({
  idOrganizacao: index + 1,
  nomeOrganizacao: index === 0
    ? 'Organização de demonstração com nome longo para testar quebra de linha'
    : `Organização ${index + 1}`,
  ativo: true,
}))

const reservations = users.map((person, index) => ({
  idReserva: index + 1,
  idSolicitante: person.idUsuario,
  idEspaco: index + 1,
  tituloEvento: index === 0
    ? 'Encontro de demonstração com título longo para validar o cartão em celular'
    : `Evento fictício ${index + 1}`,
  dataReserva: '2026-11-08',
  horarioInicio: '10:00',
  horarioFim: '11:00',
  statusReserva: 'Pendente',
  quantidadeParticipantes: 8,
}))

const teamUser = { idUsuario: 99, nome: 'Equipe fictícia', email: 'equipe@example.invalid', perfil: 1 }

function TestCrm() {
  const [reservationState, setReservationState] = useState([])
  const [spaceState, setSpaceState] = useState([])
  return <Crm
    user={teamUser}
    setUser={vi.fn()}
    onReturnHome={vi.fn()}
    spaces={spaceState}
    setSpaces={setSpaceState}
    reservations={reservationState}
    setReservations={setReservationState}
  />
}

describe('CRM responsive lists', () => {
  afterEach(() => {
    cleanup()
    vi.unstubAllGlobals()
  })

  it('renders fictional registrations and reservations in dedicated card containers', async () => {
    vi.stubGlobal('fetch', vi.fn(async input => {
      const path = new URL(input).pathname.replace('/api/v1', '')
      const data = {
        '/reservas': reservations,
        '/usuarios': users,
        '/organizacoes': organizations,
        '/duvidas': [],
        '/auditoria': [],
      }[path] || []
      return { ok: true, status: 200, text: async () => JSON.stringify(data) }
    }))

    const { container } = render(<TestCrm />)
    fireEventClick(await screen.findByRole('button', { name: 'Cadastros' }))

    const registrationPanel = await screen.findByText('Organização de demonstração com nome longo para testar quebra de linha')
    const registrationCards = container.querySelectorAll('.registration-card')
    expect(registrationPanel).toBeTruthy()
    expect(registrationCards).toHaveLength(8)
    expect(within(registrationCards[0]).getByText(/Ana Responsável com um nome excepcionalmente longo/)).toBeTruthy()
    expect(within(registrationCards[0]).getByText('Aguardando classificação')).toBeTruthy()

    fireEventClick(screen.getByRole('button', { name: /^Reservas/ }))
    const reservationCards = await screen.findAllByText(/Evento fictício|Encontro de demonstração/)
    expect(reservationCards).toHaveLength(8)
    expect(container.querySelectorAll('.crm-reservation')).toHaveLength(8)
  })

  it('opens the mobile navigation as a focus-trapped drawer and restores focus when closed', async () => {
    vi.stubGlobal('matchMedia', vi.fn(query => ({
      matches: query === '(max-width: 767px)',
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
    })))
    vi.stubGlobal('fetch', vi.fn(async input => {
      const path = new URL(input).pathname.replace('/api/v1', '')
      const data = {
        '/reservas': reservations,
        '/usuarios': users,
        '/organizacoes': organizations,
        '/duvidas': [],
        '/auditoria': [],
      }[path] || []
      return { ok: true, status: 200, text: async () => JSON.stringify(data) }
    }))

    const { container } = render(<TestCrm />)
    const menuButton = await screen.findByRole('button', { name: 'Alternar menu lateral' })
    const mobileLogo = container.querySelector('.crm-mobile-logo')
    expect(mobileLogo).toBeTruthy()
    expect(mobileLogo.getAttribute('aria-label')).toBe('Ir para o início do site')
    expect(mobileLogo.getAttribute('href')).toBe('/?view=home')
    expect(within(mobileLogo).getByRole('img', { name: 'InPETU hub' })).toBeTruthy()
    fireEvent.click(menuButton)

    const sidebar = await screen.findByRole('dialog', { name: 'Navegação do painel' })
    const closeButton = within(sidebar).getByRole('button', { name: 'Fechar menu' })
    expect(sidebar.getAttribute('aria-modal')).toBe('true')
    expect(document.activeElement).toBe(closeButton)

    fireEvent.keyDown(window, { key: 'Tab', shiftKey: true })
    expect(document.activeElement).toBe(within(sidebar).getByRole('button', { name: 'Perfil de Equipe fictícia' }))
    fireEvent.keyDown(window, { key: 'Tab' })
    expect(document.activeElement).toBe(closeButton)
    fireEvent.keyDown(window, { key: 'Escape' })

    expect(await screen.findByRole('complementary', { name: 'Navegação do painel' })).toBeTruthy()
    expect(document.querySelector('.crm-layout').classList.contains('crm-mobile-sidebar-open')).toBe(false)
    expect(menuButton.getAttribute('aria-expanded')).toBe('false')
    expect(document.activeElement).toBe(menuButton)

    fireEvent.click(menuButton)
    const reopenedSidebar = await screen.findByRole('dialog', { name: 'Navegação do painel' })
    fireEvent.click(within(reopenedSidebar).getByRole('button', { name: 'Fechar menu' }))
    expect(await screen.findByRole('complementary', { name: 'Navegação do painel' })).toBeTruthy()
    expect(document.activeElement).toBe(menuButton)

    fireEvent.click(menuButton)
    await screen.findByRole('dialog', { name: 'Navegação do painel' })
    fireEvent.click(document.querySelector('.crm-sidebar-backdrop'))
    expect(await screen.findByRole('complementary', { name: 'Navegação do painel' })).toBeTruthy()
    expect(document.activeElement).toBe(menuButton)
  })
})

function fireEventClick(element) {
  element.click()
}
