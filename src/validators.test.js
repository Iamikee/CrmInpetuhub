import { describe, expect, it } from 'vitest'
import {
  normalizeEmail,
  normalizeFullName,
  sanitizeNameInput,
  validateEmail,
  validateFullName,
  validateIdentityFields,
  validateOrganization,
  validateRole,
} from './validators.js'

describe('shared field validation', () => {
  it('filters invalid full-name characters and normalizes whitespace', () => {
    expect(sanitizeNameInput('76575675676576576')).toBe('')
    expect(sanitizeNameInput('João 123 Silva')).toBe('João Silva')
    expect(normalizeFullName('  João   da Silva  ')).toBe('João da Silva')
  })

  it('requires two name words with at least two letters except connectors', () => {
    expect(validateFullName('Maria')).toBe('Informe nome e sobrenome')
    expect(validateFullName('João da Silva')).toBe('')
    expect(validateFullName(sanitizeNameInput('João 123 Silva'))).toBe('')
    expect(validateFullName('João 😀 Silva')).toBe('Use apenas letras no nome')
  })

  it('validates and lowercases email addresses', () => {
    expect(validateEmail('765765')).toBe('E-mail inválido')
    expect(validateEmail('a@b')).toBe('E-mail inválido')
    expect(validateEmail('Pessoa@Example.org')).toBe('')
    expect(normalizeEmail('Pessoa@Example.org')).toBe('pessoa@example.org')
    expect(validateEmail(`${'a'.repeat(85)}@example.invalid`)).toBe('O e-mail deve ter no máximo 100 caracteres')
  })

  it('validates organization and role without rejecting the allowed 3M example', () => {
    expect(validateOrganization('3M')).toBe('')
    expect(validateOrganization('Empresa 123')).toBe('')
    expect(validateOrganization('765765')).toBe('Informe uma organização válida')
    expect(validateOrganization('!!')).toBe('Informe uma organização válida')
    expect(validateOrganization('A'.repeat(101))).toBe('Informe uma organização válida')
    expect(validateRole('Pesquisadora')).toBe('')
    expect(validateRole('765765')).toBe('Informe um cargo ou vínculo válido')
  })

  it('returns errors only for the fields present in a form payload', () => {
    expect(validateIdentityFields({
      nomeCompleto: 'Maria',
      email: 'a@b',
      organizacao: '765765',
      cargoVinculo: '99',
    })).toEqual({
      nomeCompleto: 'Informe nome e sobrenome',
      email: 'E-mail inválido',
      organizacao: 'Informe uma organização válida',
      cargoVinculo: 'Informe um cargo ou vínculo válido',
    })
  })
})
