import { useState, useEffect } from 'react';
import type { Product, ProductRequestDTO } from '../types/Product';
import api from '../api/axios';
import { X } from 'lucide-react';

interface ProductFormProps {
  product?: Product | null;
  onClose: () => void;
  onSave: () => void;
}

// Estado del formulario: los campos numéricos permiten '' para que arranquen vacíos
// (en vez de mostrar un 0). Se convierten a número al enviar.
type ProductFormState = Omit<ProductRequestDTO, 'price' | 'initialQuantity' | 'minimumStock'> & {
  price: number | '';
  initialQuantity: number | '';
  minimumStock: number | '';
};

export const ProductForm: React.FC<ProductFormProps> = ({ product, onClose, onSave }) => {
  const [formData, setFormData] = useState<ProductFormState>({
    name: '',
    skuCode: '',
    description: '',
    category: '',
    price: '',
    initialQuantity: '',
    minimumStock: '',
    isActive: true,
  });

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (product) {
      setFormData({
        name: product.name,
        skuCode: product.skuCode,
        description: product.description || '',
        category: product.category || '',
        price: product.price,
        initialQuantity: product.initialQuantity,
        minimumStock: product.minimumStock,
        isActive: product.isActive,
      });
    }
  }, [product]);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
    const { name, value, type } = e.target;
    setFormData((prev) => ({
      ...prev,
      // Si el campo numérico está vacío lo dejamos como '' (input vacío); si no, lo convertimos a número
      [name]: type === 'number' ? (value === '' ? '' : Number(value)) : value,
    }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError(null);

    try {
      // Convertimos los campos numéricos ('' → 0) para cumplir con el DTO del backend
      const payload: ProductRequestDTO = {
        ...formData,
        price: Number(formData.price),
        initialQuantity: Number(formData.initialQuantity),
        minimumStock: Number(formData.minimumStock),
      };
      if (product) {
        await api.put(`/api/v1/products/${product.id}`, payload);
      } else {
        await api.post('/api/v1/products', payload);
      }
      onSave();
    } catch (err: unknown) {
      // 409 = conflicto de unicidad en BD: el SKU ya existe
      if ((err as { response?: { status?: number } }).response?.status === 409) {
        setError(`El SKU "${formData.skuCode}" ya está registrado. Usa uno diferente.`);
      } else {
        // ProblemDetail (RFC 7807) trae el texto en 'detail'; dejamos 'message' como respaldo
        setError((err as { response?: { data?: { detail?: string; message?: string } } }).response?.data?.detail || (err as { response?: { data?: { message?: string } } }).response?.data?.message || 'Error al guardar el producto');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4">
      <div className="bg-surface border border-border w-full max-w-2xl rounded-2xl shadow-xl overflow-hidden flex flex-col max-h-[90vh]">
        <div className="flex justify-between items-center p-6 border-b border-border">
          <h2 className="text-xl font-semibold text-gray-800">
            {product ? 'Editar Producto' : 'Crear Producto'}
          </h2>
          <button onClick={onClose} className="text-gray-400 hover:text-gray-800 transition-colors">
            <X size={24} />
          </button>
        </div>
        
        <div className="p-6 overflow-y-auto custom-scrollbar">
          {error && (
            <div className="mb-4 p-3 bg-red-500/10 border border-red-500/50 text-red-400 rounded-lg text-sm">
              {error}
            </div>
          )}
          
          <form id="product-form" onSubmit={handleSubmit} className="space-y-4">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label className="label-text">Nombre</label>
                <input
                  type="text"
                  name="name"
                  value={formData.name}
                  onChange={handleChange}
                  data-testid="product-name"
                  required
                  className="input-field"
                  placeholder="Ej: Laptop Dell XPS 15"
                />
              </div>
              <div>
                <label className="label-text">SKU</label>
                <input
                  type="text"
                  name="skuCode"
                  value={formData.skuCode}
                  onChange={handleChange}
                  data-testid="product-sku"
                  required
                  className="input-field"
                  placeholder="Ej: LAP-DELL-XPS15"
                />
              </div>
              <div className="md:col-span-2">
                <label className="label-text">Descripción</label>
                <textarea
                  name="description"
                  value={formData.description}
                  onChange={handleChange}
                  data-testid="product-description"
                  className="input-field min-h-25 resize-y"
                  placeholder="Descripción detallada del producto..."
                />
              </div>
              <div>
                <label className="label-text">Categoría</label>
                <input
                  type="text"
                  name="category"
                  value={formData.category}
                  onChange={handleChange}
                  data-testid="product-category"
                  className="input-field"
                  placeholder="Ej: Electrónica"
                />
              </div>
              <div>
                <label className="label-text">Precio</label>
                <input
                  type="number"
                  name="price"
                  value={formData.price}
                  onChange={handleChange}
                  data-testid="product-price"
                  required
                  min="0"
                  step="0.01"
                  className="input-field"
                  placeholder="0"
                />
              </div>
              <div>
                <label className="label-text">Stock Inicial</label>
                <input
                  type="number"
                  name="initialQuantity"
                  value={formData.initialQuantity}
                  onChange={handleChange}
                  data-testid="product-initial-quantity"
                  required
                  min="0"
                  className="input-field"
                  placeholder="0"
                  disabled={!!product} // La cantidad inicial solo se define en la creación
                />
              </div>
              <div>
                <label className="label-text">Stock Mínimo</label>
                <input
                  type="number"
                  name="minimumStock"
                  value={formData.minimumStock}
                  onChange={handleChange}
                  data-testid="product-minimum-stock"
                  required
                  min="0"
                  className="input-field"
                  placeholder="0"
                />
              </div>
              {product && (
                <div className="md:col-span-2">
                  <label className="label-text">Estado</label>
                  <div className="flex items-center gap-3">
                    <button
                      type="button"
                      role="switch"
                      aria-checked={formData.isActive}
                      onClick={() => setFormData((prev) => ({ ...prev, isActive: !prev.isActive }))}
                      data-testid="product-active-switch"
                      className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors ${
                        formData.isActive ? 'bg-primary-600' : 'bg-gray-300'
                      }`}
                    >
                      <span
                        className={`inline-block h-4 w-4 transform rounded-full bg-white transition-transform ${
                          formData.isActive ? 'translate-x-6' : 'translate-x-1'
                        }`}
                      />
                    </button>
                    <span className="text-sm text-gray-700">
                      {formData.isActive ? 'Activo' : 'Inactivo'}
                    </span>
                  </div>
                </div>
              )}
            </div>
          </form>
        </div>
        
        <div className="p-6 border-t border-border flex justify-end space-x-3 bg-surface-hover/30">
          <button type="button" onClick={onClose} className="btn-secondary">
            Cancelar
          </button>
          <button
            type="submit"
            form="product-form"
            disabled={loading}
            data-testid="product-submit"
            className="btn-primary flex items-center"
          >
            {loading ? 'Guardando...' : 'Guardar Producto'}
          </button>
        </div>
      </div>
    </div>
  );
};
