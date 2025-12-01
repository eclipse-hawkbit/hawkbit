/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.rest.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.eclipse.hawkbit.artifact.model.ArtifactStream;
import org.eclipse.hawkbit.rest.exception.FileStreamingFailedException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/**
 * Utility class for artifact file streaming.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
public final class FileStreamingUtil {

    private static final int BUFFER_SIZE = 0x2000; // 8k

    /**
     * <p>
     * Write response with target relation and publishes events concerning the
     * download progress based on given update action status.
     * </p>
     * <p>
     * The request supports RFC7233 range requests.
     * </p>
     *
     * @param artifact the artifact
     * @param filename to be written to the client response
     * @param lastModified unix timestamp of the artifact
     * @param response to be sent back to the requesting client
     * @param request from the client
     * @param progressListener to write progress updates to
     * @return http response
     * @throws FileStreamingFailedException if streaming fails
     * @see <a href="https://tools.ietf.org/html/rfc7233">https://tools.ietf.org/html/rfc7233</a>
     */
    public static ResponseEntity<InputStream> writeFileResponse(
            final ArtifactStream artifact, final String filename,
            final long lastModified, final HttpServletResponse response, final HttpServletRequest request,
            final FileStreamingProgressListener progressListener) {
        ResponseEntity<InputStream> result;

        final String etag = artifact.getSha1Hash();
        final long length = artifact.getSize();

        resetResponseExceptHeaders(response);

        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + filename);
        response.setHeader(HttpHeaders.ETAG, etag);
        response.setHeader(HttpHeaders.ACCEPT_RANGES, "bytes");
        // set the x-content-type options header to prevent browsers from doing
        // MIME-sniffing when downloading an artifact, as this could cause a
        // security vulnerability
        response.setHeader("X-Content-Type-Options", "nosniff");
        if (lastModified > 0) {
            response.setDateHeader(HttpHeaders.LAST_MODIFIED, lastModified);
        }

        response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        response.setBufferSize(BUFFER_SIZE);

        final ByteRange full = new ByteRange(0, length - 1, length);
        final List<ByteRange> ranges = new ArrayList<>();

        // Validate and process Range and If-Range headers.
        final String range = request.getHeader("Range");
        if (lastModified > 0 && range != null) {
            log.debug("range header for filename ({}) is: {}", filename, range);

            // Range header matches"bytes=n-n,n-n,n-n..."
            if (!range.matches("^bytes=\\d*-\\d*(,\\d*-\\d*)*+$")) {
                response.setHeader(HttpHeaders.CONTENT_RANGE, "bytes */" + length);
                log.debug("range header for filename ({}) is not satisfiable: ", filename);
                return new ResponseEntity<>(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE);
            }

            // RFC: if the representation is unchanged, send me the part(s) that I am requesting in
            // Range; otherwise, send me the entire representation.
            checkForShortcut(request, etag, lastModified, full, ranges);

            // it seems there are valid ranges
            result = extractRange(response, length, ranges, range);
            // return if range extraction turned out to be invalid
            if (result != null) {
                return result;
            }
        }

        try (final InputStream inputStream = artifact) {
            // full request - no range
            if (ranges.isEmpty() || ranges.get(0).equals(full)) {
                log.debug("filename ({}) results into a full request: ", filename);
                result = handleFullFileRequest(inputStream, filename, response, progressListener, full);
            } else if (ranges.size() == 1) { // standard range request
                log.debug("filename ({}) results into a standard range request: ", filename);
                result = handleStandardRangeRequest(inputStream, filename, response, progressListener, ranges.get(0));
            } else { // multipart range request
                log.debug("filename ({}) results into a multipart range request: ", filename);
                result = handleMultipartRangeRequest(inputStream, filename, response, progressListener, ranges);
            }
        } catch (final IOException e) {
            log.error("streaming of file ({}) failed!", filename, e);
            throw new FileStreamingFailedException(filename, e);
        }

        return result;
    }

    private static void resetResponseExceptHeaders(final HttpServletResponse response) {
        // do backup the current headers (like CORS related)
        final Map<String, String> storedHeaders = new HashMap<>();
        for (final String header : response.getHeaderNames()) {
            storedHeaders.put(header, response.getHeader(header));
        }
        // resetting the response is needed only partially. Headers set before e.b. by
        // the CORS security config needs to be persisted.
        response.reset();
        // restore headers again
        storedHeaders.forEach(response::addHeader);
    }

    private static ResponseEntity<InputStream> handleFullFileRequest(
            final InputStream inputStream, final String filename, final HttpServletResponse response,
            final FileStreamingProgressListener progressListener, final ByteRange full) {
        response.setHeader(HttpHeaders.CONTENT_RANGE, "bytes " + full.getStart() + "-" + full.getEnd() + "/" + full.getTotal());
        response.setContentLengthLong(full.getLength());

        try {
            final ServletOutputStream to = response.getOutputStream();
            copyStreams(inputStream, to, progressListener, full.getStart(), full.getLength(), filename);
        } catch (final IOException e) {
            throw new FileStreamingFailedException("fullfileRequest " + filename, e);
        }

        return ResponseEntity.ok().build();
    }

    private static ResponseEntity<InputStream> extractRange(final HttpServletResponse response, final long length,
            final List<ByteRange> ranges, final String range) {

        if (ranges.isEmpty()) {
            for (final String part : range.substring(6).split(",")) {
                long start = sublong(part, 0, part.indexOf('-'));
                long end = sublong(part, part.indexOf('-') + 1, part.length());

                if (start == -1) {
                    start = length - end;
                    end = length - 1;
                } else if (end == -1 || end > length - 1) {
                    end = length - 1;
                }

                // Check if Range is syntactically valid. If not, then return
                // 416.
                if (start > end) {
                    response.setHeader(HttpHeaders.CONTENT_RANGE, "bytes */" + length);
                    return new ResponseEntity<>(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE);
                }

                // Add range.
                ranges.add(new ByteRange(start, end, length));
            }
        }

        return null;
    }

    private static long sublong(final String value, final int beginIndex, final int endIndex) {
        final String substring = value.substring(beginIndex, endIndex);
        return substring.isEmpty() ? -1 : Long.parseLong(substring);
    }

    private static void checkForShortcut(final HttpServletRequest request, final String etag, final long lastModified,
            final ByteRange full, final List<ByteRange> ranges) {
        final String ifRange = request.getHeader(HttpHeaders.IF_RANGE);
        if (ifRange != null && !ifRange.equals(etag)) {
            try {
                final long ifRangeTime = request.getDateHeader(HttpHeaders.IF_RANGE);
                if (ifRangeTime != -1 && ifRangeTime + 1000 < lastModified) {
                    ranges.add(full);
                }
            } catch (final IllegalArgumentException ignored) {
                log.info("Invalid if-range header field", ignored);
                ranges.add(full);
            }
        }
    }

    private static ResponseEntity<InputStream> handleMultipartRangeRequest(
            final InputStream inputStream, final String filename, final HttpServletResponse response,
            final FileStreamingProgressListener progressListener, final List<ByteRange> ranges) {
        ranges.sort((r1, r2) -> Long.compare(r1.getStart(), r2.getStart()));
        response.setContentType("multipart/byteranges; boundary=" + ByteRange.MULTIPART_BOUNDARY);
        response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);

        try {
            final ServletOutputStream to = response.getOutputStream();

            long streamPos = 0;
            for (final ByteRange range : ranges) {
                if (streamPos > range.getStart()) {
                    throw new FileStreamingFailedException("Ranges are overlapping or not in order");
                }

                // Add multipart boundary and header fields for every range.
                to.println();
                to.println("--" + ByteRange.MULTIPART_BOUNDARY);
                to.println(HttpHeaders.CONTENT_RANGE + ": bytes " + range.getStart() + "-" + range.getEnd() + "/" + range.getTotal());

                // Copy single part range of multipart range.
                copyStreams(inputStream, to, progressListener, range.getStart() - streamPos, range.getLength(), filename);
                streamPos = range.getStart() + range.getLength();
            }

            // End with final multipart boundary.
            to.println();
            to.print("--" + ByteRange.MULTIPART_BOUNDARY + "--");
        } catch (final IOException e) {
            throw new FileStreamingFailedException("multipartRangeRequest " + filename, e);
        }

        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT).build();
    }

    private static ResponseEntity<InputStream> handleStandardRangeRequest(
            final InputStream inputStream, final String filename, final HttpServletResponse response,
            final FileStreamingProgressListener progressListener, final ByteRange range) {
        response.setHeader(HttpHeaders.CONTENT_RANGE, "bytes " + range.getStart() + "-" + range.getEnd() + "/" + range.getTotal());
        response.setContentLengthLong(range.getLength());
        response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);

        try {
            final ServletOutputStream to = response.getOutputStream();
            copyStreams(inputStream, to, progressListener, range.getStart(), range.getLength(), filename);
        } catch (final IOException e) {
            log.error("standardRangeRequest of file ({}) failed!", filename, e);
            throw new FileStreamingFailedException(filename);
        }

        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT).build();
    }

    private static long copyStreams(
            final InputStream from, final OutputStream to,
            final FileStreamingProgressListener progressListener,
            final long start, final long length,
            final String filename) throws IOException {
        final long startMillis = System.currentTimeMillis();
        log.trace("Start of copy-streams of file {} from {} to {}", filename, start, length);

        Objects.requireNonNull(from);
        Objects.requireNonNull(to);
        final byte[] buf = new byte[BUFFER_SIZE];
        long total = 0;
        int progressPercent = 1;

        IOUtils.skipFully(from, start);

        long toRead = length;
        boolean toContinue = true;
        long shippedSinceLastEvent = 0;

        while (toContinue) {
            final int r = from.read(buf, 0, Math.min(BUFFER_SIZE, toRead > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) toRead));
            if (r == -1) {
                break;
            }

            toRead -= r;
            if (toRead > 0) {
                to.write(buf, 0, r);
                total += r;
                shippedSinceLastEvent += r;
            } else {
                to.write(buf, 0, (int) toRead + r);
                total += toRead + r;
                shippedSinceLastEvent += toRead + r;
                toContinue = false;
            }

            if (progressListener != null) {
                final int newPercent = (int) Math.floor(total * 100.0 / length);

                // every 10 percent an event
                if (newPercent == 100 || newPercent > progressPercent + 10) {
                    progressPercent = newPercent;
                    progressListener.progress(length, shippedSinceLastEvent, total);
                    shippedSinceLastEvent = 0;
                }
            }
        }

        final long totalTime = System.currentTimeMillis() - startMillis;

        if (total < length) {
            throw new FileStreamingFailedException(
                    filename + ": " + (length - total) + " bytes could not be written to client, total time on write: !" + totalTime + " ms");
        }

        log.trace("Finished copy-stream of file {} with length {} in {} ms", filename, length, totalTime);

        return total;
    }

    /**
     * Listener for progress on artifact file streaming.
     */
    @FunctionalInterface
    public interface FileStreamingProgressListener {

        /**
         * Called multiple times during streaming.
         *
         * @param requestedBytes requested bytes of the request
         * @param shippedBytesSinceLast since the last report
         * @param shippedBytesOverall during the request
         */
        void progress(long requestedBytes, long shippedBytesSinceLast, long shippedBytesOverall);
    }

    private static final class ByteRange {

        private static final String MULTIPART_BOUNDARY = "THIS_STRING_SEPARATES_MULTIPART";

        private final long start;
        private final long end;
        private final long length;
        private final long total;

        private ByteRange(final long start, final long end, final long total) {
            this.start = start;
            this.end = end;
            length = end - start + 1;
            this.total = total;
        }

        @Override
        // Generated code
        @SuppressWarnings("squid:S864")
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (int) (end ^ (end >>> 32));
            result = prime * result + (int) (length ^ (length >>> 32));
            result = prime * result + (int) (start ^ (start >>> 32));
            result = prime * result + (int) (total ^ (total >>> 32));
            return result;
        }

        @Override
        // Generated code
        @SuppressWarnings("squid:S1126")
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final ByteRange other = (ByteRange) obj;
            if (end != other.end) {
                return false;
            }
            if (length != other.length) {
                return false;
            }
            if (start != other.start) {
                return false;
            }
            if (total != other.total) {
                return false;
            }
            return true;
        }

        private long getStart() {
            return start;
        }

        private long getEnd() {
            return end;
        }

        private long getLength() {
            return length;
        }

        private long getTotal() {
            return total;
        }
    }
}