export type MovementType = 'IN' | 'OUT';

export interface StockMovement {
  id: string;
  productId: string;
  productName: string;
  movementType: MovementType;
  previousQuantity: number;
  newQuantity: number;
  date: string; // ISO datetime
  username: string;
  observations?: string;
}

export interface StockMovementRequestDTO {
  productId: string;
  movementType: MovementType;
  quantity: number;
  observations?: string;
}
