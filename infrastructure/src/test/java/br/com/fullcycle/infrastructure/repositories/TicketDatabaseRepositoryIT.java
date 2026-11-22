package br.com.fullcycle.infrastructure.repositories;

import br.com.fullcycle.IntegrationTest;
import br.com.fullcycle.domain.customer.Customer;
import br.com.fullcycle.domain.event.EventId;
import br.com.fullcycle.domain.event.ticket.Ticket;
import br.com.fullcycle.domain.event.ticket.TicketId;
import br.com.fullcycle.domain.event.ticket.TicketRepository;
import br.com.fullcycle.domain.event.ticket.TicketStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Set;
import java.util.stream.Collectors;

class TicketDatabaseRepositoryIT extends IntegrationTest {

    @Autowired
    private TicketRepository ticketRepository;

    @BeforeEach
    void setUp() {
        ticketRepository.deleteAll();
    }

    @Test
    @DisplayName("Deve buscar no banco apenas os ingressos do evento informado")
    public void testTicketsByEventId() {
        // given
        final var anEventId = EventId.unique();
        final var otherEventId = EventId.unique();
        final var aCustomer = Customer.newCustomer("Gabriel Doe", "123.456.789-01", "gabriel.doe@gmail.com");
        final var otherCustomer = Customer.newCustomer("Maria Doe", "123.456.789-02", "maria.doe@gmail.com");

        final var ticket1 = ticketRepository.create(Ticket.newTicket(aCustomer.customerId(), anEventId));
        final var ticket2 = ticketRepository.create(Ticket.newTicket(otherCustomer.customerId(), anEventId));
        ticketRepository.create(Ticket.newTicket(aCustomer.customerId(), otherEventId));

        final var expectedIds = Set.of(ticket1.ticketId(), ticket2.ticketId());

        // when
        final var actualTickets = ticketRepository.ticketsByEventId(anEventId);

        // then
        final Set<TicketId> actualIds = actualTickets.stream().map(Ticket::ticketId).collect(Collectors.toSet());
        Assertions.assertEquals(expectedIds, actualIds);
        Assertions.assertTrue(actualTickets.stream().allMatch(it -> it.eventId().equals(anEventId)));
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando o evento não possui ingressos")
    public void testTicketsByEventIdWithoutTickets() {
        // when
        final var actualTickets = ticketRepository.ticketsByEventId(EventId.unique());

        // then
        Assertions.assertTrue(actualTickets.isEmpty());
    }

    @Test
    @DisplayName("Deve persistir o status CANCELLED de um ingresso")
    public void testUpdateCancelledTicket() {
        // given
        final var aCustomer = Customer.newCustomer("Gabriel Doe", "123.456.789-01", "gabriel.doe@gmail.com");
        final var aTicket = ticketRepository.create(Ticket.newTicket(aCustomer.customerId(), EventId.unique()));

        // when
        aTicket.cancel();
        ticketRepository.update(aTicket);

        // then
        final var actualTicket = ticketRepository.ticketOfId(aTicket.ticketId()).orElseThrow();
        Assertions.assertEquals(TicketStatus.CANCELLED, actualTicket.status());
    }
}
