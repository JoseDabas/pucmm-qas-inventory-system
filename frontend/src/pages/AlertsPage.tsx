import React, { useEffect, useState } from 'react';
import { Bell, AlertTriangle, Package } from 'lucide-react';
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

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-indigo-600"></div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-900 flex items-center gap-2">
          <Bell className="w-6 h-6 text-red-500" />
          Alertas de Stock Crítico
        </h1>
        <p className="mt-1 text-sm text-gray-500">
          Listado de productos cuyo stock actual es menor o igual a su límite mínimo.
        </p>
      </div>

      {error && (
        <div className="bg-red-50 text-red-700 p-4 rounded-md flex items-center gap-2">
          <AlertTriangle className="w-5 h-5" />
          {error}
        </div>
      )}

      {!error && alerts.length === 0 ? (
        <div className="bg-green-50 border border-green-200 rounded-lg p-8 text-center">
          <div className="mx-auto w-12 h-12 bg-green-100 rounded-full flex items-center justify-center mb-4">
            <Package className="w-6 h-6 text-green-600" />
          </div>
          <h3 className="text-lg font-medium text-green-900 mb-1">¡Todo en orden!</h3>
          <p className="text-green-700">No hay productos con stock crítico en este momento.</p>
        </div>
      ) : (
        <div className="bg-white shadow rounded-lg overflow-hidden border border-gray-200">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Producto
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  SKU
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Categoría
                </th>
                <th className="px-6 py-3 text-center text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Stock Mínimo
                </th>
                <th className="px-6 py-3 text-center text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Stock Actual
                </th>
                <th className="px-6 py-3 text-center text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Estado
                </th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {alerts.map((product) => {
                const isOutOfStock = product.stockActual === 0;
                return (
                  <tr key={product.id} className="hover:bg-gray-50 transition-colors">
                    <td className="px-6 py-4 whitespace-nowrap">
                      <div className="text-sm font-medium text-gray-900">{product.name}</div>
                      <div className="text-sm text-gray-500 truncate max-w-[200px]" title={product.description}>
                        {product.description}
                      </div>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                      {product.skuCode}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                      {product.category}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-center font-medium text-gray-600">
                      {product.minimumStock}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-center">
                      <span className={`font-bold ${isOutOfStock ? 'text-red-600' : 'text-orange-600'}`}>
                        {product.stockActual}
                      </span>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-center">
                      <span
                        className={`px-2 inline-flex text-xs leading-5 font-semibold rounded-full ${
                          isOutOfStock ? 'bg-red-100 text-red-800' : 'bg-orange-100 text-orange-800'
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
      )}
    </div>
  );
};
