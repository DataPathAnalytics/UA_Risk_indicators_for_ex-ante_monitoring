package com.datapath.web.services;

import com.datapath.persistence.entities.monitoring.User;
import com.datapath.persistence.repositories.monitoring.RoleRepository;
import com.datapath.persistence.repositories.monitoring.UserRepository;
import com.datapath.web.dto.ApplicationUser;
import com.datapath.web.dto.PasswordDTO;
import com.datapath.web.exceptions.UserAlreadyExistException;
import com.datapath.web.util.UserUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.MessageFormat;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;


    @Autowired
    public UserService(UserRepository userRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.bCryptPasswordEncoder = new BCryptPasswordEncoder();
    }

    @Transactional
    public ApplicationUser findByEmail(String email) {
        ApplicationUser applicationUser = new ApplicationUser();
        User dbUser = userRepository.findByEmail(email);
        BeanUtils.copyProperties(dbUser, applicationUser);
        applicationUser.setRole(dbUser.getRole().getRole());
        return applicationUser;
    }

    @Transactional
    public User findDBUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public boolean isCredentialsCorrect(ApplicationUser credentials, ApplicationUser appUser) {
        return appUser == null || !new BCryptPasswordEncoder().matches(credentials.getPassword(), appUser.getPassword());
    }

    @Transactional
    public ApplicationUser findById(Long id) {
        return UserUtils.convertToDTO(userRepository.findOneById(id));
    }

    public List<ApplicationUser> list() {
        return userRepository.findAll().stream().map(UserUtils::convertToDTO).collect(Collectors.toList());
    }

    public ApplicationUser create(ApplicationUser appUser) {
        User existed = userRepository.findByEmail(appUser.getEmail());
        if (existed != null) {
            throw new UserAlreadyExistException();
        }
        User user = new User();
        BeanUtils.copyProperties(appUser, user);
        user.setPassword(bCryptPasswordEncoder.encode(appUser.getPassword()));
        user.setRole(roleRepository.findByRole(appUser.getRole()));
        return UserUtils.convertToDTO(userRepository.save(user));
    }

    public void updatePassword(PasswordDTO passwordDTO, Long userId) {
        User existed = userRepository.findOneById(userId);
        if (existed == null) {
            throw new UsernameNotFoundException(MessageFormat.format("User with id {0} not found", userId));
        }
        if (!bCryptPasswordEncoder.matches(passwordDTO.getOldPassword(), existed.getPassword())) {
            throw new BadCredentialsException("Old password is not matched");
        }
        existed.setPassword(bCryptPasswordEncoder.encode(passwordDTO.getNewPassword()));
        userRepository.save(existed);
    }

    public void updatePassword(String newPassword, Long userId) {
        User existed = userRepository.findOneById(userId);
        if (existed == null) {
            throw new UsernameNotFoundException(MessageFormat.format("User with id {0} not found", userId));
        }

        existed.setPassword(bCryptPasswordEncoder.encode(newPassword));
        userRepository.save(existed);
    }

    public ApplicationUser update(ApplicationUser user) {
        User existedUser = userRepository.findOneById(user.getId());
        existedUser.setRole(roleRepository.findByRole(user.getRole()));
        existedUser.setEmail(user.getEmail());
        existedUser.setFirstName(user.getFirstName());
        existedUser.setLastName(user.getLastName());
        existedUser.setRegion(user.getRegion());
        return UserUtils.convertToDTO(userRepository.save(existedUser));
    }

    public void delete(Long id) {
        userRepository.deleteById(id);
    }

}
