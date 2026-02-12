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

import static org.springframework.http.HttpHeaders.ACCEPT_RANGES;
import static org.springframework.http.HttpHeaders.CONTENT_DISPOSITION;
import static org.springframework.http.HttpHeaders.CONTENT_RANGE;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpHeaders.ETAG;
import static org.springframework.http.HttpHeaders.IF_RANGE;
import static org.springframework.http.HttpHeaders.LAST_MODIFIED;
import static org.springframework.http.HttpHeaders.RANGE;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.eclipse.hawkbit.artifact.model.ArtifactStream;
import org.eclipse.hawkbit.rest.exception.FileStreamingFailedException;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Utility class for artifact file streaming supporting RFC-7233 range requests.
 * <p/>
 *
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc7233">RFC-7233</a>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
public final class FileStreamingUtil {

    private static final int BUFFER_SIZE = 0x2000; // 8k

    /**
     * <p>
     * Write response with target relation and publishes events concerning the download progress based on given update action status.
     * </p>
     *
     * @param artifact the artifact
     * @param filename to be written to the client response
     * @param lastModified unix timestamp of the artifact
     * @param request from the client
     * @param response to be sent back to the requesting client
     * @param progressListener to write progress updates to
     * @return response entity containing the input stream of the artifact file (or ranges requested)
     * @throws FileStreamingFailedException if streaming fails
     */
    @SuppressWarnings("java:S3776") // not so complex - linear logic at one place
    public static ResponseEntity<InputStream> writeFileResponse(
            final ArtifactStream artifact, final String filename, final long lastModified,
            final HttpServletRequest request, final HttpServletResponse response,
            final FileStreamingProgressListener progressListener) {
        resetResponseExceptHeaders(response);

        response.setHeader(CONTENT_DISPOSITION, "attachment;filename=" + encodeFilename(filename));
        if (lastModified > 0) {
            response.setDateHeader(LAST_MODIFIED, lastModified);
        }
        final String etag = '"' + artifact.getSha1Hash() + '"'; // ETag header value should be quoted as per RFC-7232, section 2.3.
        response.setHeader(ETAG, etag);
        response.setHeader(ACCEPT_RANGES, "bytes"); // range-unit -> bytes
        response.setContentType(APPLICATION_OCTET_STREAM_VALUE);
        // set the x-content-type options header to prevent browsers from doing MIME-sniffing when downloading an artifact,
        // as this could cause a security vulnerability
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setBufferSize(BUFFER_SIZE);

        final long length = artifact.getSize();
        // Validate and process Range (only, by spec, for GET methods) and If-Range headers.
        final String rangeHeader = "GET".equalsIgnoreCase(request.getMethod()) ? request.getHeader(RANGE) : null;
        final List<Range> ranges;
        if (rangeHeader != null) {
            log.debug("range header for filename ({}) is: {}", filename, rangeHeader);

            if (checkIfRangeSendAll(request, etag, lastModified)) {
                ranges = List.of();
            } else {
                // it seems there are valid ranges
                try {
                    ranges = Range.of(rangeHeader, length);
                } catch (final IllegalArgumentException e) {
                    log.debug("range header ({}) for filename ({}) is not satisfiable: {}", rangeHeader, filename, e.getMessage());
                    response.setHeader(CONTENT_RANGE, "bytes */" + length);
                    return new ResponseEntity<>(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE);
                }
            }
        } else {
            ranges = List.of();
        }

        try (final InputStream inputStream = artifact) {
            // full request - no range
            if (ranges.isEmpty()) {
                log.debug("filename ({}) results into a full request: ", filename);
                return handleFullFileRequest(inputStream, filename, length, response, progressListener);
            } else if (ranges.size() == 1) { // standard range request
                log.debug("filename ({}) results into a single range request: ", filename);
                return handleSingleRangeRequest(inputStream, ranges.get(0), filename, response, progressListener);
            } else { // multipart range request
                log.debug("filename ({}) results into a multipart range request: ", filename);
                return handleMultipartRangeRequest(inputStream, ranges, filename, response, progressListener);
            }
        } catch (final IOException e) {
            log.error("streaming of file ({}) failed!", filename, e);
            throw new FileStreamingFailedException(filename, e);
        }
    }

    private static void resetResponseExceptHeaders(final HttpServletResponse response) {
        // do backup the current headers (like CORS related)
        final Map<String, String> storedHeaders = new HashMap<>();
        for (final String header : response.getHeaderNames()) {
            storedHeaders.put(header, response.getHeader(header));
        }
        // resetting the response is needed only partially. Headers set before e.b. by the CORS security config needs to be persisted.
        response.reset();
        // restore headers again
        storedHeaders.forEach(response::addHeader);
    }

