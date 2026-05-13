import { useState, useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { getUserDetails, createUser, updateUser } from '../services/userService';
import { useParams, useNavigate, Link } from 'react-router-dom';
import type { AxiosError } from 'axios';
import { UserRequestDto } from 'src/types';
import { useFieldErrors } from 'src/hooks/useFieldErrors';
import { FieldError } from 'src/components/form/FieldError';
import { FormSection } from 'src/components/form/FormSection';
import { FormGrid } from 'src/components/form/FormGrid';
import { FormActions } from 'src/components/form/FormActions';
import './UserFormPage.css';
import toast from 'react-hot-toast';

const allRoles = ['ROLE_ADMIN', 'ROLE_CLIENTE'];
type BackendError = { message?: string; errors?: Record<string, string> };

/**
 * Página de formulario para crear o editar usuarios.
 * Gestiona la carga inicial, el envío al backend y la visualización de errores de validación.
 */
const UserFormPage = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const isEditMode = !!id;

  const [formData, setFormData] = useState<UserRequestDto>({
    id: undefined,
    username: '',
    email: '',
    fullName: '',
    password: '',
    roles: ['ROLE_CLIENTE'],
  });

  const {
    errors,
    setFieldError,
    clearFieldError,
    clearAllErrors,
    bindField,
  } = useFieldErrors<'username' | 'email' | 'fullName' | 'password'>();

  const { data: existingUser, isLoading } = useQuery({
    queryKey: ['user', id],
    queryFn: () => getUserDetails(Number(id)),
    enabled: isEditMode,
  });

  useEffect(() => {
    if (isEditMode && existingUser) {
      setFormData({
        id: existingUser.id,
        username: existingUser.username,
        email: existingUser.email,
        fullName: existingUser.fullName,
        password: '', // Password should not be pre-filled
        roles: existingUser.roles,
      });
    }
  }, [isEditMode, existingUser]);

  const mutation = useMutation({
    mutationFn: (user: UserRequestDto) => {
      // Clean up password field if it's empty
      const userData = { ...user };
      if (userData.password === '') {
        delete userData.password;
      }
      if (isEditMode) {
        return updateUser(Number(id), userData);
      } else {
        return createUser(userData);
      }
    },
    onSuccess: () => {
      clearAllErrors();
      queryClient.invalidateQueries({ queryKey: ['users'] });
      toast.success(isEditMode ? 'Usuario actualizado correctamente' : 'Usuario creado correctamente');
      navigate('/users');
    },
    onError: (error) => {
      // Intentamos mapear errores de validación por campo si vienen del backend
      const typedError = error as AxiosError<BackendError> & Error;
      const fieldErrors = typedError.response?.data?.errors;

      if (fieldErrors && typeof fieldErrors === 'object') {
        Object.entries(fieldErrors).forEach(([field, message]) => {
          setFieldError(field as 'username' | 'email' | 'fullName' | 'password', String(message));
        });
      } else {
        toast.error(`Error al guardar el usuario: ${typedError.response?.data?.message || typedError.message}`);
      }
    },
  });

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
    clearFieldError(name as 'username' | 'email' | 'fullName' | 'password');
  };

  const handleRoleChange = (role: string) => {
    setFormData((prev) => ({ ...prev, roles: [role] }));
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (formData.roles.length === 0) {
      toast.error('Un usuario debe tener al menos un rol.');
      return;
    }
    mutation.mutate(formData);
  };

  if (isLoading && isEditMode) {
    return <div className="p-8 text-center text-white">Cargando formulario...</div>;
  }

  return (
    <div className="user-form-page">
      <div className="form-container">
        <div className="form-header">
          <h1 className="form-title">{isEditMode ? 'Actualizar usuario' : 'Crear usuario'}</h1>
          <Link to="/users" className="back-link">← Volver al listado</Link>
        </div>

        <form onSubmit={handleSubmit} className="user-form">
          <FormSection title="Información del usuario">
            <FormGrid>
              <div className="form-group">
                <label htmlFor="username">Usuario</label>
                <input
                  id="username"
                  name="username"
                  value={formData.username}
                  onChange={handleInputChange}
                  required
                  className="form-input"
                  {...bindField('username')}
                />
                <FieldError field="username" message={errors.username} />
              </div>
              <div className="form-group">
                <label htmlFor="email">Email</label>
                <input
                  id="email"
                  name="email"
                  type="email"
                  value={formData.email}
                  onChange={handleInputChange}
                  required
                  className="form-input"
                  {...bindField('email')}
                />
                <FieldError field="email" message={errors.email} />
              </div>
              <div className="form-group">
                <label htmlFor="fullName">Nombre completo</label>
                <input
                  id="fullName"
                  name="fullName"
                  value={formData.fullName ?? ''}
                  onChange={handleInputChange}
                  className="form-input"
                  {...bindField('fullName')}
                />
                <FieldError field="fullName" message={errors.fullName} />
              </div>
              <div className="form-group">
                <label htmlFor="password">Contraseña</label>
                <input
                  id="password"
                  name="password"
                  type="password"
                  value={formData.password ?? ''}
                  onChange={handleInputChange}
                  placeholder={isEditMode ? 'Deja en blanco para conservar la actual' : ''}
                  required={!isEditMode}
                  className="form-input"
                  {...bindField('password')}
                />
                <FieldError field="password" message={errors.password} />
              </div>
            </FormGrid>
          </FormSection>

          <FormSection title="Roles">
            <div className="roles-checkbox-group">
              {allRoles.map((role) => (
                <label key={role} className="role-label">
                  <input
                    type="radio"
                    name="user-role"
                    checked={formData.roles.includes(role)}
                    onChange={() => handleRoleChange(role)}
                  />
                  {role}
                </label>
              ))}
            </div>
          </FormSection>

          <FormActions>
            <button type="submit" disabled={mutation.isPending} className="btn-primary">
              {mutation.isPending ? 'Guardando...' : (isEditMode ? 'Actualizar usuario' : 'Crear usuario')}
            </button>
          </FormActions>
        </form>
      </div>
    </div>
  );
};

export default UserFormPage;
