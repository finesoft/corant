/*
 * Copyright (c) 2013-2023, Bingo.Chen (finesoft@gmail.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.corant.modules.mail;

import static java.util.Collections.singletonMap;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;
import static org.corant.shared.ubiquity.Throwing.uncheckedConsumer;
import static org.corant.shared.ubiquity.Throwing.uncheckedFunction;
import static org.corant.shared.util.Assertions.shouldBeTrue;
import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.Strings.isBlank;
import static org.corant.shared.util.Strings.isNotBlank;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import jakarta.activation.DataHandler;
import jakarta.activation.DataSource;
import jakarta.mail.Address;
import jakarta.mail.Message.RecipientType;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.internet.MimeUtility;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.exception.NotSupportedException;
import org.corant.shared.resource.ByteArrayResource;
import org.corant.shared.resource.FileSystemResource;
import org.corant.shared.resource.Resource;
import org.corant.shared.resource.WritableResource;
import org.corant.shared.util.Validates;

/**
 * corant-modules-mail
 *
 * @author bingo 10:59:57
 */
public class Mail {

  public static final String CHARSET = StandardCharsets.UTF_8.displayName();

  protected Address from;
  protected final List<String> to = new ArrayList<>();
  protected final List<String> bcc = new ArrayList<>();
  protected final List<String> cc = new ArrayList<>();
  protected String bounce;
  protected final List<String> replyTo = new ArrayList<>();
  protected final Map<String, List<String>> headers = new HashMap<>();
  protected String subject;
  protected String text;
  protected String html;
  protected final List<Attachment> attachments = new ArrayList<>();

  public Mail() {}

  /**
   * Creates a new instance of {@link Mail} that contains a "html" body. The returned instance can
   * be modified.
   *
   * @param to the address of the recipient
   * @param subject the subject
   * @param html the body
   * @return the new {@link Mail} instance.
   */
  public static Mail ofHtml(String to, String subject, String html) {
    return new Mail().addTo(to).subject(subject).html(html);
  }

  /**
   * Creates a new instance of {@link Mail} that contains a "text" body. The returned instance can
   * be modified.
   *
   * @param to the address of the recipient
   * @param subject the subject
   * @param text the body
   * @return the new {@link Mail} instance.
   */
  public static Mail ofText(String to, String subject, String text) {
    return new Mail().addTo(to).subject(subject).text(text);
  }

  /**
   * Adds an attachment.
   *
   * @param name the name of the attachment, generally a file name.
   * @param data the binary data to be attached
   * @param contentType the content type.
   * @return the current {@link Mail}
   */
  public Mail addAttachment(String name, byte[] data, String contentType) {
    attachments.add(new Attachment(name, data, contentType));
    return this;
  }

  /**
   * Adds an attachment.
   *
   * @param name the name of the attachment, generally a file name.
   * @param data the binary data to be attached
   * @param contentType the content type
   * @param description the description of the attachment
   * @param inline whether inline
   * @return the current {@link Mail}
   */
  public Mail addAttachment(String name, byte[] data, String contentType, String description,
      boolean inline) {
    attachments
        .add(new Attachment(name, data, contentType).description(description).inline(inline));
    return this;
  }

  /**
   * Adds an attachment.
   *
   * @param name the name of the attachment, generally a file name.
   * @param file the file to be attached.
   * @param contentType the content type.
   * @return the current {@link Mail}
   */
  public Mail addAttachment(String name, File file, String contentType) {
    attachments.add(new Attachment(name, file, contentType));
    return this;
  }

  /**
   * Adds BCC recipients.
   *
   * @param bccs the recipients, each item must be a valid email address.
   * @return the current {@link Mail}
   */
  public Mail addBcc(String... bccs) {
    if (bccs != null) {
      for (String a : bccs) {
        if (!bcc.contains(a)) {
          bcc.add(checkMailAddress(a));
        }
      }
    }
    return this;
  }

  /**
   * Adds CC recipients.
   *
   * @param ccs the recipients, each item must be a valid email address.
   * @return the current {@link Mail}
   */
  public Mail addCc(String... ccs) {
    if (ccs != null) {
      for (String a : ccs) {
        if (!cc.contains(a)) {
          cc.add(checkMailAddress(a));
        }
      }
    }
    return this;
  }

  /**
   * Adds a header value.
   *
   * @param key the header name, must not be {@code null}
   * @param values the header values, must not be {@code null}
   * @return the current {@link Mail}
   */
  public Mail addHeader(String key, String... values) {
    if (key == null || values == null) {
      throw new IllegalArgumentException("Cannot add header, key and value must not be null");
    }
    Collections.addAll(headers.computeIfAbsent(key, k -> new ArrayList<>()), values);
    return this;
  }

