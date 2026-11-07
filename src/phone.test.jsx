import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { PhoneInput } from './App.jsx'
import { formatPhoneNumber, isValidPhoneNumber, normalizePhoneToE164 } from './phone.js'

describe('phone input', () => {
  afterEach(cleanup)

  it('keeps new numbers in E.164 and formats existing Brazilian values', () => {
    expect(normalizePhoneToE164('(48) 99999-1234')).toBe('+5548999991234')
    expect(formatPhoneNumber('+5548999991234')).toBe('+55 (48) 99999-1234')
    expect(isValidPhoneNumber('+5548999991234')).toBe(true)
    expect(isValidPhoneNumber('+554899123')).toBe(false)
    expect(isValidPhoneNumber('+999123456789')).toBe(false)
  })

  it('collects country code, area code and subscriber number separately', () => {
    const onChange = vi.fn()
    render(<PhoneInput value="" onChange={onChange} />)

    fireEvent.change(screen.getByRole('combobox', { name: 'Código do país (DDI)' }), { target: { value: '1' } })
    fireEvent.change(screen.getByRole('textbox', { name: 'DDD' }), { target: { value: '212' } })
    fireEvent.change(screen.getByRole('textbox', { name: 'Número do telefone' }), { target: { value: '5550100' } })

    expect(onChange).toHaveBeenLastCalledWith('+12125550100')
    expect(screen.getByRole('textbox', { name: 'DDD' }).value).toBe('212')
    expect(screen.getByRole('textbox', { name: 'Número do telefone' }).value).toBe('555-0100')
  })
})
