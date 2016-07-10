package org.jshaw.demo.service;

import org.jshaw.demo.domain.User;
import org.jshaw.demo.exception.UserAlreadyExistException;
import org.jshaw.demo.repository.UserRepository;
import org.jshaw.demo.security.Role;
import org.jshaw.demo.security.TokenAuthenticationService;
import org.jshaw.demo.security.UserAuthentication;
import org.jshaw.demo.security.UserDetailsImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import javax.servlet.http.HttpServletResponse;

@Service
public class UserServiceImpl implements UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private TokenAuthenticationService tokenAuthenticationService;

    @Autowired
    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder, TokenAuthenticationService tokenAuthenticationService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenAuthenticationService = tokenAuthenticationService;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public User signUp(User user, HttpServletResponse response) throws UserAlreadyExistException {
        Assert.notNull(user);

        // check if username exists
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            throw new UserAlreadyExistException("User already exist with username: " + user.getUsername());
        }

        // encode password and set default role
        String rawPassword = user.getPassword();
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(Role.ROLE_USER);

        logger.info("save user:" + user.toString());
        User u = userRepository.save(user);

        // load user and set auth so that user does not to login again after signup
        UserDetailsImpl userDetails = new UserDetailsImpl(u);
        UserAuthentication userAuthentication = new UserAuthentication(userDetails);
        userAuthentication.setAuthenticated(true);
        tokenAuthenticationService.addAuthentication(response, userAuthentication);

        return u;
    }
}
