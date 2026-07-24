package edu.pucmm.cs.inventory.domain;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad de dominio pura que representa un Movimiento de Stock en el sistema
 * de inventario.
 * 
 * Esta clase es un POJO (Plain Old Java Object) que forma parte de la capa de
 * dominio,
 * siguiendo los principios de Clean Architecture y Domain-Driven Design (DDD).
 * Se mantiene completamente agnóstica de frameworks externos como Spring Boot o
 * JPA.
 * 
 * Encapsula la lógica central para los movimientos de inventario, asegurando
 * mediante
 * su constructor que cualquier movimiento creado se encuentre en un estado
 * válido.
 */
public class StockMovement {

    // Identificador único universal del movimiento de stock
    private UUID id;

    // Identificador único del producto afectado por este movimiento
    private UUID productId;

    // Tipo de movimiento (Entrada - IN o Salida - OUT)
    private MovementType movementType;

    // Foto del stock del producto ANTES de aplicar este movimiento
    private Integer previousQuantity;

    // Foto del stock del producto DESPUÉS de aplicar este movimiento
    private Integer newQuantity;

    // Fecha y hora exacta en la que se registra el movimiento
    private LocalDateTime date;

    // Nombre de usuario o identificador de quien realiza la operación
    private String username;

    // Anotaciones, comentarios o justificaciones sobre el movimiento
    private String observations;

    /**
     * Constructor completo que valida las invariantes del negocio antes de
     * instanciar el objeto.
     *
     * @param id               Identificador del movimiento (no puede ser nulo).
     * @param productId        Identificador del producto (no puede ser nulo).
     * @param movementType     Tipo de movimiento, IN o OUT (no puede ser nulo).
     * @param previousQuantity Stock antes del movimiento (no puede ser negativo).
     * @param newQuantity      Stock después del movimiento (no puede ser negativo).
     * @param date             Fecha de la transacción (no puede ser nula).
     * @param username         Usuario responsable de la acción (no puede ser nulo ni
     *                         estar vacío).
     * @param observations     Notas adicionales (opcional, puede ser nulo o vacío).
     * @throws IllegalArgumentException si alguna de las validaciones falla.
     */
    public StockMovement(UUID id, UUID productId, MovementType movementType,
            Integer previousQuantity, Integer newQuantity,
            LocalDateTime date, String username, String observations) {

        // Validación de campos obligatorios básicos
        if (id == null) {
            throw new IllegalArgumentException("El ID del movimiento de stock no puede ser nulo.");
        }
        if (productId == null) {
            throw new IllegalArgumentException("El ID del producto asociado no puede ser nulo.");
        }
        if (movementType == null) {
            throw new IllegalArgumentException("El tipo de movimiento (IN/OUT) no puede ser nulo.");
        }

        // Regla de negocio: los snapshots de stock no pueden ser negativos
        if (previousQuantity == null || previousQuantity < 0) {
            throw new IllegalArgumentException("La cantidad anterior no puede ser nula ni negativa.");
        }
        if (newQuantity == null || newQuantity < 0) {
            throw new IllegalArgumentException("La cantidad nueva no puede ser nula ni negativa.");
        }

        // Validación de fecha y auditoría manual
        if (date == null) {
            throw new IllegalArgumentException("La fecha del movimiento no puede ser nula.");
        }
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del usuario que realiza el movimiento es obligatorio.");
        }

        // Asignación de estado tras validación exitosa
        this.id = id;
        this.productId = productId;
        this.movementType = movementType;
        this.previousQuantity = previousQuantity;
        this.newQuantity = newQuantity;
        this.date = date;
        this.username = username;
        this.observations = observations;
    }

    // ==========================================
    // GETTERS
    // ==========================================

    /** @return El identificador único del movimiento. */
    public UUID getId() {
        return id;
    }

    /** @return El identificador del producto involucrado. */
    public UUID getProductId() {
        return productId;
    }

    /** @return El tipo de movimiento de inventario. */
    public MovementType getMovementType() {
        return movementType;
    }

    /** @return El stock del producto antes del movimiento. */
    public Integer getPreviousQuantity() {
        return previousQuantity;
    }

    /** @return El stock del producto después del movimiento. */
    public Integer getNewQuantity() {
        return newQuantity;
    }

    /** @return La fecha y hora de la transacción. */
    public LocalDateTime getDate() {
        return date;
    }

    /** @return El nombre de usuario que realizó el movimiento. */
    public String getUsername() {
        return username;
    }

    /** @return Las observaciones adicionales al movimiento. */
    public String getObservations() {
        return observations;
    }
}
