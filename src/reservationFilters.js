function normalizeText(value) {
  return String(value ?? '')
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase()
}

export function filterReservations(reservations, { texto = '', campoData = 'dataReserva', de = '', ate = '', status = 'Todas' } = {}) {
  const search = normalizeText(texto.trim())
  const validDateRange = !(de && ate && ate < de)

  return reservations.filter(reservation => {
    if (status !== 'Todas' && reservation.statusReserva !== status) return false

    if (search) {
      const requestNumber = String(reservation.idReserva ?? '')
      const searchableValues = [
        reservation.tituloEvento,
        reservation.espaco,
        reservation.nomeEspaco,
        requestNumber,
        `#${requestNumber}`,
      ]

      if (!searchableValues.some(value => normalizeText(value).includes(search))) return false
    }

    if (validDateRange && (de || ate)) {
      const value = campoData === 'solicitacao'
        ? String(reservation.dataSolicitacao ?? '').slice(0, 10)
        : String(reservation.dataReserva ?? '').slice(0, 10)

      if (!value || (de && value < de) || (ate && value > ate)) return false
    }

    return true
  })
}
