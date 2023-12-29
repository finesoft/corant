/*
 * Copyright (c) 2013-2021, Bingo.Chen (finesoft@gmail.com).
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
package org.corant.shared.resource;

import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Objects.max;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.function.Supplier;
import org.corant.shared.util.FileUtils;
import org.corant.shared.util.Streams;

/**
 * corant-shared
 * <p>
 * An output stream which triggers an event when a specified number of bytes of data have been
 * written to it. The event can be used, for example, to throw an exception if a maximum has been
 * reached, or to switch the underlying stream type when the threshold is exceeded.
 * <p>
 * This class overrides all <code>OutputStream</code> methods. However, these overrides ultimately
 * call the corresponding methods in the underlying output stream implementation.
 * <p>
 * NOTE: This implementation may trigger the event <em>before</em> the threshold is actually
 * reached, since it triggers when a pending write operation would cause the threshold to be
 * exceeded.
 *
 * <p>
 * <b> NOTE: ALL CODE IN THIS CLASS COPY FROM APACHE COMMON IO, IF THERE IS INFRINGEMENT, PLEASE
 * INFORM ME(finesoft@gmail.com). </b>
 *
 * @author bingo 下午5:24:41
 */
public abstract class ThresholdingOutputStream extends OutputStream {

  // ----------------------------------------------------------- Data members

  /**
   * The threshold at which the event will be triggered.
   */
  private final int threshold;

  /**
   * The number of bytes written to the output stream.
   */
  private long written;

  /**
   * Whether or not the configured threshold has been exceeded.
   */
  private boolean thresholdExceeded;

  // ----------------------------------------------------------- Constructors

  /**
   * Constructs an instance of this class which will trigger an event at the specified threshold.
   *
   * @param threshold The number of bytes at which to trigger an event.
   */
  public ThresholdingOutputStream(final int threshold) {
    this.threshold = threshold;
  }

  // --------------------------------------------------- OutputStream methods

  /**
   * Closes this output stream and releases any system resources associated with this stream.
   *
   * @throws IOException if an error occurs.
   */
  @Override
  public void close() throws IOException {
    try {
      flush();
    } catch (final IOException ignored) {
      // ignore
    }
    getStream().close();
  }

  /**
   * Flushes this output stream and forces any buffered output bytes to be written out.
   *
   * @throws IOException if an error occurs.
   */
  @Override
  public void flush() throws IOException {
    getStream().flush();
  }

  /**
   * Returns the number of bytes that have been written to this output stream.
   *
   * @return The number of bytes written.
   */
  public long getByteCount() {
    return written;
  }

  /**
   * Returns the threshold, in bytes, at which an event will be triggered.
   *
   * @return The threshold point, in bytes.
   */
  public int getThreshold() {
    return threshold;
  }

  /**
   * Determines whether or not the configured threshold has been exceeded for this output stream.
   *
   * @return {@code true} if the threshold has been reached; {@code false} otherwise.
   */
  public boolean isThresholdExceeded() {
    return written > threshold;
  }

  // --------------------------------------------------------- Public methods

  /**
   * Writes <code>b.length</code> bytes from the specified byte array to this output stream.
   *
   * @param b The array of bytes to be written.
   *
   * @throws IOException if an error occurs.
   */
  @Override
  public void write(final byte b[]) throws IOException {
    checkThreshold(b.length);
    getStream().write(b);
    written += b.length;
  }

  /**
   * Writes <code>len</code> bytes from the specified byte array starting at offset <code>off</code>
   * to this output stream.
   *
   * @param b The byte array from which the data will be written.
   * @param off The start offset in the byte array.
   * @param len The number of bytes to write.
   *
   * @throws IOException if an error occurs.
   */
  @Override
  public void write(final byte b[], final int off, final int len) throws IOException {
    checkThreshold(len);
    getStream().write(b, off, len);
    written += len;
  }

  /**
   * Writes the specified byte to this output stream.
   *
   * @param b The byte to be written.
   *
   * @throws IOException if an error occurs.
   */
  @Override
  public void write(final int b) throws IOException {
    checkThreshold(1);
    getStream().write(b);
    written++;
  }

  // ------------------------------------------------------ Protected methods

