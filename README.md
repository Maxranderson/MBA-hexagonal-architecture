# MBA Hexagonal Architecture

## Subindo o projeto

1. Suba o MySQL (porta `3306`, banco `events`, usuário e senha `root`):

   ```bash
   docker compose up -d
   ```

2. Inicie a aplicação:

   ```bash
   ./gradlew :infrastructure:bootRun
   ```

## Rodando os testes

A suíte completa (domínio, aplicação e integração):

```bash
./gradlew test
```

## Cascata de cancelamento

O `CancelEventUseCase` chama `Event.cancel()`. Esse método muda o status do agregado para `CANCELLED` e registra o evento de domínio `EventCancelled` (tipo `event.cancelled`). Ao salvar o evento, o `EventDatabaseRepository` grava o `EventCancelled` na tabela de outbox na mesma transação. O comando termina aí, sem tocar em nenhum ingresso. Em seguida, o `OutboxRelay`, um job agendado a cada 2 segundos, lê as mensagens não publicadas e as entrega ao `ConsumerQueueGateway`. O gateway consome as mensagens de forma assíncrona, no executor `queueExecutor`, e encaminha as do tipo `event.cancelled` para o `CancelEventTicketsUseCase`. Esse caso de uso busca os ingressos do evento por `TicketRepository.ticketsByEventId` e chama `Ticket.cancel()` em cada um. A operação é idempotente: reprocessar a mesma mensagem não falha e os ingressos continuam `CANCELLED`. Assim, os agregados Evento e Ingresso permanecem desacoplados. O teste `EventCancelledOutboxFlowIT` percorre esse caminho inteiro.
