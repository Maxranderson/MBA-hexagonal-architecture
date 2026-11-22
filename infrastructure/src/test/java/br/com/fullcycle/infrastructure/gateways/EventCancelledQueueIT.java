package br.com.fullcycle.infrastructure.gateways;

import br.com.fullcycle.IntegrationTest;
import br.com.fullcycle.domain.customer.Customer;
import br.com.fullcycle.domain.event.Event;
import br.com.fullcycle.domain.event.EventCancelled;
import br.com.fullcycle.domain.event.EventRepository;
import br.com.fullcycle.domain.event.ticket.Ticket;
import br.com.fullcycle.domain.event.ticket.TicketRepository;
import br.com.fullcycle.domain.event.ticket.TicketStatus;
import br.com.fullcycle.domain.partner.Partner;
import br.com.fullcycle.domain.partner.PartnerRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class EventCancelledQueueIT extends IntegrationTest {
    @Autowired private QueueGateway consumerQueueGateway;
    @Autowired private EventRepository eventRepository;
    @Autowired private TicketRepository ticketRepository;
    @Autowired private PartnerRepository partnerRepository;
    @Autowired private ObjectMapper objectMapper;

    @BeforeEach
    void clean() {
        ticketRepository.deleteAll();
        eventRepository.deleteAll();
        partnerRepository.deleteAll();
    }

    @Test
    void shouldCancelEventTicketsWhenEventCancelledMessageIsConsumed() throws Exception {
        var partner = partnerRepository.create(Partner.newPartner("Partner", "41.536.538/0001-00", "partner@example.com"));
        var event = eventRepository.create(Event.newEvent("Show", "2027-01-01", 10, partner));
        event.cancel();
        eventRepository.update(event);
        Assertions.assertEquals("CANCELLED", eventRepository.eventOfId(event.eventId()).orElseThrow().status().name());
        var customer = Customer.newCustomer("Customer", "123.456.789-01", "customer@example.com");
        ticketRepository.create(Ticket.newTicket(customer.customerId(), event.eventId()));
        ticketRepository.create(Ticket.newTicket(customer.customerId(), event.eventId()));

        consumerQueueGateway.publish(objectMapper.writeValueAsString(new EventCancelled(event.eventId())));

        long deadline = System.currentTimeMillis() + 5_000;
        while (System.currentTimeMillis() < deadline && ticketRepository.ticketsByEventId(event.eventId()).stream()
                .anyMatch(ticket -> ticket.status() != TicketStatus.CANCELLED)) {
            Thread.sleep(50);
        }

        var tickets = ticketRepository.ticketsByEventId(event.eventId());
        Assertions.assertEquals(2, tickets.size());
        Assertions.assertTrue(tickets.stream().allMatch(ticket -> ticket.status() == TicketStatus.CANCELLED));
    }
}