  /**
   * Checks to see if writing the specified number of bytes would cause the configured threshold to
   * be exceeded. If so, triggers an event to allow a concrete implementation to take action on
   * this.
   *
   * @param count The number of bytes about to be written to the underlying output stream.
   *
   * @throws IOException if an error occurs.
   */
  protected void checkThreshold(final int count) throws IOException {
    if (!thresholdExceeded && written + count > threshold) {
      thresholdExceeded = true;
      thresholdReached();
    }
  }

  /**
   * Returns the underlying output stream, to which the corresponding <code>OutputStream</code>
   * methods in this class will ultimately delegate.
   *
   * @return The underlying output stream.
   *
   * @throws IOException if an error occurs.
   */
  protected abstract OutputStream getStream() throws IOException;

  /**
   * Resets the byteCount to zero. You can call this from {@link #thresholdReached()} if you want
   * the event to be triggered again.
   */
  protected void resetByteCount() {
    thresholdExceeded = false;
    written = 0;
  }

  // ------------------------------------------------------- Abstract methods

  /**
   * Sets the byteCount to count. Useful for re-opening an output stream that has previously been
   * written to.
   *
   * @param count The number of bytes that have already been written to the output stream
   *
   * @since 2.5
   */
  protected void setByteCount(final long count) {
    written = count;
  }

  /**
   * Indicates that the configured threshold has been reached, and that a subclass should take
   * whatever action necessary on this event. This may include changing the underlying output
   * stream.
   *
   * @throws IOException if an error occurs.
   */
  protected abstract void thresholdReached() throws IOException;

  /**
   * corant-shared
   *
   * @author bingo 下午10:55:50
   *
   */
  public static class DeferredFileOutputStream extends ThresholdingOutputStream {

    public static final int DEFAULT_SIZE = 1024;
    // ----------------------------------------------------------- Data members

    /**
     * The output stream to which data will be written prior to the threshold being reached.
     */
    private ByteArrayOutputStream memoryOutputStream;

    /**
     * The output stream to which data will be written at any given time. This will always be one of
     * <code>memoryOutputStream</code> or <code>diskOutputStream</code>.
     */
    private OutputStream currentOutputStream;

    /**
     * The file to which output will be directed if the threshold is exceeded.
     */
    private File outputFile;

    /**
     * The temporary file prefix.
     */
    private final String prefix;

    /**
     * The temporary file suffix.
     */
    private final String suffix;

    /**
     * The directory to use for temporary files.
     */
    private final File directory;

    /**
     * True when close() has been called successfully.
     */
    private boolean closed = false;

    // ----------------------------------------------------------- Constructors

    /**
     * Constructs an instance of this class which will trigger an event at the specified threshold,
     * and save data to a file beyond that point. The initial buffer size will default to 1024 bytes
     * which is ByteArrayOutputStream's default buffer size.
     *
     * @param threshold The number of bytes at which to trigger an event.
     * @param outputFile The file to which data is saved beyond the threshold.
     */
    public DeferredFileOutputStream(final int threshold, final File outputFile) {
      this(threshold, outputFile, null, null, null, DEFAULT_SIZE);
    }

    /**
     * Constructs an instance of this class which will trigger an event at the specified threshold,
     * and save data to a file beyond that point.
     *
     * @param threshold The number of bytes at which to trigger an event.
     * @param initialBufferSize The initial size of the in memory buffer.
     * @param outputFile The file to which data is saved beyond the threshold.
     *
     * @since 2.5
     */
    public DeferredFileOutputStream(final int threshold, final int initialBufferSize,
        final File outputFile) {
      this(threshold, outputFile, null, null, null, initialBufferSize);
      if (initialBufferSize < 0) {
        throw new IllegalArgumentException("Initial buffer size must be atleast 0.");
      }
    }

    /**
     * Constructs an instance of this class which will trigger an event at the specified threshold,
     * and save data to a temporary file beyond that point.
     *
     * @param threshold The number of bytes at which to trigger an event.
     * @param initialBufferSize The initial size of the in memory buffer.
     * @param prefix Prefix to use for the temporary file.
     * @param suffix Suffix to use for the temporary file.
     * @param directory Temporary file directory.
     *
     * @since 2.5
     */
    public DeferredFileOutputStream(final int threshold, final int initialBufferSize,
        final String prefix, final String suffix, final File directory) {
      this(threshold, null, prefix, suffix, directory, initialBufferSize);
      if (prefix == null) {
        throw new IllegalArgumentException("Temporary file prefix is missing");
      }
      if (initialBufferSize < 0) {
        throw new IllegalArgumentException("Initial buffer size must be atleast 0.");
      }
    }

