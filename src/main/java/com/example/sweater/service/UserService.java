package com.example.sweater.service;

import com.example.sweater.domain.Role;
import com.example.sweater.domain.User;
import com.example.sweater.repos.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserService implements UserDetailsService {
    @Autowired
    private MailSender mailSender;

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    public boolean addUser(User user) {
        User userDb = userRepo.findByUsername(user.getUsername());
        if (userDb != null) {
            return false;
        }
        user.setActive(true);
        user.setRoles(Collections.singleton(Role.USER));
        user.setActivationCode(UUID.randomUUID().toString());
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepo.save(user);
        sendMessage(user);
        return true;
    }
    @Value("${siteDomain}")
    private String domain;
    private void sendMessage(User user) {
        if (!StringUtils.isEmpty(user.getEmail())) {
            String message = String.format("Hello, %s! \n" +
                    "Welcome to MySweater. Please, visit next link: %s",
                    user.getUsername(),
                    domain + "/activate/" + user.getActivationCode());
            mailSender.send(user.getEmail(), "Activation code", message);
        }
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (userRepo.findByUsername(username) == null) {
            throw new UsernameNotFoundException("User not found!");
        } else
            return userRepo.findByUsername(username);
    }

    public boolean activateUser(String code) {
        User user = userRepo.findByActivationCode(code);

        if (user == null) {
            return false;
        }

        user.setActivationCode(null);

        userRepo.save(user);

        return true;
    }

    public List<User> findAll() {
        return userRepo.findAll();
    }

    public void saveUser(User user, String username, Map<String, String> form) {

        user.setUsername(username);
        Set<String> Roles = Arrays.stream(Role.values())
                .map(Role::name)
                .collect(Collectors.toSet());

        user.getRoles().clear();
        for (String key: form.keySet()
        ) {
            if (Roles.contains(key))
                user.getRoles().add(Role.valueOf(key));
        }
        System.out.println('4');
        userRepo.save(user);
        System.out.println('5');
    }

    public void updateProfile(User user, String password, String email) {
        String userOldEmail = user.getEmail();
        boolean isChanged = (email != null && !email.equals(user.getEmail()));
        if (isChanged) {
            user.setEmail(email);

            if (!StringUtils.isEmpty(email)) {
                user.setActivationCode(UUID.randomUUID().toString());
            }
        }

        if (!StringUtils.isEmpty(password)) {
            user.setPassword(passwordEncoder.encode(password));

        }
        user.setPassword2(passwordEncoder.encode(user.getPassword()));
        userRepo.save(user);
        if (isChanged)
            sendMessage(user);
    }

    public void subscribe(User currentUser, User user) {

            user.getSubscribers().add(currentUser);
            //currentUser.getSubscriptions().add(user);
            userRepo.save(user);

    }

    public void unsubscribe(User currentUser, User user) {
            user.getSubscribers().remove(currentUser);
            userRepo.save(user);

    }
}

