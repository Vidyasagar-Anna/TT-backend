package com.timetable.backend;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sections")
@CrossOrigin(origins = "http://localhost:5173")
public class SectionController {

    private final SectionRepository sectionRepository;

    public SectionController(SectionRepository sectionRepository) {
        this.sectionRepository = sectionRepository;
    }

    @GetMapping
    public List<Section> getAllSections() {
        return sectionRepository.findAll();
    }

    @PostMapping
    public Section addSection(@RequestBody Section section) {
        return sectionRepository.save(section);
    }

    @PutMapping("/{id}")
    public Section updateSection(
            @PathVariable Long id,
            @RequestBody Section section) {

        Section existing = sectionRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Section not found"));

        existing.setName(section.getName());
        existing.setDepartment(section.getDepartment());
        existing.setYear(section.getYear());
        existing.setSemester(section.getSemester());
        existing.setClassRoom(section.getClassRoom());

        return sectionRepository.save(existing);
    }

    @DeleteMapping("/{id}")
    public void deleteSection(@PathVariable Long id) {
        sectionRepository.deleteById(id);
    }
}