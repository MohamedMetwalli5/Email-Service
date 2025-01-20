import React from "react";

const EmailsSnippetView = () => {
  const title = "Inbox";

  const emails = [
    {
      id: 1,
      sender: "test1@example.com",
      subject: "Meeting Reminder",
      priority: 1,
    },
    {
      id: 2,
      sender: "test2@example.com",
      subject: "Special Event!",
      priority: 2,
    },
  ];

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

  return (
    <div className="h-full bg-gray-900 text-gray-100 rounded-lg shadow-md p-4">
      <h2 className="text-2xl font-semibold mb-4">{title}</h2>
      <ul className="space-y-4 overflow-y-auto hover:cursor-pointer">
        {emails.length === 0 ? (
          <li className="text-center text-gray-500">No emails available.</li>
        ) : (
          emails.map((email) => (
            <li
              key={email.id}
              className="flex flex-col p-4 bg-gray-800 rounded-lg hover:bg-gray-700 transition-colors"
            >
              <div className="flex justify-between items-center">
                <p className="text-sm font-medium">
                  <span className="text-blue-400">Sender:</span> {email.sender}
                </p>
              </div>
              <div className="flex justify-between items-center mt-2">
                <p className="text-sm font-medium">
                  <span className="text-blue-400">Priority:{' '}</span>
                  <span className={getPriorityColor(email.priority)}>
                    {email.priority}
                  </span>
                </p>
              </div>
              <p className="text-sm mt-2">
                <span className="text-blue-400">Subject:</span> {email.subject}
              </p>
            </li>
          ))
        )}
      </ul>
    </div>
  );
};

export default EmailsSnippetView;