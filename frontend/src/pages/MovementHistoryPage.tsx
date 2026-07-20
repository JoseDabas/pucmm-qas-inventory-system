import { StockMovementList } from '../components/StockMovementList';

/**
 * Página del Historial de Movimientos. Solo compone: envuelve la tabla de
 * movimientos de stock existente.
 */
export const MovementHistoryPage: React.FC = () => {
  return <StockMovementList />;
};
