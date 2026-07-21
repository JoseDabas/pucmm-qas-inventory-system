package edu.pucmm.cs.inventory.application;

import edu.pucmm.cs.inventory.domain.Category;
import edu.pucmm.cs.inventory.domain.MovementType;
import edu.pucmm.cs.inventory.infrastructure.persistence.entity.ProductEntity;
import edu.pucmm.cs.inventory.infrastructure.persistence.entity.StockMovementEntity;
import edu.pucmm.cs.inventory.infrastructure.persistence.repository.CategoryJpaRepository;
import edu.pucmm.cs.inventory.infrastructure.persistence.repository.ProductJpaRepository;
import edu.pucmm.cs.inventory.infrastructure.persistence.repository.ProductStockView;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;
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
        // Activo por defecto si no se especifica; permite crear inactivo si el cliente lo indica
        entity.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);

        // Guardamos el producto en la base de datos
        ProductEntity savedProduct = productRepository.save(entity);

        // 2. Registro de evento de dominio: Movimiento inicial
        // Si el producto se registra con una cantidad mayor a cero, disparamos el
        // historial
        if (request.getInitialQuantity() != null && request.getInitialQuantity() > 0) {
            registerStockMovement(savedProduct.getId(), request.getInitialQuantity(), MovementType.IN,
                    "Registro inicial del producto");
        }

        // 3. Devolvemos la representación segura. Recién creado, el stock actual equivale
        // a la cantidad inicial (único movimiento existente en el ledger).
        int initialStock = savedProduct.getInitialQuantity() != null ? savedProduct.getInitialQuantity() : 0;
        return mapToResponseDTO(savedProduct, initialStock);
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
        // Permite alternar el estado activo/inactivo desde la edición
        if (request.getIsActive() != null) {
            entity.setIsActive(request.getIsActive());
        }

        ProductEntity updatedProduct = productRepository.save(entity);
        // La edición no altera el stock; se calcula desde el ledger para la respuesta.
        return mapToResponseDTO(updatedProduct, currentStock(id));
    }

    /**
     * Consulta de productos con stock crítico
     * Delega el cálculo de stock al motor de base de datos para evitar condiciones de carrera.
     * 
     * @return Lista de productos en estado crítico.
     */
    @Transactional(readOnly = true)
    public List<ProductResponseDTO> getCriticalStockAlerts() {
        List<ProductEntity> criticalProducts = productRepository.findProductsWithCriticalStock();
        Map<UUID, Integer> stocks = stockMap(criticalProducts);
        return criticalProducts.stream()
                .map(entity -> mapToResponseDTO(entity, stocks.getOrDefault(entity.getId(), 0)))
                .collect(Collectors.toList());
    }

    /**
     * Ejecuta una consulta paginada de productos, opcionalmente filtrada por un
     * término de búsqueda que se compara contra el nombre o el código SKU.
     *
     * @param search   Término de búsqueda opcional (null/vacío devuelve todo).
     * @param pageable Configuración de paginación provista por Spring Web.
     * @return Página de resultados estructurada en DTOs.
     */
    @Transactional(readOnly = true) // Optimiza la transacción marcándola como de solo lectura (evita flush
                                    // innecesario)
    public Page<ProductResponseDTO> getProducts(String search, @NonNull Pageable pageable) {
        Page<ProductEntity> productEntities;
        if (search == null || search.isBlank()) {
            productEntities = productRepository.findAll(pageable);
        } else {
            // Búsqueda en toda la base de datos por nombre o SKU (case-insensitive),
            // respetando la paginación provista por Spring Web.
            String term = search.trim();
            productEntities = productRepository
                    .findByNameContainingIgnoreCaseOrSkuCodeContainingIgnoreCase(term, term, pageable);
        }
        // Calcula el stock actual de todos los productos de la página en una sola consulta
        // (evita N+1) y mapea cada entidad a DTO con su stock correspondiente.
        Map<UUID, Integer> stocks = stockMap(productEntities.getContent());
        return productEntities.map(entity -> mapToResponseDTO(entity, stocks.getOrDefault(entity.getId(), 0)));
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
        // Movimiento inicial del producto: el stock pasa de 0 a la cantidad registrada.
        movement.setPreviousQuantity(0);
        movement.setNewQuantity(quantity);
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
    private ProductResponseDTO mapToResponseDTO(ProductEntity entity, int stockActual) {
        ProductResponseDTO dto = new ProductResponseDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setSkuCode(entity.getSkuCode());
        dto.setDescription(entity.getDescription());
        dto.setCategory(entity.getCategory() != null ? entity.getCategory().getName() : null);
        dto.setPrice(entity.getPrice());
        dto.setInitialQuantity(entity.getInitialQuantity());
        dto.setMinimumStock(entity.getMinimumStock());
        dto.setStockActual(stockActual);
        dto.setIsActive(entity.getIsActive());
        return dto;
    }

    /**
     * Calcula el stock actual de un único producto desde el ledger de movimientos,
     * devolviendo 0 si aún no tiene movimientos registrados.
     */
    private int currentStock(UUID productId) {
        Integer sum = stockMovementRepository.sumSignedQuantityByProductId(productId);
        return sum != null ? sum : 0;
    }

    /**
     * Calcula el stock actual de un conjunto de productos en una sola consulta y lo
     * devuelve como un mapa productId -> stock. Los productos sin movimientos quedan
     * fuera del mapa (se resuelven como 0 al consultar con getOrDefault).
     */
    private Map<UUID, Integer> stockMap(List<ProductEntity> products) {
        List<UUID> productIds = products.stream()
                .map(ProductEntity::getId)
                .collect(Collectors.toList());
        if (productIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return stockMovementRepository.sumSignedQuantitiesByProductIds(productIds).stream()
                .collect(Collectors.toMap(ProductStockView::getProductId,
                        view -> view.getTotal() != null ? view.getTotal().intValue() : 0));
    }
}
