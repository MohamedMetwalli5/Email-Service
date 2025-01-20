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

    return (
        <AppContext.Provider value={{ sharedUserEmail, setSharedUserEmail, sharedMailBoxOption, setSharedMailBoxOption, sharedEmailToFullyView, setSharedEmailToFullyView }}>
            {children}
        </AppContext.Provider>
    );
};

export { AppContext, DataProvider };