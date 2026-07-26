import api from './axios';
import type { ProductAuditEntry, StockMovementAuditEntry } from '../types/Audit';

export const getProductAuditHistory = async (): Promise<ProductAuditEntry[]> => {
  const response = await api.get('/api/v1/audit/products');
  return response.data;
};

export const getStockMovementAuditHistory = async (): Promise<StockMovementAuditEntry[]> => {
  const response = await api.get('/api/v1/audit/stock-movements');
  return response.data;
};