    /**
     * Constructs an instance of this class which will trigger an event at the specified threshold,
     * and save data to a temporary file beyond that point. The initial buffer size will default to
     * 32 bytes which is ByteArrayOutputStream's default buffer size.
     *
     * @param threshold The number of bytes at which to trigger an event.
     * @param prefix Prefix to use for the temporary file.
     * @param suffix Suffix to use for the temporary file.
     * @param directory Temporary file directory.
     *
     * @since 1.4
     */
    public DeferredFileOutputStream(final int threshold, final String prefix, final String suffix,
        final File directory) {
      this(threshold, null, prefix, suffix, directory, DEFAULT_SIZE);
      if (prefix == null) {
        throw new IllegalArgumentException("Temporary file prefix is missing");
      }
    }

    /**
     * Constructs an instance of this class which will trigger an event at the specified threshold,
     * and save data either to a file beyond that point.
     *
     * @param threshold The number of bytes at which to trigger an event.
     * @param outputFile The file to which data is saved beyond the threshold.
     * @param prefix Prefix to use for the temporary file.
     * @param suffix Suffix to use for the temporary file.
     * @param directory Temporary file directory.
     * @param initialBufferSize The initial size of the in memory buffer.
     */
    private DeferredFileOutputStream(final int threshold, final File outputFile,
        final String prefix, final String suffix, final File directory,
        final int initialBufferSize) {
      super(threshold);
      this.outputFile = outputFile;
      this.prefix = prefix;
      this.suffix = suffix;
      this.directory = directory;

      memoryOutputStream = new ByteArrayOutputStream(initialBufferSize);
      currentOutputStream = memoryOutputStream;
    }

    // --------------------------------------- ThresholdingOutputStream methods

    /**
     * Closes underlying output stream, and mark this as closed
     *
     * @throws IOException if an error occurs.
     */
    @Override
    public void close() throws IOException {
      super.close();
      closed = true;
    }

    /**
     * Returns the data for this output stream as an array of bytes, assuming that the data has been
     * retained in memory. If the data was written to disk, this method returns {@code null}.
     *
     * @return The data for this output stream, or {@code null} if no such data is available.
     */
    public byte[] getData() {
      if (memoryOutputStream != null) {
        return memoryOutputStream.toByteArray();
      }
      return null;
    }

    // --------------------------------------------------------- Public methods

    /**
     * Returns either the output file specified in the constructor or the temporary file created or
     * null.
     * <p>
     * If the constructor specifying the file is used then it returns that same output file, even
     * when threshold has not been reached.
     * <p>
     * If constructor specifying a temporary file prefix/suffix is used then the temporary file
     * created once the threshold is reached is returned If the threshold was not reached then
     * {@code null} is returned.
     *
     * @return The file for this output stream, or {@code null} if no such file exists.
     */
    public File getFile() {
      return outputFile;
    }

    /**
     * Determines whether or not the data for this output stream has been retained in memory.
     *
     * @return {@code true} if the data is available in memory; {@code false} otherwise.
     */
    public boolean isInMemory() {
      return !isThresholdExceeded();
    }

    /**
     * Writes the data from this output stream to the specified output stream, after it has been
     * closed.
     *
     * @param out output stream to write to.
     * @throws IOException if this stream is not yet closed or an error occurs.
     */
    public void writeTo(final OutputStream out) throws IOException {
      // we may only need to check if this is closed if we are working with a file
      // but we should force the habit of closing wether we are working with
      // a file or memory.
      if (!closed) {
        throw new IOException("Stream not closed");
      }

      if (isInMemory()) {
        memoryOutputStream.writeTo(out);
      } else {
        try (FileInputStream fis = new FileInputStream(outputFile)) {
          Streams.copy(fis, out);
        }
      }
    }

    /**
     * Returns the current output stream. This may be memory based or disk based, depending on the
     * current state with respect to the threshold.
     *
     * @return The underlying output stream.
     *
     * @throws IOException if an error occurs.
     */
    @Override
    protected OutputStream getStream() throws IOException {
      return currentOutputStream;
    }

