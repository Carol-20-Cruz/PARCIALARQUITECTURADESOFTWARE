package com.iglesia;

import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
//importancion 
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;

import java.util.List;

@RestController
@RequestMapping("/api/people")
public class PersonController {
    private final PersonRepository personRepository;
    private final ChurchRepository churchRepository;

    public PersonController(PersonRepository personRepository, ChurchRepository churchRepository) {
        this.personRepository = personRepository;
        this.churchRepository = churchRepository;
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('CLIENT')")
    //CAMBIO 
    @PostMapping
public PersonResponse create(@Valid @RequestBody PersonRequest request) {

    Church church = requireChurch();

    Person person = new Person();
    person.setFirstName(request.firstName());
    person.setLastName(request.lastName());
    person.setDocument(request.document());
    person.setPhone(request.phone());
    person.setEmail(request.email());
    person.setChurch(church);

    personRepository.save(person);

    return PersonResponse.from(person);
}

    @PreAuthorize("hasRole('ADMIN') or hasRole('CLIENT')")
    @GetMapping
    public List<PersonResponse> list() {
        Church church = requireChurch();
        return personRepository.findAllByChurchId(church.getId())
            .stream()
            .map(PersonResponse::from)
            .toList();
    }

    private Church requireChurch() {
        return churchRepository.findAll()
            .stream()
            .findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe registrar una iglesia primero"));
    }
 
    //Cambio 
    public record PersonRequest(
    @NotBlank(message = "El nombre no puede estar vacío")
    @Pattern(regexp = "^[A-Za-zÁÉÍÓÚáéíóúñÑ ]+$", message = "El nombre solo puede contener letras")
    String firstName,

    @NotBlank(message = "El apellido no puede estar vacío")
    @Pattern(regexp = "^[A-Za-zÁÉÍÓÚáéíóúñÑ ]+$", message = "El apellido solo puede contener letras")
    String lastName,

    String document,
    String phone,
    String email
) {}

    public record PersonResponse(
        Long id,
        String firstName,
        String lastName,
        String document,
        String phone,
        String email
    ) {
        public static PersonResponse from(Person person) {
            return new PersonResponse(
                person.getId(),
                person.getFirstName(),
                person.getLastName(),
                person.getDocument(),
                person.getPhone(),
                person.getEmail()
            );
        }
    }
}
