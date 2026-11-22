const permittedNameCharacters = /^[\p{L}\p{M} '.-]*$/u
const invalidNameCharacters = /[^\p{L}\p{M} '.-]/gu
const permittedBusinessCharacters = /^[\p{L}\p{M}\p{N} .,;:'’&()/_+:#-]+$/u
const nameConnectors = new Set(['de', 'da', 'do', 'dos', 'das', 'e'])

export function sanitizeNameInput(value) {
  return String(value ?? '').replace(invalidNameCharacters, '').replace(/ {2,}/g, ' ')
}

export function normalizeFullName(value) {
  return sanitizeNameInput(value).trim()
}

export function normalizeEmail(value) {
  return String(value ?? '').toLowerCase()
}

export function validateFullName(value) {
  const rawName = String(value ?? '')
  if (!rawName.trim()) return 'Informe nome e sobrenome'
  if (!permittedNameCharacters.test(rawName)) return 'Use apenas letras no nome'
  const name = normalizeFullName(rawName)
  if (name.length < 3 || name.length > 100) return 'O nome deve ter de 3 a 100 caracteres'
  const words = name.split(' ')
  const principalWords = words.filter(word => !nameConnectors.has(word.toLocaleLowerCase('pt-BR')))
  if (principalWords.length < 2 || words.some(word => !nameConnectors.has(word.toLocaleLowerCase('pt-BR')) && [...word].filter(letter => /\p{L}/u.test(letter)).length < 2)) {
    return 'Informe nome e sobrenome'
  }
  return ''
}

export function validateEmail(value) {
  const email = String(value ?? '')
  if (email.length > 100) return 'O e-mail deve ter no máximo 100 caracteres'
  const localPart = "[A-Z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[A-Z0-9!#$%&'*+/=?^_`{|}~-]+)*"
  const domainLabel = '[A-Z0-9](?:[A-Z0-9-]{0,61}[A-Z0-9])?'
  const pattern = new RegExp(`^${localPart}@(?:${domainLabel}\\.)+[A-Z]{2,}$`, 'i')
  return pattern.test(email) ? '' : 'E-mail inválido'
}

export function sanitizeCpfCnpj(value) {
  return String(value ?? '').replace(/\D/g, '')
}

export function maskCpfCnpj(value) {
  const clean = sanitizeCpfCnpj(value)
  if (clean.length === 11) return `***.***.${clean.slice(6, 9)}-${clean.slice(9)}`
  if (clean.length === 14) return `**.${clean.slice(2, 5)}.${clean.slice(5, 8)}/${clean.slice(8, 12)}-**`
  return clean ? '***.***.***-**' : ''
}

export function validateCpf(value) {
  const cpf = sanitizeCpfCnpj(value)
  if (!/^\d{11}$/.test(cpf) || /^(\d)\1{10}$/.test(cpf)) return 'CPF inválido'
  const digit = (digits, length) => {
    let sum = 0
    for (let i = 0; i < length; i++) sum += Number(digits[i]) * (length + 1 - i)
    const rest = (sum * 10) % 11
    return rest === 10 ? 0 : rest
  }
  return digit(cpf, 9) === Number(cpf[9]) && digit(cpf, 10) === Number(cpf[10]) ? '' : 'CPF inválido'
}

export function validateCnpj(value) {
  const cnpj = sanitizeCpfCnpj(value)
  if (!/^\d{14}$/.test(cnpj) || /^(\d)\1{13}$/.test(cnpj)) return 'CNPJ inválido'
  const digit = (input, length) => {
    let sum = 0
    let weight = length === 12 ? 5 : 6
    for (let i = 0; i < length; i++) {
      sum += Number(input[i]) * weight
      weight = weight === 2 ? 9 : weight - 1
    }
    const rest = sum % 11
    return rest < 2 ? 0 : 11 - rest
  }
  return digit(cnpj, 12) === Number(cnpj[12]) && digit(cnpj, 13) === Number(cnpj[13]) ? '' : 'CNPJ inválido'
}

export function validateOrganization(value) {
  const organization = String(value ?? '').trim()
  if (!organization) return 'Informe a organização'
  if (organization.length < 2 || organization.length > 100 || !permittedBusinessCharacters.test(organization) || !/\p{L}/u.test(organization)) {
    return 'Informe uma organização válida'
  }
  return ''
}

export function validateRole(value) {
  const role = String(value ?? '').trim()
  if (!role) return 'Informe o cargo ou vínculo'
  if (role.length < 2 || role.length > 80 || !permittedBusinessCharacters.test(role) || [...role].filter(letter => /\p{L}/u.test(letter)).length < 2) {
    return 'Informe um cargo ou vínculo válido'
  }
  return ''
}

export function validateIdentityFields(fields) {
  const errors = {}
  if (Object.hasOwn(fields, 'nomeCompleto')) errors.nomeCompleto = validateFullName(fields.nomeCompleto)
  if (Object.hasOwn(fields, 'email')) errors.email = validateEmail(fields.email)
  if (Object.hasOwn(fields, 'emailContato')) errors.emailContato = validateEmail(fields.emailContato)
  if (Object.hasOwn(fields, 'organizacao')) errors.organizacao = validateOrganization(fields.organizacao)
  if (Object.hasOwn(fields, 'cargoVinculo')) errors.cargoVinculo = validateRole(fields.cargoVinculo)
  return Object.fromEntries(Object.entries(errors).filter(([, message]) => message))
}
