import React, { useState } from "react";
import { FaRegTrashCan } from "react-icons/fa6";


const EmailFullView = () => {
  const [isDeleted, setIsDeleted] = useState(false);

  const email = {
    id: 1,
    sender: "test1@example.com",
    subject: "Meeting Reminder",
    priority: 1,
    date: "2025-01-20",
    body: "This is a reminder for the upcoming meeting scheduled for 2025-01-18 at 11:00 AM."
  };

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

  const moveToTrash = () => {
    setIsDeleted(true);
    console.log(`Email ${email.id} moved to trash`);
  };

  if(isDeleted){
    return (
      <div className="h-full w-full bg-gray-900 text-gray-100 rounded-lg shadow-md p-6">
        <h2 className="text-xl font-semibold mb-4 text-red-400">This email has been moved to Trash</h2>
      </div>
    );
  }

  return (
    <div className="h-full w-full bg-gray-900 text-gray-100 rounded-lg shadow-md p-6 relative">
      <button
        onClick={moveToTrash}
        className="absolute top-4 right-4 text-gray-400 hover:text-red-500 transition-all duration-300"
      >
        <FaRegTrashCan size={20} />
      </button>

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
    </div>
  );
};

export default EmailFullView;