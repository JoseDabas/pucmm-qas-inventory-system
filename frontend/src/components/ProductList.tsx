import { useEffect, useState } from 'react';
import { useAuth } from 'react-oidc-context';
import type { Product } from '../types/Product';
import api from '../api/axios';
import { ProductForm } from './ProductForm';
import { Plus, Edit2, Trash2, Package } from 'lucide-react';

export const ProductList: React.FC = () => {
  const [products, setProducts] = useState<Product[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const auth = useAuth();

  // Función auxiliar para leer los roles desde el access_token
  const canManageProducts = () => {
    if (!auth.user?.access_token) return false;
    try {
      const payload = JSON.parse(atob(auth.user.access_token.split('.')[1]));
      const roles = payload.realm_access?.roles || [];
      return roles.includes('product:manage');
    } catch (e) {
      return false;
    }
  };

  const canManage = canManageProducts();

  // Form State
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [selectedProduct, setSelectedProduct] = useState<Product | null>(null);

  const fetchProducts = async () => {
    try {
      setLoading(true);
      setError(null);
      const response = await api.get('/api/v1/products');
      setProducts(response.data.content || []);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Error al cargar los productos');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchProducts();
  }, []);

  const handleCreate = () => {
    setSelectedProduct(null);
    setIsFormOpen(true);
  };

  const handleEdit = (product: Product) => {
    setSelectedProduct(product);
    setIsFormOpen(true);
  };

  const handleDelete = async (id: string) => {
    if (window.confirm('¿Estás seguro de eliminar este producto?')) {
      try {
        await api.delete(`/api/v1/products/${id}`);
        fetchProducts();
      } catch (err: any) {
        alert(err.response?.data?.message || 'Error al eliminar el producto');
      }
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
          <h1 className="text-3xl font-bold text-white flex items-center gap-3">
            <Package className="text-primary-500" size={32} />
            Inventario
          </h1>
          <p className="text-gray-400 mt-1">Gestiona los productos de tu empresa</p>
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

      <div className="bg-surface border border-border rounded-xl shadow-xl overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse" data-testid="products-table">
            <thead>
              <tr className="bg-surface-hover border-b border-border text-gray-300 text-sm uppercase tracking-wider">
                <th className="p-4 font-semibold">SKU</th>
                <th className="p-4 font-semibold">Nombre</th>
                <th className="p-4 font-semibold">Categoría</th>
                <th className="p-4 font-semibold">Precio</th>
                <th className="p-4 font-semibold">Stock Inicial</th>
                <th className="p-4 font-semibold text-center">Acciones</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-border">
              {loading ? (
                <tr>
                  <td colSpan={6} className="p-8 text-center text-gray-400">
                    <div className="flex justify-center items-center space-x-2">
                      <div className="w-4 h-4 bg-primary-500 rounded-full animate-pulse" />
                      <div className="w-4 h-4 bg-primary-500 rounded-full animate-pulse delay-75" />
                      <div className="w-4 h-4 bg-primary-500 rounded-full animate-pulse delay-150" />
                    </div>
                  </td>
                </tr>
              ) : products.length === 0 ? (
                <tr>
                  <td colSpan={6} className="p-8 text-center text-gray-400">
                    No hay productos registrados.
                  </td>
                </tr>
              ) : (
                products.map((product) => (
                  <tr
                    key={product.id}
                    data-testid="product-row"
                    className="hover:bg-surface-hover/50 transition-colors group"
                  >
                    <td className="p-4 text-gray-300 font-mono text-sm">{product.skuCode}</td>
                    <td className="p-4 text-white font-medium">{product.name}</td>
                    <td className="p-4">
                      <span className="bg-accent-500/20 text-accent-500 px-2.5 py-1 rounded-full text-xs font-medium">
                        {product.category || 'N/A'}
                      </span>
                    </td>
                    <td className="p-4 text-primary-400 font-medium">
                      ${product.price.toFixed(2)}
                    </td>
                    <td className="p-4 text-gray-300">{product.initialQuantity}</td>
                    <td className="p-4">
                      {canManage && (
                        <div className="flex justify-center space-x-2 opacity-0 group-hover:opacity-100 transition-opacity">
                          <button
                            onClick={() => handleEdit(product)}
                            data-testid="edit-product-button"
                            className="p-2 text-gray-400 hover:text-white hover:bg-gray-700 rounded-lg transition-colors"
                            title="Editar"
                          >
                            <Edit2 size={18} />
                          </button>
                          <button
                            onClick={() => handleDelete(product.id)}
                            data-testid="delete-product-button"
                            className="p-2 text-gray-400 hover:text-red-500 hover:bg-red-500/10 rounded-lg transition-colors"
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

      {isFormOpen && (
        <ProductForm
          product={selectedProduct}
          onClose={() => setIsFormOpen(false)}
          onSave={handleFormSave}
        />
      )}
    </div>
  );
};
