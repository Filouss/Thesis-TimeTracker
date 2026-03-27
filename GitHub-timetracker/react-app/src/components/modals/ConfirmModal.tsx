import { useState } from "react";

type Props = {
  title: string,
  body: string
  onConfirm: (notes?: string) => void
  onCancel: () => void
  noteFieldDisplayed: boolean
}

export function ConfirmModal({title ,body, onConfirm, onCancel, noteFieldDisplayed }: Props) {
  const [notes, setNotes] = useState("");

  return (
    <div className="modal-overlay" onClick={onCancel}>
      <div className="modal confirm" onClick={e => e.stopPropagation()}>

        <p>{title}</p>

        <span className="confirm-body">{body}</span>

        {noteFieldDisplayed && (
          <textarea 
            placeholder="Enter session notes..."
            value={notes}
            onChange={(e) => setNotes(e.target.value)}
          />
        )}

        <div className="modal-actions">
          <button onClick={onCancel} className="modal-cancel">Cancel</button>
          <button onClick={() => onConfirm(notes || undefined)} className="modal-confirm">Confirm</button>
        </div>

      </div>
    </div>
  )
}