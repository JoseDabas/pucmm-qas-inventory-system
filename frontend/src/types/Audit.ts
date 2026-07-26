export interface ProductAuditEntry {
  revision: number;
  entityId: string;
  revisionType: 'CREATED' | 'UPDATED' | 'DELETED';
  revisionDate: string;
  name?: string;
  skuCode?: string;
  description?: string;
  category?: string;
  price?: number;
  initialQuantity?: number;
  minimumStock?: number;
  isActive?: boolean;
}

export interface StockMovementAuditEntry {
  revision: number;
  entityId: string;
  revisionType: 'CREATED' | 'UPDATED' | 'DELETED';
  revisionDate: string;
  productId?: string;
  movementType?: string;
  previousQuantity?: number;
  newQuantity?: number;
  movementDate?: string;
  username?: string;
  observations?: string;
}
