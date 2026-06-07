// Created: 06 Mai 2025
package de.freese.arser.core.model;

/**
 * @author Thomas Freese
 */
public abstract class AbstractFileResource implements FileResource {
    private final long contentLength;

    // private final List<ResourceProcessor> processors = new ArrayList<>();

    protected AbstractFileResource(final long contentLength) {
        super();

        this.contentLength = contentLength;
    }

    @Override
    public long getContentLength() {
        return contentLength;
    }

    // @Override
    // public void transferTo(final OutputStream outputStream) throws Exception {
    //     final Path tempFile = Files.createTempFile("large_upload_", ".tmp");
    //     Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
    //
    //     try (DataContainer = new DataContainer(MEMORY_THRESHOLD)) {
    //         container.readFrom(inputStream);
    //
    //         processors.forEach(container);
    //     }
    //     finally {
    //         Files.deleteIfExists(tempFile);
    //     }
    // }
}
