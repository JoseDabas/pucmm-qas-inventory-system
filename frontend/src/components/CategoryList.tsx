import { useCallback, useEffect, useState } from 'react';
import { useAuth } from 'react-oidc-context';
import type { Category } from '../types/Category';
import api from '../api/axios';
import { CategoryForm } from './CategoryForm';
import { ConfirmDialog } from './ConfirmDialog';
import { Plus, Trash2, Tags, Search } from 'lucide-react';

export const CategoryList: React.FC = () => {
  const [categories, setCategories] = useState<Category[]>([]);
  const [error, setError] = useState<string | null>(null);
  const auth = useAuth();

  const [isFormOpen, setIsFormOpen] = useState(false);
  const [categoryToDelete, setCategoryToDelete] = useState<Category | null>(null);

  // Búsqueda y paginación del lado del cliente: el endpoint devuelve todas las
  // categorías, así que filtramos y paginamos en memoria replicando la UX de las
  // demás secciones (page en base 0, 10 por página).
  const [searchTerm, setSearchTerm] = useState('');
  const [page, setPage] = useState(0);
  const PAGE_SIZE = 10;

  // Solo quien puede gestionar productos puede crear/eliminar categorías.
  const canManage = (() => {
    if (!auth.user?.access_token) return false;
    try {
      const base64Url = auth.user.access_token.split('.')[1];
      let base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
      while (base64.length % 4) {
        base64 += '=';
      }
      const jsonPayload = decodeURIComponent(
        window
          .atob(base64)
          .split('')
          .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
          .join('')
      );
      const payload = JSON.parse(jsonPayload);
      const roles = payload.realm_access?.roles || [];
      return roles.includes('product:manage');
    } catch (e) {
      console.error('Error al decodificar el access_token:', e);
      return false;
    }
  })();

  const fetchCategories = useCallback(async () => {
    try {
      setError(null);
      const response = await api.get('/api/v1/categories');
      setCategories(response.data || []);
    } catch (err: unknown) {
      setError(
        (err as { response?: { data?: { detail?: string; message?: string } } }).response?.data?.detail ||
          'Error al cargar las categorías'
      );
    }
  }, []);

  useEffect(() => {
    fetchCategories();
  }, [fetchCategories]);

  const handleFormSave = () => {
    setIsFormOpen(false);
    fetchCategories();
  };

  const confirmDelete = async () => {
    if (!categoryToDelete) return;
    try {
      await api.delete(`/api/v1/categories/${categoryToDelete.id}`);
      setCategoryToDelete(null);
      fetchCategories();
    } catch (err: unknown) {
      setError(
        (err as { response?: { data?: { detail?: string; message?: string } } }).response?.data?.detail ||
          'Error al eliminar la categoría'
      );
      setCategoryToDelete(null);
    }
  };

  // Filtra por nombre (case-insensitive) y pagina en memoria.
  const filteredCategories = categories.filter((c) =>
    c.name.toLowerCase().includes(searchTerm.trim().toLowerCase())
  );
  const totalPages = Math.ceil(filteredCategories.length / PAGE_SIZE);
  // Página segura por si el conjunto se reduce (tras buscar o eliminar).
  const safePage = Math.min(page, Math.max(0, totalPages - 1));
  const pagedCategories = filteredCategories.slice(
    safePage * PAGE_SIZE,
    safePage * PAGE_SIZE + PAGE_SIZE
  );

  return (
    <div className="max-w-7xl mx-auto p-6">
      <div className="flex justify-between items-center mb-8">
        <div>
          <h1 className="text-2xl font-semibold text-gray-800 flex items-center gap-3">
            <Tags className="text-primary-600" size={28} />
            Categorías
          </h1>
          <p className="text-gray-500 mt-1">Clasificación de los productos de tu inventario</p>
        </div>
        {canManage && (
          <button
            onClick={() => setIsFormOpen(true)}
            data-testid="create-category-button"
            className="btn-primary flex items-center gap-2"
          >
            <Plus size={20} />
            <span>Nueva Categoría</span>
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
          onChange={(e) => {
            setSearchTerm(e.target.value);
            setPage(0);
          }}
          placeholder="Buscar por nombre..."
          data-testid="category-search-input"
          className="w-full max-w-sm pl-10 pr-4 py-2 border border-border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
        />
      </div>

      <div className="bg-surface border border-border rounded-xl overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse" data-testid="categories-table">
            <thead>
              <tr className="bg-surface-hover border-b border-border text-gray-500 text-sm uppercase tracking-wider">
                <th className="p-4 font-semibold">Nombre</th>
                <th className="p-4 font-semibold">Descripción</th>
                <th className="p-4 font-semibold text-center">Cantidad de productos</th>
                <th className="p-4 font-semibold text-center">Acciones</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-border">
              {filteredCategories.length === 0 ? (
                <tr>
                  <td colSpan={4} className="p-8 text-center text-gray-400">
                    {searchTerm ? 'No se encontraron categorías.' : 'No hay categorías registradas.'}
                  </td>
                </tr>
              ) : (
                pagedCategories.map((category) => (
                  <tr key={category.id} data-testid="category-row">
                    <td className="p-4 text-gray-900 font-medium ">{category.name}</td>
                    <td className="p-4 text-gray-600">
                      <div className="truncate max-w-xs" title={category.description}>
                        {category.description || '—'}
                      </div>
                    </td>
                    <td className="p-4 text-gray-900 font-semibold text-center">{category.productCount}</td>
                    <td className="p-4 text-center">
                      {canManage && (
                        <div className="flex justify-center">
                          <button
                            onClick={() => setCategoryToDelete(category)}
                            disabled={category.productCount > 0}
                            data-testid="delete-category-button"
                            className="p-2 text-gray-500 hover:text-red-600 hover:bg-red-50 rounded-lg transition-colors disabled:opacity-40 disabled:cursor-not-allowed disabled:hover:bg-transparent disabled:hover:text-gray-500"
                            title={
                              category.productCount > 0
                                ? 'No se puede eliminar: tiene productos asociados'
                                : 'Eliminar'
                            }
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

      {totalPages > 0 && (
        <div className="flex items-center justify-between mt-4">
          <span className="text-sm text-gray-500">
            Página {safePage + 1} de {totalPages}
          </span>
          <div className="flex gap-2">
            <button
              onClick={() => setPage((p) => Math.max(0, p - 1))}
              disabled={safePage === 0}
              data-testid="prev-page-button"
              className="px-3 py-1.5 border border-border rounded-lg disabled:opacity-50 hover:bg-surface-hover"
            >
              Anterior
            </button>
            <button
              onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
              disabled={safePage >= totalPages - 1}
              data-testid="next-page-button"
              className="px-3 py-1.5 border border-border rounded-lg disabled:opacity-50 hover:bg-surface-hover"
            >
              Siguiente
            </button>
          </div>
        </div>
      )}

      {isFormOpen && <CategoryForm onClose={() => setIsFormOpen(false)} onSave={handleFormSave} />}

      {categoryToDelete && (
        <ConfirmDialog
          title="Eliminar categoría"
          message={`¿Estás seguro de eliminar la categoría "${categoryToDelete.name}"? Esta acción no se puede deshacer.`}
          confirmLabel="Eliminar"
          onConfirm={confirmDelete}
          onCancel={() => setCategoryToDelete(null)}
        />
      )}
    </div>
  );
};
