import { useCallback, useEffect, useState } from 'react';
import { useAuth } from 'react-oidc-context';
import type { StockMovement } from '../types/StockMovement';
import api from '../api/axios';
import { StockMovementForm } from './StockMovementForm';
import { History, Plus, Search} from 'lucide-react';

export const StockMovementList: React.FC = () => {
  const [movements, setMovements] = useState<StockMovement[]>([]);
  const [error, setError] = useState<string | null>(null);
  const auth = useAuth();

  // Búsqueda y paginación (page en base 0, igual que Spring Data)
  const [searchTerm, setSearchTerm] = useState('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const PAGE_SIZE = 10;

  const [isFormOpen, setIsFormOpen] = useState(false);

  // Solo quien puede gestionar productos puede registrar movimientos.
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

  const fetchMovements = useCallback(async () => {
    try {
      setError(null);
      const response = await api.get('/api/v1/stock-movements', {
        params: {
          page,
          size: PAGE_SIZE,
          search: searchTerm || undefined,
        },
      });
      setMovements(response.data.content || []);
      setTotalPages(response.data.totalPages ?? 0);
    } catch (err: unknown) {
      setError(
        (err as { response?: { data?: { detail?: string; message?: string } } }).response?.data?.detail ||
          'Error al cargar el historial de movimientos'
      );
    }
  }, [page, searchTerm]);

  // Recarga al cambiar de página o el término de búsqueda. El debounce evita
  // pegarle al API en cada tecla mientras se escribe la búsqueda.
  useEffect(() => {
    const timer = setTimeout(fetchMovements, 300);
    return () => clearTimeout(timer);
  }, [fetchMovements]);

  const handleFormSave = () => {
    setIsFormOpen(false);
    setPage(0);
    fetchMovements();
  };

  const formatDate = (iso: string) =>
    new Date(iso).toLocaleString('es-DO', {
      dateStyle: 'medium',
      timeStyle: 'short',
    });

  return (
    <div className="max-w-7xl mx-auto p-6">
      <div className="flex justify-between items-center mb-8">
        <div>
          <h1 className="text-2xl font-semibold text-gray-800 flex items-center gap-3">
            <History className="text-primary-600" size={28} />
            Historial de Movimientos
          </h1>
          <p className="text-gray-500 mt-1">Entradas y salidas de stock de tus productos</p>
        </div>
        {canManage && (
          <button
            onClick={() => setIsFormOpen(true)}
            data-testid="create-movement-button"
            className="btn-primary flex items-center gap-2"
          >
            <Plus size={20} />
            <span>Registrar Movimiento</span>
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
          placeholder="Buscar por producto o usuario..."
          data-testid="movement-search-input"
          className="w-full max-w-sm pl-10 pr-4 py-2 border border-border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
        />
      </div>

      <div className="bg-surface border border-border rounded-xl overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse" data-testid="movements-table">
            <thead>
              <tr className="bg-surface-hover border-b border-border text-gray-500 text-sm uppercase tracking-wider">
                <th className="p-4 font-semibold">Fecha</th>
                <th className="p-4 font-semibold">Producto</th>
                <th className="p-4 font-semibold">Usuario</th>
                <th className="p-4 font-semibold">Tipo</th>
                <th className="p-4 font-semibold text-right">Cant. anterior</th>
                <th className="p-4 font-semibold text-right">Cant. nueva</th>
                <th className="p-4 font-semibold">Observaciones</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-border">
              {movements.length === 0 ? (
                <tr>
                  <td colSpan={7} className="p-8 text-center text-gray-400">
                    {searchTerm
                      ? 'No se encontraron movimientos.'
                      : 'No hay movimientos registrados.'}
                  </td>
                </tr>
              ) : (
                movements.map((m) => (
                  <tr key={m.id} data-testid="movement-row">
                    <td className="p-4 text-gray-600 text-sm whitespace-nowrap">{formatDate(m.date)}</td>
                    <td className="p-4 text-gray-900 font-medium">{m.productName || 'N/A'}</td>
                    <td className="p-4 text-gray-600">{m.username}</td>
                    <td className="p-4">
                      {m.movementType === 'IN' ? (
                        <span className="inline-flex items-center gap-1 bg-primary-50 text-primary-700 px-2.5 py-1 rounded-full text-xs font-medium">
                          Entrada
                        </span>
                      ) : (
                        <span className="inline-flex items-center gap-1 bg-red-50 text-red-600 px-2.5 py-1 rounded-full text-xs font-medium">
                          Salida
                        </span>
                      )}
                    </td>
                    <td className="p-4 text-gray-600">{m.previousQuantity}</td>
                    <td className="p-4 text-gray-900 font-semibold">{m.newQuantity}</td>
                    <td className="p-4 text-gray-600">
                      <div className="truncate max-w-xs" title={m.observations}>
                        {m.observations || '—'}
                      </div>
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
          <span className="text-sm text-gray-500">
            Página {page + 1} de {totalPages}
          </span>
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
        <StockMovementForm onClose={() => setIsFormOpen(false)} onSave={handleFormSave} />
      )}
    </div>
  );
};
