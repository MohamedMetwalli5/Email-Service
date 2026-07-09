import React from 'react';
import apiClient from '../api/apiClient';
import { FaDiscord } from "react-icons/fa6";
import { useTranslation } from 'react-i18next';

const SignInWithDiscord = () => {
    const { t } = useTranslation();
    const discordClientID = import.meta.env.VITE_CLIENT_ID;
    const redirectURI = import.meta.env.VITE_DISCORD_REDIRECT_URI;

    const handleSignIn = async () => {
        const redirectUri = encodeURIComponent(redirectURI);
        const scopes = encodeURIComponent("identify email");
        const clientId = discordClientID;

        // Fetch a CSRF state nonce from the backend before redirecting.
        // The backend stores it in Redis and validates it on the callback.
        let state;
        try {
            const { data } = await apiClient.get('/auth/discord/state');
            state = data.state;
        } catch {
            return;
        }

        // Redirecting the user to Discord's authorization endpoint
        window.location.href = `https://discord.com/oauth2/authorize?response_type=code&client_id=${clientId}&redirect_uri=${redirectUri}&scope=${scopes}&state=${state}`;
    };

    return (
        <button
            className="flex items-center justify-center w-full bg-purple-400 hover:bg-purple-500 text-white font-bold py-3 rounded-md transition-all duration-300 focus:outline-none focus:ring-2 focus:ring-purple-300"
            onClick={handleSignIn}
        >
            <FaDiscord className="w-5 h-5 mr-2"/>
            {t('SIGN_IN_WITH_DISCORD')}
        </button>
    );
};

export default SignInWithDiscord;