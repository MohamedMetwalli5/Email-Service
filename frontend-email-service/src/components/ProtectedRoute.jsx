import { useContext } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { AppContext } from '../AppContext.jsx';

const ProtectedRoute = ({ children }) => {
  const { authToken } = useContext(AppContext);
  const { search } = useLocation();
  const hasDiscordCode = new URLSearchParams(search).has('code');

  if (!authToken && !hasDiscordCode) {
    return <Navigate to="/sign-in" replace />;
  }

  return children;
};

export default ProtectedRoute;
