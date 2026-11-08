import { describe, expect, it } from 'vitest'
import { getSuccessfulLoginDestination, shouldRedirectCrmUserToPanel } from './App.jsx'

describe('authenticated CRM navigation', () => {
  const coordinator = { idUsuario: 1, perfil: 1 }

  it('does not send an authenticated CRM user back to the panel from the public home page', () => {
    expect(shouldRedirectCrmUserToPanel(coordinator, '/', '?view=home')).toBe(false)
  })

  it('keeps direct panel navigation on the panel route', () => {
    expect(shouldRedirectCrmUserToPanel(coordinator, '/paineladm', '')).toBe(false)
  })

  it('does not redirect a visitor without a session away from the panel login route', () => {
    expect(shouldRedirectCrmUserToPanel(null, '/paineladm', '')).toBe(false)
  })

  it('still redirects an authenticated CRM user from the default root route to the panel', () => {
    expect(shouldRedirectCrmUserToPanel(coordinator, '/', '')).toBe(true)
  })

  it('still sends a successfully authenticated CRM user to the panel', () => {
    expect(getSuccessfulLoginDestination(coordinator)).toBe('/paineladm')
    expect(getSuccessfulLoginDestination({ idUsuario: 2, perfil: 4 })).toBe(null)
  })
})