  /**
   * Adds an inline attachment.
   *
   * @param name the name of the attachment, generally a file name.
   * @param data the binary data to be attached
   * @param contentType the content type
   * @param contentId the content id. It must follow the {@code <some-id@some-domain>} syntax. Then
   *        the HTML content can reference this attachment using
   *        {@code src="cid:some-id@some-domain"}.
   * @return the current {@link Mail}
   */
  public Mail addInlineAttachment(String name, byte[] data, String contentType, String contentId) {
    attachments.add(new Attachment(name, data, contentType).contentId(contentId).inline(true));
    return this;
  }

  /**
   * Adds an inline attachment.
   *
   * @param name the name of the attachment, generally a file name.
   * @param file the file to be attached. Note that the file will be read asynchronously.
   * @param contentType the content type
   * @param contentId the content id. It must follow the {@code <some-id@some-domain>} syntax. Then
   *        the HTML content can reference this attachment using
   *        {@code src="cid:some-id@some-domain"}.
   * @return the current {@link Mail}
   */
  public Mail addInlineAttachment(String name, File file, String contentType, String contentId) {
    attachments.add(new Attachment(name, file, contentType).contentId(contentId).inline(true));
    return this;
  }

  /**
   * Adds a reply-to address.
   *
   * @param replyTo the address to use as reply-to. Must be a valid email address.
   * @return the current {@link Mail}
   * @see #replyTo(String)
   */
  public Mail addReplyTo(String replyTo) {
    if (!this.replyTo.contains(replyTo)) {
      this.replyTo.add(checkMailAddress(replyTo));
    }
    return this;
  }

  /**
   * Adds TO recipients.
   *
   * @param to the recipients, each item must be a valid email address.
   * @return the current {@link Mail}
   */
  public Mail addTo(String... to) {
    if (to != null) {
      for (String a : to) {
        if (!this.to.contains(a)) {
          this.to.add(checkMailAddress(a));
        }
      }
    }
    return this;
  }

  /**
   * @return the list of attachments
   */
  public List<Attachment> attachments() {
    return unmodifiableList(attachments);
  }

  /**
   * Sets the attachment list.
   *
   * @param attachments the attachments.
   * @return the current {@link Mail}
   */
  public Mail attachments(Collection<Resource> attachments) {
    this.attachments.clear();
    if (isNotEmpty(attachments)) {
      attachments.stream().filter(Objects::nonNull).map(Attachment::new)
          .forEach(this.attachments::add);
    }
    return this;
  }

  /**
   * Sets the attachment list.
   *
   * @param attachments the attachments.
   * @return the current {@link Mail}
   */
  public Mail attachments(List<Attachment> attachments) {
    this.attachments.clear();
    if (isNotEmpty(attachments)) {
      attachments.stream().filter(Objects::nonNull).forEach(this.attachments::add);
    }
    return this;
  }

  /**
   * @return the BCC recipients.
   */
  public List<String> bcc() {
    return unmodifiableList(bcc);
  }

  /**
   * Sets the BCC recipients.
   *
   * @param bcc the list of recipients
   * @return the current {@link Mail}
   */
  public Mail bcc(Collection<String> bcc) {
    this.bcc.clear();
    if (isNotEmpty(bcc)) {
      bcc.stream().map(this::checkMailAddress).distinct().forEach(this.bcc::add);
    }
    return this;
  }

  /**
   * Sets the CC recipient.
   *
   * @param bcc the recipient
   * @return the current {@link Mail}
   */
  public Mail bcc(String bcc) {
    this.bcc.clear();
    this.bcc.add(checkMailAddress(bcc));
    return this;
  }

  /**
   * @return the bounce address.
   */
  public String bounce() {
    return bounce;
  }

  /**
   * Sets the bounce address. A default sender address can be configured in the application
   * properties
   *
   * @param bounce the bounce address, must be a valid email address.
   * @return the current {@link Mail}
   */
  public Mail bounce(String bounce) {
    this.bounce = checkMailAddress(bounce);
    return this;
  }

  /**
   * @return the CC recipients.
   */
  public List<String> cc() {
    return unmodifiableList(cc);
  }

  /**
   * Sets the CC recipients.
   *
   * @param cc the list of recipients
   * @return the current {@link Mail}
   */
  public Mail cc(Collection<String> cc) {
    this.cc.clear();
    if (isNotEmpty(cc)) {
      cc.stream().map(this::checkMailAddress).distinct().forEach(this.cc::add);
    }
    return this;
  }

