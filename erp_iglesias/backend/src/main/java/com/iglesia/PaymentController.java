package com.iglesia;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    private final PaymentRepository paymentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final OfferingRepository offeringRepository;

    public PaymentController(PaymentRepository paymentRepository,
                             EnrollmentRepository enrollmentRepository,
                             OfferingRepository offeringRepository) {
        this.paymentRepository = paymentRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.offeringRepository = offeringRepository;
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('CLIENT')")
    @GetMapping
    public List<PaymentResponse> list(@RequestParam(name = "status", required = false) PaymentStatus status) {
        List<Payment> payments = status == null ? paymentRepository.findAll() : paymentRepository.findAllByStatus(status);
        return payments.stream().map(PaymentResponse::from).toList();
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('CLIENT')")
    @PostMapping("/{id}/confirm")
    public PaymentResponse confirm(@PathVariable Long id) {
        Payment payment = paymentRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pago no encontrado"));

        payment.setStatus(PaymentStatus.CONFIRMADO);
        paymentRepository.save(payment);

        if (payment.getType() == PaymentType.INSCRIPCION_CURSO) {
            Enrollment enrollment = enrollmentRepository.findById(payment.getReferenceId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Inscripción no encontrada"));
            enrollment.setStatus(EnrollmentStatus.PAGADA);
            enrollmentRepository.save(enrollment);
        } else if (payment.getType() == PaymentType.OFRENDA) {
            Offering offering = offeringRepository.findById(payment.getReferenceId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ofrenda no encontrada"));
            offering.setStatus(OfferingStatus.REGISTRADA);
            offeringRepository.save(offering);
        }

        return PaymentResponse.from(payment);
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('CLIENT')")
    @PostMapping("/{id}/fail")
    public PaymentResponse fail(@PathVariable Long id) {
        Payment payment = paymentRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pago no encontrado"));

        if (payment.getStatus() == PaymentStatus.CONFIRMADO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El pago ya fue confirmado");
        }

        payment.setAttempts(payment.getAttempts() + 1);
        payment.setStatus(PaymentStatus.FALLIDO);
        paymentRepository.save(payment);

        return PaymentResponse.from(payment);
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('CLIENT')")
    //CAMBIOS 
    @PostMapping("/{id}/retry")
    @PostMapping("/payments/{id}/retry")
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

    // Aquí puedes llamar la lógica de procesamiento de pago nuevamente
    // processPayment(payment);

    return ResponseEntity.ok(payment);
}

    public record PaymentResponse(
        Long id,
        String type,
        String status,
        String amount,
        int attempts,
        Long referenceId
    ) {
        public static PaymentResponse from(Payment payment) {
            return new PaymentResponse(
                payment.getId(),
                payment.getType().name(),
                payment.getStatus().name(),
                payment.getAmount().toPlainString(),
                payment.getAttempts(),
                payment.getReferenceId()
            );
        }
    }
}
