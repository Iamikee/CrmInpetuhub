import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { PhoneInput } from './App.jsx'
import { brazilianAreaCodes, formatPhoneNumber, isValidPhoneNumber, normalizePhoneToE164 } from './phone.js'

describe('phone input', () => {
  afterEach(cleanup)

  it.each([
    ['Brazilian mobile with mask', '(48) 98426-0543', true, '+5548984260543'],
    ['Brazilian mobile with national digits', '48984260543', true, '+5548984260543'],
    ['Brazilian mobile in E.164', '+5548984260543', true, '+5548984260543'],
    ['Brazilian mobile with carrier prefix zero', '048 98426-0543', true, '+5548984260543'],
    ['Brazilian mobile with separated prefix zero', '0 48 98426-0543', true, '+5548984260543'],
    ['Brazilian mobile with masked prefix zero', '(048) 98426-0543', true, '+5548984260543'],
    ['empty optional value', '', true, ''],
    ['blank optional value', '  ', true, ''],
    ['DDD 00', '+5500984260543', false, '+5500984260543'],
    ['nonexistent DDD 20', '+5520984260543', false, '+5520984260543'],
    ['prefix zero without DDD', '0984260543', false, '+550984260543'],
    ['prefix zero before nonexistent DDD', '020984260543', false, '+55020984260543'],
    ['fixed line with 8 digits', '+554833335555', false, '+554833335555'],
    ['text without digits', 'telefone', false, ''],
    ['international country code', '+12125550100', false, '+12125550100'],
  ])('validates %s', (_, input, valid, normalized) => {
    expect(isValidPhoneNumber(input)).toBe(valid)
    expect(normalizePhoneToE164(input)).toBe(normalized)
  })

  it('uses the 67 official Brazilian area codes', () => {
    expect(brazilianAreaCodes.size).toBe(67)
    for (const areaCode of brazilianAreaCodes) {
      expect(isValidPhoneNumber(`+55${areaCode}984260543`)).toBe(true)
    }
  })

  it('collects the Brazilian area code and mobile subscriber separately', () => {
    const onChange = vi.fn()
    render(<PhoneInput value="" onChange={onChange} />)

    fireEvent.change(screen.getByRole('textbox', { name: 'DDD' }), { target: { value: '48' } })
    fireEvent.change(screen.getByRole('textbox', { name: 'Número do telefone' }), { target: { value: '984260543' } })

    expect(onChange).toHaveBeenLastCalledWith('+5548984260543')
    expect(screen.getByRole('textbox', { name: 'DDD' }).value).toBe('48')
    expect(screen.getByRole('textbox', { name: 'Número do telefone' }).value).toBe('98426-0543')
  })

  it.each([
    ['formatted Brazilian mobile', '(48) 98426-0543'],
    ['Brazilian national digits', '48984260543'],
    ['E.164 Brazilian mobile', '+5548984260543'],
  ])('accepts a complete pasted %s and emits E.164', (_, input) => {
    const onChange = vi.fn()
    render(<PhoneInput value="" onChange={onChange} />)

    fireEvent.change(screen.getByRole('textbox', { name: 'Número do telefone' }), { target: { value: input } })

    expect(screen.getByRole('textbox', { name: 'DDD' }).value).toBe('48')
    expect(screen.getByRole('textbox', { name: 'Número do telefone' }).value).toBe('98426-0543')
    expect(onChange).toHaveBeenLastCalledWith('+5548984260543')
    expect(screen.getByRole('textbox', { name: 'Número do telefone' }).getAttribute('aria-invalid')).toBe('false')
  })

  it('distributes a pasted formatted number and rejects fixed lines', () => {
    const onChange = vi.fn()
    render(<PhoneInput value="" onChange={onChange} />)
    const number = screen.getByRole('textbox', { name: 'Número do telefone' })
    fireEvent.paste(number, {
      clipboardData: { getData: () => '(48) 98426-0543' },
    })

    expect(screen.getByRole('textbox', { name: 'DDD' }).value).toBe('48')
    expect(number.value).toBe('98426-0543')
    expect(onChange).toHaveBeenLastCalledWith('+5548984260543')

    fireEvent.change(screen.getByRole('textbox', { name: 'DDD' }), { target: { value: '48' } })
    fireEvent.change(number, { target: { value: '33335555' } })
    expect(number.value).toBe('3333-5555')
    expect(number.getAttribute('aria-invalid')).toBe('true')
    expect(screen.getByText('Informe DDI, DDD e número válidos.')).toBeTruthy()
    expect(onChange).toHaveBeenLastCalledWith('+554833335555')
  })

  it.each(['00', '20'])('rejects invalid area code %s', areaCode => {
    render(<PhoneInput value="" onChange={() => {}} />)
    fireEvent.change(screen.getByRole('textbox', { name: 'DDD' }), { target: { value: areaCode } })
    fireEvent.change(screen.getByRole('textbox', { name: 'Número do telefone' }), { target: { value: '984260543' } })

    expect(screen.getByRole('textbox', { name: 'Número do telefone' }).getAttribute('aria-invalid')).toBe('true')
    expect(screen.getByText('Informe DDI, DDD e número válidos.')).toBeTruthy()
  })

  it('rejects text instead of treating it as an empty optional phone', () => {
    const onChange = vi.fn()
    render(<PhoneInput value="" onChange={onChange} />)
    const number = screen.getByRole('textbox', { name: 'Número do telefone' })
    fireEvent.change(number, { target: { value: 'telefone' } })

    expect(onChange).toHaveBeenLastCalledWith('telefone')
    expect(number.getAttribute('aria-invalid')).toBe('true')
    expect(screen.getByText('Informe DDI, DDD e número válidos.')).toBeTruthy()
  })
})
