package edu.pucmm.cs.inventory.application;

import edu.pucmm.cs.inventory.domain.Category;
import edu.pucmm.cs.inventory.domain.MovementType;
import edu.pucmm.cs.inventory.infrastructure.persistence.entity.ProductEntity;
import edu.pucmm.cs.inventory.infrastructure.persistence.entity.StockMovementEntity;
import edu.pucmm.cs.inventory.infrastructure.persistence.repository.CategoryJpaRepository;
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
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Servicio de Aplicación (Caso de Uso) para la gestión de Productos.
 * 
 * Implementa la lógica de orquestación requerida para manipular el catálogo de
 * productos.
 * Actúa como intermediario entre los controladores REST (Capa de Presentación)
 * y
 * los repositorios Spring Data JPA (Capa de Infraestructura).
 */
@Service
public class ProductService {

    private final ProductJpaRepository productRepository;
    private final StockMovementJpaRepository stockMovementRepository;
    private final CategoryJpaRepository categoryRepository;

    /**
     * Inyección de dependencias recomendada vía constructor.
     *
     * @param productRepository       Repositorio para la entidad ProductEntity
     * @param stockMovementRepository Repositorio para la entidad
     *                                StockMovementEntity
     * @param categoryRepository      Repositorio para la entidad Category
     */
    public ProductService(ProductJpaRepository productRepository,
            StockMovementJpaRepository stockMovementRepository,
            CategoryJpaRepository categoryRepository) {
        this.productRepository = productRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.categoryRepository = categoryRepository;
    }

    /**
     * Crea un nuevo producto y registra su stock inicial de forma atómica.
     * 
     * @param request Datos de entrada capturados vía API REST.
     * @return El DTO de salida con los datos persistidos y el UUID generado.
     */
    @Transactional // Inicia una transacción de base de datos para asegurar consistencia e
                   // integridad (ACID)
    public ProductResponseDTO createProduct(ProductRequestDTO request) {
        // 1. Mapeo explícito de DTO a Entidad JPA
        ProductEntity entity = new ProductEntity();
        entity.setId(UUID.randomUUID());
        entity.setName(request.getName());
        entity.setSkuCode(request.getSkuCode());
        entity.setDescription(request.getDescription());
        entity.setCategory(resolveCategory(request.getCategory()));
        entity.setPrice(request.getPrice());
        entity.setInitialQuantity(request.getInitialQuantity());
        entity.setMinimumStock(request.getMinimumStock());
        entity.setIsActive(true); // Se fuerza a activo por defecto en la creación

        // Guardamos el producto en la base de datos
        ProductEntity savedProduct = productRepository.save(entity);

        // 2. Registro de evento de dominio: Movimiento inicial
        // Si el producto se registra con una cantidad mayor a cero, disparamos el
        // historial
        if (request.getInitialQuantity() != null && request.getInitialQuantity() > 0) {
            registerStockMovement(savedProduct.getId(), request.getInitialQuantity(), MovementType.IN,
                    "Registro inicial del producto");
        }

        // 3. Devolvemos la representación segura
        return mapToResponseDTO(savedProduct);
    }

    /**
     * Actualiza la información descriptiva y de configuración de un producto
     * existente.
     * 
     * @param id      Identificador único del producto.
     * @param request Datos a modificar.
     * @return El producto modificado.
     */
    @Transactional
    public ProductResponseDTO updateProduct(@NonNull UUID id, ProductRequestDTO request) {
        ProductEntity entity = productRepository.findById(id)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException(
                        "El producto no fue encontrado con el ID proporcionado: " + id));

        entity.setName(request.getName());
        entity.setSkuCode(request.getSkuCode());
        entity.setDescription(request.getDescription());
        entity.setCategory(resolveCategory(request.getCategory()));
        entity.setPrice(request.getPrice());
        entity.setMinimumStock(request.getMinimumStock());

        ProductEntity updatedProduct = productRepository.save(entity);
        return mapToResponseDTO(updatedProduct);
    }

    /**
     * Consulta de productos con stock crítico
     * Delega el cálculo de stock al motor de base de datos para evitar condiciones de carrera.
     * 
     * @return Lista de productos en estado crítico.
     */
    @Transactional(readOnly = true)
    public List<ProductResponseDTO> getCriticalStockAlerts() {
        return productRepository.findProductsWithCriticalStock().stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Ejecuta una consulta paginada de todos los productos.
     * 
     * @param pageable Configuración de paginación provista por Spring Web.
     * @return Página de resultados estructurada en DTOs.
     */
    @Transactional(readOnly = true) // Optimiza la transacción marcándola como de solo lectura (evita flush
                                    // innecesario)
    public Page<ProductResponseDTO> getProducts(@NonNull Pageable pageable) {
        Page<ProductEntity> productEntities = productRepository.findAll(pageable);
        // Mapea internamente la Page de entidades a una Page de DTOs utilizando el
        // método helper
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
            throw new jakarta.persistence.EntityNotFoundException("Operación denegada. El producto especificado no existe.");
        }
        // Eliminar primero los movimientos de stock para evitar violaciones de clave foránea
        stockMovementRepository.deleteByProductId(id);
        
        productRepository.deleteById(id);
    }

    /**
     * Método auxiliar (helper) para persistir un movimiento de inventario en el
     * historial (Auditoría operativa).
     */
    private void registerStockMovement(UUID productId, Integer quantity, MovementType type, String observations) {
        // Extraemos el nombre de usuario directamente del token JWT inyectado en el
        // SecurityContext
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
        // Proveemos un nombre por defecto en caso de operaciones fuera de un contexto
        // seguro (ej. tareas asíncronas internas)
        movement.setUsername(username != null && !username.isEmpty() ? username : "sistema_interno");
        movement.setObservations(observations);

        stockMovementRepository.save(movement);
    }

    /**
     * Resuelve la categoría a partir de su nombre. Si la categoría no existe aún,
     * la crea de forma transparente (patrón "find-or-create"), garantizando la
     * integridad referencial con la tabla 'categories'.
     *
     * @param categoryName nombre de la categoría; puede ser nulo o vacío.
     * @return la entidad Category persistida, o null si no se proporcionó nombre.
     */
    private Category resolveCategory(String categoryName) {
        if (categoryName == null || categoryName.trim().isEmpty()) {
            return null;
        }
        String normalized = categoryName.trim();
        return categoryRepository.findByName(normalized)
                .orElseGet(() -> categoryRepository.save(new Category(UUID.randomUUID(), normalized, null)));
    }

    /**
     * Transforma manualmente una Entidad (Capa de Infraestructura) a DTO (Capa de
     * Presentación).
     * Aisla los cambios de modelo de base de datos respecto a los consumidores de
     * la API.
     */
    private ProductResponseDTO mapToResponseDTO(ProductEntity entity) {
        ProductResponseDTO dto = new ProductResponseDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setSkuCode(entity.getSkuCode());
        dto.setDescription(entity.getDescription());
        dto.setCategory(entity.getCategory() != null ? entity.getCategory().getName() : null);
        dto.setPrice(entity.getPrice());
        dto.setInitialQuantity(entity.getInitialQuantity());
        dto.setMinimumStock(entity.getMinimumStock());
        dto.setIsActive(entity.getIsActive());
        return dto;
    }
}
