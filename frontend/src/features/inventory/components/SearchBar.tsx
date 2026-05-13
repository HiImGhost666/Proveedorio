import './SearchBar.css';

interface SearchBarProps {
  onSearch: (query: string, productCategory?: string) => void;
  isSyncing: boolean;
}

const SearchBar = ({ onSearch, isSyncing }: SearchBarProps) => {
  const handleSearch = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const formData = new FormData(e.currentTarget);
    const query = formData.get('search') as string;
    const productCategory = (formData.get('productCategory') as string) || undefined;
    onSearch(query, productCategory);
  };

  return (
    <div className="search-bar-container">
      <form onSubmit={handleSearch} className="search-form">
        <input
          type="search"
          name="search"
          placeholder="Buscar por MPN, Modelo, Marca..."
          className="search-input"
          disabled={isSyncing}
        />
        <input
          type="text"
          name="productCategory"
          placeholder="Categoría (PC, impresora, móvil...)"
          className="search-input"
          disabled={isSyncing}
        />
      </form>
    </div>
  );
};

export default SearchBar;
