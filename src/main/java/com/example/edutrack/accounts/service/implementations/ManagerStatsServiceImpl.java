package com.example.edutrack.accounts.service.implementations;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Locale;
import java.util.List;
import java.text.NumberFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import java.util.UUID;
import com.example.edutrack.transactions.repository.TransactionRepository;
import com.example.edutrack.accounts.dto.ManagerStatsDTO;
import com.example.edutrack.accounts.dto.RevenueChartDTO;
import com.example.edutrack.accounts.dto.TopMentorDTO;
import com.example.edutrack.curriculum.repository.CourseMentorRepository;
import com.example.edutrack.curriculum.repository.CourseRepository;
import com.example.edutrack.accounts.service.interfaces.ManagerStatsService;
import com.example.edutrack.accounts.repository.MentorRepository;
import com.example.edutrack.accounts.repository.MenteeRepository;

@Service
public class ManagerStatsServiceImpl implements ManagerStatsService {
        @Autowired
        private TransactionRepository transactionRepository;

        @Autowired
        private CourseMentorRepository courseMentorRepository;

        @Autowired
        private MentorRepository mentorRepository;

        @Autowired
        private MenteeRepository menteeRepository;

        @Autowired
        public CourseRepository courseRepository;

        private final NumberFormat vndFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

        public ManagerStatsDTO getManagerStats(String period) {
                LocalDateTime startDate = getStartDateByPeriod(period);
                ManagerStatsDTO stats = new ManagerStatsDTO();

                // Tính toán doanh thu từ database
                Double totalRevenue = transactionRepository.getTotalRevenueFromDate(startDate);
                if (totalRevenue == null)
                        totalRevenue = 0.0;
                stats.setTotalRevenue(-totalRevenue == 0.0 ? 0.0 : -totalRevenue);

                // Tính toán tăng trưởng doanh thu
                LocalDateTime previousPeriodStart = getPreviousPeriodStart(period, startDate);
                Double previousRevenue = transactionRepository.getTotalRevenueFromDate(previousPeriodStart);
                if (previousRevenue == null)
                        previousRevenue = 0.0;

                Double revenueGrowth = calculateGrowthRate(-totalRevenue, -previousRevenue);
                stats.setRevenueGrowth(revenueGrowth);

                // Tính toán số mentor hoạt động (chỉ tính ACCEPTED)
                Long activeMentors = courseMentorRepository.getActiveMentorCount();
                stats.setTotalMentors(activeMentors != null ? activeMentors.intValue() : 0);
                // Tính toán tăng trưởng số mentor
                Long newMentors = mentorRepository.getNewMentorCountFromDate(startDate);
                stats.setMentorGrowth(newMentors != null ? newMentors.intValue() : 0);

                // Tính toán doanh thu trung bình per mentor
                if (activeMentors != null && activeMentors > 0) {
                        stats.setAvgRevenuePerMentor(-totalRevenue / activeMentors);
                } else {
                        stats.setAvgRevenuePerMentor(0.0);
                }

                // Tính toán đánh giá trung bình
                Double avgRating = mentorRepository.getAverageRating();
                if (avgRating != null) {
                        avgRating = Math.round(avgRating * 100.0) / 100.0; // Làm tròn đến 2 chữ số thập phân
                }
                stats.setAvgRating(avgRating != null ? avgRating : 0.0);

                // Tính toán số lượng học viên
                Long totalStudents = menteeRepository.count();
                stats.setTotalStudents(totalStudents != null ? totalStudents.intValue() : 0);

                // Tính toán tăng trưởng học viên
                Long newStudents = menteeRepository.getNewMenteeCountFromDate(startDate);
                stats.setStudentGrowth(newStudents != null ? newStudents.intValue() : 0);

                // Tính toán học viên trung bình per mentor
                if (activeMentors != null && activeMentors > 0 && totalStudents != null) {
                        double avgStudentsPerMentor = totalStudents.doubleValue() / activeMentors.doubleValue();
                        avgStudentsPerMentor = Math.round(avgStudentsPerMentor * 100.0) / 100.0;
                        stats.setAvgStudentsPerMentor(avgStudentsPerMentor);
                } else {
                        stats.setAvgStudentsPerMentor(0.0);
                }

                // đếm số lượng khóa học active
                Long activeCourses = courseRepository.countByIsOpen(true);
                stats.setActiveCourses(activeCourses != null ? activeCourses.intValue() : 0);

                stats.setSatisfactionRate(94.2);

                // Lấy dữ liệu biểu đồ doanh thu
                stats.setRevenueChart(getRevenueChartData(period, startDate));

                // Lấy top mentor
                stats.setTopMentors(getTopMentors(startDate));

                return stats;
        }

