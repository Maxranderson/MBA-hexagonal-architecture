package br.com.fullcycle.infrastructure.rest.presenters;

import br.com.fullcycle.application.Presenter;
import br.com.fullcycle.application.event.GetEventByIdUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import java.util.Optional;

@Component("privateGetEvent")
public class GetEventByIdResponseEntity implements Presenter<Optional<GetEventByIdUseCase.Output>, Object> {
    @Override public Object present(Optional<GetEventByIdUseCase.Output> output) { return output.<ResponseEntity<?>>map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build()); }
    @Override public Object present(Throwable error) { return ResponseEntity.notFound().build(); }
}
