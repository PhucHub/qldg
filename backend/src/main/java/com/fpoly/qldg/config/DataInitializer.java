package com.fpoly.qldg.config;

import com.fpoly.qldg.domain.*;
import com.fpoly.qldg.repo.Repos.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;

/**
 * Nạp dữ liệu nền mẫu: 14 cơ sở, 20 bộ môn toàn cục, các tài khoản đủ vai
 * (Head Office, TBĐT, TC&QLĐT, CNBM, TM, GV), mốc kỳ Fall 2026 (Block 1 & 2),
 * các môn học, phân công giảng dạy và buổi học mẫu để test ngay lập tức.
 */
@Component
public class DataInitializer implements CommandLineRunner {
    private final CampusRepo campusRepo;
    private final FacultyRepo facultyRepo;
    private final CampusFacultyRepo campusFacultyRepo;
    private final SemesterRepo semesterRepo;
    private final UserRepo userRepo;
    private final SubjectRepo subjectRepo;
    private final TeachingRepo teachingRepo;
    private final CalendarSessionRepo calendarRepo;

    public DataInitializer(
            CampusRepo campusRepo, FacultyRepo facultyRepo, CampusFacultyRepo campusFacultyRepo,
            SemesterRepo semesterRepo, UserRepo userRepo, SubjectRepo subjectRepo,
            TeachingRepo teachingRepo, CalendarSessionRepo calendarRepo) {
        this.campusRepo = campusRepo;
        this.facultyRepo = facultyRepo;
        this.campusFacultyRepo = campusFacultyRepo;
        this.semesterRepo = semesterRepo;
        this.userRepo = userRepo;
        this.subjectRepo = subjectRepo;
        this.teachingRepo = teachingRepo;
        this.calendarRepo = calendarRepo;
    }

