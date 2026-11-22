package br.com.fullcycle.infrastructure.gateways;

import br.com.fullcycle.IntegrationTest;
import br.com.fullcycle.application.event.CancelEventUseCase;
import br.com.fullcycle.domain.customer.Customer;
import br.com.fullcycle.domain.event.Event;
import br.com.fullcycle.domain.event.EventId;
import br.com.fullcycle.domain.event.EventRepository;
import br.com.fullcycle.domain.event.ticket.Ticket;
import br.com.fullcycle.domain.event.ticket.TicketRepository;
import br.com.fullcycle.domain.event.ticket.TicketStatus;
import br.com.fullcycle.domain.partner.Partner;
import br.com.fullcycle.domain.partner.PartnerRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Fluxo completo sem atalhos: CancelEventUseCase -> outbox -> OutboxRelay (agendado)
 * -> ConsumerQueueGateway -> CancelEventTicketsUseCase.
 */
class EventCancelledOutboxFlowIT extends IntegrationTest {

    @Autowired
    private CancelEventUseCase cancelEventUseCase;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private PartnerRepository partnerRepository;

    @BeforeEach
    void setUp() {
        ticketRepository.deleteAll();
        eventRepository.deleteAll();
        partnerRepository.deleteAll();
    }

    @Test
    @DisplayName("Deve cancelar os ingressos do evento via outbox e fila após cancelar o evento")
    public void testCancelEventCascadesThroughOutbox() throws Exception {
        // given
        final var aPartner = partnerRepository.create(Partner.newPartner("John Doe", "41.536.538/0001-00", "john.doe@gmail.com"));
        final var anEvent = eventRepository.create(Event.newEvent("Disney on Ice", "2021-01-01", 10, aPartner));
        final var otherEvent = eventRepository.create(Event.newEvent("Disney on Fire", "2021-01-02", 10, aPartner));
        final var aCustomer = Customer.newCustomer("Gabriel Doe", "123.456.789-01", "gabriel.doe@gmail.com");

        ticketRepository.create(Ticket.newTicket(aCustomer.customerId(), anEvent.eventId()));
        ticketRepository.create(Ticket.newTicket(aCustomer.customerId(), anEvent.eventId()));
        final var otherTicket = ticketRepository.create(Ticket.newTicket(aCustomer.customerId(), otherEvent.eventId()));

        // when
        cancelEventUseCase.execute(new CancelEventUseCase.Input(anEvent.eventId().value()));

        // then
        Assertions.assertTrue(
                waitUntilAllTicketsCancelled(anEvent.eventId(), 15_000),
                "Os ingressos do evento cancelado deveriam terminar CANCELLED"
        );
        Assertions.assertEquals(2, ticketRepository.ticketsByEventId(anEvent.eventId()).size());
        Assertions.assertEquals(TicketStatus.PENDING, ticketRepository.ticketOfId(otherTicket.ticketId()).orElseThrow().status());
    }

    private boolean waitUntilAllTicketsCancelled(final EventId eventId, final long timeoutMillis) throws InterruptedException {
        final long deadline = System.currentTimeMillis() + timeoutMillis;
        while (System.currentTimeMillis() < deadline) {
            final var tickets = ticketRepository.ticketsByEventId(eventId);
            if (!tickets.isEmpty() && tickets.stream().allMatch(it -> it.status() == TicketStatus.CANCELLED)) {
                return true;
            }
            Thread.sleep(100);
        }
        return false;
    }
}
