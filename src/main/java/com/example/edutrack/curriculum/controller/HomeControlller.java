package com.example.edutrack.curriculum.controller;

import com.example.edutrack.accounts.model.Mentee;
import com.example.edutrack.accounts.model.Mentor;
import com.example.edutrack.accounts.model.User;
import com.example.edutrack.accounts.repository.MenteeRepository;
import com.example.edutrack.accounts.service.interfaces.MenteeService;
import com.example.edutrack.accounts.service.interfaces.MentorService;
import com.example.edutrack.curriculum.dto.CourseCardDTO;
import com.example.edutrack.curriculum.dto.TagEnrollmentCountDTO;
import com.example.edutrack.curriculum.model.CourseMentor;
import com.example.edutrack.curriculum.model.LandingPageConfig;
import com.example.edutrack.curriculum.model.MenteeLandingRole;
import com.example.edutrack.curriculum.model.Tag;
import com.example.edutrack.curriculum.service.interfaces.*;
import com.example.edutrack.timetables.service.interfaces.EnrollmentService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Controller
public class HomeControlller {

    private final LandingPageConfigService landingPageConfigService;
    private final SuggestionService suggestionService;
    private final CourseTagService courseTagService;
    private final EnrollmentService enrollmentService;
    private final CourseMentorService courseMentorService;
    private final MentorService mentorService;
    private final DashboardService dashboardService;
    private final MenteeRepository menteeRepository;
    private final MenteeService menteeService;

    public HomeControlller(LandingPageConfigService landingPageConfigService,
                           SuggestionService suggestionService,
                           CourseTagService courseTagService,
                           EnrollmentService enrollmentService, CourseMentorService courseMentorService, MentorService mentorService,
                           MenteeRepository menteeRepository,
                           DashboardService dashboardService,
                           MenteeService menteeService) {
        this.landingPageConfigService = landingPageConfigService;
        this.suggestionService = suggestionService;
        this.courseTagService = courseTagService;
        this.enrollmentService = enrollmentService;
        this.courseMentorService = courseMentorService;
        this.menteeService = menteeService;
        this.menteeRepository = menteeRepository;
        this.dashboardService = dashboardService;
        this.mentorService = mentorService;
    }

    @GetMapping("/")
    public String home(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedInUser");

        if (user == null) return handleGuestUser(model);
        if (user instanceof Mentor) return handleGuestUser(model);
        if ("Manager".equals(user.getClass().getSimpleName())) return "redirect:/manager/dashboard";

        Mentee mentee = (Mentee) user;
        if (enrollmentService.getEnrollmentsByMenteeId(mentee.getId()).isEmpty()) {
            return handleNewMenteeUser(mentee, model);
        } else {
            return handleExperiencedMenteeUser(mentee, model);
        }
    }

    private String handleGuestUser(Model model) {
        LandingPageConfig config = landingPageConfigService.getConfigByRole(MenteeLandingRole.GUEST);
        populateCommonModel(model, config);


        model.addAttribute("userType", "guest");
        model.addAttribute("showSchedulesLink", false);
        model.addAttribute("showTracker", false);

        model.addAttribute("courseSectionOne", enrollmentService.mapToCourseCardDTOList(
                suggestionService.getSuggestedCourses(config.getCourseSectionOneSuggestion(), null, 8)));
        model.addAttribute("courseSectionTwo", enrollmentService.mapToCourseCardDTOList(
                suggestionService.getSuggestedCourses(config.getCourseSectionTwoSuggestion(), null, 8)));
        model.addAttribute("recommendedMentors",
                suggestionService.getSuggestedMentors(config.getMentorSuggestion(), null, 7));

        return "mentee/mentee-landing-page";
    }

    private String handleNewMenteeUser(Mentee user, Model model) {
        LandingPageConfig config = landingPageConfigService.getConfigByRole(MenteeLandingRole.MENTEE_NEW);
        populateCommonModel(model, config);

        model.addAttribute("heroHeadline", "Welcome," + user.getFullName());
        model.addAttribute("userType", "newUser");
        model.addAttribute("showSchedulesLink", false);
        model.addAttribute("showDashboard", true);

        model.addAttribute("headerCTA", "Logout");
        model.addAttribute("headerCTALink", "/logout");

        UUID userId = user.getId();

        model.addAttribute("courseSectionOne", enrollmentService.mapToCourseCardDTOList(
                suggestionService.getSuggestedCourses(config.getCourseSectionOneSuggestion(), userId, 8)));
        model.addAttribute("courseSectionTwo", enrollmentService.mapToCourseCardDTOList(
                suggestionService.getSuggestedCourses(config.getCourseSectionTwoSuggestion(), userId, 8)));
        model.addAttribute("recommendedMentors",
                suggestionService.getSuggestedMentors(config.getMentorSuggestion(), userId, 5));

        Mentee mentee = menteeRepository.findById(userId).orElse(null);
        boolean showInterestModal = mentee != null && (mentee.getInterests() == null || mentee.getInterests().isEmpty());
        model.addAttribute("userId", userId.toString());
        model.addAttribute("showInterestModal", showInterestModal);

        return "mentee/mentee-landing-page";
    }

