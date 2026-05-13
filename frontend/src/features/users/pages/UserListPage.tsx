import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { getUsers, deleteUser } from '../services/userService';
import { Link } from 'react-router-dom';
import { Badge } from '../../../components/badge/Badge';
import toast from 'react-hot-toast';
import { DataTable, Column } from '@/components/ui/DataTable';
import ErrorBoundary from '@/components/ui/ErrorBoundary';
import { UserDto } from 'src/types';

const UserListPage = () => {
  const queryClient = useQueryClient();

  const { data: users, isLoading, isError, refetch } = useQuery({
    queryKey: ['users'],
    queryFn: getUsers,
  });

  const deleteUserMutation = useMutation({
    mutationFn: deleteUser,
    onSuccess: () => {
      toast.success('Usuario eliminado correctamente');
      queryClient.invalidateQueries({ queryKey: ['users'] });
    },
    onError: () => {
      toast.error('Error al eliminar el usuario');
    },
  });

  const handleDelete = (id: number) => {
    if (window.confirm('¿Seguro que deseas eliminar este usuario?')) {
      deleteUserMutation.mutate(id);
    }
  };

  const columns: Column<UserDto>[] = [
    { header: 'Usuario', accessorKey: 'username' },
    { header: 'Email', accessorKey: 'email' },
    { header: 'Nombre completo', accessorKey: 'fullName' },
    {
      header: 'Roles',
      cell: (user: UserDto) => (
        <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
          {user.roles.map((role: string) => (
            <Badge key={role} variant="info">{role}</Badge>
          ))}
        </div>
      ),
    },
    {
      header: 'Acciones',
      align: 'right',
      cell: (user) => (
        <div className="actions-cell">
          <Link to={`/users/edit/${user.id}`} className="btn-action btn-action-edit">Editar</Link>
          <button onClick={() => handleDelete(user.id)} className="btn-action btn-action-delete">Eliminar</button>
        </div>
      ),
    },
  ];

  return (
    <div className="list-page-container">
      <div className="page-header">
        <h1 className="page-title">Gestión de usuarios</h1>
        <Link to="/users/new" className="btn-create">
          Crear usuario
        </Link>
      </div>

      <ErrorBoundary
        fallbackMessage="Error al cargar la lista de usuarios. Asegúrese de que el servidor está en funcionamiento."
        onRetry={refetch}
      >
        <DataTable
          columns={columns}
          data={users || []}
          isLoading={isLoading}
          isError={isError}
          loadingMessage="Cargando usuarios..."
          keyExtractor={(user) => user.id}
        />
      </ErrorBoundary>
    </div>
  );
};

export default UserListPage;
