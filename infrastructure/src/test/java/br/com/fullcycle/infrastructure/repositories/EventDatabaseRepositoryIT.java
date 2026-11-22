package br.com.fullcycle.infrastructure.repositories;

import br.com.fullcycle.IntegrationTest;
import br.com.fullcycle.domain.event.Event;
import br.com.fullcycle.domain.event.EventRepository;
import br.com.fullcycle.domain.event.EventStatus;
import br.com.fullcycle.domain.partner.Partner;
import br.com.fullcycle.domain.partner.PartnerRepository;
import br.com.fullcycle.infrastructure.jpa.repositories.OutboxJpaRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.stream.StreamSupport;

class EventDatabaseRepositoryIT extends IntegrationTest {

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private PartnerRepository partnerRepository;

    @Autowired
    private OutboxJpaRepository outboxJpaRepository;

    @BeforeEach
    void setUp() {
        eventRepository.deleteAll();
        partnerRepository.deleteAll();
    }

    @Test
    @DisplayName("Deve persistir um evento novo como ACTIVE")
    public void testCreateActiveEvent() {
        // given
        final var aPartner = partnerRepository.create(Partner.newPartner("John Doe", "41.536.538/0001-00", "john.doe@gmail.com"));

        // when
        final var anEvent = eventRepository.create(Event.newEvent("Disney on Ice", "2021-01-01", 10, aPartner));

        // then
        final var actualEvent = eventRepository.eventOfId(anEvent.eventId()).orElseThrow();
        Assertions.assertEquals(EventStatus.ACTIVE, actualEvent.status());
    }

    @Test
    @DisplayName("Deve persistir o cancelamento do evento e gravar EventCancelled na outbox")
    public void testUpdateCancelledEvent() {
        // given
        final var aPartner = partnerRepository.create(Partner.newPartner("John Doe", "41.536.538/0001-00", "john.doe@gmail.com"));
        final var anEvent = eventRepository.create(Event.newEvent("Disney on Ice", "2021-01-01", 10, aPartner));
        final var eventId = anEvent.eventId().value();

        // when
        anEvent.cancel();
        eventRepository.update(anEvent);

        // then
        final var actualEvent = eventRepository.eventOfId(anEvent.eventId()).orElseThrow();
        Assertions.assertEquals(EventStatus.CANCELLED, actualEvent.status());

        final var outboxMessages = StreamSupport.stream(outboxJpaRepository.findAll().spliterator(), false)
                .filter(it -> it.content().contains("event.cancelled") && it.content().contains(eventId))
                .toList();
        Assertions.assertEquals(1, outboxMessages.size());
    }
}
