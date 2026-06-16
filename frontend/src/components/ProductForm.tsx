import { useState, useEffect } from 'react';
import type { Product, ProductRequestDTO } from '../types/Product';
import api from '../api/axios';
import { X } from 'lucide-react';

interface ProductFormProps {
  product?: Product | null;
  onClose: () => void;
  onSave: () => void;
}

export const ProductForm: React.FC<ProductFormProps> = ({ product, onClose, onSave }) => {
  const [formData, setFormData] = useState<ProductRequestDTO>({
    name: '',
    skuCode: '',
    description: '',
    category: '',
    price: 0,
    initialQuantity: 0,
    minimumStock: 0,
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
      });
    }
  }, [product]);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
    const { name, value, type } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]: type === 'number' ? Number(value) : value,
    }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError(null);

    try {
      if (product) {
        await api.put(`/api/v1/products/${product.id}`, formData);
      } else {
        await api.post('/api/v1/products', formData);
      }
      onSave();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Error al guardar el producto');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4">
      <div className="bg-surface border border-border w-full max-w-2xl rounded-2xl shadow-2xl overflow-hidden flex flex-col max-h-[90vh]">
        <div className="flex justify-between items-center p-6 border-b border-border">
          <h2 className="text-xl font-bold text-gray-100">
            {product ? 'Editar Producto' : 'Crear Producto'}
          </h2>
          <button onClick={onClose} className="text-gray-400 hover:text-white transition-colors">
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
                  className="input-field min-h-[100px] resize-y"
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
                />
              </div>
              <div>
                <label className="label-text">Cantidad Inicial</label>
                <input
                  type="number"
                  name="initialQuantity"
                  value={formData.initialQuantity}
                  onChange={handleChange}
                  data-testid="product-initial-quantity"
                  required
                  min="0"
                  className="input-field"
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
                />
              </div>
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
