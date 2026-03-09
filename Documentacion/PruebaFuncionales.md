# Pruebas Funcionales de Cambios Implementados

---
# ADR 1 – Validación de datos en el módulo Personas

Para mejorar la calidad de los datos ingresados al sistema se implementaron validaciones en los campos de nombre y apellido dentro de la entidad Person.

Con esta implementación el sistema evita registrar nombres vacíos o con valores numéricos, mejorando la integridad de los datos almacenados en la base de datos.




## Documentación de prueba

La verificación del cambio se realizó revisando el comportamiento del sistema y comparando el código antes y después de la implementación.

Las capturas muestran:

- El estado original del código sin validaciones.
- La implementación de las anotaciones de validación en la entidad.
- La confirmación de que el sistema ahora exige valores válidos para los campos de nombre y apellido.

Esto demuestra que el sistema ahora evita el registro de datos inválidos y mejora la calidad de la información almacenada.

Implementación en PersonController.java

Se agregó la anotación @Valid para activar las validaciones al momento de registrar una persona.

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

Validaciones en PersonRequest
public record PersonRequest(

    @NotBlank(message = "El nombre no puede estar vacío")
    @Pattern(regexp = "^[A-Za-zÁÉÍÓÚáéíóúñÑ ]+$",
    message = "El nombre solo puede contener letras")
    String firstName,

    @NotBlank(message = "El apellido no puede estar vacío")
    @Pattern(regexp = "^[A-Za-zÁÉÍÓÚáéíóúñÑ ]+$",
    message = "El apellido solo puede contener letras")
    String lastName,

    String document,
    String phone,
    String email
) {}
---

# ADR 2 – Validación del correo electrónico en el módulo Usuarios

El módulo Usuarios permite registrar un usuario con correo y contraseña, pero al guardar no había validación de formato. Esto podía generar errores en futuras funcionalidades (autenticación, comunicación).

Agregar validaciones **@NotBlank** y **@Email** al campo de correo electrónico en **appUser.java**.

Se asegura que los usuarios tengan correos válidos y se evitan errores futuros.


## Documentación de prueba

Se documentó el cambio mediante capturas del código antes y después de la implementación.

Las evidencias muestran:

- El campo correo sin validaciones en la versión inicial del código.
- La incorporación de las anotaciones de validación en la entidad AppUser.

---
El cambio se implementó en el archivo:

AppUser.java

Implementación

Implementación en AppUser.java
@Column(nullable = false, unique = true)

@Email(message = "Debe ingresar un correo electrónico válido")
@NotBlank(message = "El correo electrónico no puede estar vacío")

private String email;


---

# ADR 3 – Registro de intentos en el proceso de pagos

Se modificó la acción de Reintentar en **PaymentController.java** para que cada vez que se vuelva a procesar un pago fallido, el campo **attempts** se incremente en 1.

Esto permite llevar un control claro de cuántas veces se ha intentado procesar un pago, manteniendo restricciones como máximo 3 reintentos y solo pagos con estado FALLIDO.

---

## Beneficios

- Mejora la trazabilidad y control del proceso de pagos.
- Asegura la integridad de la información, evitando confusión sobre el número de intentos.
- Es buena práctica aplicar **Single Responsibility Principle (SRP)**, ya que el campo `attempts` se encarga exclusivamente de registrar los intentos del pago.

---


## Documentación de prueba

La verificación se realizó mediante la comparación del código antes y después del cambio.

Las capturas evidencian:

- El método original sin control de incremento de intentos.
- La implementación de la lógica que incrementa el valor del campo `attempts`.

Esto permite registrar correctamente el número de reintentos realizados durante el proceso de pago.

---

Implementación del control de reintentos

Se creó un método que permite reintentar un pago fallido y aumentar el número de intentos.

@PostMapping("/{id}/retry")
public ResponseEntity<?> retryPayment(@PathVariable Long id) {

    Payment payment = paymentRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Pago no encontrado"));

    // Solo permitir reintento si el pago está fallido
    if (payment.getStatus() != PaymentStatus.FALLIDO) {
        return ResponseEntity
                .badRequest()
                .body("El pago no se encuentra en estado FALLIDO, no se puede reintentar.");
    }

    // Limitar a máximo 3 reintentos
    if (payment.getAttempts() >= 3) {
        return ResponseEntity
                .badRequest()
                .body("Se ha alcanzado el máximo número de reintentos.");
    }

    // Incrementar intentos
    payment.setAttempts(payment.getAttempts() + 1);
    paymentRepository.save(payment);

    return ResponseEntity.ok(payment);
}

# ADR 4 – Validación del campo concepto en el módulo Ofrendas

En esta mejora se implementó la validación del campo **concept** en la entidad **Offering.java** para garantizar que las ofrendas registradas en el sistema contengan descripciones claras y consistentes.

Específicamente, se agregaron las siguientes anotaciones de validación de Jakarta Bean Validation:

- **@NotBlank**: asegura que el campo no quede vacío al registrar una ofrenda.
- **@Pattern**: limita el contenido del campo únicamente a letras y espacios.

---

## Objetivo de la implementación

- Evitar registros incorrectos o confusos en el módulo de ofrendas.
- Garantizar la calidad de la información financiera almacenada.
- Facilitar la interpretación de los datos en reportes posteriores.

---

## Por qué es buena práctica

Aplica la **validación de dominio**, asegurando que cada entidad cumpla con las reglas del negocio antes de persistir los datos en la base de datos.

---


## Documentación de prueba

La evidencia del cambio se presenta mediante capturas del código antes y después de la implementación.

Estas capturas permiten identificar claramente la incorporación de las validaciones en la entidad Offering, demostrando que ahora el sistema exige descripciones válidas para registrar una ofrenda.

---

Implementación en Offering.java

Implementación en Offering.java
@NotBlank(message = "El concepto no puede estar vacío")
@Pattern(regexp = "^[a-zA-Z ]+$",
message = "El concepto solo puede contener letras y espacios")

@Column(nullable = false)
private String concept;



# ADR 10 – Control de activación de cursos en el sistema

Se implementó un método **toggleActive()** en **CourseController.java** que permite activar o desactivar cursos existentes, alternando el valor del campo **active**.

Esto facilita gestionar la disponibilidad de los cursos sin eliminarlos de la base de datos.

---

## Beneficios

- Permite controlar qué cursos están disponibles para inscripción.
- Mantiene el historial de cursos y sus registros asociados.
- Es buena práctica aplicar **Single Responsibility Principle (SRP)**, ya que el campo `active` se encarga exclusivamente de controlar la disponibilidad del curso.

---



## Documentación de prueba

La verificación del cambio se documentó mediante capturas del código antes y después de la implementación.

Estas evidencias muestran:

- El estado original del controlador sin la funcionalidad de activación o desactivación.
- La implementación del método que permite alternar el estado del curso.

Con esta mejora ahora es posible gestionar la disponibilidad de los cursos sin eliminar registros del sistema.
Implementación del método toggleActive()

Se agregó un endpoint que permite activar o desactivar un curso sin eliminarlo de la base de datos.

@PreAuthorize("hasRole('ADMIN')")
@PatchMapping("/{id}/toggle-active")
public CourseResponse toggleActive(@PathVariable Long id) {

    Course course = courseRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Curso no encontrado"));

    // Alternar estado del curso
    course.setActive(!course.isActive());
    courseRepository.save(course);

    return CourseResponse.from(course);
}