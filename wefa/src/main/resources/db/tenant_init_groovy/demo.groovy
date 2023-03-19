import org.springframework.security.crypto.password.PasswordEncoder

import hu.lanoga.toolbox.repository.jdbc.JdbcRepositoryManager
import hu.lanoga.toolbox.file.*
import hu.lanoga.toolbox.spring.*
import hu.lanoga.toolbox.util.*
import hu.lanoga.toolbox.user.*;
import hu.lanoga.toolbox.tenant.*;

import hu.lanoga.wefa.SysKeys;
import hu.lanoga.wefa.model.*;
import hu.lanoga.wefa.service.*;
import hu.lanoga.wefa.repository.*;

UserService userService = ApplicationContextHelper.getBean(UserService.class);
UserJdbcRepository userJdbcRepository = ApplicationContextHelper.getBean(UserJdbcRepository.class);
UserPasswordJdbcRepository userPasswordJdbcRepository = ApplicationContextHelper.getBean(UserPasswordJdbcRepository.class);

ActivitiHelperService activitiHelperService = ApplicationContextHelper.getBean(ActivitiHelperService.class);

PasswordEncoder passwordEncoder = ApplicationContextHelper.getBean(PasswordEncoder.class);

// users

t: {

	t: {
	
		UserPassword userPassword = userPasswordJdbcRepository.findOneBy("userId", 11);
		userPassword.setPassword(passwordEncoder.encode("admin"));
		userPasswordJdbcRepository.save(userPassword);
	
	}

	t: {
		User user = new User();

		user.setUsername("clerk1");
		user.setFamilyName("Smith");
		user.setGivenName("James");
		user.setEmail("jsmith@example.com");
		user.setEnabled(true);
		user.setUserRoles("[" + SysKeys.UserAuth.ROLE_CLERK_CS_ID + "]");

		User savedUser = userService.save(user, true);

		UserPassword userPassword = userPasswordJdbcRepository.findOneBy("userId", savedUser.getId());
		userPassword.setPassword(passwordEncoder.encode(savedUser.getUsername()));

		userPasswordJdbcRepository.save(userPassword);
	}
	
	t: {
		User user = new User();

		user.setUsername("clerk2");
		user.setFamilyName("Walter");
		user.setGivenName("White");
		user.setEmail("wwhite@example.com");
		user.setEnabled(true);
		user.setUserRoles("[" + SysKeys.UserAuth.ROLE_CLERK_CS_ID + "]");

		User savedUser = userService.save(user, true);

		UserPassword userPassword = userPasswordJdbcRepository.findOneBy("userId", savedUser.getId());
		userPassword.setPassword(passwordEncoder.encode(savedUser.getUsername()));

		userPasswordJdbcRepository.save(userPassword);
	}

	t: {
		User user = new User();

		user.setUsername("approver1");
		user.setFamilyName("Williams");
		user.setGivenName("Mary");
		user.setEmail("mwilliams@example.com");
		user.setEnabled(true);
		user.setUserRoles("[" + SysKeys.UserAuth.ROLE_APPROVER_CS_ID + "]");

		User savedUser = userService.save(user, true);

		UserPassword userPassword = userPasswordJdbcRepository.findOneBy("userId", savedUser.getId());
		userPassword.setPassword(passwordEncoder.encode(savedUser.getUsername()));

		userPasswordJdbcRepository.save(userPassword);
	}
	
	t: {
		User user = new User();

		user.setUsername("approver2");
		user.setFamilyName("Archer");
		user.setGivenName("Jonathan");
		user.setEmail("jarcher@example.com");
		user.setEnabled(true);
		user.setUserRoles("[" + SysKeys.UserAuth.ROLE_APPROVER_CS_ID + "]");

		User savedUser = userService.save(user, true);

		UserPassword userPassword = userPasswordJdbcRepository.findOneBy("userId", savedUser.getId());
		userPassword.setPassword(passwordEncoder.encode(savedUser.getUsername()));

		userPasswordJdbcRepository.save(userPassword);
	}
	
}