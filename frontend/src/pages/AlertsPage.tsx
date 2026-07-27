import React, { useEffect, useState } from 'react';
import { Bell, AlertTriangle, CheckCircle2 } from 'lucide-react';
import api from '../api/axios';
import type { Product } from '../types/Product';

export const AlertsPage: React.FC = () => {
  const [alerts, setAlerts] = useState<Product[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetchAlerts();
  }, []);

  const fetchAlerts = async () => {
    try {
      setLoading(true);
      setError(null);
      const response = await api.get('/api/v1/products/alerts/critical-stock');
      setAlerts(response.data);
    } catch (err) {
      console.error('Error fetching alerts:', err);
      setError('No se pudieron cargar las alertas de stock crítico.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="max-w-7xl mx-auto p-6">
      <div className="mb-8">
        <h1 className="text-2xl font-semibold text-gray-800 flex items-center gap-3">
          <Bell className="text-primary-600" size={28} />
          Alertas de Stock Crítico
        </h1>
        <p className="text-gray-500 mt-1">
          Productos cuyo stock actual es menor o igual a su límite mínimo.
        </p>
      </div>

      {error && (
        <div className="bg-red-500/10 border border-red-500/50 text-red-500 p-4 rounded-lg mb-6 flex items-center gap-2">
          <AlertTriangle size={18} />
          {error}
        </div>
      )}

      {loading ? (
        <p className="text-gray-400">Cargando alertas…</p>
      ) : !error && alerts.length === 0 ? (
        <div className="bg-surface border border-border rounded-xl p-10 text-center">
          <div className="mx-auto w-12 h-12 bg-green-500/10 rounded-full flex items-center justify-center mb-4">
            <CheckCircle2 className="text-green-600" size={24} />
          </div>
          <h3 className="text-lg font-medium text-gray-800 mb-1">¡Todo en orden!</h3>
          <p className="text-gray-500">No hay productos con stock crítico en este momento.</p>
        </div>
      ) : (
        <div className="bg-surface border border-border rounded-xl overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="bg-surface-hover border-b border-border text-gray-500 text-sm uppercase tracking-wider">
                  <th className="p-4 font-semibold">Producto</th>
                  <th className="p-4 font-semibold">SKU</th>
                  <th className="p-4 font-semibold">Categoría</th>
                  <th className="p-4 font-semibold text-center">Stock Mínimo</th>
                  <th className="p-4 font-semibold text-center">Stock Actual</th>
                  <th className="p-4 font-semibold text-center">Estado</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border">
                {alerts.map((product) => {
                  const isOutOfStock = product.stockActual === 0;
                  return (
                    <tr key={product.id}>
                      <td className="p-4">
                        <div className="text-gray-900 font-medium">{product.name}</div>
                        <div className="text-gray-500 text-sm truncate max-w-[240px]" title={product.description}>
                          {product.description}
                        </div>
                      </td>
                      <td className="p-4 text-gray-600 text-sm">{product.skuCode}</td>
                      <td className="p-4">
                        <span className="bg-accent-500/10 text-accent-500 px-2.5 py-1 rounded-full text-xs font-medium">
                          {product.category || 'N/A'}
                        </span>
                      </td>
                      <td className="p-4 text-center text-gray-600 font-medium">{product.minimumStock}</td>
                      <td className="p-4 text-center">
                        <span className={`font-semibold ${isOutOfStock ? 'text-red-600' : 'text-orange-600'}`}>
                          {product.stockActual}
                        </span>
                      </td>
                      <td className="p-4 text-center">
                        <span
                          className={`px-2.5 py-1 rounded-full text-xs font-medium ${
                            isOutOfStock ? 'bg-red-100 text-red-700' : 'bg-orange-100 text-orange-700'
                          }`}
                        >
                          {isOutOfStock ? 'Agotado' : 'Crítico'}
                        </span>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
};
