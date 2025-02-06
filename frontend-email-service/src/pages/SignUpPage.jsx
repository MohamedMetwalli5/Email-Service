import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';
import { AppContext } from '../AppContext.jsx';
import { useContext } from 'react';
import hash from 'hash.js';
import LeftCharactersSticker from "../assets/LeftCharactersSticker.svg";


const SignUpPage = () => {

  const backendUrl = import.meta.env.VITE_BACKEND_API_URL;

  const { setSharedUserEmail } = useContext(AppContext);
  const { setAuthToken } = useContext(AppContext);

  const navigate = useNavigate();

  const [formData, setFormData] = useState({
    email: '',
    password: '',
    confirmPassword: ''
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

  const handleSubmit = (e) => {
    e.preventDefault();
    if (formData.password.length < 8) {
      alert("Password must be at least 8 characters long!");
      return;
    }else if (formData.password !== formData.confirmPassword) {
      alert("Passwords do not match!");
      return;
    }else if (!formData.email.endsWith("@seamail.com")) {
      alert("Emails must end with @seamail.com");
      return;
    }
    formData.password = handleHashPassword(formData.password);
    signUp();
  };

  const signUp = async() => {
    try {
      const response = await axios.post(`${backendUrl}/signup`, formData);
      const token = response.data.token;

      setAuthToken(token);
      setSharedUserEmail(formData.email);
      console.log("User added and token stored:", token);
      
      navigate("/home");
    } catch (error) {
      console.error("Error adding user:", error.response?.data || error.message);
      alert("An error occurred during sign-up. Please try again.");
    }
  }

  return (
    <div className="min-h-screen bg-gradient-to-b from-blue-500 to-blue-600 flex items-center justify-center p-4">
      <img
        src={LeftCharactersSticker}
        alt=""
        className="w-0 h-0 md:w-80 md:h-80"
      />
      <div className="w-full max-w-lg bg-white rounded-lg shadow-lg p-8 space-y-6">
        <h2 className="text-4xl font-bold text-center text-blue-700">Seamail</h2>
        <p className="text-center text-gray-600">Join Seamail and start exploring!</p>
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
          <div>
            <label htmlFor="confirmPassword" className="block text-lg text-gray-800">Confirm Password</label>
            <input
              type="password"
              id="confirmPassword"
              name="confirmPassword"
              placeholder="••••••••"
              value={formData.confirmPassword}
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
              Sign Up
            </button>
          </div>
        </form>
        <p className="text-center text-sm text-gray-500">
          Already have an account?{' '}
          <a href="/signin" className="text-blue-500 hover:text-blue-700">
            Sign In
          </a>
        </p>
      </div>
    </div>
  );
};

export default SignUpPage;
