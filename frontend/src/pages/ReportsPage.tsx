import { useState, useEffect } from 'react';
import api from '../api/axios';
import { BarChart3, Download, Calendar, Filter } from 'lucide-react';
import type { Category } from '../types/Category';

export const ReportsPage: React.FC = () => {
  const [categories, setCategories] = useState<Category[]>([]);
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [categoryId, setCategoryId] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    // Cargar categorías para el filtro
    api.get('/api/v1/categories')
      .then(res => setCategories(res.data || []))
      .catch(err => console.error("Error cargando categorías", err));
  }, []);

  const handleDownloadReport = async () => {
    if (!startDate || !endDate) {
      setError('Por favor, selecciona una fecha de inicio y una de fin.');
      return;
    }
    
    // Validar rango lógico
    if (new Date(startDate) > new Date(endDate)) {
      setError('La fecha de inicio no puede ser posterior a la fecha de fin.');
      return;
    }

    try {
      setError(null);
      setIsLoading(true);

      // Usar axios para descargar el archivo blob
      const response = await api.get('/api/v1/reports/movements', {
        params: {
          startDate: new Date(startDate).toISOString(),
          endDate: new Date(endDate).toISOString(),
          categoryId: categoryId || undefined
        },
        responseType: 'blob' // Importante para recibir binarios
      });

      // Crear URL del blob y disparar la descarga
      const url = window.URL.createObjectURL(new Blob([response.data], { type: 'application/pdf' }));
      const link = document.createElement('a');
      link.href = url;
      // Extraer filename del header si es posible, o usar uno por defecto
      let filename = 'reporte_movimientos.pdf';
      const disposition = response.headers['content-disposition'];
      if (disposition && disposition.indexOf('filename=') !== -1) {
        const matches = /filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/.exec(disposition);
        if (matches != null && matches[1]) { 
          filename = matches[1].replace(/['"]/g, '');
        }
      }
      link.setAttribute('download', filename);
      document.body.appendChild(link);
      link.click();
      
      // Limpieza
      link.parentNode?.removeChild(link);
      window.URL.revokeObjectURL(url);

    } catch (err: unknown) {
      console.error(err);
      setError('Error al generar el reporte. Verifica tu conexión o intenta con un rango menor.');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="max-w-4xl mx-auto p-6">
      <div className="mb-8">
        <h1 className="text-2xl font-semibold text-gray-800 flex items-center gap-3">
          <BarChart3 className="text-primary-600" size={28} />
          Reportes y Análisis
        </h1>
        <p className="text-gray-500 mt-1">Genera reportes detallados en PDF sobre el flujo de inventario.</p>
      </div>

      <div className="bg-surface border border-border rounded-xl p-6 shadow-sm">
        <h2 className="text-lg font-medium text-gray-800 mb-6 flex items-center gap-2">
          <Filter size={20} className="text-gray-400"/>
          Parámetros del Reporte de Movimientos
        </h2>

        {error && (
          <div className="bg-red-500/10 border border-red-500/50 text-red-500 p-4 rounded-lg mb-6 text-sm">
            {error}
          </div>
        )}

        <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-8">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">Fecha y Hora de Inicio</label>
            <div className="relative">
              <input
                type="datetime-local"
                value={startDate}
                onChange={(e) => setStartDate(e.target.value)}
                className="w-full pl-10 pr-4 py-2 border border-border rounded-lg focus:ring-2 focus:ring-primary-500 outline-none"
              />
              <Calendar className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" size={18} />
            </div>
          </div>
          
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">Fecha y Hora de Fin</label>
            <div className="relative">
              <input
                type="datetime-local"
                value={endDate}
                onChange={(e) => setEndDate(e.target.value)}
                className="w-full pl-10 pr-4 py-2 border border-border rounded-lg focus:ring-2 focus:ring-primary-500 outline-none"
              />
              <Calendar className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" size={18} />
            </div>
          </div>

          <div className="md:col-span-2">
            <label className="block text-sm font-medium text-gray-700 mb-2">Categoría (Opcional)</label>
            <select
              value={categoryId}
              onChange={(e) => setCategoryId(e.target.value)}
              className="w-full px-4 py-2 border border-border rounded-lg focus:ring-2 focus:ring-primary-500 outline-none bg-white"
            >
              <option value="">Todas las categorías</option>
              {categories.map(cat => (
                <option key={cat.id} value={cat.id}>{cat.name}</option>
              ))}
            </select>
            <p className="text-xs text-gray-500 mt-2">
              Deja esta opción como "Todas" para incluir el inventario global, o selecciona una categoría para acotar la búsqueda.
            </p>
          </div>
        </div>

        <div className="flex justify-end pt-4 border-t border-border">
          <button
            onClick={handleDownloadReport}
            disabled={isLoading}
            className="btn-primary flex items-center gap-2 px-6 py-2"
          >
            {isLoading ? (
              <>
                <div className="w-5 h-5 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                <span>Generando PDF...</span>
              </>
            ) : (
              <>
                <Download size={20} />
                <span>Descargar Reporte PDF</span>
              </>
            )}
          </button>
        </div>
      </div>
    </div>
  );
};
