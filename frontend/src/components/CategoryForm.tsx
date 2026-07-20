import { useState } from 'react';
import type { CategoryRequestDTO } from '../types/Category';
import api from '../api/axios';
import { X } from 'lucide-react';

interface CategoryFormProps {
  onClose: () => void;
  onSave: () => void;
}

export const CategoryForm: React.FC<CategoryFormProps> = ({ onClose, onSave }) => {
  const [formData, setFormData] = useState<CategoryRequestDTO>({
    name: '',
    description: '',
  });

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError(null);

    try {
      const payload: CategoryRequestDTO = {
        name: formData.name,
        description: formData.description || undefined,
      };
      await api.post('/api/v1/categories', payload);
      onSave();
    } catch (err: unknown) {
      // 409 = conflicto de unicidad en BD: el nombre ya existe
      if ((err as { response?: { status?: number } }).response?.status === 409) {
        setError(`La categoría "${formData.name}" ya existe. Usa un nombre diferente.`);
      } else {
        // ProblemDetail (RFC 7807) trae el texto en 'detail'; dejamos 'message' como respaldo
        setError(
          (err as { response?: { data?: { detail?: string; message?: string } } }).response?.data?.detail ||
            (err as { response?: { data?: { message?: string } } }).response?.data?.message ||
            'Error al guardar la categoría'
        );
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4">
      <div className="bg-surface border border-border w-full max-w-lg rounded-2xl shadow-xl overflow-hidden flex flex-col max-h-[90vh]">
        <div className="flex justify-between items-center p-6 border-b border-border">
          <h2 className="text-xl font-semibold text-gray-800">Crear Categoría</h2>
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

          <form id="category-form" onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="label-text">Nombre</label>
              <input
                type="text"
                name="name"
                value={formData.name}
                onChange={handleChange}
                data-testid="category-name"
                required
                className="input-field"
                placeholder="Ej: Electrónica"
              />
            </div>
            <div>
              <label className="label-text">Descripción</label>
              <textarea
                name="description"
                value={formData.description}
                onChange={handleChange}
                data-testid="category-description"
                className="input-field min-h-25 resize-y"
                placeholder="Descripción opcional de la categoría..."
              />
            </div>
          </form>
        </div>

        <div className="p-6 border-t border-border flex justify-end space-x-3 bg-surface-hover/30">
          <button type="button" onClick={onClose} className="btn-secondary">
            Cancelar
          </button>
          <button
            type="submit"
            form="category-form"
            disabled={loading}
            data-testid="category-submit"
            className="btn-primary flex items-center"
          >
            {loading ? 'Guardando...' : 'Guardar Categoría'}
          </button>
        </div>
      </div>
    </div>
  );
};
