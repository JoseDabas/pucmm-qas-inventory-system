import { useEffect, useState } from 'react';
import { LayoutDashboard, Package, Tags, ArrowLeftRight, AlertTriangle, Boxes, DollarSign, Percent } from 'lucide-react';
import api from '../api/axios';
import type { Product } from '../types/Product';
import { MetricCard } from '../components/dashboard/MetricCard';

// Cantidad de productos que se traen para calcular los totales agregados
// (unidades y valor del inventario). Suficiente para la escala del proyecto.
const AGGREGATE_PAGE_SIZE = 1000;

const number = new Intl.NumberFormat('es-DO');
const money = new Intl.NumberFormat('es-DO', { minimumFractionDigits: 2, maximumFractionDigits: 2 });

/**
 * Tablero de control: visión general del estado del inventario mediante métricas
 * del sistema e indicadores operacionales. Cada consulta se resuelve por separado
 * para que un fallo aislado (por ejemplo un 403 por falta de 'report:view') no
 * impida mostrar el resto de los indicadores.
 */
export const DashboardPage: React.FC = () => {
  const [loading, setLoading] = useState(true);

  // Métricas del sistema e indicadores operacionales (null = no disponible → se muestra "—")
  const [totalProducts, setTotalProducts] = useState<number | null>(null);
  const [totalUnits, setTotalUnits] = useState<number | null>(null);
  const [totalValue, setTotalValue] = useState<number | null>(null);
  const [totalCategories, setTotalCategories] = useState<number | null>(null);
  const [totalMovements, setTotalMovements] = useState<number | null>(null);
  const [criticalCount, setCriticalCount] = useState<number | null>(null);

  useEffect(() => {
    const load = async () => {
      setLoading(true);

      // Se lanzan todas las peticiones en paralelo y luego se resuelve cada una por
      // separado, de modo que un fallo aislado no tumbe el resto del tablero.
      const productsP = api.get('/api/v1/products', { params: { page: 0, size: AGGREGATE_PAGE_SIZE } });
      const categoriesP = api.get('/api/v1/categories');
      const criticalP = api.get('/api/v1/products/alerts/critical-stock');
      const movementsP = api.get('/api/v1/stock-movements', { params: { page: 0, size: 1 } });

      try {
        const { data } = await productsP;
        const list: Product[] = data.content || [];
        setTotalProducts(data.totalElements ?? list.length);
        setTotalUnits(list.reduce((sum, p) => sum + (p.stockActual || 0), 0));
        setTotalValue(list.reduce((sum, p) => sum + (p.price || 0) * (p.stockActual || 0), 0));
      } catch {
        // Se dejan las métricas en null → la UI muestra "—".
      }

      try {
        const { data } = await categoriesP;
        setTotalCategories(Array.isArray(data) ? data.length : 0);
      } catch {
        // Categoría no disponible → "—".
      }

      try {
        const { data } = await criticalP;
        setCriticalCount(Array.isArray(data) ? data.length : 0);
      } catch {
        // Sin permiso (report:view) o error → "—".
      }

      try {
        const { data } = await movementsP;
        setTotalMovements(data.totalElements ?? 0);
      } catch {
        // Sin permiso (report:view) o error → "—".
      }

      setLoading(false);
    };

    load();
  }, []);

  const criticalPct =
    criticalCount === null || totalProducts === null || totalProducts === 0
      ? null
      : Math.round((criticalCount / totalProducts) * 1000) / 10;

  const show = (value: number | null) => (value === null ? '—' : number.format(value));

  return (
    <div className="max-w-7xl mx-auto p-6">
      <div className="mb-8">
        <h1 className="text-2xl font-semibold text-gray-800 flex items-center gap-3">
          <LayoutDashboard className="text-primary-600" size={28} />
          Dashboard
        </h1>
        <p className="text-gray-500 mt-1">Visión general del estado del inventario y estadísticas clave</p>
      </div>

      {loading ? (
        <p className="text-gray-400">Cargando indicadores…</p>
      ) : (
        <div className="space-y-8">
          {/* Métricas del sistema */}
          <section>
            <h2 className="text-sm font-semibold text-gray-500 uppercase tracking-wider mb-3">Métricas del sistema</h2>
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
              <MetricCard label="Productos" value={show(totalProducts)} icon={Package} />
              <MetricCard label="Categorías" value={show(totalCategories)} icon={Tags} />
              <MetricCard label="Movimientos" value={show(totalMovements)} icon={ArrowLeftRight} />
              <MetricCard label="Productos críticos" value={show(criticalCount)} icon={AlertTriangle} />
            </div>
          </section>

          {/* Indicadores operacionales */}
          <section>
            <h2 className="text-sm font-semibold text-gray-500 uppercase tracking-wider mb-3">Indicadores operacionales</h2>
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
              <MetricCard label="Unidades en inventario" value={show(totalUnits)} icon={Boxes} />
              <MetricCard
                label="Valor del inventario"
                value={totalValue === null ? '—' : `$${money.format(totalValue)}`}
                icon={DollarSign}
              />
              <MetricCard
                label="% en estado crítico"
                value={criticalPct === null ? '—' : `${criticalPct}%`}
                icon={Percent}
                hint={criticalCount !== null ? `${criticalCount} de ${show(totalProducts)} productos` : undefined}
              />
            </div>
          </section>
        </div>
      )}
    </div>
  );
};
