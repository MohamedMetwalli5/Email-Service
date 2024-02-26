CREATE SCHEMA IF NOT EXISTS seamail ;
USE seamail ;

CREATE TABLE IF NOT EXISTS emails (
    emailID INT AUTO_INCREMENT PRIMARY KEY,
    sender VARCHAR(255) NOT NULL,
    receiver VARCHAR(255),
    subject NVARCHAR(40),
    body NVARCHAR(100),
    priority INT,
    date DATETIME,
    trash ENUM('Yes', 'No') NOT NULL DEFAULT 'No'
);


CREATE TABLE IF NOT EXISTS users (
  `email` VARCHAR(40) NOT NULL,
  `password` VARCHAR(20) NOT NULL,
  PRIMARY KEY (`email`)
);