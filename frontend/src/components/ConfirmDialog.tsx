import { useState } from 'react';
import { AlertTriangle } from 'lucide-react';

interface ConfirmDialogProps {
  title: string;
  message: string;
  confirmLabel?: string;
  cancelLabel?: string;
  requiredInputText?: string;
  onConfirm: () => void;
  onCancel: () => void;
}

/**
 * Modal de confirmación reutilizable (sí/no). Sustituye al window.confirm nativo
 * para mantener un estilo coherente con el resto de la aplicación.
 */
export const ConfirmDialog: React.FC<ConfirmDialogProps> = ({
  title,
  message,
  confirmLabel = 'Confirmar',
  cancelLabel = 'Cancelar',
  requiredInputText,
  onConfirm,
  onCancel,
}) => {
  const [inputValue, setInputValue] = useState('');
  
  const isConfirmDisabled = requiredInputText ? inputValue !== requiredInputText : false;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4">
      <div className="bg-surface border border-border w-full max-w-md rounded-2xl shadow-xl p-6">
        <div className="flex items-start gap-4">
          <div className="shrink-0 w-10 h-10 rounded-full bg-red-50 flex items-center justify-center">
            <AlertTriangle className="text-red-600" size={20} />
          </div>
          <div>
            <h2 className="text-lg font-semibold text-gray-800">{title}</h2>
            <p className="text-sm text-gray-500 mt-1">{message}</p>
          </div>
        </div>

        {requiredInputText && (
          <div className="mt-4">
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Escribe "{requiredInputText}" para confirmar:
            </label>
            <input
              type="text"
              value={inputValue}
              onChange={(e) => setInputValue(e.target.value)}
              className="w-full form-input"
              autoFocus
            />
          </div>
        )}

        <div className="mt-6 flex justify-end gap-3">
          <button type="button" onClick={onCancel} className="btn-secondary">
            {cancelLabel}
          </button>
          <button
            type="button"
            onClick={onConfirm}
            disabled={isConfirmDisabled}
            data-testid="confirm-delete-button"
            className="btn-danger disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {confirmLabel}
          </button>
        </div>
      </div>
    </div>
  );
};
