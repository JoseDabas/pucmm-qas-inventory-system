import { useEffect, useState } from 'react';
import type { Product } from '../types/Product';
import api from '../api/axios';
import { usePermissions } from '../auth/usePermissions';
import { ProductForm } from './ProductForm';
import { ConfirmDialog } from './ConfirmDialog';
import { Plus, Edit2, Trash2, Package, Search, ArrowUp, ArrowDown} from 'lucide-react';

export const ProductList: React.FC = () => {
  const [products, setProducts] = useState<Product[]>([]);
  const [error, setError] = useState<string | null>(null);
  const { hasPermission } = usePermissions();

  // Búsqueda y paginación (page en base 0, igual que Spring Data)
  const [searchTerm, setSearchTerm] = useState('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const PAGE_SIZE = 10;

  // Ordenamiento server-side (usa el parámetro sort de Spring Data: "campo,dir")
  const [sortBy, setSortBy] = useState<'name' | 'price' | null>(null);
  const [sortDir, setSortDir] = useState<'asc' | 'desc'>('asc');

  // Alterna la dirección si ya se ordena por ese campo; si no, ordena asc por él.
  const handleSort = (field: 'name' | 'price') => {
    if (sortBy === field) {
      setSortDir((d) => (d === 'asc' ? 'desc' : 'asc'));
    } else {
      setSortBy(field);
      setSortDir('asc');
    }
    setPage(0);
  };

  // Muestra la flecha activa (asc/desc) o un icono neutro si la columna no ordena.
  const renderSortIcon = (field: 'name' | 'price') => {
    if (sortBy !== field) return <ArrowUp size={14} />;
    return sortDir === 'asc' ? <ArrowUp size={14} /> : <ArrowDown size={14} />;
  };

  const canManage = hasPermission('product:manage');

  // Form State
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [selectedProduct, setSelectedProduct] = useState<Product | null>(null);

  // Producto pendiente de confirmación de borrado (null = diálogo cerrado)
  const [productToDelete, setProductToDelete] = useState<Product | null>(null);

  const fetchProducts = async () => {
    try {
      setError(null);
      const response = await api.get('/api/v1/products', {
        params: {
          page,
          size: PAGE_SIZE,
          search: searchTerm || undefined,
          sort: sortBy ? `${sortBy},${sortDir}` : undefined,
        },
      });
      setProducts(response.data.content || []);
      setTotalPages(response.data.totalPages ?? 0);
    } catch (err: unknown) {
      setError((err as { response?: { data?: { message?: string } } }).response?.data?.message || 'Error al cargar los productos');
    }
  };

  // Recarga al cambiar de página o el término de búsqueda. El debounce evita
  // pegarle al API en cada tecla mientras se escribe la búsqueda.
  useEffect(() => {
    const timer = setTimeout(fetchProducts, 300);
    return () => clearTimeout(timer);
  }, [fetchProducts, searchTerm, sortBy, sortDir]);

  const handleCreate = () => {
    setSelectedProduct(null);
    setIsFormOpen(true);
  };

  const handleEdit = (product: Product) => {
    setSelectedProduct(product);
    setIsFormOpen(true);
  };

  const confirmDelete = async () => {
    if (!productToDelete) return;
    try {
      await api.delete(`/api/v1/products/${productToDelete.id}`);
      setProductToDelete(null);
      fetchProducts();
    } catch (err: unknown) {
      alert((err as { response?: { data?: { message?: string } } }).response?.data?.message || 'Error al eliminar el producto');
      setProductToDelete(null);
    }
  };

  const handleFormSave = () => {
    setIsFormOpen(false);
    fetchProducts();
  };

  return (
    <div className="max-w-7xl mx-auto p-6">
      <div className="flex justify-between items-center mb-8">
        <div>
          <h1 className="text-2xl font-semibold text-gray-800 flex items-center gap-3">
            <Package className="text-primary-600" size={28} />
            Inventario
          </h1>
          <p className="text-gray-500 mt-1">Gestiona los productos de tu empresa</p>
        </div>
        {canManage && (
          <button
            onClick={handleCreate}
            data-testid="create-product-button"
            className="btn-primary flex items-center gap-2"
          >
            <Plus size={20} />
            <span>Nuevo Producto</span>
          </button>
        )}
      </div>

      {error && (
        <div className="bg-red-500/10 border border-red-500/50 text-red-400 p-4 rounded-lg mb-6">
          {error}
        </div>
      )}

      <div className="relative mb-4">
        <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" size={18} />
        <input
          type="text"
          value={searchTerm}
          onChange={(e) => { setSearchTerm(e.target.value); setPage(0); }}
          placeholder="Buscar por SKU o nombre..."
          data-testid="product-search-input"
          className="w-full max-w-sm pl-10 pr-4 py-2 border border-border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
        />
      </div>

      <div className="bg-surface border border-border rounded-xl overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full table-fixed text-left border-collapse" data-testid="products-table">
            <thead>
              <tr className="bg-surface-hover border-b border-border text-gray-500 text-sm uppercase tracking-wider">
                <th className="p-4 font-semibold">SKU</th>
                <th className="p-4 font-semibold">
                  <button
                    onClick={() => handleSort('name')}
                    data-testid="sort-name"
                    className="flex items-center gap-1 uppercase tracking-wider hover:text-gray-700 transition-colors"
                  >
                    Nombre
                    {renderSortIcon('name')}
                  </button>
                </th>
                <th className="p-4 font-semibold">Descripción</th>
                <th className="p-4 font-semibold">Categoría</th>
                <th className="p-4 font-semibold">
                  <button
                    onClick={() => handleSort('price')}
                    data-testid="sort-price"
                    className="flex items-center gap-1 uppercase tracking-wider hover:text-gray-700 transition-colors"
                  >
                    Precio
                    {renderSortIcon('price')}
                  </button>
                </th>
                <th className="p-4 font-semibold">Stock Inicial</th>
                <th className="p-4 font-semibold">Stock Mínimo</th>
                <th className="p-4 font-semibold">Stock Actual</th>
                <th className="p-4 font-semibold">Estado</th>
                <th className="p-4 font-semibold text-center">Acciones</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-border">
              {products.length === 0 ? (
                <tr>
                  <td colSpan={10} className="p-8 text-center text-gray-400">
                    {searchTerm ? 'No se encontraron productos.' : 'No hay productos registrados.'}
                  </td>
                </tr>
              ) : (
                products.map((product) => (
                  <tr
                    key={product.id}
                    data-testid="product-row"
                  >
                    <td className="p-4 text-gray-600 text-sm">{product.skuCode}</td>
                    <td className="p-4 text-gray-900 font-medium">{product.name}</td>
                    <td className="p-4 text-gray-600">
                      <div className="truncate" title={product.description}>
                        {product.description || 'N/A'}
                      </div>
                    </td>
                    <td className="p-4">
                      <span className="bg-accent-500/10 text-accent-500 px-2.5 py-1 rounded-full text-xs font-medium">
                        {product.category || 'N/A'}
                      </span>
                    </td>
                    <td className="p-4 text-gray-900 font-semibold">
                      ${product.price.toFixed(2)}
                    </td>
                    <td className="p-4 text-gray-600">{product.initialQuantity}</td>
                    <td className="p-4 text-gray-600">{product.minimumStock}</td>
                    <td className="p-4">
                      <span
                        className={
                          product.stockActual <= product.minimumStock
                            ? 'font-semibold text-red-600'
                            : 'font-semibold text-gray-900'
                        }
                        title={
                          product.stockActual <= product.minimumStock
                            ? 'Stock en nivel crítico (menor o igual al stock mínimo)'
                            : undefined
                        }
                      >
                        {product.stockActual}
                      </span>
                    </td>
                    <td className="p-4">
                      <span
                        className={
                          product.isActive
                            ? 'bg-primary-50 text-primary-700 px-2.5 py-1 rounded-full text-xs font-medium'
                            : 'bg-gray-100 text-gray-500 px-2.5 py-1 rounded-full text-xs font-medium'
                        }
                      >
                        {product.isActive ? 'Activo' : 'Inactivo'}
                      </span>
                    </td>
                    <td className="p-4">
                      {canManage && (
                        <div className="flex justify-center space-x-2">
                          <button
                            onClick={() => handleEdit(product)}
                            data-testid="edit-product-button"
                            className="p-2 text-gray-500 hover:text-gray-900 hover:bg-surface-hover rounded-lg transition-colors"
                            title="Editar"
                          >
                            <Edit2 size={18} />
                          </button>
                          <button
                            onClick={() => setProductToDelete(product)}
                            data-testid="delete-product-button"
                            className="p-2 text-gray-500 hover:text-red-600 hover:bg-red-50 rounded-lg transition-colors"
                            title="Eliminar"
                          >
                            <Trash2 size={18} />
                          </button>
                        </div>
                      )}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      
      {totalPages >= 0 && (
        <div className="flex items-center justify-between mt-4">
          <span className="text-sm text-gray-500">Página {page + 1} de {totalPages}</span>
          <div className="flex gap-2">
            <button
              onClick={() => setPage((p) => Math.max(0, p - 1))}
              disabled={page === 0}
              data-testid="prev-page-button"
              className="px-3 py-1.5 border border-border rounded-lg disabled:opacity-50 hover:bg-surface-hover"
            >
              Anterior
            </button>
            <button
              onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
              disabled={page >= totalPages - 1}
              data-testid="next-page-button"
              className="px-3 py-1.5 border border-border rounded-lg disabled:opacity-50 hover:bg-surface-hover"
            >
              Siguiente
            </button>
          </div>
        </div>
      )}

      {isFormOpen && (
        <ProductForm
          product={selectedProduct}
          onClose={() => setIsFormOpen(false)}
          onSave={handleFormSave}
        />
      )}

      {productToDelete && (
        <ConfirmDialog
          title="Eliminar producto"
          message={`¿Estás seguro de eliminar "${productToDelete.name}"? Esta acción no se puede deshacer.`}
          confirmLabel="Eliminar"
          onConfirm={confirmDelete}
          onCancel={() => setProductToDelete(null)}
        />
      )}
    </div>
  );
};
