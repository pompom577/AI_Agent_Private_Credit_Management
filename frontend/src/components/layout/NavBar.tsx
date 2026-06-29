import { NavLink } from 'react-router-dom';

const NAV_ITEMS = [
  { to: '/', label: 'Deal Ingestion', end: true },
  { to: '/term-sheet-review', label: 'Term Sheet Review' },
  { to: '/ocr-review', label: 'OCR Review' },
];

export default function NavBar() {
  return (
    <nav
      className="sticky top-0 z-20 border-b backdrop-blur-sm"
      style={{
        backgroundColor: 'rgba(248, 247, 245, 0.92)',
        borderColor: 'var(--color-border)',
      }}
    >
      <div className="mx-auto flex h-14 max-w-7xl items-center justify-between px-6">
        <div className="flex items-center gap-2.5">
          <span
            className="flex h-8 w-8 items-center justify-center rounded-lg text-xs font-bold text-white"
            style={{ backgroundColor: 'var(--color-accent)' }}
            aria-hidden="true"
          >
            DI
          </span>
          <span
            className="text-base font-semibold tracking-tight"
            style={{
              fontFamily: 'var(--font-display)',
              color: 'var(--color-text-primary)',
            }}
          >
            Deal Ingestion Platform
          </span>
        </div>

        <div className="flex items-center gap-1" role="navigation" aria-label="Main navigation">
          {NAV_ITEMS.map(({ to, label, end }) => (
            <NavLink
              key={to}
              to={to}
              end={end}
              className="rounded-md px-3 py-1.5 text-sm font-medium transition-colors"
              style={({ isActive }) => ({
                backgroundColor: isActive ? 'var(--color-accent)' : 'transparent',
                color: isActive ? '#fff' : 'var(--color-text-muted)',
              })}
            >
              {label}
            </NavLink>
          ))}
        </div>
      </div>
    </nav>
  );
}