    // RFC: "if the representation is unchanged, send me the part(s) that I am requesting in Range; otherwise, send me the entire representation."
    private static boolean checkIfRangeSendAll(final HttpServletRequest request, final String etag, final long lastModified) {
        final String ifRange = request.getHeader(IF_RANGE);
        if (ifRange != null && !ifRange.equals(etag)) {
            try {
                final long ifRangeTime = request.getDateHeader(IF_RANGE);
                if (ifRangeTime != -1 && ifRangeTime < lastModified) {
                    return true;
                }
            } catch (final IllegalArgumentException e) {
                log.info("Invalid if-range header field", e);
                return true;
            }
        }
        return false;
    }

    private static ResponseEntity<InputStream> handleFullFileRequest(
            final InputStream inputStream, final String filename, final long length, final HttpServletResponse response,
            final FileStreamingProgressListener progressListener) {
        response.setContentLengthLong(length);

        try {
            final ServletOutputStream to = response.getOutputStream();
            copyStreams(inputStream, 0, length, filename, to, progressListener);
        } catch (final IOException e) {
            throw new FileStreamingFailedException("fullFileRequest " + filename, e);
        }

        return ResponseEntity.ok().build();
    }

    private static ResponseEntity<InputStream> handleSingleRangeRequest(
            final InputStream inputStream, final Range range, final String filename, final HttpServletResponse response,
            final FileStreamingProgressListener progressListener) {
        response.setHeader(CONTENT_RANGE, range.contentRange());
        response.setContentLengthLong(range.getPartLen());
        response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);

