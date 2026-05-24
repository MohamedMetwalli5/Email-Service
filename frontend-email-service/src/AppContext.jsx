import React, { createContext, useContext, useState } from 'react';
import { Toaster } from 'react-hot-toast';

const AppContext = createContext(null);

const DataProvider = ({ children }) => {
  const [authToken, setAuthTokenState] = useState(
    () => localStorage.getItem('authToken') || null
  );
  const [refreshToken, setRefreshTokenState] = useState(
    () => localStorage.getItem('refreshToken') || null
  );
  const [sharedUserEmail, setSharedUserEmailState] = useState(
    () => localStorage.getItem('sharedUserEmail') || null
  );
  const [sharedMailBoxOption, setSharedMailBoxOptionState] = useState(
    () => localStorage.getItem('sharedMailBoxOption') || 'Inbox'
  );
  const [sharedEmailToFullyView, setSharedEmailToFullyViewState] = useState(
    () => {
      const raw = localStorage.getItem('sharedEmailToFullyView');
      try { return raw ? JSON.parse(raw) : null; }
      catch { return null; }
    }
  );
  const [sharedUserLanguage, setSharedUserLanguageState] = useState(
    () => localStorage.getItem('sharedUserLanguage') || 'en'
  );

  const setAuthToken = (token) => {
    setAuthTokenState(token);
    if (token) localStorage.setItem('authToken', token);
    else localStorage.removeItem('authToken');
  };

  const setRefreshToken = (token) => {
    setRefreshTokenState(token);
    if (token) localStorage.setItem('refreshToken', token);
    else localStorage.removeItem('refreshToken');
  };

  const setSharedUserEmail = (email) => {
    setSharedUserEmailState(email);
    if (email) localStorage.setItem('sharedUserEmail', email);
    else localStorage.removeItem('sharedUserEmail');
  };

  const setSharedMailBoxOption = (option) => {
    setSharedMailBoxOptionState(option);
    localStorage.setItem('sharedMailBoxOption', option);
  };

  const setSharedEmailToFullyView = (email) => {
    setSharedEmailToFullyViewState(email);
    // JSON.stringify before storing (prevents "[object Object]" on reload)
    localStorage.setItem('sharedEmailToFullyView', JSON.stringify(email));
  };

  const setSharedUserLanguage = (lang) => {
    setSharedUserLanguageState(lang);
    localStorage.setItem('sharedUserLanguage', lang);
  };

  // clearSession resets both localStorage and context state atomically
  const clearSession = () => {
    setAuthToken(null);
    setRefreshToken(null);
    setSharedUserEmailState(null);
    localStorage.removeItem('authToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('sharedUserEmail');
    localStorage.removeItem('sharedMailBoxOption');
    localStorage.removeItem('sharedEmailToFullyView');
    localStorage.removeItem('sharedUserLanguage');
  };

  return (
    <AppContext.Provider value={{
      authToken, setAuthToken,
      refreshToken, setRefreshToken,
      sharedUserEmail, setSharedUserEmail,
      sharedMailBoxOption, setSharedMailBoxOption,
      sharedEmailToFullyView, setSharedEmailToFullyView,
      sharedUserLanguage, setSharedUserLanguage,
      clearSession,
    }}>
      <Toaster position="top-center" />
      {children}
    </AppContext.Provider>
  );
};

const useAppContext = () => useContext(AppContext);

const AppProvider = DataProvider;

export { AppContext, DataProvider, AppProvider, useAppContext };
