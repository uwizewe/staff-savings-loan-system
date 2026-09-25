import { useEffect, useId, useLayoutEffect, useRef, useState } from 'react';
import { createPortal } from 'react-dom';
import { ChevronDown, Loader2, MoreHorizontal } from 'lucide-react';

export default function ActionMenu({ label, title, subtitle, items, onAction }) {
  const [open, setOpen] = useState(false);
  const [busy, setBusy] = useState(false);
  const [position, setPosition] = useState({ top: 0, left: 0 });
  const trigger = useRef(null), panel = useRef(null), initialFocus = useRef(0);
  const id = useId();
  const close = (restore = false) => { setOpen(false); if (restore) trigger.current?.focus(); };

  useLayoutEffect(() => {
    if (!open) return;
    const anchor = trigger.current.getBoundingClientRect();
    const menu = panel.current.getBoundingClientRect();
    setPosition({ left: Math.max(8, Math.min(anchor.right - menu.width, window.innerWidth - menu.width - 8)), top: Math.max(8, anchor.bottom + menu.height + 8 <= window.innerHeight ? anchor.bottom + 8 : anchor.top - menu.height - 8) });
    const buttons = panel.current.querySelectorAll('[role="menuitem"]');
    buttons[initialFocus.current === -1 ? buttons.length - 1 : 0]?.focus();
  }, [open]);

  useEffect(() => {
    if (!open) return;
    const outside = event => { if (!panel.current?.contains(event.target) && !trigger.current?.contains(event.target)) close(); };
    const reposition = event => { if (!panel.current?.contains(event.target)) close(true); };
    document.addEventListener('pointerdown', outside);
    document.addEventListener('focusin', outside);
    window.addEventListener('resize', reposition);
    window.addEventListener('scroll', reposition, true);
    return () => { document.removeEventListener('pointerdown', outside); document.removeEventListener('focusin', outside); window.removeEventListener('resize', reposition); window.removeEventListener('scroll', reposition, true); };
  }, [open]);

  const keyboard = event => {
    if (event.key === 'Escape') { event.preventDefault(); close(true); }
    if (event.key === 'Tab') close(true);
    if (['ArrowDown', 'ArrowUp', 'Home', 'End'].includes(event.key)) {
      event.preventDefault();
      const buttons = [...panel.current.querySelectorAll('[role="menuitem"]')];
      const current = buttons.indexOf(document.activeElement);
      const next = event.key === 'Home' ? 0 : event.key === 'End' ? buttons.length - 1 : (current + (event.key === 'ArrowDown' ? 1 : -1) + buttons.length) % buttons.length;
      buttons[next]?.focus();
    }
  };
  const choose = async key => { close(true); setBusy(true); try { await onAction(key); } finally { setBusy(false); } };

  return <>
    <button ref={trigger} type="button" aria-label={label} aria-haspopup="menu" aria-expanded={open} aria-controls={open ? id : undefined} disabled={busy}
      onClick={() => { initialFocus.current = 0; setOpen(value => !value); }}
      onKeyDown={event => { if (['ArrowDown', 'ArrowUp'].includes(event.key)) { event.preventDefault(); initialFocus.current = event.key === 'ArrowUp' ? -1 : 0; setOpen(true); } }}
      className={`inline-flex min-h-10 items-center gap-2 rounded-xl border px-3 py-2 text-xs font-semibold shadow-sm transition focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-teal-600 disabled:opacity-50 ${open ? 'border-teal-300 bg-teal-50 text-teal-800' : 'border-slate-200 bg-white text-slate-600 hover:border-teal-300 hover:bg-teal-50 hover:text-teal-800'}`}>
      {busy ? <Loader2 size={16} className="animate-spin" aria-hidden="true"/> : <MoreHorizontal size={17} aria-hidden="true"/>}Actions<ChevronDown size={13} aria-hidden="true" className={`transition-transform ${open ? 'rotate-180' : ''}`}/>
    </button>
    {open && createPortal(<div ref={panel} id={id} role="menu" aria-label={label} onKeyDown={keyboard} style={position}
      className="fixed z-[70] w-72 max-w-[calc(100vw-16px)] max-h-[calc(100dvh-16px)] overflow-y-auto rounded-2xl border border-slate-200 bg-white p-1.5 shadow-[0_16px_48px_-12px_rgba(15,23,42,0.28)]">
      <div role="presentation" className="mb-1 border-b border-slate-100 px-3 py-3"><p className="truncate text-sm font-semibold text-slate-900">{title}</p><p className="mt-1 truncate text-[11px] text-slate-500">{subtitle}</p></div>
      {items.map(({ key, label: itemLabel, description, icon: Icon, tone = 'bg-teal-50 text-teal-700', separator }) => <div role="none" key={key} className={separator ? 'mt-1 border-t border-slate-100 pt-1' : ''}>
        <button type="button" role="menuitem" tabIndex={-1} onClick={() => choose(key)} className="group flex min-h-14 w-full items-center gap-3 rounded-xl px-3 py-2.5 text-left transition hover:bg-slate-50 focus:bg-teal-50 focus:outline-none">
          <span aria-hidden="true" className={`grid size-9 shrink-0 place-items-center rounded-xl ${tone}`}><Icon size={17}/></span>
          <span className="min-w-0"><span className="block text-xs font-semibold text-slate-800">{itemLabel}</span><span className="mt-0.5 block text-[11px] text-slate-500">{description}</span></span>
        </button>
      </div>)}
    </div>, document.body)}
  </>;
}
