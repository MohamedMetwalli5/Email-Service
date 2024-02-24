package com.backendemailservice.backendemailservice.controller;

import org.apache.catalina.connector.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import com.backendemailservice.backendemailservice.entity.User;
import com.backendemailservice.backendemailservice.service.UserService;
import java.util.Optional;


@RestController
public class AccessController {
	
	private final UserService userService;
	
	@Autowired
    public AccessController(UserService userService) {
        this.userService = userService;
    }
	
	@PostMapping("/signin") 
	public String signin(User user) {
		// logic to use an implemented user service to check on the DB for the existence of a user with both email and password
		if (userService.findUser(user.getEmail(), user.getPassword()).isPresent()) {
            // User exists in the database
			System.out.println("pppppppppppppppppppppppppppppppppppppppppp");
            return "user found";
        } else {
            // User does not exist
    		System.out.println("hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh");
            return "user not found";
        }
	}
	
	
//	@PostMapping("/signup") 
//	public User signup(User user) {
//		// logic to use an implemented user service to check on the DB for the existence of a user with this email
//		return UserService.createUser(user);
//	}
	
}
