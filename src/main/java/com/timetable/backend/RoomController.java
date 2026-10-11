package com.timetable.backend;

import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/rooms")
@CrossOrigin(origins = "http://localhost:5174")
public class RoomController {

    private final RoomRepository roomRepository;

    public RoomController(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    // GET all rooms
    @GetMapping
    public List<Room> getAllRooms() {
        return roomRepository.findAll();
    }

    // GET room by ID
    @GetMapping("/{id}")
    public Room getRoomById(@PathVariable Long id) {
        return roomRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Room not found"));
    }

    // ADD room
    @PostMapping
    public Room addRoom(@RequestBody Room room) {
        return roomRepository.save(room);
    }

    // UPDATE room
    @PutMapping("/{id}")
    public Room updateRoom(
            @PathVariable Long id,
            @RequestBody Room room) {

        Room existingRoom = roomRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Room not found"));

        existingRoom.setRoomNumber(room.getRoomNumber());
        existingRoom.setCapacity(room.getCapacity());
        existingRoom.setDepartment(room.getDepartment());
        existingRoom.setRoomType(room.getRoomType());

        return roomRepository.save(existingRoom);
    }

    // DELETE room
    @DeleteMapping("/{id}")
    public void deleteRoom(@PathVariable Long id) {
        roomRepository.deleteById(id);
    }
}