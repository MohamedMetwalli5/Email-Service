import React, { useState, useEffect } from "react";
import { FaRegTrashCan } from "react-icons/fa6";
import axios from "axios";
import { useContext } from 'react';
import { AppContext } from '../../AppContext.jsx';

const EmailFullView = () => {

  const backendUrl = import.meta.env.VITE_BACKEND_API_URL;
  
  const { authToken } = useContext(AppContext);
  const { sharedMailBoxOption } = useContext(AppContext);
  const { sharedEmailToFullyView } = useContext(AppContext);
  const { sharedUserEmail } = useContext(AppContext);

  const [email, setEmail] = useState({});
  const [isMovedToTrash, setIsMovedToTrash] = useState(false);
  const [isDeletedPermanently, setIsDeletedPermanently] = useState(false);

  const getPriorityColor = (priority) => {
    switch (priority) {
      case 1:
        return "text-red-400";
      case 2:
        return "text-yellow-400";
      case 3:
        return "text-green-400";
      default:
        return "text-gray-400";
    }
  };

  const moveToTrash = async() => {
    if(email == {}){
      return;
    }
    try {
      const response = await axios.post(`${backendUrl}/moveemailtotrashbox`, email, {
        headers: {
          Authorization: `Bearer ${authToken}`,
        },
      });
      console.log(`Email with id ${email.emailID} moved to the trash box`, response.data);
      setEmail({});
      setIsMovedToTrash(true);
    } catch (error) {
      console.error(error.response?.data || error.message);
    }
  };
  
  const deletePermanently = async() => {
    try {
      const response = await axios.post(`${backendUrl}/deleteemail`, email, {
        headers: {
          Authorization: `Bearer ${authToken}`,
        },
      });
      console.log(response.data);
      setEmail({});
      setIsDeletedPermanently(true);
    } catch (error) {
      console.error(error.response?.data || error.message);
    }
  }

  useEffect(() => {
    setIsDeletedPermanently(false);
    setIsMovedToTrash(false);
    setEmail(sharedEmailToFullyView);
  }, [sharedEmailToFullyView]);

  if(isDeletedPermanently || isMovedToTrash){
    return (
      <div className="h-full w-full bg-gray-900 text-gray-100 rounded-lg shadow-md p-6">
        {isDeletedPermanently?
          <h2 className="text-xl font-semibold mb-4 text-red-400">This email has been deleted</h2>
          :
          <h2 className="text-xl font-semibold mb-4 text-red-400">This email has been moved to trashbox</h2>
        }
      </div>
    );
  }

  return (
    <div className="h-full w-full bg-gray-900 text-gray-100 rounded-lg shadow-md p-6 relative">
      {email.emailID && (sharedMailBoxOption === "Inbox" || sharedMailBoxOption === "Trashbox")? (
        <button
          onClick={() =>
            email.trash === "No"? moveToTrash() : deletePermanently()
          }
          className="absolute top-4 right-4 text-gray-400 hover:text-red-500 transition-all duration-300"
        >
          <FaRegTrashCan size={20} />
        </button>
      ):("")}
      
      {email.sender? (
        <>
          <h2 className="text-xl font-semibold mb-4 mr-4">{email.subject}</h2>
          <div className="space-y-4">
            <div className="flex justify-between items-center">
              <p className="text-sm font-medium">
                <span className="text-blue-400">Sender:</span> {email.sender}
              </p>
              <span className="text-sm text-gray-400 ml-2">{email.date}</span>
            </div>

            <div className="flex justify-between items-center mt-2">
              <p className="text-sm font-medium">
                <span className="text-blue-400">Priority:{' '}</span>
                <span className={getPriorityColor(email.priority)}>
                  {email.priority}
                </span>
              </p>
            </div>

            <div className="border-t border-gray-700 my-2"></div>

            <div className="mt-4 rounded p-2">
              <p className="text-sm mt-2">{email.body}</p>
            </div>
          </div>
        </>
      ):("")}
    </div>
  );
};

export default EmailFullView;