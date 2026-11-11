package com.plantsync.platform.intelligence;

import static org.springframework.http.HttpStatus.*;

import java.io.*;
import javax.imageio.ImageIO;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Component
public class ImageSanitizer {
  public byte[] sanitize(MultipartFile file) {
    if (file.isEmpty() || file.getSize() > 10L * 1024 * 1024)
      throw new ResponseStatusException(PAYLOAD_TOO_LARGE, "IMAGE_SIZE");
    try (var input = ImageIO.createImageInputStream(file.getInputStream())) {
      var readers = ImageIO.getImageReaders(input);
      if (!readers.hasNext())
        throw new ResponseStatusException(UNSUPPORTED_MEDIA_TYPE, "IMAGE_FORMAT");
      var reader = readers.next();
      try {
        reader.setInput(input, true, true);
        var format = reader.getFormatName().toLowerCase(java.util.Locale.ROOT);
        if (!java.util.Set.of("png", "jpeg", "jpg", "webp").contains(format))
          throw new ResponseStatusException(UNSUPPORTED_MEDIA_TYPE, "IMAGE_FORMAT");
        int width = reader.getWidth(0), height = reader.getHeight(0);
        if (width < 1 || height < 1 || (long) width * height > 12_000_000)
          throw new ResponseStatusException(PAYLOAD_TOO_LARGE, "IMAGE_DIMENSIONS");
        var image = reader.read(0);
        var output = new ByteArrayOutputStream();
        ImageIO.write(
            image, "png", output); // Re-encode pixels; discard EXIF/GPS and source metadata.
        if (output.size() > 20 * 1024 * 1024)
          throw new ResponseStatusException(PAYLOAD_TOO_LARGE, "IMAGE_SIZE");
        return output.toByteArray();
      } finally {
        reader.dispose();
      }
    } catch (ResponseStatusException e) {
      throw e;
    } catch (Exception e) {
      throw new ResponseStatusException(UNSUPPORTED_MEDIA_TYPE, "IMAGE_INVALID");
    }
  }
}
