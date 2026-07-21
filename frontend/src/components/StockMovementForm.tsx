import { useState, useEffect } from 'react';
import type { Product } from '../types/Product';
import type { MovementType, StockMovementRequestDTO } from '../types/StockMovement';
import api from '../api/axios';
import { X } from 'lucide-react';

interface StockMovementFormProps {
  onClose: () => void;
  onSave: () => void;
}

// Estado del formulario: la cantidad permite '' para arrancar vacía (en vez de un 0).
interface MovementFormState {
  productId: string;
  movementType: MovementType;
  quantity: number | '';
  observations: string;
}

export const StockMovementForm: React.FC<StockMovementFormProps> = ({ onClose, onSave }) => {
  const [formData, setFormData] = useState<MovementFormState>({
    productId: '',
    movementType: 'IN',
    quantity: '',
    observations: '',
  });

  const [products, setProducts] = useState<Product[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Cargamos los productos para el selector. Traemos una página amplia porque el
  // selector no pagina; suficiente para el catálogo del proyecto.
  useEffect(() => {
    api
      .get('/api/v1/products', { params: { page: 0, size: 100, sort: 'name,asc' } })
      .then((res) => setProducts(res.data.content || []))
      .catch(() => setError('No se pudieron cargar los productos.'));
  }, []);

  const handleChange = (
    e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>
  ) => {
    const { name, value, type } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]: type === 'number' ? (value === '' ? '' : Number(value)) : value,
    }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError(null);

    try {
      const payload: StockMovementRequestDTO = {
        productId: formData.productId,
        movementType: formData.movementType,
        quantity: Number(formData.quantity),
        observations: formData.observations || undefined,
      };
      await api.post('/api/v1/stock-movements', payload);
      onSave();
    } catch (err: unknown) {
      // ProblemDetail (RFC 7807) trae el texto en 'detail'; dejamos 'message' como respaldo.
      setError(
        (err as { response?: { data?: { detail?: string; message?: string } } }).response?.data?.detail ||
          (err as { response?: { data?: { message?: string } } }).response?.data?.message ||
          'Error al registrar el movimiento'
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4">
      <div className="bg-surface border border-border w-full max-w-2xl rounded-2xl shadow-xl overflow-hidden flex flex-col max-h-[90vh]">
        <div className="flex justify-between items-center p-6 border-b border-border">
          <h2 className="text-xl font-semibold text-gray-800">Registrar Movimiento</h2>
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

          <form id="movement-form" onSubmit={handleSubmit} className="space-y-4">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div className="md:col-span-2">
                <label className="label-text">Producto</label>
                <select
                  name="productId"
                  value={formData.productId}
                  onChange={handleChange}
                  data-testid="movement-product"
                  required
                  className="input-field"
                >
                  <option value="" disabled>
                    Selecciona un producto...
                  </option>
                  {products.map((p) => (
                    <option key={p.id} value={p.id}>
                      {p.name} ({p.skuCode})
                    </option>
                  ))}
                </select>
              </div>
              <div>
                <label className="label-text">Tipo de movimiento</label>
                <select
                  name="movementType"
                  value={formData.movementType}
                  onChange={handleChange}
                  data-testid="movement-type"
                  className="input-field"
                >
                  <option value="IN">Entrada (IN)</option>
                  <option value="OUT">Salida (OUT)</option>
                </select>
              </div>
              <div>
                <label className="label-text">Cantidad</label>
                <input
                  type="number"
                  name="quantity"
                  value={formData.quantity}
                  onChange={handleChange}
                  data-testid="movement-quantity"
                  required
                  min="1"
                  className="input-field"
                  placeholder="0"
                />
              </div>
              <div className="md:col-span-2">
                <label className="label-text">Observaciones</label>
                <textarea
                  name="observations"
                  value={formData.observations}
                  onChange={handleChange}
                  data-testid="movement-observations"
                  className="input-field min-h-25 resize-y"
                  placeholder="Motivo o detalle del movimiento..."
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
            form="movement-form"
            disabled={loading}
            data-testid="movement-submit"
            className="btn-primary flex items-center"
          >
            {loading ? 'Guardando...' : 'Registrar Movimiento'}
          </button>
        </div>
      </div>
    </div>
  );
};
