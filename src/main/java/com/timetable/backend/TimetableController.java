package com.timetable.backend;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/timetables")
@CrossOrigin(origins = "http://localhost:5173")
public class TimetableController {

    private final TimetableRepository timetableRepository;
    private final TimetableGenerator timetableGenerator;

    public TimetableController(
            TimetableRepository timetableRepository,
            TimetableGenerator timetableGenerator) {

        this.timetableRepository = timetableRepository;
        this.timetableGenerator = timetableGenerator;
    }

    // =====================================================
    // GET ALL TIMETABLE RECORDS
    // =====================================================

    @GetMapping
    public List<Timetable> getAllTimetables() {
        return timetableRepository.findAll();
    }

    // =====================================================
    // GET TIMETABLE BY ID
    // =====================================================

    @GetMapping("/{id}")
    public Timetable getTimetableById(@PathVariable Long id) {
        return timetableRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Timetable not found with ID: " + id));
    }

    // =====================================================
    // ADD TIMETABLE MANUALLY
    // =====================================================

    @PostMapping
    public Timetable addTimetable(@RequestBody Timetable timetable) {
        return timetableRepository.save(timetable);
    }

    // =====================================================
    // UPDATE TIMETABLE
    // =====================================================

    @PutMapping("/{id}")
    public Timetable updateTimetable(
            @PathVariable Long id,
            @RequestBody Timetable updatedTimetable) {

        Timetable existing = timetableRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Timetable not found with ID: " + id));

        existing.setDay(updatedTimetable.getDay());
        existing.setTime(updatedTimetable.getTime());
        existing.setSection(updatedTimetable.getSection());
        existing.setSubject(updatedTimetable.getSubject());
        existing.setFaculty(updatedTimetable.getFaculty());
        existing.setRoom(updatedTimetable.getRoom());

        return timetableRepository.save(existing);
    }

    // =====================================================
    // DELETE TIMETABLE
    // =====================================================

    @DeleteMapping("/{id}")
    public String deleteTimetable(@PathVariable Long id) {

        if (!timetableRepository.existsById(id)) {
            throw new RuntimeException(
                    "Timetable not found with ID: " + id);
        }

        timetableRepository.deleteById(id);

        return "Timetable deleted successfully";
    }

    // =====================================================
    // DELETE ALL TIMETABLES
    // =====================================================

    @DeleteMapping
    public String deleteAllTimetables() {

        timetableRepository.deleteAll();

        return "All timetable records deleted successfully";
    }

    // =====================================================
    // GENERATE TIMETABLE
    // =====================================================
    //
    // IMPORTANT:
    // Frontend sends:
    // POST /api/timetables/generate
    //
    // =====================================================

    @PostMapping("/generate")
    public List<Timetable> generateTimetable() {

        // Delete old generated timetable
        timetableRepository.deleteAll();

        // Generate new timetable
        List<Timetable> generated =
                timetableGenerator.generateTimetable();

        // Save generated timetable
        return timetableRepository.saveAll(generated);
    }
}