export const phoneCountryCodes = [
  { code: '55', country: 'Brasil', areaCodeLength: 2 },
]

// DDDs assigned to Brazil under the national numbering plan.
export const brazilianAreaCodes = new Set([
  '11', '12', '13', '14', '15', '16', '17', '18', '19',
  '21', '22', '24', '27', '28',
  '31', '32', '33', '34', '35', '37', '38',
  '41', '42', '43', '44', '45', '46', '47', '48', '49',
  '51', '53', '54', '55',
  '61', '62', '63', '64', '65', '66', '67', '68', '69',
  '71', '73', '74', '75', '77', '79',
  '81', '82', '83', '84', '85', '86', '87', '88', '89',
  '91', '92', '93', '94', '95', '96', '97', '98', '99',
])

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
  const withoutPrefixZero = digits.startsWith('0') ? digits.slice(1) : ''
  const nationalNumber = withoutPrefixZero.length >= 11
    && brazilianAreaCodes.has(withoutPrefixZero.slice(0, 2))
    ? withoutPrefixZero
    : digits
  return `+55${nationalNumber}`
}

export function hasPhoneInput(value) {
  return String(value || '').trim().length > 0
}

export function isValidPhoneNumber(value) {
  if (!hasPhoneInput(value)) return true
  const normalized = normalizePhoneToE164(value)
  if (!/^\+55\d{11}$/.test(normalized)) return false

  const { countryCode, areaCode, subscriber } = parsePhoneParts(normalized)
  return countryCode === '55'
    && brazilianAreaCodes.has(areaCode)
    && subscriber.length === 9
    && subscriber.startsWith('9')
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
