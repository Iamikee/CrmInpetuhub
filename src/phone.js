export const phoneCountryCodes = [
  { code: '55', country: 'Brasil', areaCodeLength: 2 },
  { code: '1', country: 'Estados Unidos e Canadá', areaCodeLength: 3 },
  { code: '54', country: 'Argentina', areaCodeLength: 2 },
  { code: '56', country: 'Chile', areaCodeLength: 1 },
  { code: '57', country: 'Colômbia', areaCodeLength: 1 },
  { code: '52', country: 'México', areaCodeLength: 2 },
  { code: '351', country: 'Portugal', areaCodeLength: 2 },
  { code: '44', country: 'Reino Unido', areaCodeLength: 2 },
  { code: '34', country: 'Espanha', areaCodeLength: 2 },
  { code: '33', country: 'França', areaCodeLength: 1 },
  { code: '49', country: 'Alemanha', areaCodeLength: 2 },
  { code: '39', country: 'Itália', areaCodeLength: 2 },
  { code: '61', country: 'Austrália', areaCodeLength: 1 },
  { code: '81', country: 'Japão', areaCodeLength: 2 },
  { code: '91', country: 'Índia', areaCodeLength: 2 },
]

const countryCodesByLength = [...phoneCountryCodes].sort((left, right) => right.code.length - left.code.length)
const onlyDigits = value => String(value || '').replace(/\D/g, '')

export function parsePhoneParts(value) {
  const raw = String(value || '').trim()
  const digits = onlyDigits(raw)
  if (!digits) return { countryCode: '55', areaCode: '', subscriber: '' }

  let countryCode = '55'
  let areaCodeLength = 2
  let nationalNumber = digits
  const hasCountryPrefix = raw.startsWith('+') || (digits.startsWith('55') && digits.length > 11)
  if (hasCountryPrefix) {
    const knownCode = countryCodesByLength.find(({ code }) => digits.startsWith(code))
    if (knownCode) {
      countryCode = knownCode.code
      areaCodeLength = knownCode.areaCodeLength
      nationalNumber = digits.slice(countryCode.length)
    } else if (raw.startsWith('+')) {
      countryCode = digits.slice(0, Math.min(3, digits.length))
      nationalNumber = digits.slice(countryCode.length)
    }
  }

  if (!raw.startsWith('+') && !hasCountryPrefix && nationalNumber.startsWith('0')) {
    nationalNumber = nationalNumber.slice(1)
  }
  return {
    countryCode,
    areaCode: nationalNumber.slice(0, areaCodeLength),
    subscriber: nationalNumber.slice(areaCodeLength, areaCodeLength + 10),
  }
}

export function normalizePhoneToE164(value) {
  const raw = String(value || '').trim()
  const digits = onlyDigits(raw)
  if (!digits) return ''
  if (raw.startsWith('+')) return `+${digits}`
  if (digits.startsWith('55') && digits.length > 11) return `+${digits}`
  const nationalNumber = digits.startsWith('0') ? digits.slice(1) : digits
  return `+55${nationalNumber}`
}

export function hasPhoneInput(value) {
  return onlyDigits(value).length > 0
}

export function isValidPhoneNumber(value) {
  if (!hasPhoneInput(value)) return true
  const normalized = normalizePhoneToE164(value)
  if (!/^\+[1-9]\d{7,14}$/.test(normalized)) return false

  const { countryCode, areaCode, subscriber } = parsePhoneParts(normalized)
  if (!phoneCountryCodes.some(option => option.code === countryCode)) return false
  const areaCodeLength = phoneCountryCodes.find(option => option.code === countryCode)?.areaCodeLength || 2
  if (!new RegExp(`^[1-9]\\d{${areaCodeLength - 1}}$`).test(areaCode) || subscriber.length < 6) return false
  return countryCode !== '55' || [8, 9].includes(subscriber.length)
}

function formatSubscriber(subscriber) {
  if (subscriber.length === 7) return `${subscriber.slice(0, 3)}-${subscriber.slice(3)}`
  if (subscriber.length === 8) return `${subscriber.slice(0, 4)}-${subscriber.slice(4)}`
  if (subscriber.length === 9) return `${subscriber.slice(0, 5)}-${subscriber.slice(5)}`
  if (subscriber.length === 10) return `${subscriber.slice(0, 6)}-${subscriber.slice(6)}`
  return subscriber
}

export function formatPhoneNumber(value) {
  const normalized = normalizePhoneToE164(value)
  if (!normalized) return ''
  const { countryCode, areaCode, subscriber } = parsePhoneParts(normalized)
  if (!areaCode) return `+${countryCode} ${subscriber}`
  return `+${countryCode} (${areaCode}) ${formatSubscriber(subscriber)}`.trim()
}

export function phoneNumberFromParts({ countryCode, areaCode, subscriber }) {
  if (!areaCode && !subscriber) return ''
  return `+${countryCode}${areaCode}${subscriber}`
}

export function formatPhoneSubscriber(value) {
  return formatSubscriber(onlyDigits(value).slice(0, 10))
}
