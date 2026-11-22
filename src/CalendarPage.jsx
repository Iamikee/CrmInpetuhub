import { useEffect, useState } from 'react'
import { CalendarDays, ChevronLeft, ChevronRight, Users } from 'lucide-react'
import { Button, RoomPhoto, Title, addMinutesToTime, isGuidedTour, localDateValue, request, saoPauloDateTime } from './App.jsx'
import BookingTimePicker from './BookingTimePicker.jsx'
import { formatWeeklyHours, getApprovalMessage, getClosedDayMessage, getDefaultEndTime, getHoursForDate, getPossibleEndTimes, getTimeSlots, validateOperatingHours } from './openingHours.js'

export default function CalendarPage({ spaces, spacesLoaded, onReserve, initialDate, initialKind, initialSpaceId, availabilityVersion }) {
  const [date, setDate] = useState(initialDate)
  const [monthCursor, setMonthCursor] = useState(() => new Date(`${initialDate}T12:00:00`))
  const [monthCalendarOpen, setMonthCalendarOpen] = useState(false)
  const [availability, setAvailability] = useState([])
  const [availabilityLoading, setAvailabilityLoading] = useState(false)
  const [availabilityError, setAvailabilityError] = useState('')
  const [availabilityReload, setAvailabilityReload] = useState(0)
  const [operatingHours, setOperatingHours] = useState([])
  const [operatingHoursLoading, setOperatingHoursLoading] = useState(false)
  const [operatingHoursError, setOperatingHoursError] = useState('')
  const typeOrder = ['Auditório', 'Sala de aula', 'Coworking', 'Sala de reunião', 'Visita guiada']
  const orderedSpaces = [...spaces].sort((left, right) => {
    const leftIndex = typeOrder.indexOf(left.tipoEspaco)
    const rightIndex = typeOrder.indexOf(right.tipoEspaco)
    return (leftIndex < 0 ? typeOrder.length : leftIndex) - (rightIndex < 0 ? typeOrder.length : rightIndex) ||
      left.nomeEspaco.localeCompare(right.nomeEspaco, 'pt-BR')
  })
  const [selectedSpaceId, setSelectedSpaceId] = useState(initialSpaceId || '')
  const [startTime, setStartTime] = useState('')
  const [endTime, setEndTime] = useState('')
  const [saoPauloNow, setSaoPauloNow] = useState(saoPauloDateTime)
  const selectedSpaceCandidate = orderedSpaces.find(space => String(space.idEspaco) === selectedSpaceId)
  const unavailableSpace = selectedSpaceCandidate?.ativo === false ? selectedSpaceCandidate : null
  const selectedSpace = selectedSpaceCandidate?.ativo !== false ? selectedSpaceCandidate : undefined
  const guidedTour = isGuidedTour(selectedSpace?.tipoEspaco)
  useEffect(() => {
    const interval = window.setInterval(() => setSaoPauloNow(saoPauloDateTime()), 30_000)
    return () => window.clearInterval(interval)
  }, [])
  useEffect(() => {
    if (!initialSpaceId || !spacesLoaded) return
    const timer = window.setTimeout(() => {
      document.getElementById('booking-date-title')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
    }, 150)
    return () => window.clearTimeout(timer)
  }, [initialSpaceId, spacesLoaded])
  useEffect(() => {
    if (selectedSpaceId || !spacesLoaded) return
    const suggestedSpace = spaces.find(space => space.tipoEspaco === initialKind && space.ativo !== false)
    if (suggestedSpace) setSelectedSpaceId(String(suggestedSpace.idEspaco))
  }, [initialKind, selectedSpaceId, spaces, spacesLoaded])
  useEffect(() => {
    setStartTime('')
    setEndTime('')
  }, [date, selectedSpace?.idEspaco])
  useEffect(() => {
    if (!selectedSpace || !date || selectedSpace.ativo === false) {
      setAvailability([])
      setAvailabilityLoading(false)
      setAvailabilityError('')
      return
    }
    let active = true
    setAvailability([])
    setAvailabilityLoading(true)
    setAvailabilityError('')
    request(`/espacos/${selectedSpace.idEspaco}/agenda?data=${encodeURIComponent(date)}`)
      .then(data => { if (active) setAvailability(data) })
      .catch(error => { if (active) setAvailabilityError(error.message) })
      .finally(() => { if (active) setAvailabilityLoading(false) })
    return () => { active = false }
  }, [selectedSpace?.idEspaco, selectedSpace?.ativo, date, availabilityReload, availabilityVersion])
  useEffect(() => {
    if (!selectedSpace) {
      setOperatingHours([])
      setOperatingHoursLoading(false)
      setOperatingHoursError('')
      return
    }
    let active = true
    setOperatingHours([])
    setOperatingHoursLoading(true)
    setOperatingHoursError('')
    request(`/espacos/${selectedSpace.idEspaco}/horarios-funcionamento`)
      .then(data => { if (active) setOperatingHours(data) })
      .catch(error => { if (active) setOperatingHoursError(error.message) })
      .finally(() => { if (active) setOperatingHoursLoading(false) })
    return () => { active = false }
  }, [selectedSpace?.idEspaco])
  const selectedDay = new Date(`${date}T12:00:00`)
  const today = new Date(`${saoPauloNow.date}T12:00:00`)
  today.setHours(12, 0, 0, 0)
  const upcomingDays = Array.from({ length: 14 }, (_, index) => {
    const day = new Date(today)
    day.setDate(today.getDate() + index)
    return day
  })
  const selectedOperatingHours = getHoursForDate(operatingHours, date)
  const selectedDateClosed = selectedOperatingHours?.fechado === true
  const selectedDateApproval = getApprovalMessage(operatingHours, date)
  const slots = getTimeSlots(operatingHours, date)
  const possibleEndTimes = getPossibleEndTimes(operatingHours, date, startTime, availability, saoPauloNow)
  const monthStart = new Date(monthCursor.getFullYear(), monthCursor.getMonth(), 1, 12)
  const monthDays = Array.from({ length: new Date(monthCursor.getFullYear(), monthCursor.getMonth() + 1, 0).getDate() }, (_, index) => {
    const day = new Date(monthStart)
    day.setDate(index + 1)
    return day
  })
  const displayDate = selectedDay.toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit', year: 'numeric' })
  function reservationAt(time) {
    const end = addMinutesToTime(time, 30)
    return availability.find(reservation => {
      const start = String(reservation.inicio).slice(0, 5)
      const finish = String(reservation.fim).slice(0, 5)
      return time < finish && end > start
    })
  }
  function isRangeAvailable(start, end) {
    if (!start || !end || end <= start || validateOperatingHours(operatingHours, date, start, end)) return false
    for (let cursor = start; cursor < end; cursor = addMinutesToTime(cursor, 30)) {
      if (reservationAt(cursor)) return false
    }
    return true
  }
  const canRequestReservation = Boolean(selectedSpace) && operatingHours.length > 0 && !operatingHoursLoading && !operatingHoursError &&
    date >= saoPauloNow.date && !availabilityLoading && !availabilityError &&
    isRangeAvailable(startTime, endTime)
  const reservationRequestHint = unavailableSpace
    ? `Indisponível: ${unavailableSpace.motivoIndisponibilidade || 'Este espaço está temporariamente indisponível.'}`
    : availabilityLoading
      ? 'Aguarde a consulta dos horários.'
      : availabilityError
        ? 'Não é possível solicitar a reserva até a disponibilidade ser validada.'
        : operatingHoursLoading
          ? 'Aguarde a consulta do horário de funcionamento.'
          : operatingHoursError
            ? 'Não é possível solicitar a reserva até o funcionamento ser validado.'
        : !selectedSpace
          ? 'Escolha um espaço para continuar.'
          : date < saoPauloNow.date
            ? 'Escolha uma data de hoje em diante.'
            : selectedDateClosed
              ? getClosedDayMessage(date)
            : !startTime
              ? 'Escolha um horário de início.'
              : !endTime
                ? 'Escolha um horário de término.'
                : canRequestReservation
                  ? 'Intervalo disponível para solicitação.'
                  : 'Escolha um intervalo disponível.'
  const reservationDurationMinutes = startTime && endTime
    ? (Number(endTime.slice(0, 2)) * 60 + Number(endTime.slice(3, 5))) - (Number(startTime.slice(0, 2)) * 60 + Number(startTime.slice(3, 5)))
    : 0
  const reservationDurationLabel = reservationDurationMinutes
    ? `${Math.floor(reservationDurationMinutes / 60) ? `${Math.floor(reservationDurationMinutes / 60)} h` : ''}${reservationDurationMinutes % 60 ? `${Math.floor(reservationDurationMinutes / 60) ? ' ' : ''}${reservationDurationMinutes % 60} min` : ''}`
    : ''
  function selectStartTime(time) {
    setStartTime(time)
    setEndTime(getDefaultEndTime(getPossibleEndTimes(operatingHours, date, time, availability, saoPauloNow), time))
  }
  const startDisabledReasons = Object.fromEntries(slots.map(time => {
    const reason = date === saoPauloNow.date && time < saoPauloNow.time
      ? 'horário já passado'
      : reservationAt(time)
        ? reservationAt(time).statusReserva === 'Pendente' ? 'em análise' : 'ocupado'
        : getPossibleEndTimes(operatingHours, date, time, availability, saoPauloNow).length === 0
          ? 'sem horário de fim disponível'
          : ''
    return [time, reason]
  }))
  const disabledStartTimes = new Set(Object.keys(startDisabledReasons).filter(time => startDisabledReasons[time]))
  function changeStart(time) {
    if (!time) {
      setStartTime('')
      setEndTime('')
      return
    }
    selectStartTime(time)
  }
  function selectDate(day) {
    const value = `${day.getFullYear()}-${String(day.getMonth() + 1).padStart(2, '0')}-${String(day.getDate()).padStart(2, '0')}`
    setDate(value)
    setMonthCursor(new Date(`${value}T12:00:00`))
  }
  function moveMonth(offset) {
    setMonthCursor(current => new Date(current.getFullYear(), current.getMonth() + offset, 1, 12))
  }
  function goToToday() {
    const value = saoPauloNow.date
    setDate(value)
    const [year, month] = value.split('-').map(Number)
    setMonthCursor(new Date(year, month - 1, 1, 12))
  }
  function dateButton(day) {
    const value = localDateValue(day)
    const isSelected = value === date
    const isToday = value === saoPauloNow.date
    const closed = getHoursForDate(operatingHours, value)?.fechado === true
    const approvalRequired = getHoursForDate(operatingHours, value)?.exigeAprovacao === true
    const dayLabel = day.toLocaleDateString('pt-BR', { weekday: 'long', day: 'numeric', month: 'long' })
    return <button key={value} type="button" disabled={value < saoPauloNow.date || operatingHoursLoading || !operatingHours.length || closed || Boolean(operatingHoursError)} className={`calendar-date-option ${closed ? 'calendar-date-closed' : ''}`} onClick={() => selectDate(day)} aria-label={`${dayLabel}${closed ? ', Fechado' : approvalRequired ? ', somente com aprovação da equipe' : ''}`} aria-current={isToday ? 'date' : undefined} aria-pressed={isSelected}>
      <span>{day.getDate()}</span>{closed && <small>Fechado</small>}{approvalRequired && <span className="calendar-date-approval-dot" role="img" aria-label="Domingo, depende de aprovação da equipe" />}
    </button>
  }
  return <main className="inner-page catalog-page booking-page">
    <header className="catalog-heading booking-heading">
      <Title as="h1">Agende um espaço</Title>
      <p>Escolha o ambiente, defina o dia e horário e envie sua solicitação.</p>
    </header>
    <ol className="booking-steps" aria-label="Etapas da reserva">
      <li className={selectedSpace ? 'booking-step-complete' : 'booking-step-current'}><span className="booking-step-number">1</span><strong>Espaço</strong></li>
      <li className={startTime && endTime ? 'booking-step-complete' : selectedSpace ? 'booking-step-current' : ''}><span className="booking-step-number">2</span><strong>Dia e horário</strong></li>
      <li className={canRequestReservation ? 'booking-step-current' : ''}><span className="booking-step-number">3</span><strong>Confirmar</strong></li>
    </ol>

    <div className="booking-layout">
      <div className="booking-main">
        <section className="booking-section" aria-labelledby="booking-space-title">
          <div className="booking-section-heading"><span className="booking-section-number">01</span><div><Title as="h2" id="booking-space-title">Escolha o espaço</Title><p>Selecione um ambiente para ver a agenda.</p></div></div>
          {!spacesLoaded ? <p className="booking-state" role="status">Carregando espaços...</p> : orderedSpaces.length === 0 ? <p className="booking-state">Nenhum espaço cadastrado no momento.</p> : <div className="booking-space-grid">
            {orderedSpaces.map(space => {
              const unavailable = space.ativo === false
              const selected = selectedSpace?.idEspaco === space.idEspaco
              const needsManualReview = space.exigeAprovacaoManual
              return <button key={space.idEspaco} type="button" className={`booking-space-card ${selected ? 'booking-space-selected' : ''}`} disabled={unavailable} onClick={() => setSelectedSpaceId(String(space.idEspaco))} aria-pressed={selected} aria-label={`${space.nomeEspaco}, até ${space.capacidadePessoas || 20} pessoas${unavailable ? `, indisponível: ${space.motivoIndisponibilidade || 'temporariamente indisponível'}` : ''}`}>
                <RoomPhoto space={space} alt="" className="booking-space-photo" />
                {unavailable && <span className="booking-unavailable-label">Indisponível</span>}
                <span className="booking-space-copy"><span className="booking-space-category">{space.tipoEspaco}</span><strong className="booking-space-name">{space.nomeEspaco}</strong><span className="booking-space-capacity icon-text"><Users size={14} /> Até {space.capacidadePessoas || 20} pessoas</span><span className={`booking-space-status ${unavailable ? 'booking-status-unavailable' : 'booking-status-available'}`}>{unavailable ? 'Indisponível' : 'Disponível'}</span>
                  {needsManualReview && <span className="booking-manual-badge">Requer análise manual</span>}
                  {unavailable && <span className="booking-unavailable-reason">{space.motivoIndisponibilidade || 'Este espaço está temporariamente indisponível.'}</span>}
                </span>
              </button>
            })}
          </div>}
        </section>

        <section className="booking-section" aria-labelledby="booking-date-title">
          <div className="booking-section-heading"><span className="booking-section-number">02</span><div><Title as="h2" id="booking-date-title">Escolha dia e horário</Title><p>Selecione uma data e um intervalo disponível.</p></div></div>
          <div className="booking-date-toolbar"><Title as="h3">Próximos 14 dias</Title><button type="button" className="booking-calendar-toggle" aria-expanded={monthCalendarOpen} aria-controls="booking-month-calendar" onClick={() => setMonthCalendarOpen(open => !open)}><CalendarDays size={15} /> {monthCalendarOpen ? 'Fechar calendário' : 'Abrir calendário'}</button></div>
          <div className="booking-date-rail" role="group" aria-label="Selecione um dos próximos 14 dias">
            {upcomingDays.map(day => {
              const value = localDateValue(day)
              const selected = date === value
              const isToday = value === localDateValue()
              const closed = getHoursForDate(operatingHours, value)?.fechado === true
              const approvalRequired = getHoursForDate(operatingHours, value)?.exigeAprovacao === true
              const weekday = day.toLocaleDateString('pt-BR', { weekday: 'long' })
              return <button key={value} type="button" className={`booking-date-card ${selected ? 'booking-date-selected' : ''} ${closed ? 'booking-date-closed' : ''}`} disabled={operatingHoursLoading || !operatingHours.length || closed || Boolean(operatingHoursError)} onClick={() => selectDate(day)} aria-label={`${weekday}, ${day.getDate()} ${day.toLocaleDateString('pt-BR', { month: 'long' })}${closed ? ', Fechado' : approvalRequired ? ', somente com aprovação da equipe' : ''}`} aria-pressed={selected} aria-current={isToday ? 'date' : undefined}>
                <span>{isToday ? 'Hoje' : day.toLocaleDateString('pt-BR', { weekday: 'short' }).replace('.', '')}</span><strong>{day.getDate()}</strong><small>{day.toLocaleDateString('pt-BR', { month: 'short' }).replace('.', '')}</small>{closed && <small className="booking-date-closed-label">Fechado</small>}{approvalRequired && <span className="booking-date-approval-dot" role="img" aria-label="Domingo, depende de aprovação da equipe" />}
              </button>
            })}
          </div>
          {monthCalendarOpen && <div id="booking-month-calendar" className="booking-month-calendar">
            <div className="calendar-month-heading"><button type="button" aria-label="Mês anterior" disabled={monthCursor.getFullYear() === new Date().getFullYear() && monthCursor.getMonth() <= new Date().getMonth()} onClick={() => moveMonth(-1)}><ChevronLeft size={17} /></button><strong className="calendar-month-label">{monthCursor.toLocaleDateString('pt-BR', { month: 'long', year: 'numeric' })}</strong><button type="button" aria-label="Próximo mês" onClick={() => moveMonth(1)}><ChevronRight size={17} /></button><button type="button" className="calendar-today-button" onClick={goToToday}>Hoje</button></div>
            {['Dom', 'Seg', 'Ter', 'Qua', 'Qui', 'Sex', 'Sáb'].map((day, index) => <span key={`${day}-${index}`} className="calendar-weekday">{day}</span>)}
            {Array.from({ length: monthStart.getDay() }, (_, index) => <span key={`empty-${index}`} className="calendar-date-empty" />)}
            {monthDays.map(day => dateButton(day))}
          </div>}
          <div className="booking-legend" aria-label="Legenda de disponibilidade"><span><i className="booking-legend-free" /> Livre</span><span><i className="booking-legend-busy" /> Ocupado</span><span><i className="booking-legend-pending" /> Em análise</span></div>
          {guidedTour && <p className="booking-guided-note" role="note">Visitas guiadas exigem análise da equipe. O horário solicitado fica reservado enquanto aguarda a avaliação.</p>}
          {selectedDateApproval && <p className="booking-guided-note" role="note">{selectedDateApproval}</p>}
          {!selectedSpace ? <p className="booking-state">{unavailableSpace ? `${unavailableSpace.nomeEspaco}: ${unavailableSpace.motivoIndisponibilidade || 'Este espaço está temporariamente indisponível.'}` : 'Escolha um espaço para consultar os horários.'}</p> : operatingHoursLoading ? <p className="booking-state" role="status">Consultando o funcionamento...</p> : operatingHoursError ? <p className="booking-state" role="alert">Não foi possível consultar o funcionamento: {operatingHoursError}</p> : selectedDateClosed ? <p className="booking-state" role="status">{getClosedDayMessage(date)}</p> : availabilityLoading ? <p className="booking-state" role="status">Consultando disponibilidade...</p> : availabilityError ? <div className="booking-error" role="alert"><span>Não foi possível consultar a disponibilidade: {availabilityError}</span><button type="button" onClick={() => setAvailabilityReload(value => value + 1)}>Tentar novamente</button></div> : <div className="booking-time-groups">
            <BookingTimePicker
              startTime={startTime}
              endTime={endTime}
              startTimes={slots}
              possibleEndTimes={possibleEndTimes}
              disabledStartTimes={disabledStartTimes}
              startDisabledReasons={startDisabledReasons}
              onStartChange={changeStart}
              onEndChange={setEndTime}
            />
          </div>}
          {startTime && endTime > startTime && !availabilityLoading && !availabilityError && !isRangeAvailable(startTime, endTime) && <p className="booking-validation-error" role="alert">{validateOperatingHours(operatingHours, date, startTime, endTime) || 'O período escolhido inclui um horário ocupado ou em análise.'}</p>}
          <p className="booking-hours-note">Funcionamento semanal: {formatWeeklyHours(operatingHours).replace(/\.$/, '') || (operatingHoursError ? 'Indisponível' : 'Carregando...')}. Horários ocupados ou em análise não podem ser reservados.</p>
        </section>
      </div>

      <aside className="booking-summary" aria-labelledby="booking-summary-title">
        <Title as="h2" id="booking-summary-title">Resumo da solicitação</Title>
        <dl><div><dt>Espaço</dt><dd>{selectedSpace?.nomeEspaco || unavailableSpace?.nomeEspaco || 'Não selecionado'}</dd></div><div><dt>Data</dt><dd>{date ? displayDate : 'Não selecionada'}</dd></div><div><dt>Horário</dt><dd>{startTime && endTime ? `${startTime}-${endTime} · ${reservationDurationLabel}` : 'Não selecionado'}</dd></div></dl>
        {selectedSpace?.exigeAprovacaoManual && <p className="booking-summary-review">Este pedido requer análise manual.</p>}
        <p id="booking-request-help" className={`booking-summary-hint ${availabilityError ? 'booking-summary-hint-error' : ''}`} role={availabilityLoading ? 'status' : undefined}>{reservationRequestHint}</p>
        <Button icon={CalendarDays} aria-describedby="booking-request-help" disabled={!canRequestReservation} onClick={() => onReserve(selectedSpace, date, startTime, endTime)}>{guidedTour ? 'Solicitar visita' : 'Solicitar reserva'}</Button>
      </aside>
    </div>
  </main>
}