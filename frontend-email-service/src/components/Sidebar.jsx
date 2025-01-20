import React from 'react';
import { LuInbox } from "react-icons/lu";
import { FaLocationArrow } from "react-icons/fa";
import { FaRegTrashCan } from "react-icons/fa6";
import { PiSignOutBold } from "react-icons/pi";


const Sidebar = () => {
  return (
    <div className="flex h-screen bg-gradient-to-b from-gray-900 to-gray-800">
      <aside
        id="sidebar"
        className="w-64 bg-gray-900 border-r border-gray-700 shadow-lg h-screen"
      >
        <div className="h-full px-4 py-6 overflow-y-auto">
          <a href="/home" className="flex items-center px-4 mb-6">
            <img
              src=""
              className="h-8 mr-3"
              alt="Seamail Logo"
            />
            <span className="text-2xl font-bold text-blue-400">Seamail</span>
          </a>
          <ul className="space-y-4">
            <li>
              <a
                href="#"
                className="flex items-center p-3 text-gray-200 bg-blue-600 rounded-lg hover:bg-blue-700 transition-all duration-300"
              >
                <LuInbox />
                <span className="ml-3 font-medium text-white">Inbox</span>
              </a>
            </li>
            <li>
              <a
                href="#"
                className="flex items-center p-3 text-gray-200 bg-blue-600 rounded-lg hover:bg-blue-700 transition-all duration-300"
              >
                <FaLocationArrow />
                <span className="ml-3 font-medium text-white">Sent</span>
              </a>
            </li>
            <li>
              <a
                href="#"
                className="flex items-center p-3 text-gray-200 bg-blue-600 rounded-lg hover:bg-blue-700 transition-all duration-300"
              >
                <FaRegTrashCan />
                <span className="ml-3 font-medium text-white">Trash</span>
              </a>
            </li>
            <li>
              <a
                href="#"
                className="flex items-center p-3 text-gray-200 bg-blue-600 rounded-lg hover:bg-blue-700 transition-all duration-300"
              >
                <PiSignOutBold />
                <span className="ml-3 font-medium text-white">Sign out</span>
              </a>
            </li>
          </ul>
        </div>
      </aside>
    </div>
  );
};

export default Sidebar;