import { Construction } from 'lucide-react';

interface PlaceholderPageProps {
  title: string;
}

/**
 * Página reutilizable para secciones aún no implementadas.
 * Muestra un estado limpio "en construcción". Se usa para
 * Categorías, Reportes y Configuración (mismo componente, distinto título).
 */
export const PlaceholderPage: React.FC<PlaceholderPageProps> = ({ title }) => {
  return (
    <div className="max-w-5xl mx-auto p-6">
      <div className="bg-surface border border-border rounded-xl p-12 flex flex-col items-center text-center">
        <Construction className="text-gray-400 mb-4" size={40} />
        <h2 className="text-xl font-semibold text-gray-800">{title}</h2>
        <p className="text-gray-500 mt-1">Esta sección está en construcción.</p>
      </div>
    </div>
  );
};