    /**
     * Switches the underlying output stream from a memory based stream to one that is backed by
     * disk. This is the point at which we realise that too much data is being written to keep in
     * memory, so we elect to switch to disk-based storage.
     *
     * @throws IOException if an error occurs.
     */
    @Override
    protected void thresholdReached() throws IOException {
      if (prefix != null) {
        outputFile = File.createTempFile(prefix, suffix, directory);
      }
      if (outputFile.getParentFile() != null) {
        FileUtils.forceMkdir(outputFile.getParentFile());
      }
      final FileOutputStream fos = new FileOutputStream(outputFile);
      try {
        memoryOutputStream.writeTo(fos);
      } catch (final IOException e) {
        fos.close();
        throw e;
      }
      currentOutputStream = fos;
      memoryOutputStream = null;
    }
  }

  /**
   * corant-shared
   *
   * @author bingo 上午10:14:24
   *
   */
  public static class SimpleDeferredFileOutputStream extends ThresholdingOutputStream {

    private ByteArrayOutputStream memoryOutputStream;
    private OutputStream currentOutputStream;
    private Supplier<File> outputFileSupplier;
    private File outputFile;
    private boolean closed = false;

    public SimpleDeferredFileOutputStream(int threshold, Supplier<File> outputFileSupplier) {
      super(max(threshold, 0));
      this.outputFileSupplier = shouldNotNull(outputFileSupplier);
      memoryOutputStream = new ByteArrayOutputStream(1024);
      currentOutputStream = memoryOutputStream;
    }

    /**
     * Closes underlying output stream, and mark this as closed
     *
     * @throws IOException if an error occurs.
     */
    @Override
    public void close() throws IOException {
      super.close();
      closed = true;
    }

    /**
     * Returns the data for this output stream as an array of bytes, assuming that the data has been
     * retained in memory. If the data was written to disk, this method returns {@code null}.
     *
     * @return The data for this output stream, or {@code null} if no such data is available.
     */
    public byte[] getData() {
      if (memoryOutputStream != null) {
        return memoryOutputStream.toByteArray();
      }
      return null;
    }

    // --------------------------------------------------------- Public methods

    /**
     * Returns either the output file specified in the constructor or the temporary file created or
     * null.
     * <p>
     * If the constructor specifying the file is used then it returns that same output file, even
     * when threshold has not been reached.
     * <p>
     * If constructor specifying a temporary file prefix/suffix is used then the temporary file
     * created once the threshold is reached is returned If the threshold was not reached then
     * {@code null} is returned.
     *
     * @return The file for this output stream, or {@code null} if no such file exists.
     */
    public File getFile() {
      return outputFile;
    }

    /**
     * Determines whether or not the data for this output stream has been retained in memory.
     *
     * @return {@code true} if the data is available in memory; {@code false} otherwise.
     */
    public boolean isInMemory() {
      return !isThresholdExceeded();
    }

    /**
     * Writes the data from this output stream to the specified output stream, after it has been
     * closed.
     *
     * @param out output stream to write to.
     * @throws IOException if this stream is not yet closed or an error occurs.
     */
    public void writeTo(final OutputStream out) throws IOException {
      // we may only need to check if this is closed if we are working with a file
      // but we should force the habit of closing wether we are working with
      // a file or memory.
      if (!closed) {
        throw new IOException("Stream not closed");
      }
      if (isInMemory()) {
        memoryOutputStream.writeTo(out);
      } else {
        try (FileInputStream fis = new FileInputStream(outputFile)) {
          Streams.copy(fis, out);
        }
      }
    }

    /**
     * Returns the current output stream. This may be memory based or disk based, depending on the
     * current state with respect to the threshold.
     *
     * @return The underlying output stream.
     *
     * @throws IOException if an error occurs.
     */
    @Override
    protected OutputStream getStream() throws IOException {
      return currentOutputStream;
    }

    /**
     * Switches the underlying output stream from a memory based stream to one that is backed by
     * disk. This is the point at which we realise that too much data is being written to keep in
     * memory, so we elect to switch to disk-based storage.
     *
     * @throws IOException if an error occurs.
     */
    @Override
    protected void thresholdReached() throws IOException {
      outputFile = outputFileSupplier.get();
      if (outputFile.getParentFile() != null) {
        FileUtils.forceMkdir(outputFile.getParentFile());
      }
      final FileOutputStream fos = new FileOutputStream(outputFile);
      try {
        memoryOutputStream.writeTo(fos);
      } catch (final IOException e) {
        fos.close();
        throw e;
      }
      currentOutputStream = fos;
      memoryOutputStream = null;
    }
  }
}
