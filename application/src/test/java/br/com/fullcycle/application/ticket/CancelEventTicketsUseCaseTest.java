package br.com.fullcycle.application.ticket;

import br.com.fullcycle.application.repository.InMemoryTicketRepository;
import br.com.fullcycle.domain.customer.Customer;
import br.com.fullcycle.domain.event.Event;
import br.com.fullcycle.domain.event.EventId;
import br.com.fullcycle.domain.event.ticket.Ticket;
import br.com.fullcycle.domain.event.ticket.TicketStatus;
import br.com.fullcycle.domain.partner.Partner;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CancelEventTicketsUseCaseTest {

    @Test
    @DisplayName("Deve cancelar apenas os ingressos do evento informado")
    public void testCancelEventTickets() {
        // given
        final var expectedCancelledTickets = 2;

        final var aPartner = Partner.newPartner("John Doe", "41.536.538/0001-00", "john.doe@gmail.com");
        final var anEvent = Event.newEvent("Disney on Ice", "2021-01-01", 10, aPartner);
        final var otherEvent = Event.newEvent("Disney on Fire", "2021-01-02", 10, aPartner);
        final var aCustomer = Customer.newCustomer("Gabriel Doe", "123.456.789-01", "gabriel.doe@gmail.com");
        final var otherCustomer = Customer.newCustomer("Maria Doe", "123.456.789-02", "maria.doe@gmail.com");

        final var ticketRepository = new InMemoryTicketRepository();
        final var ticket1 = ticketRepository.create(Ticket.newTicket(aCustomer.customerId(), anEvent.eventId()));
        final var ticket2 = ticketRepository.create(Ticket.newTicket(otherCustomer.customerId(), anEvent.eventId()));
        final var otherTicket = ticketRepository.create(Ticket.newTicket(aCustomer.customerId(), otherEvent.eventId()));

        final var input = new CancelEventTicketsUseCase.Input(anEvent.eventId().value());

        // when
        final var useCase = new CancelEventTicketsUseCase(ticketRepository);
        final var output = useCase.execute(input);

        // then
        Assertions.assertEquals(anEvent.eventId().value(), output.eventId());
        Assertions.assertEquals(expectedCancelledTickets, output.cancelledTickets());
        Assertions.assertEquals(TicketStatus.CANCELLED, ticketRepository.ticketOfId(ticket1.ticketId()).orElseThrow().status());
        Assertions.assertEquals(TicketStatus.CANCELLED, ticketRepository.ticketOfId(ticket2.ticketId()).orElseThrow().status());
        Assertions.assertEquals(TicketStatus.PENDING, ticketRepository.ticketOfId(otherTicket.ticketId()).orElseThrow().status());
    }

    @Test
    @DisplayName("Deve ser idempotente ao cancelar ingressos já cancelados")
    public void testCancelEventTicketsTwice() {
        // given
        final var aPartner = Partner.newPartner("John Doe", "41.536.538/0001-00", "john.doe@gmail.com");
        final var anEvent = Event.newEvent("Disney on Ice", "2021-01-01", 10, aPartner);
        final var aCustomer = Customer.newCustomer("Gabriel Doe", "123.456.789-01", "gabriel.doe@gmail.com");

        final var ticketRepository = new InMemoryTicketRepository();
        final var aTicket = ticketRepository.create(Ticket.newTicket(aCustomer.customerId(), anEvent.eventId()));

        final var input = new CancelEventTicketsUseCase.Input(anEvent.eventId().value());
        final var useCase = new CancelEventTicketsUseCase(ticketRepository);
        useCase.execute(input);

        // when
        Assertions.assertDoesNotThrow(() -> useCase.execute(input));

        // then
        Assertions.assertEquals(TicketStatus.CANCELLED, ticketRepository.ticketOfId(aTicket.ticketId()).orElseThrow().status());
    }

    @Test
    @DisplayName("Não deve falhar quando o evento não possui ingressos")
    public void testCancelEventTicketsWithoutTickets() {
        // given
        final var expectedCancelledTickets = 0;
        final var input = new CancelEventTicketsUseCase.Input(EventId.unique().value());

        // when
        final var useCase = new CancelEventTicketsUseCase(new InMemoryTicketRepository());
        final var output = useCase.execute(input);

        // then
        Assertions.assertEquals(expectedCancelledTickets, output.cancelledTickets());
    }
}