    private String handleExperiencedMenteeUser(Mentee user, Model model) {
        LandingPageConfig config = landingPageConfigService.getConfigByRole(MenteeLandingRole.MENTEE_EXPERIENCED);
        UUID userId = user.getId();

        if (config.isUseScheduleReminder()) {
            if (dashboardService.isAllCoursesCompleted(userId)) {
                config.setHeroHeadline("Well Done!");
                config.setHeroSubHeadline("You've completed all your sessions. Explore more advanced topics.");
                config.setHeroCTA("Find New Courses");
                config.setHeroCTALink("/courses");
            } else {
                config.setHeroHeadline("Upcoming Sessions");
                config.setHeroSubHeadline("Next session: " + dashboardService.getNextSessionTime(userId));
                config.setHeroCTA("View schedule →");
                config.setHeroCTALink("/schedules");
            }
        }

        populateCommonModel(model, config);
        model.addAttribute("landingConfig", config);

        model.addAttribute("headerCTA", "Logout");
        model.addAttribute("headerCTALink", "/logout");
        model.addAttribute("userType", "experiencedUser");
        model.addAttribute("showSchedulesLink", true);
        model.addAttribute("showDashboard", true);
        model.addAttribute("showTracker", true);

        model.addAttribute("courseSectionOne", enrollmentService.mapToCourseCardDTOList(
                suggestionService.getSuggestedCourses(config.getCourseSectionOneSuggestion(), userId, 8)));
        model.addAttribute("courseSectionTwo", enrollmentService.mapToCourseCardDTOList(
                suggestionService.getSuggestedCourses(config.getCourseSectionTwoSuggestion(), userId, 8)));
        model.addAttribute("recommendedMentors",
                suggestionService.getSuggestedMentors(config.getMentorSuggestion(), userId, 7));

        return "mentee/mentee-landing-page";
    }


    private void populateCommonModel(Model model, LandingPageConfig config) {
        model.addAttribute("heroHeadline", config.getHeroHeadline());
        model.addAttribute("heroSubHeadline", config.getHeroSubHeadline());
        model.addAttribute("heroCTA", config.getHeroCTA());
        model.addAttribute("heroCTALink", config.getHeroCTALink());

        model.addAttribute("sectionOneTitle", config.getSectionOneTitle());
        model.addAttribute("sectionOneSubtitle", config.getSectionOneSubtitle());
        model.addAttribute("sectionTwoTitle", config.getSectionTwoTitle());
        model.addAttribute("sectionTwoSubtitle", config.getSectionTwoSubtitle());

        model.addAttribute("categoryTitle", config.getCategoryTitle());
        model.addAttribute("categorySubtitle", config.getCategorySubtitle());
        model.addAttribute("categoryButtonText", config.getCategoryButtonText());

        model.addAttribute("aboutTitle", config.getAboutTitle());
        model.addAttribute("aboutSubtitle", config.getAboutSubtitle());
        model.addAttribute("aboutDescription", config.getAboutDescription());

        model.addAttribute("mentorSectionTitle", config.getMentorSectionTitle());
        model.addAttribute("mentorSectionSubtitle", config.getMentorSectionSubtitle());

        model.addAttribute("heroImageUrl", config.getHeroImageUrl());
        model.addAttribute("categorySectionBgUrl", config.getCategorySectionBgUrl());
        model.addAttribute("aboutSectionImageUrl", config.getAboutSectionImageUrl());
        model.addAttribute("courseSectionBgUrl", config.getCourseSectionBgUrl());
        model.addAttribute("mentorSectionBgUrl", config.getMentorSectionBgUrl());

        model.addAttribute("footerDescription", config.getFooterDescription());
        model.addAttribute("copyrightText", config.getCopyrightText());
        model.addAttribute("totalCourses", courseMentorService.getTotalActiveCourseCount());
        model.addAttribute("totalMentors", mentorService.getTotalMentorCount());
        model.addAttribute("totalMentees", menteeService.getTotalMenteeCount());
        model.addAttribute("topTags", suggestionService.getSuggestedTags(config.getTagSuggestion(), 9));
        List<Tag> allCourseTags = courseTagService.getAllTags();
        List<Integer> allCourseTagIds = allCourseTags.stream().map(Tag::getId).toList();
        model.addAttribute("allCourseTagIds", allCourseTagIds);
    }
}
