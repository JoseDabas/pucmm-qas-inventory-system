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
        return <span className="px-2.5 py-1 rounded-full text-xs font-medium bg-green-100 text-green-700">CREADO</span>;
      case 'UPDATED':
        return <span className="px-2.5 py-1 rounded-full text-xs font-medium bg-blue-100 text-blue-700">MODIFICADO</span>;
      case 'DELETED':
        return <span className="px-2.5 py-1 rounded-full text-xs font-medium bg-red-100 text-red-700">ELIMINADO</span>;
      default:
        return <span className="px-2.5 py-1 rounded-full text-xs font-medium bg-gray-100 text-gray-700">{type}</span>;
    }
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleString();
  };

  return (
    <div className="max-w-7xl mx-auto p-6">
      <div className="mb-8">
        <h1 className="text-2xl font-semibold text-gray-800 flex items-center gap-3">
          <ShieldCheck className="text-primary-600" size={28} />
          Auditoría del Sistema
        </h1>
        <p className="text-gray-500 mt-1">
          Registro inmutable de cambios en el sistema generados por Hibernate Envers
        </p>
      </div>

      <div className="border-b border-border mb-6">
        <nav className="-mb-px flex space-x-8" aria-label="Tabs">
          <button
            onClick={() => setActiveTab('products')}
            data-testid="audit-products-tab"
            className={`${
              activeTab === 'products'
                ? 'border-primary-500 text-primary-600'
                : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-border'
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
                ? 'border-primary-500 text-primary-600'
                : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-border'
            } whitespace-nowrap py-4 px-1 border-b-2 font-medium text-sm flex items-center gap-2`}
          >
            <ArrowRightLeft className="h-4 w-4" />
            Movimientos de Stock
          </button>
        </nav>
      </div>

      <div className="bg-surface border border-border rounded-xl overflow-hidden">
        {loading ? (
          <div className="p-8 text-center text-gray-400">Cargando registros…</div>
        ) : activeTab === 'products' ? (
          <div className="overflow-x-auto" data-testid="audit-products-table">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="bg-surface-hover border-b border-border text-gray-500 text-sm uppercase tracking-wider">
                  <th className="p-4 font-semibold">Fecha / Rev #</th>
                  <th className="p-4 font-semibold">Acción</th>
                  <th className="p-4 font-semibold">ID Entidad</th>
                  <th className="p-4 font-semibold">Detalles (Snapshot)</th>
                  <th className="p-4 font-semibold">Usuario (Auditoría)</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border">
                {productsAudit.map((audit) => (
                  <tr key={`${audit.entityId}-${audit.revision}`}>
                    <td className="p-4 whitespace-nowrap text-sm text-gray-900">
                      <div className="flex items-center gap-1 text-gray-500 mb-1">
                        <Clock className="h-3 w-3" />
                        {formatDate(audit.revisionDate)}
                      </div>
                      <div className="text-xs text-gray-400">Rev: #{audit.revision}</div>
                    </td>
                    <td className="p-4 whitespace-nowrap">
                      {renderRevisionBadge(audit.revisionType)}
                    </td>
                    <td className="p-4 whitespace-nowrap text-xs text-gray-500 font-mono">
                      {audit.entityId.substring(0, 8)}...
                    </td>
                    <td className="px-6 py-4">
                      {audit.name ? (
                        <div className="text-sm text-gray-900">
                          <span className="font-medium">{audit.name}</span> ({audit.skuCode})
                          <div className="text-xs text-gray-500 mt-1">
                            Precio: ${audit.price} | Stock Mínimo: {audit.minimumStock} | {audit.isActive ? 'Activo' : 'Inactivo'}
                          </div>
                          {audit.category && (
                            <div className="text-xs text-gray-500">
                              Cat: {audit.category}
                            </div>
                          )}
                        </div>
                      ) : (
                        <span className="text-sm text-gray-400 italic">No disponible</span>
                      )}
                    </td>
                    <td className="p-4 text-sm text-gray-900">{audit.modifiedBy || 'Sistema'}</td>
                  </tr>
                ))}
                {productsAudit.length === 0 && (
                  <tr>
                    <td colSpan={5} className="p-8 text-center text-gray-400">
                      No hay registros de auditoría de productos.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        ) : (
          <div className="overflow-x-auto" data-testid="audit-movements-table">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="bg-surface-hover border-b border-border text-gray-500 text-sm uppercase tracking-wider">
                  <th className="p-4 font-semibold">Fecha / Rev #</th>
                  <th className="p-4 font-semibold">Acción</th>
                  <th className="p-4 font-semibold">Operación (Negocio)</th>
                  <th className="p-4 font-semibold">Cambio Cantidad</th>
                  <th className="p-4 font-semibold">Usuario (Auditoría)</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border">
                {movementsAudit.map((audit) => (
                  <tr key={`${audit.entityId}-${audit.revision}`}>
                    <td className="p-4 whitespace-nowrap text-sm text-gray-900">
                      <div className="flex items-center gap-1 text-gray-500 mb-1">
                        <Clock className="h-3 w-3" />
                        {formatDate(audit.revisionDate)}
                      </div>
                      <div className="text-xs text-gray-400">Rev: #{audit.revision}</div>
                    </td>
                    <td className="p-4 whitespace-nowrap">
                      {renderRevisionBadge(audit.revisionType)}
                    </td>
                    <td className="p-4 text-sm text-gray-900">
                      {audit.revisionType !== 'DELETED' ? (
                        <div>
                          <div className="font-medium text-primary-600">{audit.username}</div>
                          <div className="text-gray-500 text-xs mt-1">
                            Tipo: {audit.movementType} | Prod: {audit.productId?.substring(0,8)}...
                            {audit.observations && ` | Obs: ${audit.observations}`}
                          </div>
                        </div>
                      ) : (
                        <span className="text-gray-400 italic">Registro eliminado</span>
                      )}
                    </td>
                    <td className="p-4 whitespace-nowrap text-sm text-gray-900 font-mono">
                      {audit.revisionType !== 'DELETED' ? (
                        `${audit.previousQuantity} → ${audit.newQuantity}`
                      ) : '-'}
                    </td>
                    <td className="p-4 text-sm text-gray-900">{audit.modifiedBy || 'Sistema'}</td>
                  </tr>
                ))}
                {movementsAudit.length === 0 && (
                  <tr>
                    <td colSpan={4} className="p-8 text-center text-gray-400">
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

