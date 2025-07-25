package com.example.edutrack.timetables.repository;

import com.example.edutrack.accounts.model.Mentee;
import com.example.edutrack.accounts.model.Mentor;
import com.example.edutrack.curriculum.model.Course;
import com.example.edutrack.curriculum.model.CourseMentor;
import com.example.edutrack.timetables.model.Enrollment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    @Query("""
             SELECT e.courseMentor
             FROM Enrollment e
             WHERE e.status = :enrollmentStatus
             GROUP BY e.courseMentor
             ORDER BY COUNT(e.id) DESC
            """)
    List<CourseMentor> findPopularCoursesByEnrollmentCount(Pageable pageable, @Param("enrollmentStatus") Enrollment.EnrollmentStatus status);


    int countByCourseMentor_IdAndStatus(UUID courseMentorId, Enrollment.EnrollmentStatus status);


    @Query("""
             SELECT e FROM Enrollment e
             WHERE e.mentee.id = :menteeId
               AND e.status = :enrollmentStatus
            """)
    List<Enrollment> findAcceptedEnrollmentsByMenteeId(@Param("menteeId") UUID menteeId, @Param("enrollmentStatus") Enrollment.EnrollmentStatus status);

    @Query("""
            SELECT e FROM Enrollment e
            WHERE e.mentee.id = :menteeId
            AND e.status = :statuses
            """)
    List<Enrollment> findEnrollmentsByMenteeIdWithStatuses(
            @Param("menteeId") UUID menteeId,
            @Param("statuses") Enrollment.EnrollmentStatus statuses
    );

    @Query("""
             SELECT DISTINCT e.courseMentor
             FROM Enrollment e
             JOIN EnrollmentSchedule s ON s.enrollment.mentee = e.mentee AND s.enrollment.courseMentor.course = e.courseMentor.course
             WHERE e.mentee.id = :menteeId
               AND e.status = :enrollmentStatus
            """)
    List<CourseMentor> findInProgressCourses(@Param("menteeId") UUID menteeId, @Param("enrollmentStatus") Enrollment.EnrollmentStatus status);

    @Query("SELECT e FROM Enrollment e WHERE e.courseMentor.mentor.id = :mentorId AND e.status = :status")
    List<Enrollment> findByStatus(Enrollment.EnrollmentStatus status, UUID mentorId);

    @Query("SELECT e.courseMentor.course FROM Enrollment e WHERE e.mentee.id = :menteeId AND e.status = :status ")
    List<Course> findByMenteeIdAndEnrollmentStatus(@Param("menteeId") UUID menteeId, @Param("status") Enrollment.EnrollmentStatus status);

    @Query("""
                SELECT DISTINCT e
                FROM Enrollment e
                WHERE e.courseMentor.mentor = :mentor
                AND e.status = 'APPROVED'
                 AND e.totalSlots > (
                    SELECT COUNT(es)
                    FROM EnrollmentSchedule es
                    WHERE es.enrollment = e AND es.attendance = 'PRESENT' AND es.report = false
                )
            """)
    List<Enrollment> findOngoingEnrollments(Mentor mentor);

    @Query("""
                SELECT DISTINCT e
                FROM Enrollment e
                WHERE e.courseMentor.mentor = :mentor
                AND e.status = 'APPROVED'
                 AND e.totalSlots <= (
                    SELECT COUNT(es)
                    FROM EnrollmentSchedule es
                    WHERE es.enrollment = e AND es.attendance = 'PRESENT' AND es.report = false
                )
            """)
    List<Enrollment> findCompletedEnrollments(Mentor mentor);

    @Query("SELECT e FROM Enrollment e WHERE e.status = 'APPROVED'")
    List<Enrollment> findAllApprovedEnrollments();


    @Query("SELECT DISTINCT e.courseMentor.mentor FROM Enrollment e WHERE e.mentee.id = :menteeId AND e.status = :status")
    List<Mentor> findDistinctMentorsByMenteeId(@Param("menteeId") UUID menteeId, @Param("status") Enrollment.EnrollmentStatus status);

    @Query("""
             SELECT DISTINCT e.courseMentor
             FROM Enrollment e
             JOIN EnrollmentSchedule s ON s.enrollment.mentee = e.mentee AND s.enrollment.courseMentor.course = e.courseMentor.course
             WHERE e.mentee.id = :menteeId
               AND e.status = :enrollmentStatus
            """)
    List<CourseMentor> findCourseMentorByMentee(@Param("menteeId") UUID menteeId, @Param("enrollmentStatus") Enrollment.EnrollmentStatus status);

    List<Enrollment> findAllByMenteeId(UUID menteeId);

    @Query("""
            SELECT COUNT(es)
            FROM EnrollmentSchedule es
            WHERE es.enrollment = :enrollment
            AND es.attendance = 'PRESENT'
            AND es.report = false
            """)
    Double getPercentComplete(Enrollment enrollment);

    @Query("SELECT e FROM Enrollment e WHERE e.mentee.id = :menteeId " +
            "AND e.status = :status " +
            "AND e.scheduleSummary IS NOT NULL " +
            "AND e.scheduleSummary != ''")
    List<Enrollment> findByMenteeIdAndStatus(
            @Param("menteeId") UUID menteeId,
            @Param("status") Enrollment.EnrollmentStatus status
    );

    Page<Enrollment> findAll(Specification<Enrollment> spec, Pageable pageable);

    @Query("SELECT DISTINCT e.mentee FROM Enrollment e")
    List<Mentee> findAllUniqueMentees();

    @Query("SELECT DISTINCT e.courseMentor.mentor FROM Enrollment e")
    List<Mentor> findAllUniqueMentors();

    @Query("""
                SELECT COUNT(e)
                FROM Enrollment e
                WHERE e.courseMentor.mentor = :mentor
                  AND e.status = 'PENDING'
            """)
    int countPendingClassRequest(Mentor mentor);

    @Query("""
            SELECT COUNT(DISTINCT e.mentee)
            FROM Enrollment e
            JOIN EnrollmentSchedule es ON es.enrollment = e
            WHERE e.courseMentor.mentor = :mentor
              AND es.attendance = 'NOT_YET'
            """)
    int countTeachingMentees(Mentor mentor);

    long countByCourseMentor_MentorAndStatus(Mentor mentor, Enrollment.EnrollmentStatus status);
    @Query("SELECT e FROM Enrollment e WHERE " +
            "e.mentee.id = :menteeId AND e.status = com.example.edutrack.timetables.model.Enrollment$EnrollmentStatus.APPROVED AND " +
            "(:keyword IS NULL OR LOWER(e.courseMentor.course.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
            "(:mentorId IS NULL OR e.courseMentor.mentor.id = :mentorId)")
    List<Enrollment> findWithFilters(
            @Param("menteeId") UUID menteeId,
            @Param("keyword") String keyword,
            @Param("mentorId") UUID mentorId
    );

}
