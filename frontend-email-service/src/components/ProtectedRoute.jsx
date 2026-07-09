import { useContext } from 'react';
import { Navigate } from 'react-router-dom';
import { AppContext } from '../AppContext.jsx';

const ProtectedRoute = ({ children }) => {
  const { authToken } = useContext(AppContext);

  if (!authToken) {
    return <Navigate to="/sign-in" replace />;
  }

  return children;
};

export default ProtectedRoute;
