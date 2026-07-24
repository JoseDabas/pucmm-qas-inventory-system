import type { LucideIcon } from 'lucide-react';

interface MetricCardProps {
  label: string;
  value: string | number;
  icon: LucideIcon;
  /** Texto auxiliar opcional debajo del valor (ej. "3 en estado crítico"). */
  hint?: string;
}

/**
 * Tarjeta de estadística reutilizable para el tablero de control.
 * Muestra un ícono, una etiqueta y un valor destacado. Sirve tanto para las
 * "Métricas del sistema" como para los "Indicadores operacionales".
 */
export const MetricCard: React.FC<MetricCardProps> = ({ label, value, hint }) => {
  return (
    <div className="bg-surface border border-border rounded-xl p-5">
      <div className="flex items-center justify-between">
        <span className="text-sm text-gray-500">{label}</span>
      </div>
      <div data-testid="metric-value" className="text-2xl font-semibold text-gray-800 mt-2">{value}</div>
      {hint && <p className="text-xs text-gray-400 mt-1">{hint}</p>}
    </div>
  );
};
