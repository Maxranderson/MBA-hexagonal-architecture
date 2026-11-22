package br.com.fullcycle.infrastructure.graphql;

import br.com.fullcycle.domain.event.Event;
import br.com.fullcycle.domain.event.EventId;
import br.com.fullcycle.domain.event.EventRepository;
import br.com.fullcycle.domain.partner.Partner;
import br.com.fullcycle.domain.partner.PartnerRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.tester.AutoConfigureHttpGraphQlTester;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.graphql.test.tester.HttpGraphQlTester;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@AutoConfigureHttpGraphQlTester
@SpringBootTest
class EventResolverTest {

    @Autowired
    private HttpGraphQlTester graphQlTester;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private PartnerRepository partnerRepository;

    private Event anEvent;

    @BeforeEach
    void setUp() {
        eventRepository.deleteAll();
        partnerRepository.deleteAll();

        final var disney = partnerRepository.create(Partner.newPartner("Disney", "45.123.123/0001-12", "disney@gmail.com"));
        anEvent = eventRepository.create(Event.newEvent("Disney on Ice", "2021-01-01", 100, disney));
    }

    @Test
    @DisplayName("Deve consultar um evento por id via GraphQL incluindo o status")
    public void testEventOfId() {
        graphQlTester.document("""
                        query($id: ID!) { eventOfId(id: $id) { id name date totalSpots status } }
                        """)
                .variable("id", anEvent.eventId().value())
                .execute()
                .path("eventOfId.id").entity(String.class).isEqualTo(anEvent.eventId().value())
                .path("eventOfId.name").entity(String.class).isEqualTo("Disney on Ice")
                .path("eventOfId.totalSpots").entity(Integer.class).isEqualTo(100)
                .path("eventOfId.status").entity(String.class).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("Deve retornar null ao consultar um evento não existente via GraphQL")
    public void testEventOfIdNotFound() {
        graphQlTester.document("""
                        query($id: ID!) { eventOfId(id: $id) { id } }
                        """)
                .variable("id", EventId.unique().value())
                .execute()
                .path("eventOfId").valueIsNull();
    }

    @Test
    @DisplayName("Deve cancelar um evento via GraphQL")
    public void testCancelEvent() {
        graphQlTester.document("""
                        mutation($id: ID!) { cancelEvent(id: $id) { id status } }
                        """)
                .variable("id", anEvent.eventId().value())
                .execute()
                .path("cancelEvent.id").entity(String.class).isEqualTo(anEvent.eventId().value())
                .path("cancelEvent.status").entity(String.class).isEqualTo("CANCELLED");

        Assertions.assertEquals("CANCELLED", eventRepository.eventOfId(anEvent.eventId()).orElseThrow().status().name());
    }

    @Test
    @DisplayName("Deve retornar erro ao cancelar via GraphQL um evento já cancelado")
    public void testCancelEventTwice() {
        final var mutation = """
                mutation($id: ID!) { cancelEvent(id: $id) { id status } }
                """;
        graphQlTester.document(mutation).variable("id", anEvent.eventId().value()).execute();

        graphQlTester.document(mutation)
                .variable("id", anEvent.eventId().value())
                .execute()
                .errors()
                .satisfy(errors -> Assertions.assertFalse(errors.isEmpty()));
    }
}