  /**
   * Sets the CC recipient.
   *
   * @param cc the recipient
   * @return the current {@link Mail}
   */
  public Mail cc(String cc) {
    this.cc.clear();
    this.cc.add(checkMailAddress(cc));
    return this;
  }

  /**
   * @return the sender address.
   */
  public Address from() {
    return from;
  }

  /**
   * Sets the sender address. Notes that it's not accepted to send an email without a sender
   * address. A default sender address can be configured in the application properties (
   * {@code corant.mail.sender.from} )
   *
   * @param from the from address
   * @return the current {@link Mail}
   */
  public Mail from(Address from) {
    this.from = from;
    return this;
  }

  /**
   * Sets the sender address. Notes that it's not accepted to send an email without a sender
   * address. A default sender address can be configured in the application properties (
   * {@code corant.mail.sender.from} )
   *
   * @param from the sender address
   * @return the current {@link Mail}
   */
  public Mail from(String from) {
    try {
      this.from = new InternetAddress(from);
    } catch (AddressException e) {
      throw new CorantRuntimeException(e);
    }
    return this;
  }

  /**
   * Sets the sender address. Notes that it's not accepted to send an email without a sender
   * address. A default sender address can be configured in the application properties (
   * {@code corant.mail.sender.from} )
   *
   * @param address the sender address
   * @param personal the sender name
   * @return the current {@link Mail}
   */
  public Mail from(String address, String personal) {
    try {
      from = new InternetAddress(address, personal, CHARSET);
    } catch (UnsupportedEncodingException e) {
      throw new CorantRuntimeException(e);
    }
    return this;
  }

  /**
   * @return the current set of headers.
   */
  public Map<String, List<String>> headers() {
    return unmodifiableMap(headers);
  }

  /**
   * Sets the list of headers.
   *
   * @param headers the headers
   * @return the current {@link Mail}
   */
  public Mail headers(Map<String, List<String>> headers) {
    this.headers.clear();
    if (isNotEmpty(headers)) {
      headers.forEach((k, v) -> {
        if (k == null || v == null) {
          throw new IllegalArgumentException("Cannot add header, key and value must not be null");
        }
        this.headers.put(k, v);
      });
    }
    return this;
  }

  /**
   * @return the HTML content of the email
   */
  public String html() {
    return html;
  }

  /**
   * Sets the body of the email as HTML.
   *
   * @param html the content
   * @return the current {@link Mail}
   */
  public Mail html(String html) {
    this.html = html;
    return this;
  }

  /**
   * Removes all the attachment of this attachments list that satisfy the given predicate.
   *
   * @param predicate the predicate
   * @return the current {@link Mail}
   */
  public Mail removeAttachmentIf(Predicate<Attachment> predicate) {
    attachments.removeIf(predicate);
    return this;
  }

  /**
   * Removes all the BCC address of this BCC list that satisfy the given predicate.
   *
   * @param predicate the predicate
   * @return the current {@link Mail}
   */
  public Mail removeBccIf(Predicate<String> predicate) {
    bcc.removeIf(predicate);
    return this;
  }

  /**
   * Removes all the CC address of this CC list that satisfy the given predicate.
   *
   * @param predicate the predicate
   * @return the current {@link Mail}
   */
  public Mail removeCcIf(Predicate<String> predicate) {
    cc.removeIf(predicate);
    return this;
  }

  /**
   * Removes a header.
   *
   * @param key the header name, must not be {@code null}.
   * @return the current {@link Mail}
   */
  public Mail removeHeader(String key) {
    if (key != null) {
      headers.remove(key);
    }
    return this;
  }

  /**
   * Removes all the reply to address of this reply to list that satisfy the given predicate.
   *
   * @param predicate the predicate
   * @return the current {@link Mail}
   */
  public Mail removeReplyToIf(Predicate<String> predicate) {
    replyTo.removeIf(predicate);
    return this;
  }

  /**
   * Removes all the TO address of this TO list that satisfy the given predicate.
   *
   * @param predicate the predicate
   * @return the current {@link Mail}
   */
  public Mail removeToIf(Predicate<String> predicate) {
    to.removeIf(predicate);
    return this;
  }

