import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { PhoneInput } from './App.jsx'
import { HelpPage } from './PublicPages.jsx'
import { brazilianAreaCodes, formatPhoneNumber, isValidPhoneNumber, normalizePhoneToE164 } from './phone.js'

describe('phone input', () => {
  afterEach(() => {
    cleanup()
    vi.unstubAllGlobals()
  })
  const formerGenericMessage = ['Informe DDI, DDD', 'e número válidos.'].join(' ')

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

    const areaCode = screen.getByRole('textbox', { name: 'DDD' })
    const number = screen.getByRole('textbox', { name: 'Número do telefone' })
    areaCode.focus()
    fireEvent.change(areaCode, { target: { value: '4' } })
    expect(document.activeElement).toBe(areaCode)
    fireEvent.change(areaCode, { target: { value: '48' } })

    expect(document.activeElement).toBe(number)
    fireEvent.change(screen.getByRole('textbox', { name: 'Número do telefone' }), { target: { value: '984260543' } })

    expect(onChange).toHaveBeenLastCalledWith('+5548984260543')
    expect(areaCode.value).toBe('48')
    expect(number.value).toBe('98426-0543')
  })

  it('ignores digits after the ninth subscriber digit without changing the area code', () => {
    render(<PhoneInput value="" onChange={() => {}} />)
    const areaCode = screen.getByRole('textbox', { name: 'DDD' })
    const number = screen.getByRole('textbox', { name: 'Número do telefone' })
    fireEvent.change(areaCode, { target: { value: '91' } })

    for (const digit of '983029809123456') {
      fireEvent.change(number, { target: { value: `${number.value}${digit}` } })
    }

    expect(areaCode.value).toBe('91')
    expect(number.value.replace(/\D/g, '')).toHaveLength(9)
    expect(number.value).toBe('98302-9809')
  })

  it('distributes a complete phone pasted in the area code field', () => {
    const onChange = vi.fn()
    render(<PhoneInput value="" onChange={onChange} />)
    const areaCode = screen.getByRole('textbox', { name: 'DDD' })
    const number = screen.getByRole('textbox', { name: 'Número do telefone' })

    fireEvent.paste(areaCode, {
      clipboardData: { getData: () => '(48) 98426-0543' },
    })

    expect(areaCode.value).toBe('48')
    expect(number.value).toBe('98426-0543')
    expect(document.activeElement).toBe(number)
    expect(onChange).toHaveBeenLastCalledWith('+5548984260543')
  })

  it('moves focus back to the area code when Backspace is pressed on an empty number', () => {
    render(<PhoneInput value="" onChange={() => {}} />)
    const areaCode = screen.getByRole('textbox', { name: 'DDD' })
    const number = screen.getByRole('textbox', { name: 'Número do telefone' })
    fireEvent.change(areaCode, { target: { value: '48' } })
    number.focus()

    fireEvent.keyDown(number, { key: 'Backspace' })

    expect(document.activeElement).toBe(areaCode)
  })

  it('keeps focus in the area code when it is edited after the number is filled', () => {
    render(<PhoneInput value="+5548984260543" onChange={() => {}} />)
    const areaCode = screen.getByRole('textbox', { name: 'DDD' })

    areaCode.focus()
    fireEvent.change(areaCode, { target: { value: '47' } })

    expect(document.activeElement).toBe(areaCode)
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

  it.each([
    ['national digits', '91283029809', '28302-9809'],
    ['parentheses and hyphen', '(91) 98302-9809', '98302-9809'],
    ['E.164 with formatting', '+55 91 98302-9809', '98302-9809'],
    ['country-code digits', '5591983029809', '98302-9809'],
  ])('distributes pasted %s into either phone field', (_, pasted, expectedSubscriber) => {
    for (const targetName of ['DDD', 'Número do telefone']) {
      const onChange = vi.fn()
      const { unmount } = render(<PhoneInput value="" onChange={onChange} />)
      const target = screen.getByRole('textbox', { name: targetName })
      fireEvent.paste(target, { clipboardData: { getData: () => pasted } })

      expect(screen.getByRole('combobox', { name: 'Código do país (DDI)' }).value).toBe('55')
      expect(screen.getByRole('textbox', { name: 'DDD' }).value).toBe('91')
      expect(screen.getByRole('textbox', { name: 'Número do telefone' }).value).toBe(expectedSubscriber)
      expect(onChange).toHaveBeenLastCalledWith(`+5591${expectedSubscriber.replace(/\D/g, '')}`)
      unmount()
    }
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
    expect(number.getAttribute('aria-invalid')).toBe('false')
    expect(screen.queryByText(formerGenericMessage)).toBeNull()
    fireEvent.blur(number)
    expect(number.getAttribute('aria-invalid')).toBe('true')
    expect(screen.getByText('Celular com 9 dígitos começando com 9.')).toBeTruthy()
    expect(onChange).toHaveBeenLastCalledWith('+554833335555')
  })

  it.each(['00', '20'])('rejects invalid area code %s', areaCode => {
    render(<PhoneInput value="" onChange={() => {}} />)
    fireEvent.change(screen.getByRole('textbox', { name: 'DDD' }), { target: { value: areaCode } })
    fireEvent.change(screen.getByRole('textbox', { name: 'Número do telefone' }), { target: { value: '984260543' } })

    const ddd = screen.getByRole('textbox', { name: 'DDD' })
    const number = screen.getByRole('textbox', { name: 'Número do telefone' })
    expect(ddd.getAttribute('aria-invalid')).toBe('false')
    expect(number.getAttribute('aria-invalid')).toBe('false')
    expect(screen.queryByText(formerGenericMessage)).toBeNull()
    fireEvent.blur(number)
    expect(ddd.getAttribute('aria-invalid')).toBe('true')
    expect(screen.getByText('DDD com 2 dígitos válido.')).toBeTruthy()
  })

  it('rejects text instead of treating it as an empty optional phone', () => {
    render(<PhoneInput value="" onChange={() => {}} />)
    const number = screen.getByRole('textbox', { name: 'Número do telefone' })
    fireEvent.change(number, { target: { value: 'telefone' } })

    expect(number.getAttribute('aria-invalid')).toBe('false')
    expect(screen.queryByText(formerGenericMessage)).toBeNull()
    fireEvent.blur(number)
    expect(number.getAttribute('aria-invalid')).toBe('true')
    expect(screen.getByText('Celular com 9 dígitos começando com 9.')).toBeTruthy()
  })

  it('describes an invalid field only after blur or a save attempt', () => {
    render(<PhoneInput value="+554833335555" onChange={() => {}} />)
    const number = screen.getByRole('textbox', { name: 'Número do telefone' })

    expect(number.getAttribute('aria-invalid')).toBe('false')
    expect(number.getAttribute('aria-describedby')).toBeNull()
    fireEvent.blur(number)

    expect(number.getAttribute('aria-invalid')).toBe('true')
    expect(number.getAttribute('aria-describedby')).toContain('subscriber-error')
    expect(screen.queryByText(formerGenericMessage)).toBeNull()
  })

  it('has a field-specific invalid state after a save attempt', () => {
    const { rerender } = render(<PhoneInput value="+554833335555" onChange={() => {}} />)
    const number = screen.getByRole('textbox', { name: 'Número do telefone' })
    expect(number.getAttribute('aria-invalid')).toBe('false')

    rerender(<PhoneInput value="+554833335555" onChange={() => {}} validationAttempted />)

    expect(number.getAttribute('aria-invalid')).toBe('true')
    expect(number.getAttribute('aria-describedby')).toContain('subscriber-error')
    expect(screen.queryByText(formerGenericMessage)).toBeNull()
  })

  it('blocks the legacy contact form on an invalid phone and only shows the field error after submit', () => {
    const fetchSpy = vi.fn()
    vi.stubGlobal('fetch', fetchSpy)
    render(<HelpPage />)

    fireEvent.change(screen.getByLabelText('Seu e-mail'), { target: { value: 'pessoa@example.test' } })
    fireEvent.change(screen.getByLabelText('Sua mensagem'), { target: { value: 'Teste local' } })
    fireEvent.change(screen.getByRole('textbox', { name: 'DDD' }), { target: { value: '20' } })

    const ddd = screen.getByRole('textbox', { name: 'DDD' })
    expect(ddd.getAttribute('aria-invalid')).toBe('false')
    expect(screen.queryByText('DDD com 2 dígitos válido.')).toBeNull()
    expect(screen.queryByText(formerGenericMessage)).toBeNull()

    fireEvent.submit(screen.getByRole('button', { name: 'Enviar mensagem' }).closest('form'))

    expect(ddd.getAttribute('aria-invalid')).toBe('true')
    expect(screen.getByText('DDD com 2 dígitos válido.')).toBeTruthy()
    expect(ddd.getAttribute('aria-describedby')).toContain('area-error')
    expect(screen.queryByText(formerGenericMessage)).toBeNull()
    expect(fetchSpy).not.toHaveBeenCalled()
  })
})
