package br.com.fullcycle.application.repository;

import br.com.fullcycle.domain.event.ticket.Ticket;
import br.com.fullcycle.domain.event.ticket.TicketId;
import br.com.fullcycle.domain.event.ticket.TicketRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.List;
import br.com.fullcycle.domain.event.EventId;

public class InMemoryTicketRepository implements TicketRepository {

    private final Map<String, Ticket> tickets;

    public InMemoryTicketRepository() {
        this.tickets = new HashMap<>();
    }

    @Override
    public Optional<Ticket> ticketOfId(TicketId anId) {
        return Optional.ofNullable(this.tickets.get(Objects.requireNonNull(anId).value()));
    }

    @Override
    public Ticket create(Ticket ticket) {
        this.tickets.put(ticket.ticketId().value(), ticket);
        return ticket;
    }

    @Override
    public Ticket update(Ticket ticket) {
        this.tickets.put(ticket.ticketId().value(), ticket);
        return ticket;
    }

    @Override
    public List<Ticket> ticketsByEventId(EventId eventId) {
        return tickets.values().stream().filter(t -> t.eventId().equals(eventId)).toList();
    }

    @Override
    public void deleteAll() {
        this.tickets.clear();
    }
}