  /**
   * @return the reply-to address. In the case of multiple addresses, the comma-separated list is
   *         returned, following the <a href=
   *         "https://datatracker.ietf.org/doc/html/rfc5322#section-3.6.2">rfc5322#section-3.6.2</a>
   *         recommendation. If no reply-to address has been set, it returns {@code null}.
   */
  public String replyTo() {
    if (isEmpty(replyTo)) {
      return null;
    }
    return String.join(",", replyTo);
  }

  /**
   * Sets the reply-to addresses.
   *
   * @param replyTo the addresses to use as reply-to. Must contain valid email addresses, must
   *        contain at least one address.
   * @return the current {@link Mail}
   */
  public Mail replyTo(Collection<String> replyTo) {
    this.replyTo.clear();
    if (isNotEmpty(replyTo)) {
      replyTo.stream().map(this::checkMailAddress).distinct().forEach(this.replyTo::add);
    }
    return this;
  }

  /**
   * Sets the reply-to address.
   *
   * @param replyTo the address to use as reply-to. Must be a valid email address.
   * @return the current {@link Mail}
   * @see #replyTo(String)
   */
  public Mail replyTo(String replyTo) {
    this.replyTo.clear();
    this.replyTo.add(checkMailAddress(replyTo));
    return this;
  }

  /**
   * @return the subject
   */
  public String subject() {
    return subject;
  }

  /**
   * Sets the email subject.
   *
   * @param subject the subject
   * @return the current {@link Mail}
   */
  public Mail subject(String subject) {
    this.subject = subject;
    return this;
  }

  /**
   * @return the text content of the email
   */
  public String text() {
    return text;
  }

  /**
   * Sets the body of the email as plain text.
   *
   * @param text the content
   * @return the current {@link Mail}
   */
  public Mail text(String text) {
    this.text = text;
    return this;
  }

  /**
   * @return the TO recipients.
   */
  public List<String> to() {
    return unmodifiableList(to);
  }

  /**
   * Sets the TO recipients.
   *
   * @param to the list of recipients
   * @return the current {@link Mail}
   */
  public Mail to(List<String> to) {
    this.to.clear();
    if (isNotEmpty(to)) {
      to.stream().map(this::checkMailAddress).distinct().forEach(this.to::add);
    }
    return this;
  }

  /**
   * Sets the TO recipient.
   *
   * @param to the recipient
   * @return the current {@link Mail}
   */
  public Mail to(String to) {
    this.to.clear();
    this.to.add(checkMailAddress(to));
    return this;
  }

