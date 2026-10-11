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
@RequestMapping("/api/faculty")
@CrossOrigin(origins = "http://localhost:5174")
public class FacultyController {

    private final FacultyRepository facultyRepository;

    public FacultyController(FacultyRepository facultyRepository) {
        this.facultyRepository = facultyRepository;
    }

    // Get all faculty
    @GetMapping
    public Iterable<Faculty> getAllFaculty() {
        return facultyRepository.findAll();
    }

    // Add faculty
    @PostMapping
    public Faculty addFaculty(@RequestBody Faculty faculty) {
        return facultyRepository.save(faculty);
    }

    // Get faculty by ID
    @GetMapping("/{id}")
    public Faculty getFacultyById(@PathVariable Long id) {
        return facultyRepository.findById(id).orElse(null);
    }

    // Update faculty
    @PutMapping("/{id}")
    public Faculty updateFaculty(
            @PathVariable Long id,
            @RequestBody Faculty faculty) {

        Faculty existingFaculty =
                facultyRepository.findById(id).orElse(null);

        if (existingFaculty == null) {
            return null;
        }

        existingFaculty.setName(faculty.getName());
        existingFaculty.setDepartment(faculty.getDepartment());
        existingFaculty.setEmail(faculty.getEmail());

        return facultyRepository.save(existingFaculty);
    }

    // Delete faculty
    @DeleteMapping("/{id}")
    public void deleteFaculty(@PathVariable Long id) {
        facultyRepository.deleteById(id);
    }
}