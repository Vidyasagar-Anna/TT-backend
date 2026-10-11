package com.timetable.backend;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/subjects")
@CrossOrigin(origins = {
        "http://localhost:5173",
        "http://localhost:5174"
})
public class SubjectController {

    private final SubjectRepository subjectRepository;

    public SubjectController(SubjectRepository subjectRepository) {
        this.subjectRepository = subjectRepository;
    }

    // =====================================================
    // GET ALL SUBJECTS
    // =====================================================

    @GetMapping
    public Iterable<Subject> getAllSubjects() {
        return subjectRepository.findAll();
    }

    // =====================================================
    // ADD SUBJECT
    // =====================================================

    @PostMapping
    public Subject addSubject(@RequestBody Subject subject) {
        return subjectRepository.save(subject);
    }

    // =====================================================
    // GET SUBJECT BY ID
    // =====================================================

    @GetMapping("/{id}")
    public Subject getSubjectById(@PathVariable Long id) {

        return subjectRepository.findById(id)
                .orElse(null);
    }

    // =====================================================
    // UPDATE SUBJECT
    // =====================================================

    @PutMapping("/{id}")
    public Subject updateSubject(
            @PathVariable Long id,
            @RequestBody Subject subject) {

        Subject existingSubject =
                subjectRepository.findById(id)
                        .orElse(null);

        if (existingSubject == null) {
            return null;
        }

        existingSubject.setName(subject.getName());
        existingSubject.setCode(subject.getCode());
        existingSubject.setDepartment(subject.getDepartment());
        existingSubject.setSemester(subject.getSemester());

        // IMPORTANT:
        // Save the faculty assigned to this subject
        existingSubject.setFaculty(subject.getFaculty());

        return subjectRepository.save(existingSubject);
    }

    // =====================================================
    // DELETE SUBJECT
    // =====================================================

    @DeleteMapping("/{id}")
    public void deleteSubject(@PathVariable Long id) {
        subjectRepository.deleteById(id);
    }
}