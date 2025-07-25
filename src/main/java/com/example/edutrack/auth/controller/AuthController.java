package com.example.edutrack.auth.controller;

import com.example.edutrack.accounts.model.Staff;
import com.example.edutrack.accounts.model.User;
import com.example.edutrack.accounts.service.interfaces.StaffService;
import com.example.edutrack.auth.service.RecaptchaService;
import com.example.edutrack.auth.service.UserService;
import com.example.edutrack.common.formatter.NameFormatter;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.Date;
import java.util.Optional;

@Controller
public class AuthController {
    private final UserService userService;
    private final StaffService staffService;

    private RecaptchaService recaptchaService;

    public AuthController(UserService userService, StaffService staffService, RecaptchaService recaptchaService) {
        this.userService = userService;
        this.staffService = staffService;
        this.recaptchaService = recaptchaService;
    }

    @GetMapping("/login")
    public String showLoginForm(HttpServletRequest request, Model model) {
        String emailFromCookie;
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("email".equals(cookie.getName())) {
                    emailFromCookie = cookie.getValue();
                    model.addAttribute("email", emailFromCookie);
                    break;
                }
            }
        }
        return "auth/login";
    }

    @GetMapping("/login/admin")
    public String showLoginAdminForm(HttpServletRequest request, Model model) {
        String adminFromCookie;
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("admin".equals(cookie.getName())) {
                    adminFromCookie = cookie.getValue();
                    model.addAttribute("admin", adminFromCookie);
                    break;
                }
            }
        }
        return "auth/login_admin";
    }

    @GetMapping("/signup")
    public String showSignupForm(HttpServletRequest request, Model model) {
        User user = new User();
        user.setGender("male");
        model.addAttribute("user", user);
        return "auth/signup";
    }

    private boolean isAtLeast20YearsOld(User user) {
        Date birthDate = user.getBirthDate();
        if (birthDate == null) return false;

        LocalDate birthLocal = birthDate.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();

        return Period.between(birthLocal, LocalDate.now()).getYears() >= 20;
    }

    @PostMapping("/signup")
    public String processSignup(@ModelAttribute("user") User user,
                                @RequestParam("g-recaptcha-response") String recaptchaResponse,
                                @RequestParam String confirm_password,
                                @RequestParam String role,
                                HttpServletRequest request,
                                Model model) {
        System.out.println("g-recaptcha-response: " + recaptchaResponse);
        if (recaptchaResponse == null || recaptchaResponse.isEmpty()) {
            model.addAttribute("error", "captcha null");
            return "auth/signup";
        }
        String clientIp = request.getRemoteAddr();
        if (!recaptchaService.verify(recaptchaResponse, clientIp)) {
            model.addAttribute("error", "Please verify you are not a robot");
            return "auth/signup";
        }

        if (userService.isEmailExists(user.getEmail())) {
            model.addAttribute("error", "Email already exists");
            return "auth/signup";
        }

        if("mentor".equals(role) && !isAtLeast20YearsOld(user)) {
            model.addAttribute("error", "Mentor is at least 20 years old");
            return "auth/signup";
        }

        if (user.getPassword().length() < 6) {
            model.addAttribute("error", "Password must be at least 6 characters");
            return "auth/signup";
        }

        if (user.getFullName().trim().split("\\s+").length < 2) {
            model.addAttribute("error", "Full name must contain at least two words");
            return "auth/signup";
        }

        if (!user.getPhone().matches("\\d{10}")) {
            model.addAttribute("error", "Phone number must be 10 digits");
            return "auth/signup";
        }

        LocalDate dob = user.getBirthDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        if (Period.between(dob, LocalDate.now()).getYears() < 6) {
            model.addAttribute("error", "You must be at least 6 years old");
            return "auth/signup";
        }


        if(!confirm_password.equals(user.getPassword())) {
            model.addAttribute("error", "Repeated password does not match original password");
            return "auth/signup";
        }

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hashed = encoder.encode(user.getPassword());
        user.setPassword(hashed);
        user.setFullName(NameFormatter.formatName(user.getFullName()));
        if("mentee".equals(role)){
            userService.registerMentee(user);
        }else if("mentor".equals(role)){
            userService.registerMentor(user);
        }
        return "redirect:/login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String email,
                        @RequestParam String password,
                        @RequestParam(required = false) String rememberMe,
                        HttpServletResponse response,
                        HttpSession session,
                        Model model) {
        Optional<User> userOpt = userService.findByEmail(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if(!user.getIsActive()){
                model.addAttribute("error", "Account is inactive");
                return "auth/login";
            }if(user.getIsLocked()){
                model.addAttribute("error", "Account is locked");
                return "auth/login";
            }
            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
            if (encoder.matches(password, user.getPassword())) {
                session.setAttribute("loggedInUser", user);
                if ("on".equals(rememberMe)) {
                    Cookie cookie = new Cookie("email", email);
                    cookie.setMaxAge(7 * 24 * 60 * 60);
                    cookie.setPath("/");
                    response.addCookie(cookie);
                } else {
                    Cookie cookie = new Cookie("email", null);
                    cookie.setMaxAge(0);
                    cookie.setPath("/");
                    response.addCookie(cookie);
                }
                if(userService.isMentee(user)){
                    session.setAttribute("role", "mentee");
                    return "redirect:/";
                }if(userService.isMentor(user)){
                    session.setAttribute("role", "mentor");
                    return "redirect:/mentor";
                }
            }
        }
        model.addAttribute("error", "Invalid email or password");
        model.addAttribute("user", new User());
        return "auth/login";
    }

    @PostMapping("/login/admin")
    public String loginAdmin(@RequestParam String email,
                        @RequestParam String password,
                        @RequestParam(required = false) String rememberMe,
                        HttpServletResponse response,
                        HttpSession session,
                        Model model) {
        Optional<Staff> userOpt = staffService.findByEmail(email);
        if (userOpt.isPresent()) {
            Staff user = userOpt.get();
            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
            if (encoder.matches(password, user.getPassword())) {
                session.setAttribute("loggedInAdmin", user);
                if ("on".equals(rememberMe)) {
                    Cookie cookie = new Cookie("admin", email);
                    cookie.setMaxAge(7 * 24 * 60 * 60);
                    cookie.setPath("/");
                    response.addCookie(cookie);
                } else {
                    Cookie cookie = new Cookie("admin", null);
                    cookie.setMaxAge(0);
                    cookie.setPath("/");
                    response.addCookie(cookie);
                }
                String role = user.getRole().name().toLowerCase();
                session.setAttribute("role", role);
                if("admin".equals(role)){
                    return "redirect:/admin/dashboard";
                }else if("manager".equals(role)){
                    return "redirect:/manager/dashboard";
                }
            }
        }
        model.addAttribute("error", "Invalid email or password");
        model.addAttribute("user", new Staff());
        return "auth/login_admin";
    }

    // Logout
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }
}