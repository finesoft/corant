package org.corant.modules.jaxrs.resteasy;

import static jakarta.ws.rs.core.HttpHeaders.CONTENT_DISPOSITION;
import static jakarta.ws.rs.core.HttpHeaders.CONTENT_LENGTH;
import static jakarta.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static jakarta.ws.rs.core.HttpHeaders.LAST_MODIFIED;
import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.ContentDispositions.parse;
import static org.corant.shared.util.Objects.defaultObject;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.corant.shared.resource.Resource;
import org.corant.shared.resource.SourceType;
import org.corant.shared.util.ContentDispositions.ContentDisposition;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;

/**
 * corant-modules-jaxrs-resteasy
 * <p>
 * resteasy InputPart resource
 *
 * @author don
 * @since 2019-09-26
 */
public class InputPartResource implements Resource {

  protected InputPart inputPart;

  protected String filename;

  protected Map<String, Object> metadata;

  protected ContentDisposition disposition;

  public InputPartResource(InputPart inputPart) {
    this(shouldNotNull(inputPart), parse(inputPart.getHeaders().getFirst(CONTENT_DISPOSITION)));
  }

  protected InputPartResource(InputPart inputPart, ContentDisposition disposition) {
    this.inputPart = inputPart;
    this.disposition = disposition;
    filename = defaultObject(disposition.getFilename(), () -> "unnamed-" + UUID.randomUUID());
    metadata = new HashMap<>();
    metadata.put(META_NAME, filename);
    if (inputPart.getMediaType() != null) {
      metadata.put(CONTENT_TYPE, inputPart.getMediaType().toString());
    }
    if (disposition.getModificationDate() != null) {
      metadata.put(LAST_MODIFIED, disposition.getModificationDate().toInstant().toEpochMilli());
    }
    if (disposition.getSize() != null) {
      metadata.put(CONTENT_LENGTH, disposition.getSize());
    }
  }

  public String getContentType() {
    return inputPart.getMediaType().toString();
  }

  public ContentDisposition getDisposition() {
    return disposition;
  }

  public String getFilename() {
    return filename;
  }

  public InputPart getInputPart() {
    return inputPart;
  }

  @Override
  public String getLocation() {
    return getName();
  }

  @Override
  public Map<String, Object> getMetadata() {
    return metadata;
  }

  @Override
  public String getName() {
    return filename;
  }

  @Override
  public SourceType getSourceType() {
    return null;
  }

  @Override
  public InputStream openInputStream() throws IOException {
    return inputPart.getBody(InputStream.class, null);
  }

  @Override
  public <T> T unwrap(Class<T> cls) {
    if (InputPartResource.class.isAssignableFrom(cls)) {
      return cls.cast(this);
    }
    return Resource.super.unwrap(cls);
  }
}
