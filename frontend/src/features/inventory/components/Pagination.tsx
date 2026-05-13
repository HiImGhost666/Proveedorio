import { PaginationProps } from '@/types';
import './Pagination.css';

const MAX_VISIBLE_PAGES = 5;

const getVisiblePages = (page: number, totalPages: number) => {
  if (totalPages <= MAX_VISIBLE_PAGES) {
    return Array.from({ length: totalPages }, (_, index) => index);
  }

  const halfWindow = Math.floor(MAX_VISIBLE_PAGES / 2);
  let start = Math.max(0, page - halfWindow);
  const end = Math.min(totalPages - 1, start + MAX_VISIBLE_PAGES - 1);

  if (end - start + 1 < MAX_VISIBLE_PAGES) {
    start = Math.max(0, end - MAX_VISIBLE_PAGES + 1);
  }

  return Array.from({ length: end - start + 1 }, (_, index) => start + index);
};

const Pagination = ({ page, totalPages, onPageChange }: PaginationProps) => {
  const visiblePages = getVisiblePages(page, totalPages);

  return (
    <div className="pagination-container">
      <button
        onClick={() => onPageChange(page - 1)}
        disabled={page === 0}
        className="pagination-button prev"
      >
        Anterior
      </button>

      <div className="pagination-pages" aria-label="Navegación de páginas">
        {visiblePages[0] > 0 && (
          <>
            <button
              onClick={() => onPageChange(0)}
              className="pagination-button page-number"
            >
              1
            </button>
            {visiblePages[0] > 1 && <span className="pagination-ellipsis">…</span>}
          </>
        )}

        {visiblePages.map((visiblePage) => (
          <button
            key={visiblePage}
            onClick={() => onPageChange(visiblePage)}
            className={`pagination-button page-number ${visiblePage === page ? 'active' : ''}`}
            aria-current={visiblePage === page ? 'page' : undefined}
          >
            {visiblePage + 1}
          </button>
        ))}

        {visiblePages[visiblePages.length - 1] < totalPages - 1 && (
          <>
            {visiblePages[visiblePages.length - 1] < totalPages - 2 && <span className="pagination-ellipsis">…</span>}
            <button
              onClick={() => onPageChange(totalPages - 1)}
              className="pagination-button page-number"
            >
              {totalPages}
            </button>
          </>
        )}
      </div>

      <span className="pagination-info">
        Página {page + 1} de {totalPages}
      </span>

      <button
        onClick={() => onPageChange(page + 1)}
        disabled={page + 1 >= totalPages}
        className="pagination-button next"
      >
        Siguiente
      </button>
    </div>
  );
};

export default Pagination;
