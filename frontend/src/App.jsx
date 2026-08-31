import React, { useEffect } from 'react';
import AppRoutes from './routes/AppRoutes';
import { fetchMe } from './api/authApi';

function App() {
  useEffect(() => {
    // Attempt to load user from claims on app start
    fetchMe();
  }, []);

  return (
    <AppRoutes />
  );
}

export default App;
