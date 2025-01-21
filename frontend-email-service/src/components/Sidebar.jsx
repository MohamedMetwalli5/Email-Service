import React, {useState} from 'react';
import { LuInbox } from "react-icons/lu";
import { FaLocationArrow, FaPlus } from "react-icons/fa";
import { FaRegTrashCan } from "react-icons/fa6";
import { PiSignOutBold } from "react-icons/pi";
import SeamailWebsiteLogo from "../../public/SeamailWebsiteLogo.svg";
import { useNavigate } from 'react-router-dom';
import NewMessageComposer from './NewMessageComposer';
import { useContext } from 'react';
import { AppContext } from '../AppContext.jsx';


const Sidebar = () => {

  const { setSharedMailBoxOption } = useContext(AppContext);

  const [isComposerOpen, setComposerOpen] = useState(false);
  
  const navigate = useNavigate();

  return (
    <div className="flex h-full bg-gradient-to-b from-gray-900 to-gray-800 text-sm md:text-lg">
      <aside
        id="sidebar"
        className={`${
          isComposerOpen ? "w-80" : "w-64"
        } bg-gray-900 border-r border-gray-700 shadow-lg flex flex-col h-screen`}
      >
        <div className="flex flex-col h-full px-4 py-6 overflow-y-auto">
          <a className="flex items-center px-4 mb-6 hover:cursor-pointer" 
            onClick={() => navigate("/home")}
          >
            <img
              src={SeamailWebsiteLogo}
              className="h-10 mr-3"
              alt="Seamail Logo"
            />
            <span className="text-3xl font-bold text-blue-400">Seamail</span>
          </a>
          <ul className="space-y-4 flex-grow">
            <li>
              <a
                onClick={() => setComposerOpen(true)}
                className="flex items-center p-3 text-gray-200 bg-yellow-500 rounded-lg hover:bg-yellow-600 hover:cursor-pointer transition-all duration-300"
              >
                <FaPlus />
                <span className="ml-3 font-medium text-white">New message</span>
              </a>
              {isComposerOpen && (
                <NewMessageComposer onClose={() => setComposerOpen(false)} />
              )}
            </li>
            <li>
              <a
                className="flex items-center p-3 text-gray-200 bg-blue-600 rounded-lg hover:bg-blue-700 hover:cursor-pointer transition-all duration-300"
                onClick={() => setSharedMailBoxOption("Inbox")}
              >
                <LuInbox />
                <span className="ml-3 font-medium text-white">Inbox</span>
              </a>
            </li>
            <li>
              <a
                className="flex items-center p-3 text-gray-200 bg-blue-600 rounded-lg hover:bg-blue-700 hover:cursor-pointer transition-all duration-300"
                onClick={() => setSharedMailBoxOption("Outbox")}
              >
                <FaLocationArrow />
                <span className="ml-3 font-medium text-white">Sent</span>
              </a>
            </li>
            <li>
              <a
                className="flex items-center p-3 text-gray-200 bg-blue-600 rounded-lg hover:bg-blue-700 hover:cursor-pointer transition-all duration-300"
                onClick={() => setSharedMailBoxOption("Trashbox")}
              >
                <FaRegTrashCan />
                <span className="ml-3 font-medium text-white">Trash</span>
              </a>
            </li>
          </ul>
          <li className="mt-auto">
            <a
              className="flex items-center p-3 text-gray-200 bg-red-500 rounded-lg hover:bg-red-600 hover:cursor-pointer transition-all duration-300"
              onClick={() => {
                localStorage.removeItem('authToken');
                localStorage.removeItem('sharedUserEmail');
                navigate('/');
              }}
            >
              <PiSignOutBold />
              <span className="ml-3 font-medium text-white"> Sign out</span>
            </a>
          </li>
        </div>
      </aside>
    </div>
  );
};

export default Sidebar;