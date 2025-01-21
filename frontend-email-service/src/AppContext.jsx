import React, { createContext, useState, useEffect } from 'react';

const AppContext = createContext();

const DataProvider = ({ children }) => {

    // Retrieving the initial value for sharedUserEmail from local storage or set it to an empty string
    const [sharedUserEmail, setSharedUserEmail] = useState(() => {
        return localStorage.getItem('sharedUserEmail') || "";
    });

    // Retrieving the initial value for sharedMailBoxOption from local storage or set it to "Inbox"
    const [sharedMailBoxOption, setSharedMailBoxOption] = useState(() => {
        return localStorage.getItem('sharedMailBoxOption') || "Inbox";
    });

    // Retrieving the initial value for sharedEmailToFullyView from local storage or set it to an empty object
    const [sharedEmailToFullyView, setSharedEmailToFullyView] = useState(() => {
        return localStorage.getItem('sharedEmailToFullyView') || {};
    });
    
    // Retrieving the initial value for authToken from local storage or null
    const [authToken, setAuthToken] = useState(() => {
        return localStorage.getItem('authToken') || null;
      });

    // Retrieving the initial value for sharedUserLanguage from local storage or null
    const [sharedUserLanguage, setSharedUserLanguage] = useState(() => {
        return localStorage.getItem('sharedUserLanguage') || "English";
      });

    // Updating the local storage whenever the sharedUserEmail state changes
    useEffect(() => {
        localStorage.setItem('sharedUserEmail', sharedUserEmail);
    }, [sharedUserEmail]);

    // Updating the local storage whenever the sharedMailBoxOption state changes
    useEffect(() => {
        localStorage.setItem('sharedMailBoxOption', sharedMailBoxOption);
    }, [sharedMailBoxOption]);

    // Updating the local storage whenever the sharedEmailToFullyView state changes
    useEffect(() => {
        localStorage.setItem('sharedEmailToFullyView', sharedEmailToFullyView);
    }, [sharedEmailToFullyView]);

    // Updating the local storage whenever the authToken state changes
    useEffect(() => {
        localStorage.setItem('authToken', authToken);
      }, [authToken]);

    // Updating the local storage whenever the sharedUserLanguage state changes
    useEffect(() => {
    localStorage.setItem('sharedUserLanguage', sharedUserLanguage);
    }, [sharedUserLanguage]);
      
    return (
        <AppContext.Provider value={{ sharedUserEmail, setSharedUserEmail, sharedMailBoxOption, setSharedMailBoxOption, sharedEmailToFullyView, setSharedEmailToFullyView , authToken, setAuthToken, sharedUserLanguage, setSharedUserLanguage}}>
            {children}
        </AppContext.Provider>
    );
};

export { AppContext, DataProvider };