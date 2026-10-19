package com.fpoly.qldg.repo;

import com.fpoly.qldg.domain.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface Repos {
    interface CampusRepo extends JpaRepository<Campus, Long> {
        Optional<Campus> findByCode(String code);
    }

    interface FacultyRepo extends JpaRepository<Faculty, Long> {
        Optional<Faculty> findByCode(String code);
    }

    interface CampusFacultyRepo extends JpaRepository<CampusFaculty, Long> {
        List<CampusFaculty> findByCampusIdAndActiveTrue(Long campusId);
        Optional<CampusFaculty> findByCampusIdAndFacultyId(Long campusId, Long facultyId);
        List<CampusFaculty> findByCampusIdAndGroupKey(Long campusId, String groupKey);
        List<CampusFaculty> findByCnbmId(Long cnbmId);
        List<CampusFaculty> findByTmId(Long tmId);
    }

    interface SemesterRepo extends JpaRepository<Semester, Long> {
        Optional<Semester> findByTermAndYear(String term, int year);
        List<Semester> findAllByOrderByOrdinalDesc();
    }

    interface UserRepo extends JpaRepository<AppUser, Long> {
        Optional<AppUser> findByEmailAndCampusId(String email, Long campusId);
        Optional<AppUser> findByAccountAndCampusId(String account, Long campusId);
        List<AppUser> findByFacultyIdAndCampusIdAndActiveTrue(Long facultyId, Long campusId);
        List<AppUser> findByCampusIdAndActiveTrue(Long campusId);

        // UC-11: Tìm giảng viên toàn hệ thống, tối thiểu 2 ký tự
        @Query("SELECT u FROM AppUser u WHERE u.active = true AND " +
               "(LOWER(u.account) LIKE LOWER(CONCAT('%', :kw, '%')) OR " +
               " LOWER(u.fullName) LIKE LOWER(CONCAT('%', :kw, '%')))")
        List<AppUser> searchInspectorsGlobal(@Param("kw") String keyword);
    }

    interface SubjectRepo extends JpaRepository<Subject, Long> {
        Optional<Subject> findByCode(String code);
        List<Subject> findByFacultyId(Long facultyId);
    }

    interface TeachingRepo extends JpaRepository<Teaching, Long> {
        Optional<Teaching> findByClassCodeAndSubjectIdAndSemesterIdAndBlockAndTeacherId(
                String classCode, Long subjectId, Long semesterId, int block, Long teacherId);

        List<Teaching> findByCampusIdAndSemesterIdAndBlock(Long campusId, Long semesterId, int block);

        @Query("SELECT t FROM Teaching t WHERE t.campus.id = :campusId AND t.semester.id = :semesterId " +
               "AND t.block = :block AND t.subject.faculty.id IN :facultyIds")
        List<Teaching> findForFaculties(
                @Param("campusId") Long campusId,
                @Param("semesterId") Long semesterId,
                @Param("block") int block,
                @Param("facultyIds") List<Long> facultyIds);

        // Lấy lịch sử các kỳ trước để kiểm tra lần đầu dạy / nghỉ 6 kỳ
        @Query("SELECT t FROM Teaching t WHERE t.teacher.id = :teacherId AND t.subject.code = :subjectCode " +
               "ORDER BY t.semester.ordinal DESC")
        List<Teaching> findHistoryByTeacherAndSubject(
                @Param("teacherId") Long teacherId,
                @Param("subjectCode") String subjectCode);
    }

    interface CalendarSessionRepo extends JpaRepository<CalendarSession, Long> {
        List<CalendarSession> findByTeachingIdOrderByDateAscShiftAsc(Long teachingId);
        List<CalendarSession> findByTeachingIdAndSessionNo(Long teachingId, Integer sessionNo);
        List<CalendarSession> findByTeachingIdAndDateAndShift(Long teachingId, java.time.LocalDate date, int shift);
        boolean existsByTeachingId(Long teachingId);
    }

    interface ImportJobRepo extends JpaRepository<ImportJob, Long> {
        Page<ImportJob> findByCampusIdOrderByCreatedAtDesc(Long campusId, Pageable pageable);
    }

    interface ImportRowRepo extends JpaRepository<ImportRow, Long> {
        Page<ImportRow> findByJobIdOrderByRowNoAsc(Long jobId, Pageable pageable);
        List<ImportRow> findByJobId(Long jobId);
        long countByJobIdAndResult(Long jobId, String result);
    }

    interface NewSubjectAlertRepo extends JpaRepository<NewSubjectAlert, Long> {
        List<NewSubjectAlert> findByJobId(Long jobId);
    }

    interface NotificationRepo extends JpaRepository<Notification, Long> {
        List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);
    }

    interface InspectionRepo extends JpaRepository<Inspection, Long> {
        Optional<Inspection> findByCode(String code);

        @Query("SELECT i FROM Inspection i WHERE i.campus.id = :campusId AND i.semester.id = :semesterId " +
               "AND i.block = :block AND i.faculty.id IN :facultyIds ORDER BY i.createdAt DESC")
        List<Inspection> findList(
                @Param("campusId") Long campusId,
                @Param("semesterId") Long semesterId,
                @Param("block") int block,
                @Param("facultyIds") List<Long> facultyIds);

        @Query("SELECT i FROM Inspection i WHERE i.campus.id = :campusId AND i.semester.id = :semesterId " +
               "AND i.block = :block AND i.status = 'APPROVED'")
        List<Inspection> findApproved(
                @Param("campusId") Long campusId,
                @Param("semesterId") Long semesterId,
                @Param("block") int block);
    }

    interface InspectionDetailRepo extends JpaRepository<InspectionDetail, Long> {
        List<InspectionDetail> findByInspectionId(Long inspectionId);

        @Query("SELECT d FROM InspectionDetail d WHERE d.teaching.semester.id = :semesterId " +
               "AND d.teaching.block = :block AND d.inspection.status <> 'CANCELLED'")
        List<InspectionDetail> findActiveInBlock(
                @Param("semesterId") Long semesterId,
                @Param("block") int block);
    }

    interface InspectionHistoryRepo extends JpaRepository<InspectionHistory, Long> {
        List<InspectionHistory> findByInspectionIdOrderByCreatedAtAsc(Long inspectionId);
    }
}
