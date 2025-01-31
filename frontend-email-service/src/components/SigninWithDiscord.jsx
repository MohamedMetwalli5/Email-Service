import React, { useEffect, useState } from 'react';
import axios from 'axios';
import { v4 as uuidv4 } from 'uuid';
import { FaDiscord } from "react-icons/fa6";



const SignInWithDiscord = () => {  
    const backendUrl = import.meta.env.VITE_BACKEND_API_URL;
    const discordClientID = import.meta.env.VITE_CLIENT_ID;
    const redirectURI = `${backendUrl}/DiscordSignin`;

    
    const handleSignIn = () => {
        const redirectUri = encodeURIComponent(redirectURI);
        const scopes = encodeURIComponent("identify email");
        const clientId = discordClientID; // The Discord Seamail application client ID
        const state = uuidv4(); // a random unique string securely for the CSRF Issue
        
        // Redirecting the user to Discord's authorization endpoint
        window.location.href = `https://discord.com/oauth2/authorize?response_type=code&client_id=${clientId}&redirect_uri=${redirectUri}&scope=${scopes}&state=${state}`;      
    };

    return (
        <button
            className="flex items-center justify-center w-full bg-purple-400 hover:bg-purple-500 text-white font-bold py-3 rounded-md transition-all duration-300 focus:outline-none focus:ring-2 focus:ring-purple-300"
            onClick={handleSignIn}
        >
            <FaDiscord className="w-5 h-5 mr-2"/>
            Sign In with Discord
        </button>
    );
};

export default SignInWithDiscord;