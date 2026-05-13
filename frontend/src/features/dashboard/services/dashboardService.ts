import api from 'services/api'; 
import { DashboardSummaryDto } from 'src/types';

export const getDashboardSummary = async (): Promise<DashboardSummaryDto> => {
  const response = await api.get('/dashboard/summary');
  return response.data.data; 
};
