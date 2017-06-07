/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.rest.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.hawkbit.artifact.repository.model.DbArtifact;
import org.eclipse.hawkbit.repository.ControllerManagement;
import org.eclipse.hawkbit.repository.model.ActionStatus;
import org.eclipse.hawkbit.repository.model.Artifact;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.google.common.base.Preconditions;
import com.google.common.math.DoubleMath;
import com.google.common.net.HttpHeaders;

/**
 * Utility class for the Rest Source API.
 */
public final class RestResourceConversionHelper {

    private static final Logger LOG = LoggerFactory.getLogger(RestResourceConversionHelper.class);

    private static final int BUFFER_SIZE = 4096;

    private RestResourceConversionHelper() {

    }

    /**
     * Write response without target relation.
     *
     * @param artifact
     *            the artifact
     * @param servletResponse
     *            to be sent back to the requesting client
     * @param request
     *            from the client
     * @param file
     *            to be write to the client response
     *
     * @return http code
     */
    public static ResponseEntity<InputStream> writeFileResponse(final Artifact artifact,
            final HttpServletResponse servletResponse, final HttpServletRequest request, final DbArtifact file) {
        return writeFileResponse(artifact, servletResponse, request, file, null, null);
    }

    /**
     * <p>
     * Write response with target relation and publishes events concerning the
     * download progress based on given update action status.
     * </p>
     *
     * <p>
     * The request supports RFC7233 range requests.
     * </p>
     *
     * @param artifact
     *            the artifact
     * @param response
     *            to be sent back to the requesting client
     * @param request
     *            from the client
     * @param file
     *            to be write to the client response
     * @param controllerManagement
     *            to write progress updates to
     * @param statusId
     *            of the {@link ActionStatus}
     *
     * @return http code
     *
     * @see <a href="https://tools.ietf.org/html/rfc7233">https://tools.ietf.org
     *      /html/rfc7233</a>
     */
    public static ResponseEntity<InputStream> writeFileResponse(final Artifact artifact,
            final HttpServletResponse response, final HttpServletRequest request, final DbArtifact file,
            final ControllerManagement controllerManagement, final Long statusId) {

        ResponseEntity<InputStream> result;

        final String etag = artifact.getSha1Hash();
        final Long lastModified = artifact.getLastModifiedAt() != null ? artifact.getLastModifiedAt()
                : artifact.getCreatedAt();
        final long length = file.getSize();

        response.reset();
        response.setBufferSize(BUFFER_SIZE);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + artifact.getFilename());
        response.setHeader(HttpHeaders.ETAG, etag);
        response.setHeader(HttpHeaders.ACCEPT_RANGES, "bytes");
        response.setDateHeader(HttpHeaders.LAST_MODIFIED, lastModified);
        response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);

        final ByteRange full = new ByteRange(0, length - 1, length);
        final List<ByteRange> ranges = new ArrayList<>();

        // Validate and process Range and If-Range headers.
        final String range = request.getHeader("Range");
        if (range != null) {
            LOG.debug("range header for filename ({}) is: {}", artifact.getFilename(), range);

            // Range header matches"bytes=n-n,n-n,n-n..."
            if (!range.matches("^bytes=\\d*-\\d*(,\\d*-\\d*)*$")) {
                response.setHeader(HttpHeaders.CONTENT_RANGE, "bytes */" + length);
                LOG.debug("range header for filename ({}) is not satisfiable: ", artifact.getFilename());
                return new ResponseEntity<>(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE);
            }

            // RFC: if the representation is unchanged, send me the part(s) that
            // I am requesting in
            // Range; otherwise, send me the entire representation.
            checkForShortcut(request, etag, lastModified, full, ranges);

            // it seems there are valid ranges
            result = extractRange(response, length, ranges, range);
            // return if range extraction turned out to be invalid
            if (result != null) {
                return result;
            }
        }

        // full request - no range
        if (ranges.isEmpty() || ranges.get(0).equals(full)) {
            LOG.debug("filename ({}) results into a full request: ", artifact.getFilename());
            handleFullFileRequest(artifact, response, file, controllerManagement, statusId, full);
            result = ResponseEntity.ok().build();
        }
        // standard range request
        else if (ranges.size() == 1) {
            LOG.debug("filename ({}) results into a standard range request: ", artifact.getFilename());
            handleStandardRangeRequest(artifact, response, file, controllerManagement, statusId, ranges);
            result = new ResponseEntity<>(HttpStatus.PARTIAL_CONTENT);
        }
        // multipart range request
        else {
            LOG.debug("filename ({}) results into a multipart range request: ", artifact.getFilename());
            handleMultipartRangeRequest(artifact, response, file, controllerManagement, statusId, ranges);
            result = new ResponseEntity<>(HttpStatus.PARTIAL_CONTENT);
        }

        return result;
    }

    private static void handleFullFileRequest(final Artifact artifact, final HttpServletResponse response,
            final DbArtifact file, final ControllerManagement controllerManagement, final Long statusId,
            final ByteRange full) {
        final ByteRange r = full;
        response.setHeader(HttpHeaders.CONTENT_RANGE, "bytes " + r.getStart() + "-" + r.getEnd() + "/" + r.getTotal());
        response.setHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(r.getLength()));

        try (InputStream inputStream = file.getFileInputStream()) {
            copyStreams(inputStream, response.getOutputStream(), controllerManagement, statusId, r.getStart(),
                    r.getLength());
        } catch (final IOException e) {
            LOG.error("fullfileRequest of file ({}) failed!", artifact.getFilename(), e);
            throw new FileSteamingFailedException(artifact.getFilename());
        }
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
        return substring.length() > 0 ? Long.parseLong(substring) : -1;
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
            } catch (final IllegalArgumentException ignore) {
                LOG.info("Invalid if-range header field", ignore);
                ranges.add(full);
            }
        }
    }

    private static void handleMultipartRangeRequest(final Artifact artifact, final HttpServletResponse response,
            final DbArtifact file, final ControllerManagement controllerManagement, final Long statusId,
            final List<ByteRange> ranges) {
        response.setContentType("multipart/byteranges; boundary=" + ByteRange.MULTIPART_BOUNDARY);
        response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);

        for (final ByteRange r : ranges) {
            try (InputStream inputStream = file.getFileInputStream()) {

                // Add multipart boundary and header fields for every range.
                response.getOutputStream().println();
                response.getOutputStream().println("--" + ByteRange.MULTIPART_BOUNDARY);
                response.getOutputStream()
                        .println("Content-Range: bytes " + r.getStart() + "-" + r.getEnd() + "/" + r.getTotal());

                // Copy single part range of multi part range.
                copyStreams(inputStream, response.getOutputStream(), controllerManagement, statusId, r.getStart(),
                        r.getLength());
            } catch (final IOException e) {
                throwFileStreamingFailedException(artifact, e);
            }
        }
        try {
            // End with final multipart boundary.
            response.getOutputStream().println();
            response.getOutputStream().print("--" + ByteRange.MULTIPART_BOUNDARY + "--");
        } catch (final IOException e) {
            throwFileStreamingFailedException(artifact, e);
        }
    }

    private static void throwFileStreamingFailedException(final Artifact artifact, final IOException e) {
        LOG.error("multipartRangeRequest of file ({}) failed!", artifact.getFilename(), e);
        throw new FileSteamingFailedException(artifact.getFilename());
    }

    private static void handleStandardRangeRequest(final Artifact artifact, final HttpServletResponse response,
            final DbArtifact file, final ControllerManagement controllerManagement, final Long statusId,
            final List<ByteRange> ranges) {
        final ByteRange r = ranges.get(0);
        response.setHeader(HttpHeaders.CONTENT_RANGE, "bytes " + r.getStart() + "-" + r.getEnd() + "/" + r.getTotal());
        response.setHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(r.getLength()));
        response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);

        try (InputStream inputStream = file.getFileInputStream()) {
            copyStreams(inputStream, response.getOutputStream(), controllerManagement, statusId, r.getStart(),
                    r.getLength());
        } catch (final IOException e) {
            LOG.error("standardRangeRequest of file ({}) failed!", artifact.getFilename(), e);
            throw new FileSteamingFailedException(artifact.getFilename());
        }
    }

    private static long copyStreams(final InputStream from, final OutputStream to,
            final ControllerManagement controllerManagement, final Long statusId, final long start, final long length)
            throws IOException {
        Preconditions.checkNotNull(from);
        Preconditions.checkNotNull(to);
        final byte[] buf = new byte[BUFFER_SIZE];
        long total = 0;
        int progressPercent = 1;

        // skipp until start is reached
        long skipped = 0;
        do {
            skipped += from.skip(start);
        } while (skipped < start);

        long toRead = length;
        boolean toContinue = true;
        long shippedSinceLastEvent = 0;

        while (toContinue) {
            final int r = from.read(buf);
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

            if (controllerManagement != null) {
                final int newPercent = DoubleMath.roundToInt(total * 100.0 / length, RoundingMode.DOWN);

                // every 10 percent an event
                if (newPercent == 100 || newPercent > progressPercent + 10) {
                    progressPercent = newPercent;
                    controllerManagement.downloadProgress(statusId, length, shippedSinceLastEvent, total);
                    shippedSinceLastEvent = 0;
                }
            }
        }
        return total;
    }

    /**
     * Checks given CSV string for defined match value or * wildcard.
     *
     * @param matchHeader
     *            to search through
     * @param toMatch
     *            to search for
     *
     * @return <code>true</code> if string matches.
     */
    public static boolean matchesHttpHeader(final String matchHeader, final String toMatch) {
        final String[] matchValues = matchHeader.split("\\s*,\\s*");
        Arrays.sort(matchValues);
        return Arrays.binarySearch(matchValues, toMatch) > -1 || Arrays.binarySearch(matchValues, "*") > -1;
    }

}
