package edu.pucmm.cs.inventory.infrastructure.persistence.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.envers.Audited;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Entidad JPA que representa la tabla 'stock_movements' en la base de datos.
 * 
 * Corresponde a la capa de infraestructura, actuando como el adaptador de datos
 * para la persistencia del historial de inventarios. Su único propósito es
 * facilitar el mapeo relacional a través de JPA.
 */
@Entity // Define a la clase como una entidad que será gestionada por el motor de persistencia (Hibernate)
@Table(name = "stock_movements") // Establece el mapeo explícito con la tabla 'stock_movements' de la base de datos
@Audited // Hibernate Envers: Permite la auditoría automática, registrando cada cambio de esta entidad en la tabla 'stock_movements_aud'
public class StockMovementEntity {

    @Id // Especifica que este campo sirve como llave primaria de la tabla
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "movement_type", nullable = false, length = 50)
    private String movementType; // "IN" o "OUT"

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "previous_quantity", nullable = false)
    private Integer previousQuantity; // Stock del producto ANTES de este movimiento

    @Column(name = "new_quantity", nullable = false)
    private Integer newQuantity; // Stock del producto DESPUÉS de este movimiento

    @Column(name = "movement_date", nullable = false)
    private LocalDateTime date;

    @Column(name = "username", nullable = false, length = 150)
    private String username;

    @Column(name = "observations", columnDefinition = "TEXT")
    private String observations;

    /**
     * Constructor público sin argumentos, requerido por el ciclo de vida de JPA
     * para la instanciación de objetos desde los ResultSets de la base de datos.
     */
    public StockMovementEntity() {
    }

    // ==========================================
    // GETTERS Y SETTERS
    // Necesarios para que JPA o librerías como MapStruct accedan y modifiquen los datos
    // ==========================================

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getProductId() { return productId; }
    public void setProductId(UUID productId) { this.productId = productId; }

    public String getMovementType() { return movementType; }
    public void setMovementType(String movementType) { this.movementType = movementType; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public Integer getPreviousQuantity() { return previousQuantity; }
    public void setPreviousQuantity(Integer previousQuantity) { this.previousQuantity = previousQuantity; }

    public Integer getNewQuantity() { return newQuantity; }
    public void setNewQuantity(Integer newQuantity) { this.newQuantity = newQuantity; }

    public LocalDateTime getDate() { return date; }
    public void setDate(LocalDateTime date) { this.date = date; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getObservations() { return observations; }
    public void setObservations(String observations) { this.observations = observations; }
}
