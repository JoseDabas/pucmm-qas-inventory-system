package edu.pucmm.cs.inventory.application;

import edu.pucmm.cs.inventory.domain.MovementType;
import edu.pucmm.cs.inventory.infrastructure.persistence.entity.ProductEntity;
import edu.pucmm.cs.inventory.infrastructure.persistence.entity.StockMovementEntity;
import edu.pucmm.cs.inventory.infrastructure.persistence.repository.ProductJpaRepository;
import edu.pucmm.cs.inventory.infrastructure.persistence.repository.StockMovementJpaRepository;
import edu.pucmm.cs.inventory.infrastructure.web.dto.StockMovementRequestDTO;
import edu.pucmm.cs.inventory.infrastructure.web.dto.StockMovementResponseDTO;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.lang.NonNull;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Servicio de Aplicación (Caso de Uso) para el Historial de Movimientos de stock.
 *
 * Orquesta la consulta paginada del historial (entradas y salidas) y el registro
 * de nuevos movimientos, calculando la cantidad anterior y nueva a partir del
 * ledger inmutable de movimientos. Se mantiene separado de ProductService para
 * respetar la responsabilidad única.
 */
@Service
public class StockMovementService {

    private final StockMovementJpaRepository stockMovementRepository;
    private final ProductJpaRepository productRepository;

    public StockMovementService(StockMovementJpaRepository stockMovementRepository,
            ProductJpaRepository productRepository) {
        this.stockMovementRepository = stockMovementRepository;
        this.productRepository = productRepository;
    }

    /**
     * Consulta paginada del historial de movimientos. Si no se especifica un
     * ordenamiento explícito, se ordena por fecha descendente (lo más reciente
     * primero). Admite un término de búsqueda opcional por nombre de producto o
     * usuario.
     *
     * @param search   término de búsqueda opcional (null/vacío devuelve todo)
     * @param pageable configuración de paginación provista por Spring Web
     * @return página de movimientos mapeada a DTOs de respuesta
     */
    @Transactional(readOnly = true)
    public Page<StockMovementResponseDTO> getMovements(String search, @NonNull Pageable pageable) {
        Pageable effective = pageable.getSort().isSorted()
                ? pageable
                : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                        Sort.by(Sort.Direction.DESC, "date"));

        Page<StockMovementEntity> movements;
        if (search == null || search.isBlank()) {
            movements = stockMovementRepository.findAll(effective);
        } else {
            movements = stockMovementRepository.searchByProductNameOrUsername(search.trim(), effective);
        }

        // Resolvemos los nombres de producto en una sola consulta para evitar N+1.
        Map<UUID, String> productNames = resolveProductNames(movements.getContent());
        return movements.map(m -> mapToResponseDTO(m, productNames.get(m.getProductId())));
    }

    /**
     * Registra una entrada (IN) o salida (OUT) de stock para un producto existente.
     * Calcula la cantidad anterior desde el ledger y la cantidad nueva resultante,
     * rechazando cualquier salida que dejaría el stock en negativo.
     *
     * @param request datos del movimiento a registrar
     * @return el movimiento registrado como DTO de respuesta
     */
    @Transactional
    public StockMovementResponseDTO registerMovement(StockMovementRequestDTO request) {
        ProductEntity product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "El producto no fue encontrado con el ID proporcionado: " + request.getProductId()));

        int previousQuantity = stockMovementRepository.sumSignedQuantityByProductId(product.getId());
        int newQuantity = request.getMovementType() == MovementType.IN
                ? previousQuantity + request.getQuantity()
                : previousQuantity - request.getQuantity();

        // Regla de negocio: una salida no puede dejar el inventario en negativo.
        if (newQuantity < 0) {
            throw new IllegalArgumentException(
                    "La salida solicitada (" + request.getQuantity() + ") supera el stock disponible ("
                            + previousQuantity + ").");
        }

        StockMovementEntity movement = new StockMovementEntity();
        movement.setId(UUID.randomUUID());
        movement.setProductId(product.getId());
        movement.setMovementType(request.getMovementType().name());
        movement.setQuantity(request.getQuantity());
        movement.setPreviousQuantity(previousQuantity);
        movement.setNewQuantity(newQuantity);
        movement.setDate(LocalDateTime.now());
        movement.setUsername(currentUsername());
        movement.setObservations(request.getObservations());

        StockMovementEntity saved = stockMovementRepository.save(movement);
        return mapToResponseDTO(saved, product.getName());
    }

    /**
     * Extrae el nombre de usuario del token JWT inyectado en el SecurityContext.
     * Provee un valor por defecto si la operación ocurre fuera de un contexto seguro.
     */
    private String currentUsername() {
        String username = null;
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            username = SecurityContextHolder.getContext().getAuthentication().getName();
        }
        return username != null && !username.isEmpty() ? username : "sistema_interno";
    }

    /**
     * Obtiene un mapa productId -> nombre para el conjunto de movimientos de la página,
     * resolviendo todos los nombres en una única consulta.
     */
    private Map<UUID, String> resolveProductNames(List<StockMovementEntity> movements) {
        List<UUID> productIds = movements.stream()
                .map(StockMovementEntity::getProductId)
                .distinct()
                .collect(Collectors.toList());
        return productRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(ProductEntity::getId, ProductEntity::getName));
    }

    /**
     * Transforma una entidad de movimiento a su DTO de respuesta, incorporando el
     * nombre del producto resuelto por separado.
     */
    private StockMovementResponseDTO mapToResponseDTO(StockMovementEntity entity, String productName) {
        StockMovementResponseDTO dto = new StockMovementResponseDTO();
        dto.setId(entity.getId());
        dto.setProductId(entity.getProductId());
        dto.setProductName(productName);
        dto.setMovementType(entity.getMovementType());
        dto.setQuantity(entity.getQuantity());
        dto.setPreviousQuantity(entity.getPreviousQuantity());
        dto.setNewQuantity(entity.getNewQuantity());
        // El movimiento se guarda como LocalDateTime (hora del servidor); se expone con
        // el offset de zona para cumplir el formato date-time (RFC 3339) del contrato.
        dto.setDate(entity.getDate() != null
                ? entity.getDate().atZone(ZoneId.systemDefault()).toOffsetDateTime()
                : null);
        dto.setUsername(entity.getUsername());
        dto.setObservations(entity.getObservations());
        return dto;
    }
}
