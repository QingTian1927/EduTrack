package com.example.edutrack.accounts.controller;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.UUID;
import java.util.Date;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.example.edutrack.accounts.model.Mentee;
import com.example.edutrack.accounts.model.Mentor;
import com.example.edutrack.accounts.model.User;
import com.example.edutrack.accounts.repository.MenteeRepository;
import com.example.edutrack.accounts.repository.MentorRepository;
import com.example.edutrack.accounts.repository.UserRepository;
import com.example.edutrack.accounts.service.implementations.MenteeServiceImpl;
import com.example.edutrack.accounts.service.interfaces.ProfileService;
import com.example.edutrack.accounts.service.interfaces.MentorService;
import com.example.edutrack.accounts.service.interfaces.MenteeService;
import org.apache.commons.validator.routines.EmailValidator;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Controller
public class AccountsController {

        private final UserRepository userRepository;
        private final MenteeRepository menteeRepository;
        private final MentorRepository mentorRepository;
        private final ProfileService userService;
        private final MentorService mentorService;
        private final MenteeService menteeService;

        public AccountsController(ProfileService userService,
                        MentorService mentorService,
                        MenteeService menteeService,
                        UserRepository userRepository,
                        MenteeRepository menteeRepository,
                        MentorRepository mentorRepository) {
                this.userService = userService;
                this.mentorService = mentorService;
                this.menteeService = menteeService;
                this.userRepository = userRepository;
                this.menteeRepository = menteeRepository;
                this.mentorRepository = mentorRepository;
        }

        @GetMapping("/profile")
        public String viewProfile(Model model, HttpSession session) throws IOException {
                User loggedInUser = (User) session.getAttribute("loggedInUser");
                String id = loggedInUser.getId().toString();
                User user = userService.getUserById(id);
                model.addAttribute("user", user);
                Mentor mentor = mentorService.getMentorById(id);
                if (mentor != null) {
                        model.addAttribute("role", "mentor");
                        model.addAttribute("mentor", mentor);
                } else {
                        model.addAttribute("role", "mentee");
                        model.addAttribute("mentee", menteeService.getMenteeById(id));
                }
                return "accounts/html/profile";
        }

        public boolean verifyEmail(String email) {
                if (email == null || email.trim().isEmpty())
                        return false;
                EmailValidator validator = EmailValidator.getInstance();
                return validator.isValid(email);
        }

        public boolean verifyPhone(String phone) {
                if (phone == null || phone.trim().isEmpty())
                        return false;
                // Việt Nam: bắt đầu bằng 0, tổng 10 hoặc 11 chữ số (0xxx... hoặc 0xxxxxxxxx)
                String phoneRegex = "^0\\d{9,10}$";
                return phone.matches(phoneRegex);
        }

        public boolean verifyBirthDate(String birthDateStr) {
                if (birthDateStr == null || birthDateStr.trim().isEmpty()) {
                        return false;
                }
                try {
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                        sdf.setLenient(false); // Strict parsing
                        Date birthDate = sdf.parse(birthDateStr);
                        Date today = new Date();

                        // Check if birth date is in the future
                        if (!birthDate.before(today)) {
                                return false;
                        }

                        return true;
                } catch (Exception e) {
                        return false; // Invalid date format
                }
        }

        // Function 5
        @PostMapping("/profile/edit")
        public String editProfile(HttpSession session,
                        @RequestParam String fullName,
                        @RequestParam String phone,
                        @RequestParam String bio,
                        @RequestParam String birthDate,
                        @RequestParam String gender,
                        @RequestParam(required = false) String expertise,
                        @RequestParam(required = false) String interests,
                        org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes)
                        throws IOException {
                User loggedInUser = (User) session.getAttribute("loggedInUser");
                String id = loggedInUser.getId().toString();
                // Validate fullName
                if (fullName == null || fullName.trim().isEmpty()) {
                        redirectAttributes.addFlashAttribute("error", "Full name is required");
                        return "redirect:/profile" + "#edit";
                }

                // Validate phone
                if (!verifyPhone(phone)) {
                        redirectAttributes.addFlashAttribute("error",
                                        "Phone number is not valid (must start with 0 and be 10 or 11 digits)");
                        return "redirect:/profile" + "#edit";
                }

                // Validate birthDate
                if (!verifyBirthDate(birthDate)) {
                        redirectAttributes.addFlashAttribute("error",
                                        "Birth date is not valid (must be a past date in yyyy-MM-dd format)");
                        return "redirect:/profile" + "#edit";
                }

                // Validate expertise for mentors
                Mentor mentor = mentorService.getMentorById(id);
                if (mentor != null) {
                        if (expertise == null || expertise.trim().isEmpty()) {
                                redirectAttributes.addFlashAttribute("error", "Expertise is required");
                                return "redirect:/profile" + "#edit";
                        }
                        if (expertise.trim().length() < 3 || expertise.trim().length() > 100) {
                                redirectAttributes.addFlashAttribute("error",
                                                "Expertise must be between 3 and 100 characters");
                                return "redirect:/profile" + "#edit";
                        }
                }

                // Validate interests for mentees
                Mentee mentee = null;
                try {
                        mentee = menteeService.getMenteeById(id);
                } catch (Exception e) {
                        mentee = null;
                }
                if (mentor == null && mentee != null) {
                        if (interests != null && (interests.trim().length() < 3 || interests.trim().length() > 100)) {
                                redirectAttributes.addFlashAttribute("error",
                                                "Interests must be between 3 and 100 characters");
                                return "redirect:/profile" + "#edit";
                        }
                }

                // Load user
                User user = userService.getUserById(id);
                if (user == null) {
                        throw new IllegalArgumentException("User not found with id: " + id);
                }

                // Update user
                user.setFullName(fullName.trim());
                user.setPhone(phone);
                user.setBio(bio != null ? bio.trim() : null);
                try {
                        user.setBirthDate(new SimpleDateFormat("yyyy-MM-dd").parse(birthDate));
                } catch (java.text.ParseException e) {
                        redirectAttributes.addFlashAttribute("error", "Invalid birth date format.");
                        return "redirect:/profile" + "#edit";
                }
                user.setGender(gender);
                userRepository.save(user);

                // Update mentor or mentee
                if (mentor != null) {
                        mentor.setExpertise(expertise.trim());
                        mentorRepository.save(mentor);
                } else if (mentee != null) {
                        mentee.setInterests(interests != null ? interests.trim() : null);
                        menteeRepository.save(mentee);
                }

                return "redirect:/profile";
        }

        @PostMapping("/avatar/upload/")
        public String uploadAvatar(
                        HttpSession session, @RequestParam("image") MultipartFile file)
                        throws IOException {
                User loggedInUser = (User) session.getAttribute("loggedInUser");
                String id = loggedInUser.getId().toString();
                userService.updateAvatar(id, file);
                // Update the session attribute to reflect the new avatar
                User updatedUser = userService.getUserById(id);
                session.setAttribute("loggedInUser", updatedUser);
                return "redirect:/profile";
        }

        @GetMapping("/avatar")
        public void getAvatar(HttpSession session, HttpServletResponse response) throws IOException {
                User loggedInUser = (User) session.getAttribute("loggedInUser");

                if (loggedInUser != null && loggedInUser.getAvatar() != null) {
                        response.setContentType("image/jpeg");
                        response.getOutputStream().write(loggedInUser.getAvatar());
                } else {
                        response.sendRedirect("/assets/images/default-avatar.svg");
                        return;
                }

                response.getOutputStream().close();
        }

}
