package edu.pucmm.cs.inventory.application;

import edu.pucmm.cs.inventory.domain.MovementType;
import edu.pucmm.cs.inventory.infrastructure.persistence.entity.ProductEntity;
import edu.pucmm.cs.inventory.infrastructure.persistence.entity.StockMovementEntity;
import edu.pucmm.cs.inventory.infrastructure.persistence.repository.ProductJpaRepository;
import edu.pucmm.cs.inventory.infrastructure.persistence.repository.StockMovementJpaRepository;
import edu.pucmm.cs.inventory.infrastructure.web.dto.ProductRequestDTO;
import edu.pucmm.cs.inventory.infrastructure.web.dto.ProductResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.lang.NonNull;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Servicio de Aplicación (Caso de Uso) para la gestión de Productos.
 * 
 * Implementa la lógica de orquestación requerida para manipular el catálogo de productos.
 * Actúa como intermediario entre los controladores REST (Capa de Presentación) y 
 * los repositorios Spring Data JPA (Capa de Infraestructura).
 */
@Service
public class ProductService {

    private final ProductJpaRepository productRepository;
    private final StockMovementJpaRepository stockMovementRepository;

    /**
     * Inyección de dependencias recomendada vía constructor.
     * 
     * @param productRepository Repositorio para la entidad ProductEntity
     * @param stockMovementRepository Repositorio para la entidad StockMovementEntity
     */
    public ProductService(ProductJpaRepository productRepository, 
                          StockMovementJpaRepository stockMovementRepository) {
        this.productRepository = productRepository;
        this.stockMovementRepository = stockMovementRepository;
    }

    /**
     * Crea un nuevo producto y registra su stock inicial de forma atómica.
     * 
     * @param request Datos de entrada capturados vía API REST.
     * @return El DTO de salida con los datos persistidos y el UUID generado.
     */
    @Transactional // Inicia una transacción de base de datos para asegurar consistencia e integridad (ACID)
    public ProductResponseDTO createProduct(ProductRequestDTO request) {
        // 1. Mapeo explícito de DTO a Entidad JPA
        ProductEntity entity = new ProductEntity();
        entity.setId(UUID.randomUUID());
        entity.setName(request.getName());
        entity.setSkuCode(request.getSkuCode());
        entity.setDescription(request.getDescription());
        entity.setCategory(request.getCategory());
        entity.setPrice(request.getPrice());
        entity.setInitialQuantity(request.getInitialQuantity());
        entity.setMinimumStock(request.getMinimumStock());
        entity.setIsActive(true); // Se fuerza a activo por defecto en la creación

        // Guardamos el producto en la base de datos
        ProductEntity savedProduct = productRepository.save(entity);

        // 2. Registro de evento de dominio: Movimiento inicial
        // Si el producto se registra con una cantidad mayor a cero, disparamos el historial
        if (request.getInitialQuantity() != null && request.getInitialQuantity() > 0) {
            registerStockMovement(savedProduct.getId(), request.getInitialQuantity(), MovementType.IN, "Registro inicial del producto");
        }

        // 3. Devolvemos la representación segura
        return mapToResponseDTO(savedProduct);
    }

    /**
     * Actualiza la información descriptiva y de configuración de un producto existente.
     * 
     * @param id Identificador único del producto.
     * @param request Datos a modificar.
     * @return El producto modificado.
     */
    @Transactional
    public ProductResponseDTO updateProduct(@NonNull UUID id, ProductRequestDTO request) {
        ProductEntity entity = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("El producto no fue encontrado con el ID proporcionado: " + id));

        entity.setName(request.getName());
        entity.setSkuCode(request.getSkuCode());
        entity.setDescription(request.getDescription());
        entity.setCategory(request.getCategory());
        entity.setPrice(request.getPrice());
        entity.setMinimumStock(request.getMinimumStock());

        ProductEntity updatedProduct = productRepository.save(entity);
        return mapToResponseDTO(updatedProduct);
    }

    /**
     * Ejecuta una consulta paginada de todos los productos.
     * 
     * @param pageable Configuración de paginación provista por Spring Web.
     * @return Página de resultados estructurada en DTOs.
     */
    @Transactional(readOnly = true) // Optimiza la transacción marcándola como de solo lectura (evita flush innecesario)
    public Page<ProductResponseDTO> getProducts(@NonNull Pageable pageable) {
        Page<ProductEntity> productEntities = productRepository.findAll(pageable);
        // Mapea internamente la Page de entidades a una Page de DTOs utilizando el método helper
        return productEntities.map(this::mapToResponseDTO);
    }

    /**
     * Elimina permanentemente un producto de la base de datos.
     * 
     * @param id Identificador único del producto.
     */
    @Transactional
    public void deleteProduct(@NonNull UUID id) {
        if (!productRepository.existsById(id)) {
            throw new IllegalArgumentException("Operación denegada. El producto especificado no existe.");
        }
        productRepository.deleteById(id);
    }

    /**
     * Método auxiliar (helper) para persistir un movimiento de inventario en el historial (Auditoría operativa).
     */
    private void registerStockMovement(UUID productId, Integer quantity, MovementType type, String observations) {
        // Extraemos el nombre de usuario directamente del token JWT inyectado en el SecurityContext
        String username = null;
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            username = SecurityContextHolder.getContext().getAuthentication().getName();
        }

        StockMovementEntity movement = new StockMovementEntity();
        movement.setId(UUID.randomUUID());
        movement.setProductId(productId);
        movement.setMovementType(type.name());
        movement.setQuantity(quantity);
        movement.setDate(LocalDateTime.now());
        // Proveemos un nombre por defecto en caso de operaciones fuera de un contexto seguro (ej. tareas asíncronas internas)
        movement.setUsername(username != null && !username.isEmpty() ? username : "sistema_interno");
        movement.setObservations(observations);

        stockMovementRepository.save(movement);
    }

    /**
     * Transforma manualmente una Entidad (Capa de Infraestructura) a DTO (Capa de Presentación).
     * Aisla los cambios de modelo de base de datos respecto a los consumidores de la API.
     */
    private ProductResponseDTO mapToResponseDTO(ProductEntity entity) {
        ProductResponseDTO dto = new ProductResponseDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setSkuCode(entity.getSkuCode());
        dto.setDescription(entity.getDescription());
        dto.setCategory(entity.getCategory());
        dto.setPrice(entity.getPrice());
        dto.setMinimumStock(entity.getMinimumStock());
        dto.setIsActive(entity.getIsActive());
        return dto;
    }
}