    @Override
    public void run(String... args) {
        if (campusRepo.count() > 0) return; // Đã có dữ liệu

        // 1. 14 Cơ sở FPT Polytechnic
        String[][] campusData = {
                {"dna", "CĐ FPT Polytechnic Đà Nẵng", "137 Nguyễn Thị Thập, Liên Chiểu, Đà Nẵng"},
                {"hn", "CĐ FPT Polytechnic Hà Nội", "Trịnh Văn Bô, Nam Từ Liêm, Hà Nội"},
                {"hcm", "CĐ FPT Polytechnic TP.HCM", "Quang Trung, Gò Vấp, TP.HCM"},
                {"ct", "CĐ FPT Polytechnic Cần Thơ", "Số 288 Nguyễn Văn Cừ, An Hòa, Cần Thơ"},
                {"tn", "CĐ FPT Polytechnic Tây Nguyên", "300/6 Hà Huy Tập, Buôn Ma Thuột"},
                {"hp", "CĐ FPT Polytechnic Hải Phòng", "Hải Phòng"},
                {"bd", "CĐ FPT Polytechnic Bình Định", "Quy Nhơn, Bình Định"},
                {"th", "CĐ FPT Polytechnic Thanh Hóa", "Thanh Hóa"},
                {"qn", "CĐ FPT Polytechnic Quảng Ninh", "Quảng Ninh"},
                {"dn", "CĐ FPT Polytechnic Đồng Nai", "Đồng Nai"},
                {"vt", "CĐ FPT Polytechnic Vũng Tàu", "Bà Rịa - Vũng Tàu"},
                {"nt", "CĐ FPT Polytechnic Nha Trang", "Nha Trang, Khánh Hòa"},
                {"bdu", "CĐ FPT Polytechnic Bình Dương", "Bình Dương"},
                {"gl", "CĐ FPT Polytechnic Gia Lai", "Pleiku, Gia Lai"}
        };
        Map<String, Campus> campusMap = new HashMap<>();
        for (String[] cd : campusData) {
            Campus c = new Campus();
            c.code = cd[0];
            c.name = cd[1];
            c.address = cd[2];
            campusMap.put(c.code, campusRepo.save(c));
        }

        // 2. 20 Bộ môn toàn cục (faculties - không có cột cơ sở)
        String[][] facultyData = {
                {"CNTT", "Công nghệ thông tin"},
                {"CB", "Cơ bản"},
                {"TA", "Tiếng Anh"},
                {"TT", "Tiếng Trung"},
                {"QTKD", "Quản trị kinh doanh"},
                {"TMDZ", "Thương mại điện tử"},
                {"TKDH", "Thiết kế đồ họa"},
                {"UDPM", "Ứng dụng phần mềm"},
                {"DL", "Du lịch & Khách sạn"},
                {"CSD", "Chăm sóc sắc đẹp"},
                {"TDTT", "Thể dục thể thao"},
                {"KT", "Kế toán"},
                {"XLA", "Xử lý ảnh & Video"},
                {"IOT", "Internet of Things"},
                {"MKT", "Marketing & Truyền thông"},
                {"LG", "Logistics & Chuỗi cung ứng"},
                {"TDH", "Tự động hóa"},
                {"CK", "Cơ khí & Chế tạo"},
                {"ATTT", "An toàn thông tin"},
                {"LTDD", "Lập trình di động"}
        };
        Map<String, Faculty> facultyMap = new HashMap<>();
        for (String[] fd : facultyData) {
            Faculty f = new Faculty();
            f.code = fd[0];
            f.name = fd[1];
            facultyMap.put(f.code, facultyRepo.save(f));
        }

        // 3. Mốc kỳ học: 2026 (Spring, Summer, Fall)
        // Fall 2026: Block 1 (01/09 - 25/10/2026), Block 2 (26/10 - 20/12/2026)
        Semester fa26 = new Semester();
        fa26.term = "FALL";
        fa26.year = 2026;
        fa26.ordinal = Semester.ordinalOf("FALL", 2026);
        fa26.startDate = LocalDate.of(2026, 9, 1);
        fa26.block1End = LocalDate.of(2026, 10, 25);
        fa26.block2Start = LocalDate.of(2026, 10, 26);
        fa26.endDate = LocalDate.of(2026, 12, 20);
        fa26 = semesterRepo.save(fa26);

        Semester su26 = new Semester();
        su26.term = "SUMMER";
        su26.year = 2026;
        su26.ordinal = Semester.ordinalOf("SUMMER", 2026);
        su26.startDate = LocalDate.of(2026, 5, 2);
        su26.block1End = LocalDate.of(2026, 6, 25);
        su26.block2Start = LocalDate.of(2026, 6, 26);
        su26.endDate = LocalDate.of(2026, 8, 25);
        semesterRepo.save(su26);

        Semester sp26 = new Semester();
        sp26.term = "SPRING";
        sp26.year = 2026;
        sp26.ordinal = Semester.ordinalOf("SPRING", 2026);
        sp26.startDate = LocalDate.of(2026, 1, 5);
        sp26.block1End = LocalDate.of(2026, 2, 28);
        sp26.block2Start = LocalDate.of(2026, 3, 1);
        sp26.endDate = LocalDate.of(2026, 4, 30);
        semesterRepo.save(sp26);

        // 4. Người dùng mẫu
        Campus dna = campusMap.get("dna");
        Campus hn = campusMap.get("hn");
        Campus hcm = campusMap.get("hcm");
        Faculty cntt = facultyMap.get("CNTT");
        Faculty cb = facultyMap.get("CB");
        Faculty ta = facultyMap.get("TA");

        // Super Admin (Head Office)
        AppUser ho = createUser("ho_admin", "ho_admin@fpt.edu.vn", "Nguyễn Văn Hùng", "00100001", "HEAD_OFFICE", dna, null, true);

        // TBĐT Đà Nẵng
        AppUser tbdtDna = createUser("loanth", "loanth@fpt.edu.vn", "Hoàng Thị Loan", "00246900", "TBDT", dna, null, false);
        // TC&QLĐT Đà Nẵng
        AppUser tcqldtDna = createUser("tuanna", "tuanna@fpt.edu.vn", "Nguyễn Anh Tuấn", "00246901", "TCQLDT", dna, null, false);

        // CNBM CNTT Đà Nẵng
        AppUser cnbmCntt = createUser("dungpt", "dungpt@fpt.edu.vn", "Phan Tiến Dũng", "00246910", "CNBM", dna, cntt, false);
        // TM Lập trình Web (thuộc bộ môn CNTT)
        AppUser tmWeb = createUser("haidv", "haidv@fpt.edu.vn", "Đỗ Văn Hải", "00246911", "TM", dna, cntt, false);

        // CNBM Cơ bản (Quản lý nhóm CB gồm: Cơ bản + Tiếng Anh + Tiếng Trung)
        AppUser cnbmCb = createUser("nhungpt4", "nhungpt4@fpt.edu.vn", "Phạm Thị Nhung", "00246920", "CNBM", dna, cb, false);

        // Giảng viên Đà Nẵng
        AppUser gvNam = createUser("namnv", "namnv@fpt.edu.vn", "Nguyễn Văn Nam", "00246931", "GV", dna, cntt, false);
        AppUser gvLinh = createUser("linhdt", "linhdt@fpt.edu.vn", "Đặng Thùy Linh", "00246932", "GV", dna, cntt, false);
        AppUser gvTung = createUser("tunglt", "tunglt@fpt.edu.vn", "Lê Thanh Tùng", "00246933", "GV", dna, cntt, false);
        AppUser gvHah = createUser("hah7", "hah7@fpt.edu.vn", "Hoàng Anh Huy", "00246934", "GV", dna, cntt, false);
        AppUser gvHuong = createUser("huongnt", "huongnt@fpt.edu.vn", "Nguyễn Thị Hương", "00246935", "GV", dna, cb, false);

        // Giảng viên cơ sở khác (để test dự giờ chéo UC-11)
        AppUser gvHn = createUser("nhungpt_hn", "nhungpt_hn@fpt.edu.vn", "Phạm Thu Nhung", "00188901", "GV", hn, cb, false);
        AppUser gvHcm = createUser("minhtv", "minhtv@fpt.edu.vn", "Trần Văn Minh", "00388902", "GV", hcm, cntt, false);

        // 5. Cấu hình Cơ cấu bộ môn tại Đà Nẵng (campus_faculty)
        // Bật CNTT (đứng riêng, CNBM = dungpt, TM = haidv)
        CampusFaculty cfCntt = new CampusFaculty();
        cfCntt.campus = dna;
        cfCntt.faculty = cntt;
        cfCntt.active = true;
        cfCntt.groupKey = "CNTT";
        cfCntt.cnbm = cnbmCntt;
        cfCntt.tm = tmWeb;
        campusFacultyRepo.save(cfCntt);

        // Nhóm CB (Cơ bản + Tiếng Anh + Tiếng Trung gộp chung group_key "CB", cùng CNBM nhungpt4)
        for (String fCode : List.of("CB", "TA", "TT")) {
            CampusFaculty cf = new CampusFaculty();
            cf.campus = dna;
            cf.faculty = facultyMap.get(fCode);
            cf.active = true;
            cf.groupKey = "CB";
            cf.cnbm = cnbmCb;
            campusFacultyRepo.save(cf);
        }

        // Bật thêm TKDH, QTKD tại Đà Nẵng
        for (String fCode : List.of("TKDH", "QTKD", "UDPM")) {
            CampusFaculty cf = new CampusFaculty();
            cf.campus = dna;
            cf.faculty = facultyMap.get(fCode);
            cf.active = true;
            campusFacultyRepo.save(cf);
        }

        // 6. Môn học
        Subject com108 = createSubject("COM108", "Nhập môn lập trình", cntt, false);
        Subject pro2113 = createSubject("PRO2113", "Dự án tốt nghiệp (CNTT)", cntt, false);
        Subject web1013 = createSubject("WEB1013", "Thiết kế Web với HTML5 & CSS3", cntt, false);
        Subject mob1014 = createSubject("MOB1014", "Lập trình Java 1", cntt, false);
        Subject eng101 = createSubject("ENG101", "Tiếng Anh 1.1", ta, false);
        Subject mat101 = createSubject("MAT101", "Toán rời rạc", cb, false);

        // 7. Phân công giảng dạy mẫu cho Fall 2026, Block 1
        // Lớp 1: WD18301 - COM108 - GV: namnv (Đã từng dạy -> Nhóm các lớp khác)
        Teaching t1 = createTeaching("WD18301", com108, fa26, 1, gvNam, dna);
        createSessions(t1, gvNam, List.of(
                LocalDate.of(2026, 10, 20), LocalDate.of(2026, 10, 22),
                LocalDate.of(2026, 10, 24), LocalDate.of(2026, 10, 25)), 2, "F201");

        // Lớp 2: CP18301 - PRO2113 - GV: linhdt (Lần đầu dạy môn này -> Nhóm bắt buộc)
        Teaching t2 = createTeaching("CP18301", pro2113, fa26, 1, gvLinh, dna);
        createSessions(t2, gvLinh, List.of(
                LocalDate.of(2026, 10, 21), LocalDate.of(2026, 10, 23),
                LocalDate.of(2026, 10, 25)), 3, "F202");

        // Lớp 3: IT18302 - MOB1014 - GV: tunglt (Chưa từng dạy -> Bắt buộc)
        Teaching t3 = createTeaching("IT18302", mob1014, fa26, 1, gvTung, dna);
        createSessions(t3, gvTung, List.of(
                LocalDate.of(2026, 10, 20), LocalDate.of(2026, 10, 22)), 1, "L102");

        // Lớp 4: IT18303 - WEB1013 - GV: hah7 (Đã từng dạy -> Chọn thêm)
        Teaching t4 = createTeaching("IT18303", web1013, fa26, 1, gvHah, dna);
        createSessions(t4, gvHah, List.of(
                LocalDate.of(2026, 10, 21), LocalDate.of(2026, 10, 23)), 4, "L103");

        // Lớp 5: CB18301 - ENG101 - GV: huongnt
        Teaching t5 = createTeaching("CB18301", eng101, fa26, 1, gvHuong, dna);
        createSessions(t5, gvHuong, List.of(
                LocalDate.of(2026, 10, 20), LocalDate.of(2026, 10, 22)), 1, "T301");

        // Tạo lịch sử quá khứ cho gvNam từng dạy COM108 ở Spring 2026 để kiểm tra logic
        createTeaching("WD17301", com108, sp26, 1, gvNam, dna);
    }