        try {
            copyStreams(inputStream, range.getStart(), range.getPartLen(), filename, response.getOutputStream(), progressListener);
        } catch (final IOException e) {
            log.error("standardRangeRequest of file ({}) failed!", filename, e);
            throw new FileStreamingFailedException(filename);
        }

        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT).build();
    }

    private static final String CRLF = "\r\n";
    private static final String DASH_DASH = "--";
    private static final String BOUNDARY = "THIS_STRING_SEPARATES_MULTIPART"; // boundary := 0*69<bchars> bcharsnospace
    private static final String CONTENT_TYPE_MULTIPART_BYTE_RANGES_AND_BOUNDARY = "multipart/byteranges; boundary=" + BOUNDARY;
    private static final String DASH_BOUNDARY = DASH_DASH + BOUNDARY; // dash-boundary := "--" boundary
    private static final String DELIMITER = CRLF + DASH_BOUNDARY; // delimiter := CRLF dash-boundary
    private static final String CLOSE_DELIMITER = DELIMITER + DASH_DASH; // close-delimiter := delimiter "--"

    // follows the RFC-2046 -> https://datatracker.ietf.org/doc/html/rfc2046#section-5.1
    private static ResponseEntity<InputStream> handleMultipartRangeRequest(
            final InputStream inputStream, final List<Range> ranges, final String filename, final HttpServletResponse response,
            final FileStreamingProgressListener progressListener) {
        // add headers
        response.setContentType(CONTENT_TYPE_MULTIPART_BYTE_RANGES_AND_BOUNDARY);
        response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);

        // write multipart-body as defined in RFC-2046 (we use transport-padding is empty as per RFC-2046, don't send the optional preamble and epilogue):
        //   multipart-body :=
        //     dash-boundary CRLF body-part // first body-part (range, headers + content)
        //     *encapsulation // next ranges
        //     close-delimiter
        //   encapsulation := delimiter CRLF body-part
        //   body-part := MIME-part-headers [CRLF *OCTET]
        try {
            // println of ServletOutputStream appends CRLF, which is required for separating multipart boundaries and header fields.
            final ServletOutputStream to = response.getOutputStream();
            long streamPos = 0;
            for (int i = 0; i < ranges.size(); i++) { // dash-boundary CRLF body-part or encapsulation (delimiter CRLF body-part)
                final Range range = ranges.get(i);
                // write body-part prefix - first body-part is prefixed with dash-boundary, the following ones with delimiter
                to.println(i == 0 ? DASH_BOUNDARY : DELIMITER);
                // write body-part
                // * write MIME-part-headers
                // If the selected representation would have had a Content-Type header field in a 200 (OK) response,
                // the server SHOULD generate that same Content-Type field in the header area of each body part.
                to.print(CONTENT_TYPE);
                to.print(": ");
                to.println(APPLICATION_OCTET_STREAM_VALUE);
                to.print(CONTENT_RANGE);
                to.print(": ");
                to.println(range.contentRange());
                // * write [CRLF *OCTET]
                to.println();
                copyStreams(inputStream, range.getStart() - streamPos, range.getPartLen(), filename, to, progressListener);
                // update stream position
                streamPos = range.getStart() + range.getPartLen();
            }
            // write close-delimiter
            to.print(CLOSE_DELIMITER);
        } catch (final IOException e) {
            throw new FileStreamingFailedException("multipartRangeRequest " + filename, e);
        }

        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT).build();
    }

    private static void copyStreams(
            final InputStream from, final long start, final long length, final String filename,
            final OutputStream to, final FileStreamingProgressListener progressListener) throws IOException {
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
    }

    // tspecials :=  "(" / ")" / "<" / ">" / "@" / "," / ";" / ":" / "\" / <"> / "/" / "[" / "]" / "?" / "="
    private static final String TSPECIALS = "()<>@,;:\\\"/[]?=";

    // Encodes filename for Content-Disposition header according to RFC-2183 (https://datatracker.ietf.org/doc/html/rfc2183)
    private static String encodeFilename(String filename) {
        if (filename.length() > 78) {
            filename = filename.substring(0, 78); // RFC-2183: parameter values longer than 78 characters should be truncated to 78 characters
        }

        // Check if filename contains any tspecials, and only if so, quotes
        for (int i = 0; i < filename.length(); i++) {
            if (TSPECIALS.indexOf(filename.charAt(i)) != -1) {
                return quotedString(filename);
            }
        }
        return filename;
    }

    private static @NonNull String quotedString(final String filename) {
        // RFC-2183: A short parameter value containing only ASCII characters, but including `tspecials' characters, SHOULD be represented as `quoted-string'
        // RFC-822:  quoted-string = <"> *(qtext/quoted-pair) <">, quoted-pair = "\" CHAR - we need to escape " and \ inside the quoted string
        final StringBuilder quoted = new StringBuilder("\"");
        for (int i = 0; i < filename.length(); i++) {
            final char c = filename.charAt(i);
            // Escape backslash and quote characters
            if (c == '"' || c == '\\') {
                quoted.append('\\');
            }
            quoted.append(c);
        }
        quoted.append('"');
        return quoted.toString();
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

    @Value
    private static class Range {

        // byte-content-range  = bytes-unit SP ( byte-range-resp / unsatisfied-range )
        // byte-range-resp     = byte-range "/" ( complete-length / "*" )
        // byte-range          = first-byte-pos "-" last-byte-pos
        private static final String CONTENT_RANGE_FORMAT = "bytes %d-%d/%d";

        long start; // first-byte-pos
        long end; // last-byte-pos
        long completeLen; // complete-length
        long partLen; // partial, range length (end - start + 1)

        private Range(final long start, final long end, final long completeLen) {
            this.start = start;
            this.end = end;
            this.completeLen = completeLen;
            partLen = end - start + 1;
        }

        // throws IllegalArgumentException if the header doesn't conform the expected format and constraints (like non-overlapping)
        // return validated, ordered and non-overlapping ranges
        private static List<Range> of(final String rangeHeader, final long length) {
            // Range header matches"bytes=n-n,n-n,n-n..."
            if (!rangeHeader.matches("^bytes=\\d*-\\d*(,\\d*-\\d*)*+$")) {
                throw new IllegalArgumentException("Doesn't match pattern");
            }

            final List<Range> ranges = new ArrayList<>();
            // parse range header
            for (final String part : rangeHeader.substring(6).split(",")) {
                final int index = part.indexOf('-');
                final long start;
                final long end;
                if (index == 0) { // -n, means last n bytes
                    start = length - Long.parseLong(part.substring(index + 1));
                    end = length - 1;
                } else {
                    start = Long.parseLong(part.substring(0, index));
                    end = index == part.length() - 1 ? length - 1 : Math.min(Long.parseLong(part.substring(index + 1)), length - 1);
                }
                // Check if Range is syntactically valid. If not, then return 416.
                if (start > end) {
                    throw new IllegalArgumentException("Start bigger then end");
                }
                // Add range.
                ranges.add(new Range(start, end, length));
            }
            // ranges must not be empty
            if (ranges.isEmpty()) {
                throw new IllegalArgumentException("Empty range list");
            }
            // order ranges by start position
            ranges.sort(Comparator.comparingLong(Range::getStart));
            // validate ranges, we don't allow overlapping, as this would make the streaming logic more complex and computational expensive.
            long streamPos = 0;
            for (final Range range : ranges) {
                if (streamPos > range.getStart()) {
                    throw new IllegalArgumentException("Ranges are overlapping or not in order");
                }
                streamPos = range.getStart() + range.getPartLen();
            }
            return ranges;
        }

        private String contentRange() {
            return String.format(CONTENT_RANGE_FORMAT, start, end, completeLen);
        }
    }
}