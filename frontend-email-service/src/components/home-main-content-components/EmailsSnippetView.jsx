import React, { useState, useEffect } from "react";
import { useContext } from 'react';
import { AppContext } from '../../AppContext.jsx';
import axios from "axios";


const EmailsSnippetView = () => {

  const backendUrl = import.meta.env.VITE_BACKEND_API_URL;

  const { sharedMailBoxOption } = useContext(AppContext);
  const { authToken } = useContext(AppContext);
  const { setSharedEmailToFullyView } = useContext(AppContext);

  const [filterType, setFilterType] = useState("");
  const [filterText, setFilterText] = useState("");
  const [sortType, setSortType] = useState("");
  const [emails, setEmails] = useState([]);

  const getPriorityColor = (priority) => {
    switch (priority) {
      case "1":
        return "text-red-400";
      case "2":
        return "text-yellow-400";
      case "3":
        return "text-green-400";
      default:
        return "text-gray-400";
    }
  };

  const handleSendOptions = () => {
    const options = {
      filterType,
      filterText,
      sortType,
    };

    console.log("Sending options to backend:", options);
  };

  const getEmails = async(sharedMailBoxOption) => {
    try {
      const response = await axios.post(`${backendUrl}/${sharedMailBoxOption.toLowerCase()}`, null, {
        headers: {
          Authorization: `Bearer ${authToken}`,
        },
      });
      setEmails(response.data);
    } catch (error) {
      console.error(error.response?.data || error.message);
    }
  }

  useEffect(() => {
    getEmails(sharedMailBoxOption);
  }, [sharedMailBoxOption]);
  

  return (
    <div className="flex flex-col bg-gray-900 w-fit p-4 rounded-lg h-full text-gray-100">
      <h2 className="text-2xl font-semibold mb-4">{sharedMailBoxOption}</h2>

      <div className="flex space-x-4 mb-4 text-center">
        <div className="flex flex-col">
          <label htmlFor="filter" className="text-sm text-gray-400 mb-1">
            Filter By
          </label>
          <div className="flex space-x-2">
            <select
              id="filter"
              className="bg-gray-800 text-gray-100 p-2 rounded cursor-pointer"
              onChange={(e) => setFilterType(e.target.value)}
            >
              <option value="">None</option>
              <option value="subject">Subject</option>
              <option value="sender">Sender</option>
            </select>
            <input
              type="text"
              className="bg-gray-800 text-gray-100 p-2 rounded"
              placeholder="Search..."
              value={filterText}
              onChange={(e) => setFilterText(e.target.value)}
            />
          </div>
        </div>

        <div className="flex flex-col">
          <label htmlFor="sort" className="text-sm text-gray-400 mb-1">
            Sort By
          </label>
          <select
            id="sort"
            className="bg-gray-800 text-gray-100 p-2 rounded cursor-pointer"
            onChange={(e) => setSortType(e.target.value)}
          >
            <option value="">None</option>
            <option value="priority">Priority</option>
            <option value="date">Date</option>
          </select>
        </div>
      </div>

      <div className="flex justify-center">
        <button
          className="bg-green-500 text-white py-2 px-4 rounded mb-4"
          onClick={handleSendOptions}
        >
          Send
        </button>
      </div>

      <div className="flex-grow h-52 overflow-y-auto">
        <ul className="space-y-4">
          {emails.map((email, index) => (
            <li
              key={email.id || index}
              className="flex flex-col p-4 bg-gray-800 rounded-lg hover:bg-gray-700 transition-colors hover:cursor-pointer"
              onClick={() => setSharedEmailToFullyView(email)}
            >
              <div className="flex justify-between items-center">
                <p className="text-sm font-medium text-gray-200">
                  {(sharedMailBoxOption === "Inbox" || sharedMailBoxOption === "Trashbox") ? (
                    <>
                      <span className="text-blue-400">Sender:</span> {email.sender}
                    </>
                  ) : (
                    <>
                      <span className="text-blue-400">Receiver:</span> {email.receiver}
                    </>
                  )}
                </p>
                <p className="text-sm font-medium text-gray-200">
                  <span className="text-blue-400">Date:</span> {email.date}
                </p>
              </div>
              <div className="flex justify-between items-center mt-2">
                <p className="text-sm font-medium text-gray-200">
                  <span className="text-blue-400">Priority:{' '}</span>
                  <span className={getPriorityColor(email.priority)}>
                    {email.priority}
                  </span>
                </p>
              </div>
              <p className="text-sm mt-2 text-gray-200">
                <span className="text-blue-400">Subject:</span> {email.subject}
              </p>
            </li>
          ))}
        </ul>
      </div>
    </div>
  );
};

export default EmailsSnippetView;