    private AppUser createUser(
            String account, String email, String fullName, String staffCode,
            String role, Campus campus, Faculty faculty, boolean ho) {
        AppUser u = new AppUser();
        u.account = account;
        u.email = email;
        u.fullName = fullName;
        u.staffCode = staffCode;
        u.role = role;
        u.campus = campus;
        u.faculty = faculty;
        u.headOffice = ho;
        return userRepo.save(u);
    }

    private Subject createSubject(String code, String name, Faculty f, boolean auto) {
        Subject s = new Subject();
        s.code = code;
        s.name = name;
        s.faculty = f;
        s.autoCreated = auto;
        return subjectRepo.save(s);
    }

    private Teaching createTeaching(
            String classCode, Subject s, Semester sem, int block, AppUser t, Campus c) {
        Teaching th = new Teaching();
        th.classCode = classCode;
        th.subject = s;
        th.semester = sem;
        th.block = block;
        th.teacher = t;
        th.campus = c;
        return teachingRepo.save(th);
    }

    private void createSessions(Teaching t, AppUser teacher, List<LocalDate> dates, int shift, String room) {
        int no = 1;
        for (LocalDate d : dates) {
            CalendarSession s = new CalendarSession();
            s.teaching = t;
            s.teacher = teacher;
            s.date = d;
            s.shift = shift;
            s.room = room;
            s.sessionNo = no++;
            calendarRepo.save(s);
        }
    }
}
