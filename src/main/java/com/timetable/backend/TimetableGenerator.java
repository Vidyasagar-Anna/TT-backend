package com.timetable.backend;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class TimetableGenerator {

    private final SubjectRepository subjectRepository;
    private final FacultyRepository facultyRepository;
    private final RoomRepository roomRepository;
    private final SectionRepository sectionRepository;

    public TimetableGenerator(
            SubjectRepository subjectRepository,
            FacultyRepository facultyRepository,
            RoomRepository roomRepository,
            SectionRepository sectionRepository) {

        this.subjectRepository = subjectRepository;
        this.facultyRepository = facultyRepository;
        this.roomRepository = roomRepository;
        this.sectionRepository = sectionRepository;
    }

    // =========================================================
    // MAIN TIMETABLE GENERATOR
    // =========================================================

    public List<Timetable> generateTimetable() {

        List<Timetable> result = new ArrayList<>();

        List<Subject> subjects = subjectRepository.findAll();
        List<Faculty> faculties = facultyRepository.findAll();
        List<Room> rooms = roomRepository.findAll();
        List<Section> sections = sectionRepository.findAll();

        // ---------------------------------------------------------
        // Check required data
        // ---------------------------------------------------------

        if (subjects.isEmpty()) {
            System.out.println("No subjects found.");
            return result;
        }

        if (faculties.isEmpty()) {
            System.out.println("No faculties found.");
            return result;
        }

        if (rooms.isEmpty()) {
            System.out.println("No rooms found.");
            return result;
        }

        if (sections.isEmpty()) {
            System.out.println("No sections found.");
            return result;
        }

        // ---------------------------------------------------------
        // Working days
        // Monday is holiday
        // Tuesday to Saturday are working days
        // ---------------------------------------------------------

        String[] days = {
                "Tuesday",
                "Wednesday",
                "Thursday",
                "Friday",
                "Saturday"
        };

        // ---------------------------------------------------------
        // Periods
        // ---------------------------------------------------------

        String[] times = {
                "9:00-9:55 AM",
                "9:55-10:50 AM",
                "10:50-11:45 AM",
                "11:45-12:40 PM",
                "1:20-2:15 PM",
                "2:15-3:10 PM",
                "3:10-4:05 PM"
        };

        // =========================================================
        // PROCESS EACH SECTION
        // =========================================================

        for (Section section : sections) {

            System.out.println(
                    "Generating timetable for section: "
                            + section.getName());

            List<Subject> sectionSubjects =
                    getMatchingSubjects(subjects, section);

            if (sectionSubjects.isEmpty()) {

                System.out.println(
                        "No matching subjects for section: "
                                + section.getName());

                continue;
            }

            // -----------------------------------------------------
            // Separate labs and theory subjects
            // -----------------------------------------------------

            List<Subject> labSubjects = new ArrayList<>();
            List<Subject> theorySubjects = new ArrayList<>();

            for (Subject subject : sectionSubjects) {

                if (isLab(subject)) {
                    labSubjects.add(subject);
                } else {
                    theorySubjects.add(subject);
                }
            }

            // -----------------------------------------------------
            // Track number of periods for each subject
            // -----------------------------------------------------

            Map<Long, Integer> subjectCount = new HashMap<>();

            for (Subject subject : sectionSubjects) {

                subjectCount.put(
                        subject.getId(),
                        0
                );
            }

            // =====================================================
            // STEP 1: GENERATE LABS
            // LAB = 3 CONSECUTIVE PERIODS
            // =====================================================

            for (Subject lab : labSubjects) {

                boolean labScheduled = false;

                // -------------------------------------------------
                // Try every day
                // -------------------------------------------------

                for (String day : days) {

                    if (labScheduled) {
                        break;
                    }

                    // -------------------------------------------------
                    // Need 3 consecutive periods
                    // -------------------------------------------------

                    for (int start = 0;
                         start <= times.length - 3;
                         start++) {

                        if (canScheduleLab(
                                result,
                                section,
                                lab,
                                day,
                                times,
                                start,
                                faculties,
                                rooms)) {

                            // -------------------------------------------------
                            // Find faculty for first period
                            // -------------------------------------------------

                            Faculty faculty =
                                    findFaculty(
                                            lab,
                                            section,
                                            faculties,
                                            result,
                                            day,
                                            times[start]);

                            // -------------------------------------------------
                            // Find lab room
                            // -------------------------------------------------

                            Room room =
                                    findLabRoom(
                                            section,
                                            lab,
                                            rooms,
                                            result,
                                            day,
                                            times[start]);

                            if (faculty == null || room == null) {
                                continue;
                            }

                            // -------------------------------------------------
                            // Add 3 consecutive periods
                            // -------------------------------------------------

                            for (int offset = 0; offset < 3; offset++) {

                                String time =
                                        times[start + offset];

                                Timetable entry =
                                        new Timetable(
                                                day,
                                                time,
                                                section.getName(),
                                                lab.getName(),
                                                faculty.getName(),
                                                room.getRoomNumber()
                                        );

                                result.add(entry);
                            }

                            // -------------------------------------------------
                            // Lab count = 3
                            // -------------------------------------------------

                            subjectCount.put(
                                    lab.getId(),
                                    3
                            );

                            labScheduled = true;

                            System.out.println(
                                    "Lab scheduled: "
                                            + lab.getName()
                                            + " -> "
                                            + section.getName()
                                            + " -> "
                                            + day
                            );

                            break;
                        }
                    }
                }

                if (!labScheduled) {

                    System.out.println(
                            "WARNING: Could not schedule lab "
                                    + lab.getName()
                                    + " for section "
                                    + section.getName()
                    );
                }
            }

            // =====================================================
            // STEP 2: GENERATE THEORY SUBJECTS
            //
            // EACH THEORY SUBJECT = 3 PERIODS PER WEEK
            //
            // IMPORTANT:
            // MAXIMUM 1 PERIOD OF SAME SUBJECT PER DAY
            //
            // Example:
            //
            // Tuesday    -> EMTL
            // Wednesday  -> EMTL
            // Thursday   -> EMTL
            //
            // NOT:
            //
            // Tuesday    -> EMTL
            // Tuesday    -> EMTL
            // Tuesday    -> EMTL
            // =====================================================

            for (Subject subject : theorySubjects) {

                int requiredPeriods = 3;

                // -------------------------------------------------
                // Store days already used by this subject
                // -------------------------------------------------

                Set<String> subjectDays = new HashSet<>();

                while (subjectCount.get(subject.getId())
                        < requiredPeriods) {

                    boolean scheduled = false;

                    // -------------------------------------------------
                    // Try every day
                    // -------------------------------------------------

                    for (String day : days) {

                        if (scheduled) {
                            break;
                        }

                        // -------------------------------------------------
                        // DO NOT SCHEDULE SAME THEORY SUBJECT
                        // TWICE ON SAME DAY
                        // -------------------------------------------------

                        if (subjectDays.contains(day)) {
                            continue;
                        }

                        // -------------------------------------------------
                        // Try every period
                        // -------------------------------------------------

                        for (String time : times) {

                            // -------------------------------------------------
                            // Section conflict
                            // -------------------------------------------------

                            if (sectionBusy(
                                    result,
                                    section.getName(),
                                    day,
                                    time)) {

                                continue;
                            }

                            // -------------------------------------------------
                            // Find faculty
                            // -------------------------------------------------

                            Faculty faculty =
                                    findFaculty(
                                            subject,
                                            section,
                                            faculties,
                                            result,
                                            day,
                                            time);

                            if (faculty == null) {
                                continue;
                            }

                            // -------------------------------------------------
                            // Find theory room
                            // -------------------------------------------------

                            Room room =
                                    findTheoryRoom(
                                            section,
                                            subject,
                                            rooms,
                                            result,
                                            day,
                                            time);

                            if (room == null) {
                                continue;
                            }

                            // -------------------------------------------------
                            // Create timetable entry
                            // -------------------------------------------------

                            Timetable entry =
                                    new Timetable(
                                            day,
                                            time,
                                            section.getName(),
                                            subject.getName(),
                                            faculty.getName(),
                                            room.getRoomNumber()
                                    );

                            result.add(entry);

                            // -------------------------------------------------
                            // Increase subject count
                            // -------------------------------------------------

                            subjectCount.put(
                                    subject.getId(),
                                    subjectCount.get(subject.getId()) + 1
                            );

                            // -------------------------------------------------
                            // Remember this day
                            // -------------------------------------------------

                            subjectDays.add(day);

                            scheduled = true;

                            break;
                        }
                    }

                    // -------------------------------------------------
                    // Prevent infinite loop
                    // -------------------------------------------------

                    if (!scheduled) {

                        System.out.println(
                                "WARNING: Could not schedule remaining "
                                        + "periods for subject "
                                        + subject.getName()
                                        + " in section "
                                        + section.getName()
                        );

                        break;
                    }
                }

                System.out.println(
                        "Subject: "
                                + subject.getName()
                                + " -> "
                                + section.getName()
                                + " -> "
                                + subjectCount.get(subject.getId())
                                + " periods"
                );
            }
        }

        // =========================================================
        // TOTAL RECORDS
        // =========================================================

        System.out.println(
                "TOTAL GENERATED RECORDS = "
                        + result.size()
        );

        return result;
    }

    // =========================================================
    // MATCH SUBJECTS WITH SECTION
    // =========================================================

    private List<Subject> getMatchingSubjects(
            List<Subject> subjects,
            Section section) {

        List<Subject> matching = new ArrayList<>();

        for (Subject subject : subjects) {

            if (subject.getDepartment() == null
                    || section.getDepartment() == null) {

                continue;
            }

            // -----------------------------------------------------
            // Department matching
            // -----------------------------------------------------

            if (!subject.getDepartment()
                    .equalsIgnoreCase(
                            section.getDepartment())) {

                continue;
            }

            // -----------------------------------------------------
            // Semester matching
            // -----------------------------------------------------

            if (subject.getSemester() != null
                    && section.getSemester() != null) {

                if (!subject.getSemester()
                        .equalsIgnoreCase(
                                section.getSemester())) {

                    continue;
                }
            }

            matching.add(subject);
        }

        return matching;
    }

    // =========================================================
    // IDENTIFY LAB
    // =========================================================

    private boolean isLab(Subject subject) {

        if (subject.getName() == null) {
            return false;
        }

        String name =
                subject.getName()
                        .trim()
                        .toLowerCase();

        return name.contains("lab");
    }

    // =========================================================
    // CHECK LAB 3 CONSECUTIVE PERIODS
    // =========================================================

    private boolean canScheduleLab(
            List<Timetable> result,
            Section section,
            Subject lab,
            String day,
            String[] times,
            int start,
            List<Faculty> faculties,
            List<Room> rooms) {

        // ---------------------------------------------------------
        // Check all 3 periods first
        // ---------------------------------------------------------

        for (int i = 0; i < 3; i++) {

            String time = times[start + i];

            // -----------------------------------------------------
            // Section conflict
            // -----------------------------------------------------

            if (sectionBusy(
                    result,
                    section.getName(),
                    day,
                    time)) {

                return false;
            }

            // -----------------------------------------------------
            // Faculty conflict
            // -----------------------------------------------------

            Faculty faculty =
                    findFaculty(
                            lab,
                            section,
                            faculties,
                            result,
                            day,
                            time);

            if (faculty == null) {
                return false;
            }

            // -----------------------------------------------------
            // Lab room conflict
            // -----------------------------------------------------

            Room room =
                    findLabRoom(
                            section,
                            lab,
                            rooms,
                            result,
                            day,
                            time);

            if (room == null) {
                return false;
            }
        }

        return true;
    }

    // =========================================================
    // FIND FACULTY
    // =========================================================

    private Faculty findFaculty(
            Subject subject,
            Section section,
            List<Faculty> faculties,
            List<Timetable> result,
            String day,
            String time) {

        // ---------------------------------------------------------
        // First preference:
        // Faculty assigned to subject
        // ---------------------------------------------------------

        if (subject.getFaculty() != null
                && !subject.getFaculty()
                .trim()
                .isEmpty()) {

            for (Faculty faculty : faculties) {

                if (faculty.getName() == null) {
                    continue;
                }

                if (!faculty.getName()
                        .equalsIgnoreCase(
                                subject.getFaculty()
                                        .trim())) {

                    continue;
                }

                // -------------------------------------------------
                // Department check
                // -------------------------------------------------

                if (faculty.getDepartment() != null
                        && section.getDepartment() != null
                        && !faculty.getDepartment()
                        .equalsIgnoreCase(
                                section.getDepartment())) {

                    continue;
                }

                // -------------------------------------------------
                // Faculty conflict
                // -------------------------------------------------

                if (facultyBusy(
                        result,
                        faculty.getName(),
                        day,
                        time)) {

                    return null;
                }

                return faculty;
            }
        }

        // ---------------------------------------------------------
        // Second preference:
        // Any available faculty from department
        // ---------------------------------------------------------

        for (Faculty faculty : faculties) {

            if (faculty.getName() == null) {
                continue;
            }

            if (faculty.getDepartment() != null
                    && section.getDepartment() != null
                    && !faculty.getDepartment()
                    .equalsIgnoreCase(
                            section.getDepartment())) {

                continue;
            }

            if (facultyBusy(
                    result,
                    faculty.getName(),
                    day,
                    time)) {

                continue;
            }

            return faculty;
        }

        return null;
    }

    // =========================================================
    // FIND LAB ROOM
    // =========================================================

    private Room findLabRoom(
            Section section,
            Subject subject,
            List<Room> rooms,
            List<Timetable> result,
            String day,
            String time) {

        // ---------------------------------------------------------
        // First find an actual LAB room
        // ---------------------------------------------------------

        for (Room room : rooms) {

            if (room.getRoomNumber() == null) {
                continue;
            }

            if (room.getRoomType() == null) {
                continue;
            }

            if (!room.getRoomType()
                    .toLowerCase()
                    .contains("lab")) {

                continue;
            }

            // -----------------------------------------------------
            // Department check
            // -----------------------------------------------------

            if (room.getDepartment() != null
                    && section.getDepartment() != null
                    && !room.getDepartment()
                    .equalsIgnoreCase(
                            section.getDepartment())) {

                continue;
            }

            // -----------------------------------------------------
            // Room conflict
            // -----------------------------------------------------

            if (roomBusy(
                    result,
                    room.getRoomNumber(),
                    day,
                    time)) {

                continue;
            }

            return room;
        }

        // ---------------------------------------------------------
        // If no lab room exists,
        // use any free department room
        // ---------------------------------------------------------

        for (Room room : rooms) {

            if (room.getRoomNumber() == null) {
                continue;
            }

            if (roomBusy(
                    result,
                    room.getRoomNumber(),
                    day,
                    time)) {

                continue;
            }

            if (room.getDepartment() != null
                    && section.getDepartment() != null
                    && !room.getDepartment()
                    .equalsIgnoreCase(
                            section.getDepartment())) {

                continue;
            }

            return room;
        }

        return null;
    }

    // =========================================================
    // FIND THEORY ROOM
    // =========================================================

    private Room findTheoryRoom(
            Section section,
            Subject subject,
            List<Room> rooms,
            List<Timetable> result,
            String day,
            String time) {

        // ---------------------------------------------------------
        // First preference:
        // Section classroom
        // ---------------------------------------------------------

        if (section.getClassRoom() != null
                && !section.getClassRoom()
                .trim()
                .isEmpty()) {

            for (Room room : rooms) {

                if (room.getRoomNumber() == null) {
                    continue;
                }

                if (!room.getRoomNumber()
                        .equalsIgnoreCase(
                                section.getClassRoom()
                                        .trim())) {

                    continue;
                }

                if (roomBusy(
                        result,
                        room.getRoomNumber(),
                        day,
                        time)) {

                    continue;
                }

                return room;
            }
        }

        // ---------------------------------------------------------
        // Second preference:
        // Department room
        // ---------------------------------------------------------

        for (Room room : rooms) {

            if (room.getRoomNumber() == null) {
                continue;
            }

            if (roomBusy(
                    result,
                    room.getRoomNumber(),
                    day,
                    time)) {

                continue;
            }

            if (room.getDepartment() != null
                    && section.getDepartment() != null
                    && !room.getDepartment()
                    .equalsIgnoreCase(
                            section.getDepartment())) {

                continue;
            }

            // -----------------------------------------------------
            // Do not use lab rooms for theory
            // -----------------------------------------------------

            if (room.getRoomType() != null
                    && room.getRoomType()
                    .toLowerCase()
                    .contains("lab")) {

                continue;
            }

            return room;
        }

        // ---------------------------------------------------------
        // Final fallback
        // ---------------------------------------------------------

        for (Room room : rooms) {

            if (room.getRoomNumber() == null) {
                continue;
            }

            if (roomBusy(
                    result,
                    room.getRoomNumber(),
                    day,
                    time)) {

                continue;
            }

            return room;
        }

        return null;
    }

    // =========================================================
    // SECTION CONFLICT
    // =========================================================

    private boolean sectionBusy(
            List<Timetable> result,
            String section,
            String day,
            String time) {

        for (Timetable entry : result) {

            if (entry.getSection() == null) {
                continue;
            }

            if (entry.getDay() == null
                    || entry.getTime() == null) {

                continue;
            }

            if (entry.getSection()
                    .equalsIgnoreCase(section)

                    && entry.getDay()
                    .equalsIgnoreCase(day)

                    && entry.getTime()
                    .equalsIgnoreCase(time)) {

                return true;
            }
        }

        return false;
    }

    // =========================================================
    // FACULTY CONFLICT
    // =========================================================

    private boolean facultyBusy(
            List<Timetable> result,
            String faculty,
            String day,
            String time) {

        for (Timetable entry : result) {

            if (entry.getFaculty() == null) {
                continue;
            }

            if (entry.getDay() == null
                    || entry.getTime() == null) {

                continue;
            }

            if (entry.getFaculty()
                    .equalsIgnoreCase(faculty)

                    && entry.getDay()
                    .equalsIgnoreCase(day)

                    && entry.getTime()
                    .equalsIgnoreCase(time)) {

                return true;
            }
        }

        return false;
    }

    // =========================================================
    // ROOM CONFLICT
    // =========================================================

    private boolean roomBusy(
            List<Timetable> result,
            String room,
            String day,
            String time) {

        for (Timetable entry : result) {

            if (entry.getRoom() == null) {
                continue;
            }

            if (entry.getDay() == null
                    || entry.getTime() == null) {

                continue;
            }

            if (entry.getRoom()
                    .equalsIgnoreCase(room)

                    && entry.getDay()
                    .equalsIgnoreCase(day)

                    && entry.getTime()
                    .equalsIgnoreCase(time)) {

                return true;
            }
        }

        return false;
    }
}