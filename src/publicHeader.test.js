import { readFileSync } from 'node:fs'
import { describe, expect, it } from 'vitest'

describe('public header navigation', () => {
  it('keeps calendar entry points while removing the header scheduling button', () => {
    const source = readFileSync('src/App.jsx', 'utf8')
    const header = source.slice(source.indexOf('function Header('), source.indexOf('export function SpaceCardAction'))

    expect(header).not.toContain('reference-header-cta')
    expect(header).not.toContain('Agendar Espaço')
    expect(header).not.toContain('header-cta-short')
    expect(header).toContain('>Nossos Espaços</button>')
    expect(source).toContain('className="all-spaces-link icon-text" onClick={onOpenCalendar}')
    expect(source).toContain('<Button icon={CalendarDays} onClick={onOpenCalendar}>Consultar horários</Button>')
  })
})
