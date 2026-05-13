import { createContext } from 'react';
import type { ErrorContextType } from './ErrorContext';

export const ErrorContext = createContext<ErrorContextType | undefined>(undefined);
