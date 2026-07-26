import React, { useEffect, useState } from 'react';
import { ShieldCheck, Package, ArrowRightLeft, Clock } from 'lucide-react';
import { getProductAuditHistory, getStockMovementAuditHistory } from '../api/audit';
import type { ProductAuditEntry, StockMovementAuditEntry } from '../types/Audit';

export const AuditPage: React.FC = () => {
  const [activeTab, setActiveTab] = useState<'products' | 'movements'>('products');
  const [productsAudit, setProductsAudit] = useState<ProductAuditEntry[]>([]);
  const [movementsAudit, setMovementsAudit] = useState<StockMovementAuditEntry[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchAudit = async () => {
      setLoading(true);
      try {
        if (activeTab === 'products') {
          const data = await getProductAuditHistory();
          setProductsAudit(data);
        } else {
          const data = await getStockMovementAuditHistory();
          setMovementsAudit(data);
        }
      } catch (error) {
        console.error('Error fetching audit data', error);
      } finally {
        setLoading(false);
      }
    };
    fetchAudit();
  }, [activeTab]);

  const renderRevisionBadge = (type: string) => {
    switch (type) {
      case 'CREATED':
        return <span className="px-2 inline-flex text-xs leading-5 font-semibold rounded-full bg-green-100 text-green-800">CREADO</span>;
      case 'UPDATED':
        return <span className="px-2 inline-flex text-xs leading-5 font-semibold rounded-full bg-blue-100 text-blue-800">MODIFICADO</span>;
      case 'DELETED':
        return <span className="px-2 inline-flex text-xs leading-5 font-semibold rounded-full bg-red-100 text-red-800">ELIMINADO</span>;
      default:
        return <span className="px-2 inline-flex text-xs leading-5 font-semibold rounded-full bg-gray-100 text-gray-800">{type}</span>;
    }
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleString();
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold text-gray-900 flex items-center gap-2">
          <ShieldCheck className="h-6 w-6 text-indigo-600" />
          Auditoría del Sistema
        </h1>
        <p className="mt-2 text-sm text-gray-600">
          Registro inmutable de cambios en el sistema generados por Hibernate Envers.
        </p>
      </div>

      <div className="border-b border-gray-200">
        <nav className="-mb-px flex space-x-8" aria-label="Tabs">
          <button
            onClick={() => setActiveTab('products')}
            data-testid="audit-products-tab"
            className={`${
              activeTab === 'products'
                ? 'border-indigo-500 text-indigo-600'
                : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
            } whitespace-nowrap py-4 px-1 border-b-2 font-medium text-sm flex items-center gap-2`}
          >
            <Package className="h-4 w-4" />
            Productos
          </button>
          <button
            onClick={() => setActiveTab('movements')}
            data-testid="audit-movements-tab"
            className={`${
              activeTab === 'movements'
                ? 'border-indigo-500 text-indigo-600'
                : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
            } whitespace-nowrap py-4 px-1 border-b-2 font-medium text-sm flex items-center gap-2`}
          >
            <ArrowRightLeft className="h-4 w-4" />
            Movimientos de Stock
          </button>
        </nav>
      </div>

      <div className="bg-white shadow rounded-lg overflow-hidden">
        {loading ? (
          <div className="p-8 text-center text-gray-500">Cargando registros...</div>
        ) : activeTab === 'products' ? (
          <div className="overflow-x-auto" data-testid="audit-products-table">
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Fecha / Rev #</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Acción</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">ID Entidad</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Detalles (Snapshot)</th>
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-gray-200">
                {productsAudit.map((audit) => (
                  <tr key={`${audit.entityId}-${audit.revision}`}>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                      <div className="flex items-center gap-1 text-gray-500 mb-1">
                        <Clock className="h-3 w-3" />
                        {formatDate(audit.revisionDate)}
                      </div>
                      <div className="text-xs text-gray-400">Rev: #{audit.revision}</div>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      {renderRevisionBadge(audit.revisionType)}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500 font-mono text-xs">
                      {audit.entityId.substring(0, 8)}...
                    </td>
                    <td className="px-6 py-4 text-sm text-gray-900">
                      {audit.revisionType !== 'DELETED' ? (
                        <div>
                          <span className="font-medium">{audit.name}</span> ({audit.skuCode})
                          <div className="text-gray-500 text-xs mt-1">
                            Precio: ${audit.price} | Stock Mínimo: {audit.minimumStock} | {audit.isActive ? 'Activo' : 'Inactivo'}
                          </div>
                        </div>
                      ) : (
                        <span className="text-gray-400 italic">Registro eliminado</span>
                      )}
                    </td>
                  </tr>
                ))}
                {productsAudit.length === 0 && (
                  <tr>
                    <td colSpan={4} className="px-6 py-8 text-center text-gray-500">
                      No hay registros de auditoría de productos.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        ) : (
          <div className="overflow-x-auto" data-testid="audit-movements-table">
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Fecha / Rev #</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Acción</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Usuario / Movimiento</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Cambio Cantidad</th>
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-gray-200">
                {movementsAudit.map((audit) => (
                  <tr key={`${audit.entityId}-${audit.revision}`}>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                      <div className="flex items-center gap-1 text-gray-500 mb-1">
                        <Clock className="h-3 w-3" />
                        {formatDate(audit.revisionDate)}
                      </div>
                      <div className="text-xs text-gray-400">Rev: #{audit.revision}</div>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      {renderRevisionBadge(audit.revisionType)}
                    </td>
                    <td className="px-6 py-4 text-sm text-gray-900">
                      {audit.revisionType !== 'DELETED' ? (
                        <div>
                          <div className="font-medium text-indigo-600">{audit.username}</div>
                          <div className="text-gray-500 text-xs mt-1">
                            Tipo: {audit.movementType} | Prod: {audit.productId?.substring(0,8)}...
                            {audit.observations && ` | Obs: ${audit.observations}`}
                          </div>
                        </div>
                      ) : (
                        <span className="text-gray-400 italic">Registro eliminado</span>
                      )}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900 font-mono">
                      {audit.revisionType !== 'DELETED' ? (
                        `${audit.previousQuantity} → ${audit.newQuantity}`
                      ) : '-'}
                    </td>
                  </tr>
                ))}
                {movementsAudit.length === 0 && (
                  <tr>
                    <td colSpan={4} className="px-6 py-8 text-center text-gray-500">
                      No hay registros de auditoría de movimientos.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
};

