import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';
import { useContext } from 'react';
import { AppContext } from '../AppContext.jsx';
import hash from 'hash.js';


const SignInPage = () => {

  const backendUrl = import.meta.env.VITE_BACKEND_API_URL;
  
  const { setSharedUserEmail } = useContext(AppContext);
  const { setAuthToken } = useContext(AppContext);

  const navigate = useNavigate();

  const [formData, setFormData] = useState({
    email: '',
    password: ''
  });

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData((prevState) => ({
      ...prevState,
      [name]: value
    }));
  };

  const handleHashPassword = (password) => {
    return hash.sha256().update(password).digest('hex');
  };

  const handleSubmit = async(e) => {
    e.preventDefault();
    formData.password = handleHashPassword(formData.password);
    console.log(formData.password);
    try {
      const response = await axios.post(`${backendUrl}/signin`, formData);
      const token = response.data.split(' ')[1];

      setAuthToken(token);
      setSharedUserEmail(formData.email);
      console.log("User signed in and token stored:", token);
      
      navigate("/home");
    } catch (error) {
      console.error("Error adding user:", error.response?.data || error.message);
      alert("An error occurred during sign-in. Please try again.");
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-b from-blue-500 to-blue-600 flex items-center justify-center p-4">
      <div className="w-full max-w-lg bg-white rounded-lg shadow-lg p-8 space-y-6">
        <h2 className="text-4xl font-bold text-center text-blue-700">Seamail</h2>
        <p className="text-center text-gray-600">Welcome back to Seamail! Sign in to continue.</p>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label htmlFor="email" className="block text-lg text-gray-800">Email</label>
            <input
              type="email"
              id="email"
              name="email"
              placeholder="example@seamail.com"
              value={formData.email}
              onChange={handleInputChange}
              className="w-full p-3 mt-2 rounded-md border border-gray-300 focus:outline-none focus:ring-2 focus:ring-blue-500"
              required
            />
          </div>
          <div>
            <label htmlFor="password" className="block text-lg text-gray-800">Password</label>
            <input
              type="password"
              id="password"
              name="password"
              placeholder="••••••••"
              value={formData.password}
              onChange={handleInputChange}
              className="w-full p-3 mt-2 rounded-md border border-gray-300 focus:outline-none focus:ring-2 focus:ring-blue-500"
              required
            />
          </div>
          <div className="flex justify-between items-center">
            <button
              type="submit"
              className="w-full bg-yellow-400 hover:bg-yellow-500 text-white font-bold py-3 rounded-md transition-all duration-300"
            >
              Sign In
            </button>
          </div>
        </form>
        <p className="text-center text-sm text-gray-500">
          Don't have an account?{' '}
          <a href="/" className="text-blue-500 hover:text-blue-700">
            Sign Up
          </a>
        </p>
      </div>
    </div>
  );
};

export default SignInPage;