        public List<RevenueChartDTO> getRevenueChartData(String period, LocalDateTime startDate) {
                List<RevenueChartDTO> chartData = new ArrayList<>();

                if ("week".equals(period)) {
                        // Dữ liệu theo ngày
                        List<Object[]> dailyRevenue = transactionRepository.getDailyRevenueFromDate(startDate);
                        for (Object[] row : dailyRevenue) {
                                LocalDate date = ((java.sql.Date) row[0]).toLocalDate();
                                Double revenue = (Double) row[1];
                                String label = date.format(DateTimeFormatter.ofPattern("dd/MM"));
                                chartData.add(new RevenueChartDTO(date, revenue, label));
                        }
                } else if ("month".equals(period)) {
                        // Dữ liệu theo tháng
                        List<Object[]> monthlyRevenue = transactionRepository.getMonthlyRevenueFromDate(startDate);
                        for (Object[] row : monthlyRevenue) {
                                Integer year = (Integer) row[0];
                                Integer month = (Integer) row[1];
                                Double revenue = (Double) row[2];
                                LocalDate date = LocalDate.of(year, month, 1);
                                String label = String.format("%02d/%d", month, year);
                                chartData.add(new RevenueChartDTO(date, revenue, label));
                        }
                } else if ("quarter".equals(period)) {
                        // Dữ liệu theo quy
                        List<Object[]> quarterlyRevenue = transactionRepository.getQuarterlyRevenueFromDate(startDate);
                        for (Object[] row : quarterlyRevenue) {
                                Integer year = (Integer) row[0];
                                Integer quarter = (Integer) row[1];
                                Double revenue = (Double) row[2];
                                LocalDate date = LocalDate.of(year, (quarter - 1) * 3 + 1, 1);
                                String label = String.format("Q%d/%d", quarter, year);
                                chartData.add(new RevenueChartDTO(date, revenue, label));
                        }
                } else if ("year".equals(period)) {
                        // Dữ liệu theo năm
                        List<Object[]> yearlyRevenue = transactionRepository.getYearlyRevenueFromDate(startDate);
                        for (Object[] row : yearlyRevenue) {
                                Integer year = (Integer) row[0];
                                Double revenue = (Double) row[1];
                                LocalDate date = LocalDate.of(year, 1, 1);
                                String label = String.valueOf(year);
                                chartData.add(new RevenueChartDTO(date, revenue, label));
                        }
                }

                return chartData;
        }

        public List<TopMentorDTO> getTopMentors(LocalDateTime startDate) {
                // Convert LocalDateTime to Date
                Date start = Date.from(startDate.atZone(ZoneId.systemDefault()).toInstant());

                List<Object[]> topMentorsData = transactionRepository.getTopMentorsByRevenue(
                                startDate, PageRequest.of(0, 5));

                List<TopMentorDTO> topMentors = new ArrayList<>();
                int rank = 1;

                for (Object[] row : topMentorsData) {
                        UUID mentorId = (UUID) row[0];
                        String mentorName = (String) row[1];
                        Double totalRevenue = -(Double) row[2];

                        TopMentorDTO mentor = new TopMentorDTO(mentorId, mentorName, totalRevenue, rank);
                        mentor.setFormattedRevenue(formatCurrency(totalRevenue));
                        topMentors.add(mentor);
                        rank++;
                }
                int descRank = topMentors.size();
                List<TopMentorDTO> descTopMentor = new ArrayList<>();
                for (int i = topMentors.size() - 1; i >= 0; i--) {
                        TopMentorDTO mentor = topMentors.get(i);
                        mentor.setRank(descRank);
                        descTopMentor.add(mentor);
                        descRank--;
                }
                return descTopMentor;
        }

        private LocalDateTime getStartDateByPeriod(String period) {
                LocalDateTime now = LocalDateTime.now();
                switch (period) {
                        case "week":
                                return now.minusDays(7);
                        case "month":
                                return now.minusDays(30);
                        case "quarter":
                                return now.minusDays(90);
                        case "year":
                                return now.minusDays(365);
                        default:
                                return now.minusDays(7);
                }
        }

        private LocalDateTime getPreviousPeriodStart(String period, LocalDateTime currentStart) {
                switch (period) {
                        case "week":
                                return currentStart.minusDays(7);
                        case "month":
                                return currentStart.minusDays(30);
                        case "quarter":
                                return currentStart.minusDays(90);
                        case "year":
                                return currentStart.minusDays(365);
                        default:
                                return currentStart.minusDays(7);
                }
        }

        private Double calculateGrowthRate(Double current, Double previous) {
                if (previous == null || previous == 0)
                        return 0.0;
                return ((current - previous) / previous) * 100;
        }

        public String formatCurrency(Double amount) {
                if (amount == null)
                        return "0₫";
                return vndFormat.format(amount).replace("₫", "₫");
        }
}
