import { useEffect, useState } from 'react'
import { Check } from 'lucide-react'
import { getEndTimeDisplayOptions } from './openingHours.js'

function formatDuration(start, end) {
  const startMinutes = Number(start.slice(0, 2)) * 60 + Number(start.slice(3, 5))
  const endMinutes = Number(end.slice(0, 2)) * 60 + Number(end.slice(3, 5))
  const durationMinutes = endMinutes - startMinutes
  const hours = Math.floor(durationMinutes / 60)
  const minutes = durationMinutes % 60
  return [hours ? `${hours} h` : '', minutes ? `${minutes} min` : ''].filter(Boolean).join(' ')
}

export default function BookingTimePicker({
  startTime,
  endTime,
  startTimes,
  possibleEndTimes,
  disabledStartTimes,
  startDisabledReasons = {},
  disabled = false,
  onStartChange,
  onEndChange,
}) {
  const [expandedOverride, setExpandedOverride] = useState(null)
  const endTimeOptions = getEndTimeDisplayOptions(possibleEndTimes, endTime, expandedOverride)
  useEffect(() => {
    setExpandedOverride(null)
  }, [startTime])

  function changeStart(time) {
    setExpandedOverride(null)
    onStartChange(time)
  }

  return <div className="booking-time-picker">
    <fieldset className="booking-time-step" disabled={disabled}>
      <legend>Passo 1: Início</legend>
      {startTime
        ? <div className="booking-locked-start"><strong>Início: {startTime}</strong><button type="button" className="booking-change-start" onClick={() => changeStart('')}>Alterar</button></div>
        : <div className="booking-choice-grid" role="group" aria-label="Escolha o horário de início">
          {startTimes.map(time => {
            const reason = startDisabledReasons[time]
            return <button key={time} type="button" className="booking-choice-option" disabled={disabled || disabledStartTimes.has(time)} onClick={() => changeStart(time)} aria-label={`${time}${reason ? `, ${reason}` : ', disponível'}`}>
              <strong>{time}</strong>
            </button>
          })}
        </div>}
    </fieldset>
    {startTime && <section className="booking-time-step" aria-labelledby="booking-end-step-title">
      <h3 id="booking-end-step-title">Passo 2: Agora escolha o fim</h3>
      {possibleEndTimes.length
        ? <div className="booking-choice-grid" role="group" aria-label="Escolha o horário de fim">
          {endTimeOptions.visibleEndTimes.map(time => {
            const duration = formatDuration(startTime, time)
            return <button key={time} type="button" className={`booking-choice-option booking-end-option ${time === endTime ? 'booking-choice-selected' : ''}`} disabled={disabled} onClick={() => onEndChange(time)} aria-pressed={time === endTime} aria-label={`${time}, ${duration}${time === endTime ? ', selecionado' : ''}`}>
              <strong>{time}{time === endTime && <Check className="booking-choice-check" size={16} aria-hidden="true" />}</strong><small>{duration}</small>
            </button>
          })}
          {endTimeOptions.hasMore && <button type="button" className="booking-more-times" aria-expanded={endTimeOptions.expanded} onClick={() => setExpandedOverride(!endTimeOptions.expanded)}>{endTimeOptions.expanded ? 'Ver menos' : 'Ver mais horários'}</button>}
        </div>
        : <p className="booking-state" role="status">Não há horários de fim disponíveis para este início.</p>}
    </section>}
    {startTime && endTime && <p className="booking-time-summary" role="status">{startTime} às {endTime} ({formatDuration(startTime, endTime)})</p>}
  </div>
}
