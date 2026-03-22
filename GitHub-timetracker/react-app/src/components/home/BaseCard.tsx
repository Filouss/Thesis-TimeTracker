type BaseCardProps = {
  header: React.ReactNode;
  meta?: React.ReactNode;
  footer?: React.ReactNode;
  onClick?: () => void;
};

export function BaseCard({ header, meta, footer, onClick }: BaseCardProps) {
  
  return (
    <div className="issue-card" onClick={onClick}>
      <div className="card-row">{header}</div>

      {meta && <div className="card-row">{meta}</div>}

      {footer && <div className="card-row footer">{footer}</div>}
    </div>
  );
}