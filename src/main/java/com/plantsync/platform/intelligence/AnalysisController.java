package com.plantsync.platform.intelligence;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/plant-analyses")
@RequiredArgsConstructor
public class AnalysisController {
  private final AnalysisService service;

  @PostMapping(consumes = "multipart/form-data")
  public ResponseEntity<?> submit(
      @RequestPart("image") MultipartFile image,
      @RequestParam(required = false) Long plantId,
      @RequestHeader("Idempotency-Key") String key) {
    var job = service.submit(image, plantId, key);
    return ResponseEntity.accepted().body(Map.of("id", job.getId(), "status", job.getStatus()));
  }

  @GetMapping("/{id}")
  public Object get(@PathVariable String id) {
    return service.view(service.own(id));
  }

  @GetMapping
  public Object list(
      @RequestParam(required = false) Long plantId,
      @RequestParam(defaultValue = "0") int cursor,
      @RequestParam(defaultValue = "20") int limit) {
    return service.list(plantId, cursor, limit);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<?> delete(@PathVariable String id) {
    service.delete(id);
    return ResponseEntity.noContent().build();
  }
}