  /**
   * Convert and returns the current {@link Mail} to {@link MimeMessage}
   *
   * @param session the session use for building
   * @return the mime message
   */
  public MimeMessage toMimeMessage(Session session) {
    try {
      MimeMessage mm = new MimeMessage(session);
      mm.setFrom(from);
      mm.setSubject(subject, CHARSET);
      if (isNotEmpty(headers)) {
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
          for (String v : entry.getValue()) {
            mm.addHeader(entry.getKey(), v);
          }
        }
      }
      // handle recipients
      if (isNotEmpty(to)) {
        mm.setRecipients(RecipientType.TO, to.stream().map(uncheckedFunction(InternetAddress::new))
            .toArray(InternetAddress[]::new));
      }
      if (isNotEmpty(cc)) {
        mm.setRecipients(RecipientType.CC, cc.stream().map(uncheckedFunction(InternetAddress::new))
            .toArray(InternetAddress[]::new));
      }
      if (isNotEmpty(bcc)) {
        mm.setRecipients(RecipientType.BCC, bcc.stream()
            .map(uncheckedFunction(InternetAddress::new)).toArray(InternetAddress[]::new));
      }
      if (isNotEmpty(replyTo)) {
        mm.setReplyTo(replyTo.stream().map(uncheckedFunction(InternetAddress::new))
            .toArray(InternetAddress[]::new));
      }
      if (isNotBlank(bounce)) {
        session.getProperties().put("mail.smtp.from", bounce);
      }

      MimeMultipart root = new MimeMultipart("mixed");

      // handle content
      MimeBodyPart contentBodyPart;
      if (isNotBlank(html)) {
        contentBodyPart = new MimeBodyPart();
        MimeMultipart alt = new MimeMultipart("alternative");

        MimeBodyPart htmlBodyPart = new MimeBodyPart();
        htmlBodyPart.setText(html, CHARSET, "html");
        alt.addBodyPart(htmlBodyPart);

        String altText = isBlank(text) ? html.replaceAll("<[^>]*>", "") : text;
        MimeBodyPart altBodyPart = new MimeBodyPart();
        altBodyPart.setHeader("Content-Transfer-Encoding", "base64");
        altBodyPart.setText(altText, CHARSET, "plain");
        alt.addBodyPart(altBodyPart);

        contentBodyPart.setContent(alt);
      } else {
        contentBodyPart = new MimeBodyPart();
        contentBodyPart.setText(text, CHARSET, "plain");
      }
      // handle in-line
      List<Attachment> inlines = attachments.stream().filter(Attachment::inline).toList();
      if (isNotEmpty(inlines)) {
        MimeBodyPart relatedBodyPart = new MimeBodyPart();
        MimeMultipart related = new MimeMultipart("related");
        related.addBodyPart(contentBodyPart);
        inlines.stream().map(Attachment::toMimeBodyPart)
            .forEach(uncheckedConsumer(related::addBodyPart));
        relatedBodyPart.setContent(related);
        root.addBodyPart(relatedBodyPart);
      } else {
        root.addBodyPart(contentBodyPart);
      }

      // handle attachments
      attachments.stream().filter(a -> !a.inline()).map(Attachment::toMimeBodyPart)
          .forEach(uncheckedConsumer(root::addBodyPart));

      mm.setContent(root);
      return mm;
    } catch (Exception e) {
      throw new CorantRuntimeException(e);
    }
  }

  protected String checkMailAddress(String address) {
    shouldBeTrue(Validates.isMailAddress(address), "[%s] is not a valid email address", address);
    return address;
  }

  /**
   * corant-modules-mail
   *
   * @author bingo 11:05:47
   */
  public static class Attachment {

    protected String name;
    protected Resource resource;
    protected String description;
    protected String contentType = "application/octet-stream";
    protected String contentId;
    protected boolean inline;

    public Attachment() {}

    public Attachment(byte[] bytes, String name) {
      this(new ByteArrayResource(bytes, null, name, null));
    }

    public Attachment(File file) {
      this(new FileSystemResource(file));
    }

    public Attachment(Resource resource) {
      this.resource = shouldNotNull(resource);
      String contentType = resource.getMetadataValue(Resource.META_CONTENT_TYPE, String.class);
      name(resource.getName());
      if (isNotBlank(contentType)) {
        contentType(contentType);
      }
    }

    public Attachment(String name, byte[] bytes, String contentType) {
      this(new ByteArrayResource(bytes, null, name,
          singletonMap(Resource.META_CONTENT_TYPE, contentType)));
    }

    public Attachment(String name, File file, String contentType) {
      this(new FileSystemResource(file, singletonMap(Resource.META_CONTENT_TYPE, String.class)));
      name(name).contentType(contentType);
    }

    public String contentId() {
      return contentId;
    }

    public Attachment contentId(String contentId) {
      if (contentId != null && (!contentId.startsWith("<") || !contentId.endsWith(">"))) {
        this.contentId = "<" + contentId + ">";
      } else {
        this.contentId = contentId;
      }
      return this;
    }

    public String contentType() {
      return contentType;
    }

    public Attachment contentType(String contentType) {
      this.contentType = contentType;
      return this;
    }

    public String description() {
      return description;
    }

    public Attachment description(String description) {
      this.description = description;
      return this;
    }

    public boolean inline() {
      return inline;
    }

    public Attachment inline(boolean inline) {
      this.inline = inline;
      return this;
    }

    public String name() {
      return name;
    }

    public Attachment name(String name) {
      this.name = name;
      return this;
    }

    public Resource resource() {
      return resource;
    }

    public Attachment resource(Resource resource) {
      this.resource = resource;
      return this;
    }

    public MimeBodyPart toMimeBodyPart() {
      try {
        MimeBodyPart part = new MimeBodyPart();
        part.setHeader("Content-Type", contentType);
        part.setDataHandler(new DataHandler(new DataSource() {

          @Override
          public String getContentType() {
            return contentType;
          }

          @Override
          public InputStream getInputStream() throws IOException {
            return resource.openInputStream();
          }

          @Override
          public String getName() {
            return name;
          }

          @Override
          public OutputStream getOutputStream() throws IOException {
            if (resource instanceof WritableResource wr) {
              return wr.openOutputStream();
            }
            throw new NotSupportedException();
          }
        }));
        if (name != null) {
          part.setFileName(MimeUtility.encodeText(name));
        }
        if (contentId != null) {
          part.setContentID(contentId);
        }
        if (description != null) {
          part.setDescription(description, CHARSET);
        }
        part.setDisposition(inline ? Part.INLINE : Part.ATTACHMENT);
        return part;
      } catch (Exception e) {
        throw new CorantRuntimeException(e);
      }
    }
  }
}
